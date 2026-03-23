package com.bretttech.gallery.ui.duplicates;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;
import com.bretttech.gallery.databinding.FragmentDuplicatesBinding;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.utils.DuplicateFinder;

import java.util.ArrayList;
import java.util.List;

public class DuplicatesFragment extends Fragment {

    private FragmentDuplicatesBinding binding;
    private DuplicatesViewModel viewModel;
    private DuplicateGroupsAdapter adapter;
    private DuplicateFinder duplicateFinder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDuplicatesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DuplicatesViewModel.class);
        duplicateFinder = new DuplicateFinder(requireContext());

        setupRecyclerView();
        setupButtons();

        viewModel.getAllImages().observe(getViewLifecycleOwner(), images -> {
            if (!images.isEmpty()) {
                startDuplicateScan(images);
            } else {
                binding.emptyStateMessage.setText(R.string.no_duplicates_found);
                binding.emptyStateMessage.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
                binding.duplicatesRecyclerView.setVisibility(View.GONE);
            }
        });

        viewModel.getDuplicateGroups().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null && !groups.isEmpty()) {
                adapter.setDuplicateGroups(groups);
                binding.emptyStateMessage.setVisibility(View.GONE);
                binding.duplicatesRecyclerView.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
                updateStatusMessage(groups);
            } else {
                binding.emptyStateMessage.setText(R.string.no_duplicates_found);
                binding.emptyStateMessage.setVisibility(View.VISIBLE);
                binding.duplicatesRecyclerView.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        // Load all images and start scan
        viewModel.loadAllImages();
    }

    private void setupRecyclerView() {
        adapter = new DuplicateGroupsAdapter(requireContext());
        binding.duplicatesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.duplicatesRecyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.buttonDeleteDuplicates.setOnClickListener(v -> deleteDuplicates());
        binding.buttonRescan.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.duplicatesRecyclerView.setVisibility(View.GONE);
            viewModel.loadAllImages();
        });
    }

    private void startDuplicateScan(List<Image> images) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.duplicatesRecyclerView.setVisibility(View.GONE);
        binding.emptyStateMessage.setVisibility(View.GONE);

        duplicateFinder.findDuplicates(images, new DuplicateFinder.DuplicateFinderCallback() {
            @Override
            public void onDuplicatesFound(List<DuplicateFinder.DuplicateGroup> duplicateGroups) {
                viewModel.setDuplicateGroups(duplicateGroups);
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyStateMessage.setVisibility(View.VISIBLE);
                    binding.emptyStateMessage.setText(error);
                });
            }
        });
    }

    private void deleteDuplicates() {
        List<Image> selectedImages = adapter.getSelectedDuplicates();
        if (selectedImages.isEmpty()) {
            Toast.makeText(requireContext(), "No duplicates selected for deletion", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.deleteDuplicates(selectedImages);
        Toast.makeText(requireContext(), selectedImages.size() + " duplicate(s) deleted", Toast.LENGTH_SHORT).show();

        // Refresh the list
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.loadAllImages();
    }

    private void updateStatusMessage(List<DuplicateFinder.DuplicateGroup> groups) {
        int totalDuplicates = 0;
        for (DuplicateFinder.DuplicateGroup group : groups) {
            totalDuplicates += group.images.size() - 1; // -1 because one is the original
        }
        String message = String.format("Found %d group(s) with %d duplicate(s)", groups.size(), totalDuplicates);
        binding.statusText.setText(message);
        binding.statusText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        duplicateFinder.shutdown();
        binding = null;
    }
}
