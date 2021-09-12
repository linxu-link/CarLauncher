/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.view.View;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.function.Function;

/**
 * This is a utility class designed to make it easier to create a new {@link DrawableStateView}.
 *
 * To use, subclass a view and forward it's {@link View#onCreateDrawableState(int)} and
 * {@link DrawableStateView#setExtraDrawableState(int[], int[])} methods to this object.
 */
class DrawableStateUtil implements DrawableStateView {

    private final View mView;
    @Nullable
    private int[] mStateToAdd;
    @Nullable
    private int[] mStateToRemove;

    DrawableStateUtil(View view) {
        mView = view;
    }

    /**
     * Forward the View's onCreateDrawableState to this method.
     *
     * @param extraSpace The extraSpace parameter passed to the View's onCreateDrawableState
     * @param callSuper  A reference to the View's super.onCreateDrawableState()
     * @return The intended result of the View's onCreateDrawableState()
     */
    public int[] onCreateDrawableState(int extraSpace, Function<Integer, int[]> callSuper) {
        int[] result;
        if (mStateToAdd == null) {
            result = callSuper.apply(extraSpace);
        } else {
            result = mergeDrawableStates(
                    callSuper.apply(extraSpace + mStateToAdd.length), mStateToAdd);
        }

        if (mStateToRemove != null && mStateToRemove.length != 0) {
            result = Arrays.stream(result)
                    .filter(state -> Arrays.stream(mStateToRemove).noneMatch(
                            toRemove -> state == toRemove))
                    .toArray();
        }

        return result;
    }

    /**
     * Forward your View's setExtraDrawableState here.
     */
    @Override
    public void setExtraDrawableState(@Nullable int[] stateToAdd, @Nullable int[] stateToRemove) {
        mStateToAdd = stateToAdd;
        mStateToRemove = stateToRemove;
        mView.refreshDrawableState();
    }

    /** Copied from {@link View} */
    private static int[] mergeDrawableStates(int[] baseState, int[] additionalState) {
        int i = baseState.length - 1;
        while (i >= 0 && baseState[i] == 0) {
            i--;
        }
        System.arraycopy(additionalState, 0, baseState, i + 1, additionalState.length);
        return baseState;
    }
}
