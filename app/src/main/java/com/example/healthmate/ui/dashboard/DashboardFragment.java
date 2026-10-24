package com.example.healthmate.ui.dashboard;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.healthmate.MainActivity;
import com.example.healthmate.R;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.data.prefs.DailyGoals;
import com.example.healthmate.data.prefs.SessionManager;
import com.example.healthmate.ui.editrecord.EditRecordFragment;
import com.example.healthmate.utils.DateUtils;
import com.example.healthmate.utils.HealthStats;
import com.example.healthmate.viewmodel.HealthRecordViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/*
 * Home screen (dashboard).
 * Every number here comes from today's record in the database and from my goals.
 * Nothing is typed in by hand. It updates by itself because it observes LiveData.
 */
public class DashboardFragment extends Fragment {

    private HealthRecordViewModel viewModel;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault());

    private TextView dateText;
    private TextView greetingText;
    private TextView motivationText;
    private TextView wellnessHeadline;
    private TextView wellnessSummary;
    private TextView wellnessScore;
    private CircularProgressIndicator wellnessProgress;
    private View cardSteps;
    private View cardCalories;
    private View cardWater;
    private View cardSleep;
    private MaterialButton addButton;
    private LinearLayout recentContainer;
    private TextView recentEmpty;
    private TextView streakText;

    @Nullable
    private HealthRecord todayRecord;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HealthRecordViewModel.class);

        dateText = view.findViewById(R.id.text_date);
        greetingText = view.findViewById(R.id.text_greeting);
        motivationText = view.findViewById(R.id.text_motivation);
        wellnessHeadline = view.findViewById(R.id.text_wellness_headline);
        wellnessSummary = view.findViewById(R.id.text_wellness_summary);
        wellnessScore = view.findViewById(R.id.text_wellness_score);
        wellnessProgress = view.findViewById(R.id.progress_wellness);
        cardSteps = view.findViewById(R.id.card_steps);
        cardCalories = view.findViewById(R.id.card_calories);
        cardWater = view.findViewById(R.id.card_water);
        cardSleep = view.findViewById(R.id.card_sleep);
        addButton = view.findViewById(R.id.button_add_record);
        recentContainer = view.findViewById(R.id.container_recent);
        recentEmpty = view.findViewById(R.id.text_recent_empty);
        streakText = view.findViewById(R.id.text_streak);

        // clicks
        addButton.setOnClickListener(v -> {
            if (todayRecord != null) {
                openEditor(todayRecord.getId());
            } else {
                mainActivity().openTab(R.id.addRecordFragment);
            }
        });
        View.OnClickListener openSleep = v -> NavHostFragment.findNavController(this)
                .navigate(R.id.sleepFragment);
        cardSleep.setOnClickListener(openSleep);
        view.findViewById(R.id.card_sleep_tracker).setOnClickListener(openSleep);
        view.findViewById(R.id.button_see_all).setOnClickListener(v ->
                mainActivity().openTab(R.id.recordsFragment));

        // watch the ViewModel, the screen redraws when the data changes
        viewModel.getToday().observe(getViewLifecycleOwner(), this::renderHeader);
        viewModel.getTodayRecord().observe(getViewLifecycleOwner(), record -> {
            todayRecord = record;
            renderToday();
        });
        viewModel.getGoals().observe(getViewLifecycleOwner(), goals -> renderToday());
        // show ALL saved records on Home (newest first), not only the latest few
        viewModel.getAllRecords().observe(getViewLifecycleOwner(), records -> {
            renderRecent(records);
            renderStreak(records);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshToday(); // keeps "today" right if the app stayed open past midnight
    }

    // ---------------- Show the data ----------------

    private void renderHeader(String todayIso) {
        dateText.setText(DateUtils.formatFull(todayIso));

        int part = DateUtils.partOfDay(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        @StringRes int greeting = part == 0 ? R.string.greeting_morning
                : part == 1 ? R.string.greeting_afternoon : R.string.greeting_evening;
        // "Good morning, Fathima"
        greetingText.setText(getString(R.string.greeting_with_name,
                getString(greeting), SessionManager.displayName(viewModel.getUsername())));

        String[] messages = getResources().getStringArray(R.array.motivational_messages);
        Calendar calendar = Calendar.getInstance();
        motivationText.setText(messages[calendar.get(Calendar.DAY_OF_YEAR) % messages.length]);
    }

    private void renderToday() {
        DailyGoals goals = viewModel.getCurrentGoals();
        HealthRecord record = todayRecord;

        int steps = record == null ? 0 : record.getSteps();
        int calories = record == null ? 0 : record.getCalories();
        int water = record == null ? 0 : record.getWater();
        int sleepMinutes = record == null ? 0 : record.getSleepMinutes();

        bindMetric(cardSteps, R.drawable.ic_steps, R.color.hm_steps, R.color.hm_steps_light, R.color.hm_steps_dark,
                R.string.metric_steps, numberFormat.format(steps), getString(R.string.unit_steps),
                HealthStats.progressPercent(steps, goals.getStepsGoal()),
                getString(R.string.value_steps, numberFormat.format(goals.getStepsGoal())));

        bindMetric(cardCalories, R.drawable.ic_calories, R.color.hm_calories, R.color.hm_calories_light, R.color.hm_calories_dark,
                R.string.metric_calories, numberFormat.format(calories), getString(R.string.unit_kcal),
                HealthStats.progressPercent(calories, goals.getCaloriesGoal()),
                getString(R.string.value_kcal, numberFormat.format(goals.getCaloriesGoal())));

        bindMetric(cardWater, R.drawable.ic_water, R.color.hm_water, R.color.hm_water_light, R.color.hm_water_dark,
                R.string.metric_water, numberFormat.format(water), getString(R.string.unit_ml),
                HealthStats.progressPercent(water, goals.getWaterGoal()),
                getString(R.string.value_ml, numberFormat.format(goals.getWaterGoal())));

        bindMetric(cardSleep, R.drawable.ic_sleep, R.color.hm_sleep, R.color.hm_sleep_light, R.color.hm_sleep_dark,
                R.string.metric_sleep, HealthStats.formatSleep(sleepMinutes), "",
                HealthStats.progressPercent(sleepMinutes / 60.0, goals.getSleepGoal()),
                getString(R.string.value_hours, HealthStats.formatHours(goals.getSleepGoal())));

        // wellness card at the top
        int score = HealthStats.wellnessScore(record, goals);
        wellnessProgress.setProgressCompat(score, true);
        wellnessScore.setText(getString(R.string.dashboard_wellness_score, score));
        if (record == null) {
            wellnessHeadline.setText(R.string.dashboard_empty_title);
            wellnessSummary.setText(R.string.dashboard_empty_message);
            addButton.setText(R.string.dashboard_add_record);
            addButton.setIconResource(R.drawable.ic_add);
        } else {
            wellnessHeadline.setText(score >= 100
                    ? R.string.dashboard_headline_complete : R.string.dashboard_headline_progress);
            wellnessSummary.setText(score >= 100
                    ? getString(R.string.dashboard_wellness_complete)
                    : getString(R.string.dashboard_wellness_summary, score));
            addButton.setText(R.string.dashboard_update_record);
            addButton.setIconResource(R.drawable.ic_edit);
        }
    }

    // fills one of the 4 small cards (item_metric_card.xml)
    private void bindMetric(View card, @DrawableRes int iconRes, @ColorRes int colorRes,
                            @ColorRes int lightColorRes, @ColorRes int darkColorRes,
                            @StringRes int labelRes, String value,
                            String unit, int percent, String goalText) {
        int color = ContextCompat.getColor(requireContext(), colorRes);
        int lightColor = ContextCompat.getColor(requireContext(), lightColorRes);

        ImageView icon = card.findViewById(R.id.metric_icon);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(color));
        icon.setBackgroundTintList(ColorStateList.valueOf(lightColor));

        TextView percentText = card.findViewById(R.id.metric_percent);
        percentText.setText(getString(R.string.dashboard_wellness_score, percent));
        percentText.setTextColor(ContextCompat.getColor(requireContext(), darkColorRes));
        percentText.setBackgroundTintList(ColorStateList.valueOf(lightColor));

        ((TextView) card.findViewById(R.id.metric_label)).setText(labelRes);
        ((TextView) card.findViewById(R.id.metric_value)).setText(value);
        ((TextView) card.findViewById(R.id.metric_unit)).setText(unit);
        ((TextView) card.findViewById(R.id.metric_goal)).setText(getString(R.string.dashboard_goal, goalText));

        LinearProgressIndicator progress = card.findViewById(R.id.metric_progress);
        progress.setIndicatorColor(color);
        progress.setTrackColor(lightColor);
        progress.setProgressCompat(percent, true);

        card.setContentDescription(getString(R.string.cd_metric_card,
                getString(labelRes), value, unit, percent));
    }

    // "3 day streak" badge on the blue card
    private void renderStreak(@Nullable List<HealthRecord> records) {
        String today = viewModel.getToday().getValue();
        int streak = HealthStats.currentStreak(records, today != null ? today : DateUtils.today());
        if (streak <= 0) {
            streakText.setVisibility(View.GONE);
            return;
        }
        streakText.setText(getResources().getQuantityString(R.plurals.dashboard_streak, streak, streak));
        streakText.setVisibility(View.VISIBLE);
    }

    private void renderRecent(@Nullable List<HealthRecord> records) {
        recentContainer.removeAllViews();
        boolean empty = records == null || records.isEmpty();
        recentEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (HealthRecord record : records) {
            View row = inflater.inflate(R.layout.item_recent_activity, recentContainer, false);
            ((TextView) row.findViewById(R.id.text_recent_day)).setText(DateUtils.dayOfMonth(record.getDate()));
            ((TextView) row.findViewById(R.id.text_recent_date)).setText(DateUtils.formatLong(record.getDate()));
            ((TextView) row.findViewById(R.id.text_recent_summary)).setText(getString(R.string.recent_summary,
                    numberFormat.format(record.getSteps()),
                    numberFormat.format(record.getCalories()),
                    numberFormat.format(record.getWater()),
                    HealthStats.formatSleep(record.getSleepMinutes())));
            row.setOnClickListener(v -> openEditor(record.getId()));
            recentContainer.addView(row);
        }
    }

    private void openEditor(long recordId) {
        NavHostFragment.findNavController(this).navigate(
                R.id.editRecordFragment, EditRecordFragment.createArgs(recordId));
    }

    private MainActivity mainActivity() {
        return (MainActivity) requireActivity();
    }
}
