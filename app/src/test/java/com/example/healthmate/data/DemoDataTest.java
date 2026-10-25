package com.example.healthmate.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.healthmate.data.local.DemoData;
import com.example.healthmate.utils.DateUtils;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Demo data must look real, have one row per date and never use today.
 */
public class DemoDataTest {

    @Test
    public void demoRows_coverSixPastDaysWithUniqueDates() {
        String today = "2026-10-18";
        List<Object[]> rows = DemoData.buildRows(today);

        assertEquals(6, rows.size());
        Set<String> dates = new HashSet<>();
        for (Object[] row : rows) {
            String date = (String) row[0];
            assertTrue(DateUtils.isValidIso(date));
            assertNotEquals(today, date);
            assertTrue(DateUtils.isAfter(today, date));
            dates.add(date);
            assertEquals(5, row.length);
        }
        assertEquals(6, dates.size());
    }

    @Test
    public void demoSql_neverOverwritesExistingRows() {
        assertTrue(DemoData.INSERT_SQL.startsWith("INSERT OR IGNORE"));
    }
}