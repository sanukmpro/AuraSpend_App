package com.auraspend.app.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "auraspend_session";
    private static final String KEY_USER_EMAIL = "current_user_email";
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String email) {
        if (email == null) {
            clearSession();
        } else {
            prefs.edit().putString(KEY_USER_EMAIL, email.trim().toLowerCase()).apply();
        }
    }

    public String getCurrentUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public boolean isLoggedIn() {
        return getCurrentUserEmail() != null;
    }

    public void clearSession() {
        prefs.edit().remove(KEY_USER_EMAIL).apply();
    }
}
