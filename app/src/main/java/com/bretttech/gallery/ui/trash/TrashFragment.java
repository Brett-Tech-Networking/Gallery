package com.bretttech.gallery.ui.trash;

import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
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
import com.bretttech.gallery.databinding.FragmentTrashBinding;
import com.bretttech.gallery.ui.pictures.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TrashFragment extends Fragment implements androidx.appcompat.view.ActionMode.Callback { // Implement ActionMode.Callback for CAB

    private FragmentTrashBinding binding;
    private TrashViewModel trashViewModel;
    private TrashAdapter trashAdapter;
    private List<Image> images = new ArrayList<>(); // NEW: To hold the list for PhotoViewActivity

    private androidx.appcompat.view.ActionMode actionMode; // NEW: To manage the CAB

    private final ActivityResultLauncher<IntentSenderRequest> actionResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                trashViewModel.loadTrashedImages(); // Refresh the list
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setHasOptionsMenu(true); // REMOVED: Menu is now managed by the CAB
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTrashBinding.inflate(inflater, container, false);
        trashViewModel = new ViewModelProvider(this).get(TrashViewModel.class);

        setupRecyclerView();

        trashViewModel.getTrashedImages().observe(getViewLifecycleOwner(), images -> {
            this.images = images; // Store the full list locally
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
        // UPDATED: New adapter constructor with two listeners
        trashAdapter = new TrashAdapter(
                // 1. Single-click (for viewing or toggling selection)
                image -> {
                    if (actionMode != null) {
                        // In selection mode, a single click toggles selection
                        toggleSelection(image);
                    } else {
                        // Not in selection mode, view the image (Feature 3)
                        if (images != null && !images.isEmpty()) {
                            ImageDataHolder.getInstance().setImageList(images);
                            Intent intent = new Intent(getContext(), PhotoViewActivity.class);
                            intent.putExtra(PhotoViewActivity.EXTRA_IMAGE_POSITION, images.indexOf(image));
                            startActivity(intent);
                        }
                    }
                },
                // 2. Long-click (for starting/toggling selection)
                this::toggleSelection
        );

        binding.recyclerViewTrash.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.recyclerViewTrash.setAdapter(trashAdapter);
    }

    // NEW: Method to handle selection logic
    private void toggleSelection(Image image) {
        trashAdapter.toggleSelection(image);
        int selectionCount = trashAdapter.getSelectedImages().size();

        if (selectionCount > 0) {
            if (actionMode == null) {
                // Start Contextual Action Bar (CAB)
                actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(this);
            }
            // Update the CAB title
            actionMode.setTitle(selectionCount + " selected");
        } else {
            // Exit CAB if nothing is selected
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    // Implementation of ActionMode.Callback (NEW)
    @Override
    public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.trash_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode, MenuItem item) {
        List<Uri> uris = trashAdapter.getSelectedImages().stream().map(Image::getUri).collect(Collectors.toList());

        if (uris.isEmpty()) {
            Toast.makeText(getContext(), "No images selected", Toast.LENGTH_SHORT).show();
            return true;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.action_restore) {
            updateTrashStatus(uris, false);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_delete_forever) {
            deletePermanently(uris);
            mode.finish();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
        actionMode = null;
        trashAdapter.clearSelection();
    }

    // MODIFIED: Methods to handle list of URIs for batch operations

    private void updateTrashStatus(List<Uri> uris, boolean trash) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // MediaStore.createTrashRequest for multiple items
                // FIX: Removed the '!' operator. 'trash' (passed as false for restore) is the desired final state of isTrashed.
                IntentSender intentSender = MediaStore.createTrashRequest(contentResolver, uris, trash).getIntentSender();
                IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                actionResultLauncher.launch(request);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deletePermanently(List<Uri> uris) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // MediaStore.createDeleteRequest for multiple items
                IntentSender intentSender = MediaStore.createDeleteRequest(contentResolver, uris).getIntentSender();
                IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                actionResultLauncher.launch(request);
            } else {
                // For older versions, iterate and attempt to delete.
                for (Uri uri : uris) {
                    contentResolver.delete(uri, null, null);
                }
                trashViewModel.loadTrashedImages(); // Refresh the list manually
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        // Reset subtitle when leaving the fragment
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setSubtitle(null);
    }
}