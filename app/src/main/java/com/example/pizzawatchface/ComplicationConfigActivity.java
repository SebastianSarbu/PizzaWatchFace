/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.pizzawatchface;

import static com.example.pizzawatchface.Constants.*;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;

import java.util.concurrent.Executors;

public class ComplicationConfigActivity extends Activity implements View.OnClickListener {

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;

    // Selected complication id by user.
    private int mSelectedComplicationId;

    // ComponentName used to identify a specific service that renders the watch face.
    private ComponentName mWatchFaceComponentName;

    // Required to retrieve complication data from watch face for preview.
    private ProviderInfoRetriever mProviderInfoRetriever;


    private ImageView mRightComplicationBackground;
    private ImageView mTopRightComplicationBackground;
    private ImageView mTopRightRangedComplicationBackground;
    private ImageView mTopComplicationBackground;
    private ImageView mTopLeftComplicationBackground;
    private ImageView mTopLeftRangedComplicationBackground;
    private ImageView mLeftComplicationBackground;
    private ImageView mBottomComplicationBackground;
    private ImageView mBottomLeftRangedComplicationBackground;
    private ImageView mBottomRightRangedComplicationBackground;
    private ImageView mCenterComplicationBackground;

    private ImageButton mRightComplication;
    private ImageButton mTopRightComplication;
    private ImageButton mTopRightRangedComplication;
    private ImageButton mTopComplication;
    private ImageButton mTopLeftComplication;
    private ImageButton mTopLeftRangedComplication;
    private ImageButton mLeftComplication;
    private ImageButton mBottomComplication;
    private ImageButton mBottomLeftRangedComplication;
    private ImageButton mBottomRightRangedComplication;
    private ImageButton mCenterComplication;

