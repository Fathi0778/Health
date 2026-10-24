package com.example.healthmate;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/*
 * Runs once when the app starts.
 * I only made a light colour theme, so I keep the app in light mode
 * (dark mode would make some text hard to read).
 */
public class HealthMateApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}