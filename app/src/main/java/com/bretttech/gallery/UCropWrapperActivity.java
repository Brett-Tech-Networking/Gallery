package com.bretttech.gallery;

import android.os.Bundle;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.yalantis.ucrop.UCropActivity;
import com.yalantis.ucrop.R;

/**
 * Thin wrapper around UCropActivity that forces the window to fit system bars
 * before UCrop inflates its own views. This prevents the UCrop toolbar from
 * rendering behind the status bar on Android 15+ edge-to-edge devices.
 */
public class UCropWrapperActivity extends UCropActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        super.onCreate(savedInstanceState);
        applyToolbarInsets();
    }

    private void applyToolbarInsets() {
        View content = findViewById(android.R.id.content);
        View toolbar = findViewById(R.id.toolbar);
        if (content == null || toolbar == null) {
            return;
        }

        final int basePaddingTop = toolbar.getPaddingTop();
        final int basePaddingBottom = toolbar.getPaddingBottom();
        final int basePaddingLeft = toolbar.getPaddingLeft();
        final int basePaddingRight = toolbar.getPaddingRight();

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            toolbar.setPadding(
                    basePaddingLeft,
                    basePaddingTop + statusBars.top,
                    basePaddingRight,
                    basePaddingBottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
