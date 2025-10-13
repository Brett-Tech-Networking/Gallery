package com.bretttech.gallery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bretttech.gallery.filters.FilterListener;
import com.bretttech.gallery.filters.FilterViewAdapter;
import com.bretttech.gallery.text.TextEditorDialogFragment;
import com.yalantis.ucrop.UCrop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
    private Uri mImageUri;
    private Bitmap originalBitmap;
    private Bitmap adjustedBitmap;

    private View mainToolNavigation;
    private View toolPropertiesContainer;
    private View adjustToolsPanel;
    private RecyclerView filterRecyclerView;
    private RecyclerView adjustmentsRecyclerView;
    private SeekBar adjustmentSeekBar;
    private TextView sliderValueText;
    private TextView adjustLabel;

    private String currentAdjustmentType = "Brightness";
    private float brightnessValue = 0f;
    private float contrastValue = 1f;
    private float saturationValue = 1f;

    private final ActivityResultLauncher<Intent> uCropLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            final Uri resultUri = UCrop.getOutput(result.getData());
            if (resultUri != null) {
                mImageUri = resultUri;
                loadBitmapFromUri(mImageUri);
            }
        } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(result.getData());
            Toast.makeText(this, "Crop error: " + cropError, Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        initViews();
        setupPhotoEditor();
        setupListeners();
        setupOnBackPressed();
        mImageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        if (mImageUri != null) {
            loadBitmapFromUri(mImageUri);
        }
    }

    private void initViews() {
        mPhotoEditorView = findViewById(R.id.photoEditorView);
        mainToolNavigation = findViewById(R.id.main_tool_navigation);
        toolPropertiesContainer = findViewById(R.id.tool_properties_container);
        adjustToolsPanel = findViewById(R.id.adjust_tools_panel);
        filterRecyclerView = findViewById(R.id.filter_recyclerview);

        adjustmentsRecyclerView = adjustToolsPanel.findViewById(R.id.adjustments_recycler_view);
        adjustmentSeekBar = adjustToolsPanel.findViewById(R.id.adjustment_seekbar);
        sliderValueText = adjustToolsPanel.findViewById(R.id.slider_value_text);
        adjustLabel = adjustToolsPanel.findViewById(R.id.adjust_title);

        filterRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupPhotoEditor() {
        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView).build();
        mPhotoEditor.setOnPhotoEditorListener(this);
    }

    private void loadBitmapFromUri(Uri uri) {
        try {
            Bitmap loadedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ExifInterface exifInterface = new ExifInterface(inputStream);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) matrix.postRotate(90);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) matrix.postRotate(180);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) matrix.postRotate(270);
            originalBitmap = Bitmap.createBitmap(loadedBitmap, 0, 0, loadedBitmap.getWidth(), loadedBitmap.getHeight(), matrix, true);
            adjustedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);

            // FINAL FIX: This is the correct way to handle the image display.
            mPhotoEditorView.getSource().setImageBitmap(originalBitmap);
            mPhotoEditorView.getSource().setScaleType(ImageView.ScaleType.FIT_CENTER);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        findViewById(R.id.imgClose).setOnClickListener(this);
        findViewById(R.id.tool_adjust).setOnClickListener(this);
        findViewById(R.id.tool_filters).setOnClickListener(this);
        findViewById(R.id.tool_crop_rotate).setOnClickListener(this);

        // Listeners for the new buttons inside the adjustment panel
        adjustToolsPanel.findViewById(R.id.btnSaveAdjust).setOnClickListener(v -> saveImage());
        adjustToolsPanel.findViewById(R.id.btnCancelAdjust).setOnClickListener(v -> finish());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imgClose) {
            finish();
        } else if (id == R.id.tool_adjust) {
            setupAdjustmentsPanel();
            showToolPanel(adjustToolsPanel);
        } else if (id == R.id.tool_filters) {
            if (originalBitmap != null) {
                FilterViewAdapter filterAdapter = new FilterViewAdapter(this, originalBitmap);
                filterRecyclerView.setAdapter(filterAdapter);
                showToolPanel(filterRecyclerView);
            }
        } else if (id == R.id.tool_crop_rotate) {
            startCrop(mImageUri);
        }
    }

    private void showToolPanel(View toolToShow) {
        mainToolNavigation.setVisibility(View.GONE);
        toolPropertiesContainer.setVisibility(View.VISIBLE);
        adjustToolsPanel.setVisibility(View.GONE);
        filterRecyclerView.setVisibility(View.GONE);
        toolToShow.setVisibility(View.VISIBLE);
    }

    private void hideAllToolPanels() {
        // Reset the view to the last saved state before hiding panels
        mPhotoEditorView.getSource().setImageBitmap(originalBitmap);
        // Also reset any adjustment values
        brightnessValue = 0f;
        contrastValue = 1f;
        saturationValue = 1f;

        toolPropertiesContainer.setVisibility(View.GONE);
        mainToolNavigation.setVisibility(View.VISIBLE);
    }

    private void setupAdjustmentsPanel() {
        List<AdjustmentsAdapter.AdjustmentTool> tools = new ArrayList<>();
        tools.add(new AdjustmentsAdapter.AdjustmentTool("Brightness", R.drawable.ic_brightness, true));
        tools.add(new AdjustmentsAdapter.AdjustmentTool("Exposure", R.drawable.ic_brightness, false)); // Placeholder
        tools.add(new AdjustmentsAdapter.AdjustmentTool("Contrast", R.drawable.ic_filter, false)); // Placeholder icon
        tools.add(new AdjustmentsAdapter.AdjustmentTool("Saturation", R.drawable.ic_filter, false));
        tools.add(new AdjustmentsAdapter.AdjustmentTool("Highlight", R.drawable.ic_sort, false)); // Placeholder
        tools.add(new AdjustmentsAdapter.AdjustmentTool("Shadow", R.drawable.ic_sort, false)); // Placeholder
        tools.add(new AdjustmentsAdapter.AdjustmentTool("Vignette", R.drawable.ic_sort, false)); // Placeholder

        AdjustmentsAdapter adapter = new AdjustmentsAdapter(this, tools, tool -> {
            currentAdjustmentType = tool.name;
            updateSeekBarForCurrentTool();
        });
        adjustmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adjustmentsRecyclerView.setAdapter(adapter);
        adjustmentSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateValueFromSeekBar(progress);
                    applyBitmapAdjustments();
                    updateSliderValueTextPosition();
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        updateSeekBarForCurrentTool();
    }

    private void updateSeekBarForCurrentTool() {
        int progress = 100;
        if ("Brightness".equals(currentAdjustmentType)) progress = (int) (brightnessValue + 100);
        else if ("Contrast".equals(currentAdjustmentType)) progress = (int) (contrastValue * 100);
        else if ("Saturation".equals(currentAdjustmentType)) progress = (int) (saturationValue * 100);
        adjustmentSeekBar.setProgress(progress);
        updateValueFromSeekBar(progress);
        updateSliderValueTextPosition();
    }

    private void updateValueFromSeekBar(int progress) {
        int value = progress - 100;
        String prefix = value > 0 ? "+" : "";
        // For Contrast/Saturation, we can show a percentage or multiplier
        if ("Contrast".equals(currentAdjustmentType) || "Saturation".equals(currentAdjustmentType)) {
            sliderValueText.setText(prefix + value);
        } else { // Brightness
            sliderValueText.setText(prefix + value);
        }

        if ("Brightness".equals(currentAdjustmentType)) brightnessValue = value;
        else if ("Contrast".equals(currentAdjustmentType)) contrastValue = progress / 100f;
        else if ("Saturation".equals(currentAdjustmentType)) saturationValue = progress / 100f;
    }

    private void updateSliderValueTextPosition() {
        Rect thumbRect = adjustmentSeekBar.getThumb().getBounds();
        sliderValueText.setX(adjustmentSeekBar.getLeft() + thumbRect.left + (float)(thumbRect.width() - sliderValueText.getWidth()) / 2);
    }

    private void applyBitmapAdjustments() {
        if (originalBitmap == null) return;
        adjustedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
        Paint paint = new Paint();

        ColorMatrix finalMatrix = new ColorMatrix();

        // Saturation
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(saturationValue);
        finalMatrix.postConcat(saturationMatrix);

        // Contrast
        ColorMatrix contrastMatrix = new ColorMatrix();
        float t = (1.0f - contrastValue) / 2.0f * 255.0f;
        contrastMatrix.set(new float[]{
                contrastValue, 0, 0, 0, t,
                0, contrastValue, 0, 0, t,
                0, 0, contrastValue, 0, t,
                0, 0, 0, 1, 0
        });
        finalMatrix.postConcat(contrastMatrix);

        // Brightness
        ColorMatrix brightnessMatrix = new ColorMatrix();
        brightnessMatrix.set(new float[]{
                1, 0, 0, 0, brightnessValue,
                0, 1, 0, 0, brightnessValue,
                0, 0, 1, 0, brightnessValue,
                0, 0, 0, 1, 0
        });
        finalMatrix.postConcat(brightnessMatrix);

        paint.setColorFilter(new ColorMatrixColorFilter(finalMatrix));
        new Canvas(adjustedBitmap).drawBitmap(originalBitmap, 0, 0, paint);
        mPhotoEditorView.getSource().setImageBitmap(adjustedBitmap);
    }

    private void setupOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (toolPropertiesContainer.getVisibility() == View.VISIBLE) {
                    hideAllToolPanels();
                } else {
                    finish();
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void saveImage() {
        mPhotoEditorView.getSource().setImageBitmap(adjustedBitmap != null ? adjustedBitmap : originalBitmap);
        if (requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Gallery");
            if (!file.exists()) file.mkdirs();
            File finalFile = new File(file, System.currentTimeMillis() + ".png");
            mPhotoEditor.saveAsFile(finalFile.getAbsolutePath(), new SaveSettings.Builder().build(), new PhotoEditor.OnSaveListener() {
                @Override
                public void onSuccess(@NonNull String imagePath) {
                    Toast.makeText(ImageEditActivity.this, "Saved successfully!", Toast.LENGTH_SHORT).show();
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(imagePath))));
                    finish(); // Close activity after saving
                }
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(ImageEditActivity.this, "Failed to save: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public boolean requestPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, READ_WRITE_STORAGE);
            return false;
        }
        return true;
    }

    private void startCrop(@NonNull Uri uri) {
        String destFileName = "CroppedImage.jpg";
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(this, R.color.purple_700));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.purple_700));
        uCropLauncher.launch(UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destFileName))).withOptions(options).getIntent(this));
    }

    @Override public void onFilterSelected(PhotoFilter photoFilter) {
        if (adjustedBitmap != null) {
            originalBitmap = adjustedBitmap;
            brightnessValue = 0f;
            contrastValue = 1f;
            saturationValue = 1f;
        }
        mPhotoEditor.setFilterEffect(photoFilter);
    }

    @Override public void onEditTextChangeListener(final View rootView, String text, int colorCode) {}
    @Override public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {}
    @Override public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews) {}
    @Override public void onStartViewChangeListener(ViewType viewType) {}
    @Override public void onStopViewChangeListener(ViewType viewType) {}
    @Override public void onTouchSourceImage(MotionEvent event) {}
}