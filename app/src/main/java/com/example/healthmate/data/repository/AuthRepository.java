package com.example.healthmate.data.repository;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;

import androidx.annotation.NonNull;

import com.example.healthmate.data.local.HealthMateDatabase;
import com.example.healthmate.data.local.HealthRecordDao;
import com.example.healthmate.data.local.UserDao;
import com.example.healthmate.data.model.User;
import com.example.healthmate.utils.AppExecutors;
import com.example.healthmate.utils.PasswordUtils;

import java.util.Locale;
import java.util.concurrent.ExecutorService;

/*
 * Creating accounts and logging in (runs on a background thread).
 *
 * - usernames are saved in lower case, so "Fathima" and "fathima" are the same account
 * - passwords are hashed with PasswordUtils, the real password is never saved
 * - the first account created also gets the demo records
 */
public class AuthRepository {

    public enum Status { SUCCESS, USERNAME_TAKEN, WRONG_CREDENTIALS, ERROR }

    // the answer of register() or login()
    public static final class AuthResult {
        public final Status status;
        public final long userId;
        public final String username;

        private AuthResult(Status status, long userId, String username) {
            this.status = status;
            this.userId = userId;
            this.username = username;
        }

        static AuthResult success(long userId, String username) {
            return new AuthResult(Status.SUCCESS, userId, username);
        }

        static AuthResult fail(Status status) {
            return new AuthResult(status, -1, null);
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
    }

    public interface Callback {
        void onComplete(@NonNull AuthResult result);
    }

    private final UserDao userDao;
    private final HealthRecordDao recordDao;
    private final ExecutorService executor;

    public AuthRepository(@NonNull Application application) {
        this(HealthMateDatabase.getInstance(application).userDao(),
                HealthMateDatabase.getInstance(application).healthRecordDao(),
                AppExecutors.database());
    }

    // used by tests
    public AuthRepository(@NonNull UserDao userDao, @NonNull HealthRecordDao recordDao,
                          @NonNull ExecutorService executor) {
        this.userDao = userDao;
        this.recordDao = recordDao;
        this.executor = executor;
    }

    public static String normalise(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public void register(@NonNull String username, @NonNull String password, @NonNull Callback callback) {
        String name = normalise(username);
        executor.execute(() -> {
            try {
                if (userDao.findByUsername(name) != null) {
                    callback.onComplete(AuthResult.fail(Status.USERNAME_TAKEN));
                    return;
                }
                boolean firstUser = userDao.count() == 0;
                String salt = PasswordUtils.newSalt();
                long id = userDao.insert(new User(name, PasswordUtils.hash(password, salt), salt));
                if (firstUser) {
                    recordDao.claimUnownedRecords(id); // demo records go to the first account
                }
                callback.onComplete(AuthResult.success(id, name));
            } catch (SQLiteConstraintException e) {
                callback.onComplete(AuthResult.fail(Status.USERNAME_TAKEN)); // UNIQUE(username) safety net
            } catch (Exception e) {
                callback.onComplete(AuthResult.fail(Status.ERROR));
            }
        });
    }

    public void login(@NonNull String username, @NonNull String password, @NonNull Callback callback) {
        String name = normalise(username);
        executor.execute(() -> {
            try {
                User user = userDao.findByUsername(name);
                // same message for "no such user" and "wrong password", so it gives no hints
                if (user == null || !PasswordUtils.matches(password, user.getSalt(), user.getPasswordHash())) {
                    callback.onComplete(AuthResult.fail(Status.WRONG_CREDENTIALS));
                    return;
                }
                callback.onComplete(AuthResult.success(user.getId(), user.getUsername()));
            } catch (Exception e) {
                callback.onComplete(AuthResult.fail(Status.ERROR));
            }
        });
    }
}