package com.bretttech.gallery;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class GalleryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int themeMode = prefs.getInt(SettingsActivity.KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
}