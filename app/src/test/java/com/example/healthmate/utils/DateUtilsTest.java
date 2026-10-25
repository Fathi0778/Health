package com.example.healthmate.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/*
 * Checks the date helpers used for one record per day, search and the 7 day charts.
 */
public class DateUtilsTest {

    @Test
    public void today_isIsoFormatted() {
        assertTrue(DateUtils.isValidIso(DateUtils.today()));
    }

    @Test
    public void isValidIso_checksRealCalendarDates() {
        assertTrue(DateUtils.isValidIso("2026-10-18"));
        assertTrue(DateUtils.isValidIso("2024-02-29")); // leap year
        assertFalse(DateUtils.isValidIso("2026-02-29"));
        assertFalse(DateUtils.isValidIso("2026-13-01"));
        assertFalse(DateUtils.isValidIso("2026-10"));
        assertFalse(DateUtils.isValidIso(null));
    }

    @Test
    public void addDays_crossesMonthAndYear() {
        assertEquals("2026-09-30", DateUtils.addDays("2026-10-01", -1));
        assertEquals("2027-01-01", DateUtils.addDays("2026-12-31", 1));
        assertEquals("2024-02-29", DateUtils.addDays("2024-03-01", -1));
    }

    @Test
    public void lastDays_returnsSevenCalendarDaysOldestFirst() {
        List<String> days = DateUtils.lastDays("2026-10-18", 7);
        assertEquals(Arrays.asList("2026-10-12", "2026-10-13", "2026-10-14", "2026-10-15",
                "2026-10-16", "2026-10-17", "2026-10-18"), days);
    }

    @Test
    public void utcMillis_roundTrip() {
        long millis = DateUtils.isoToUtcMillis("2026-10-18");
        assertEquals("2026-10-18", DateUtils.utcMillisToIso(millis));
    }

    @Test
    public void isoStrings_sortChronologically() {
        assertTrue(DateUtils.isAfter("2026-10-18", "2026-09-30"));
        assertFalse(DateUtils.isAfter("2025-12-31", "2026-01-01"));
    }

    @Test
    public void partOfDay_matchesGreeting() {
        assertEquals(0, DateUtils.partOfDay(8));
        assertEquals(1, DateUtils.partOfDay(13));
        assertEquals(2, DateUtils.partOfDay(20));
    }
}