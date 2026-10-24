package com.example.healthmate.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.healthmate.R;
import com.example.healthmate.utils.ValidationUtils;

import java.text.NumberFormat;
import java.util.Locale;

/*
 * Turns an Error from ValidationUtils into a message the user can read (from strings.xml).
 */
public final class ValidationMessages {

    private ValidationMessages() {
    }

    @Nullable
    public static String forNumber(@NonNull Context context, @NonNull ValidationUtils.Error error,
                                   double min, double max, boolean isHours) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
        switch (error) {
            case NONE:
                return null;
            case REQUIRED:
                return context.getString(R.string.error_required);
            case NOT_WHOLE_NUMBER:
                return context.getString(R.string.error_whole_number);
            case NEGATIVE:
                return context.getString(R.string.error_negative);
            case TOO_LARGE:
                return context.getString(R.string.error_too_large, format.format(max));
            case TOO_SMALL:
                return context.getString(R.string.error_too_small, format.format(min));
            case OUT_OF_RANGE:
                return context.getString(R.string.error_range, format.format(min), format.format(max));
            case SLEEP_OVER_24H:
                return context.getString(R.string.error_sleep_over_24);
            case INVALID_NUMBER:
            default:
                return context.getString(isHours ? R.string.error_sleep_invalid : R.string.error_whole_number);
        }
    }

    @Nullable
    public static String forDate(@NonNull Context context, @NonNull ValidationUtils.Error error) {
        switch (error) {
            case NONE:
                return null;
            case REQUIRED:
                return context.getString(R.string.error_date_required);
            case FUTURE_DATE:
                return context.getString(R.string.error_date_future);
            case INVALID_DATE:
            default:
                return context.getString(R.string.error_date_invalid);
        }
    }
}