package com.bretttech.gallery.ui.secure;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bretttech.gallery.R;

public class PinSetupActivity extends AppCompatActivity {

    private EditText pinEditText;
    private Button confirmButton;
    private TextView titleTextView;
    private TextView subtitleTextView;

    private boolean isConfirming = false;
    private String firstPin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        pinEditText = findViewById(R.id.pin_edit_text);
        confirmButton = findViewById(R.id.pin_confirm_button);
        titleTextView = findViewById(R.id.pin_title);
        subtitleTextView = findViewById(R.id.pin_subtitle);

        confirmButton.setOnClickListener(v -> handlePinConfirmation());
    }

    private void handlePinConfirmation() {
        String pin = pinEditText.getText().toString();
        if (pin.length() != 4) {
            Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isConfirming) {
            firstPin = pin;
            isConfirming = true;
            titleTextView.setText(R.string.confirm_pin);
            subtitleTextView.setText(R.string.confirm_pin);
            pinEditText.setText("");
        } else {
            if (pin.equals(firstPin)) {
                // For simplicity, storing a hash. In a real app, use more secure storage.
                String pinHash = String.valueOf(pin.hashCode());
                SharedPreferences prefs = getSharedPreferences("secure_folder_prefs", Context.MODE_PRIVATE);
                prefs.edit().putString("pin_hash", pinHash).apply();

                Toast.makeText(this, R.string.pin_set_successfully, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, R.string.pins_do_not_match, Toast.LENGTH_SHORT).show();
                isConfirming = false;
                firstPin = "";
                titleTextView.setText(R.string.setup_pin_title);
                subtitleTextView.setText(R.string.enter_new_pin);
                pinEditText.setText("");
            }
        }
    }
}