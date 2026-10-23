package com.example.healthmate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.data.prefs.DailyGoals;
import com.example.healthmate.data.prefs.GoalPreferences;
import com.example.healthmate.data.prefs.SessionManager;
import com.example.healthmate.data.repository.HealthRecordRepository;
import com.example.healthmate.data.repository.SaveResult;
import com.example.healthmate.utils.DateUtils;

import java.util.List;
import java.util.Objects;

/*
 * ViewModel - keeps the data for the screens.
 *
 * All fragments share this one ViewModel (it belongs to MainActivity),
 * and it is not lost when the phone is rotated.
 *
 * Fragment -> ViewModel -> Repository -> DAO -> SQLite
 * When the table changes: SQLite -> LiveData -> Fragment updates by itself
 *
 * It only works with the records and goals of the user who is logged in.
 */
public class HealthRecordViewModel extends AndroidViewModel {

    public static final int RECENT_LIMIT = 3;
    public static final int WEEK_DAYS = 7;

    private final HealthRecordRepository repository;
    private final GoalPreferences goalPreferences;
    private final SessionManager session;
    private final long userId;

    // Inputs (things the UI changes)
    private final MutableLiveData<String> today = new MutableLiveData<>(DateUtils.today());
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> dateToCheck = new MutableLiveData<>();
    private final MutableLiveData<DailyGoals> goals = new MutableLiveData<>();

    // Outputs (things the UI observes)
    private final LiveData<List<HealthRecord>> allRecords;
    private final LiveData<List<HealthRecord>> filteredRecords;
    private final LiveData<List<HealthRecord>> recentRecords;
    private final LiveData<List<HealthRecord>> lastSevenDaysRecords;
    private final LiveData<HealthRecord> todayRecord;
    private final LiveData<HealthRecord> recordForCheckedDate;

    public HealthRecordViewModel(@NonNull Application application) {
        super(application);
        session = new SessionManager(application);
        userId = session.getUserId();
        repository = new HealthRecordRepository(application, userId);
        goalPreferences = new GoalPreferences(application);
        goals.setValue(goalPreferences.getGoals(userId));

        allRecords = repository.observeAll();
        recentRecords = repository.observeRecent(RECENT_LIMIT);

        // switchMap: whenever the input changes, switch to a new Room query.
        filteredRecords = Transformations.switchMap(searchQuery, query ->
                query == null || query.isEmpty()
                        ? repository.observeAll()
                        : repository.observeByDatePrefix(query));

        todayRecord = Transformations.switchMap(today, repository::observeByDate);

        lastSevenDaysRecords = Transformations.switchMap(today, day ->
                repository.observeBetween(DateUtils.addDays(day, -(WEEK_DAYS - 1)), day));

        recordForCheckedDate = Transformations.switchMap(dateToCheck, repository::observeByDate);
    }

    // ---------------- Dates ----------------

    public LiveData<String> getToday() {
        return today;
    }

    // call from onResume so "today" stays correct if the app is left open past midnight.
    public void refreshToday() {
        String now = DateUtils.today();
        if (!now.equals(today.getValue())) {
            today.setValue(now);
        }
    }

    // ---------------- Records ----------------

    public LiveData<List<HealthRecord>> getAllRecords() {
        return allRecords;
    }

    public LiveData<List<HealthRecord>> getFilteredRecords() {
        return filteredRecords;
    }

    public LiveData<List<HealthRecord>> getRecentRecords() {
        return recentRecords;
    }

    public LiveData<List<HealthRecord>> getLastSevenDaysRecords() {
        return lastSevenDaysRecords;
    }

    public LiveData<HealthRecord> getTodayRecord() {
        return todayRecord;
    }

    public LiveData<HealthRecord> getRecordById(long id) {
        return repository.observeById(id);
    }

    // ---------------- Search ----------------

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    // keeps only digits and '-' so the text is safe to use in a LIKE query.
    public void setSearchQuery(String rawQuery) {
        String cleaned = rawQuery == null ? "" : rawQuery.replaceAll("[^0-9-]", "");
        if (!Objects.equals(cleaned, searchQuery.getValue())) {
            searchQuery.setValue(cleaned);
        }
    }

    public void clearSearch() {
        setSearchQuery("");
    }

    // ---------------- Duplicate-date check (Add screen) ----------------

    public void checkDate(String isoDate) {
        if (!Objects.equals(isoDate, dateToCheck.getValue())) {
            dateToCheck.setValue(isoDate);
        }
    }

    // the record that already exists for the date being checked, or null.
    public LiveData<HealthRecord> getRecordForCheckedDate() {
        return recordForCheckedDate;
    }

    // ---------------- CRUD (results arrive asynchronously) ----------------

    public LiveData<SaveResult> insert(@NonNull HealthRecord record) {
        MutableLiveData<SaveResult> result = new MutableLiveData<>();
        repository.insert(record, result::postValue);
        return result;
    }

    public LiveData<SaveResult> update(@NonNull HealthRecord record) {
        MutableLiveData<SaveResult> result = new MutableLiveData<>();
        repository.update(record, result::postValue);
        return result;
    }

    public LiveData<SaveResult> delete(@NonNull HealthRecord record) {
        MutableLiveData<SaveResult> result = new MutableLiveData<>();
        repository.delete(record, result::postValue);
        return result;
    }

    // ---------------- Goals (SharedPreferences) ----------------

    public LiveData<DailyGoals> getGoals() {
        return goals;
    }

    public DailyGoals getCurrentGoals() {
        DailyGoals value = goals.getValue();
        return value != null ? value : DailyGoals.defaults();
    }

    public void saveGoals(@NonNull DailyGoals newGoals) {
        goalPreferences.saveGoals(userId, newGoals);
        goals.setValue(newGoals);
    }

    public void resetGoals() {
        saveGoals(DailyGoals.defaults());
    }

    public void setOnboardingComplete(boolean complete) {
        goalPreferences.setOnboardingComplete(complete);
    }

    // ---------------- Account ----------------

    public String getUsername() {
        return session.getUsername();
    }

    public void logout() {
        session.logout();
    }
}