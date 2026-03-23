package com.bretttech.gallery.ui.shredding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.ImageDataHolder;
import com.bretttech.gallery.R;
import com.bretttech.gallery.databinding.FragmentShreddingBinding;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.ui.pictures.PicturesAdapter;
import com.bretttech.gallery.utils.ImageShredder;

import java.util.ArrayList;
import java.util.List;

public class ShreddingFragment extends Fragment implements androidx.appcompat.view.ActionMode.Callback {

    private FragmentShreddingBinding binding;
    private ShreddingViewModel viewModel;
    private PicturesAdapter adapter;
    private List<Image> images = new ArrayList<>();
    private androidx.appcompat.view.ActionMode actionMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShreddingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ShreddingViewModel.class);

        setupRecyclerView();
        setupButtons();

        viewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            this.images = images;
            adapter.setImages(images);

            if (images.isEmpty()) {
                binding.emptyStateMessage.setVisibility(View.VISIBLE);
                binding.recyclerViewImages.setVisibility(View.GONE);
                binding.buttonShreddingPanel.setVisibility(View.GONE);
            } else {
                binding.emptyStateMessage.setVisibility(View.GONE);
                binding.recyclerViewImages.setVisibility(View.VISIBLE);
                binding.buttonShreddingPanel.setVisibility(View.VISIBLE);
            }
        });

        viewModel.loadImages();
    }

    private void setupRecyclerView() {
        adapter = new PicturesAdapter(
                image -> {
                    if (actionMode != null) {
                        toggleSelection(image);
                    }
                },
                null
        );

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        binding.recyclerViewImages.setLayoutManager(layoutManager);
        binding.recyclerViewImages.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.buttonSelectAll.setOnClickListener(v -> selectAll());
        binding.buttonDeselect.setOnClickListener(v -> deselectAll());
        binding.buttonShred.setOnClickListener(v -> shreddSelectedImages());
    }

    private void toggleSelection(Image image) {
        adapter.toggleSelection(image);
    }

    private void selectAll() {
        for (Image image : images) {
            if (!adapter.getSelectedImages().contains(image)) {
                adapter.toggleSelection(image);
            }
        }
    }

    private void deselectAll() {
        adapter.clearSelection();
    }

    private void shreddSelectedImages() {
        List<Image> selectedImages = adapter.getSelectedImages();
        if (selectedImages.isEmpty()) {
            Toast.makeText(requireContext(), "No images selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirm Secure Deletion")
                .setMessage("This will permanently and securely delete " + selectedImages.size() + " image(s). This action cannot be undone.")
                .setPositiveButton("Shred", (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.statusText.setVisibility(View.VISIBLE);
                    binding.statusText.setText("Shredding " + selectedImages.size() + " image(s)...");

                    viewModel.shredImages(selectedImages, new ShreddingViewModel.ShredCallback() {
                        @Override
                        public void onProgress(int current, int total) {
                            binding.statusText.setText(String.format("Shredding... %d/%d", current, total));
                        }

                        @Override
                        public void onComplete(int successCount, int failureCount) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(),
                                    successCount + " deleted, " + failureCount + " failed",
                                    Toast.LENGTH_SHORT).show();
                            viewModel.loadImages();
                            deselectAll();
                            binding.statusText.setVisibility(View.GONE);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, android.view.Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, android.view.Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode, android.view.MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
        actionMode = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
