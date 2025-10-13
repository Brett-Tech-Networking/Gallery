package com.bretttech.gallery.ui.secure;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SecureAlbumDetailFragment extends Fragment implements androidx.appcompat.view.ActionMode.Callback {

    private FragmentAlbumDetailBinding binding;
    private SecureAlbumDetailViewModel viewModel;
    private PicturesAdapter picturesAdapter;
    private List<Image> images = new ArrayList<>();
    private String albumFolderPath;
    private androidx.appcompat.view.ActionMode actionMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlbumDetailBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(SecureAlbumDetailViewModel.class);

        albumFolderPath = getArguments() != null ? getArguments().getString("albumFolderPath") : null;
        if (albumFolderPath != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(new File(albumFolderPath).getName());
        }

        setupRecyclerView();

        if (albumFolderPath != null) {
            viewModel.loadImagesFromSecureAlbum(albumFolderPath);
        }

        viewModel.getImages().observe(getViewLifecycleOwner(), imageList -> {
            this.images = imageList;
            picturesAdapter.setImages(imageList);
        });

        getParentFragmentManager().setFragmentResultListener(MoveToAlbumDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            if (bundle.getBoolean(MoveToAlbumDialogFragment.KEY_MOVE_SUCCESS)) {
                viewModel.loadImagesFromSecureAlbum(albumFolderPath); // Refresh
            }
        });

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        picturesAdapter = new PicturesAdapter(
                image -> { // onClick
                    if (actionMode != null) {
                        toggleSelection(image);
                    } else {
                        Intent intent;
                        if (image.isVideo()) {
                            intent = new Intent(getContext(), VideoPlayerActivity.class);
                            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, image.getUri());
                        } else {
                            ImageDataHolder.getInstance().setImageList(images);
                            intent = new Intent(getContext(), PhotoViewActivity.class);
                            intent.putExtra(PhotoViewActivity.EXTRA_IMAGE_POSITION, images.indexOf(image));
                        }
                        intent.putExtra("isSecure", true); // Let PhotoViewActivity know it's in secure mode
                        startActivity(intent);
                    }
                },
                this::toggleSelection // onLongClick
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
        } else {
            if (actionMode != null) {
                actionMode.finish();
            }
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

    @Override
    public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.secure_album_detail_menu, menu);
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
        if (itemId == R.id.action_move_out) {
            // Move to a public album
            MoveToAlbumDialogFragment.newInstance(uris, false).show(getParentFragmentManager(), MoveToAlbumDialogFragment.TAG);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_move_to_another_secure_album) {
            // Move to another secure album
            MoveToAlbumDialogFragment.newInstance(uris, true).show(getParentFragmentManager(), MoveToAlbumDialogFragment.TAG);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_delete) {
            for (Uri uri : uris) {
                new File(uri.getPath()).delete();
            }
            viewModel.loadImagesFromSecureAlbum(albumFolderPath); // Refresh
            Toast.makeText(getContext(), "Deleted " + uris.size() + " items", Toast.LENGTH_SHORT).show();
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
}