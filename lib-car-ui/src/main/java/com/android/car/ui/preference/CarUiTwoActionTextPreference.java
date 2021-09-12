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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.car.ui.R;

import java.util.function.Consumer;

/**
 * A preference that has a text button that can be pressed independently of pressing the main
 * body of the preference.
 */
public class CarUiTwoActionTextPreference extends CarUiTwoActionBasePreference {

    @Nullable
    protected Runnable mSecondaryActionOnClickListener;
    @Nullable
    private CharSequence mSecondaryActionText;

    public CarUiTwoActionTextPreference(Context context,
            AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CarUiTwoActionTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CarUiTwoActionTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarUiTwoActionTextPreference(Context context) {
        super(context);
    }

    @Override
    protected void init(@Nullable AttributeSet attrs) {
        super.init(attrs);

        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.CarUiTwoActionTextPreference);
        int actionStyle = 0;
        try {
            actionStyle = a.getInteger(
                    R.styleable.CarUiTwoActionTextPreference_secondaryActionStyle, 0);
            mSecondaryActionText = a.getString(
                    R.styleable.CarUiTwoActionTextPreference_secondaryActionText);
        } finally {
            a.recycle();
        }

        setLayoutResourceInternal(actionStyle == 0
                ? R.layout.car_ui_preference_two_action_text
                : R.layout.car_ui_preference_two_action_text_borderless);
    }

    @Override
    protected void performSecondaryActionClickInternal() {
        if (isSecondaryActionEnabled()) {
            if (isUxRestricted()) {
                Consumer<Preference> restrictedListener = getOnClickWhileRestrictedListener();
                if (restrictedListener != null) {
                    restrictedListener.accept(this);
                }
            } else if (mSecondaryActionOnClickListener != null) {
                mSecondaryActionOnClickListener.run();
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
        Button secondaryButton = requireViewByRefId(holder.itemView,
                R.id.car_ui_secondary_action);

        holder.itemView.setFocusable(false);
        holder.itemView.setClickable(false);
        firstActionContainer.setOnClickListener(this::performClick);
        firstActionContainer.setEnabled(isEnabled());
        firstActionContainer.setFocusable(isEnabled());

        secondActionContainer.setVisibility(mSecondaryActionVisible ? View.VISIBLE : View.GONE);
        secondaryButton.setText(mSecondaryActionText);
        secondaryButton.setOnClickListener(v -> performSecondaryActionClickInternal());
        secondaryButton.setEnabled(isSecondaryActionEnabled());
        secondaryButton.setFocusable(isSecondaryActionEnabled());
    }

    @Nullable
    public CharSequence getSecondaryActionText() {
        return mSecondaryActionText;
    }

    /**
     * Sets the title of the secondary action button.
     *
     * @param title The text to display on the secondary action.
     */
    public void setSecondaryActionText(@Nullable CharSequence title) {
        mSecondaryActionText = title;
        notifyChanged();
    }

    /**
     * Sets the title of the secondary action button.
     *
     * @param resid A string resource of the text to display on the secondary action.
     */
    public void setSecondaryActionText(@StringRes int resid) {
        setSecondaryActionText(getContext().getString(resid));
    }

    /**
     * Sets the on-click listener of the secondary action button.
     */
    public void setOnSecondaryActionClickListener(@Nullable Runnable onClickListener) {
        mSecondaryActionOnClickListener = onClickListener;
        notifyChanged();
    }
}
