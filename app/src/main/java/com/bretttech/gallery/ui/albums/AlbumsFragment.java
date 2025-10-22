package com.bretttech.gallery.ui.albums;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import com.bretttech.gallery.R;
import com.bretttech.gallery.SharedViewModel;
import com.bretttech.gallery.SettingsActivity;
import com.bretttech.gallery.ui.pictures.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsFragment extends Fragment implements androidx.appcompat.view.ActionMode.Callback {

    private AlbumsViewModel albumsViewModel;
    private AlbumsAdapter albumsAdapter;
    private androidx.appcompat.view.ActionMode actionMode;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedViewModel sharedViewModel;
    private List<File> mFoldersToDelete;
    private ScaleGestureDetector scaleGestureDetector;
    private GridLayoutManager gridLayoutManager;
    private RecyclerView recyclerView;
    private static final String PREF_ALBUM_COLUMNS = "album_columns";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showCreateAlbumDialog();
                } else {
                    Toast.makeText(getContext(), "Permission denied to write to your External storage", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> changeCoverLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                    Uri newCoverUri = result.getData().getData();
                    int mediaType = result.getData().getIntExtra(ChangeCoverActivity.RESULT_COVER_MEDIA_TYPE, Image.MEDIA_TYPE_IMAGE);
                    if (newCoverUri != null && !albumsAdapter.getSelectedAlbums().isEmpty()) {
                        String albumPath = albumsAdapter.getSelectedAlbums().get(0).getFolderPath();
                        albumsViewModel.setAlbumCover(albumPath, newCoverUri, mediaType);
                        Toast.makeText(getContext(), "Cover photo changed!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<IntentSenderRequest> trashResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(getContext(), "Album contents moved to trash.", Toast.LENGTH_SHORT).show();
                    if (mFoldersToDelete != null && !mFoldersToDelete.isEmpty()) {
                        deleteFolders(new ArrayList<>(mFoldersToDelete));
                        mFoldersToDelete.clear();
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to move album contents to trash.", Toast.LENGTH_SHORT).show();
                    if (mFoldersToDelete != null) {
                        mFoldersToDelete.clear();
                    }
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_albums, container, false);

        recyclerView = root.findViewById(R.id.recycler_view_albums);
        SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        int savedSpanCount = prefs.getInt(PREF_ALBUM_COLUMNS, 3);
        gridLayoutManager = new GridLayoutManager(getContext(), savedSpanCount);
        recyclerView.setLayoutManager(gridLayoutManager);

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
        albumsAdapter.setSpanCount(savedSpanCount);
        recyclerView.setAdapter(albumsAdapter);

        albumsViewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);
        albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> albumsAdapter.setAlbums(albums));

        sharedViewModel.getRefreshRequest().observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                if (actionMode == null) {
                    albumsViewModel.loadAlbums();
                }
            }
        });

        setupPinchToZoom();

        return root;
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
                        TransitionManager.beginDelayedTransition(recyclerView, new AutoTransition().setDuration(250));
                        gridLayoutManager.setSpanCount(2);
                        editor.putInt(PREF_ALBUM_COLUMNS, 2);
                        editor.apply();
                        albumsAdapter.setSpanCount(2);
                    }
                } else {
                    // Pinching in
                    if (gridLayoutManager.getSpanCount() < 3) {
                        TransitionManager.beginDelayedTransition(recyclerView, new AutoTransition().setDuration(250));
                        gridLayoutManager.setSpanCount(3);
                        editor.putInt(PREF_ALBUM_COLUMNS, 3);
                        editor.apply();
                        albumsAdapter.setSpanCount(3);
                    }
                }
                return true;
            }
        });

        recyclerView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return false;
        });
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

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                albumsViewModel.searchAlbums(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                albumsViewModel.searchAlbums(newText);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_create_album) {
            requestStoragePermissionAndShowCreateAlbumDialog();
            return true;
        } else if (itemId == R.id.sort_date_desc) {
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
        if (itemId != R.id.action_create_album) {
            item.setChecked(true);
        }
        return true;
    }

    private void requestStoragePermissionAndShowCreateAlbumDialog() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                showCreateAlbumDialog();
            }
        } else {
            showCreateAlbumDialog();
        }
    }

    private void showCreateAlbumDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create New Album");
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_new_album, null);
        builder.setView(customLayout);
        builder.setPositiveButton("Create", (dialog, which) -> {
            EditText editText = customLayout.findViewById(R.id.new_album_name);
            String albumName = editText.getText().toString().trim();
            if (!albumName.isEmpty()) {
                createNewAlbum(albumName);
            } else {
                Toast.makeText(getContext(), "Album name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    private void createNewAlbum(String albumName) {
        File publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File newAlbumDir = new File(publicPicturesDir, albumName);
        if (!newAlbumDir.exists()) {
            if (newAlbumDir.mkdirs()) {
                Toast.makeText(getContext(), "Album '" + albumName + "' created", Toast.LENGTH_SHORT).show();
                albumsViewModel.addEmptyAlbum(albumName, newAlbumDir.getAbsolutePath());
            } else {
                Toast.makeText(getContext(), "Failed to create album", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Album already exists", Toast.LENGTH_SHORT).show();
            albumsViewModel.addEmptyAlbum(albumName, newAlbumDir.getAbsolutePath());
        }
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

        // UPDATED: Dynamically select and apply NavOptions based on animation preference
        NavOptions navOptions = null;
        String animationType = SettingsActivity.getAnimationType(requireContext());

        if (animationType.equals(SettingsActivity.ANIMATION_SLIDE)) {
            navOptions = new NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build();
        } else if (animationType.equals(SettingsActivity.ANIMATION_FLY)) {
            navOptions = new NavOptions.Builder()
                    .setEnterAnim(R.anim.fly_in_up)
                    .setExitAnim(R.anim.fly_out_down)
                    .setPopEnterAnim(R.anim.fly_in_down)
                    .setPopExitAnim(R.anim.fly_out_up)
                    .build();
        } else if (animationType.equals(SettingsActivity.ANIMATION_FADE)) {
            // Pixel In/Out (Fade/Crossfade)
            navOptions = new NavOptions.Builder()
                    .setEnterAnim(R.anim.fade_in)
                    .setExitAnim(R.anim.fade_out)
                    .setPopEnterAnim(R.anim.fade_in)
                    .setPopExitAnim(R.anim.fade_out)
                    .build();
        }

        // If animationType is ANIMATION_OFF, navOptions remains null and default transition is used.
        navController.navigate(R.id.action_navigation_albums_to_albumDetailFragment, bundle, navOptions);
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
        List<Album> selectedAlbums = new ArrayList<>(albumsAdapter.getSelectedAlbums());
        if (selectedAlbums.isEmpty()) return false;

        int itemId = item.getItemId();
        if (itemId == R.id.action_delete_album) {
            showDeleteConfirmation(selectedAlbums, mode);
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

    private void showDeleteConfirmation(final List<Album> albumsToDelete, final androidx.appcompat.view.ActionMode mode) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + albumsToDelete.size() + " Album(s)?")
                // .setMessage("This will move all photos inside to the trash and permanently delete the album folder. This action cannot be undone.")
                .setMessage("This will delete all photos inside and permanently delete the album folder. This action cannot be undone.")

                .setPositiveButton("Delete", (dialog, which) -> {
                    startAlbumDeletionProcess(albumsToDelete);
                    if (mode != null) {
                        mode.finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void moveAlbumsToSecure(List<Album> albumsToMove) {
        // Your existing logic here
    }

    private void startAlbumDeletionProcess(List<Album> albumsToDelete) {
        executor.execute(() -> {
            final Context context = getContext();
            if (context == null) {
                Log.w("AlbumsFragment", "Context was null, aborting deletion process.");
                return;
            }

            mFoldersToDelete = new ArrayList<>();
            List<Uri> urisToTrash = new ArrayList<>();

            for (Album album : albumsToDelete) {
                File albumDir = new File(album.getFolderPath());
                mFoldersToDelete.add(albumDir);
                albumsViewModel.removeCustomCover(album.getFolderPath());
                urisToTrash.addAll(getMediaUrisForAlbum(context, album.getFolderPath()));
            }

            if (urisToTrash.isEmpty()) {
                Log.d("AlbumsFragment", "No media files found to trash, deleting empty folders.");
                deleteFolders(mFoldersToDelete);
                return;
            }

            requestTrash(context, urisToTrash);
        });
    }

    private List<Uri> getMediaUrisForAlbum(Context context, String folderPath) {
        List<Uri> uris = new ArrayList<>();
        // Query for images
        uris.addAll(queryMedia(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, folderPath));
        // Query for videos
        uris.addAll(queryMedia(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, folderPath));
        return uris;
    }

    private List<Uri> queryMedia(Context context, Uri contentUri, String folderPath) {
        List<Uri> uris = new ArrayList<>();
        String[] projection = {MediaStore.MediaColumns._ID};
        String selection = MediaStore.MediaColumns.DATA + " LIKE ?";
        String[] selectionArgs = {folderPath + "/%"};

        try (Cursor cursor = context.getContentResolver().query(contentUri, projection, selection, selectionArgs, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                    uris.add(ContentUris.withAppendedId(contentUri, id));
                }
            }
        } catch (Exception e) {
            Log.e("AlbumsFragment", "Error querying for media in album: " + contentUri, e);
        }
        return uris;
    }


    private void requestTrash(Context context, List<Uri> urisToTrash) {
        ContentResolver resolver = context.getContentResolver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                PendingIntent trashIntent = MediaStore.createTrashRequest(resolver, urisToTrash, true);
                IntentSenderRequest request = new IntentSenderRequest.Builder(trashIntent.getIntentSender()).build();
                trashResultLauncher.launch(request);
            } catch (Exception e) {
                Log.e("AlbumsFragment", "Error creating trash request for Android R+", e);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(context, "Error creating trash request.", Toast.LENGTH_SHORT).show());
                }
            }
        } else {
            try {
                for (Uri uri : urisToTrash) {
                    resolver.delete(uri, null, null);
                }
                deleteFolders(mFoldersToDelete);
            } catch (SecurityException e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e instanceof RecoverableSecurityException) {
                    RecoverableSecurityException rse = (RecoverableSecurityException) e;
                    IntentSenderRequest request = new IntentSenderRequest.Builder(rse.getUserAction().getActionIntent().getIntentSender()).build();
                    trashResultLauncher.launch(request);
                } else {
                    Log.e("AlbumsFragment", "SecurityException on pre-R device.", e);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> Toast.makeText(context, "Permission denied.", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        }
    }

    private void deleteFolders(List<File> folders) {
        executor.execute(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

            for (File folder : folders) {
                if (folder.exists()) {
                    deleteDirectory(folder);
                }
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> albumsViewModel.loadAlbums());
            }
        });
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            }
            file.delete();
        }
        dir.delete();
    }


    @Override
    public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
        actionMode = null;
        albumsAdapter.clearSelection();
    }
}