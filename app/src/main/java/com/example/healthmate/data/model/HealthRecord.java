package com.example.healthmate.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/*
 * HealthRecord = one row in the health_records table.
 *
 *  id            INTEGER  primary key (auto increment)
 *  user_id       INTEGER  which user the record belongs to (users.id)
 *  date          TEXT     yyyy-MM-dd, one record per day for each user
 *  steps         INTEGER
 *  calories      INTEGER
 *  water         INTEGER  in ml
 *  sleep_minutes INTEGER  total sleep in minutes (7h 30m = 450)
 *
 * I save sleep as minutes so I don't have decimal problems.
 * The screens show it as hours and minutes.
 */
@Entity(
        tableName = "health_records",
        indices = {@Index(value = {"user_id", "date"}, unique = true)}
)
public class HealthRecord {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "user_id")
    private long userId;

    @NonNull
    @ColumnInfo(name = "date")
    private String date;

    @ColumnInfo(name = "steps")
    private int steps;

    @ColumnInfo(name = "calories")
    private int calories;

    @ColumnInfo(name = "water")
    private int water;

    @ColumnInfo(name = "sleep_minutes")
    private int sleepMinutes;

    // Room uses this constructor
    public HealthRecord(@NonNull String date, int steps, int calories, int water, int sleepMinutes) {
        this.date = date;
        this.steps = steps;
        this.calories = calories;
        this.water = water;
        this.sleepMinutes = sleepMinutes;
    }

    // easier constructor for the form: hours and minutes separately
    @Ignore
    public HealthRecord(@NonNull String date, int steps, int calories, int water,
                        int sleepHours, int sleepExtraMinutes) {
        this(date, steps, calories, water, sleepHours * 60 + sleepExtraMinutes);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    public void setDate(@NonNull String date) {
        this.date = date;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public int getWater() {
        return water;
    }

    public void setWater(int water) {
        this.water = water;
    }

    public int getSleepMinutes() {
        return sleepMinutes;
    }

    public void setSleepMinutes(int sleepMinutes) {
        this.sleepMinutes = sleepMinutes;
    }

    // sleep as decimal hours (450 min -> 7.5), used for charts and goals
    public double getSleepHours() {
        return sleepMinutes / 60.0;
    }

    // used by the list adapter to check if a card changed
    public boolean hasSameContent(HealthRecord other) {
        return other != null
                && id == other.id
                && date.equals(other.date)
                && steps == other.steps
                && calories == other.calories
                && water == other.water
                && sleepMinutes == other.sleepMinutes;
    }

    // copy of the record (used for undo after delete)
    public HealthRecord copy() {
        HealthRecord copy = new HealthRecord(date, steps, calories, water, sleepMinutes);
        copy.setId(id);
        copy.setUserId(userId);
        return copy;
    }
}