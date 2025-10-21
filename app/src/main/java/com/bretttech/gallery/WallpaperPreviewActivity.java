package com.bretttech.gallery;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Canvas; // NEW IMPORT
import android.graphics.Matrix; // NEW IMPORT
import android.graphics.Paint; // NEW IMPORT
import android.graphics.RectF; // NEW IMPORT
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics; // NEW IMPORT
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.IOException;

public class WallpaperPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    private Uri imageUri;
    private ImageView previewImageView;
    private RadioGroup scaleOptionsGroup;
    private Bitmap loadedBitmap;

    // Store the current scale choice ID
    private int currentScaleId = R.id.radio_fill;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_preview);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.dialog_wallpaper_title);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        previewImageView = findViewById(R.id.wallpaper_preview_image);
        scaleOptionsGroup = findViewById(R.id.scale_options_group);
        Button setWallpaperButton = findViewById(R.id.button_set_wallpaper);

        loadPreviewImage();
        setupScaleOptions();

        setWallpaperButton.setOnClickListener(v -> setWallpaper());
    }

    private void loadPreviewImage() {
        if (imageUri == null) {
            Toast.makeText(this, "Image data is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Glide.with(this)
                .asBitmap()
                .load(imageUri)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        loadedBitmap = resource;
                        previewImageView.setImageBitmap(resource);
                        // Manually check the default radio button if one is available
                        if (scaleOptionsGroup.getCheckedRadioButtonId() == View.NO_ID) {
                            scaleOptionsGroup.check(R.id.radio_fill);
                        } else {
                            // Apply the initial scale type
                            updatePreviewScaleType(R.id.radio_fill);
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        loadedBitmap = null;
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        Toast.makeText(WallpaperPreviewActivity.this, "Failed to load image for preview.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void setupScaleOptions() {
        scaleOptionsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            currentScaleId = checkedId; // Update the stored selection
            updatePreviewScaleType(checkedId);
        });
    }

    private void updatePreviewScaleType(int checkedId) {
        ImageView.ScaleType scaleType;
        if (checkedId == R.id.radio_fit) {
            scaleType = ImageView.ScaleType.FIT_CENTER;
        } else if (checkedId == R.id.radio_stretch) {
            scaleType = ImageView.ScaleType.FIT_XY;
        } else {
            // radio_fill
            scaleType = ImageView.ScaleType.CENTER_CROP;
        }
        previewImageView.setScaleType(scaleType);
    }

    private void setWallpaper() {
        if (loadedBitmap == null) {
            Toast.makeText(this, "Image is still loading or failed to load.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());

            // Get screen dimensions to know the target size for the wallpaper bitmap
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int targetWidth = metrics.widthPixels;
            int targetHeight = metrics.heightPixels;

            // Use the determined scale ID to create the properly scaled bitmap
            Bitmap finalBitmap = createScaledBitmapForWallpaper(loadedBitmap, targetWidth, targetHeight, currentScaleId);

            // Set the correctly scaled bitmap
            wallpaperManager.setBitmap(finalBitmap);

            Toast.makeText(this, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show();
            finish();

        } catch (IOException e) {
            Toast.makeText(this, "Failed to set wallpaper: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "An unexpected error occurred while setting wallpaper.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Creates a new bitmap scaled and positioned according to the user's selected option.
     * This is necessary because WallpaperManager.setBitmap() doesn't support ScaleType.
     *
     * @param originalBitmap The source bitmap.
     * @param targetWidth The device screen width.
     * @param targetHeight The device screen height.
     * @param scaleId The selected radio button ID (R.id.radio_fill, R.id.radio_fit, R.id.radio_stretch).
     * @return The correctly scaled/padded bitmap.
     */
    private Bitmap createScaledBitmapForWallpaper(Bitmap originalBitmap, int targetWidth, int targetHeight, int scaleId) {
        // 1. Create a new mutable target bitmap with the exact screen size (or slightly larger for scrolling)
        Bitmap finalBitmap = Bitmap.createBitmap(targetWidth, targetHeight, originalBitmap.getConfig());
        Canvas canvas = new Canvas(finalBitmap);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);

        float scale;
        float dx = 0, dy = 0;
        float originalWidth = originalBitmap.getWidth();
        float originalHeight = originalBitmap.getHeight();

        if (scaleId == R.id.radio_fit) {
            // Fit (Letterbox/Pillarbox): Calculate scale to fit both dimensions, centered.
            float scaleX = targetWidth / originalWidth;
            float scaleY = targetHeight / originalHeight;
            scale = Math.min(scaleX, scaleY);

            dx = (targetWidth - originalWidth * scale) / 2;
            dy = (targetHeight - originalHeight * scale) / 2;

        } else if (scaleId == R.id.radio_stretch) {
            // Stretch (FIT_XY): Scale X and Y independently to fill the entire target area.
            scale = 1.0f; // Scale is handled by the matrix setRectToRect

            // Use Matrix and setRectToRect for precise FIT_XY scaling
            Matrix matrix = new Matrix();
            RectF src = new RectF(0, 0, originalWidth, originalHeight);
            RectF dst = new RectF(0, 0, targetWidth, targetHeight);
            matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);

            canvas.drawBitmap(originalBitmap, matrix, paint);
            return finalBitmap;

        } else { // R.id.radio_fill (Center Crop)
            // Fill (CENTER_CROP): Calculate scale to fill the smallest dimension, then center/crop.
            float scaleX = targetWidth / originalWidth;
            float scaleY = targetHeight / originalHeight;
            scale = Math.max(scaleX, scaleY);

            dx = (targetWidth - originalWidth * scale) / 2;
            dy = (targetHeight - originalHeight * scale) / 2;
        }

        // Apply scaling and translation
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);

        // Draw the scaled bitmap onto the final canvas
        canvas.drawBitmap(originalBitmap, matrix, paint);

        return finalBitmap;
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