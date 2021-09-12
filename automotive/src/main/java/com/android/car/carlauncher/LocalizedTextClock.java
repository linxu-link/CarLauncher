package com.android.car.carlauncher;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextClock;

import java.util.Locale;

/**
 * {@link TextClock} implementation which expects a date format skeleton for
 * {@link android.R.styleable#TextClock_format12Hour} and
 * {@link android.R.styleable#TextClock_format24Hour} and applies the best format as determined by
 * {@link DateFormat#getBestDateTimePattern(java.util.Locale, String)}.
 */
public class LocalizedTextClock extends TextClock {

    public LocalizedTextClock(Context context) {
        super(context);
    }

    public LocalizedTextClock(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public LocalizedTextClock(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LocalizedTextClock(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setFormat12Hour(DateFormat.getBestDateTimePattern(Locale.getDefault(),
                getFormat12Hour().toString()));
        setFormat24Hour(DateFormat.getBestDateTimePattern(Locale.getDefault(),
                getFormat24Hour().toString()));
    }
}
