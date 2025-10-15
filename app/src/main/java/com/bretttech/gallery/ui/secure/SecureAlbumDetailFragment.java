package com.bretttech.gallery.ui.secure;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.bretttech.gallery.ImageDataHolder;
import com.bretttech.gallery.PhotoViewActivity;
import com.bretttech.gallery.R;
import com.bretttech.gallery.databinding.FragmentAlbumDetailBinding;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.ui.pictures.PicturesAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SecureAlbumDetailFragment extends Fragment implements ActionMode.Callback {

    private FragmentAlbumDetailBinding binding;
    private SecureAlbumDetailViewModel viewModel;
    private PicturesAdapter picturesAdapter;
    private List<Image> images;
    private String albumFolderPath;
    private ActionMode actionMode;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlbumDetailBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(SecureAlbumDetailViewModel.class);

        albumFolderPath = getArguments() != null ? getArguments().getString("albumFolderPath") : null;

        setupRecyclerView();

        if (albumFolderPath != null) {
            viewModel.loadImages(albumFolderPath);

            if (getActivity() instanceof AppCompatActivity) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(new File(albumFolderPath).getName());
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
            viewModel.loadImages(albumFolderPath); // Refresh
        }
    }

    private void setupRecyclerView() {
        picturesAdapter = new PicturesAdapter(
                image -> {
                    if (actionMode != null) {
                        toggleSelection(image);
                    } else {
                        if (images != null && !images.isEmpty()) {
                            ImageDataHolder.getInstance().setImageList(images);
                            Intent intent = new Intent(getContext(), PhotoViewActivity.class);
                            intent.putExtra(PhotoViewActivity.EXTRA_IMAGE_POSITION, images.indexOf(image));
                            intent.putExtra("isSecure", true); // Let PhotoViewActivity know it's in secure mode
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.secure_album_detail_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_move_out) {
            // Your logic for moving images out of the secure folder
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.secure_album_context_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        List<Image> selectedImages = picturesAdapter.getSelectedImages();
        List<Uri> uris = selectedImages.stream().map(Image::getUri).collect(Collectors.toList());

        if (uris.isEmpty()) {
            return true;
        }

        if (item.getItemId() == R.id.action_delete_secure) {
            deleteSecureImages(uris);
            mode.finish();
            return true;
        }
        return false;
    }

    private void deleteSecureImages(List<Uri> uris) {
        for (Uri uri : uris) {
            File file = new File(uri.getPath());
            if (file.exists()) {
                file.delete();
            }
        }
        Toast.makeText(getContext(), uris.size() + " item(s) deleted", Toast.LENGTH_SHORT).show();
        viewModel.loadImages(albumFolderPath); // Refresh
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
        picturesAdapter.clearSelection();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}