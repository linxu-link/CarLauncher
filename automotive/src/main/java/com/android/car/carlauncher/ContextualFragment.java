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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

/** {@link Fragment} which displays relevant information that changes over time. */
public class ContextualFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.contextual_fragment, container, false);
        ImageView iconView = rootView.findViewById(R.id.icon);
        TextView topLineView = rootView.findViewById(R.id.top_line);
        TextView bottomLineView = rootView.findViewById(R.id.bottom_line);
        View dateDividerView = rootView.findViewById(R.id.date_divider);
        View dateView = rootView.findViewById(R.id.date);
        View bottomLineContainerView = rootView.findViewById(R.id.bottom_line_container);

        ContextualViewModel viewModel = ViewModelProviders.of(this).get(ContextualViewModel.class);

        viewModel.getContextualInfo().observe(this, info -> {
            if (info == null) {
                return;
            }

            iconView.setImageDrawable(info.getIcon());
            topLineView.setText(info.getTopLine());

            boolean showBottomLineMessage = (info.getBottomLine() != null);

            bottomLineView.setVisibility(showBottomLineMessage ? View.VISIBLE : View.GONE);
            bottomLineView.setText(info.getBottomLine());

            dateView.setVisibility(info.getShowClock() ? View.VISIBLE : View.GONE);

            // If both the bottom-line message and the clock are shown, show the divider.
            dateDividerView.setVisibility(
                    (showBottomLineMessage && info.getShowClock()) ? View.VISIBLE : View.GONE);
            // Hide the bottom-line container if neither the bottom-line message nor the clock
            // is being shown. This will center the top-line message in the card.
            bottomLineContainerView.setVisibility(
                    (showBottomLineMessage || info.getShowClock()) ? View.VISIBLE : View.GONE);

            Intent onClickActivity = info.getOnClickActivity();
            View.OnClickListener listener =
                    onClickActivity != null
                            ? v -> startActivity(info.getOnClickActivity())
                            : null;
            rootView.setOnClickListener(listener);
            rootView.setClickable(listener != null);
        });

        return rootView;
    }
}
