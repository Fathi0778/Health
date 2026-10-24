package com.example.healthmate.ui.settings;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.healthmate.MainActivity;
import com.example.healthmate.R;
import com.example.healthmate.data.prefs.DailyGoals;
import com.example.healthmate.data.prefs.SessionManager;
import com.example.healthmate.ui.auth.LoginActivity;
import com.example.healthmate.ui.common.ValidationMessages;
import com.example.healthmate.ui.onboarding.WelcomeActivity;
import com.example.healthmate.utils.HealthStats;
import com.example.healthmate.utils.ValidationUtils;
import com.example.healthmate.viewmodel.HealthRecordViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;

/*
 * Settings screen - account (log out), my daily goals (saved in SharedPreferences) and About.
 */
public class SettingsFragment extends Fragment {

    private HealthRecordViewModel viewModel;

    private TextInputLayout stepsLayout;
    private TextInputLayout caloriesLayout;
    private TextInputLayout waterLayout;
    private TextInputLayout sleepLayout;
    private TextInputEditText stepsInput;
    private TextInputEditText caloriesInput;
    private TextInputEditText waterInput;
    private TextInputEditText sleepInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HealthRecordViewModel.class);

        stepsLayout = view.findViewById(R.id.input_layout_goal_steps);
        caloriesLayout = view.findViewById(R.id.input_layout_goal_calories);
        waterLayout = view.findViewById(R.id.input_layout_goal_water);
        sleepLayout = view.findViewById(R.id.input_layout_goal_sleep);
        stepsInput = view.findViewById(R.id.input_goal_steps);
        caloriesInput = view.findViewById(R.id.input_goal_calories);
        waterInput = view.findViewById(R.id.input_goal_water);
        sleepInput = view.findViewById(R.id.input_goal_sleep);

        stepsLayout.setHelperText(rangeText(ValidationUtils.MIN_GOAL_STEPS, ValidationUtils.MAX_GOAL_STEPS));
        caloriesLayout.setHelperText(rangeText(ValidationUtils.MIN_GOAL_CALORIES, ValidationUtils.MAX_GOAL_CALORIES));
        waterLayout.setHelperText(rangeText(ValidationUtils.MIN_GOAL_WATER, ValidationUtils.MAX_GOAL_WATER));
        sleepLayout.setHelperText(rangeText((int) ValidationUtils.MIN_GOAL_SLEEP, (int) ValidationUtils.MAX_GOAL_SLEEP));

        // fill the boxes only the first time (after rotation, keep what I typed)
        if (savedInstanceState == null) {
            showGoals(viewModel.getCurrentGoals());
        }

        view.findViewById(R.id.button_save_goals).setOnClickListener(v -> saveGoals());
        view.findViewById(R.id.button_reset_goals).setOnClickListener(v -> {
            viewModel.resetGoals();
            showGoals(viewModel.getCurrentGoals());
            ((MainActivity) requireActivity()).showMessage(R.string.settings_goals_reset);
        });
        ((TextView) view.findViewById(R.id.text_signed_in_as)).setText(getString(
                R.string.settings_signed_in_as, SessionManager.displayName(viewModel.getUsername())));
        view.findViewById(R.id.button_logout).setOnClickListener(v -> confirmLogout());

        view.findViewById(R.id.button_show_welcome).setOnClickListener(v -> {
            viewModel.setOnboardingComplete(false);
            startActivity(new Intent(requireContext(), WelcomeActivity.class));
            requireActivity().finish();
        });

        ((TextView) view.findViewById(R.id.text_version)).setText(versionText());
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_person)
                .setTitle(R.string.settings_logout_title)
                .setMessage(R.string.settings_logout_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_logout, (dialog, which) -> {
                    viewModel.logout();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .show();
    }

    private void showGoals(DailyGoals goals) {
        stepsInput.setText(String.valueOf(goals.getStepsGoal()));
        caloriesInput.setText(String.valueOf(goals.getCaloriesGoal()));
        waterInput.setText(String.valueOf(goals.getWaterGoal()));
        sleepInput.setText(HealthStats.formatHours(goals.getSleepGoal()));
        stepsLayout.setError(null);
        caloriesLayout.setError(null);
        waterLayout.setError(null);
        sleepLayout.setError(null);
    }

    private void saveGoals() {
        ValidationUtils.NumberResult steps = ValidationUtils.parseWholeNumber(text(stepsInput),
                ValidationUtils.MIN_GOAL_STEPS, ValidationUtils.MAX_GOAL_STEPS);
        ValidationUtils.NumberResult calories = ValidationUtils.parseWholeNumber(text(caloriesInput),
                ValidationUtils.MIN_GOAL_CALORIES, ValidationUtils.MAX_GOAL_CALORIES);
        ValidationUtils.NumberResult water = ValidationUtils.parseWholeNumber(text(waterInput),
                ValidationUtils.MIN_GOAL_WATER, ValidationUtils.MAX_GOAL_WATER);
        ValidationUtils.NumberResult sleep = ValidationUtils.parseSleepGoal(text(sleepInput));

        // one "&" on purpose, so every box is checked and all errors show at once
        boolean valid = showError(stepsLayout, steps, ValidationUtils.MIN_GOAL_STEPS, ValidationUtils.MAX_GOAL_STEPS, false)
                & showError(caloriesLayout, calories, ValidationUtils.MIN_GOAL_CALORIES, ValidationUtils.MAX_GOAL_CALORIES, false)
                & showError(waterLayout, water, ValidationUtils.MIN_GOAL_WATER, ValidationUtils.MAX_GOAL_WATER, false)
                & showError(sleepLayout, sleep, ValidationUtils.MIN_GOAL_SLEEP, ValidationUtils.MAX_GOAL_SLEEP, true);

        MainActivity activity = (MainActivity) requireActivity();
        if (!valid) {
            activity.showMessage(R.string.error_fix_fields);
            return;
        }
        viewModel.saveGoals(new DailyGoals(steps.intValue(), water.intValue(), calories.intValue(), sleep.value));
        activity.showMessage(R.string.settings_goals_saved);
    }

    private boolean showError(TextInputLayout layout, ValidationUtils.NumberResult result,
                              double min, double max, boolean isHours) {
        layout.setError(ValidationMessages.forNumber(requireContext(), result.error, min, max, isHours));
        return result.isValid();
    }

    private String rangeText(int min, int max) {
        NumberFormat format = NumberFormat.getIntegerInstance();
        return getString(R.string.settings_range, format.format(min), format.format(max));
    }

    @SuppressWarnings("deprecation") // the old way is only used on old Android versions
    private String versionText() {
        try {
            PackageManager pm = requireContext().getPackageManager();
            String packageName = requireContext().getPackageName();
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info = pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0));
            } else {
                info = pm.getPackageInfo(packageName, 0);
            }
            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? info.getLongVersionCode() : info.versionCode;
            return getString(R.string.settings_version, info.versionName, (int) versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return getString(R.string.settings_version, "1.0", 1);
        }
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}