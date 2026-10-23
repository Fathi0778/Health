package com.example.healthmate.utils;

import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.data.prefs.DailyGoals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * The maths for the Dashboard, Progress and Sleep screens.
 * Plain Java (no Android), so it is tested in HealthStatsTest.
 */
public final class HealthStats {

    private HealthStats() {
    }

    // how much of the goal is done, from 0 to 100
    public static int progressPercent(double value, double goal) {
        if (goal <= 0 || value <= 0) {
            return 0;
        }
        double percent = (value / goal) * 100.0;
        return (int) Math.min(100, Math.round(percent));
    }

    // wellness score = average of the 4 goal percentages. No record = 0
    public static int wellnessScore(HealthRecord record, DailyGoals goals) {
        if (record == null) {
            return 0;
        }
        int sum = progressPercent(record.getSteps(), goals.getStepsGoal())
                + progressPercent(record.getCalories(), goals.getCaloriesGoal())
                + progressPercent(record.getWater(), goals.getWaterGoal())
                + progressPercent(record.getSleepHours(), goals.getSleepGoal());
        return Math.round(sum / 4f);
    }

    /*
     * Makes the numbers for a list of days (for example the last 7 days).
     * A day with no record stays 0 and present[i] is false.
     * Averages only count the days that have a record.
     */
    public static Summary summarize(List<HealthRecord> records, List<String> days) {
        Map<String, HealthRecord> byDate = new HashMap<>();
        if (records != null) {
            for (HealthRecord record : records) {
                byDate.put(record.getDate(), record);
            }
        }

        int n = days.size();
        Summary s = new Summary(n);
        long totalSteps = 0;
        long totalWater = 0;
        long totalCalories = 0;
        long totalSleepMinutes = 0;

        for (int i = 0; i < n; i++) {
            String day = days.get(i);
            s.days[i] = day;
            HealthRecord record = byDate.get(day);
            if (record == null) {
                continue;
            }
            s.present[i] = true;
            s.steps[i] = record.getSteps();
            s.calories[i] = record.getCalories();
            s.water[i] = record.getWater();
            s.sleep[i] = (float) record.getSleepHours();
            s.daysWithRecords++;
            totalSteps += record.getSteps();
            totalWater += record.getWater();
            totalCalories += record.getCalories();
            totalSleepMinutes += record.getSleepMinutes();
        }

        s.totalSteps = totalSteps;
        s.totalCalories = totalCalories;
        s.totalWater = totalWater;
        s.totalSleepMinutes = totalSleepMinutes;
        if (s.daysWithRecords > 0) {
            s.averageSteps = Math.round((double) totalSteps / s.daysWithRecords);
            s.averageWater = Math.round((double) totalWater / s.daysWithRecords);
            s.averageCalories = Math.round((double) totalCalories / s.daysWithRecords);
            s.averageSleepMinutes = (int) Math.round((double) totalSleepMinutes / s.daysWithRecords);
        }
        return s;
    }

    // how many nights reached the sleep goal
    public static int nightsAtGoal(Summary summary, double sleepGoalHours) {
        int count = 0;
        for (int i = 0; i < summary.days.length; i++) {
            if (summary.present[i] && summary.sleep[i] >= sleepGoalHours) {
                count++;
            }
        }
        return count;
    }

    // longest sleep in the list, in minutes
    public static int bestSleepMinutes(List<HealthRecord> records) {
        int best = 0;
        if (records == null) {
            return 0;
        }
        for (HealthRecord record : records) {
            best = Math.max(best, record.getSleepMinutes());
        }
        return best;
    }

    /*
     * Streak = how many days in a row have a record.
     * If today has no record yet, I start counting from yesterday,
     * so the streak is not lost in the morning before I log today.
     */
    public static int currentStreak(List<HealthRecord> records, String todayIso) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        Set<String> dates = new HashSet<>();
        for (HealthRecord record : records) {
            dates.add(record.getDate());
        }
        String day = dates.contains(todayIso) ? todayIso : DateUtils.addDays(todayIso, -1);
        int streak = 0;
        while (dates.contains(day)) {
            streak++;
            day = DateUtils.addDays(day, -1);
        }
        return streak;
    }

    public static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // 7.0 -> "7", 7.5 -> "7.5" (used for goals)
    public static String formatHours(double hours) {
        double rounded = Math.round(hours * 100.0) / 100.0;
        if (rounded == Math.floor(rounded)) {
            return String.valueOf((long) rounded);
        }
        String text = String.valueOf(rounded);
        return text.endsWith("0") && text.contains(".") ? text.substring(0, text.length() - 1) : text;
    }

    // 450 -> "7h 30m", 480 -> "8h", 45 -> "45m", 0 -> "0h"
    public static String formatSleep(int totalMinutes) {
        if (totalMinutes < 0) {
            totalMinutes = 0;
        }
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (minutes == 0) {
            return hours + "h";
        }
        if (hours == 0) {
            return minutes + "m";
        }
        return hours + "h " + minutes + "m";
    }

    // decimal hours to the same text (7.5 -> "7h 30m"), used by the chart labels
    public static String formatSleepHours(double hours) {
        return formatSleep((int) Math.round(hours * 60));
    }

    // holds the result of summarize()
    public static final class Summary {
        public final String[] days;
        public final boolean[] present;
        public final float[] steps;
        public final float[] calories;
        public final float[] water;
        public final float[] sleep; // in hours, for the chart
        public int daysWithRecords;
        public long totalSteps;
        public long totalCalories;
        public long totalWater;
        public long totalSleepMinutes;
        public long averageSteps;
        public long averageCalories;
        public long averageWater;
        public int averageSleepMinutes;

        Summary(int size) {
            days = new String[size];
            present = new boolean[size];
            steps = new float[size];
            calories = new float[size];
            water = new float[size];
            sleep = new float[size];
        }
    }
}