package com.bretttech.gallery.ui.secure;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bretttech.gallery.R;

public class PinEntryActivity extends AppCompatActivity {

    private EditText pinEditText;
    private Button confirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_entry);

        pinEditText = findViewById(R.id.pin_entry_edit_text);
        confirmButton = findViewById(R.id.pin_entry_confirm_button);

        confirmButton.setOnClickListener(v -> checkPin());
    }

    private void checkPin() {
        String pin = pinEditText.getText().toString();
        if (pin.length() != 4) {
            Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("secure_folder_prefs", Context.MODE_PRIVATE);
        String storedHash = prefs.getString("pin_hash", null);
        String enteredHash = String.valueOf(pin.hashCode());

        if (enteredHash.equals(storedHash)) {
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, R.string.wrong_pin, Toast.LENGTH_SHORT).show();
            pinEditText.setText("");
        }
    }
}