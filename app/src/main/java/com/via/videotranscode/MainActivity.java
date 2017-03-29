package com.via.videotranscode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    AvcDecoder avcDecoder = null;
    AvcEncoder avcEncoder = null;

    AvcDecoder.FrameListener frameListener = null;
    AvcEncoder.EncodedFrameListener encodedFrameListener = null;
    int color_format = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
    int encode_width = 1280;
    int encode_height = 720;
    int encode_bitrate = 8000000;
    MediaMuxer mediaMuxer = null;
    int videoTrack = -1;

    int frameCount = 0;
    long time = 0;


    private EditText source_et = null;
    private EditText destination_et = null;
    private EditText bitrate_et = null;
    private TextView source_info_tv = null;
    private Button loadBtn = null;
    private Button startBtn = null;
    private ProgressView progressView = null;
    private String videoPath;
    private boolean bInit = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        source_et = (EditText) findViewById(R.id.source);
        destination_et = (EditText) findViewById(R.id.output);
        bitrate_et = (EditText) findViewById(R.id.bitrate);
        source_info_tv = (TextView) findViewById(R.id.sourceInfo);
        progressView = (ProgressView) findViewById(R.id.progressView);


        source_et.setText("/storage/extsdcard/Video_1280x720_F57.mp4");

        loadBtn = (Button) findViewById(R.id.loadBtn);
        loadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File videoFile = new File(source_et.getText().toString());
                if(videoFile.exists() && videoFile.canRead()) {
                    avcDecoder = new AvcDecoder();
                    if(avcDecoder.init(source_et.getText().toString(), color_format, frameListener)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("  Video Path: "+source_et.getText().toString()+"\n");
                        sb.append("  Video Resolution: "+avcDecoder.getWidth()+"x"+avcDecoder.getHeight()+"\n");
                        sb.append("  Video Duration: "+avcDecoder.getDuration());
                        source_info_tv.setText(sb.toString());

                        String str = source_et.getText().toString();
                        String default_destination_path = str.substring(0, str.lastIndexOf('.'));
                        videoPath = source_et.getText().toString();
                        progressView.setMax(avcDecoder.getDuration());
                        destination_et.setText(default_destination_path+"_"+bitrate_et.getText().toString()+"bps_transcode.mp4");
//                        startBtn.setClickable(true);
                        startBtn.setVisibility(View.VISIBLE);
                        bInit = true;


                    } else {
                        avcDecoder = null;
                        source_info_tv.setText("Cannot Decode this file");
                    }
                } else {
                    source_info_tv.setText("File not found or not readable");
                }
            }
        });

        bitrate_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(bInit) {
                    String str = source_et.getText().toString();
                    String default_destination_path = str.substring(0, str.lastIndexOf('.'));
                    destination_et.setText(default_destination_path+"_"+bitrate_et.getText().toString()+"bps_transcode.mp4");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        source_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                bInit = false;
                startBtn.setVisibility(View.INVISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });





        startBtn = (Button) findViewById(R.id.startBtn);
//        startBtn.setClickable(false);
        startBtn.setVisibility(View.INVISIBLE);


        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    avcDecoder = new AvcDecoder();


                    frameListener = new AvcDecoder.FrameListener() {
                        @Override
                        public void onFrameDecoded(ByteBuffer b, int offset, int size) {

                            avcEncoder.offerEncoder(b, offset, size);
                        }

                        public void onEOS() {
                            mediaMuxer.stop();
                            mediaMuxer.release();
                            mediaMuxer = null;
                            videoTrack = -1;
                            avcDecoder = null;
                            try {
                                avcEncoder.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };


                    avcDecoder.init(videoPath, color_format, frameListener);
                    encode_height = avcDecoder.getHeight();
                    encode_width = avcDecoder.getWidth();

                    mediaMuxer = new MediaMuxer(destination_et.getText().toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    final MediaFormat mediaFormat = new MediaFormat();
                    mediaFormat.setInteger(MediaFormat.KEY_WIDTH, encode_width);
                    mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, encode_height);
                    mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC);
                    mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30);

                    encodedFrameListener = new AvcEncoder.EncodedFrameListener() {
                        @Override
                        public void getSpsPps(byte[] sps, byte[] pps) {
                            mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
                            mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
                            if (videoTrack == -1) videoTrack = mediaMuxer.addTrack(mediaFormat);

                            mediaMuxer.start();
                        }

                        @Override
                        public void getNalu(byte[] nalu) {
//                            Log.d("HANK","Nalu:"+nalu[0]+","+nalu[1]+","+nalu[2]+","+nalu[3]+","+nalu[4]+","+nalu[5]);
                            Log.d("HANK", "FrameCount:" + frameCount);
                            frameCount++;
                            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                            info.presentationTimeUs = time * 1000;
                            info.size = nalu.length;
                            info.offset = 0;
                            time += 33;
                            info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                            mediaMuxer.writeSampleData(videoTrack, ByteBuffer.wrap(nalu), info);

                            progressView.setCurrent(avcDecoder.getCurrentPosition());

                        }
                    };

                    AvcEncoder.EncodeParameters encodeParameters = new AvcEncoder.EncodeParameters(encode_width, encode_height, Integer.valueOf(bitrate_et.getText().toString()), color_format);

                    avcEncoder = new AvcEncoder(encodeParameters, encodedFrameListener);


                    avcDecoder.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
