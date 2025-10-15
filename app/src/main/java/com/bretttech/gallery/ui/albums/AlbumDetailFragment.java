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
import com.bretttech.gallery.data.FavoritesManager;
import com.bretttech.gallery.databinding.FragmentAlbumDetailBinding;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.ui.pictures.MoveToAlbumDialogFragment;
import com.bretttech.gallery.ui.pictures.PicturesAdapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AlbumDetailFragment extends Fragment implements androidx.appcompat.view.ActionMode.Callback {

    private FragmentAlbumDetailBinding binding;
    private AlbumDetailViewModel viewModel;
    private PicturesAdapter picturesAdapter;
    private List<Image> images;
    private String albumFolderPath;
    private androidx.appcompat.view.ActionMode actionMode;
    private AlbumsViewModel albumsViewModel;
    private FavoritesManager favoritesManager;

    private final ActivityResultLauncher<IntentSenderRequest> trashResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    Toast.makeText(getContext(), "Item(s) moved to trash", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to move item(s) to trash", Toast.LENGTH_SHORT).show();
                }
                viewModel.loadImagesFromAlbum(albumFolderPath);
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        albumsViewModel = new ViewModelProvider(requireActivity()).get(AlbumsViewModel.class);
        favoritesManager = new FavoritesManager(requireContext());

        getParentFragmentManager().setFragmentResultListener(MoveToAlbumDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            boolean success = bundle.getBoolean(MoveToAlbumDialogFragment.KEY_MOVE_SUCCESS);
            if (success) {
                ArrayList<Uri> movedUris = bundle.getParcelableArrayList(MoveToAlbumDialogFragment.KEY_MOVED_URIS);
                if (picturesAdapter != null && movedUris != null) {
                    picturesAdapter.removeImagesByUri(movedUris);
                }
                if (albumsViewModel != null) {
                    albumsViewModel.loadAlbums();
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

        viewModel.getImages().observe(getViewLifecycleOwner(), imageList -> {
            this.images = imageList;
            picturesAdapter.setImages(imageList);
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

    @Override
    public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.multi_select_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        MenuItem setWallpaperItem = menu.findItem(R.id.action_set_wallpaper);
        List<Image> selectedImages = picturesAdapter.getSelectedImages();
        setWallpaperItem.setVisible(selectedImages.size() == 1 && !selectedImages.get(0).isVideo());
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
        if (itemId == R.id.action_share) {
            shareImages(uris);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_delete) {
            trashMediaItems(uris);
            mode.finish();
        } else if (itemId == R.id.action_set_wallpaper) {
            if (selectedImages.size() == 1) setWallpaper(selectedImages.get(0));
            mode.finish();
        } else if (itemId == R.id.action_move_to_album) {
            MoveToAlbumDialogFragment.newInstance(uris, false).show(getParentFragmentManager(), MoveToAlbumDialogFragment.TAG);
            mode.finish();
        } else if (item.getItemId() == R.id.action_move_to_secure_folder) {
            MoveToAlbumDialogFragment.newInstance(uris, true).show(getParentFragmentManager(), MoveToAlbumDialogFragment.TAG);
            mode.finish();
        } else {
            return false;
        }
        return true;
    }

    private void shareImages(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) {
            return;
        }

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(uris));
        shareIntent.setType("*/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share to..."));
    }

    @Override
    public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
        actionMode = null;
        picturesAdapter.clearSelection();
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
                viewModel.loadImagesFromAlbum(albumFolderPath);
            }
            for (Uri uri : uris) {
                for (Image image : images) {
                    if (image.getUri().equals(uri)) {
                        favoritesManager.removeFavorite(image);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void setWallpaper(Image image) {
        if (image.isVideo()) {
            Toast.makeText(getContext(), R.string.wallpaper_error, Toast.LENGTH_SHORT).show();
            return;
        }
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(image.getUri())) {
            WallpaperManager.getInstance(getContext()).setStream(inputStream);
            Toast.makeText(getContext(), "Wallpaper set successfully!", Toast.LENGTH_LONG).show();
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