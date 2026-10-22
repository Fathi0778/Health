package com.example.healthmate.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/*
 * User = one row in the users table (one account on this phone).
 *
 *  id             INTEGER  primary key (auto increment)
 *  username       TEXT     unique, saved in lower case
 *  password_hash  TEXT     the password after hashing (never the real password)
 *  salt           TEXT     random value mixed into the hash
 */
@Entity(
        tableName = "users",
        indices = {@Index(value = {"username"}, unique = true)}
)
public class User {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "username")
    private String username;

    @NonNull
    @ColumnInfo(name = "password_hash")
    private String passwordHash;

    @NonNull
    @ColumnInfo(name = "salt")
    private String salt;

    public User(@NonNull String username, @NonNull String passwordHash, @NonNull String salt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    @NonNull
    public String getPasswordHash() {
        return passwordHash;
    }

    @NonNull
    public String getSalt() {
        return salt;
    }
}