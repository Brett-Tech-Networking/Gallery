package com.bretttech.gallery.ui.secure;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.biometric.BiometricManager;

import com.bretttech.gallery.R;

public class SecureSettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "secure_folder_prefs";
    private static final String KEY_BIOMETRICS_ENABLED = "biometrics_enabled";
    private static final String KEY_PIN_HASH = "pin_hash";
    private SharedPreferences prefs;
    private SwitchCompat biometricSwitch;

    // Launcher to handle the result from PinSetupActivity
    private final ActivityResultLauncher<Intent> pinSetupLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    // If the user canceled PIN setup, re-enable the biometrics toggle
                    // to prevent being locked out.
                    biometricSwitch.setChecked(true);
                    prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, true).apply();
                    Toast.makeText(this, "PIN setup canceled. Biometrics remain enabled.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secure_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_secure_settings);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        biometricSwitch = findViewById(R.id.switch_biometrics);

        // --- Change PIN Button ---
        Button changePinButton = findViewById(R.id.btn_change_pin);
        changePinButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PinSetupActivity.class);
            startActivity(intent);
        });

        // --- Biometrics Toggle Logic ---
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            // Device supports biometrics and has them enrolled.
            biometricSwitch.setEnabled(true);
            biometricSwitch.setChecked(prefs.getBoolean(KEY_BIOMETRICS_ENABLED, true));
        } else {
            // No biometrics available/enrolled. Force switch off and disable it.
            biometricSwitch.setEnabled(false);
            biometricSwitch.setChecked(false);
            prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, false).apply();
            Toast.makeText(this, "Biometrics not enrolled or available.", Toast.LENGTH_LONG).show();
        }

        biometricSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, isChecked).apply();
            Toast.makeText(this, isChecked ? "Biometrics Enabled" : "Biometrics Disabled", Toast.LENGTH_SHORT).show();

            // If user disables biometrics, ensure an in-app PIN exists.
            if (!isChecked && !prefs.contains(KEY_PIN_HASH)) {
                Toast.makeText(this, "Please set up an in-app PIN to continue.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, PinSetupActivity.class);
                pinSetupLauncher.launch(intent);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}