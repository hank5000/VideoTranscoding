package com.via.videotranscode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by hankwu on 2/16/17.
 */

public class ProgressView extends View {
    long max = 0;
    long current = 0;
    Paint background_paint;
    Paint progress_paint;
    Paint text_paint;

    public ProgressView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        background_paint = new Paint();
        background_paint.setColor(Color.GRAY);
        progress_paint = new Paint();
        progress_paint.setColor(Color.GREEN);
        text_paint = new Paint();
        text_paint.setColor(Color.RED);
        text_paint.setTextSize(30);


    }

    @Override
    public synchronized void draw(final Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        float percentage = (float)current/(float)max;
        float drawWidth = width*percentage;
        float drawHeight = height/3;

        canvas.drawRect(0,drawHeight-10,width,drawHeight*2+10,background_paint);

//        Log.d("HANK",width+"x"+height);
//        Log.d("HANK",drawWidth+"x"+drawHeight);
        canvas.drawRect(0,drawHeight,drawWidth,drawHeight*2,progress_paint);

        canvas.drawText(current+"/"+max,width/2,height/2,text_paint);


    }

    public void setMax(long m) {
        max = m;
        postInvalidate();
    }

    public void setCurrent(long c) {
        current = c;
        postInvalidate();
    }



}
