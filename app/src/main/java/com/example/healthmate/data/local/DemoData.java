package com.example.healthmate.data.local;

import com.example.healthmate.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

/*
 * Dummy records for testing (the coursework asks for this).
 * They are added only once, when the database is created for the first time.
 * They use the 6 days before today, so today is empty and I can add my own record.
 * They are saved with user_id 0, and the first account created gets them.
 */
public final class DemoData {

    // OR IGNORE = if a record for that date is already there, skip it
    public static final String INSERT_SQL =
            "INSERT OR IGNORE INTO health_records (user_id, date, steps, calories, water, sleep_minutes) "
                    + "VALUES (0, ?, ?, ?, ?, ?)";

    // position 0 = yesterday, 1 = 2 days ago, ...
    private static final int[] STEPS = {8420, 10350, 6210, 9105, 12040, 7480};
    private static final int[] CALORIES = {430, 515, 320, 470, 610, 390};
    private static final int[] WATER = {1900, 2250, 1500, 2000, 2400, 1750};
    // sleep in minutes: 7h30, 8h, 6h45, 7h10, 8h20, 6h
    private static final int[] SLEEP_MINUTES = {450, 480, 405, 430, 500, 360};

    private DemoData() {
    }

    public static List<Object[]> buildRows(String todayIso) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < STEPS.length; i++) {
            String date = DateUtils.addDays(todayIso, -(i + 1));
            rows.add(new Object[]{date, STEPS[i], CALORIES[i], WATER[i], SLEEP_MINUTES[i]});
        }
        return rows;
    }
}