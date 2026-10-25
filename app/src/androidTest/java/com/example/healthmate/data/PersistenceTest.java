package com.example.healthmate.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.healthmate.data.local.HealthMateDatabase;
import com.example.healthmate.data.model.HealthRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/*
 * Test case 10: records are still there after the app restarts.
 * I copy a restart by closing the database file and opening it again.
 */
@RunWith(AndroidJUnit4.class)
public class PersistenceTest {

    private static final String TEST_DB_NAME = "healthmate_persistence_test.db";
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(TEST_DB_NAME);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(TEST_DB_NAME);
    }

    private HealthMateDatabase open() {
        return Room.databaseBuilder(context, HealthMateDatabase.class, TEST_DB_NAME)
                .allowMainThreadQueries()
                .build();
    }

    @Test
    public void recordSurvivesClosingAndReopeningDatabase() {
        HealthMateDatabase first = open();
        HealthRecord record = new HealthRecord("2026-10-18", 7850, 420, 2000, 450);
        record.setUserId(1);
        first.healthRecordDao().insert(record);
        first.close(); // "app closed"

        HealthMateDatabase second = open(); // "app opened again"
        HealthRecord restored = second.healthRecordDao().getByDate(1, "2026-10-18");
        second.close();

        assertNotNull(restored);
        assertEquals(7850, restored.getSteps());
        assertEquals(450, restored.getSleepMinutes());
    }
}