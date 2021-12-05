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

import static com.android.car.carlauncher.AppLauncherUtils.APP_TYPE_LAUNCHABLES;
import static com.android.car.carlauncher.AppLauncherUtils.APP_TYPE_MEDIA_SERVICES;

import android.app.Activity;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.content.pm.CarPackageManager;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.car.media.CarMediaManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.carlauncher.AppLauncherUtils.LauncherAppsInfo;
import com.android.car.ui.FocusArea;
import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.Toolbar;
import com.android.car.ui.toolbar.ToolbarController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Launcher activity that shows a grid of apps.
 */
public final class AppGridActivity extends Activity implements InsetsChangedListener {
    private static final String TAG = "AppGridActivity";
    private static final String MODE_INTENT_EXTRA = "com.android.car.carlauncher.mode";

    private int mColumnNumber;
    private boolean mShowAllApps = true;
    private final Set<String> mHiddenApps = new HashSet<>();
    private final Set<String> mCustomMediaComponents = new HashSet<>();
    private AppGridAdapter mGridAdapter;
    private PackageManager mPackageManager;
    private UsageStatsManager mUsageStatsManager;
    private AppInstallUninstallReceiver mInstallUninstallReceiver;
    private Car mCar;
    private CarUxRestrictionsManager mCarUxRestrictionsManager;
    private CarPackageManager mCarPackageManager;
    private CarMediaManager mCarMediaManager;
    private Mode mMode;

    private enum Mode {
        ALL_APPS(R.string.app_launcher_title_all_apps,
                APP_TYPE_LAUNCHABLES + APP_TYPE_MEDIA_SERVICES,
                true),
        MEDIA_ONLY(R.string.app_launcher_title_media_only,
                APP_TYPE_MEDIA_SERVICES,
                true),
        MEDIA_POPUP(R.string.app_launcher_title_media_only,
                APP_TYPE_MEDIA_SERVICES,
                false),
        ;
        public final @StringRes int mTitleStringId;
        public final @AppLauncherUtils.AppTypes int mAppTypes;
        public final boolean mOpenMediaCenter;

        Mode(@StringRes int titleStringId, @AppLauncherUtils.AppTypes int appTypes,
                boolean openMediaCenter) {
            mTitleStringId = titleStringId;
            mAppTypes = appTypes;
            mOpenMediaCenter = openMediaCenter;
        }
    }

    private ServiceConnection mCarConnectionListener = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mCarUxRestrictionsManager = (CarUxRestrictionsManager) mCar.getCarManager(
                        Car.CAR_UX_RESTRICTION_SERVICE);
                mGridAdapter.setIsDistractionOptimizationRequired(
                        mCarUxRestrictionsManager
                                .getCurrentCarUxRestrictions()
                                .isRequiresDistractionOptimization());
                mCarUxRestrictionsManager.registerListener(
                        restrictionInfo ->
                                mGridAdapter.setIsDistractionOptimizationRequired(
                                        restrictionInfo.isRequiresDistractionOptimization()));

                mCarPackageManager = (CarPackageManager) mCar.getCarManager(Car.PACKAGE_SERVICE);
                mCarMediaManager = (CarMediaManager) mCar.getCarManager(Car.CAR_MEDIA_SERVICE);
                updateAppsLists();
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in CarConnectionListener", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCarUxRestrictionsManager = null;
            mCarPackageManager = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mColumnNumber = getResources().getInteger(R.integer.car_app_selector_column_number);
        mPackageManager = getPackageManager();
        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        mCar = Car.createCar(this, mCarConnectionListener);
        mHiddenApps.addAll(Arrays.asList(getResources().getStringArray(R.array.hidden_apps)));
        mCustomMediaComponents.addAll(
                Arrays.asList(getResources().getStringArray(R.array.custom_media_packages)));

        setContentView(R.layout.app_grid_activity);

        updateMode();

        ToolbarController toolbar = CarUi.requireToolbar(this);
        toolbar.setNavButtonMode(Toolbar.NavButtonMode.CLOSE);
        toolbar.setState(Toolbar.State.SUBPAGE);

        if (Build.IS_DEBUGGABLE) {
            toolbar.setMenuItems(Collections.singletonList(MenuItem.builder(this)
                    .setDisplayBehavior(MenuItem.DisplayBehavior.NEVER)
                    .setTitle(R.string.hide_debug_apps)
                    .setOnClickListener(i -> {
                        mShowAllApps = !mShowAllApps;
                        i.setTitle(mShowAllApps
                                ? R.string.hide_debug_apps
                                : R.string.show_debug_apps);
                        updateAppsLists();
                    })
                    .build()));
        }

