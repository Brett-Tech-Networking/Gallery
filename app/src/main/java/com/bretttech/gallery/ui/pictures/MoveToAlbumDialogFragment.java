package com.bretttech.gallery.ui.pictures;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;
import com.bretttech.gallery.ui.albums.Album;
import com.bretttech.gallery.ui.albums.AlbumsAdapter;
import com.bretttech.gallery.ui.albums.AlbumsViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoveToAlbumDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "MoveToAlbumDialog";
    public static final String REQUEST_KEY = "move_complete_request";
    public static final String KEY_MOVE_SUCCESS = "move_success";
    public static final String KEY_MOVED_URIS = "moved_uris";

    private static final String ARG_URIS = "uris_to_move";
    private static final String ARG_IS_SECURE_MOVE = "is_secure_move";
    private static final String ARG_IS_MOVING_OUT_OF_SECURE = "is_moving_out_of_secure";

    private List<Uri> urisToMove;
    private boolean isSecureMove = false;
    private boolean isMovingOutOfSecure = false;

    private RecyclerView albumsRecyclerView;
    private EditText newAlbumNameEditText;
    private Button createAlbumButton;
    private TextView dialogTitle;

    private AlbumsViewModel albumsViewModel;
    private AlbumsAdapter albumsAdapter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static MoveToAlbumDialogFragment newInstance(List<Uri> uris, boolean isSecureMove) {
        MoveToAlbumDialogFragment fragment = new MoveToAlbumDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_URIS, new ArrayList<>(uris));
        args.putBoolean(ARG_IS_SECURE_MOVE, isSecureMove);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            urisToMove = getArguments().getParcelableArrayList(ARG_URIS);
            isSecureMove = getArguments().getBoolean(ARG_IS_SECURE_MOVE, false);
            // Check if we are moving out of the secure folder (from secure album detail)
            if (getParentFragment() instanceof com.bretttech.gallery.ui.secure.SecureAlbumDetailFragment) {
                isMovingOutOfSecure = true;
            }
        }
        if (urisToMove == null) {
            urisToMove = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_move_to_album, container, false);

        albumsRecyclerView = view.findViewById(R.id.albums_recycler_view);
        newAlbumNameEditText = view.findViewById(R.id.new_album_name);
        createAlbumButton = view.findViewById(R.id.create_album_button);
        dialogTitle = view.findViewById(R.id.dialog_title);

        setupUI();
        setupRecyclerView();

        albumsViewModel = new ViewModelProvider(requireActivity()).get(AlbumsViewModel.class);
        albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> {
            List<Album> filteredAlbums = new ArrayList<>();
            for (Album album : albums) {
                // Filter albums based on whether we are moving to a secure or public location
                if (isSecureMove) {
                    // Only show albums in the app's secure directory
                    if (album.getFolderPath().contains(requireContext().getFilesDir().getAbsolutePath() + "/secure")) {
                        filteredAlbums.add(album);
                    }
                } else {
                    // Only show public albums (and implicitly filter secure folders via ViewModel logic)
                    if (!album.getFolderPath().contains(requireContext().getFilesDir().getAbsolutePath() + "/secure")) {
                        filteredAlbums.add(album);
                    }
                }
            }
            albumsAdapter.setAlbums(filteredAlbums);
            albumsRecyclerView.setVisibility(View.VISIBLE);
        });

        if (!isSecureMove) {
            albumsViewModel.loadAlbums();
        }


        return view;
    }

    private void setupUI() {
        if (isSecureMove) {
            dialogTitle.setText("Move to Secure Album");
        } else if (isMovingOutOfSecure) {
            dialogTitle.setText("Move to Public Album");
        } else {
            dialogTitle.setText("Move to Album");
        }

        newAlbumNameEditText.setVisibility(View.VISIBLE);
        // MODIFIED: Updated hint to mention 'Moved' folder
        newAlbumNameEditText.setHint("Create new " + (isSecureMove ? "secure " : "public ") + "album... (Leave blank for 'Moved')");
        createAlbumButton.setVisibility(View.VISIBLE);
        createAlbumButton.setText("Move to New Album");

        createAlbumButton.setOnClickListener(v -> {
            String newAlbumName = newAlbumNameEditText.getText().toString().trim();

            if (newAlbumName.isEmpty()) {
                // NEW LOGIC: If input is empty and it's a secure move, use the default "Moved" folder
                if (isSecureMove) {
                    startMoveOperation("Moved");
                } else {
                    // If it's a public move and no name is provided, prompt the user
                    Toast.makeText(getContext(), "Please enter a name for the new public album", Toast.LENGTH_SHORT).show();
                }
            } else {
                startMoveOperation(newAlbumName);
            }
        });
    }

    private void setupRecyclerView() {
        albumsAdapter = new AlbumsAdapter(null, new AlbumsAdapter.OnAlbumClickListener() {
            @Override
            public void onAlbumClick(Album album) {
                startMoveOperation(album.getName());
            }

            @Override
            public void onAlbumLongClick(Album album) {
                // Not used
            }
        });
        albumsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        albumsRecyclerView.setAdapter(albumsAdapter);
    }

    private void startMoveOperation(String albumName) {
        Toast.makeText(getContext(), "Moving " + urisToMove.size() + " items to " + albumName + "...", Toast.LENGTH_LONG).show();
        executor.execute(() -> {
            boolean success = moveMedia(albumName);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(getContext(), "Move completed successfully.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Move failed.", Toast.LENGTH_SHORT).show();
                    }
                    notifyCallingFragment(success);
                    dismiss();
                });
            }
        });
    }

    private boolean moveMedia(String albumName) {
        boolean allSuccess = true;

        // Determine destination folder path
        File destinationDir;
        if (isSecureMove) {
            File secureRoot = new File(requireContext().getFilesDir(), "secure");
            destinationDir = new File(secureRoot, albumName);
        } else {
            File publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            destinationDir = new File(publicPicturesDir, albumName);
        }

        if (!destinationDir.exists()) destinationDir.mkdirs(); // This correctly creates the folder only if it doesn't exist
        if (!destinationDir.exists()) return false; // Failed to create directory

        List<Uri> urisToDelete = new ArrayList<>();

        for (Uri sourceUri : urisToMove) {
            try {
                // 1. Get the original filename (since content URI won't hold it directly)
                String displayName = getFileNameFromUri(sourceUri);
                if (displayName == null) {
                    allSuccess = false;
                    continue;
                }

                // 2. Create the destination file path
                File destinationFile = new File(destinationDir, displayName);

                // 3. Move/Copy operation
                if (isMovingOutOfSecure) {
                    // Move from private to public (File rename operation is safe here)
                    File sourceFile = new File(sourceUri.getPath());
                    if (sourceFile.renameTo(destinationFile)) {
                        // Scan the new public file
                        AlbumsViewModel.scanFile(requireContext(), Uri.fromFile(destinationFile));
                        urisToDelete.add(sourceUri); // Mark private URI for removal if needed
                    } else {
                        allSuccess = false;
                    }
                } else {
                    // Move from public to secure (Need to copy bytes and use MediaStore delete/trash)
                    copyStream(requireContext().getContentResolver().openInputStream(sourceUri), new FileOutputStream(destinationFile));
                    urisToDelete.add(sourceUri); // Mark public URI for deletion/trash
                }

            } catch (Exception e) {
                e.printStackTrace();
                allSuccess = false;
            }
        }

        // 4. Clean up original files (only for public -> private move)
        if (!isMovingOutOfSecure && allSuccess) {
            try {
                ContentResolver resolver = requireContext().getContentResolver();

                for (Uri uri : urisToDelete) {
                    resolver.delete(uri, null, null);
                }

            } catch (Exception e) {
                e.printStackTrace();
                // Do not mark as failure, as the move to secure folder succeeded
            }
        }

        // We only return true if the move operation (copying/renaming) itself succeeded
        return allSuccess;
    }

    private String getFileNameFromUri(Uri uri) {
        String displayName = null;
        if (uri.getScheme().equals("content")) {
            String[] projection = { MediaStore.MediaColumns.DISPLAY_NAME };
            try (Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                // Fallback to path name is handled below
            }
        }

        if (displayName == null && uri.getPath() != null) {
            displayName = new File(uri.getPath()).getName();
        }
        return displayName;
    }

    private long copyStream(InputStream input, OutputStream output) throws IOException {
        long bytesCopied = 0;
        byte[] buffer = new byte[8 * 1024];
        int bytes = input.read(buffer);
        while (bytes >= 0) {
            output.write(buffer, 0, bytes);
            bytesCopied += bytes;
            bytes = input.read(buffer);
        }
        input.close();
        output.close();
        return bytesCopied;
    }

    private void notifyCallingFragment(boolean success) {
        Bundle result = new Bundle();
        result.putBoolean(KEY_MOVE_SUCCESS, success);
        // Pass the URIs back so the calling fragment can remove them from its list/adapter
        result.putParcelableArrayList(KEY_MOVED_URIS, new ArrayList<>(urisToMove));
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Ensure the ViewModel is loaded to refresh the list in the parent fragment (PicturesFragment/AlbumDetailFragment)
        if (!isMovingOutOfSecure) {
            // Need to reload public albums/pictures after deletion/move
            if (albumsViewModel != null) {
                albumsViewModel.loadAlbums();
            }
        }
    }
}