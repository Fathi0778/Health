package com.example.healthmate.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/*
 * Remembers who is logged in (saved in SharedPreferences),
 * so the user does not have to log in every time the app opens.
 */
public class SessionManager {

    private static final String PREFS_NAME = "healthmate_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final long NO_USER = -1;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return getUserId() != NO_USER;
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, NO_USER);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public void login(long userId, String username) {
        prefs.edit().putLong(KEY_USER_ID, userId).putString(KEY_USERNAME, username).apply();
    }

    public void logout() {
        prefs.edit().clear().apply();
    }

    // "fathima" -> "Fathima" (usernames are saved in lower case)
    public static String displayName(String username) {
        if (username == null || username.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(username.charAt(0)) + username.substring(1);
    }
}