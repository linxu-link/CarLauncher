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

import static android.content.Context.ACTIVITY_SERVICE;

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
 * Android Automotive的基本启动器，演示如何使用{@link ActivityView}托管地图内容。
 *
 * <p>注意：在某些设备上，ActivityView的渲染宽度、高度和/或纵横比可能不符合Android兼容性定义。
 * 开发人员应与内容所有者合作，以确保在扩展或模拟此类时正确呈现内容。
 *
 * <p>注意：由于ActivityView中的hosted maps活动当前处于虚拟屏幕，因此系统认为该活动始终位于前面。
 * 以直接意图启动“地图”活动将不起作用。
 * 要在real display上启动“地图”活动，请使用{@link Intent#CATEGORY_APP_MAPS}类别将意图发送给启动器，
 * 启动器将在real display上启动活动。
 *
 * <p>注意：从当前用户切换或切换回当前用户时，ActivityView中虚拟显示的状态是不确定的。
 * 为避免崩溃，此活动将在切换用户时完成。
 */
public class CarLauncher extends FragmentActivity {
    private static final String TAG = "CarLauncher";
    private static final boolean DEBUG = false;

    private CarActivityView mActivityView;
    private boolean mActivityViewReady;
    private boolean mIsStarted;
    private DisplayManager mDisplayManager;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    /**
     * 一旦我们记录活动已完全绘制，则设置为{@code true}。
     */
    private boolean mIsReadyLogged;

    private final ActivityView.StateCallback mActivityViewCallback = new ActivityView.StateCallback() {
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
                Log.d(TAG, "onTaskMovedToFront(" + getUserId() + "): started=" + mIsStarted);
            }
            try {
                if (mIsStarted) {
                    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    am.moveTaskToFront(CarLauncher.this.getTaskId(), /* flags= */ 0);
                }
            } catch (RuntimeException e) {
                Log.w(TAG, "Failed to move CarLauncher to front.");
            }
        }
    };

    private final DisplayListener mDisplayListener = new DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

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
        // 在多窗口模式下不显示“地图”面板。
        // 注意：拆分屏幕的CTS测试与启动器默认活动的活动视图不兼容
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
        // 如果我们碰巧被重新呈现为多显示模式，我们将跳过在“Activity”视图中启动内容，因为我们无论如何都会被重新创建。
        if (isInMultiWindowMode() || isInPictureInPictureMode()) {
            return;
        }
        // 在“活动可见性测试（ActivityVisibilityTests）”的显示关闭时不要启动地图。
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
        // 为应用程序的主Activity创建一个意图，不指定要运行的特定Activity，而是提供一个选择器来查找该Activity。
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

    /**
     * 记录活动已准备就绪。用于启动时间诊断。
     */
    private void maybeLogReady() {
        if (DEBUG) {
            Log.d(TAG, "maybeLogReady(" + getUserId() + "): activityReady=" + mActivityViewReady
                    + ", started=" + mIsStarted + ", alreadyLogged: " + mIsReadyLogged);
        }
        if (mActivityViewReady && mIsStarted) {
            // 我们每次都应该报告-Android框架将在第一次有效绘制日志时处理日志记录，但是。。。。
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
