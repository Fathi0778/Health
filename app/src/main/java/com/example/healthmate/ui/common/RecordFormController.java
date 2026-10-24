package com.example.healthmate.ui.common;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.healthmate.R;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.utils.DateUtils;
import com.example.healthmate.utils.ValidationUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/*
 * Controls the form in view_record_form.xml.
 * The Add screen and the Edit screen both use this class,
 * so both screens check the inputs in exactly the same way.
 */
public class RecordFormController {

    // tells the screen when the user picks another date
    public interface OnDateChangedListener {
        void onDateChanged(@NonNull String isoDate);
    }

    private static final String DATE_PICKER_TAG = "record_form_date_picker";

    private final Fragment fragment;
    private final Context context;

    private final TextInputLayout dateLayout;
    private final TextInputLayout stepsLayout;
    private final TextInputLayout caloriesLayout;
    private final TextInputLayout waterLayout;
    private final TextInputLayout sleepHoursLayout;
    private final TextInputLayout sleepMinutesLayout;

    private final TextInputEditText dateInput;
    private final TextInputEditText stepsInput;
    private final TextInputEditText caloriesInput;
    private final TextInputEditText waterInput;
    private final TextInputEditText sleepHoursInput;
    private final TextInputEditText sleepMinutesInput;

    @Nullable
    private String selectedDate;
    @Nullable
    private OnDateChangedListener dateChangedListener;

    public RecordFormController(@NonNull Fragment fragment, @NonNull View formRoot) {
        this.fragment = fragment;
        this.context = formRoot.getContext();

        dateLayout = formRoot.findViewById(R.id.input_layout_date);
        stepsLayout = formRoot.findViewById(R.id.input_layout_steps);
        caloriesLayout = formRoot.findViewById(R.id.input_layout_calories);
        waterLayout = formRoot.findViewById(R.id.input_layout_water);
        sleepHoursLayout = formRoot.findViewById(R.id.input_layout_sleep_hours);
        sleepMinutesLayout = formRoot.findViewById(R.id.input_layout_sleep_minutes);

        dateInput = formRoot.findViewById(R.id.input_date);
        stepsInput = formRoot.findViewById(R.id.input_steps);
        caloriesInput = formRoot.findViewById(R.id.input_calories);
        waterInput = formRoot.findViewById(R.id.input_water);
        sleepHoursInput = formRoot.findViewById(R.id.input_sleep_hours);
        sleepMinutesInput = formRoot.findViewById(R.id.input_sleep_minutes);

        // tap on the date box or its icons -> open the calendar
        View.OnClickListener openPicker = v -> openDatePicker();
        dateInput.setOnClickListener(openPicker);
        dateLayout.setStartIconOnClickListener(openPicker);
        dateLayout.setEndIconOnClickListener(openPicker);

        // remove the red error when the user starts typing again
        clearErrorOnEdit(stepsInput, stepsLayout);
        clearErrorOnEdit(caloriesInput, caloriesLayout);
        clearErrorOnEdit(waterInput, waterLayout);
        clearErrorOnEdit(sleepHoursInput, sleepHoursLayout);
        clearErrorOnEdit(sleepMinutesInput, sleepMinutesLayout);

        // after rotating the phone, connect the open calendar again
        DatePickerHelper.reattach(fragment.getChildFragmentManager(), DATE_PICKER_TAG, this::setDate);
    }

    public void setOnDateChangedListener(@Nullable OnDateChangedListener listener) {
        this.dateChangedListener = listener;
    }

    // ---------------- Date ----------------

    private void openDatePicker() {
        DatePickerHelper.show(fragment.getChildFragmentManager(), DATE_PICKER_TAG,
                selectedDate, R.string.form_pick_date, this::setDate);
    }

    public void setDate(@Nullable String isoDate) {
        selectedDate = isoDate;
        dateInput.setText(isoDate == null ? "" : DateUtils.formatLong(isoDate));
        dateLayout.setError(null);
        if (isoDate != null && dateChangedListener != null) {
            dateChangedListener.onDateChanged(isoDate);
        }
    }

    @Nullable
    public String getSelectedDate() {
        return selectedDate;
    }

    public void showDateError(@Nullable String message) {
        dateLayout.setError(message);
    }

    // ---------------- Fill / clear ----------------

    // put a saved record into the form (Edit screen)
    public void fill(@NonNull HealthRecord record) {
        setDate(record.getDate());
        stepsInput.setText(String.valueOf(record.getSteps()));
        caloriesInput.setText(String.valueOf(record.getCalories()));
        waterInput.setText(String.valueOf(record.getWater()));
        sleepHoursInput.setText(String.valueOf(record.getSleepMinutes() / 60));
        sleepMinutesInput.setText(String.valueOf(record.getSleepMinutes() % 60));
        clearErrors();
    }

