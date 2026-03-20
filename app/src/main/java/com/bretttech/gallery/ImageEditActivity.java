package com.bretttech.gallery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.filters.FilterListener;
import com.bretttech.gallery.filters.FilterViewAdapter;
import com.bretttech.gallery.text.ColorPickerAdapter;
import com.bretttech.gallery.text.TextEditorDialogFragment;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.OnSaveBitmap;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;
import ja.burhanrashid52.photoeditor.ViewType;

public class ImageEditActivity extends AppCompatActivity implements OnPhotoEditorListener, View.OnClickListener, FilterListener {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    private static final String TAG = "ImageEditActivity";

    private PhotoEditor mPhotoEditor;
    private PhotoEditorView mPhotoEditorView;

    private Uri mImageUri;
    private Uri mOriginalImageUri;

    private Bitmap originalBitmap;
    private Bitmap adjustedBitmap;

    private View mainToolNavigation;
    private View toolPropertiesContainer;
    private View adjustToolsPanel;
    private RecyclerView filterRecyclerView;
    private RecyclerView adjustmentsRecyclerView;
    private SeekBar adjustmentSeekBar;
    private TextView sliderValueText;

    private View brushToolsPanel;
    private SeekBar brushSizeSeekbar;
    private RecyclerView colorPickerRecyclerView;

    private View editorRoot;
    private View topActionBar;
    private View bottomBarContainer;

    private String currentAdjustmentType = "Brightness";
    private float brightnessValue = 0f;
    private float contrastValue = 1f;
    private float saturationValue = 1f;
    private final List<View> pinchGesturePatchedTextRoots = new ArrayList<>();
    private View pendingEditTextRootView;
    private String pendingEditTextValue;
    private int pendingEditTextColor = Color.WHITE;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

