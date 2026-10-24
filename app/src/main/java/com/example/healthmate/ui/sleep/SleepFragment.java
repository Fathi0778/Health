package com.example.healthmate.ui.sleep;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.healthmate.MainActivity;
import com.example.healthmate.R;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.ui.common.CardBinder;
import com.example.healthmate.utils.DateUtils;
import com.example.healthmate.utils.HealthStats;
import com.example.healthmate.viewmodel.HealthRecordViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

/*
 * Sleep tracker screen (my extra feature).
 *
 * Sleep is saved in the same health_records table (sleep_minutes column),
 * so I don't need a second table. This screen shows the sleep part of each
 * record: today's sleep, weekly average, nights at goal, a 7 night chart
 * and the sleep history.
 */
public class SleepFragment extends Fragment {

    private static final int HISTORY_LIMIT = 14;

    private HealthRecordViewModel viewModel;

    private TextView recordedText;
    private TextView statusText;
    private TextView goalText;
    private TextView percentText;
    private CircularProgressIndicator todayProgress;
    private View tileAverage;
    private View tileNightsAtGoal;
    private View tileBestNight;
    private View weekChart;
    private LinearLayout historyContainer;
    private TextView historyEmpty;

    @Nullable
    private HealthRecord todayRecord;
    @Nullable
    private List<HealthRecord> weekRecords;
    @Nullable
    private List<HealthRecord> allRecords;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sleep, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HealthRecordViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        recordedText = view.findViewById(R.id.text_sleep_recorded);
        statusText = view.findViewById(R.id.text_sleep_status_today);
        goalText = view.findViewById(R.id.text_sleep_goal);
        percentText = view.findViewById(R.id.text_sleep_percent);
        todayProgress = view.findViewById(R.id.progress_sleep_today);
        tileAverage = view.findViewById(R.id.tile_weekly_average);
        tileNightsAtGoal = view.findViewById(R.id.tile_nights_goal);
        tileBestNight = view.findViewById(R.id.tile_best_night);
        weekChart = view.findViewById(R.id.chart_sleep_week);
        historyContainer = view.findViewById(R.id.container_sleep_history);
        historyEmpty = view.findViewById(R.id.text_sleep_history_empty);

        view.findViewById(R.id.button_log_sleep).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openTab(R.id.addRecordFragment));

        viewModel.getTodayRecord().observe(getViewLifecycleOwner(), record -> {
            todayRecord = record;
            renderToday();
        });
        viewModel.getLastSevenDaysRecords().observe(getViewLifecycleOwner(), records -> {
            weekRecords = records;
            renderWeek();
        });
        viewModel.getAllRecords().observe(getViewLifecycleOwner(), records -> {
            allRecords = records;
            renderHistory();
        });
        viewModel.getGoals().observe(getViewLifecycleOwner(), goals -> {
            renderToday();
            renderWeek();
            renderHistory();
        });
    }

    private double sleepGoal() {
        return viewModel.getCurrentGoals().getSleepGoal();
    }

    private void renderToday() {
        double goal = sleepGoal();
        goalText.setText(getString(R.string.sleep_goal_value, HealthStats.formatHours(goal)));

        if (todayRecord == null) {
            recordedText.setText(R.string.sleep_not_recorded);
            statusText.setText(R.string.dashboard_empty_message);
            todayProgress.setProgressCompat(0, true);
            percentText.setText(getString(R.string.dashboard_wellness_score, 0));
            return;
        }
        int minutes = todayRecord.getSleepMinutes();
        int goalMinutes = (int) Math.round(goal * 60);
        int percent = HealthStats.progressPercent(minutes, goalMinutes);
        recordedText.setText(getString(R.string.sleep_recorded, HealthStats.formatSleep(minutes)));
        statusText.setText(minutes >= goalMinutes
                ? getString(R.string.sleep_goal_met)
                : getString(R.string.sleep_goal_short, HealthStats.formatSleep(goalMinutes - minutes)));
        todayProgress.setProgressCompat(percent, true);
        percentText.setText(getString(R.string.dashboard_wellness_score, percent));
    }

    private void renderWeek() {
        String today = viewModel.getToday().getValue();
        if (today == null) {
            today = DateUtils.today();
        }
        List<String> days = DateUtils.lastDays(today, HealthRecordViewModel.WEEK_DAYS);
        HealthStats.Summary summary = HealthStats.summarize(weekRecords, days);
        double goal = sleepGoal();

        String[] labels = new String[days.size()];
        for (int i = 0; i < days.size(); i++) {
            labels[i] = DateUtils.dayOfWeekShort(days.get(i));
        }

        CardBinder.bindStatTile(tileAverage, R.drawable.ic_sleep, R.color.hm_sleep, R.color.hm_sleep_light,
                HealthStats.formatSleep(summary.averageSleepMinutes),
                getString(R.string.sleep_weekly_average));
        CardBinder.bindStatTile(tileNightsAtGoal, R.drawable.ic_check_circle, R.color.hm_success,
                R.color.hm_success_light,
                getString(R.string.sleep_nights_value, HealthStats.nightsAtGoal(summary, goal),
                        summary.daysWithRecords),
                getString(R.string.sleep_nights_goal));
        CardBinder.bindStatTile(tileBestNight, R.drawable.ic_trophy, R.color.hm_calories,
                R.color.hm_calories_light,
                HealthStats.formatSleep(HealthStats.bestSleepMinutes(weekRecords)),
                getString(R.string.sleep_best_night));

        CardBinder.bindChart(weekChart, R.drawable.ic_sleep, R.color.hm_sleep, R.color.hm_sleep_light,
                getString(R.string.sleep_last_7),
                getString(R.string.dashboard_goal, getString(R.string.value_hours, HealthStats.formatHours(goal))),
                summary.sleep, summary.present, labels, (float) goal,
                value -> HealthStats.formatHours(HealthStats.roundOneDecimal(value)) + "h");
    }

    private void renderHistory() {
        historyContainer.removeAllViews();
        boolean empty = allRecords == null || allRecords.isEmpty();
        historyEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            return;
        }
        double goal = sleepGoal();
        int success = ContextCompat.getColor(requireContext(), R.color.hm_success_dark);
        int successLight = ContextCompat.getColor(requireContext(), R.color.hm_success_light);
        int warning = ContextCompat.getColor(requireContext(), R.color.hm_warning);
        int warningLight = ContextCompat.getColor(requireContext(), R.color.hm_warning_light);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int shown = 0;
        for (HealthRecord record : allRecords) { // already newest first
            if (shown >= HISTORY_LIMIT) {
                break;
            }
            View row = inflater.inflate(R.layout.item_sleep_history, historyContainer, false);
            double hours = record.getSleepHours();
            boolean met = record.getSleepMinutes() >= Math.round(goal * 60);

            ((TextView) row.findViewById(R.id.text_sleep_date)).setText(DateUtils.formatLong(record.getDate()));
            ((TextView) row.findViewById(R.id.text_sleep_hours))
                    .setText(HealthStats.formatSleep(record.getSleepMinutes()));

            TextView status = row.findViewById(R.id.text_sleep_status);
            status.setText(met ? R.string.sleep_status_met : R.string.sleep_status_below);
            status.setTextColor(met ? success : warning);
            status.setBackgroundTintList(ColorStateList.valueOf(met ? successLight : warningLight));

            LinearProgressIndicator bar = row.findViewById(R.id.progress_sleep_night);
            bar.setProgressCompat(HealthStats.progressPercent(hours, goal), false);

            historyContainer.addView(row);
            shown++;
        }
    }
}
