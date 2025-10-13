package com.bretttech.gallery.ui.pictures;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.ImageDataHolder;
import com.bretttech.gallery.PhotoViewActivity;
import com.bretttech.gallery.VideoPlayerActivity; // NEW IMPORT
import com.bretttech.gallery.databinding.FragmentPicturesBinding;

import java.util.ArrayList;
import java.util.List;

public class PicturesFragment extends Fragment {

    private FragmentPicturesBinding binding;
    private PicturesViewModel picturesViewModel;
    private PicturesAdapter picturesAdapter;
    private List<Image> images = new ArrayList<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    picturesViewModel.loadImages();
                } else {
                    Toast.makeText(getContext(), "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        picturesViewModel = new ViewModelProvider(this).get(PicturesViewModel.class);
        binding = FragmentPicturesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();

        picturesViewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            this.images = images;
            picturesAdapter.setImages(images);
        });

        requestStoragePermission();

        return root;
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewPictures;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // UPDATED: Check media type to launch appropriate activity
        picturesAdapter = new PicturesAdapter(image -> {
            if (image.isVideo()) {
                Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, image.getUri());
                startActivity(intent);
            } else if (images != null && !images.isEmpty()) {
                // Original logic for images
                ImageDataHolder.getInstance().setImageList(images);
                Intent intent = new Intent(getContext(), PhotoViewActivity.class);
                intent.putExtra(PhotoViewActivity.EXTRA_IMAGE_POSITION, images.indexOf(image));
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(picturesAdapter);
    }

    private void requestStoragePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            picturesViewModel.loadImages();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}