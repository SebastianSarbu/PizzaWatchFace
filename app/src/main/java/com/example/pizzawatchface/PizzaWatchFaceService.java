package com.example.pizzawatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import static com.example.pizzawatchface.Constants.*;
import static com.example.pizzawatchface.ComplicationLocation.*;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn"t
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class PizzaWatchFaceService extends CanvasWatchFaceService {

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    // Used by {@link ComplicationConfigActivity} to retrieve complication types supported by
    // location.
    static int[] getSupportedComplicationTypes(
            ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        if (complicationLocation== BOTTOM)
            return LARGE_COMPLICATION_TYPES;
        else
            return NORMAL_COMPLICATION_TYPES;

    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<PizzaWatchFaceService.Engine> mWeakReference;

        public EngineHandler(PizzaWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            PizzaWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;
        private float mComplicationMargin;
        private Paint mCenterPaint;
        private Paint mBackgroundPaint;
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private ComplicationDrawable[] mComplicationDrawables;

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private ComplicationData[] complicationData;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(PizzaWatchFaceService.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);

            mCenterPaint = new Paint();
            mCenterPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mCenterPaint.setStrokeWidth(1f);
            mCenterPaint.setTextAlign(Paint.Align.CENTER);
            mCenterPaint.setColor(Color.WHITE);
            mCenterPaint.setAntiAlias(true);

            initializeComplications();
        }

        private void initializeComplications() {
            complicationData = new ComplicationData[COMPLICATION_IDS.length];
            mComplicationDrawables = new ComplicationDrawable[COMPLICATION_IDS.length];

            for(int i = 0; i < COMPLICATION_IDS.length; i++) {
                initializeComplication(i);
            }

            setActiveComplications(COMPLICATION_IDS);
        }

        private void initializeComplication(int complicationId) {
            ComplicationDrawable complicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            if (complicationDrawable != null) {
                complicationDrawable.setContext(getApplicationContext());
                mComplicationDrawables[complicationId] = complicationDrawable;
            }
        }

        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            // Adds/updates active complication data in the array.
            this.complicationData[complicationId] = complicationData;

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawables[complicationId];
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }


        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            int sizeOfComplication = width / 4;
            mCenterPaint.setTextSize(width / 8f);

            mComplicationMargin = sizeOfComplication / 18;

            mCenterPaint.setTextSize(width / 8f);

            //region Center bounds and complications

            int midpointOfScreen = width / 2;
            int radialMarginOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            // Left, Top, Right, Bottom
            Rect rightBounds = new Rect(
                    (width - sizeOfComplication),
                    verticalOffset,
                    (width),
                    (verticalOffset + sizeOfComplication));
            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawables[RIGHT_COMPLICATION_ID];
            rightComplicationDrawable.setBounds(rightBounds);

            Rect topRightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (width - radialMarginOffset - sizeOfComplication),
                            (radialMarginOffset),
                            (width - radialMarginOffset),
                            (radialMarginOffset + sizeOfComplication));
            ComplicationDrawable topRightComplicationDrawable =
                    mComplicationDrawables[TOP_RIGHT_COMPLICATION_ID];
            topRightComplicationDrawable.setBounds(topRightBounds);

            Rect topBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen - radialMarginOffset),
                            (0),
                            (midpointOfScreen + radialMarginOffset),
                            (sizeOfComplication + radialMarginOffset));
            ComplicationDrawable topComplicationDrawable =
                    mComplicationDrawables[TOP_COMPLICATION_ID];
            topComplicationDrawable.setBounds(topBounds);

            Rect topLeftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (radialMarginOffset),
                            (radialMarginOffset),
                            (radialMarginOffset + sizeOfComplication),
                            (radialMarginOffset + sizeOfComplication));
            ComplicationDrawable topLeftComplicationDrawable =
                    mComplicationDrawables[TOP_LEFT_COMPLICATION_ID];
            topLeftComplicationDrawable.setBounds(topLeftBounds);

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            0,
                            verticalOffset,
                            (sizeOfComplication),
                            (verticalOffset + sizeOfComplication));
            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawables[LEFT_COMPLICATION_ID];
            leftComplicationDrawable.setBounds(leftBounds);

            // Left, Top, Right, Bottom
            Rect bottomBounds = new Rect(
                    radialMarginOffset,
                    leftBounds.bottom,
                    (width - radialMarginOffset),
                    leftBounds.bottom + sizeOfComplication);
            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawables[BOTTOM_COMPLICATION_ID];
            bottomComplicationDrawable.setBounds(bottomBounds);

            Rect centerBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            leftBounds.right,
                            topBounds.bottom,
                            rightBounds.left,
                            bottomBounds.top);
            ComplicationDrawable centerComplicationDrawable =
                    mComplicationDrawables[CENTER_COMPLICATION_ID];
            centerComplicationDrawable.setBounds(centerBounds);

            //endregion
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(getApplicationContext(), "Watch face tapped", Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);

            if (complicationData[CENTER_COMPLICATION_ID] != null) {
                if (complicationData[CENTER_COMPLICATION_ID].getShortText() != null)
                    canvas.drawText(complicationData[CENTER_COMPLICATION_ID].getShortText().getText(
                            getApplicationContext(), now).toString(),
                            mCenterX, mCenterY + mComplicationMargin, mCenterPaint);
            }

            drawComplications(canvas, now);

        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            for (int i = 0; i < CENTER_COMPLICATION_ID; i++) {
                ComplicationDrawable complicationDrawable = mComplicationDrawables[i];
                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        private void drawBackground(Canvas canvas) {

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawPaint(mBackgroundPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren"t visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            PizzaWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            PizzaWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}