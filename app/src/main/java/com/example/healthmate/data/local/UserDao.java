package com.example.healthmate.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.healthmate.data.model.User;

/*
 * DAO for the users table. All of these run on a background thread.
 */
@Dao
public interface UserDao {

    // ABORT = throw SQLiteConstraintException if the username is already taken
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(User user);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User findByUsername(String username);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User findById(long id);

    @Query("SELECT COUNT(*) FROM users")
    int count();
}