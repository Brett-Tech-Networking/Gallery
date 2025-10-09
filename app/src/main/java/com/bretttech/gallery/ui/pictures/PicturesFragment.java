package com.bretttech.gallery.ui.pictures;

import android.Manifest;
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

import com.bretttech.gallery.databinding.FragmentPicturesBinding;

public class PicturesFragment extends Fragment {

    private FragmentPicturesBinding binding;
    private PicturesViewModel picturesViewModel;
    private PicturesAdapter picturesAdapter;

    // Prepare the permission request launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your app.
                    picturesViewModel.loadImages();
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied.
                    Toast.makeText(getContext(), "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
            });

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        picturesViewModel = new ViewModelProvider(this).get(PicturesViewModel.class);
        binding = FragmentPicturesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();

        picturesViewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            picturesAdapter.setImages(images);
        });

        requestStoragePermission();

        return root;
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewPictures;
        // Display images in a grid with 3 columns
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        picturesAdapter = new PicturesAdapter();
        recyclerView.setAdapter(picturesAdapter);
    }

    private void requestStoragePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            // Older versions
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            picturesViewModel.loadImages();
        } else {
            // You can directly ask for the permission.
            requestPermissionLauncher.launch(permission);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
