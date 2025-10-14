package com.bretttech.gallery.ui.secure;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import com.bretttech.gallery.R;
import com.bretttech.gallery.auth.BiometricAuthManager;
import com.bretttech.gallery.databinding.FragmentSecureFolderBinding;
import com.bretttech.gallery.ui.albums.Album;
import com.bretttech.gallery.ui.albums.AlbumsAdapter;
import com.bretttech.gallery.ui.albums.AlbumsViewModel;
import com.bretttech.gallery.ui.albums.ChangeCoverActivity;

import java.io.File;
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

        secureFolderViewModel = new ViewModelProvider(this).get(SecureFolderViewModel.class);
        secureFolderViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> {
            albumsAdapter.setAlbums(albums);
        });

        // Enable options menu for the toolbar
        setHasOptionsMenu(true);

        authenticate();

        return binding.getRoot();
    }

    // NEW: Add onCreateOptionsMenu to inflate the settings icon
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflate the new settings menu
        inflater.inflate(R.menu.secure_folder_settings_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // NEW: Handle the settings icon click
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
        SharedPreferences prefs = requireActivity().getSharedPreferences("secure_folder_prefs", Context.MODE_PRIVATE);

        // Check if biometrics is disabled by the user in settings
        boolean biometricsEnabled = prefs.getBoolean("biometrics_enabled", true); // Default to true if not set

        // If PIN is already set, or biometrics is disabled, go directly to PIN entry
        if (prefs.contains("pin_hash") || !biometricsEnabled) {
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
                // If biometric fails/is canceled, fall back to PIN entry
                pinEntryLauncher.launch(new Intent(getContext(), PinEntryActivity.class));
            }

            @Override
            public void onAuthFailed() {
                // Biometric failed (e.g., wrong fingerprint), nothing to do but prompt the user to try again/use PIN
                Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoSecurityEnrolled() {
                // No biometrics available, force PIN setup
                pinSetupLauncher.launch(new Intent(getContext(), PinSetupActivity.class));
            }
        }).authenticate();
    }

    private void loadContent() {
        secureFolderViewModel.loadAlbumsFromSecureFolder();
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
        binding.recyclerViewSecureAlbums.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recyclerViewSecureAlbums.setAdapter(albumsAdapter);
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
}