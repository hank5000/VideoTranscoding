package com.via.videotranscode;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.support.annotation.Nullable;

public class AvcEncoder {

    private MediaCodec mediaCodec;
    private byte[] sps;
    private byte[] pps;

    static public class EncodeParameters {
        int width;
        int height;
        int bitrate;
        int color_format;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getBitrate() {
            return bitrate;
        }

        public void setBitrate(int bitrate) {
            this.bitrate = bitrate;
        }

        public int getColor_format() {
            return color_format;
        }

        public void setColor_format(int color_format) {
            this.color_format = color_format;
        }

        public EncodeParameters(int w, int h, int b, int c) {
            width = w;
            height = h;
            bitrate = b;
            color_format = c;
        }

        public EncodeParameters(EncodeParameters e) {
            width = e.getWidth();
            height = e.getHeight();
            bitrate = e.getBitrate();
            color_format = e.getColor_format();
        }
    }

    public interface EncodedFrameListener {
        void getSpsPps(byte[] sps, byte[] pps);
        void getNalu(byte[] nalu);
    }

    private EncodedFrameListener frameListener;

    public AvcEncoder(EncodeParameters encodeParameters, @Nullable EncodedFrameListener listener) throws IOException {
        mediaCodec = MediaCodec.createEncoderByType("video/avc");
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", encodeParameters.getWidth(), encodeParameters.getHeight());
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, encodeParameters.getBitrate());
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, encodeParameters.getColor_format());
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30);
        frameListener = listener;
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    public void close() throws IOException {
        mediaCodec.stop();
        mediaCodec.release();
    }

    public void offerEncoder(ByteBuffer b, int offset, int size) {
        try {
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                byte[] bbb = new byte[1280*720*3/2];
                b.get(bbb);
                inputBuffer.put(ByteBuffer.wrap(bbb));



                mediaCodec.queueInputBuffer(inputBufferIndex, 0, 1280*720*3/2, 0, 0);

            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);
//                Log.d("HANK",outData[0]+","+outData[1]+","+outData[2]+","+outData[3]+","+outData[4]);
                if (sps != null && pps != null) {
                    if(null != frameListener) {
                        frameListener.getNalu(outData);
                    }
                } else {
                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
                    if (spsPpsBuffer.getInt() == 0x00000001) {
                        System.out.println("parsing sps/pps");
                    } else {
                        System.out.println("something is amiss?");
                    }
                    int ppsIndex = 0;
                    while(!(spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x01)) {

                    }
                    ppsIndex = spsPpsBuffer.position();
                    sps = new byte[ppsIndex - 4];
                    System.arraycopy(outData, 0 , sps, 0, sps.length);
                    pps = new byte[outData.length - ppsIndex + 4];
                    System.arraycopy(outData, ppsIndex-4, pps, 0, pps.length);
                    if (null != frameListener) {
                        frameListener.getSpsPps(sps, pps);
                    }
                }
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }
}