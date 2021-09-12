/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.carlauncher;

import android.app.ActivityManager;
import android.app.ActivityView;
import android.car.app.CarActivityView;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.android.car.media.common.PlaybackFragment;

/**
 * Basic Launcher for Android Automotive which demonstrates the use of {@link ActivityView} to host
 * maps content.
 *
 * <p>Note: On some devices, the ActivityView may render with a width, height, and/or aspect
 * ratio that does not meet Android compatibility definitions. Developers should work with content
 * owners to ensure content renders correctly when extending or emulating this class.
 *
 * <p>Note: Since the hosted maps Activity in ActivityView is currently in a virtual display, the
 * system considers the Activity to always be in front. Launching the maps Activity with a direct
 * Intent will not work. To start the maps Activity on the real display, send the Intent to the
 * Launcher with the {@link Intent#CATEGORY_APP_MAPS} category, and the launcher will start the
 * Activity on the real display.
 *
 * <p>Note: The state of the virtual display in the ActivityView is nondeterministic when
 * switching away from and back to the current user. To avoid a crash, this Activity will finish
 * when switching users.
 */
public class CarLauncher extends FragmentActivity {
    private static final String TAG = "CarLauncher";
    private static final boolean DEBUG = false;

    private CarActivityView mActivityView;
    private boolean mActivityViewReady;
    private boolean mIsStarted;
    private DisplayManager mDisplayManager;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    /** Set to {@code true} once we've logged that the Activity is fully drawn. */
    private boolean mIsReadyLogged;

    private final ActivityView.StateCallback mActivityViewCallback =
            new ActivityView.StateCallback() {
                @Override
                public void onActivityViewReady(ActivityView view) {
                    if (DEBUG) Log.d(TAG, "onActivityViewReady(" + getUserId() + ")");
                    mActivityViewReady = true;
                    startMapsInActivityView();
                    maybeLogReady();
                }

                @Override
                public void onActivityViewDestroyed(ActivityView view) {
                    if (DEBUG) Log.d(TAG, "onActivityViewDestroyed(" + getUserId() + ")");
                    mActivityViewReady = false;
                }

                @Override
                public void onTaskMovedToFront(int taskId) {
                    if (DEBUG) {
                        Log.d(TAG, "onTaskMovedToFront(" + getUserId() + "): started="
                                + mIsStarted);
                    }
                    try {
                        if (mIsStarted) {
                            ActivityManager am =
                                    (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                            am.moveTaskToFront(CarLauncher.this.getTaskId(), /* flags= */ 0);
                        }
                    } catch (RuntimeException e) {
                        Log.w(TAG, "Failed to move CarLauncher to front.");
                    }
                }
            };

    private final DisplayListener mDisplayListener = new DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {}
        @Override
        public void onDisplayRemoved(int displayId) {}

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId != getDisplay().getDisplayId()) {
                return;
            }
            // startMapsInActivityView() will check Display's State.
            startMapsInActivityView();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Don't show the maps panel in multi window mode.
        // NOTE: CTS tests for split screen are not compatible with activity views on the default
        // activity of the launcher
        if (isInMultiWindowMode() || isInPictureInPictureMode()) {
            setContentView(R.layout.car_launcher_multiwindow);
        } else {
            setContentView(R.layout.car_launcher);
        }
        initializeFragments();
        mActivityView = findViewById(R.id.maps);
        if (mActivityView != null) {
            mActivityView.setCallback(mActivityViewCallback);
        }
        mDisplayManager = getSystemService(DisplayManager.class);
        mDisplayManager.registerDisplayListener(mDisplayListener, mMainHandler);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        startMapsInActivityView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsStarted = true;
        maybeLogReady();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsStarted = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisplayManager.unregisterDisplayListener(mDisplayListener);
        if (mActivityView != null && mActivityViewReady) {
            mActivityView.release();
        }
    }

    private void startMapsInActivityView() {
        if (mActivityView == null || !mActivityViewReady) {
            return;
        }
        // If we happen to be be resurfaced into a multi display mode we skip launching content
        // in the activity view as we will get recreated anyway.
        if (isInMultiWindowMode() || isInPictureInPictureMode()) {
            return;
        }
        // Don't start Maps when the display is off for ActivityVisibilityTests.
        if (getDisplay().getState() != Display.STATE_ON) {
            return;
        }
        try {
            mActivityView.startActivity(getMapsIntent());
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Maps activity not found", e);
        }
    }

    private Intent getMapsIntent() {
        return Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initializeFragments();
    }

    private void initializeFragments() {
        PlaybackFragment playbackFragment = new PlaybackFragment();
        ContextualFragment contextualFragment = null;
        FrameLayout contextual = findViewById(R.id.contextual);
        if (contextual != null) {
            contextualFragment = new ContextualFragment();
        }

        FragmentTransaction fragmentTransaction =
                getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.playback, playbackFragment);
        if (contextual != null) {
            fragmentTransaction.replace(R.id.contextual, contextualFragment);
        }
        fragmentTransaction.commitNow();
    }

    /** Logs that the Activity is ready. Used for startup time diagnostics. */
    private void maybeLogReady() {
        if (DEBUG) {
            Log.d(TAG, "maybeLogReady(" + getUserId() + "): activityReady=" + mActivityViewReady
                    + ", started=" + mIsStarted + ", alreadyLogged: " + mIsReadyLogged);
        }
        if (mActivityViewReady && mIsStarted) {
            // We should report everytime - the Android framework will take care of logging just
            // when it's effectivelly drawn for the first time, but....
            reportFullyDrawn();
            if (!mIsReadyLogged) {
                // ... we want to manually check that the Log.i below (which is useful to show
                // the user id) is only logged once (otherwise it would be logged everytime the user
                // taps Home)
                Log.i(TAG, "Launcher for user " + getUserId() + " is ready");
                mIsReadyLogged = true;
            }
        }
    }
}
