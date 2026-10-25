package com.example.healthmate.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.healthmate.data.local.HealthMateDatabase;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.data.repository.HealthRecordRepository;
import com.example.healthmate.data.repository.SaveResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Checks the Repository rules (uses the real background thread).
 */
@RunWith(AndroidJUnit4.class)
public class HealthRecordRepositoryTest {

    private static final long USER = 1;

    private HealthMateDatabase database;
    private ExecutorService executor;
    private HealthRecordRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, HealthMateDatabase.class).build();
        executor = Executors.newSingleThreadExecutor();
        repository = new HealthRecordRepository(database.healthRecordDao(), executor, USER);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        database.close();
    }

    private SaveResult insert(HealthRecord record) throws InterruptedException {
        AtomicReference<SaveResult> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        repository.insert(record, r -> {
            result.set(r);
            latch.countDown();
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        return result.get();
    }

    private SaveResult update(HealthRecord record) throws InterruptedException {
        AtomicReference<SaveResult> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        repository.update(record, r -> {
            result.set(r);
            latch.countDown();
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        return result.get();
    }

    @Test
    public void insert_returnsSuccessWithNewId() throws Exception {
        SaveResult result = insert(new HealthRecord("2026-10-18", 5000, 300, 1500, 420));
        assertEquals(SaveResult.Status.SUCCESS, result.status);
        assertTrue(result.recordId > 0);
    }

    // Test case 3 (repository): a duplicate date gives back the record that already exists
    @Test
    public void insertDuplicateDate_returnsDuplicateWithExistingId() throws Exception {
        SaveResult first = insert(new HealthRecord("2026-10-18", 5000, 300, 1500, 420));
        SaveResult second = insert(new HealthRecord("2026-10-18", 9999, 999, 999, 540));

        assertEquals(SaveResult.Status.DUPLICATE_DATE, second.status);
        assertEquals(first.recordId, second.recordId);
        // the first values were NOT overwritten
        assertEquals(5000, database.healthRecordDao().getByDate(USER, "2026-10-18").getSteps());
    }

    @Test
    public void updateToAnotherRecordsDate_isRejected() throws Exception {
        insert(new HealthRecord("2026-10-17", 1, 1, 1, 1));
        SaveResult second = insert(new HealthRecord("2026-10-18", 2, 2, 2, 2));

        HealthRecord moved = new HealthRecord("2026-10-17", 2, 2, 2, 2);
        moved.setId(second.recordId);

        assertEquals(SaveResult.Status.DUPLICATE_DATE, update(moved).status);
    }

    @Test
    public void updateMissingRecord_returnsNotFound() throws Exception {
        HealthRecord ghost = new HealthRecord("2026-10-18", 1, 1, 1, 1);
        ghost.setId(12345);
        assertEquals(SaveResult.Status.NOT_FOUND, update(ghost).status);
    }

    // the repository saves every record for its own user
    @Test
    public void insert_setsTheLoggedInUser() throws Exception {
        SaveResult result = insert(new HealthRecord("2026-10-18", 1, 1, 1, 1));
        assertEquals(USER, database.healthRecordDao().getById(USER, result.recordId).getUserId());
    }

    // another user's repository can't delete my record
    @Test
    public void otherUser_cannotDeleteMyRecord() throws Exception {
        SaveResult mine = insert(new HealthRecord("2026-10-18", 1, 1, 1, 1));
        HealthRecord record = database.healthRecordDao().getById(USER, mine.recordId);

        HealthRecordRepository other = new HealthRecordRepository(database.healthRecordDao(), executor, 2);
        AtomicReference<SaveResult> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        other.delete(record, r -> {
            result.set(r);
            latch.countDown();
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(SaveResult.Status.NOT_FOUND, result.get().status);
        assertEquals(1, database.healthRecordDao().count(USER));
    }
}