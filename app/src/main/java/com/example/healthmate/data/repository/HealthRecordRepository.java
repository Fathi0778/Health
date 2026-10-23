package com.example.healthmate.data.repository;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.healthmate.data.local.HealthMateDatabase;
import com.example.healthmate.data.local.HealthRecordDao;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.utils.AppExecutors;

import java.util.List;
import java.util.concurrent.ExecutorService;

/*
 * Repository - the screens never talk to the database directly, they go through here.
 *
 * - Reading gives back LiveData from Room, so the screens update by themselves.
 * - Saving, editing and deleting run on a background thread (not the UI thread)
 *   and the answer comes back as a SaveResult.
 * - The rule "only one record per date" is checked here before saving.
 * - Every repository works for ONE user (the one logged in), so each user
 *   only reads and changes their own records.
 */
public class HealthRecordRepository {

    // receives the result of a background operation (called on the background thread).
    public interface Callback {
        void onComplete(@NonNull SaveResult result);
    }

    private final HealthRecordDao dao;
    private final ExecutorService executor;
    private final long userId;

    public HealthRecordRepository(@NonNull Application application, long userId) {
        this(HealthMateDatabase.getInstance(application).healthRecordDao(), AppExecutors.database(), userId);
    }

    // used by tests to supply an in-memory database and/or a custom executor.
    public HealthRecordRepository(@NonNull HealthRecordDao dao, @NonNull ExecutorService executor, long userId) {
        this.dao = dao;
        this.executor = executor;
        this.userId = userId;
    }

    // ---------------- READ ----------------

    public LiveData<List<HealthRecord>> observeAll() {
        return dao.observeAll(userId);
    }

    public LiveData<List<HealthRecord>> observeByDatePrefix(String datePrefix) {
        return dao.observeByDatePrefix(userId, datePrefix);
    }

    public LiveData<HealthRecord> observeById(long id) {
        return dao.observeById(userId, id);
    }

    public LiveData<HealthRecord> observeByDate(String date) {
        return dao.observeByDate(userId, date);
    }

    public LiveData<List<HealthRecord>> observeBetween(String startDate, String endDate) {
        return dao.observeBetween(userId, startDate, endDate);
    }

    public LiveData<List<HealthRecord>> observeRecent(int limit) {
        return dao.observeRecent(userId, limit);
    }

    // ---------------- CREATE ----------------

    public void insert(@NonNull HealthRecord record, @NonNull Callback callback) {
        record.setUserId(userId);
        executor.execute(() -> {
            try {
                HealthRecord existing = dao.getByDate(userId, record.getDate());
                if (existing != null) {
                    callback.onComplete(SaveResult.duplicate(existing.getId()));
                    return;
                }
                long newId = dao.insert(record);
                callback.onComplete(SaveResult.success(newId));
            } catch (SQLiteConstraintException e) {
                // UNIQUE(user_id, date) safety net
                HealthRecord existing = dao.getByDate(userId, record.getDate());
                callback.onComplete(SaveResult.duplicate(existing != null ? existing.getId() : -1));
            } catch (Exception e) {
                callback.onComplete(SaveResult.error(e.getMessage()));
            }
        });
    }

    // ---------------- UPDATE ----------------

    public void update(@NonNull HealthRecord record, @NonNull Callback callback) {
        record.setUserId(userId);
        executor.execute(() -> {
            try {
                if (dao.getById(userId, record.getId()) == null) {
                    callback.onComplete(SaveResult.notFound());
                    return;
                }
                HealthRecord sameDate = dao.getByDate(userId, record.getDate());
                if (sameDate != null && sameDate.getId() != record.getId()) {
                    callback.onComplete(SaveResult.duplicate(sameDate.getId()));
                    return;
                }
                int rows = dao.update(record); // the primary key (id) is preserved
                callback.onComplete(rows > 0
                        ? SaveResult.success(record.getId())
                        : SaveResult.notFound());
            } catch (SQLiteConstraintException e) {
                callback.onComplete(SaveResult.duplicate(-1));
            } catch (Exception e) {
                callback.onComplete(SaveResult.error(e.getMessage()));
            }
        });
    }

    // ---------------- DELETE ----------------

    public void delete(@NonNull HealthRecord record, @NonNull Callback callback) {
        executor.execute(() -> {
            try {
                if (dao.getById(userId, record.getId()) == null) {
                    callback.onComplete(SaveResult.notFound()); // not this user's record
                    return;
                }
                int rows = dao.delete(record); // deletes by primary key only
                callback.onComplete(rows > 0
                        ? SaveResult.success(record.getId())
                        : SaveResult.notFound());
            } catch (Exception e) {
                callback.onComplete(SaveResult.error(e.getMessage()));
            }
        });
    }
}