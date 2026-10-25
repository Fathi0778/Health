# HealthMate – Personal Health & Wellness Tracker

Native Android app written in **Java** with **XML layouts**, **MVVM**, **Room (SQLite)**,
**ViewModel + LiveData**, **Navigation (single activity + fragments + BottomNavigationView)**,
**RecyclerView** and **Material Components**. Works completely offline (no INTERNET permission).

Tracks per day: **steps**, **calories burned**, **water (ml)** and **sleep in hours + minutes** (additional feature).

Each person has their own **account** (login / create account). Records and daily goals belong to the logged-in user only.

Extra features: user accounts with hashed passwords, sleep tracker (saved as minutes, entered as hours + minutes), Progress screen with 7-day bar charts, daily goals in Settings, a day-streak badge on the dashboard, quick filter chips (All / Today / This month) and undo after delete.

---

## 1. Toolchain (kept from the original Android Studio project)

| Item | Value |
|---|---|
| Android Gradle Plugin | 9.4.0 |
| Gradle wrapper | 9.6.0 |
| Gradle daemon JDK (toolchain) | 25 |
| Java source/target | 11 |
| compileSdk / targetSdk | 37 / 37 |
| minSdk | 24 (Android 7.0) |
| Package | `com.example.healthmate` |
| Build scripts | Kotlin DSL (`*.gradle.kts`) + version catalog (`gradle/libs.versions.toml`) – the app source is 100% Java |

## 2. Dependencies (all Java-compatible AndroidX / Google libraries)

| Library | Version | Purpose |
|---|---|---|
| androidx.room:room-runtime + room-compiler (annotationProcessor) | 2.7.2 | SQLite database |
| androidx.lifecycle:lifecycle-viewmodel / lifecycle-livedata | 2.9.2 | MVVM state |
| androidx.navigation:navigation-fragment / navigation-ui | 2.9.3 | Fragment navigation + bottom bar |
| androidx.recyclerview:recyclerview | 1.4.0 | Records list |
| com.google.android.material:material | 1.12.0 | Material 3 UI components, DatePicker |
| androidx.appcompat:appcompat | 1.7.1 | AppCompat base |
| androidx.activity:activity-ktx, constraintlayout | 1.8.0, 2.1.4 | Edge-to-edge screen support |
| junit, androidx.test.ext:junit, espresso-core, arch core-testing | test only | Tests |

Charts are drawn by my own `BarChartView` (Canvas API) – no third-party chart library.

**Why Room and not sqflite?** The brief mentions sqflite, which is the SQLite plugin for Flutter/Dart. This app is native Android Java, so it uses **Room**, Google's official library on top of **SQLite**. The data is still stored in an embedded SQLite database (`healthmate_database`) with the `health_records` table. In the same way, **ViewModel + LiveData** is used for state management instead of Flutter packages such as Provider or Riverpod.

## 3. How to open and run

1. Open Android Studio → **File › Open** → select the project folder.
2. Wait for **Gradle Sync** to finish (first sync downloads the libraries above – internet is needed once for that).
3. Create/start an emulator (**Device Manager** → e.g. Pixel, API 34+), or connect a phone with USB debugging.
4. Press **Run ▶** (`app` configuration).
5. First launch shows the **Welcome** screen → tap **Get Started**.
6. Tap **Create account**, choose a username (3–20 letters, numbers or _) and a password (6+ characters). The first account also gets the demo records.
7. Next time the app opens straight to Home (the login is remembered). **Settings › Log out** returns to the login screen.

If Android Studio offers to update library versions, that is optional.

## 4. Architecture

```
UI (Activity + Fragments, XML)  →  HealthRecordViewModel  →  HealthRecordRepository  →  HealthRecordDao  →  SQLite (Room)
          ↑                                                                                                 │
          └──────────────────────────── LiveData (automatic refresh) ───────────────────────────────────────┘

LoginActivity  →  AuthViewModel  →  AuthRepository  →  UserDao  →  SQLite (Room)
                       └→ SessionManager (SharedPreferences: who is logged in)
```

```
app/src/main/java/com/example/healthmate/
├── MainActivity.java              single activity, NavHost + bottom navigation
├── HealthMateApplication.java
├── data/
│   ├── local/   HealthMateDatabase.java, HealthRecordDao.java, UserDao.java, DemoData.java
│   ├── model/   HealthRecord.java, User.java (Room entities)
│   ├── prefs/   GoalPreferences.java, DailyGoals.java, SessionManager.java (SharedPreferences)
│   └── repository/ HealthRecordRepository.java, AuthRepository.java, SaveResult.java
├── viewmodel/   HealthRecordViewModel.java, AuthViewModel.java
├── ui/
│   ├── onboarding/ WelcomeActivity      ├── auth/       LoginActivity
│   ├── dashboard/  DashboardFragment
│   ├── records/    RecordsFragment, HealthRecordAdapter
│   ├── addrecord/  AddRecordFragment    ├── editrecord/ EditRecordFragment
│   ├── progress/   ProgressFragment     ├── sleep/      SleepFragment
│   ├── settings/   SettingsFragment
│   └── common/     RecordFormController, DatePickerHelper, BarChartView, CardBinder, ValidationMessages
└── utils/       DateUtils, ValidationUtils, HealthStats, PasswordUtils, AppExecutors
```

