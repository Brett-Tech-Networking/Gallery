package com.bretttech.gallery;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.OutputStream;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URI = "extra_video_uri";
    private boolean isUIHidden = false;
    private VideoView videoView;
    private FloatingActionButton fabScreenshot;
    private boolean isVideoPrepared = false;
    private Handler hideHandler = new Handler(Looper.getMainLooper());
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    
    // Custom controls
    private View controlsOverlay;
    private SeekBar videoSeekBar;
    private ImageButton btnPlayPause;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoView = findViewById(R.id.video_view);
        fabScreenshot = findViewById(R.id.fab_screenshot);
        controlsOverlay = findViewById(R.id.controls_overlay);
        videoSeekBar = findViewById(R.id.video_seekbar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        
        ActionBar actionBar = getSupportActionBar();

        // Force black background to prevent transparency issues
        getWindow().setBackgroundDrawableResource(android.R.color.black);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Uri videoUri = getIntent().getParcelableExtra(EXTRA_VIDEO_URI);

        if (videoUri != null) {
            videoView.setVideoURI(videoUri);

            videoView.setOnPreparedListener(mp -> {
                isVideoPrepared = true;
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();
                int viewWidth = videoView.getWidth();
                int viewHeight = videoView.getHeight();

                float aspectRatio = (float) videoWidth / videoHeight;
                float viewRatio = (float) viewWidth / viewHeight;

                ViewGroup.LayoutParams params = videoView.getLayoutParams();

                if (viewRatio > aspectRatio) {
                    params.width = (int) (viewHeight * aspectRatio);
                    params.height = viewHeight;
                } else {
                    params.width = viewWidth;
                    params.height = (int) (viewWidth / aspectRatio);
                }

                videoView.setLayoutParams(params);
                
                // Setup seekbar
                int duration = videoView.getDuration();
                videoSeekBar.setMax(duration);
                tvTotalTime.setText(formatTime(duration));
                
                mp.start();
                isPlaying = true;
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                
                // Start updating progress
                startProgressUpdate();
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Error playing video.", Toast.LENGTH_LONG).show();
                return true;
            });

            // Toggle UI on video click
            videoView.setOnClickListener(v -> toggleSystemUiVisibility());

            // Play/Pause button
            btnPlayPause.setOnClickListener(v -> {
                if (isPlaying) {
                    videoView.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    isPlaying = false;
                    stopProgressUpdate();
                } else {
                    videoView.start();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    isPlaying = true;
                    startProgressUpdate();
                }
            });

            // Seekbar
            videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        videoView.seekTo(progress);
                        tvCurrentTime.setText(formatTime(progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    stopProgressUpdate();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (isPlaying) {
                        startProgressUpdate();
                    }
                }
            });

            fabScreenshot.setOnClickListener(v -> takeScreenshot());

            if (actionBar != null) {
                actionBar.setTitle("Video Playback");
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            // Initially show UI
            updateSystemUiVisibility(false);
            
            // Auto-hide after a delay
            hideHandler.postDelayed(() -> updateSystemUiVisibility(true), 3000);

        } else {
            Toast.makeText(this, "Video URI is missing.", Toast.LENGTH_LONG).show();
            finish();
        }

        // UPDATED: Apply Animation for Activity entry
        applyActivityTransition(false);
    }

    private void startProgressUpdate() {
        updateHandler.post(updateProgressRunnable);
    }

    private void stopProgressUpdate() {
        updateHandler.removeCallbacks(updateProgressRunnable);
    }

    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (videoView != null && isPlaying) {
                int currentPosition = videoView.getCurrentPosition();
                videoSeekBar.setProgress(currentPosition);
                tvCurrentTime.setText(formatTime(currentPosition));
                updateHandler.postDelayed(this, 100);
            }
        }
    };

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void takeScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Bitmap bitmap = Bitmap.createBitmap(videoView.getWidth(), videoView.getHeight(), Bitmap.Config.ARGB_8888);
            try {
                PixelCopy.request(videoView, bitmap, copyResult -> {
                    if (copyResult == PixelCopy.SUCCESS) {
                        saveBitmapToGallery(bitmap);
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Screenshot failed to capture.", Toast.LENGTH_SHORT).show());
                    }
                }, new Handler(Looper.getMainLooper()));
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "Screenshot not supported on this device/view.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Screenshot requires Android 7.0+.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBitmapToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Video_Screenshot_" + System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GalleryApp");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                runOnUiThread(() -> Toast.makeText(this, "Screenshot saved to Gallery.", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to save screenshot.", Toast.LENGTH_SHORT).show());
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        applyActivityTransition(true);
    }

    private void applyActivityTransition(boolean isExiting) {
        String animationType = SettingsActivity.getAnimationType(this);
        int enterAnim = 0;
        int exitAnim = 0;

        if (animationType.equals(SettingsActivity.ANIMATION_SLIDE)) {
            if (isExiting) {
                enterAnim = R.anim.slide_in_left;
                exitAnim = R.anim.slide_out_right;
            } else {
                enterAnim = R.anim.slide_in_right;
                exitAnim = R.anim.slide_out_left;
            }
        } else if (animationType.equals(SettingsActivity.ANIMATION_FLY)) {
            if (isExiting) {
                enterAnim = R.anim.fly_in_down;
                exitAnim = R.anim.fly_out_up;
            } else {
                enterAnim = R.anim.fly_in_up;
                exitAnim = R.anim.fly_out_down;
            }
        } else if (animationType.equals(SettingsActivity.ANIMATION_FADE)) {
            enterAnim = R.anim.fade_in;
            exitAnim = R.anim.fade_out;
        }

        if (enterAnim != 0 || exitAnim != 0) {
            overridePendingTransition(enterAnim, exitAnim);
        }
    }

    private void toggleSystemUiVisibility() {
        updateSystemUiVisibility(!isUIHidden);
    }

    private void updateSystemUiVisibility(boolean hide) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ActionBar actionBar = getSupportActionBar();

        if (controller == null) return;

        // Always clear pending hide actions when state changes
        hideHandler.removeCallbacksAndMessages(null);

        if (hide) {
            // Fade out controls smoothly
            controlsOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    controlsOverlay.setVisibility(View.GONE);
                    
                    controller.hide(WindowInsetsCompat.Type.systemBars());
                    controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

                    if (actionBar != null) {
                        actionBar.hide();
                    }
                })
                .start();
            
            isUIHidden = true;
        } else {
            // Fade in controls smoothly
            controlsOverlay.setVisibility(View.VISIBLE);
            controlsOverlay.setAlpha(0f);
            controlsOverlay.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
            
            controller.show(WindowInsetsCompat.Type.systemBars());

            if (actionBar != null) {
                actionBar.show();
            }
            
            isUIHidden = false;

            // Auto-hide after 3 seconds
            hideHandler.postDelayed(() -> updateSystemUiVisibility(true), 3000);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopProgressUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideHandler.removeCallbacksAndMessages(null);
        updateHandler.removeCallbacksAndMessages(null);
    }
}