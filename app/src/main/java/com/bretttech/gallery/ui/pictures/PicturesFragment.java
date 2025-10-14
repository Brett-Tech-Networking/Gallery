package com.bretttech.gallery.ui.pictures;

import android.Manifest;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bretttech.gallery.ImageDataHolder;
import com.bretttech.gallery.PhotoViewActivity;
import com.bretttech.gallery.R;
import com.bretttech.gallery.VideoPlayerActivity;
import com.bretttech.gallery.databinding.FragmentPicturesBinding;
import com.bretttech.gallery.ui.albums.AlbumsViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PicturesFragment extends Fragment implements androidx.appcompat.view.ActionMode.Callback {

    private FragmentPicturesBinding binding;
    private PicturesViewModel picturesViewModel;
    private PicturesAdapter picturesAdapter;
    private List<Image> images = new ArrayList<>();
    private androidx.appcompat.view.ActionMode actionMode;
    private AlbumsViewModel albumsViewModel;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    picturesViewModel.loadImages();
                } else {
                    Toast.makeText(getContext(), "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<IntentSenderRequest> trashResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    Toast.makeText(getContext(), "Item(s) moved to trash", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to move item(s) to trash", Toast.LENGTH_SHORT).show();
                }
                picturesViewModel.loadImages();
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        albumsViewModel = new ViewModelProvider(requireActivity()).get(AlbumsViewModel.class);

        getParentFragmentManager().setFragmentResultListener(MoveToAlbumDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            boolean success = bundle.getBoolean(MoveToAlbumDialogFragment.KEY_MOVE_SUCCESS);
            if (success) {
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
                }
                if (albumsViewModel != null) {
                    albumsViewModel.loadAlbums();
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        picturesViewModel = new ViewModelProvider(this).get(PicturesViewModel.class);
        binding = FragmentPicturesBinding.inflate(inflater, container, false);

        setupRecyclerView();

        picturesViewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            this.images = images;
            picturesAdapter.setImages(images);
        });

        requestStoragePermission();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (actionMode == null) {
            picturesViewModel.loadImages();
        }
    }

    private void setupRecyclerView() {
        picturesAdapter = new PicturesAdapter(
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
                this::toggleSelection
        );
        binding.recyclerViewPictures.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.recyclerViewPictures.setAdapter(picturesAdapter);
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

    @Override
    public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.multi_select_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode, MenuItem item) {
        List<Image> selectedImages = picturesAdapter.getSelectedImages();
        List<Uri> uris = selectedImages.stream().map(Image::getUri).collect(Collectors.toList());

        if (uris.isEmpty()) return true;

        int itemId = item.getItemId();
        if (itemId == R.id.action_share) {
            shareImages(uris);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_move_to_album) {
            MoveToAlbumDialogFragment.newInstance(uris, false).show(getParentFragmentManager(), MoveToAlbumDialogFragment.TAG);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_move_to_secure_folder) {
            MoveToAlbumDialogFragment.newInstance(uris, true).show(getParentFragmentManager(), MoveToAlbumDialogFragment.TAG);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_delete) {
            trashMediaItems(uris);
            mode.finish();
            return true;
        }
        return false;
    }

    private void shareImages(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) {
            return;
        }

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(uris));
        shareIntent.setType("*/*"); // Handle both images and videos
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share to..."));
    }

    private void trashMediaItems(List<Uri> uris) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                IntentSender intentSender = MediaStore.createTrashRequest(contentResolver, uris, true).getIntentSender();
                trashResultLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
            } else {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    for (Uri uri : uris) {
                        try {
                            contentResolver.delete(uri, null, null);
                        } catch (RecoverableSecurityException e) {
                            IntentSenderRequest request = new IntentSenderRequest.Builder(e.getUserAction().getActionIntent().getIntentSender()).build();
                            trashResultLauncher.launch(request);
                            return;
                        }
                    }
                } else {
                    for (Uri uri : uris) {
                        contentResolver.delete(uri, null, null);
                    }
                    Toast.makeText(getContext(), uris.size() + " item(s) permanently deleted", Toast.LENGTH_SHORT).show();
                }
                picturesViewModel.loadImages();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
        actionMode = null;
        picturesAdapter.clearSelection();
    }

    private void requestStoragePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            picturesViewModel.loadImages();
        } else {
            requestPermissionLauncher.launch(permission);
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