## 5. Database

Database file: `healthmate_database` · Tables: `health_records`, `users`

**health_records**

| Column | Type | Rule |
|---|---|---|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| user_id | INTEGER | NOT NULL, owner of the record (0 = demo row not yet claimed) |
| date | TEXT | NOT NULL, ISO `YYYY-MM-DD`, UNIQUE together with user_id |
| steps | INTEGER | NOT NULL |
| calories | INTEGER | NOT NULL |
| water | INTEGER | NOT NULL (ml) |
| sleep_minutes | INTEGER | NOT NULL (7h 30m is saved as 450) |

**users**

| Column | Type | Rule |
|---|---|---|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| username | TEXT | NOT NULL, UNIQUE, saved in lower case |
| password_hash | TEXT | PBKDF2 hash (the password itself is never saved) |
| salt | TEXT | random 16 bytes per user |

* One record per calendar date **per user** (checked in the repository **and** enforced by the UNIQUE (user_id, date) index). Every query filters by the logged-in user's id, so users never see each other's records.
* 6 demo records (the 6 days before install) are inserted **only** when the database file is first created (`RoomDatabase.Callback.onCreate`, `INSERT OR IGNORE`) with user_id 0. The first account created takes them over (`claimUnownedRecords`).
* All writes run on a background `ExecutorService`; reads are observable `LiveData`.
* Database version 3. `MIGRATION_1_2` converts the old `sleep_hours` column to `sleep_minutes`; `MIGRATION_2_3` adds the `users` table and the `user_id` column. Records saved with older versions are kept.
* Passwords: PBKDF2WithHmacSHA1, 10,000 iterations, 256-bit hash, random salt (`PasswordUtils`). Daily goals are saved per user in SharedPreferences.

## 6. Tests

### Unit tests (JVM) – `app/src/test`
`ValidationUtilsTest`, `DateUtilsTest`, `HealthStatsTest`, `DemoDataTest`, `PasswordUtilsTest` – **37 tests**
Run: right-click `app/src/test/java` → **Run 'Tests in java'**, or `gradlew testDebugUnitTest`.

### Instrumentation tests (emulator) – `app/src/androidTest`
`HealthRecordDaoTest`, `HealthRecordRepositoryTest`, `PersistenceTest`, `AuthRepositoryTest` – **23 tests**
Run with an emulator running: right-click `app/src/androidTest/java` → **Run**, or `gradlew connectedDebugAndroidTest`.

| # | Requirement | Covered by |
|---|---|---|
| 1 | Add a valid record | ValidationUtilsTest, HealthRecordDaoTest.insertValidRecord…, RepositoryTest.insert… |
| 2 | Reject invalid input | ValidationUtilsTest (empty, negative, decimals, text, overflow, sleep range, future date) |
| 3 | Reject duplicate dates | HealthRecordDaoTest.insertDuplicateDate…, RepositoryTest.insertDuplicateDate… / updateToAnotherRecordsDate… |
| 4 | View saved records | HealthRecordDaoTest.observeAll_returnsNewestFirst |
| 5 | Update a record | HealthRecordDaoTest.updateRecord…, RepositoryTest.updateMissingRecord… |
| 6 | Delete a record | HealthRecordDaoTest.deleteRecord_removesOnlyThatRecord |
| 7 | Search by date | HealthRecordDaoTest.searchByDatePrefix… |
| 8 | Dashboard totals | HealthStatsTest.dashboardTotals…, progressPercent…, noRecordToday… |
| 9 | Record sleep (hours + minutes) | HealthRecordDaoTest.sleep_isSavedAsMinutes, HealthStatsTest.sleepStats… |
| 10 | Persist after restart | PersistenceTest |
| 11 | Accounts and login | AuthRepositoryTest, PasswordUtilsTest, ValidationUtilsTest.username_rules / password_rules |
| 12 | Users only see their own data | HealthRecordDaoTest.users_onlySeeTheirOwnRecords / twoUsers_canUseTheSameDate, RepositoryTest.otherUser_cannotDeleteMyRecord |

All 60 tests pass (37 unit + 23 on the Pixel 7 emulator, API 35).

See `docs/testing/manual-test-checklist.md` for the manual emulator checklist.

## 7. Release APK (signed)

No keystore or password is stored in this project.

1. **Build › Generate Signed App Bundle / APK…** → choose **APK** → Next.
2. **Create new…** keystore (store it OUTSIDE the project folder, e.g. `Documents/keys/healthmate.jks`) and remember the passwords.
3. Select the **release** build variant → **Create**.
4. When the build finishes, click **locate**. The default output is
   `app/release/app-release.apk`.
5. Install on a device: drag the APK onto the emulator, or `adb install app/release/app-release.apk`.

`.gitignore` excludes `*.jks`, `*.keystore` and `/app/release` so keys and APKs are never committed.

## 8. Git

The project is tracked with Git, with one commit per development step (`git log --oneline`).
Repository: https://github.com/Fathi0778/Health