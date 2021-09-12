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

import androidx.annotation.Nullable;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

/**
 * App item view holder that contains the app icon and name.
 */
public class AppItemViewHolder extends RecyclerView.ViewHolder {
    private final Context mContext;
    private View mAppItem;
    private ImageView mAppIconView;
    private TextView mAppNameView;

    AppItemViewHolder(View view, Context context) {
        super(view);
        mContext = context;
        mAppItem = view.findViewById(R.id.app_item);
        mAppIconView = mAppItem.findViewById(R.id.app_icon);
        mAppNameView = mAppItem.findViewById(R.id.app_name);
    }

    /**
     * Binds the grid app item view with the app meta data.
     *
     * @param app Pass {@code null} will empty out the view.
     */
    public void bind(@Nullable AppMetaData app, boolean isDistractionOptimizationRequired) {
        // Empty out the view
        mAppIconView.setImageDrawable(null);
        mAppNameView.setText(null);

        if (app == null) {
            return;
        }

        mAppNameView.setText(app.getDisplayName());
        mAppIconView.setImageDrawable(app.getIcon());
        boolean isLaunchable =
                !isDistractionOptimizationRequired || app.getIsDistractionOptimized();
        mAppIconView.setAlpha(mContext.getResources().getFloat(
                isLaunchable ? R.dimen.app_icon_opacity : R.dimen.app_icon_opacity_unavailable));

        if (isLaunchable) {
            mAppItem.setOnClickListener(v -> app.getLaunchCallback().accept(mContext));
            boolean hasLongClickCallback = (app.getAlternateLaunchCallback() != null);
            mAppItem.setLongClickable(hasLongClickCallback);
            if (hasLongClickCallback) {
                // Note setOnLongClickListener implicitly sets view to be long clickable
                mAppItem.setOnLongClickListener(v -> {
                    app.getAlternateLaunchCallback().accept(mContext);
                    return true;
                });
            }
        } else {
            String warningText = mContext.getResources()
                    .getString(R.string.driving_toast_text, app.getDisplayName());
            mAppItem.setOnClickListener(
                    v -> Toast.makeText(mContext, warningText, Toast.LENGTH_LONG).show());
        }
    }
}
