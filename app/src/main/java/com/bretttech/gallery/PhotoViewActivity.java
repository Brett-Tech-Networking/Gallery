package com.bretttech.gallery;

import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bretttech.gallery.ui.pictures.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PhotoViewActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_POSITION = "extra_image_position";

    private List<Image> images;
    private List<Uri> imageUris;
    private ViewPager2 viewPager;
    private PhotoPagerAdapter adapter;

    private final ActivityResultLauncher<IntentSenderRequest> trashResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Photo moved to trash", Toast.LENGTH_SHORT).show();
                    // Refresh the view after successful trashing
                    int currentPosition = viewPager.getCurrentItem();
                    if (currentPosition < imageUris.size()) {
                        imageUris.remove(currentPosition);
                        adapter.notifyItemRemoved(currentPosition);
                        if (imageUris.isEmpty()) {
                            finish();
                        }
                    }
                } else {
                    Toast.makeText(this, "Photo not moved to trash", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        viewPager = findViewById(R.id.view_pager);

        images = ImageDataHolder.getInstance().getImageList();
        int currentPosition = getIntent().getIntExtra(EXTRA_IMAGE_POSITION, 0);

        if (images != null && !images.isEmpty()) {
            // Create a mutable list for modifications
            imageUris = new ArrayList<>(images.stream().map(Image::getUri).collect(Collectors.toList()));
            adapter = new PhotoPagerAdapter(this, imageUris);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(currentPosition, false);
        } else {
            imageUris = new ArrayList<>();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo_view_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            trashCurrentImage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void trashCurrentImage() {
        if (imageUris == null || imageUris.isEmpty()) {
            return;
        }

        int currentPosition = viewPager.getCurrentItem();
        Uri imageUri = imageUris.get(currentPosition);

        ContentResolver contentResolver = getContentResolver();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                List<Uri> urisToTrash = Collections.singletonList(imageUri);
                IntentSender intentSender = MediaStore.createTrashRequest(contentResolver, urisToTrash, true).getIntentSender();
                IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                trashResultLauncher.launch(request);
            } else {
                // For older versions, this will permanently delete.
                // A custom trash implementation would be needed for a true trash feature.
                contentResolver.delete(imageUri, null, null);
                Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
                imageUris.remove(currentPosition);
                adapter.notifyItemRemoved(currentPosition);

                if (imageUris.isEmpty()) {
                    finish();
                }
            }
        } catch (SecurityException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e instanceof RecoverableSecurityException) {
                RecoverableSecurityException rse = (RecoverableSecurityException) e;
                IntentSenderRequest request = new IntentSenderRequest.Builder(rse.getUserAction().getActionIntent().getIntentSender()).build();
                trashResultLauncher.launch(request);
            } else {
                Toast.makeText(this, "Error: Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageDataHolder.getInstance().setImageList(null);
    }
}