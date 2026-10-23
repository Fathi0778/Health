package com.example.healthmate.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/*
 * Saves small settings in SharedPreferences:
 * the 4 daily goals (separate for every user) and whether the welcome screen
 * was already shown.
 * (The health records themselves are saved in the Room database, not here.)
 */
public class GoalPreferences {

    private static final String PREFS_NAME = "healthmate_prefs";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";
    private static final String KEY_GOAL_STEPS = "goal_steps";
    private static final String KEY_GOAL_WATER = "goal_water";
    private static final String KEY_GOAL_CALORIES = "goal_calories";
    private static final String KEY_GOAL_SLEEP = "goal_sleep";

    private final SharedPreferences prefs;

    public GoalPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isOnboardingComplete() {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false);
    }

    public void setOnboardingComplete(boolean complete) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply();
    }

    // each user has their own goals: the key ends with the user id, e.g. "goal_steps_3"
    private static String key(String name, long userId) {
        return name + "_" + userId;
    }

    public DailyGoals getGoals(long userId) {
        return new DailyGoals(
                prefs.getInt(key(KEY_GOAL_STEPS, userId), DailyGoals.DEFAULT_STEPS),
                prefs.getInt(key(KEY_GOAL_WATER, userId), DailyGoals.DEFAULT_WATER_ML),
                prefs.getInt(key(KEY_GOAL_CALORIES, userId), DailyGoals.DEFAULT_CALORIES),
                prefs.getFloat(key(KEY_GOAL_SLEEP, userId), (float) DailyGoals.DEFAULT_SLEEP_HOURS));
    }

    public void saveGoals(long userId, DailyGoals goals) {
        prefs.edit()
                .putInt(key(KEY_GOAL_STEPS, userId), goals.getStepsGoal())
                .putInt(key(KEY_GOAL_WATER, userId), goals.getWaterGoal())
                .putInt(key(KEY_GOAL_CALORIES, userId), goals.getCaloriesGoal())
                .putFloat(key(KEY_GOAL_SLEEP, userId), (float) goals.getSleepGoal())
                .apply();
    }
}