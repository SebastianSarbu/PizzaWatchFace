package com.example.pizzawatchface;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class BackgroundDividerDrawable {
    private final float centerX;
    private final float centerY;
    private final int width, height;
    private final float offsetX;

    private float l1StartX, l1StartY, l1EndX, l1EndY;
    private float l2StartX, l2StartY, l2EndX, l2EndY;
    private float l3StartX, l3StartY, l3EndX, l3EndY;
    private float l4StartX, l4StartY, l4EndX, l4EndY;
    private float l5StartX, l5StartY, l5EndX, l5EndY;
    private float l6StartX, l6StartY, l6EndX, l6EndY;

    private Paint mLinePaint;

    public BackgroundDividerDrawable(int width, int height) {
        this.width = width;
        this.height = height;
        centerX = width / 2f;
        centerY = height / 2f;
        offsetX = centerX + width /4f;

        Initialize();
    }

    private void Initialize() {
        mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(1f);
        mLinePaint.setColor(Color.WHITE);
        mLinePaint.setAntiAlias(true);

        //sinus part zeroes out
        //xRot=cos(θ)⋅(x−cx)−sin(θ)⋅(y−cy)+cx
        //cosine part zeroes out
        //yRot=sin(θ)⋅(x−cx)+cos(θ)⋅(y−cy)+cy

        l1StartX = Constants.COS_22_5 * (offsetX - centerX) + centerX;
        l1StartY = -Constants.SIN_22_5 * ((float) width - offsetX) + centerY;
        l1EndX = Constants.COS_22_5 * ((float) width - centerX) + centerX;
        l1EndY = -Constants.SIN_22_5 * ((float) width - centerX) + centerY;

        l2StartX = -Constants.COS_22_5 * (offsetX - centerX) + centerX;
        l2StartY = -Constants.SIN_22_5 * ((float) width - offsetX) + centerY;
        l2EndX = -Constants.COS_22_5 * ((float) width - centerX) + centerX;
        l2EndY = -Constants.SIN_22_5 * ((float) width - centerX) + centerY;

        l3StartX = Constants.SIN_22_5 * (offsetX - centerX) + centerX;
        l3StartY = -Constants.COS_22_5 * ((float) width - offsetX) + centerY;
        l3EndX = Constants.SIN_22_5 * ((float) width - centerX) + centerX;
        l3EndY = -Constants.COS_22_5 * ((float) width - centerX) + centerY;

        l4StartX = -Constants.SIN_22_5 * (offsetX - centerX) + centerX;
        l4StartY = -Constants.COS_22_5 * ((float) width - offsetX) + centerY;
        l4EndX = -Constants.SIN_22_5 * ((float) width - centerX) + centerX;
        l4EndY = -Constants.COS_22_5 * ((float) width - centerX) + centerY;

        l5StartX = -Constants.COS_22_5 * (offsetX - centerX) + centerX;
        l5StartY = Constants.SIN_22_5 * ((float) width - offsetX) + centerY;
        l5EndX = -Constants.COS_22_5 * ((float) width - centerX) + centerX;
        l5EndY = Constants.SIN_22_5 * ((float) width - centerX) + centerY;

        l6StartX = Constants.COS_22_5 * (offsetX - centerX) + centerX;
        l6StartY = Constants.SIN_22_5 * ((float) width - offsetX) + centerY;
        l6EndX = Constants.COS_22_5 * ((float) width - centerX) + centerX;
        l6EndY = Constants.SIN_22_5 * ((float) width - centerX) + centerY;

    }

    public void draw(Canvas canvas) {
        canvas.drawLine(l1StartX, l1StartY, l1EndX, l1EndY, mLinePaint);
        canvas.drawLine(l2StartX, l2StartY, l2EndX, l2EndY, mLinePaint);
        canvas.drawLine(l3StartX, l3StartY, l3EndX, l3EndY, mLinePaint);
        canvas.drawLine(l4StartX, l4StartY, l4EndX, l4EndY, mLinePaint);
        canvas.drawLine(l5StartX, l5StartY, l5EndX, l5EndY, mLinePaint);
        canvas.drawLine(l6StartX, l6StartY, l6EndX, l6EndY, mLinePaint);
    }
}
