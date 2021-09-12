/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.ui.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;

import com.android.car.ui.R;

/**
 * A base class for several types of preferences, that all have a main click action along
 * with a secondary action.
 */
public abstract class CarUiTwoActionBasePreference extends CarUiPreference {

    protected boolean mSecondaryActionEnabled = true;
    protected boolean mSecondaryActionVisible = true;

    public CarUiTwoActionBasePreference(Context context,
            AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public CarUiTwoActionBasePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public CarUiTwoActionBasePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CarUiTwoActionBasePreference(Context context) {
        super(context);
        init(null);
    }

    @CallSuper
    protected void init(@Nullable AttributeSet attrs) {
        setShowChevron(false);

        TypedArray a = getContext()
                .obtainStyledAttributes(attrs, R.styleable.CarUiTwoActionBasePreference);
        try {
            disallowResourceIds(a,
                    R.styleable.CarUiTwoActionBasePreference_layout,
                    R.styleable.CarUiTwoActionBasePreference_android_layout,
                    R.styleable.CarUiTwoActionBasePreference_widgetLayout,
                    R.styleable.CarUiTwoActionBasePreference_android_widgetLayout);
        } finally {
            a.recycle();
        }

        a = getContext().obtainStyledAttributes(attrs,
                R.styleable.CarUiTwoActionPreference);

        try {
            mSecondaryActionVisible = a.getBoolean(
                    R.styleable.CarUiTwoActionPreference_actionShown, true);
            mSecondaryActionEnabled = a.getBoolean(
                    R.styleable.CarUiTwoActionPreference_actionEnabled, true);
        } finally {
            a.recycle();
        }
    }

    /**
     * Returns whether or not the secondary action is enabled.
     */
    public boolean isSecondaryActionEnabled() {
        return mSecondaryActionEnabled && isEnabled();
    }

    /**
     * Sets whether or not the secondary action is enabled. This is secondary to the overall
     * {@link #setEnabled(boolean)} of the preference
     */
    public void setSecondaryActionEnabled(boolean enabled) {
        mSecondaryActionEnabled = enabled;
        notifyChanged();
    }

    /**
     * Returns whether or not the secondary action is visible.
     */
    public boolean isSecondaryActionVisible() {
        return mSecondaryActionVisible;
    }

    /**
     * Sets whether or not the secondary action is visible.
     */
    public void setSecondaryActionVisible(boolean visible) {
        mSecondaryActionVisible = visible;
        notifyChanged();
    }

    /**
     * Like {@link #onClick()}, but for the secondary action.
     */
    public void performSecondaryActionClick() {
        if (mSecondaryActionEnabled && mSecondaryActionVisible) {
            performSecondaryActionClickInternal();
        }
    }

    protected abstract void performSecondaryActionClickInternal();

    protected void setLayoutResourceInternal(@LayoutRes int layoutResId) {
        super.setLayoutResource(layoutResId);
    }

    @Override
    public void setLayoutResource(@LayoutRes int layoutResId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWidgetLayoutResource(@LayoutRes int widgetLayoutResId) {
        throw new UnsupportedOperationException();
    }

    private static void disallowResourceIds(@NonNull TypedArray a, @StyleableRes int ...indices) {
        for (int index : indices) {
            if (a.hasValue(index)) {
                throw new AssertionError("Setting this attribute is not allowed: "
                        + a.getResources().getResourceName(index));
            }
        }
    }
}
