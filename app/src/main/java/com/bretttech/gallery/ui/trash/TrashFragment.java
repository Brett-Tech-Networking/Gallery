package com.bretttech.gallery.ui.trash;

import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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

import com.bretttech.gallery.R;
import com.bretttech.gallery.databinding.FragmentTrashBinding;
import com.bretttech.gallery.ui.pictures.Image;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TrashFragment extends Fragment {

    private FragmentTrashBinding binding;
    private TrashViewModel trashViewModel;
    private TrashAdapter trashAdapter;
    private Image selectedImage;

    private final ActivityResultLauncher<IntentSenderRequest> actionResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                trashViewModel.loadTrashedImages(); // Refresh the list
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTrashBinding.inflate(inflater, container, false);
        trashViewModel = new ViewModelProvider(this).get(TrashViewModel.class);

        setupRecyclerView();

        trashViewModel.getTrashedImages().observe(getViewLifecycleOwner(), images -> {
            trashAdapter.setTrashedImages(images);
            if (images != null) {
                String subtitle = images.size() + " items";
                ((AppCompatActivity) requireActivity()).getSupportActionBar().setSubtitle(subtitle);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        trashViewModel.loadTrashedImages();
    }

    private void setupRecyclerView() {
        trashAdapter = new TrashAdapter(image -> {
            selectedImage = image;
            // You can add selection highlighting here if desired
        });
        binding.recyclerViewTrash.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.recyclerViewTrash.setAdapter(trashAdapter);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.trash_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (selectedImage == null) {
            Toast.makeText(getContext(), "Please select an image first", Toast.LENGTH_SHORT).show();
            return super.onOptionsItemSelected(item);
        }

        int itemId = item.getItemId();
        if (itemId == R.id.action_restore) {
            updateTrashStatus(selectedImage.getUri(), false);
            return true;
        } else if (itemId == R.id.action_delete_forever) {
            deletePermanently(selectedImage.getUri());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateTrashStatus(Uri uri, boolean trash) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                List<Uri> uris = Collections.singletonList(uri);
                IntentSender intentSender = MediaStore.createTrashRequest(contentResolver, uris, !trash).getIntentSender();
                IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                actionResultLauncher.launch(request);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deletePermanently(Uri uri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        try {
            contentResolver.delete(uri, null, null);
            trashViewModel.loadTrashedImages();
        } catch (SecurityException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e instanceof RecoverableSecurityException) {
                RecoverableSecurityException rse = (RecoverableSecurityException) e;
                IntentSenderRequest request = new IntentSenderRequest.Builder(rse.getUserAction().getActionIntent().getIntentSender()).build();
                actionResultLauncher.launch(request);
            } else {
                Toast.makeText(getContext(), "Error: Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        // Reset subtitle when leaving the fragment
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setSubtitle(null);
    }
}