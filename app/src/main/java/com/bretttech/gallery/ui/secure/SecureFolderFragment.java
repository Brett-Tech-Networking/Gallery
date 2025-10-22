package com.bretttech.gallery.ui.secure;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.bretttech.gallery.GalleryApplication;
import com.bretttech.gallery.R;
import com.bretttech.gallery.auth.BiometricAuthManager;
import com.bretttech.gallery.databinding.FragmentSecureFolderBinding;
import com.bretttech.gallery.ui.albums.Album;
import com.bretttech.gallery.ui.albums.AlbumsAdapter;
import com.bretttech.gallery.ui.albums.AlbumsViewModel;
import com.bretttech.gallery.ui.albums.ChangeCoverActivity;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecureFolderFragment extends Fragment implements androidx.appcompat.view.ActionMode.Callback {

    private FragmentSecureFolderBinding binding;
    private SecureFolderViewModel secureFolderViewModel;
    private AlbumsAdapter albumsAdapter;
    private NavController navController;
    private androidx.appcompat.view.ActionMode actionMode;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ScaleGestureDetector scaleGestureDetector;
    private GridLayoutManager gridLayoutManager;
    private static final String PREF_SECURE_ALBUM_COLUMNS = "secure_album_columns";

    private final ActivityResultLauncher<Intent> pinSetupLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    authenticate();
                } else {
                    Toast.makeText(getContext(), "PIN setup is required to use the secure folder.", Toast.LENGTH_LONG).show();
                    navController.popBackStack();
                }
            });

    private final ActivityResultLauncher<Intent> pinEntryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    GalleryApplication.isSecureFolderUnlocked = true;
                    loadContent();
                } else {
                    Toast.makeText(getContext(), "Authentication required.", Toast.LENGTH_SHORT).show();
                    navController.popBackStack();
                }
            });

    private final ActivityResultLauncher<Intent> changeCoverLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                    secureFolderViewModel.loadAlbumsFromSecureFolder();
                    Toast.makeText(getContext(), "Cover photo changed!", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSecureFolderBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);

        setupRecyclerView();
        setupPinchToZoom();

        secureFolderViewModel = new ViewModelProvider(this).get(SecureFolderViewModel.class);
        secureFolderViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> {
            if (GalleryApplication.isSecureFolderUnlocked) {
                albumsAdapter.setAlbums(albums);
            }
        });

        setHasOptionsMenu(true);

        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.secure_folder_settings_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_secure_settings) {
            Intent intent = new Intent(getContext(), SecureSettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void authenticate() {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("secure_folder_prefs", Context.MODE_PRIVATE);

        if (!prefs.contains("pin_hash")) {
            pinSetupLauncher.launch(new Intent(getContext(), PinSetupActivity.class));
            return;
        }

        boolean useBiometrics = prefs.getBoolean("biometrics_enabled", true);

        if (useBiometrics) {
            new BiometricAuthManager((AppCompatActivity) requireActivity(), new BiometricAuthManager.BiometricAuthListener() {
                @Override
                public void onAuthSuccess() {
                    GalleryApplication.isSecureFolderUnlocked = true;
                    loadContent();
                }

                @Override
                public void onAuthError(String errString) {
                    // Fallback to PIN entry on recoverable errors or user cancellation
                    pinEntryLauncher.launch(new Intent(getContext(), PinEntryActivity.class));
                }

                @Override
                public void onAuthFailed() {
                    // On final failure, exit the secure folder
                    Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                    if (isAdded()) {
                        navController.popBackStack();
                    }
                }

                @Override
                public void onNoSecurityEnrolled() {
                    // If no biometrics, go straight to PIN
                    pinEntryLauncher.launch(new Intent(getContext(), PinEntryActivity.class));
                }
            }).authenticate();
        } else {
            pinEntryLauncher.launch(new Intent(getContext(), PinEntryActivity.class));
        }
    }

    private void loadContent() {
        if (binding != null) {
            binding.authCoverView.setVisibility(View.GONE);
        }
        secureFolderViewModel.loadAlbumsFromSecureFolder();
    }

    private void hideContent() {
        if (binding != null) {
            albumsAdapter.setAlbums(Collections.emptyList());
            binding.authCoverView.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        albumsAdapter = new AlbumsAdapter(null, new AlbumsAdapter.OnAlbumClickListener() {
            @Override
            public void onAlbumClick(Album album) {
                if (actionMode != null) {
                    toggleSelection(album);
                } else {
                    openAlbum(album);
                }
            }

            @Override
            public void onAlbumLongClick(Album album) {
                toggleSelection(album);
            }
        });
        SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        int savedSpanCount = prefs.getInt(PREF_SECURE_ALBUM_COLUMNS, 3);
        gridLayoutManager = new GridLayoutManager(getContext(), savedSpanCount);
        binding.recyclerViewSecureAlbums.setLayoutManager(gridLayoutManager);
        albumsAdapter.setSpanCount(savedSpanCount);
        binding.recyclerViewSecureAlbums.setAdapter(albumsAdapter);
    }

    private void setupPinchToZoom() {
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                if (detector.getScaleFactor() > 1.0f) {
                    // Pinching out
                    if (gridLayoutManager.getSpanCount() > 2) {
                        TransitionManager.beginDelayedTransition(binding.recyclerViewSecureAlbums, new AutoTransition().setDuration(250));
                        gridLayoutManager.setSpanCount(2);
                        editor.putInt(PREF_SECURE_ALBUM_COLUMNS, 2);
                        editor.apply();
                        albumsAdapter.setSpanCount(2);
                    }
                } else {
                    // Pinching in
                    if (gridLayoutManager.getSpanCount() < 3) {
                        TransitionManager.beginDelayedTransition(binding.recyclerViewSecureAlbums, new AutoTransition().setDuration(250));
                        gridLayoutManager.setSpanCount(3);
                        editor.putInt(PREF_SECURE_ALBUM_COLUMNS, 3);
                        editor.apply();
                        albumsAdapter.setSpanCount(3);
                    }
                }
                return true;
            }
        });

        binding.recyclerViewSecureAlbums.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return false;
        });
    }

    private void toggleSelection(Album album) {
        albumsAdapter.toggleSelection(album);
        int selectionCount = albumsAdapter.getSelectedAlbums().size();
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

    private void openAlbum(Album album) {
        Bundle bundle = new Bundle();
        bundle.putString("albumFolderPath", album.getFolderPath());
        navController.navigate(R.id.action_secureFolderFragment_to_secureAlbumDetailFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.secure_album_context_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        menu.findItem(R.id.action_change_cover).setVisible(albumsAdapter.getSelectedAlbums().size() == 1);
        return true;
    }

    @Override
    public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode, MenuItem item) {
        List<Album> selectedAlbums = albumsAdapter.getSelectedAlbums();
        if (selectedAlbums.isEmpty()) return false;

        int itemId = item.getItemId();
        if (itemId == R.id.action_delete_album) {
            showDeleteConfirmation(selectedAlbums);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_move_out_of_secure) {
            moveAlbumsToPublic(selectedAlbums);
            mode.finish();
            return true;
        } else if (itemId == R.id.action_change_cover) {
            Intent intent = new Intent(getContext(), ChangeCoverActivity.class);
            intent.putExtra(ChangeCoverActivity.EXTRA_ALBUM_PATH, selectedAlbums.get(0).getFolderPath());
            changeCoverLauncher.launch(intent);
            mode.finish();
            return true;
        }
        return false;
    }

    private void moveAlbumsToPublic(List<Album> albumsToMove) {
        executor.execute(() -> {
            File publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (!publicPicturesDir.exists()) publicPicturesDir.mkdirs();

            for (Album album : albumsToMove) {
                File sourceDir = new File(album.getFolderPath());
                File destDir = new File(publicPicturesDir, album.getName());
                if (!destDir.exists()) destDir.mkdirs();

                File[] filesToMove = sourceDir.listFiles();
                if (filesToMove != null) {
                    for (File file : filesToMove) {
                        File newFile = new File(destDir, file.getName());
                        file.renameTo(newFile);
                        AlbumsViewModel.scanFile(getContext(), Uri.fromFile(newFile));
                    }
                }
                deleteDirectory(sourceDir);
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Moved " + albumsToMove.size() + " album(s) out of secure folder", Toast.LENGTH_SHORT).show();
                    secureFolderViewModel.loadAlbumsFromSecureFolder();
                });
            }
        });
    }

    private void showDeleteConfirmation(List<Album> albumsToDelete) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + albumsToDelete.size() + " Secure Album(s)?")
                .setMessage("This will permanently delete the album and all photos inside. This action cannot be undone.")
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    deleteAlbums(albumsToDelete);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAlbums(List<Album> albumsToDelete) {
        executor.execute(() -> {
            for (Album album : albumsToDelete) {
                deleteDirectory(new File(album.getFolderPath()));
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Deleted " + albumsToDelete.size() + " album(s)", Toast.LENGTH_SHORT).show();
                    secureFolderViewModel.loadAlbumsFromSecureFolder();
                });
            }
        });
    }

    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return path.delete();
    }

    @Override
    public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
        actionMode = null;
        albumsAdapter.clearSelection();
    }

    @Override
    public void onResume() {
        super.onResume();
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded()) {
                if (GalleryApplication.isSecureFolderUnlocked) {
                    loadContent();
                } else {
                    hideContent();
                    authenticate();
                }
            }
        });
    }
}