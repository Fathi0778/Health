package com.example.healthmate.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.data.prefs.DailyGoals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
 * Test cases 8 and 9: dashboard numbers and sleep maths.
 */
public class HealthStatsTest {

    private static final DailyGoals GOALS = DailyGoals.defaults(); // 10,000 / 2,000 ml / 500 kcal / 8 h

    @Test
    public void progressPercent_isCappedAndSafe() {
        assertEquals(79, HealthStats.progressPercent(7850, 10_000)); // 78.5 rounds to 79
        assertEquals(100, HealthStats.progressPercent(12_000, 10_000));
        assertEquals(0, HealthStats.progressPercent(0, 10_000));
        assertEquals(0, HealthStats.progressPercent(500, 0)); // no divide by zero
    }

    @Test
    public void dashboardTotals_forTodaysRecord() {
        HealthRecord today = new HealthRecord("2026-10-18", 7850, 420, 2000, 7, 30);
        assertEquals(79, HealthStats.progressPercent(today.getSteps(), GOALS.getStepsGoal()));
        assertEquals(84, HealthStats.progressPercent(today.getCalories(), GOALS.getCaloriesGoal()));
        assertEquals(100, HealthStats.progressPercent(today.getWater(), GOALS.getWaterGoal()));
        assertEquals(94, HealthStats.progressPercent(today.getSleepHours(), GOALS.getSleepGoal()));
        // (79 + 84 + 100 + 94) / 4 = 89.25 -> 89
        assertEquals(89, HealthStats.wellnessScore(today, GOALS));
    }

    @Test
    public void noRecordToday_givesZeroScore() {
        assertEquals(0, HealthStats.wellnessScore(null, GOALS));
    }

    @Test
    public void summarize_keepsMissingDaysAndAveragesOnlyRecordedDays() {
        List<String> days = DateUtils.lastDays("2026-10-18", 7);
        List<HealthRecord> records = Arrays.asList(
                new HealthRecord("2026-10-12", 8000, 400, 2000, 420),
                new HealthRecord("2026-10-15", 10000, 500, 1500, 480),
                new HealthRecord("2026-10-18", 6000, 300, 2500, 390));

        HealthStats.Summary s = HealthStats.summarize(records, days);

        assertEquals(3, s.daysWithRecords);
        assertTrue(s.present[0]);
        assertFalse(s.present[1]);
        assertTrue(s.present[6]);
        assertEquals(0f, s.steps[1], 0.001f);

        assertEquals(24000, s.totalSteps);
        assertEquals(1200, s.totalCalories);
        assertEquals(8000, s.averageSteps);       // 24000 / 3 (not / 7)
        assertEquals(2000, s.averageWater);       // 6000 / 3
        assertEquals(430, s.averageSleepMinutes); // (420 + 480 + 390) / 3 = 430 = 7h 10m
    }

    @Test
    public void summarize_withNoRecords_isAllZero() {
        HealthStats.Summary s = HealthStats.summarize(Collections.emptyList(),
                DateUtils.lastDays("2026-10-18", 7));
        assertEquals(0, s.daysWithRecords);
        assertEquals(0, s.averageSteps);
        assertEquals(0, s.averageSleepMinutes);
    }

    @Test
    public void sleepStats_nightsAtGoalAndBestNight() {
        List<String> days = DateUtils.lastDays("2026-10-18", 7);
        List<HealthRecord> records = Arrays.asList(
                new HealthRecord("2026-10-16", 0, 0, 0, 510),
                new HealthRecord("2026-10-17", 0, 0, 0, 360),
                new HealthRecord("2026-10-18", 0, 0, 0, 480));
        HealthStats.Summary s = HealthStats.summarize(records, days);

        assertEquals(2, HealthStats.nightsAtGoal(s, 8.0));
        assertEquals(510, HealthStats.bestSleepMinutes(records));
        assertEquals(450, s.averageSleepMinutes);
    }

    @Test
    public void formatHours_dropsTrailingZero() {
        assertEquals("7", HealthStats.formatHours(7.0));
        assertEquals("7.5", HealthStats.formatHours(7.5));
        assertEquals("7.25", HealthStats.formatHours(7.25));
        assertEquals("0", HealthStats.formatHours(0));
    }

    @Test
    public void formatSleep_showsHoursAndMinutes() {
        assertEquals("7h 30m", HealthStats.formatSleep(450));
        assertEquals("8h", HealthStats.formatSleep(480));
        assertEquals("45m", HealthStats.formatSleep(45));
        assertEquals("0h", HealthStats.formatSleep(0));
    }

    @Test
    public void streak_countsDaysInARow() {
        List<HealthRecord> records = Arrays.asList(
                new HealthRecord("2026-10-18", 1, 1, 1, 1),
                new HealthRecord("2026-10-17", 1, 1, 1, 1),
                new HealthRecord("2026-10-16", 1, 1, 1, 1),
                new HealthRecord("2026-10-14", 1, 1, 1, 1)); // gap on the 15th
        assertEquals(3, HealthStats.currentStreak(records, "2026-10-18"));
        // today not logged yet -> count from yesterday
        assertEquals(3, HealthStats.currentStreak(records, "2026-10-19"));
        // two days missed -> streak is broken
        assertEquals(0, HealthStats.currentStreak(records, "2026-10-20"));
        assertEquals(0, HealthStats.currentStreak(Collections.emptyList(), "2026-10-18"));
    }
}