        mGridAdapter = new AppGridAdapter(this);
        RecyclerView gridView = requireViewById(R.id.apps_grid);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, mColumnNumber);
        gridLayoutManager.setSpanSizeLookup(new SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mGridAdapter.getSpanSizeLookup(position);
            }
        });
        gridView.setLayoutManager(gridLayoutManager);
        gridView.setAdapter(mGridAdapter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        updateMode();
    }

    @Override
    protected void onDestroy() {
        if (mCar != null && mCar.isConnected()) {
            mCar.disconnect();
            mCar = null;
        }
        super.onDestroy();
    }

    private void updateMode() {
        mMode = parseMode(getIntent());
        setTitle(mMode.mTitleStringId);
        CarUi.requireToolbar(this).setTitle(mMode.mTitleStringId);
    }

    /**
     * Note: This activity is exported, meaning that it might receive intents from any source.
     * Intent data parsing must be extra careful.
     */
    @NonNull
    private Mode parseMode(@Nullable Intent intent) {
        String mode = intent != null ? intent.getStringExtra(MODE_INTENT_EXTRA) : null;
        try {
            return mode != null ? Mode.valueOf(mode) : Mode.ALL_APPS;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Received invalid mode: " + mode, e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Using onResume() to refresh most recently used apps because we want to refresh even if
        // the app being launched crashes/doesn't cover the entire screen.
        updateAppsLists();
    }

    /** 更新所有应用的列表以及最近使用的应用的列表 */
    private void updateAppsLists() {
        Set<String> blackList = mShowAllApps ? Collections.emptySet() : mHiddenApps;
        LauncherAppsInfo appsInfo = AppLauncherUtils.getLauncherApps(blackList,
                mCustomMediaComponents,
                mMode.mAppTypes,
                mMode.mOpenMediaCenter,
                getSystemService(LauncherApps.class),
                mCarPackageManager,
                mPackageManager,
                mCarMediaManager);
        mGridAdapter.setAllApps(appsInfo.getLaunchableComponentsList());
        mGridAdapter.setMostRecentApps(getMostRecentApps(appsInfo));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // register broadcast receiver for package installation and uninstallation
        mInstallUninstallReceiver = new AppInstallUninstallReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(mInstallUninstallReceiver, filter);

        // Connect to car service
        mCar.connect();
    }

    @Override
    protected void onStop() {
        super.onPause();
        // disconnect from app install/uninstall receiver
        if (mInstallUninstallReceiver != null) {
            unregisterReceiver(mInstallUninstallReceiver);
            mInstallUninstallReceiver = null;
        }
        // disconnect from car listeners
        try {
            if (mCarUxRestrictionsManager != null) {
                mCarUxRestrictionsManager.unregisterListener();
            }
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Error unregistering listeners", e);
        }
        if (mCar != null) {
            mCar.disconnect();
        }
    }

    /**
     * 请注意，为了从上一次boot中获得使用情况统计数据，设备必须经过干净的关闭过程。
     */
    private List<AppMetaData> getMostRecentApps(LauncherAppsInfo appsInfo) {
        ArrayList<AppMetaData> apps = new ArrayList<>();
        if (appsInfo.isEmpty()) {
            return apps;
        }

        // 获取从1年前开始的使用情况统计数据，返回如下条目：
        // "During 2017 App A is last used at 2017/12/15 18:03"
        // "During 2017 App B is last used at 2017/6/15 10:00"
        // "During 2018 App A is last used at 2018/1/1 15:12"
        List<UsageStats> stats =
                mUsageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_YEARLY,
                        System.currentTimeMillis() - DateUtils.YEAR_IN_MILLIS,
                        System.currentTimeMillis());

        if (stats == null || stats.size() == 0) {
            return apps; // empty list
        }

        stats.sort(new LastTimeUsedComparator());

        int currentIndex = 0;
        int itemsAdded = 0;
        int statsSize = stats.size();
        int itemCount = Math.min(mColumnNumber, statsSize);
        while (itemsAdded < itemCount && currentIndex < statsSize) {
            UsageStats usageStats = stats.get(currentIndex);
            String packageName = usageStats.mPackageName;
            currentIndex++;

            // 不包括自己
            if (packageName.equals(getPackageName())) {
                continue;
            }

            // TODO(b/136222320): 每个包都可以获得UsageStats，但一个包可能包含多个媒体服务。我们需要找到一种方法来获取每个服务的使用率统计数据。
            ComponentName componentName = AppLauncherUtils.getMediaSource(mPackageManager,
                    packageName);
            // 免除媒体服务的后台和启动器检查
            if (!appsInfo.isMediaService(componentName)) {
                // 不要包括仅在后台运行的应用程序
                if (usageStats.getTotalTimeInForeground() == 0) {
                    continue;
                }
                // 不要包含不支持从启动器启动的应用程序
                Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (intent == null || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                    continue;
                }
            }

            AppMetaData app = appsInfo.getAppMetaData(componentName);
            // 防止重复条目
            // e.g. app is used at 2017/12/31 23:59, and 2018/01/01 00:00
            if (app != null && !apps.contains(app)) {
                apps.add(app);
                itemsAdded++;
            }
        }
        return apps;
    }

    @Override
    public void onCarUiInsetsChanged(Insets insets) {
        requireViewById(R.id.apps_grid)
                .setPadding(0, insets.getTop(), 0, insets.getBottom());
        FocusArea focusArea = requireViewById(R.id.focus_area);
        focusArea.setHighlightPadding(0, insets.getTop(), 0, insets.getBottom());
        focusArea.setBoundsOffset(0, insets.getTop(), 0, insets.getBottom());

        requireViewById(android.R.id.content)
                .setPadding(insets.getLeft(), 0, insets.getRight(), 0);
    }

    /**
     * Comparator for {@link UsageStats} that sorts the list by the "last time used" property
     * in descending order.
     */
    private static class LastTimeUsedComparator implements Comparator<UsageStats> {
        @Override
        public int compare(UsageStats stat1, UsageStats stat2) {
            Long time1 = stat1.getLastTimeUsed();
            Long time2 = stat2.getLastTimeUsed();
            return time2.compareTo(time1);
        }
    }

    private class AppInstallUninstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getData().getSchemeSpecificPart();

            if (TextUtils.isEmpty(packageName)) {
                Log.e(TAG, "System sent an empty app install/uninstall broadcast");
                return;
            }

            updateAppsLists();
        }
    }
}
