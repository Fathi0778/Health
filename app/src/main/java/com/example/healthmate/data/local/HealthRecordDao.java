package com.example.healthmate.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.healthmate.data.model.HealthRecord;

import java.util.List;

/*
 * DAO - all the SQL queries for the health_records table.
 *
 * Every read has "WHERE user_id = :userId", so each user only sees
 * their own records.
 *
 * Queries that return LiveData are watched by the screens. When the table changes,
 * Room runs the query again and the screen gets the new list.
 * The others must run on a background thread.
 */
@Dao
public interface HealthRecordDao {

    // ---------- CREATE ----------

    // ABORT = throw SQLiteConstraintException if this user already has a record for the date.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(HealthRecord record);

    // ---------- UPDATE ----------

    @Update(onConflict = OnConflictStrategy.ABORT)
    int update(HealthRecord record);

    // demo records (user_id 0) are given to the first account that is created
    @Query("UPDATE health_records SET user_id = :userId WHERE user_id = 0")
    int claimUnownedRecords(long userId);

    // ---------- DELETE ----------

    @Delete
    int delete(HealthRecord record);

    // ---------- READ (observable) ----------

    @Query("SELECT * FROM health_records WHERE user_id = :userId ORDER BY date DESC")
    LiveData<List<HealthRecord>> observeAll(long userId);

    // search: "2026-10" matches every day in October 2026, "2026-10-18" one day.
    @Query("SELECT * FROM health_records WHERE user_id = :userId AND date LIKE :datePrefix || '%' "
            + "ORDER BY date DESC")
    LiveData<List<HealthRecord>> observeByDatePrefix(long userId, String datePrefix);

    @Query("SELECT * FROM health_records WHERE user_id = :userId AND id = :id LIMIT 1")
    LiveData<HealthRecord> observeById(long userId, long id);

    @Query("SELECT * FROM health_records WHERE user_id = :userId AND date = :date LIMIT 1")
    LiveData<HealthRecord> observeByDate(long userId, String date);

    // inclusive date range, oldest first (used by charts).
    @Query("SELECT * FROM health_records WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate "
            + "ORDER BY date ASC")
    LiveData<List<HealthRecord>> observeBetween(long userId, String startDate, String endDate);

    @Query("SELECT * FROM health_records WHERE user_id = :userId ORDER BY date DESC LIMIT :limit")
    LiveData<List<HealthRecord>> observeRecent(long userId, int limit);

    // ---------- READ (one-off, background thread only) ----------

    @Query("SELECT * FROM health_records WHERE user_id = :userId AND id = :id LIMIT 1")
    HealthRecord getById(long userId, long id);

    @Query("SELECT * FROM health_records WHERE user_id = :userId AND date = :date LIMIT 1")
    HealthRecord getByDate(long userId, String date);

    @Query("SELECT * FROM health_records WHERE user_id = :userId ORDER BY date DESC")
    List<HealthRecord> getAll(long userId);

    @Query("SELECT * FROM health_records WHERE user_id = :userId AND date LIKE :datePrefix || '%' "
            + "ORDER BY date DESC")
    List<HealthRecord> searchByDatePrefix(long userId, String datePrefix);

    @Query("SELECT * FROM health_records WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate "
            + "ORDER BY date ASC")
    List<HealthRecord> getBetween(long userId, String startDate, String endDate);

    @Query("SELECT COUNT(*) FROM health_records WHERE user_id = :userId")
    int count(long userId);
}