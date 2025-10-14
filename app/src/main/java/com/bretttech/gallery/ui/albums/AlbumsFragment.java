package com.bretttech.gallery.ui.albums;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;
import com.bretttech.gallery.SharedViewModel; // NEW IMPORT
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.ui.pictures.MoveToAlbumDialogFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsFragment extends Fragment implements androidx.appcompat.view.ActionMode.Callback {

    private AlbumsViewModel albumsViewModel;
    private AlbumsAdapter albumsAdapter;
    private androidx.appcompat.view.ActionMode actionMode;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedViewModel sharedViewModel; // NEW FIELD


    private final ActivityResultLauncher<Intent> changeCoverLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                    Uri newCoverUri = result.getData().getData();
                    // MODIFIED: Retrieve media type from result Intent
                    int mediaType = result.getData().getIntExtra(ChangeCoverActivity.RESULT_COVER_MEDIA_TYPE, Image.MEDIA_TYPE_IMAGE);
                    if (newCoverUri != null && !albumsAdapter.getSelectedAlbums().isEmpty()) {
                        String albumPath = albumsAdapter.getSelectedAlbums().get(0).getFolderPath();
                        // MODIFIED: Pass the media type to the ViewModel
                        albumsViewModel.setAlbumCover(albumPath, newCoverUri, mediaType);
                        Toast.makeText(getContext(), "Cover photo changed!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class); // NEW INIT
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_albums, container, false);

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view_albums);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        albumsAdapter = new AlbumsAdapter(new ArrayList<>(), new AlbumsAdapter.OnAlbumClickListener() {
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
        recyclerView.setAdapter(albumsAdapter);

        albumsViewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);
        albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> albumsAdapter.setAlbums(albums));

        // NEW: Observe shared refresh event (Fixes non-live update)
        sharedViewModel.getRefreshRequest().observe(getViewLifecycleOwner(), event -> {
            // Only handle the event once
            if (event.getContentIfNotHandled() != null) {
                // If we are currently showing the album list and not in CAB mode, force refresh
                if (actionMode == null) {
                    albumsViewModel.loadAlbums();
                }
            }
        });


        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        albumsViewModel.loadAlbums();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.albums_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.sort_date_desc) {
            albumsViewModel.sortAlbums(AlbumsViewModel.SortOrder.DATE_DESC);
        } else if (itemId == R.id.sort_date_asc) {
            albumsViewModel.sortAlbums(AlbumsViewModel.SortOrder.DATE_ASC);
        } else if (itemId == R.id.sort_name_asc) {
            albumsViewModel.sortAlbums(AlbumsViewModel.SortOrder.NAME_ASC);
        } else if (itemId == R.id.sort_name_desc) {
            albumsViewModel.sortAlbums(AlbumsViewModel.SortOrder.NAME_DESC);
        } else if (itemId == R.id.sort_count_desc) {
            albumsViewModel.sortAlbums(AlbumsViewModel.SortOrder.COUNT_DESC);
        } else if (itemId == R.id.sort_count_asc) {
            albumsViewModel.sortAlbums(AlbumsViewModel.SortOrder.COUNT_ASC);
        } else {
            return super.onOptionsItemSelected(item);
        }
        item.setChecked(true);
        return true;
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
        NavController navController = NavHostFragment.findNavController(this);
        Bundle bundle = new Bundle();
        bundle.putString("albumFolderPath", album.getFolderPath());
        navController.navigate(R.id.action_navigation_albums_to_albumDetailFragment, bundle);
    }

    @Override
    public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.album_context_menu, menu);
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
        } else if (itemId == R.id.action_move_to_secure) {
            moveAlbumsToSecure(selectedAlbums);
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

    private void showDeleteConfirmation(List<Album> albumsToDelete) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + albumsToDelete.size() + " Album(s)?")
                .setMessage("What would you like to do with the photos inside?")
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    deleteAlbums(albumsToDelete, false);
                })
                .setNeutralButton("Move Photos First", (dialog, which) -> {
                    // Placeholder for move logic
                    Toast.makeText(getContext(), "Move functionality needs a destination picker.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void moveAlbumsToSecure(List<Album> albumsToMove) {
        executor.execute(() -> {
            File secureRoot = new File(requireContext().getFilesDir(), "secure");
            if (!secureRoot.exists()) secureRoot.mkdirs();

            for (Album album : albumsToMove) {
                File sourceDir = new File(album.getFolderPath());
                File destDir = new File(secureRoot, album.getName());
                destDir.mkdirs();

                File[] files = sourceDir.listFiles();
                if(files != null) {
                    for(File file : files) {
                        File newFile = new File(destDir, file.getName());
                        file.renameTo(newFile);
                        // Since these are now private files, we don't need to scan them
                    }
                }
                deleteDirectory(sourceDir);
                albumsViewModel.removeCustomCover(album.getFolderPath());
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Moved " + albumsToMove.size() + " album(s) to secure folder", Toast.LENGTH_SHORT).show();
                    albumsViewModel.loadAlbums();
                });
            }
        });
    }

    private void deleteAlbums(List<Album> albumsToDelete, boolean deleteOnlyEmptyFolders) {
        executor.execute(() -> {
            for (Album album : albumsToDelete) {
                File albumDir = new File(album.getFolderPath());
                if (deleteOnlyEmptyFolders && albumDir.listFiles() != null && albumDir.listFiles().length > 0) {
                    continue;
                }
                deleteDirectory(albumDir);
                albumsViewModel.removeCustomCover(album.getFolderPath());
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Deleted " + albumsToDelete.size() + " album(s)", Toast.LENGTH_SHORT).show();
                    albumsViewModel.loadAlbums();
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