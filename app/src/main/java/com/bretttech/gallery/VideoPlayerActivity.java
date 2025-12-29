package com.bretttech.gallery;

import android.content.ContentValues;
import android.graphics.Bitmap;
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
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
    private Toolbar topToolbar;
    private SeekBar videoSeekBar;
    private ImageButton btnPlayPause;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // Force black background to prevent transparency issues
        getWindow().setBackgroundDrawableResource(android.R.color.black);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        videoView = findViewById(R.id.video_view);
        fabScreenshot = findViewById(R.id.fab_screenshot);
        controlsOverlay = findViewById(R.id.controls_overlay);
        topToolbar = findViewById(R.id.top_toolbar);
        videoSeekBar = findViewById(R.id.video_seekbar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);

        setSupportActionBar(topToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Video Playback");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

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
            controlsOverlay.setOnClickListener(v -> toggleSystemUiVisibility());

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
                    hideHandler.removeCallbacksAndMessages(null); // Keep UI while seeking
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (isPlaying) {
                        startProgressUpdate();
                    }
                    hideHandler.postDelayed(() -> updateSystemUiVisibility(true), 3000);
                }
            });

            fabScreenshot.setOnClickListener(v -> takeScreenshot());

            // Initially show UI
            updateSystemUiVisibility(false);

            // Auto-hide after a delay
            hideHandler.postDelayed(() -> updateSystemUiVisibility(true), 3000);

        } else {
            Toast.makeText(this, "Video URI is missing.", Toast.LENGTH_LONG).show();
            finish();
        }

        // UPDATED: Apply Animation for Activity entry
        postponeEnterTransition();
        videoView.post(this::startPostponedEnterTransition);
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
                        runOnUiThread(
                                () -> Toast.makeText(this, "Screenshot failed to capture.", Toast.LENGTH_SHORT).show());
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
    }

    private void toggleSystemUiVisibility() {
        isUIHidden = !isUIHidden;
        updateSystemUiVisibility(isUIHidden);
    }

    private void updateSystemUiVisibility(boolean hide) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(),
                getWindow().getDecorView());

        if (controller != null) {
            if (hide) {
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsetsCompat.Type.systemBars());
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars());
            }
        }

        // Always clear pending hide actions when state changes
        hideHandler.removeCallbacksAndMessages(null);

        float targetAlpha = hide ? 0f : 1f;
        long duration = 300;

        controlsOverlay.animate()
                .alpha(targetAlpha)
                .setDuration(duration)
                .setInterpolator(hide ? new AccelerateInterpolator() : new DecelerateInterpolator())
                .withEndAction(() -> {
                    // If hidden, we can optionally set visibility to GONE *if* we are sure it won't
                    // jump.
                    // But generally, alpha 0 with clickable=false is safer for "no jump".
                    // However, the original code had setVisibility(GONE) in endAction.
                    // Let's keep it visible but alpha 0 to avoid layout re-calculation risk,
                    // OR ensure ConstraintLayout doesn't collapse.
                    // Since controls_overlay matches parent, it won't collapse.
                    // BUT, if we set GONE, child views are gone.
                    // Let's stick to Alpha + clickability toggle for maximum safety against jumps.
                    // Actually, let's TRY to set GONE because otherwise touches might be blocked?
                    // No, we handle setClickable.
                })
                .start();

        // Toggle clickability
        setControlsClickable(!hide);
        controlsOverlay.setClickable(!hide); // Overlay itself? No, we need clicks to pass through to
                                             // VideoView/Container
        // We set controlsOverlay background to transparent, so clicks pass through if
        // not clickable?
        // Wait, controlsOverlay has children.
        // We want clicks on the empty space of controlsOverlay to TOGGLE UI (which we
        // set up with onClickListener).
        // If we hide UI, we want taps on videoView to show it.
        // If UI is hidden (alpha 0), controlsOverlay is still there. If it consumes
        // clicks, videoView won't get them.
        // So when hidden, controlsOverlay should NOT be clickable.

        if (hide) {
            controlsOverlay.setClickable(false);
            // And we rely on videoView.setOnClickListener to show it again.
            // videoView is BEHIND controlsOverlay?
            // If controlsOverlay is match_parent, it covers videoView.
            // If controlsOverlay is not clickable, clicks pass to videoView.
            // VideoView has click listener to toggle.
        } else {
            // If shown, taps on empty space should probably toggle/hide?
            controlsOverlay.setClickable(true);
        }

        if (!hide) {
            // Auto-hide after 3 seconds
            hideHandler.postDelayed(() -> updateSystemUiVisibility(true), 3000);
        }
    }

    private void setControlsClickable(boolean clickable) {
        fabScreenshot.setClickable(clickable);
        videoSeekBar.setClickable(clickable);
        btnPlayPause.setClickable(clickable);
        topToolbar.setClickable(clickable);
        // Disable children of bottom container
        View bottomContainer = findViewById(R.id.bottom_controls_container);
        if (bottomContainer != null) {
            // bottomContainer.setClickable(clickable); // This might block clicks?
            // Loop children?
            // Actually, if the parent overlay is not clickable, and children are, they
            // still work?
            // Yes. But we are animating parent alpha.
            // If parent alpha is 0, children are invisible.
            // We should disable them to be safe.
            fabScreenshot.setEnabled(clickable);
            videoSeekBar.setEnabled(clickable);
            btnPlayPause.setEnabled(clickable);
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