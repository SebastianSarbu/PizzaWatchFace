package com.example.pizzawatchface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;

public class ArcComplication {
    private final int primaryColor;
    private final float width;
    private final int secondaryColor;
    private final Context context;
    private final RectF complicationBounds;
    private final Rect iconBounds;
    private final Paint primaryPaint;
    private final Paint secondaryPaint;
    private final Paint textPaint;
    private final Path textPath;
    private final int startAngle;
    private final int sweepAngle;


    public ArcComplication(Context context, RectF complicationBounds, Rect iconBounds, float width,
                           int primaryColor, int secondaryColor, int startAngle, int sweepAngle) {
        this.context = context;
        this.complicationBounds = complicationBounds;
        this.iconBounds = iconBounds;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.startAngle = startAngle;
        this.sweepAngle = sweepAngle;
        this.width = width;

        primaryPaint = createPaint(primaryColor);
        secondaryPaint = createPaint(secondaryColor);
        textPaint = createPaint(secondaryColor);
        textPaint.setStrokeWidth(1);
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        textPath = new Path();
        textPath.addArc(complicationBounds, startAngle, sweepAngle);
    }

    private Paint createPaint(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(14);
        paint.setShadowLayer(2, 0,0, Color.BLACK);

        return paint;
    }

    public void draw(Canvas canvas, ComplicationData complicationData) {
        if(complicationData == null)
            return;

        if(complicationData.getType() == ComplicationData.TYPE_RANGED_VALUE) {
            float minValue = complicationData.getMinValue();
            float maxValue = complicationData.getMaxValue();
            float currentValue = complicationData.getValue();

            // Translate the current progress to a percentage value between 0 and 1.
            float percent = 0;
            float range = Math.abs(maxValue - minValue);
            if (range > 0) {
                percent = (currentValue - minValue) / range;

                // We don't want to deal progress values outside 0-100.
                percent = Math.max(0f, percent);
                percent = Math.min(1f, percent);
            }

            // Draw it on the canvas.
            canvas.drawArc(complicationBounds, startAngle, sweepAngle, false, secondaryPaint);
            canvas.drawArc(complicationBounds, startAngle, sweepAngle * percent, false, primaryPaint);
        } else {
            String textToDraw = "No data";
            long currentTime = System.nanoTime();
            ComplicationText complicationText = complicationData.getShortText();
            if(complicationText != null) {
                textToDraw = complicationText.getText(context, currentTime).toString();
            }
            complicationText = complicationData.getShortTitle();
            if(complicationText != null) {
                textToDraw += " " + complicationText.getText(context, currentTime).toString();
            }
            complicationText = complicationData.getImageContentDescription();
            if(complicationText != null) {
                textToDraw += " " + complicationText.getText(context, currentTime).toString();
            }

            canvas.drawArc(complicationBounds, startAngle, sweepAngle, false, primaryPaint);
            canvas.drawTextOnPath(textToDraw, textPath, -width/2, width/4   , textPaint);
        }
        drawRangeIcon(canvas, complicationData);
    }

    private void drawRangeIcon(Canvas canvas, ComplicationData complicationData) {
        Icon i = null;
        if (complicationData.getIcon() != null) {
            i = complicationData.getIcon();
        } else if (complicationData.getSmallImage() != null) {
            i = complicationData.getSmallImage();
        } else if (complicationData.getLargeImage() != null) {
            i = complicationData.getLargeImage();
        } else if (complicationData.getBurnInProtectionSmallImage() != null) {
            i = complicationData.getBurnInProtectionSmallImage();
        } else if (complicationData.getBurnInProtectionIcon() != null) {
            i = complicationData.getBurnInProtectionIcon();
        }

        if (i != null) {
            i.setTint(primaryPaint.getColor());
            Drawable icon = i.loadDrawable(context);
            icon.setBounds(iconBounds);
            icon.draw(canvas);
        }
    }

    public void setAmbientMode(boolean ambientMode) {
        if(ambientMode) {
            primaryPaint.setAntiAlias(false);
            primaryPaint.setColor(Color.DKGRAY);
            secondaryPaint.setAntiAlias(false);
            secondaryPaint.setColor(Color.LTGRAY);
        } else {
            primaryPaint.setAntiAlias(true);
            primaryPaint.setColor(primaryColor);
            secondaryPaint.setAntiAlias(true);
            secondaryPaint.setColor(secondaryColor);
        }
    }

    public void setHollow(boolean hollow) {
        if(hollow) {
            primaryPaint.setStrokeWidth(2);
            secondaryPaint.setStrokeWidth(2);
        } else {
            primaryPaint.setStrokeWidth(width);
            secondaryPaint.setStrokeWidth(width);
        }
    }
}