    public void clear(@Nullable String defaultDate) {
        stepsInput.setText(null);
        caloriesInput.setText(null);
        waterInput.setText(null);
        sleepHoursInput.setText(null);
        sleepMinutesInput.setText(null);
        clearErrors();
        setDate(defaultDate);
    }

    private void clearErrors() {
        dateLayout.setError(null);
        stepsLayout.setError(null);
        caloriesLayout.setError(null);
        waterLayout.setError(null);
        sleepHoursLayout.setError(null);
        sleepMinutesLayout.setError(null);
    }

    // ---------------- Validation ----------------

    /*
     * Checks every box. Shows a red message under each wrong box.
     * If everything is ok it returns a new HealthRecord, otherwise null.
     * (The id is not set here. The Edit screen sets it.)
     */
    @Nullable
    public HealthRecord validateAndBuild() {
        boolean valid = true;
        View firstWrongBox = null;

        ValidationUtils.Error dateError = ValidationUtils.validateDate(selectedDate, DateUtils.today());
        dateLayout.setError(ValidationMessages.forDate(context, dateError));
        if (dateError != ValidationUtils.Error.NONE) {
            valid = false;
            firstWrongBox = dateInput;
        }

        ValidationUtils.NumberResult steps = ValidationUtils.parseWholeNumber(
                text(stepsInput), ValidationUtils.MAX_STEPS);
        if (!showNumberError(stepsLayout, steps, 0, ValidationUtils.MAX_STEPS)) {
            valid = false;
            firstWrongBox = firstWrongBox == null ? stepsInput : firstWrongBox;
        }

        ValidationUtils.NumberResult calories = ValidationUtils.parseWholeNumber(
                text(caloriesInput), ValidationUtils.MAX_CALORIES);
        if (!showNumberError(caloriesLayout, calories, 0, ValidationUtils.MAX_CALORIES)) {
            valid = false;
            firstWrongBox = firstWrongBox == null ? caloriesInput : firstWrongBox;
        }

        ValidationUtils.NumberResult water = ValidationUtils.parseWholeNumber(
                text(waterInput), ValidationUtils.MAX_WATER_ML);
        if (!showNumberError(waterLayout, water, 0, ValidationUtils.MAX_WATER_ML)) {
            valid = false;
            firstWrongBox = firstWrongBox == null ? waterInput : firstWrongBox;
        }

        // sleep = hours box + minutes box
        ValidationUtils.NumberResult sleepHours =
                ValidationUtils.parseSleepHoursPart(text(sleepHoursInput));
        boolean hoursOk = showNumberError(sleepHoursLayout, sleepHours, 0, ValidationUtils.MAX_SLEEP_HOURS);
        if (!hoursOk) {
            valid = false;
            firstWrongBox = firstWrongBox == null ? sleepHoursInput : firstWrongBox;
        }

        ValidationUtils.NumberResult sleepMinutes =
                ValidationUtils.parseSleepMinutesPart(text(sleepMinutesInput));
        boolean minutesOk = showNumberError(sleepMinutesLayout, sleepMinutes, 0,
                ValidationUtils.MAX_SLEEP_MINUTES_PART);
        if (!minutesOk) {
            valid = false;
            firstWrongBox = firstWrongBox == null ? sleepMinutesInput : firstWrongBox;
        }

        // both boxes are fine on their own, but 24h 30m is still too much
        if (hoursOk && minutesOk && ValidationUtils.validateSleepTotal(
                sleepHours.intValue(), sleepMinutes.intValue()) != ValidationUtils.Error.NONE) {
            sleepHoursLayout.setError(context.getString(R.string.error_sleep_over_24));
            valid = false;
            firstWrongBox = firstWrongBox == null ? sleepHoursInput : firstWrongBox;
        }

        if (!valid) {
            if (firstWrongBox != null && firstWrongBox != dateInput) {
                firstWrongBox.requestFocus();
            }
            return null;
        }
        //noinspection ConstantConditions - date is not null here because it passed the check
        return new HealthRecord(selectedDate, steps.intValue(), calories.intValue(),
                water.intValue(), sleepHours.intValue(), sleepMinutes.intValue());
    }

    private boolean showNumberError(TextInputLayout layout, ValidationUtils.NumberResult result,
                                    double min, double max) {
        layout.setError(ValidationMessages.forNumber(context, result.error, min, max, false));
        return result.isValid();
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private static void clearErrorOnEdit(TextInputEditText input, TextInputLayout layout) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) {
                    layout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
