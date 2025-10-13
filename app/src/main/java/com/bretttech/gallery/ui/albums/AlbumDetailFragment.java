package com.bretttech.gallery.ui.albums;

import android.app.RecoverableSecurityException;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bretttech.gallery.ImageDataHolder;
import com.bretttech.gallery.PhotoViewActivity;
import com.bretttech.gallery.R;
import com.bretttech.gallery.VideoPlayerActivity;
import com.bretttech.gallery.databinding.FragmentAlbumDetailBinding;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.ui.pictures.MoveToAlbumDialogFragment;
import com.bretttech.gallery.ui.pictures.PicturesAdapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

// MODIFIED: Removed the old OnMoveCompleteListener
public class AlbumDetailFragment extends Fragment implements androidx.appcompat.view.ActionMode.Callback {

    private FragmentAlbumDetailBinding binding;
    private AlbumDetailViewModel viewModel;
    private PicturesAdapter picturesAdapter;
    private List<Image> images;
    private String albumFolderPath;
    private androidx.appcompat.view.ActionMode actionMode;

    private final ActivityResultLauncher<IntentSenderRequest> actionResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    Toast.makeText(getContext(), R.string.delete_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), R.string.delete_fail, Toast.LENGTH_SHORT).show();
                }
                if (albumFolderPath != null) {
                    viewModel.loadImagesFromAlbum(albumFolderPath);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NEW: This is the robust listener that will receive the result from the move dialog.
        getParentFragmentManager().setFragmentResultListener(MoveToAlbumDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            boolean success = bundle.getBoolean(MoveToAlbumDialogFragment.KEY_MOVE_SUCCESS);
            if (success) {
                // If the move was successful, reload the images for this album.
                // This will fetch the new, valid URIs and fix the "broken files" issue.
                if (viewModel != null && albumFolderPath != null) {
                    viewModel.loadImagesFromAlbum(albumFolderPath);
                }
            }
        });
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAlbumDetailBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(AlbumDetailViewModel.class);

        albumFolderPath = getArguments() != null ? getArguments().getString("albumFolderPath") : null;

        setupRecyclerView();

        if (albumFolderPath != null) {
            viewModel.loadImagesFromAlbum(albumFolderPath);

            if (((AppCompatActivity) getActivity()) != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                String albumName = new File(albumFolderPath).getName();
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(albumName);
            }
        }

        viewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            this.images = images;
            picturesAdapter.setImages(images);
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (albumFolderPath != null && actionMode == null) {
            viewModel.loadImagesFromAlbum(albumFolderPath);
        }
    }

    private void setupRecyclerView() {
        picturesAdapter = new PicturesAdapter(
                // 1. Single-click listener
                image -> {
                    if (actionMode != null) {
                        toggleSelection(image);
                    } else {
                        if (image.isVideo()) {
                            Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
                            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, image.getUri());
                            startActivity(intent);
                        } else if (images != null && !images.isEmpty()) {
                            ImageDataHolder.getInstance().setImageList(images);
                            Intent intent = new Intent(getContext(), PhotoViewActivity.class);
                            intent.putExtra(PhotoViewActivity.EXTRA_IMAGE_POSITION, images.indexOf(image));
                            startActivity(intent);
                        }
                    }
                },
                // 2. Long-click listener (starts selection mode)
                this::toggleSelection
        );
        binding.recyclerViewAlbumDetail.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.recyclerViewAlbumDetail.setAdapter(picturesAdapter);
    }

    private void toggleSelection(Image image) {
        picturesAdapter.toggleSelection(image);
        int selectionCount = picturesAdapter.getSelectedImages().size();

        if (selectionCount > 0) {
            if (actionMode == null) {
                actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(this);
            }
            actionMode.setTitle(selectionCount + " selected");
            actionMode.invalidate();
        } else {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    // --- ActionMode.Callback Implementation ---
    @Override
    public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.multi_select_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        MenuItem setWallpaperItem = menu.findItem(R.id.action_set_wallpaper);
        List<Image> selectedImages = picturesAdapter.getSelectedImages();

        boolean isSingleImage = selectedImages.size() == 1
                && !selectedImages.get(0).isVideo();

        setWallpaperItem.setVisible(isSingleImage);

        MenuItem moveToAlbumItem = menu.findItem(R.id.action_move_to_album);
        moveToAlbumItem.setVisible(!selectedImages.isEmpty());

        return true;
    }

    @Override
    public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode, MenuItem item) {
        List<Image> selectedImages = picturesAdapter.getSelectedImages();
        List<Uri> uris = selectedImages.stream().map(Image::getUri).collect(Collectors.toList());

        if (uris.isEmpty()) {
            Toast.makeText(getContext(), "No items selected", Toast.LENGTH_SHORT).show();
            return true;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.action_delete) {
            trashMediaItems(uris);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_set_wallpaper) {
            if (selectedImages.size() == 1) {
                setWallpaper(selectedImages.get(0));
                mode.finish();
                return true;
            }
        } else if (itemId == R.id.action_move_to_album) {
            MoveToAlbumDialogFragment dialog = MoveToAlbumDialogFragment.newInstance(uris);
            dialog.show(getParentFragmentManager(), MoveToAlbumDialogFragment.TAG);
            mode.finish();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
        actionMode = null;
        picturesAdapter.clearSelection();
    }

    // REMOVED: The old onMoveComplete method is no longer needed.

    private void trashMediaItems(List<Uri> uris) {
        // ... this method remains the same ...
        ContentResolver contentResolver = requireContext().getContentResolver();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                IntentSender intentSender = MediaStore.createTrashRequest(contentResolver, uris, true).getIntentSender();
                IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                actionResultLauncher.launch(request);
            } else {
                for (Uri uri : uris) {
                    contentResolver.delete(uri, null, null);
                }
                viewModel.loadImagesFromAlbum(albumFolderPath);
                Toast.makeText(getContext(), getString(R.string.delete_success, uris.size()), Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e instanceof RecoverableSecurityException) {
                RecoverableSecurityException rse = (RecoverableSecurityException) e;
                IntentSenderRequest request = new IntentSenderRequest.Builder(rse.getUserAction().getActionIntent().getIntentSender()).build();
                actionResultLauncher.launch(request);
            } else {
                Toast.makeText(getContext(), "Error: Permission denied", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setWallpaper(Image image) {
        // ... this method remains the same ...
        if (image.isVideo()) {
            Toast.makeText(getContext(), R.string.wallpaper_error, Toast.LENGTH_SHORT).show();
            return;
        }
        Context context = requireContext();
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        try (InputStream inputStream = context.getContentResolver().openInputStream(image.getUri())) {
            if (inputStream != null) {
                wallpaperManager.setStream(inputStream);
                Toast.makeText(context, "Wallpaper set successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Failed to set wallpaper: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (actionMode != null) {
            actionMode.finish();
        }
        binding = null;
    }
}