    private Drawable mDefaultAddComplicationDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);

        mDefaultAddComplicationDrawable = getDrawable(R.drawable.add_complication);

        mSelectedComplicationId = -1;

        mWatchFaceComponentName =
                new ComponentName(getApplicationContext(), PizzaWatchFaceService.class);

        mRightComplicationBackground = findViewById(R.id.right_complication_background);
        mRightComplication = findViewById(R.id.right_complication);
        setUpComplication(mRightComplicationBackground, mRightComplication);

        mTopRightComplicationBackground = findViewById(R.id.top_right_complication_background);
        mTopRightComplication = findViewById(R.id.top_right_complication);
        setUpComplication(mTopRightComplicationBackground, mTopRightComplication);

        mTopRightRangedComplicationBackground = findViewById(R.id.top_right_ranged_complication_background);
        mTopRightRangedComplication = findViewById(R.id.top_right_ranged_complication);
        setUpComplication(mTopRightRangedComplicationBackground, mTopRightRangedComplication);

        mTopComplicationBackground = findViewById(R.id.top_complication_background);
        mTopComplication = findViewById(R.id.top_complication);
        setUpComplication(mTopComplicationBackground, mTopComplication);

        mTopLeftComplicationBackground = findViewById(R.id.top_left_complication_background);
        mTopLeftComplication = findViewById(R.id.top_left_complication);
        setUpComplication(mTopLeftComplicationBackground, mTopLeftComplication);

        mTopLeftRangedComplicationBackground = findViewById(R.id.top_left_ranged_complication_background);
        mTopLeftRangedComplication = findViewById(R.id.top_left_ranged_complication);
        setUpComplication(mTopLeftRangedComplicationBackground, mTopLeftRangedComplication);

        mLeftComplicationBackground = findViewById(R.id.left_complication_background);
        mLeftComplication = findViewById(R.id.left_complication);
        setUpComplication(mLeftComplicationBackground, mLeftComplication);

        mBottomComplicationBackground = findViewById(R.id.bottom_complication_background);
        mBottomComplication = findViewById(R.id.bottom_complication);
        setUpComplication(mBottomComplicationBackground, mBottomComplication);

        mBottomRightRangedComplicationBackground = findViewById(R.id.bottom_right_ranged_complication_background);
        mBottomRightRangedComplication = findViewById(R.id.bottom_right_ranged_complication);
        setUpComplication(mBottomRightRangedComplicationBackground, mBottomRightRangedComplication);

        mBottomLeftRangedComplicationBackground = findViewById(R.id.bottom_left_ranged_complication_background);
        mBottomLeftRangedComplication = findViewById(R.id.bottom_left_ranged_complication);
        setUpComplication(mBottomLeftRangedComplicationBackground, mBottomLeftRangedComplication);

        mCenterComplicationBackground = findViewById(R.id.center_complication_background);
        mCenterComplication = findViewById(R.id.center_complication);
        setUpComplication(mCenterComplicationBackground, mCenterComplication);

        PizzaWatchFaceService.Engine e = PizzaWatchFaceService.getEngine();
        Switch mHollowSwitch = findViewById(R.id.hollow_switch);
        mHollowSwitch.setChecked(e.getHollowMode());
        mHollowSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                PizzaWatchFaceService.Engine e = PizzaWatchFaceService.getEngine();
                e.setHollowMode(b);
            }
        });

        // Initialization of code to retrieve active complication data for the watch face.
        mProviderInfoRetriever =
                new ProviderInfoRetriever(getApplicationContext(), Executors.newCachedThreadPool());
        mProviderInfoRetriever.init();

        retrieveInitialComplicationsData();
    }

    // Used by {@link ComplicationConfigActivity} to retrieve id for complication locations and
    // to check if complication location is supported.
    static int getComplicationId(
            ComplicationLocation complicationLocation) {
        // Add any other supported locations here you would like to support. In our case, we are
        // only supporting a left and right complication.
        switch (complicationLocation) {
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            case TOP_RIGHT:
                return TOP_RIGHT_COMPLICATION_ID;
            case TOP_RIGHT_RANGED:
                return TOP_RIGHT_RANGED_COMPLICATION_ID;
            case TOP:
                return TOP_COMPLICATION_ID;
            case TOP_LEFT:
                return TOP_LEFT_COMPLICATION_ID;
            case TOP_LEFT_RANGED:
                return TOP_LEFT_RANGED_COMPLICATION_ID;
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case BOTTOM:
                return BOTTOM_COMPLICATION_ID;
            case BOTTOM_RIGHT_RANGED:
                return BOTTOM_RIGHT_RANGED_COMPLICATION_ID;
            case BOTTOM_LEFT_RANGED:
                return BOTTOM_LEFT_RANGED_COMPLICATION_ID;
            case CENTER:
                return CENTER_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    private void setUpComplication(ImageView complicationBackground, ImageButton complication) {
        // Sets up left complication preview.
        complication.setOnClickListener(this);

        // Sets default as "Add Complication" icon.
        complication.setImageDrawable(mDefaultAddComplicationDrawable);
        complicationBackground.setVisibility(View.INVISIBLE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Required to release retriever for active complication data.
        mProviderInfoRetriever.release();
    }

    public void retrieveInitialComplicationsData() {

        final int[] complicationIds = COMPLICATION_IDS;

        mProviderInfoRetriever.retrieveProviderInfo(
                new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                    @Override
                    public void onProviderInfoReceived(
                            int watchFaceComplicationId,
                            @Nullable ComplicationProviderInfo complicationProviderInfo) {
                        updateComplicationViews(watchFaceComplicationId, complicationProviderInfo);
                    }
                },
                mWatchFaceComponentName,
                complicationIds);
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mRightComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.RIGHT);
        } else if (view.equals(mTopRightComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.TOP_RIGHT);
        } else if (view.equals(mTopRightRangedComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.TOP_RIGHT_RANGED);
        } else if (view.equals(mTopComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.TOP);
        } else if (view.equals(mTopLeftComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.TOP_LEFT);
        } else if (view.equals(mTopLeftRangedComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.TOP_LEFT_RANGED);
        } else if (view.equals(mLeftComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.LEFT);
        } else if (view.equals(mBottomComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.BOTTOM);
        } else if (view.equals(mBottomRightRangedComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.BOTTOM_RIGHT_RANGED);
        } else if (view.equals(mBottomLeftRangedComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.BOTTOM_LEFT_RANGED);
        } else if (view.equals(mCenterComplication)) {
            launchComplicationHelperActivity(ComplicationLocation.CENTER);
        }
    }

    // Verifies the watch face supports the complication location, then launches the helper
    // class, so user can choose their complication data provider.
    private void launchComplicationHelperActivity(ComplicationLocation complicationLocation) {

        mSelectedComplicationId =
                getComplicationId(complicationLocation);

        if (mSelectedComplicationId >= 0) {

            int[] supportedTypes =
                    PizzaWatchFaceService.getSupportedComplicationTypes(
                            complicationLocation);

            startActivityForResult(
                    ComplicationHelperActivity.createProviderChooserHelperIntent(
                            getApplicationContext(),
                            mWatchFaceComponentName,
                            mSelectedComplicationId,
                            supportedTypes),
                    ComplicationConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE);
        }
    }

    private void updateComplicationView(ComplicationProviderInfo complicationProviderInfo, ImageButton complication, ImageView complicationBackground) {
        if (complicationProviderInfo != null) {
            complication.setImageIcon(complicationProviderInfo.providerIcon);
            complicationBackground.setVisibility(View.VISIBLE);
            complication.setScaleType(ImageView.ScaleType.CENTER);

        } else {
            complication.setImageDrawable(mDefaultAddComplicationDrawable);
            complicationBackground.setVisibility(View.INVISIBLE);
            complication.setScaleType(ImageView.ScaleType.FIT_CENTER);

        }
    }


    public void updateComplicationViews(
            int watchFaceComplicationId, ComplicationProviderInfo complicationProviderInfo) {

        if (watchFaceComplicationId == RIGHT_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mRightComplication, mRightComplicationBackground);
        } else if (watchFaceComplicationId == TOP_RIGHT_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mTopRightComplication, mTopRightComplicationBackground);
        } else if (watchFaceComplicationId == TOP_RIGHT_RANGED_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mTopRightRangedComplication, mTopRightRangedComplicationBackground);
        } else if (watchFaceComplicationId == TOP_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mTopComplication, mTopComplicationBackground);
        } else if (watchFaceComplicationId == TOP_LEFT_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mTopLeftComplication, mTopLeftComplicationBackground);
        } else if (watchFaceComplicationId == TOP_LEFT_RANGED_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mTopLeftRangedComplication, mTopLeftRangedComplicationBackground);
        } else if (watchFaceComplicationId == LEFT_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mLeftComplication, mLeftComplicationBackground);
        } else if (watchFaceComplicationId == BOTTOM_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mBottomComplication, mBottomComplicationBackground);
        } else if (watchFaceComplicationId == BOTTOM_LEFT_RANGED_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mBottomLeftRangedComplication, mBottomLeftRangedComplicationBackground);
        } else if (watchFaceComplicationId == BOTTOM_RIGHT_RANGED_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mBottomRightRangedComplication, mBottomRightRangedComplicationBackground);
        } else if (watchFaceComplicationId == CENTER_COMPLICATION_ID) {
            updateComplicationView(complicationProviderInfo, mCenterComplication, mCenterComplicationBackground);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE && resultCode == RESULT_OK) {
            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);

            if (mSelectedComplicationId >= 0) {
                updateComplicationViews(mSelectedComplicationId, complicationProviderInfo);
            }
        }
    }
}
