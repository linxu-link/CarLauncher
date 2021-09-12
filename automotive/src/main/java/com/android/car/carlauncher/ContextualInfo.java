package com.android.car.carlauncher;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

final class ContextualInfo {
    private final Drawable mIcon;
    private final CharSequence mTopLine;
    private final @Nullable CharSequence mBottomLine;
    private final boolean mShowClock;
    private final Intent mOnClickActivity;

    public ContextualInfo(
            Drawable icon,
            CharSequence topLine,
            @Nullable CharSequence bottomLine,
            boolean showClock,
            @Nullable Intent onClickActivity) {
        mIcon = icon;
        mTopLine = topLine;
        mBottomLine = bottomLine;
        mShowClock = showClock;
        mOnClickActivity = onClickActivity;
    }

    /** Gets the icon to be shown in the contextual space. */
    public Drawable getIcon() {
        return mIcon;
    }

    /** Gets the top line of the text to be shown in the contextual space. */
    public CharSequence getTopLine() {
        return mTopLine;
    }

    /**
     * Gets the bottom line of the text to be shown in the contextual space.
     *
     * If null, no bottom-line text will be shown in the contextual space.
     */
    @Nullable
    public CharSequence getBottomLine() {
        return mBottomLine;
    }

    /** Gets whether to show the date in the contextual space. */
    public boolean getShowClock() {
        return mShowClock;
    }

    /**
     * Gets the {@link Intent} for the activity to be started when the contextual space is tapped.
     *
     * If null, the contextual space will not be tappable.
     */
    @Nullable
    public Intent getOnClickActivity() {
        return mOnClickActivity;
    }
}
