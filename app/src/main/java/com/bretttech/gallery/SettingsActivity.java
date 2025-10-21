package com.bretttech.gallery;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.github.dhaval2404.colorpicker.ColorPickerDialog;
import com.github.dhaval2404.colorpicker.listener.ColorListener;
import com.github.dhaval2404.colorpicker.model.ColorShape;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "app_settings_prefs";
    public static final String KEY_THEME = "app_theme";
    public static final String KEY_ICON = "app_icon_alias";
    public static final String KEY_ANIMATION_TYPE = "animation_type";
    public static final String KEY_BOTTOM_NAV_COLOR = "bottom_nav_color";

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
        setupBottomNavColorPicker();
    }

    private void setupBottomNavColorPicker() {
        Button pickColorButton = findViewById(R.id.pick_color_button);
        pickColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ColorPickerDialog.Builder(SettingsActivity.this)
                        .setTitle("Pick a color")
                        .setColorShape(ColorShape.SQAURE)
                        .setDefaultColor(prefs.getInt(KEY_BOTTOM_NAV_COLOR, R.color.purple_500))
                        .setColorListener(new ColorListener() {
                            @Override
                            public void onColorSelected(int color, String colorHex) {
                                prefs.edit().putInt(KEY_BOTTOM_NAV_COLOR, color).apply();
                                showRestartDialog();
                            }
                        })
                        .show();
            }
        });
    }

    private void showRestartDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Restart Required")
                .setMessage("A restart is required for the changes to take effect. Restart now?")
                .setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        restartApp();
                    }
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void restartApp() {
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
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