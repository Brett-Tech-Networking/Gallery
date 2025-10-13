package com.bretttech.gallery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.filters.FilterListener;
import com.bretttech.gallery.filters.FilterViewAdapter;
import com.bretttech.gallery.text.TextEditorDialogFragment;

import java.io.File;
import java.io.IOException;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;
import ja.burhanrashid52.photoeditor.ViewType;

public class ImageEditActivity extends AppCompatActivity implements OnPhotoEditorListener, View.OnClickListener, FilterListener {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final int READ_WRITE_STORAGE = 52;

    private PhotoEditor mPhotoEditor;
    private PhotoEditorView mPhotoEditorView;
    private RecyclerView mRvFilters;
    private FilterViewAdapter mFilterViewAdapter = new FilterViewAdapter(this);
    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mPhotoEditorView = findViewById(R.id.photoEditorView);
        mRvFilters = findViewById(R.id.filter_recyclerview);

        LinearLayoutManager llmFilters = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRvFilters.setLayoutManager(llmFilters);
        mRvFilters.setAdapter(mFilterViewAdapter);

        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true)
                .build();
        mPhotoEditor.setOnPhotoEditorListener(this);

        mImageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        if (mImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mImageUri);
                mPhotoEditorView.getSource().setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        findViewById(R.id.imgClose).setOnClickListener(this);
        findViewById(R.id.imgSave).setOnClickListener(this);
        findViewById(R.id.imgUndo).setOnClickListener(this);
        findViewById(R.id.imgRedo).setOnClickListener(this);
        findViewById(R.id.imgBrush).setOnClickListener(this);
        findViewById(R.id.imgText).setOnClickListener(this);
        findViewById(R.id.imgFilter).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imgClose) {
            onBackPressed();
        } else if (id == R.id.imgSave) {
            saveImage();
        } else if (id == R.id.imgUndo) {
            mPhotoEditor.undo();
        } else if (id == R.id.imgRedo) {
            mPhotoEditor.redo();
        } else if (id == R.id.imgBrush) {
            mPhotoEditor.setBrushDrawingMode(true);
            Toast.makeText(this, "Brush mode enabled", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.imgText) {
            TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this);
            textEditorDialogFragment.setOnTextEditorListener((inputText, colorCode) -> {
                final TextStyleBuilder styleBuilder = new TextStyleBuilder();
                styleBuilder.withTextColor(colorCode);
                mPhotoEditor.addText(inputText, styleBuilder);
            });
        } else if (id == R.id.imgFilter) {
            mRvFilters.setVisibility(mRvFilters.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
    }

    @SuppressLint("MissingPermission")
    private void saveImage() {
        if (requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    + File.separator + "Gallery" + File.separator + System.currentTimeMillis() + ".png");
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();

                SaveSettings saveSettings = new SaveSettings.Builder()
                        .setClearViewsEnabled(true)
                        .setTransparencyEnabled(false)
                        .build();

                mPhotoEditor.saveAsFile(file.getAbsolutePath(), saveSettings, new PhotoEditor.OnSaveListener() {
                    @Override
                    public void onSuccess(@NonNull String imagePath) {
                        Toast.makeText(ImageEditActivity.this, "Saved successfully!", Toast.LENGTH_SHORT).show();
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(imagePath))));
                    }

                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(ImageEditActivity.this, "Failed to save image", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean requestPermission(String permission) {
        boolean isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
        if (!isGranted) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, READ_WRITE_STORAGE);
        }
        return isGranted;
    }

    @Override
    public void onFilterSelected(PhotoFilter photoFilter) {
        mPhotoEditor.setFilterEffect(photoFilter);
    }

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode) {
        TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this, text, colorCode);
        textEditorDialogFragment.setOnTextEditorListener((inputText, newColorCode) -> {
            final TextStyleBuilder styleBuilder = new TextStyleBuilder();
            styleBuilder.withTextColor(newColorCode);
            mPhotoEditor.editText(rootView, inputText, styleBuilder);
        });
    }

    @Override public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {}
    @Override public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews) {}
    @Override public void onStartViewChangeListener(ViewType viewType) {}
    @Override public void onStopViewChangeListener(ViewType viewType) {}

    // THIS IS THE MISSING METHOD THAT CAUSED THE CRASH
    @Override
    public void onTouchSourceImage(MotionEvent event) {
        // We don't need to do anything here, but the method must be implemented.
    }
}