package com.android.car.carlauncher;

import android.car.CarProjectionManager;
import android.car.projection.ProjectionStatus;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.util.List;

/** A {@link LiveData} of {@link ContextualInfo} on projection status. */
class ProjectionContextualInfoLiveData extends LiveData<ContextualInfo>
        implements CarProjectionManager.ProjectionStatusListener {
    private static final String TAG = "ProjectionContext";

    private final Context mContext;
    private final CarProjectionManager mCarProjectionManager;

    ProjectionContextualInfoLiveData(
            Context context,
            CarProjectionManager carProjectionManager) {
        mContext = context;
        mCarProjectionManager = carProjectionManager;
    }

    @Override
    protected void onActive() {
        super.onActive();
        mCarProjectionManager.registerProjectionStatusListener(this);
    }

    @Override
    protected void onInactive() {
        mCarProjectionManager.unregisterProjectionStatusListener(this);
        super.onInactive();
    }

    @Override
    public void onProjectionStatusChanged(
            int state, @Nullable String packageName, @NonNull List<ProjectionStatus> details) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onProjectionStatusChanged state=" + state + " package=" + packageName);
        }
        if (state == ProjectionStatus.PROJECTION_STATE_INACTIVE || packageName == null) {
            setValue(null);
            return;
        }

        PackageManager pm = mContext.getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = pm.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not load projection package information", e);
            setValue(null);
            return;
        }

        setValue(
                new ContextualInfo(
                        applicationInfo.loadIcon(pm),
                        applicationInfo.loadLabel(pm),
                        getStatusMessage(packageName, details),
                        /* showClock= */ false,
                        pm.getLaunchIntentForPackage(packageName)));
    }

    @Nullable
    private String getStatusMessage(
            String packageName, List<ProjectionStatus> details) {
        for (ProjectionStatus status : details) {
            if (packageName.equals(status.getPackageName())) {
                return getStatusMessage(status);
            }
        }

        return null;
    }

    @Nullable
    private String getStatusMessage(ProjectionStatus status) {
        // The status message is as follows:
        // - If there is an unambiguous "best" device, the name of that device.
        //   "Unambiguous" is defined as only one projecting device, or no projecting devices
        //   and only one non-projecting device.
        // - If there are multiple projecting or non-projecting devices, "N devices", where N
        //   is the total number of projecting and non-projecting devices.
        // - If there are no devices at all, no message. This should not happen if projection
        //   apps are behaving properly, but may happen in the event of a projection app bug.
        String projectingDevice = null;
        String nonProjectingDevice = null;
        int projectingDeviceCount = 0;
        int nonProjectingDeviceCount = 0;
        for (ProjectionStatus.MobileDevice device : status.getConnectedMobileDevices()) {
            if (device.isProjecting()) {
                projectingDevice = device.getName();
                projectingDeviceCount++;
            } else {
                nonProjectingDevice = device.getName();
                nonProjectingDeviceCount++;
            }
        }

        if (projectingDeviceCount == 1) {
            return projectingDevice;
        } else if (projectingDeviceCount == 0 && nonProjectingDeviceCount == 1) {
            return nonProjectingDevice;
        }

        int totalDeviceCount = projectingDeviceCount + nonProjectingDeviceCount;
        if (totalDeviceCount > 0) {
            return mContext.getResources().getQuantityString(
                    R.plurals.projection_devices, totalDeviceCount, totalDeviceCount);
        } else {
            return null;
        }
    }
}
