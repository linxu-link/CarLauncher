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

import static com.android.car.ui.utils.CarUiUtils.requireViewByRefId;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.car.ui.R;

import java.util.function.Consumer;

/**
 * A preference that has a switch that can be toggled independently of pressing the main
 * body of the preference.
 */
public class CarUiTwoActionSwitchPreference extends CarUiTwoActionBasePreference {
    @Nullable
    protected Consumer<Boolean> mSecondaryActionOnClickListener;
    private boolean mSecondaryActionChecked;

    public CarUiTwoActionSwitchPreference(Context context,
            AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CarUiTwoActionSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CarUiTwoActionSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarUiTwoActionSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected void init(@Nullable AttributeSet attrs) {
        super.init(attrs);

        setLayoutResourceInternal(R.layout.car_ui_preference_two_action_switch);
    }

    @Override
    protected void performSecondaryActionClickInternal() {
        if (isSecondaryActionEnabled()) {
            if (isUxRestricted()) {
                Consumer<Preference> restrictedListener = getOnClickWhileRestrictedListener();
                if (restrictedListener != null) {
                    restrictedListener.accept(this);
                }
            } else {
                mSecondaryActionChecked = !mSecondaryActionChecked;
                notifyChanged();
                if (mSecondaryActionOnClickListener != null) {
                    mSecondaryActionOnClickListener.accept(mSecondaryActionChecked);
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View firstActionContainer = requireViewByRefId(holder.itemView,
                R.id.car_ui_first_action_container);
        View secondActionContainer = requireViewByRefId(holder.itemView,
                R.id.car_ui_second_action_container);
        View secondaryAction = requireViewByRefId(holder.itemView,
                R.id.car_ui_secondary_action);
        Switch s = requireViewByRefId(holder.itemView,
                R.id.car_ui_secondary_action_concrete);

        holder.itemView.setFocusable(false);
        holder.itemView.setClickable(false);
        firstActionContainer.setOnClickListener(this::performClick);
        firstActionContainer.setEnabled(isEnabled() || isUxRestricted());
        firstActionContainer.setFocusable(isEnabled() || isUxRestricted());

        secondActionContainer.setVisibility(mSecondaryActionVisible ? View.VISIBLE : View.GONE);
        s.setChecked(mSecondaryActionChecked);
        s.setEnabled(isSecondaryActionEnabled());

        secondaryAction.setOnClickListener(v -> performSecondaryActionClickInternal());
        secondaryAction.setEnabled(isSecondaryActionEnabled() || isUxRestricted());
        secondaryAction.setFocusable(isSecondaryActionEnabled() || isUxRestricted());
    }

    /**
     * Sets the checked state of the switch in the secondary action space.
     * @param checked Whether the switch should be checked or not.
     */
    public void setSecondaryActionChecked(boolean checked) {
        mSecondaryActionChecked = checked;
        notifyChanged();
    }

    /**
     * Returns the checked state of the switch in the secondary action space.
     * @return Whether the switch is checked or not.
     */
    public boolean isSecondaryActionChecked() {
        return mSecondaryActionChecked;
    }

    /**
     * Sets the on-click listener of the secondary action button.
     *
     * The listener is called with the current checked state of the switch.
     */
    public void setOnSecondaryActionClickListener(@Nullable Consumer<Boolean> onClickListener) {
        mSecondaryActionOnClickListener = onClickListener;
        notifyChanged();
    }
}
