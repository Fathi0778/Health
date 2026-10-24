package com.example.healthmate.ui.progress;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.healthmate.R;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.data.prefs.DailyGoals;
import com.example.healthmate.ui.common.CardBinder;
import com.example.healthmate.utils.DateUtils;
import com.example.healthmate.utils.HealthStats;
import com.example.healthmate.viewmodel.HealthRecordViewModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/*
 * Progress screen - charts for the last 7 days.
 * All numbers come from the database and the charts redraw by themselves
 * when a record or a goal changes.
 */
public class ProgressFragment extends Fragment {

    private HealthRecordViewModel viewModel;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault());

    private TextView rangeText;
    private TextView noteText;
    private View tileAvgSteps;
    private View tileAvgWater;
    private View tileTotalCalories;
    private View tileAvgSleep;
    private View chartSteps;
    private View chartCalories;
    private View chartWater;
    private View chartSleep;

    @Nullable
    private List<HealthRecord> weekRecords;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HealthRecordViewModel.class);

        rangeText = view.findViewById(R.id.text_progress_range);
        noteText = view.findViewById(R.id.text_progress_note);
        tileAvgSteps = view.findViewById(R.id.tile_avg_steps);
        tileAvgWater = view.findViewById(R.id.tile_avg_water);
        tileTotalCalories = view.findViewById(R.id.tile_total_calories);
        tileAvgSleep = view.findViewById(R.id.tile_avg_sleep);
        chartSteps = view.findViewById(R.id.chart_steps);
        chartCalories = view.findViewById(R.id.chart_calories);
        chartWater = view.findViewById(R.id.chart_water);
        chartSleep = view.findViewById(R.id.chart_sleep);

        view.findViewById(R.id.button_open_sleep).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.sleepFragment));

        viewModel.getLastSevenDaysRecords().observe(getViewLifecycleOwner(), records -> {
            weekRecords = records;
            render();
        });
        viewModel.getGoals().observe(getViewLifecycleOwner(), goals -> render());
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshToday();
    }

    private void render() {
        String today = viewModel.getToday().getValue();
        if (today == null) {
            today = DateUtils.today();
        }
        List<String> days = DateUtils.lastDays(today, HealthRecordViewModel.WEEK_DAYS);
        HealthStats.Summary summary = HealthStats.summarize(weekRecords, days);
        DailyGoals goals = viewModel.getCurrentGoals();

        String[] labels = new String[days.size()];
        for (int i = 0; i < days.size(); i++) {
            labels[i] = DateUtils.dayOfWeekShort(days.get(i));
        }

        rangeText.setText(getString(R.string.progress_subtitle,
                DateUtils.formatShort(days.get(0)), DateUtils.formatShort(today)));

        if (summary.daysWithRecords == 0) {
            noteText.setText(R.string.progress_no_data);
        } else {
            noteText.setText(getResources().getQuantityString(R.plurals.progress_missing_note,
                    summary.daysWithRecords, summary.daysWithRecords));
        }

        // the 4 tiles at the top
        CardBinder.bindStatTile(tileAvgSteps, R.drawable.ic_steps, R.color.hm_steps, R.color.hm_steps_light,
                numberFormat.format(summary.averageSteps), getString(R.string.progress_avg_steps));
        CardBinder.bindStatTile(tileAvgWater, R.drawable.ic_water, R.color.hm_water, R.color.hm_water_light,
                getString(R.string.value_ml, numberFormat.format(summary.averageWater)),
                getString(R.string.progress_avg_water));
        CardBinder.bindStatTile(tileTotalCalories, R.drawable.ic_calories, R.color.hm_calories,
                R.color.hm_calories_light,
                getString(R.string.value_kcal, numberFormat.format(summary.totalCalories)),
                getString(R.string.progress_total_calories));
        CardBinder.bindStatTile(tileAvgSleep, R.drawable.ic_sleep, R.color.hm_sleep, R.color.hm_sleep_light,
                HealthStats.formatSleep(summary.averageSleepMinutes),
                getString(R.string.progress_avg_sleep));

        // the 4 charts
        CardBinder.bindChart(chartSteps, R.drawable.ic_steps, R.color.hm_steps, R.color.hm_steps_light,
                getString(R.string.progress_chart_steps),
                getString(R.string.dashboard_goal, numberFormat.format(goals.getStepsGoal())),
                summary.steps, summary.present, labels, goals.getStepsGoal(), CardBinder::compact);

        CardBinder.bindChart(chartCalories, R.drawable.ic_calories, R.color.hm_calories,
                R.color.hm_calories_light, getString(R.string.progress_chart_calories),
                getString(R.string.dashboard_goal, getString(R.string.value_kcal,
                        numberFormat.format(goals.getCaloriesGoal()))),
                summary.calories, summary.present, labels, goals.getCaloriesGoal(), CardBinder::compact);

        CardBinder.bindChart(chartWater, R.drawable.ic_water, R.color.hm_water, R.color.hm_water_light,
                getString(R.string.progress_chart_water),
                getString(R.string.dashboard_goal, getString(R.string.value_ml,
                        numberFormat.format(goals.getWaterGoal()))),
                summary.water, summary.present, labels, goals.getWaterGoal(), CardBinder::compact);

        CardBinder.bindChart(chartSleep, R.drawable.ic_sleep, R.color.hm_sleep, R.color.hm_sleep_light,
                getString(R.string.progress_chart_sleep),
                getString(R.string.dashboard_goal, getString(R.string.value_hours,
                        HealthStats.formatHours(goals.getSleepGoal()))),
                summary.sleep, summary.present, labels, (float) goals.getSleepGoal(),
                value -> HealthStats.formatHours(HealthStats.roundOneDecimal(value)) + "h");
    }
}
