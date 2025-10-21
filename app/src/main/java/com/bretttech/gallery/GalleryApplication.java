package com.bretttech.gallery;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class GalleryApplication extends Application {

    public static boolean isSecureFolderUnlocked = false;
    public static boolean isAppInForeground = true;
    private int startedActivityCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply saved theme preference
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int themeMode = prefs.getInt(SettingsActivity.KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // Register lifecycle callbacks to detect app foreground/background changes instantly
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivityCount++;
                if (startedActivityCount > 0) {
                    isAppInForeground = true;
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivityCount--;
                if (startedActivityCount <= 0) {
                    // App has no visible activities — lock immediately
                    isAppInForeground = false;
                    isSecureFolderUnlocked = false;
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }
}
