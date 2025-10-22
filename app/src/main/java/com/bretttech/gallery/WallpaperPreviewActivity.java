package com.bretttech.gallery;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private int currentScaleId = R.id.radio_fill;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_preview);

        // This enables the edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // This listener gets the height of the status bar and applies it as padding to the toolbar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
            int insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            // Set the toolbar's height and top padding
            v.getLayoutParams().height = insets + getResources().getDimensionPixelSize(R.dimen.toolbar_height);
            v.setPadding(0, insets, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
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
                        if (scaleOptionsGroup.getCheckedRadioButtonId() == View.NO_ID) {
                            scaleOptionsGroup.check(R.id.radio_fill);
                        } else {
                            updatePreviewScaleType(scaleOptionsGroup.getCheckedRadioButtonId());
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
            currentScaleId = checkedId;
            updatePreviewScaleType(checkedId);
        });
    }

    private void updatePreviewScaleType(int checkedId) {
        ImageView.ScaleType scaleType;
        if (checkedId == R.id.radio_fit) {
            scaleType = ImageView.ScaleType.FIT_CENTER;
        } else if (checkedId == R.id.radio_stretch) {
            scaleType = ImageView.ScaleType.FIT_XY;
        } else { // radio_fill
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
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int targetWidth = metrics.widthPixels;
            int targetHeight = metrics.heightPixels;

            Bitmap finalBitmap = createScaledBitmapForWallpaper(loadedBitmap, targetWidth, targetHeight, currentScaleId);

            wallpaperManager.setBitmap(finalBitmap);

            Toast.makeText(this, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show();
            finish();

        } catch (IOException e) {
            Toast.makeText(this, "Failed to set wallpaper: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "An unexpected error occurred while setting wallpaper.", Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap createScaledBitmapForWallpaper(Bitmap originalBitmap, int targetWidth, int targetHeight, int scaleId) {
        Bitmap finalBitmap = Bitmap.createBitmap(targetWidth, targetHeight, originalBitmap.getConfig());
        Canvas canvas = new Canvas(finalBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);

        float originalWidth = originalBitmap.getWidth();
        float originalHeight = originalBitmap.getHeight();

        if (scaleId == R.id.radio_stretch) {
            RectF src = new RectF(0, 0, originalWidth, originalHeight);
            RectF dst = new RectF(0, 0, targetWidth, targetHeight);
            Matrix matrix = new Matrix();
            matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
            canvas.drawBitmap(originalBitmap, matrix, paint);
        } else {
            float scale;
            float dx = 0, dy = 0;

            if (scaleId == R.id.radio_fit) {
                float scaleX = (float) targetWidth / originalWidth;
                float scaleY = (float) targetHeight / originalHeight;
                scale = Math.min(scaleX, scaleY);
                dx = (targetWidth - originalWidth * scale) / 2f;
                dy = (targetHeight - originalHeight * scale) / 2f;
            } else { // radio_fill (Center Crop)
                float scaleX = (float) targetWidth / originalWidth;
                float scaleY = (float) targetHeight / originalHeight;
                scale = Math.max(scaleX, scaleY);
                dx = (targetWidth - originalWidth * scale) / 2f;
                dy = (targetHeight - originalHeight * scale) / 2f;
            }

            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);
            canvas.drawBitmap(originalBitmap, matrix, paint);
        }

        return finalBitmap;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}