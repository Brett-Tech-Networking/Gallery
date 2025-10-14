package com.bretttech.gallery;

import android.content.ComponentName;
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