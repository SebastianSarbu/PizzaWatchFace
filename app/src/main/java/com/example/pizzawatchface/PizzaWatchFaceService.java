package com.example.pizzawatchface;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
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

import androidx.core.content.ContextCompat;

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

    private static Engine engine;

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
        engine = new Engine();
        return engine;
    }

    public static Engine getEngine() {
        return engine;
    }

    public class EngineHandler extends Handler {
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

    public class Engine extends CanvasWatchFaceService.Engine {
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
        private Paint mBottomPaint;
        private Paint mBackgroundPaint;

        private BackgroundDividerDrawable backgroundDividerDrawable;

        private boolean isAmbientMode;
        private boolean isHollowMode;

        /*
         * Whether the display supports fewer bits for each color in ambient mode.
         * When true, we disable anti-aliasing in ambient mode.
         */
        private boolean hasLowBitAmbient;

        /*
         * Whether the display supports burn in protection in ambient mode.
         * When true, remove the persistent images in ambient mode.
         */
        private boolean hasBurnInProtection;
        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private ComplicationDrawable[] mComplicationDrawables;

        // Stores the ranged complication on the edge of the screen
        private ArcComplication[] mRangedComplications;

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

            mBottomPaint = new Paint();
            mBottomPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mBottomPaint.setStrokeWidth(1f);
            mBottomPaint.setTextAlign(Paint.Align.CENTER);
            int BOTTOM_ROW_ITEM_SIZE = 24;
            mBottomPaint.setTextSize(BOTTOM_ROW_ITEM_SIZE);
            mBottomPaint.setColor(Color.WHITE);
            mBottomPaint.setAntiAlias(true);

            initializeComplications();
        }

        private void initializeComplications() {
            complicationData = new ComplicationData[COMPLICATION_IDS.length];
            mComplicationDrawables = new ComplicationDrawable[COMPLICATION_IDS.length];
            mRangedComplications = new ArcComplication[RANGE_COMPLICATION_COUNT];

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

        public boolean getHollowMode() {
            return isHollowMode;
        }

        public void setHollowMode(boolean hollow) {
            isHollowMode = hollow;
            if(hollow) {
                mCenterPaint.setStyle(Paint.Style.STROKE);
                mBottomPaint.setStyle(Paint.Style.STROKE);
                for (ArcComplication mRangedComplication : mRangedComplications) {
                    mRangedComplication.setHollow(true);
                }
            } else {
                mCenterPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mBottomPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                for (ArcComplication mRangedComplication : mRangedComplications) {
                    mRangedComplication.setHollow(false);
                }
            }
            invalidate();
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
            hasLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            hasBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawables[i];

                if(complicationDrawable != null) {
                    complicationDrawable.setLowBitAmbient(hasLowBitAmbient);
                    complicationDrawable.setBurnInProtection(hasBurnInProtection);
                }
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            isAmbientMode = inAmbientMode;

            if(isAmbientMode) {
                if(hasLowBitAmbient) {
                    mCenterPaint.setAntiAlias(false);
                    mBottomPaint.setAntiAlias(false);
                }
            } else {
                if(hasLowBitAmbient) {
                    mCenterPaint.setAntiAlias(true);
                    mBottomPaint.setAntiAlias(true);
                }
            }

            setHollowMode(isHollowMode);

            // Update drawable complications' ambient state.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawables[i];
                complicationDrawable.setInAmbientMode(isAmbientMode);
            }

            for (ArcComplication mRangedComplication : mRangedComplications) {
                mRangedComplication.setAmbientMode(isAmbientMode);
            }

            // Check and trigger whether or not timer should be running (only in active mode).
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

            Context context = getApplicationContext();

            mCenterX = width / 2f;
            mCenterY = height / 2f;

            backgroundDividerDrawable = new BackgroundDividerDrawable(width, height);

            int sizeOfComplication = width / 4;
            mCenterPaint.setTextSize(width / 8f);

            mComplicationMargin = sizeOfComplication / 18f;
            mCenterPaint.setTextSize(width / 8f);


            float rangeWidthF = width / 20f;
            int rangeThickness = width / 20;
            float rangeOffsetF = rangeWidthF / 2;
            int rangeOffset = rangeThickness / 2;

            RectF rangeBoundsF = new RectF(rangeOffsetF, rangeOffsetF, width - rangeOffsetF,
                    height - rangeOffsetF);

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

            //region Arc bounds and complications

            Rect topRightRangedBounds =
                    // Left, Top, Right, Bottom
                    new Rect((int) rangeBoundsF.centerX() - rangeOffset,
                            0,
                            (int) rangeBoundsF.centerX() + rangeOffset,
                            rangeThickness);

            Rect topLeftRangedBounds = new Rect((int) rangeBoundsF.left - rangeOffset,
                    (int) rangeBoundsF.centerY() - rangeOffset,
                    (int) rangeBoundsF.left + rangeOffset,
                    (int) rangeBoundsF.centerY() + rangeOffset);

            Rect bottomRightRangedBounds = new Rect((int) rangeBoundsF.right - rangeOffset,
                    (int) rangeBoundsF.centerY() - rangeOffset,
                    (int) rangeBoundsF.right + rangeOffset,
                    (int) rangeBoundsF.centerY() + rangeOffset);

            Rect bottomLeftRangedBounds = new Rect((int) rangeBoundsF.centerX() - rangeOffset,
                    (int) rangeBoundsF.bottom - rangeOffset,
                    (int) rangeBoundsF.centerX() + rangeOffset,
                    (int) rangeBoundsF.bottom + rangeOffset);



            //Order matters when we add the complication in the array because we iterate clockwise
            ArcComplication topRightRanged = new ArcComplication(context, rangeBoundsF,
                    topRightRangedBounds, rangeWidthF, ContextCompat.getColor(context, R.color.purple),
                    ContextCompat.getColor(context, R.color.light_purple), 280, 70);
            mRangedComplications[0] = topRightRanged;

            ArcComplication bottomRightRanged = new ArcComplication(context, rangeBoundsF,
                    bottomRightRangedBounds, rangeWidthF, ContextCompat.getColor(context, R.color.yellow),
                    ContextCompat.getColor(context, R.color.light_yellow), 10, 70);
            mRangedComplications[1] = bottomRightRanged;

            ArcComplication bottomLeftRanged = new ArcComplication(context, rangeBoundsF, bottomLeftRangedBounds, rangeWidthF,
                    ContextCompat.getColor(context, R.color.red), ContextCompat.getColor(context, R.color.light_red),
                    100, 70);
            mRangedComplications[2] = bottomLeftRanged;

            ArcComplication topLeftRanged = new ArcComplication(context, rangeBoundsF, topLeftRangedBounds, rangeWidthF,
                    ContextCompat.getColor(context, R.color.green), ContextCompat.getColor(context, R.color.light_green),
                    190, 70);
            mRangedComplications[3] = topLeftRanged;

            //endregion
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            if (tapType == TAP_TYPE_TAP) {
                int tappedComplicationId = getTappedComplicationId(x, y);
                if (tappedComplicationId != -1) {
                    onComplicationTap(tappedComplicationId);
                }
            }
        }
        /*
         * Determines if tap inside a complication area or returns -1.
         */
        private int getTappedComplicationId(int x, int y) {
            ComplicationData complicationData;
            ComplicationDrawable complicationDrawable;

            long currentTimeMillis = System.currentTimeMillis();

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationData = this.complicationData[i];

                if ((complicationData != null)
                        && (complicationData.isActive(currentTimeMillis))
                        && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                        && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                    complicationDrawable = mComplicationDrawables[i];
                    Rect complicationBoundingRect = complicationDrawable.getBounds();

                    if (complicationBoundingRect.width() > 0) {
                        if (complicationBoundingRect.contains(x, y)) {
                            return i;
                        }
                    }
                }
            }
            return -1;
        }

        // Fires PendingIntent associated with complication (if it has one).
        private void onComplicationTap(int complicationId) {
            ComplicationData complicationData =
                    this.complicationData[complicationId];

            if (complicationData != null) {

                if (complicationData.getTapAction() != null) {
                    try {
                        complicationData.getTapAction().send();
                    } catch (PendingIntent.CanceledException e) {

                    }

                } else if (complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                    // Watch face does not have permission to receive complication data, so launch
                    // permission request.
                    ComponentName componentName =
                            new ComponentName(
                                    getApplicationContext(), PizzaWatchFaceService.class);

                    Intent permissionRequestIntent =
                            ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                    getApplicationContext(), componentName);

                    startActivity(permissionRequestIntent);
                }

            }
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

            if(!isAmbientMode) {
                backgroundDividerDrawable.draw(canvas);
                drawComplications(canvas, now);
            }

            for (int i = 0; i < mRangedComplications.length; i++) {
                ComplicationData complicationData = this.complicationData[i + RANGED_ID_OFFSET];

                mRangedComplications[i].draw(canvas, complicationData);
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            for (int i = 0; i < CENTER_COMPLICATION_ID; i++) {
                ComplicationDrawable complicationDrawable = mComplicationDrawables[i];
                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        private void drawBackground(Canvas canvas) {
            if (isAmbientMode && (hasLowBitAmbient || hasBurnInProtection)) {
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
                /* Update time zone in case it changed while we weren't visible. */
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
            return isVisible() && !isAmbientMode;
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