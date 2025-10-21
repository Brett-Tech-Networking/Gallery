package com.bretttech.gallery;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.MediaController;
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

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URI = "extra_video_uri";
    private boolean isUIHidden = false;
    private VideoView videoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoView = findViewById(R.id.video_view);
        ActionBar actionBar = getSupportActionBar();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Uri videoUri = getIntent().getParcelableExtra(EXTRA_VIDEO_URI);

        if (videoUri != null) {
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);

            videoView.setMediaController(mediaController);
            videoView.setVideoURI(videoUri);

            videoView.setOnPreparedListener(mp -> {
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
                mp.start();
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Error playing video.", Toast.LENGTH_LONG).show();
                return true;
            });

            videoView.setOnClickListener(v -> toggleSystemUiVisibility());

            if (actionBar != null) {
                actionBar.setTitle("Video Playback");
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            updateSystemUiVisibility(false);
        } else {
            Toast.makeText(this, "Video URI is missing.", Toast.LENGTH_LONG).show();
            finish();
        }

        // UPDATED: Apply Animation for Activity entry
        applyActivityTransition(false);
    }

    // UPDATED: Override finish() to apply reverse animation
    @Override
    public void finish() {
        super.finish();
        applyActivityTransition(true);
    }

    // NEW METHOD to handle animation logic
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
            // Pixel In/Out (Fade/Crossfade)
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

        if (hide) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

            if (actionBar != null) {
                actionBar.hide();
            }
            isUIHidden = true;
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars());

            if (actionBar != null) {
                actionBar.show();
            }
            isUIHidden = false;
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
}