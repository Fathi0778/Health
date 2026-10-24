package com.example.healthmate.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.healthmate.utils.DateUtils;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

/*
 * Opens the calendar. Only today or earlier dates can be picked.
 * After the phone is rotated, reattach() connects the OK button again.
 */
public final class DatePickerHelper {

    public interface OnDatePicked {
        void onDatePicked(@NonNull String isoDate);
    }

    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;

    private DatePickerHelper() {
    }

    public static void show(@NonNull FragmentManager fragmentManager,
                            @NonNull String tag,
                            @Nullable String selectedIso,
                            @StringRes int titleRes,
                            @NonNull OnDatePicked listener) {
        if (fragmentManager.findFragmentByTag(tag) != null) {
            return; // already open (stops a double tap opening two calendars)
        }
        long todayUtc = DateUtils.isoToUtcMillis(DateUtils.today());
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setEnd(todayUtc)
                .setValidator(DateValidatorPointBackward.before(todayUtc + ONE_DAY_MS - 1))
                .build();

        long selection = selectedIso != null && DateUtils.isValidIso(selectedIso)
                ? DateUtils.isoToUtcMillis(selectedIso)
                : todayUtc;

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(titleRes)
                .setSelection(selection)
                .setCalendarConstraints(constraints)
                .build();
        attach(picker, listener);
        picker.show(fragmentManager, tag);
    }

    // call this in onViewCreated, because after rotation the calendar loses its listener
    @SuppressWarnings("unchecked")
    public static void reattach(@NonNull FragmentManager fragmentManager,
                                @NonNull String tag,
                                @NonNull OnDatePicked listener) {
        Fragment existing = fragmentManager.findFragmentByTag(tag);
        if (existing instanceof MaterialDatePicker) {
            attach((MaterialDatePicker<Long>) existing, listener);
        }
    }

    private static void attach(MaterialDatePicker<Long> picker, OnDatePicked listener) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                listener.onDatePicked(DateUtils.utcMillisToIso(selection));
            }
        });
    }
}