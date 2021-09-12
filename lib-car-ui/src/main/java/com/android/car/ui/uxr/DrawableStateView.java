/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.car.ui.uxr;

import androidx.annotation.Nullable;

/**
 * An Interface to manipulate a view's drawable state.
 *
 * <p>Used by {@link com.android.car.ui.toolbar.MenuItem MenuItems} to make the views display
 * if they are ux restricted.
 */
public interface DrawableStateView {
    /**
     * Sets the drawable state. This should merge with existing drawable states
     *
     * @param stateToAdd An array of drawable states to add to the view's drawable state, along
     *                   with any drawable state that would normally be there.
     * @param stateToRemove An array of drawable states to remove from what would normally be
     *                      used to display the view.
     */
    void setExtraDrawableState(@Nullable int[] stateToAdd, @Nullable int[] stateToRemove);
}
