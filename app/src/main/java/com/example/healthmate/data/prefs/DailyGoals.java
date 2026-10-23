package com.example.healthmate.data.prefs;

/*
 * Holds my 4 daily goals (steps, water, calories, sleep).
 * The user can change them in Settings. They are example targets, not medical advice.
 */
public class DailyGoals {

    public static final int DEFAULT_STEPS = 10_000;
    public static final int DEFAULT_WATER_ML = 2_000;
    public static final int DEFAULT_CALORIES = 500;
    public static final double DEFAULT_SLEEP_HOURS = 8.0;

    private final int stepsGoal;
    private final int waterGoal;
    private final int caloriesGoal;
    private final double sleepGoal;

    public DailyGoals(int stepsGoal, int waterGoal, int caloriesGoal, double sleepGoal) {
        this.stepsGoal = stepsGoal;
        this.waterGoal = waterGoal;
        this.caloriesGoal = caloriesGoal;
        this.sleepGoal = sleepGoal;
    }

    public static DailyGoals defaults() {
        return new DailyGoals(DEFAULT_STEPS, DEFAULT_WATER_ML, DEFAULT_CALORIES, DEFAULT_SLEEP_HOURS);
    }

    public int getStepsGoal() {
        return stepsGoal;
    }

    public int getWaterGoal() {
        return waterGoal;
    }

    public int getCaloriesGoal() {
        return caloriesGoal;
    }

    public double getSleepGoal() {
        return sleepGoal;
    }
}