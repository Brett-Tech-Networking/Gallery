package com.bretttech.gallery;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "app_settings_prefs";
    public static final String KEY_THEME = "app_theme";
    public static final String KEY_ICON = "app_icon_alias";
    // UPDATED CONSTANT to store a String (key for the animation type)
    public static final String KEY_ANIMATION_TYPE = "animation_type";

    // NEW CONSTANTS for animation types
    public static final String ANIMATION_OFF = "OFF";
    public static final String ANIMATION_SLIDE = "SLIDE";
    public static final String ANIMATION_FLY = "FLY";
    public static final String ANIMATION_FADE = "FADE";

    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.settings);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupThemeSelector();
        setupIconSelector();
        setupAnimationSelector(); // Renamed call
    }

    // UPDATED HELPER METHOD: Now returns the animation type string.
    public static String getAnimationType(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Default is OFF
        return prefs.getString(KEY_ANIMATION_TYPE, ANIMATION_OFF);
    }

    private void setupThemeSelector() {
        RadioGroup themeGroup = findViewById(R.id.theme_radio_group);
        int currentTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            themeGroup.check(R.id.radio_light);
        } else if (currentTheme == R.id.radio_dark) {
            themeGroup.check(R.id.radio_dark);
        } else {
            themeGroup.check(R.id.radio_system);
        }

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int newThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.radio_light) {
                newThemeMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radio_dark) {
                newThemeMode = AppCompatDelegate.MODE_NIGHT_YES;
            }

            AppCompatDelegate.setDefaultNightMode(newThemeMode);
            prefs.edit().putInt(KEY_THEME, newThemeMode).apply();
        });
    }

    // NEW/UPDATED METHOD
    private void setupAnimationSelector() {
        RadioGroup animationGroup = findViewById(R.id.animation_radio_group);
        // Default is OFF
        String currentAnimation = prefs.getString(KEY_ANIMATION_TYPE, ANIMATION_OFF);

        // Set initial state
        if (currentAnimation.equals(ANIMATION_SLIDE)) {
            animationGroup.check(R.id.radio_animation_slide);
        } else if (currentAnimation.equals(ANIMATION_FLY)) {
            animationGroup.check(R.id.radio_animation_fly);
        } else if (currentAnimation.equals(ANIMATION_FADE)) {
            animationGroup.check(R.id.radio_animation_fade);
        } else {
            animationGroup.check(R.id.radio_animation_off);
        }

        animationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newAnimationType;
            int toastMessageId;

            if (checkedId == R.id.radio_animation_slide) {
                newAnimationType = ANIMATION_SLIDE;
                toastMessageId = R.string.setting_animation_slide;
            } else if (checkedId == R.id.radio_animation_fly) {
                newAnimationType = ANIMATION_FLY;
                toastMessageId = R.string.setting_animation_fly;
            } else if (checkedId == R.id.radio_animation_fade) {
                newAnimationType = ANIMATION_FADE;
                toastMessageId = R.string.setting_animation_fade;
            } else {
                newAnimationType = ANIMATION_OFF;
                toastMessageId = R.string.setting_animation_off;
            }

            prefs.edit().putString(KEY_ANIMATION_TYPE, newAnimationType).apply();
            Toast.makeText(this, getString(toastMessageId) + " Selected", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupIconSelector() {
        findViewById(R.id.icon_default).setOnClickListener(v -> setAppIcon("Default"));
        findViewById(R.id.icon_variant_1).setOnClickListener(v -> setAppIcon("Variant1"));
        findViewById(R.id.icon_variant_2).setOnClickListener(v -> setAppIcon("Variant2"));
    }

    private void setAppIcon(String newAliasSuffix) {
        String currentAliasSuffix = prefs.getString(KEY_ICON, "Default");
        if (currentAliasSuffix.equals(newAliasSuffix)) {
            return; // No change needed
        }

        PackageManager pm = getPackageManager();
        String packageName = getPackageName();

        // Disable the current alias
        pm.setComponentEnabledSetting(
                new ComponentName(packageName, packageName + ".MainActivityLauncher" + currentAliasSuffix),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );

        // Enable the new alias
        pm.setComponentEnabledSetting(
                new ComponentName(packageName, packageName + ".MainActivityLauncher" + newAliasSuffix),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );

        // Save the new alias preference
        prefs.edit().putString(KEY_ICON, newAliasSuffix).apply();

        Toast.makeText(this, R.string.icon_change_toast, Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}