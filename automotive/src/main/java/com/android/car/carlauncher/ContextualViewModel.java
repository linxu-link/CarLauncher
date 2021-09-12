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

import android.app.Application;
import android.car.Car;
import android.car.CarProjectionManager;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implementation {@link ViewModel} for {@link ContextualFragment}.
 *
 * Returns the first non-null {@link ContextualInfo} from a set of delegates.
 */
public class ContextualViewModel extends AndroidViewModel {
    private final MediatorLiveData<ContextualInfo> mContextualInfo = new MediatorLiveData<>();

    private final List<LiveData<ContextualInfo>> mInfoDelegates;

    private Car mCar;

    public ContextualViewModel(Application application) {
        this(application, null);
    }

    @VisibleForTesting
    ContextualViewModel(Application application, CarProjectionManager carProjectionManager) {
        super(application);

        if (carProjectionManager == null) {
            mCar = Car.createCar(application);
            carProjectionManager =
                    (CarProjectionManager) mCar.getCarManager(Car.PROJECTION_SERVICE);
        }

        mInfoDelegates =
                Collections.unmodifiableList(Arrays.asList(
                        new ProjectionContextualInfoLiveData(application, carProjectionManager),
                        new WeatherContextualInfoLiveData(application)
                ));

        Observer<Object> observer = x -> updateLiveData();
        for (LiveData<ContextualInfo> delegate : mInfoDelegates) {
            mContextualInfo.addSource(delegate, observer);
        }
    }

    @Override
    protected void onCleared() {
        if (mCar != null && mCar.isConnected()) {
            mCar.disconnect();
            mCar = null;
        }
        super.onCleared();
    }

    private void updateLiveData() {
        for (LiveData<ContextualInfo> delegate : mInfoDelegates) {
            ContextualInfo value = delegate.getValue();
            if (value != null) {
                mContextualInfo.setValue(value);
                return;
            }
        }

        mContextualInfo.setValue(null);
    }

    public LiveData<ContextualInfo> getContextualInfo() {
        return mContextualInfo;
    }
}
