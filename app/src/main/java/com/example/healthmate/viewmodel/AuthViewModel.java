package com.example.healthmate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthmate.data.prefs.SessionManager;
import com.example.healthmate.data.repository.AuthRepository;

/*
 * ViewModel for the login screen.
 * LoginActivity -> AuthViewModel -> AuthRepository -> UserDao -> SQLite
 * When login or register works, the user is saved in the session.
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final SessionManager session;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
        session = new SessionManager(application);
    }

    public LiveData<AuthRepository.AuthResult> login(String username, String password) {
        MutableLiveData<AuthRepository.AuthResult> result = new MutableLiveData<>();
        repository.login(username, password, r -> {
            if (r.isSuccess()) {
                session.login(r.userId, r.username);
            }
            result.postValue(r);
        });
        return result;
    }

    public LiveData<AuthRepository.AuthResult> register(String username, String password) {
        MutableLiveData<AuthRepository.AuthResult> result = new MutableLiveData<>();
        repository.register(username, password, r -> {
            if (r.isSuccess()) {
                session.login(r.userId, r.username);
            }
            result.postValue(r);
        });
        return result;
    }
}