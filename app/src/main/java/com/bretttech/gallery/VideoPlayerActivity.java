package com.bretttech.gallery;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import android.view.ViewGroup; // NEW IMPORT

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
    private VideoView videoView; // Made class member for access

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoView = findViewById(R.id.video_view); // Assigned class member
        ActionBar actionBar = getSupportActionBar();

        // FIX: Allow content to go edge-to-edge (behind system bars) to enable true fullscreen toggle
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Get the video URI from the intent
        Uri videoUri = getIntent().getParcelableExtra(EXTRA_VIDEO_URI);

        if (videoUri != null) {
            // Setup MediaController
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);

            videoView.setMediaController(mediaController);
            videoView.setVideoURI(videoUri);

            // Set listeners
            // FIX: Added logic to adjust VideoView's size to maintain aspect ratio
            videoView.setOnPreparedListener(mp -> {
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();
                int viewWidth = videoView.getWidth();
                int viewHeight = videoView.getHeight();

                float aspectRatio = (float) videoWidth / videoHeight;
                float viewRatio = (float) viewWidth / viewHeight;

                ViewGroup.LayoutParams params = videoView.getLayoutParams();

                if (viewRatio > aspectRatio) {
                    // View is wider than video, match view height and adjust width
                    params.width = (int) (viewHeight * aspectRatio);
                    params.height = viewHeight;
                } else {
                    // View is taller than video, match view width and adjust height
                    params.width = viewWidth;
                    params.height = (int) (viewWidth / aspectRatio);
                }

                videoView.setLayoutParams(params);
                mp.start(); // Start playback once prepared
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Error playing video.", Toast.LENGTH_LONG).show();
                return true; // Handle error
            });

            // Set click listener on the VideoView to toggle UI visibility
            videoView.setOnClickListener(v -> toggleSystemUiVisibility());

            // Set title and enable back button in action bar
            if (actionBar != null) {
                actionBar.setTitle("Video Playback");
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            // Initial state: ensure action bar is visible
            updateSystemUiVisibility(false);
        } else {
            Toast.makeText(this, "Video URI is missing.", Toast.LENGTH_LONG).show();
            finish();
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
            // Hide system bars (Status and Navigation bars)
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

            // Hide the Action Bar
            if (actionBar != null) {
                actionBar.hide();
            }
            isUIHidden = true;
        } else {
            // Show system bars
            controller.show(WindowInsetsCompat.Type.systemBars());

            // Show the Action Bar
            if (actionBar != null) {
                actionBar.show();
            }
            isUIHidden = false;
        }
    }

    // Handle the back button (Up button) press on the Action Bar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close the activity and return to the previous screen
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}