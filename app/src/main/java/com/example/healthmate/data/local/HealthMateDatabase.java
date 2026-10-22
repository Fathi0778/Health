package com.example.healthmate.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.data.model.User;
import com.example.healthmate.utils.DateUtils;

/*
 * The Room database. It is saved on the phone as a SQLite file called "healthmate_database".
 *
 * Only one object of this class is made for the whole app (singleton),
 * because opening the database many times is slow.
 *
 * Version 1 saved sleep as decimal hours (sleep_hours).
 * Version 2 saves sleep as minutes (sleep_minutes).
 * Version 3 adds the users table and a user_id column on every record (login).
 * The migrations change old data to the new format, so nobody loses their
 * records when they update the app.
 */
@Database(entities = {HealthRecord.class, User.class}, version = 3, exportSchema = false)
public abstract class HealthMateDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "healthmate_database";

    private static volatile HealthMateDatabase instance;

    public abstract HealthRecordDao healthRecordDao();

    public abstract UserDao userDao();

    public static HealthMateDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (HealthMateDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    HealthMateDatabase.class,
                                    DATABASE_NAME)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .addCallback(ADD_DEMO_DATA)
                            .build();
                }
            }
        }
        return instance;
    }

    // onCreate only runs the first time the database file is made,
    // so the demo data never replaces my real data.
    // Demo rows have user_id 0 and are given to the first account created.
    private static final RoomDatabase.Callback ADD_DEMO_DATA = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            for (Object[] row : DemoData.buildRows(DateUtils.today())) {
                db.execSQL(DemoData.INSERT_SQL, row);
            }
        }
    };

    // sleep_hours (REAL) -> sleep_minutes (INTEGER)
    // SQLite can't change a column type, so I make a new table, copy the rows,
    // delete the old table and rename the new one.
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `health_records_new` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`date` TEXT NOT NULL, "
                    + "`steps` INTEGER NOT NULL, "
                    + "`calories` INTEGER NOT NULL, "
                    + "`water` INTEGER NOT NULL, "
                    + "`sleep_minutes` INTEGER NOT NULL)");
            db.execSQL("INSERT INTO `health_records_new` (id, date, steps, calories, water, sleep_minutes) "
                    + "SELECT id, date, steps, calories, water, CAST(ROUND(sleep_hours * 60) AS INTEGER) "
                    + "FROM `health_records`");
            db.execSQL("DROP TABLE `health_records`");
            db.execSQL("ALTER TABLE `health_records_new` RENAME TO `health_records`");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_health_records_date` "
                    + "ON `health_records` (`date`)");
        }
    };

    // adds the users table and gives every record an owner (user_id).
    // Old records get user_id 0 and the first account created takes them.
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `users` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`username` TEXT NOT NULL, "
                    + "`password_hash` TEXT NOT NULL, "
                    + "`salt` TEXT NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `health_records_new` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`user_id` INTEGER NOT NULL, "
                    + "`date` TEXT NOT NULL, "
                    + "`steps` INTEGER NOT NULL, "
                    + "`calories` INTEGER NOT NULL, "
                    + "`water` INTEGER NOT NULL, "
                    + "`sleep_minutes` INTEGER NOT NULL)");
            db.execSQL("INSERT INTO `health_records_new` (id, user_id, date, steps, calories, water, sleep_minutes) "
                    + "SELECT id, 0, date, steps, calories, water, sleep_minutes FROM `health_records`");
            db.execSQL("DROP TABLE `health_records`");
            db.execSQL("ALTER TABLE `health_records_new` RENAME TO `health_records`");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_health_records_user_id_date` "
                    + "ON `health_records` (`user_id`, `date`)");
        }
    };
}