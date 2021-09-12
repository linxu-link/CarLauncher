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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.car.ui.R;

import java.util.function.Consumer;

/**
 * A preference that has an icon button that can be pressed independently of pressing the main
 * body of the preference.
 */
public class CarUiTwoActionIconPreference extends CarUiTwoActionBasePreference {
    @Nullable
    protected Runnable mSecondaryActionOnClickListener;
    @Nullable
    private Drawable mSecondaryActionIcon;

    public CarUiTwoActionIconPreference(Context context,
            AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CarUiTwoActionIconPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CarUiTwoActionIconPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarUiTwoActionIconPreference(Context context) {
        super(context);
    }

    @Override
    protected void init(@Nullable AttributeSet attrs) {
        super.init(attrs);

        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.CarUiTwoActionIconPreference);
        try {
            mSecondaryActionIcon = a.getDrawable(
                    R.styleable.CarUiTwoActionIconPreference_secondaryActionIcon);
        } finally {
            a.recycle();
        }

        setLayoutResourceInternal(R.layout.car_ui_preference_two_action_icon);
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
        ViewGroup secondaryButton = requireViewByRefId(holder.itemView,
                R.id.car_ui_secondary_action);
        ImageView iconView = requireViewByRefId(holder.itemView,
                R.id.car_ui_secondary_action_concrete);

        holder.itemView.setFocusable(false);
        holder.itemView.setClickable(false);

        firstActionContainer.setOnClickListener(this::performClick);
        firstActionContainer.setEnabled(isEnabled());
        firstActionContainer.setFocusable(isEnabled());

        secondActionContainer.setVisibility(mSecondaryActionVisible ? View.VISIBLE : View.GONE);
        iconView.setImageDrawable(mSecondaryActionIcon);
        iconView.setEnabled(isSecondaryActionEnabled());
        secondaryButton.setEnabled(isSecondaryActionEnabled());
        secondaryButton.setFocusable(isSecondaryActionEnabled());
        secondaryButton.setOnClickListener(v -> performSecondaryActionClickInternal());
    }

    /**
     * Sets the icon of the secondary action.
     *
     * The icon will be tinted to the primary text color, and resized to fit the space.
     *
     * @param drawable A {@link Drawable} to set as the icon.
     */
    public void setSecondaryActionIcon(@Nullable Drawable drawable) {
        mSecondaryActionIcon = drawable;
        notifyChanged();
    }

    /**
     * Sets the icon of the secondary action.
     *
     * The icon will be tinted to the primary text color, and resized to fit the space.
     *
     * @param resid A drawable resource id to set as the icon.
     */
    public void setSecondaryActionIcon(@DrawableRes int resid) {
        setSecondaryActionIcon(ContextCompat.getDrawable(getContext(), resid));
    }

    /**
     * Sets the on-click listener of the secondary action button.
     */
    public void setOnSecondaryActionClickListener(@Nullable Runnable onClickListener) {
        mSecondaryActionOnClickListener = onClickListener;
        notifyChanged();
    }
}
