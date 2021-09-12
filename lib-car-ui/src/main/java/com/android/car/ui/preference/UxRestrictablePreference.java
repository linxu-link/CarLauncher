/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import java.util.function.Consumer;

/**
 * An interface for preferences that can be ux restricted.
 *
 * A ux restricted preference will be displayed differently to indicate such, and will
 * display a toast message informing the user they cannot click it when they try to.
 */
public interface UxRestrictablePreference {

    /** Sets this preference as ux restricted or not */
    void setUxRestricted(boolean restricted);

    /** Returns if this preference is currently ux restricted */
    boolean isUxRestricted();

    /** Sets a listener to be called if the preference is clicked while it is ux restricted */
    void setOnClickWhileRestrictedListener(@Nullable Consumer<Preference> listener);

    /** Gets the listener to be called if the preference is clicked while it is ux restricted */
    @Nullable
    Consumer<Preference> getOnClickWhileRestrictedListener();
}