    private final ActivityResultLauncher<IntentSenderRequest> overwriteResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "Overwrite permission granted. Retrying save.");
                    saveImage(true);
                } else {
                    Log.d(TAG, "Overwrite permission denied.");
                    Toast.makeText(this, "Permission to overwrite denied. Try saving as a new image.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        initViews();
        applySystemInsets();
        setupPhotoEditor();
        setupListeners();
        setupOnBackPressed();

        mOriginalImageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        mImageUri = mOriginalImageUri;

        if (mImageUri != null) {
            loadBitmapFromUri(mImageUri);
        } else {
            Toast.makeText(this, "Error: Image URI not found.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        editorRoot = findViewById(R.id.editor_root);
        topActionBar = findViewById(R.id.top_action_bar);
        bottomBarContainer = findViewById(R.id.bottom_bar_container);

        mPhotoEditorView = findViewById(R.id.photoEditorView);
        mainToolNavigation = findViewById(R.id.main_tool_navigation);
        toolPropertiesContainer = findViewById(R.id.tool_properties_container);
        adjustToolsPanel = findViewById(R.id.adjust_tools_panel);
        filterRecyclerView = findViewById(R.id.filter_recyclerview);

        adjustmentsRecyclerView = adjustToolsPanel.findViewById(R.id.adjustments_recycler_view);
        adjustmentSeekBar = adjustToolsPanel.findViewById(R.id.adjustment_seekbar);
        sliderValueText = adjustToolsPanel.findViewById(R.id.slider_value_text);

        brushToolsPanel = findViewById(R.id.brush_tools_panel);
        brushSizeSeekbar = brushToolsPanel.findViewById(R.id.brush_size_seekbar);
        colorPickerRecyclerView = brushToolsPanel.findViewById(R.id.color_picker_recyclerview);


        filterRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupPhotoEditor() {
        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true)
                .build();
        mPhotoEditor.setOnPhotoEditorListener(this);
    }

    private void applySystemInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(editorRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Use absolute values — top bar grows downward via wrap_content + paddingTop
            topActionBar.setPadding(
                    0,
                    systemBars.top,
                    0,
                    0
            );

            // Bottom bar gains navigation-bar clearance
            bottomBarContainer.setPadding(
                    0,
                    0,
                    0,
                    systemBars.bottom
            );

            return insets;
        });
        ViewCompat.requestApplyInsets(editorRoot);
    }

    private void openTextEditorForAdd() {
        TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this);
        textEditorDialogFragment.setOnTextEditorListener((inputText, colorCode, textSize) -> {
            final TextStyleBuilder styleBuilder = new TextStyleBuilder();
            styleBuilder.withTextColor(colorCode);
            styleBuilder.withTextSize(textSize);
            mPhotoEditor.addText(inputText, styleBuilder);
            mPhotoEditorView.post(this::ensureTextPinchFallbackInstalled);
        });
    }

    private void openTextEditorForExisting(View rootView, String text, int colorCode) {
        TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this, text, colorCode);
        textEditorDialogFragment.setOnTextEditorListener((inputText, newColorCode, textSize) -> {
            TextStyleBuilder styleBuilder = new TextStyleBuilder();
            styleBuilder.withTextColor(newColorCode);
            styleBuilder.withTextSize(textSize);
            mPhotoEditor.editText(rootView, inputText, styleBuilder);
        });
    }

    private void ensureTextPinchFallbackInstalled() {
        List<View> textRoots = new ArrayList<>();
        collectTextOverlayRoots(mPhotoEditorView, textRoots);
        for (View textRoot : textRoots) {
            if (!pinchGesturePatchedTextRoots.contains(textRoot)) {
                attachPinchFallbackToTextRoot(textRoot);
                pinchGesturePatchedTextRoots.add(textRoot);
            }
        }
    }

    private void collectTextOverlayRoots(View view, List<View> result) {
        if (view == null) return;

        if (view.getTag() == ViewType.TEXT) {
            result.add(view);
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectTextOverlayRoots(group.getChildAt(i), result);
            }
        }
    }

    private void attachPinchFallbackToTextRoot(View textRoot) {
        View.OnTouchListener originalTouchListener = getExistingTouchListener(textRoot);
        if (originalTouchListener == null) return;

        final float minScale = 0.5f;
        final float maxScale = 8f;
        final float pinchSensitivity = 0.35f;
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        textRoot.bringToFront();
                        return true;
                    }

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float smoothScaleFactor = (float) Math.pow(detector.getScaleFactor(), pinchSensitivity);
                        float nextScale = textRoot.getScaleX() * smoothScaleFactor;
                        nextScale = Math.max(minScale, Math.min(maxScale, nextScale));
                        textRoot.setScaleX(nextScale);
                        textRoot.setScaleY(nextScale);
                        textRoot.bringToFront();
                        return true;
                    }
                });

        textRoot.setOnTouchListener(new View.OnTouchListener() {
            private boolean inPinch = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();

                if (action == MotionEvent.ACTION_POINTER_DOWN || event.getPointerCount() > 1) {
                    inPinch = true;
                    textRoot.bringToFront();
                }

                if (inPinch || event.getPointerCount() > 1) {
                    scaleGestureDetector.onTouchEvent(event);

                    if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        if (event.getPointerCount() <= 2) inPinch = false;
                    }

                    return true;
                }

                return originalTouchListener.onTouch(v, event);
            }
        });
    }

    private View.OnTouchListener getExistingTouchListener(View view) {
        try {
            Field listenerInfoField = View.class.getDeclaredField("mListenerInfo");
            listenerInfoField.setAccessible(true);
            Object listenerInfo = listenerInfoField.get(view);
            if (listenerInfo == null) return null;

            Field onTouchListenerField = Class.forName("android.view.View$ListenerInfo")
                    .getDeclaredField("mOnTouchListener");
            onTouchListenerField.setAccessible(true);
            return (View.OnTouchListener) onTouchListenerField.get(listenerInfo);
        } catch (Exception ignored) {
            return null;
        }
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

            mPhotoEditorView.getSource().setImageBitmap(originalBitmap);
            mPhotoEditorView.getSource().setScaleType(ImageView.ScaleType.FIT_CENTER);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        findViewById(R.id.imgClose).setOnClickListener(this);
        findViewById(R.id.imgSave).setOnClickListener(this);
        findViewById(R.id.tool_adjust).setOnClickListener(this);
        findViewById(R.id.tool_filters).setOnClickListener(this);
        findViewById(R.id.tool_crop_rotate).setOnClickListener(this);
        findViewById(R.id.tool_text).setOnClickListener(this);
        findViewById(R.id.tool_decorate).setOnClickListener(this);
        findViewById(R.id.imgUndo).setOnClickListener(this);
        findViewById(R.id.imgRedo).setOnClickListener(this);
        findViewById(R.id.imgRotate).setOnClickListener(this);
        findViewById(R.id.imgFlip).setOnClickListener(this);

        adjustToolsPanel.findViewById(R.id.btnSaveAdjust).setOnClickListener(v -> showSaveDialog());
        adjustToolsPanel.findViewById(R.id.btnCancelAdjust).setOnClickListener(v -> hideAllToolPanels());

        brushToolsPanel.findViewById(R.id.btnSaveBrush).setOnClickListener(v -> hideAllToolPanels());
        brushToolsPanel.findViewById(R.id.btnCancelBrush).setOnClickListener(v -> {
            mPhotoEditor.setBrushDrawingMode(false);
            hideAllToolPanels();
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imgClose) {
            finish();
        } else if (id == R.id.imgSave) {
            showSaveDialog();
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
        } else if (id == R.id.imgRotate) {
            rotate90();
        } else if (id == R.id.imgFlip) {
            flipHorizontal();
        } else if (id == R.id.tool_text) {
            if (pendingEditTextRootView != null && pendingEditTextRootView.getParent() != null) {
                openTextEditorForExisting(
                        pendingEditTextRootView,
                        pendingEditTextValue == null ? "" : pendingEditTextValue,
                        pendingEditTextColor
                );
            } else {
                openTextEditorForAdd();
            }
        } else if (id == R.id.tool_decorate) {
            mPhotoEditor.setBrushDrawingMode(true);
            setupBrushPanel();
            showToolPanel(brushToolsPanel);
        } else if (id == R.id.imgUndo) {
            mPhotoEditor.undo();
        } else if (id == R.id.imgRedo) {
            mPhotoEditor.redo();
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Image");
        builder.setMessage("Overwrite the current photo or save as a new image?");
        builder.setPositiveButton("Save as new", (dialog, which) -> saveImage(false));
        builder.setNegativeButton("Overwrite", (dialog, which) -> saveImage(true));
        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void showToolPanel(View toolToShow) {
        mainToolNavigation.setVisibility(View.GONE);
        toolPropertiesContainer.setVisibility(View.VISIBLE);
        adjustToolsPanel.setVisibility(View.GONE);
        filterRecyclerView.setVisibility(View.GONE);
        brushToolsPanel.setVisibility(View.GONE);
        toolToShow.setVisibility(View.VISIBLE);
    }

    private void hideAllToolPanels() {
        mPhotoEditorView.getSource().setImageBitmap(originalBitmap);
        brightnessValue = 0f;
        contrastValue = 1f;
        saturationValue = 1f;
        mPhotoEditor.setBrushDrawingMode(false);
        toolPropertiesContainer.setVisibility(View.GONE);
        mainToolNavigation.setVisibility(View.VISIBLE);
    }

    private void setupAdjustmentsPanel() {
        List<AdjustmentsAdapter.AdjustmentTool> tools = new ArrayList<>();
        tools.add(new AdjustmentsAdapter.AdjustmentTool("Brightness", R.drawable.ic_brightness, true));
        tools.add(new AdjustmentsAdapter.AdjustmentTool("Contrast", R.drawable.ic_contrast, false));
        tools.add(new AdjustmentsAdapter.AdjustmentTool("Saturation", R.drawable.ic_saturation, false));

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

    private void setupBrushPanel() {
        brushSizeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPhotoEditor.setBrushSize(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        colorPickerRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ColorPickerAdapter colorPickerAdapter = new ColorPickerAdapter(this);
        colorPickerAdapter.setOnColorPickerClickListener(colorCode -> mPhotoEditor.setBrushColor(colorCode));
        colorPickerRecyclerView.setAdapter(colorPickerAdapter);
    }

    private void updateSeekBarForCurrentTool() {
        int progress;
        if ("Brightness".equals(currentAdjustmentType)) progress = (int) (brightnessValue + 100);
        else if ("Contrast".equals(currentAdjustmentType)) progress = (int) (contrastValue * 100);
        else progress = (int) (saturationValue * 100);
        adjustmentSeekBar.setProgress(progress);
        updateValueFromSeekBar(progress);
        updateSliderValueTextPosition();
    }

    private void updateValueFromSeekBar(int progress) {
        int value = progress - 100;
        sliderValueText.setText(String.format("%s%d", value > 0 ? "+" : "", value));

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
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(saturationValue);
        finalMatrix.postConcat(saturationMatrix);

        ColorMatrix contrastMatrix = new ColorMatrix();
        float t = (1.0f - contrastValue) / 2.0f * 255.0f;
        contrastMatrix.set(new float[]{
                contrastValue, 0, 0, 0, t,
                0, contrastValue, 0, 0, t,
                0, 0, contrastValue, 0, t,
                0, 0, 0, 1, 0
        });
        finalMatrix.postConcat(contrastMatrix);

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

    private void saveImage(boolean overwrite) {
        Log.d(TAG, "saveImage called. Overwrite: " + overwrite);
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();

        SaveSettings saveSettings = new SaveSettings.Builder()
                .setClearViewsEnabled(false)
                .setTransparencyEnabled(false)
                .build();

        mPhotoEditor.saveAsBitmap(saveSettings, new OnSaveBitmap() {
            @Override
            public void onBitmapReady(Bitmap saveBitmap) {
                Log.d(TAG, "onBitmapReady: Bitmap received.");
                executor.execute(() -> {
                    if (overwrite) {
                        saveBitmapToUri(mOriginalImageUri, saveBitmap);
                    } else {
                        saveBitmapAsNew(saveBitmap);
                    }
                });
            }
        });
    }

    private void saveBitmapToUri(Uri uri, Bitmap bitmapToSave) {
        try {
            ContentResolver resolver = getContentResolver();
            try (OutputStream stream = resolver.openOutputStream(uri)) {
                if (stream != null) {
                    bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    Log.d(TAG, "Bitmap saved to URI: " + uri);
                } else {
                    throw new IOException("Failed to get output stream.");
                }
            }
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
            }
            resolver.update(uri, values, null, null);

            runOnUiThread(() -> {
                setResult(Activity.RESULT_OK);
                Toast.makeText(this, "Saved successfully!", Toast.LENGTH_SHORT).show();
                finish();
            });

        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap to URI: " + uri, e);
            runOnUiThread(() -> Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (SecurityException e) {
            Log.e(TAG, "Security error saving bitmap to URI: " + uri, e);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                handleRecoverableSecurityException(e, uri);
            } else {
                runOnUiThread(() -> Toast.makeText(this, "A security error occurred.", Toast.LENGTH_SHORT).show());
            }
        }
    }


    private void saveBitmapAsNew(Bitmap bitmap) {
        ContentResolver resolver = getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + System.currentTimeMillis() + ".png");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Gallery");

        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (imageUri != null) {
            saveBitmapToUri(imageUri, bitmap);
        } else {
            Log.e(TAG, "Failed to create new MediaStore entry.");
            runOnUiThread(() -> Toast.makeText(this, "Failed to create new image file.", Toast.LENGTH_SHORT).show());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void handleRecoverableSecurityException(SecurityException e, Uri uriToModify) {
        if (e instanceof RecoverableSecurityException) {
            RecoverableSecurityException rse = (RecoverableSecurityException) e;
            IntentSender intentSender = rse.getUserAction().getActionIntent().getIntentSender();
            IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
            runOnUiThread(() -> overwriteResultLauncher.launch(request));
        } else {
            runOnUiThread(() -> Toast.makeText(this, "A security error occurred.", Toast.LENGTH_SHORT).show());
        }
    }


    private void startCrop(@NonNull Uri uri) {
        String destFileName = "CroppedImage_" + System.currentTimeMillis() + ".jpg";
        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(this, android.R.color.black));
        options.setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));
        options.setToolbarWidgetColor(ContextCompat.getColor(this, android.R.color.white));
        options.setRootViewBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.teal_200));
        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(true);
        options.setCompressionQuality(95);

        // Provide common aspect ratios for quick crop
        options.setAspectRatioOptions(0,
                new com.yalantis.ucrop.model.AspectRatio("Original", 0, 0),
                new com.yalantis.ucrop.model.AspectRatio("1:1", 1, 1),
                new com.yalantis.ucrop.model.AspectRatio("4:5", 4, 5),
                new com.yalantis.ucrop.model.AspectRatio("16:9", 16, 9)
        );

        UCrop u = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destFileName)))
                .withOptions(options);
        android.content.Intent intent = u.getIntent(this);
        intent.setClass(this, UCropWrapperActivity.class);
        uCropLauncher.launch(intent);
    }

    private void rotate90() {
        if (originalBitmap == null) return;
        Matrix m = new Matrix();
        m.postRotate(90);
        originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), m, true);
        adjustedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
        mPhotoEditorView.getSource().setImageBitmap(originalBitmap);
    }

    private void flipHorizontal() {
        if (originalBitmap == null) return;
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), m, true);
        adjustedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
        mPhotoEditorView.getSource().setImageBitmap(originalBitmap);
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

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode) {
        pendingEditTextRootView = rootView;
        pendingEditTextValue = text;
        pendingEditTextColor = colorCode;
        Toast.makeText(this, "Text selected. Tap Text tool to edit.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
        if (viewType == ViewType.TEXT) {
            mPhotoEditorView.post(this::ensureTextPinchFallbackInstalled);
        }
    }
    @Override public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews) {}
    @Override public void onStartViewChangeListener(ViewType viewType) {}
    @Override public void onStopViewChangeListener(ViewType viewType) {}
    @Override public void onTouchSourceImage(MotionEvent event) {}
}