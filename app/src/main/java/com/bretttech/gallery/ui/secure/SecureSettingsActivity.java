package com.bretttech.gallery.ui.secure;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.biometric.BiometricManager;

import com.bretttech.gallery.R;

public class SecureSettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "secure_folder_prefs";
    private static final String KEY_BIOMETRICS_ENABLED = "biometrics_enabled";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secure_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_secure_settings);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 1. Change PIN button
        Button changePinButton = findViewById(R.id.btn_change_pin);
        changePinButton.setOnClickListener(v -> {
            // Launch the PinSetupActivity (which can be reused to set/change the PIN)
            Intent intent = new Intent(this, PinSetupActivity.class);
            startActivity(intent);
        });

        // 2. Biometrics Toggle
        SwitchCompat biometricSwitch = findViewById(R.id.switch_biometrics);

        // Check if biometrics are even supported/enrolled before showing switch
        BiometricManager biometricManager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        int canAuthenticate = biometricManager.canAuthenticate(authenticators);

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS || canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            // Biometrics is available or not enrolled (i.e., hardware is there)
            biometricSwitch.setEnabled(true);
            // Default to enabled if not set, as per user's likely expectation for this feature
            biometricSwitch.setChecked(prefs.getBoolean(KEY_BIOMETRICS_ENABLED, true));
        } else {
            // Biometrics is not supported on this device or unavailable
            biometricSwitch.setEnabled(false);
            biometricSwitch.setChecked(false);
            Toast.makeText(this, "Biometrics not supported/available on this device.", Toast.LENGTH_LONG).show();
        }

        biometricSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, isChecked).apply();
            Toast.makeText(this, isChecked ? "Biometrics Enabled" : "Biometrics Disabled", Toast.LENGTH_SHORT).show();
        });
    }

    // Handle the back button (Up button) press on the Action Bar
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}