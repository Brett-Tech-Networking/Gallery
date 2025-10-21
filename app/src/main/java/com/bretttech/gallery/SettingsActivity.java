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

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "app_settings_prefs";
    public static final String KEY_THEME = "app_theme";
    public static final String KEY_ICON = "app_icon_alias";
    public static final String KEY_ANIMATION_TYPE = "animation_type";
    public static final String KEY_BOTTOM_NAV_COLOR = "bottom_nav_color"; // New key

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
        setupAnimationSelector();
        setupBottomNavColorSelector(); // New method call
    }

    // New method to handle the BottomNavigationView color selection
    private void setupBottomNavColorSelector() {
        RadioGroup bottomNavColorGroup = findViewById(R.id.bottom_nav_color_radio_group);
        String currentBottomNavColor = prefs.getString(KEY_BOTTOM_NAV_COLOR, "Default");

        if (currentBottomNavColor.equals("Red")) {
            bottomNavColorGroup.check(R.id.radio_bottom_nav_red);
        } else if (currentBottomNavColor.equals("Green")) {
            bottomNavColorGroup.check(R.id.radio_bottom_nav_green);
        } else if (currentBottomNavColor.equals("Blue")) {
            bottomNavColorGroup.check(R.id.radio_bottom_nav_blue);
        } else {
            bottomNavColorGroup.check(R.id.radio_bottom_nav_default);
        }

        bottomNavColorGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newBottomNavColor;
            if (checkedId == R.id.radio_bottom_nav_red) {
                newBottomNavColor = "Red";
            } else if (checkedId == R.id.radio_bottom_nav_green) {
                newBottomNavColor = "Green";
            } else if (checkedId == R.id.radio_bottom_nav_blue) {
                newBottomNavColor = "Blue";
            } else {
                newBottomNavColor = "Default";
            }

            prefs.edit().putString(KEY_BOTTOM_NAV_COLOR, newBottomNavColor).apply();
            Toast.makeText(this, "Bottom navigation color changed. Please restart the app.", Toast.LENGTH_SHORT).show();
        });
    }

    public static String getAnimationType(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ANIMATION_TYPE, ANIMATION_OFF);
    }

    private void setupThemeSelector() {
        RadioGroup themeGroup = findViewById(R.id.theme_radio_group);
        int currentTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            themeGroup.check(R.id.radio_light);
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
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

    private void setupAnimationSelector() {
        RadioGroup animationGroup = findViewById(R.id.animation_radio_group);
        String currentAnimation = prefs.getString(KEY_ANIMATION_TYPE, ANIMATION_OFF);

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

        pm.setComponentEnabledSetting(
                new ComponentName(packageName, packageName + ".MainActivityLauncher" + currentAliasSuffix),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );

        pm.setComponentEnabledSetting(
                new ComponentName(packageName, packageName + ".MainActivityLauncher" + newAliasSuffix),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );

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