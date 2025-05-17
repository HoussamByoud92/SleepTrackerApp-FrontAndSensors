package com.sleeptracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.sleeptracker.model.User;

public class SessionManager {
    private final SharedPreferences prefs;
    private final String USER_ID = "user_id";
    private final String EMAIL = "email";
    private final String USERNAME = "username";

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
    }

    public void saveUser(User user) {
        prefs.edit()
                .putInt(USER_ID, user.getId())  // Ensure correct userId is being saved
                .putString(EMAIL, user.getEmail())
                .putString(USERNAME, user.getUsername())
                .apply();
        Log.d("SessionManager", "User ID Saved: " + user.getId());  // Log the saved userId
    }


    public int getUserId() {
        int userId = prefs.getInt(USER_ID, -1);
        Log.d("SessionManager", "User ID Retrieved: " + userId);  // Add this line
        return userId;
    }


    public void logout() {
        prefs.edit().clear().apply();
    }
}
