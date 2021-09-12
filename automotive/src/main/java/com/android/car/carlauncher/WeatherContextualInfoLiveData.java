package com.android.car.carlauncher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserManager;

import androidx.lifecycle.LiveData;

/** A {@link LiveData} that returns placeholder weather {@link ContextualInfo}. */
class WeatherContextualInfoLiveData extends LiveData<ContextualInfo> {
    private final Context mContext;

    WeatherContextualInfoLiveData(Context context) {
        mContext = context;
    }

    @Override
    protected void onActive() {
        super.onActive();
        setValue(
                new ContextualInfo(
                        getWeatherIcon(),
                        getGreeting(),
                        getTemperature(),
                        /* showClock= */ true,
                        /* onClickActivity= */ null));
    }

    private Drawable getWeatherIcon() {
        return mContext.getDrawable(R.drawable.ic_partly_cloudy);
    }

    private CharSequence getGreeting() {
        UserManager userManager = UserManager.get(mContext);
        String userName = userManager.getUserName();

        if (userName != null) {
            return mContext.getString(R.string.greeting, userName);
        } else {
            return "";
        }
    }

    private CharSequence getTemperature() {
        return mContext.getText(R.string.temperature_empty);
    }
}
