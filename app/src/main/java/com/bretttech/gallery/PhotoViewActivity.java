package com.bretttech.gallery;

import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.bretttech.gallery.data.FavoritesManager;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.ui.pictures.MoveToAlbumDialogFragment;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PhotoViewActivity extends AppCompatActivity implements PhotoPagerAdapter.PhotoTapListener {

    public static final String EXTRA_IMAGE_POSITION = "extra_image_position";

    private ViewPager2 viewPager;
    private PhotoPagerAdapter adapter;
    private boolean isUIHidden = false;
    private List<Image> images;
    private List<Uri> imageUris;
    private ViewGroup bottomActionBar;
    private boolean isSecureMode = false;
    private ImageButton favoriteButton;
    private FavoritesManager favoritesManager;

    private final ActivityResultLauncher<IntentSenderRequest> trashResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Photo moved to trash", Toast.LENGTH_SHORT).show();
                    removeCurrentImageFromPager();
                } else {
                    Toast.makeText(this, "Photo not moved to trash", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        isSecureMode = getIntent().getBooleanExtra("isSecure", false);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        viewPager = findViewById(R.id.view_pager);
        bottomActionBar = findViewById(R.id.bottom_action_bar);
        favoriteButton = findViewById(R.id.button_favorite);
        favoritesManager = new FavoritesManager(this);


        images = ImageDataHolder.getInstance().getImageList();
        int currentPosition = getIntent().getIntExtra(EXTRA_IMAGE_POSITION, 0);

        if (images != null && !images.isEmpty()) {
            imageUris = new ArrayList<>(images.stream().map(Image::getUri).collect(Collectors.toList()));
            adapter = new PhotoPagerAdapter(this, imageUris, this);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(currentPosition, false);
        } else {
            imageUris = new ArrayList<>();
            finish();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setupButtonListeners();
        updateFavoriteButton();
        updateSystemUiVisibility(false);
        setupFragmentResultListener();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateFavoriteButton();
            }
        });

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


    private void setupButtonListeners() {
        findViewById(R.id.button_share).setOnClickListener(v -> shareCurrentImage());
        findViewById(R.id.button_edit).setOnClickListener(v -> editCurrentImage());
        findViewById(R.id.button_favorite).setOnClickListener(v -> toggleFavorite());
    }

    private void setupFragmentResultListener() {
        getSupportFragmentManager().setFragmentResultListener(MoveToAlbumDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            boolean success = bundle.getBoolean(MoveToAlbumDialogFragment.KEY_MOVE_SUCCESS);
            if (success) {
                Toast.makeText(this, "Photo moved successfully", Toast.LENGTH_SHORT).show();
                removeCurrentImageFromPager();
            } else {
                Toast.makeText(this, "Failed to move photo", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private Uri getCurrentImageUri() {
        if (imageUris == null || imageUris.isEmpty()) return null;
        int currentPosition = viewPager.getCurrentItem();
        if (currentPosition >= 0 && currentPosition < imageUris.size()) {
            return imageUris.get(currentPosition);
        }
        return null;
    }

    private Image getCurrentImage() {
        if (images == null || images.isEmpty()) return null;
        int currentPosition = viewPager.getCurrentItem();
        if (currentPosition >= 0 && currentPosition < images.size()) {
            return images.get(currentPosition);
        }
        return null;
    }

    private void shareCurrentImage() {
        Uri imageUri = getCurrentImageUri();
        if (imageUri == null) {
            Toast.makeText(this, "Cannot share image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSecureMode) {
            Toast.makeText(this, "Sharing from secure folder is not allowed.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Image"));
    }

    private void editCurrentImage() {
        Uri imageUri = getCurrentImageUri();
        if (imageUri == null) {
            Toast.makeText(this, "Cannot edit image", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isSecureMode) {
            Toast.makeText(this, "Editing from secure folder is not yet supported.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ImageEditActivity.class);
        intent.putExtra(ImageEditActivity.EXTRA_IMAGE_URI, imageUri);
        startActivity(intent);
    }

    private void updateFavoriteButton() {
        Image currentImage = getCurrentImage();
        if (currentImage != null) {
            favoritesManager.isFavorite(currentImage, isFavorite -> {
                runOnUiThread(() -> {
                    if (isFavorite) {
                        favoriteButton.setImageResource(R.drawable.ic_favorite);
                    } else {
                        favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                    }
                });
            });
        }
    }

    private void toggleFavorite() {
        Image currentImage = getCurrentImage();
        if (currentImage != null) {
            favoritesManager.isFavorite(currentImage, isFavorite -> {
                if (isFavorite) {
                    favoritesManager.removeFavorite(currentImage);
                } else {
                    favoritesManager.addFavorite(currentImage);
                }
                runOnUiThread(this::updateFavoriteButton);
            });
        }
    }

    @Override
    public void onPhotoTapped(Uri photoUri) {
        toggleSystemUiVisibility();
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
            if (actionBar != null) actionBar.hide();
            bottomActionBar.setVisibility(View.GONE);
            isUIHidden = true;
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars());
            if (actionBar != null) actionBar.show();
            bottomActionBar.setVisibility(View.VISIBLE);
            isUIHidden = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo_view_menu, menu);
        MenuItem moveItem = menu.findItem(R.id.action_move);
        if (isSecureMode) {
            moveItem.setTitle("Move to another secure album");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_details) {
            showImageDetails();
            return true;
        } else if (itemId == R.id.action_delete) {
            if (isSecureMode) {
                deleteSecureImage();
            } else {
                trashCurrentImage();
            }
            return true;
        } else if (itemId == R.id.action_move) {
            moveCurrentImage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showImageDetails() {
        Uri imageUri = getCurrentImageUri();
        if (imageUri != null) {
            ImageDetailsDialogFragment.newInstance(imageUri).show(getSupportFragmentManager(), "image_details");
        }
    }

    private void moveCurrentImage() {
        Uri imageUri = getCurrentImageUri();
        if (imageUri != null) {
            List<Uri> uris = new ArrayList<>();
            uris.add(imageUri);
            MoveToAlbumDialogFragment.newInstance(uris, isSecureMode).show(getSupportFragmentManager(), MoveToAlbumDialogFragment.TAG);
        } else {
            Toast.makeText(this, "Could not find image to move", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSecureImage() {
        Uri imageUri = getCurrentImageUri();
        if (imageUri != null) {
            new File(imageUri.getPath()).delete();
            Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
            removeCurrentImageFromPager();
        }
    }


    private void trashCurrentImage() {
        Uri imageUri = getCurrentImageUri();
        if (imageUri == null) return;
        ContentResolver contentResolver = getContentResolver();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                trashResultLauncher.launch(new IntentSenderRequest.Builder(MediaStore.createTrashRequest(contentResolver, Collections.singletonList(imageUri), true).getIntentSender()).build());
            } else {
                contentResolver.delete(imageUri, null, null);
                Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
                removeCurrentImageFromPager();
            }
        } catch (SecurityException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e instanceof RecoverableSecurityException) {
                trashResultLauncher.launch(new IntentSenderRequest.Builder(((RecoverableSecurityException) e).getUserAction().getActionIntent().getIntentSender()).build());
            } else {
                Toast.makeText(this, "Error: Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void removeCurrentImageFromPager() {
        int currentPosition = viewPager.getCurrentItem();
        if (currentPosition < imageUris.size()) {
            imageUris.remove(currentPosition);
            adapter.notifyItemRemoved(currentPosition);

            if (images != null && currentPosition < images.size()) {
                favoritesManager.removeFavorite(images.get(currentPosition));
                images.remove(currentPosition);
            }

            if (imageUris.isEmpty()) {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageDataHolder.getInstance().setImageList(null);
    }
}