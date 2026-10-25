package com.example.healthmate.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.healthmate.data.local.HealthMateDatabase;
import com.example.healthmate.data.local.HealthRecordDao;
import com.example.healthmate.data.model.HealthRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/*
 * Database tests on a real SQLite database (in memory) - they run on the emulator.
 * Covers test cases 1, 3, 4, 5, 6, 7 and 9, and that users only see their own records.
 */
@RunWith(AndroidJUnit4.class)
public class HealthRecordDaoTest {

    // makes LiveData give values straight away
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final long USER = 1;
    private static final long OTHER_USER = 2;

    private HealthMateDatabase database;
    private HealthRecordDao dao;

    // makes a record that belongs to a user
    private static HealthRecord rec(long userId, String date, int steps, int calories, int water, int sleepMinutes) {
        HealthRecord record = new HealthRecord(date, steps, calories, water, sleepMinutes);
        record.setUserId(userId);
        return record;
    }

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, HealthMateDatabase.class)
                .allowMainThreadQueries() // ok in tests only
                .build();
        dao = database.healthRecordDao();
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    // Test case 1: adding a valid record
    @Test
    public void insertValidRecord_canBeReadBack() {
        long id = dao.insert(rec(USER, "2026-10-18", 7850, 420, 2000, 450));
        assertTrue(id > 0);

        HealthRecord saved = dao.getById(USER, id);
        assertNotNull(saved);
        assertEquals("2026-10-18", saved.getDate());
        assertEquals(7850, saved.getSteps());
        assertEquals(420, saved.getCalories());
        assertEquals(2000, saved.getWater());
    }

    // Test case 3: the UNIQUE index stops two records on the same date
    @Test
    public void insertDuplicateDate_throwsConstraintException() {
        dao.insert(rec(USER, "2026-10-18", 1000, 100, 500, 7));
        try {
            dao.insert(rec(USER, "2026-10-18", 2000, 200, 600, 8));
            fail("Expected SQLiteConstraintException for duplicate date");
        } catch (SQLiteConstraintException expected) {
            // correct
        }
        assertEquals(1, dao.count(USER));
    }

    // Test case 4: viewing saved records, newest first
    @Test
    public void observeAll_returnsNewestFirst() throws Exception {
        dao.insert(rec(USER, "2026-10-16", 1, 1, 1, 1));
        dao.insert(rec(USER, "2026-10-18", 3, 3, 3, 3));
        dao.insert(rec(USER, "2026-10-17", 2, 2, 2, 2));

        List<HealthRecord> records = LiveDataTestUtil.getOrAwaitValue(dao.observeAll(USER));
        assertEquals(3, records.size());
        assertEquals("2026-10-18", records.get(0).getDate());
        assertEquals("2026-10-16", records.get(2).getDate());
    }

    // Test case 5: updating keeps the same id
    @Test
    public void updateRecord_changesValuesAndKeepsId() {
        long id = dao.insert(rec(USER, "2026-10-18", 1000, 100, 500, 360));
        HealthRecord record = dao.getById(USER, id);
        record.setSteps(9000);
        record.setSleepMinutes(495); // 8h 15m

        assertEquals(1, dao.update(record));

        HealthRecord updated = dao.getById(USER, id);
        assertEquals(9000, updated.getSteps());
        assertEquals(495, updated.getSleepMinutes());
        assertEquals(id, updated.getId());
        assertEquals(1, dao.count(USER));
    }

    // Test case 6: deleting removes only the chosen record
    @Test
    public void deleteRecord_removesOnlyThatRecord() {
        long keepId = dao.insert(rec(USER, "2026-10-17", 1, 1, 1, 1));
        long deleteId = dao.insert(rec(USER, "2026-10-18", 2, 2, 2, 2));

        assertEquals(1, dao.delete(dao.getById(USER, deleteId)));

        assertNull(dao.getById(USER, deleteId));
        assertNotNull(dao.getById(USER, keepId));
        assertEquals(1, dao.count(USER));
    }

    // Test case 7: search by date (one day, or a whole month)
    @Test
    public void searchByDatePrefix_findsMatchingDates() {
        dao.insert(rec(USER, "2026-09-30", 1, 1, 1, 1));
        dao.insert(rec(USER, "2026-10-01", 2, 2, 2, 2));
        dao.insert(rec(USER, "2026-10-18", 3, 3, 3, 3));

        assertEquals(2, dao.searchByDatePrefix(USER, "2026-10").size());
        List<HealthRecord> exact = dao.searchByDatePrefix(USER, "2026-10-18");
        assertEquals(1, exact.size());
        assertEquals(3, exact.get(0).getSteps());
        assertEquals(0, dao.searchByDatePrefix(USER, "2025").size());
    }

    @Test
    public void dateRange_isInclusiveAndOldestFirst() {
        dao.insert(rec(USER, "2026-10-11", 1, 1, 1, 1)); // outside the 7 days
        dao.insert(rec(USER, "2026-10-12", 2, 2, 2, 2));
        dao.insert(rec(USER, "2026-10-18", 3, 3, 3, 3));

        List<HealthRecord> week = dao.getBetween(USER, "2026-10-12", "2026-10-18");
        assertEquals(2, week.size());
        assertEquals("2026-10-12", week.get(0).getDate());
    }

    // Test case 9: sleep is saved from hours + minutes (7h 30m = 450 minutes)
    @Test
    public void sleep_isSavedAsMinutes() {
        HealthRecord sleep = new HealthRecord("2026-10-18", 0, 0, 0, 7, 30);
        sleep.setUserId(USER);
        long id = dao.insert(sleep);
        assertEquals(450, dao.getById(USER, id).getSleepMinutes());
        assertEquals(7.5, dao.getByDate(USER, "2026-10-18").getSleepHours(), 0.0001);
    }

    // login: two users can each have a record on the same date
    @Test
    public void twoUsers_canUseTheSameDate() {
        dao.insert(rec(USER, "2026-10-18", 1000, 100, 500, 400));
        dao.insert(rec(OTHER_USER, "2026-10-18", 2000, 200, 600, 420));
        assertEquals(1, dao.count(USER));
        assertEquals(1, dao.count(OTHER_USER));
    }

    // login: a user never sees another user's records
    @Test
    public void users_onlySeeTheirOwnRecords() throws Exception {
        long otherId = dao.insert(rec(OTHER_USER, "2026-10-17", 5, 5, 5, 5));
        dao.insert(rec(USER, "2026-10-18", 1, 1, 1, 1));

        List<HealthRecord> mine = LiveDataTestUtil.getOrAwaitValue(dao.observeAll(USER));
        assertEquals(1, mine.size());
        assertEquals("2026-10-18", mine.get(0).getDate());
        assertNull(dao.getById(USER, otherId));
        assertEquals(0, dao.searchByDatePrefix(USER, "2026-10-17").size());
    }

    // demo records (user_id 0) are given to the first account
    @Test
    public void claimUnownedRecords_givesDemoRowsToAUser() {
        dao.insert(rec(0, "2026-10-16", 1, 1, 1, 1));
        dao.insert(rec(0, "2026-10-17", 1, 1, 1, 1));
        assertEquals(2, dao.claimUnownedRecords(USER));
        assertEquals(2, dao.count(USER));
        assertEquals(0, dao.count(0));
    }
}