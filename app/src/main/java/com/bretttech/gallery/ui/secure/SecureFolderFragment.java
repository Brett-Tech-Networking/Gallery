package com.bretttech.gallery.ui.secure;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bretttech.gallery.ImageDataHolder;
import com.bretttech.gallery.PhotoViewActivity;
import com.bretttech.gallery.R;
import com.bretttech.gallery.VideoPlayerActivity;
import com.bretttech.gallery.auth.BiometricAuthManager;
import com.bretttech.gallery.databinding.FragmentSecureFolderBinding;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.ui.pictures.MoveToAlbumDialogFragment;
import com.bretttech.gallery.ui.pictures.PicturesAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SecureFolderFragment extends Fragment implements androidx.appcompat.view.ActionMode.Callback {

    private FragmentSecureFolderBinding binding;
    private SecureFolderViewModel secureFolderViewModel;
    private PicturesAdapter picturesAdapter;
    private NavController navController;
    private List<Image> images = new ArrayList<>();
    private androidx.appcompat.view.ActionMode actionMode;


    private final ActivityResultLauncher<Intent> pinSetupLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    authenticate();
                } else {
                    navController.popBackStack();
                }
            });

    private final ActivityResultLauncher<Intent> pinEntryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    loadContent();
                } else {
                    navController.popBackStack();
                }
            });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSecureFolderBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);

        setupRecyclerView();

        secureFolderViewModel = new ViewModelProvider(this).get(SecureFolderViewModel.class);
        secureFolderViewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            this.images = images;
            picturesAdapter.setImages(images);
        });

        getParentFragmentManager().setFragmentResultListener(MoveToAlbumDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            boolean success = bundle.getBoolean(MoveToAlbumDialogFragment.KEY_MOVE_SUCCESS);
            if (success) {
                // Refresh the content of the secure folder
                loadContent();
            }
        });


        authenticate();

        return binding.getRoot();
    }

    private void authenticate() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("secure_folder_prefs", Context.MODE_PRIVATE);
        if (prefs.contains("pin_hash")) {
            pinEntryLauncher.launch(new Intent(getContext(), PinEntryActivity.class));
            return;
        }

        new BiometricAuthManager((AppCompatActivity) requireActivity(), new BiometricAuthManager.BiometricAuthListener() {
            @Override
            public void onAuthSuccess() {
                Toast.makeText(getContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                loadContent();
            }

            @Override
            public void onAuthError(String errString) {
                Toast.makeText(getContext(), errString, Toast.LENGTH_SHORT).show();
                navController.popBackStack();
            }

            @Override
            public void onAuthFailed() {
                Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoSecurityEnrolled() {
                pinSetupLauncher.launch(new Intent(getContext(), PinSetupActivity.class));
            }
        }).authenticate();
    }

    private void loadContent() {
        secureFolderViewModel.loadImagesFromSecureFolder();
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
        binding.recyclerViewSecureFolder.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.recyclerViewSecureFolder.setAdapter(picturesAdapter);
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
        binding = null;
    }

    @Override
    public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.secure_folder_menu, menu);
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

        if (uris.isEmpty()) {
            return true;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.action_move_out) {
            MoveToAlbumDialogFragment.newInstance(uris, false).show(getParentFragmentManager(), MoveToAlbumDialogFragment.TAG);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_delete) {
            for (Uri uri : uris) {
                new File(uri.getPath()).delete();
            }
            secureFolderViewModel.loadImagesFromSecureFolder();
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