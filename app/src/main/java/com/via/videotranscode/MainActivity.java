package com.via.videotranscode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.BUFFER_FLAG_SYNC_FRAME;

public class MainActivity extends AppCompatActivity {
    AvcDecoder avcDecoder = null;
    AvcEncoder avcEncoder = null;

    AvcDecoder.FrameListener frameListener = null;
    AvcEncoder.EncodedFrameListener encodedFrameListener = null;
    int color_format = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
    int encode_width = 1280;
    int encode_height = 720;
    MediaMuxer mediaMuxer = null;
    int videoTrack = -1;

    long time = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button btn = new Button(this);
        btn.setText("Start Encode");


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    mediaMuxer = new MediaMuxer("/sdcard/DCIM/output.mp4",MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    final MediaFormat mediaFormat = new MediaFormat();
                    mediaFormat.setInteger(MediaFormat.KEY_WIDTH, encode_width);
                    mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, encode_height);
                    mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC);
                    mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                    
                    encodedFrameListener = new AvcEncoder.EncodedFrameListener() {
                        @Override
                        public void getSpsPps(byte[] sps, byte[] pps) {
                            mediaFormat.setByteBuffer("csd-0",ByteBuffer.wrap(sps));
                            mediaFormat.setByteBuffer("csd-1",ByteBuffer.wrap(pps));
                            if(videoTrack == -1) videoTrack = mediaMuxer.addTrack(mediaFormat);

                            mediaMuxer.start();
                        }

                        @Override
                        public void getNalu(byte[] nalu) {
//                            Log.d("HANK","Nalu:"+nalu[0]+","+nalu[1]+","+nalu[2]+","+nalu[3]+","+nalu[4]+","+nalu[5]);
                            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                            info.presentationTimeUs = time*1000;
                            info.size = nalu.length;
                            info.offset = 0;
                            time+=33;
                            info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                            mediaMuxer.writeSampleData(videoTrack, ByteBuffer.wrap(nalu), info);
                        }
                    };

                    avcEncoder = new AvcEncoder(1280,720,color_format,encodedFrameListener);
                    frameListener = new AvcDecoder.FrameListener() {
                        @Override
                        public void onFrameDecoded(ByteBuffer b, int offset, int size) {

                            avcEncoder.offerEncoder(b, offset, size);
                        }

                        public void onEOS() {
                            mediaMuxer.stop();
                            try {
                                avcEncoder.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };

                    avcDecoder = new AvcDecoder();
                    avcDecoder.init("/sdcard/DCIM/Video_1280x720_F57.mp4", color_format, frameListener);
                    avcDecoder.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        setContentView(btn);
    }
}
