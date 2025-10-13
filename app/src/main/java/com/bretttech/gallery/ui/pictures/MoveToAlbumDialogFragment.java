package com.bretttech.gallery.ui.pictures;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import com.bretttech.gallery.R;
import com.bretttech.gallery.ui.albums.Album;
import com.bretttech.gallery.ui.albums.AlbumsAdapter;
import com.bretttech.gallery.ui.albums.AlbumsViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MoveToAlbumDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "MoveToAlbumDialog";
    public static final String REQUEST_KEY = "move_complete_request"; // Key for communication
    public static final String KEY_MOVE_SUCCESS = "move_success";   // Key for the result data

    private static final String ARG_URIS = "uris_to_move";

    private AlbumsViewModel albumsViewModel;
    private List<Uri> urisToMove;

    private static class MediaMetadata {
        String mimeType;
        String data;
        long dateAdded;
        long dateModified;
        long dateTaken;
    }

    public static MoveToAlbumDialogFragment newInstance(List<Uri> uris) {
        MoveToAlbumDialogFragment fragment = new MoveToAlbumDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_URIS, new ArrayList<>(uris));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            urisToMove = getArguments().getParcelableArrayList(ARG_URIS);
        }
        if (urisToMove == null) {
            urisToMove = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_move_to_album, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.albums_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        AlbumsAdapter albumsAdapter = new AlbumsAdapter(null, this::onAlbumSelected);
        recyclerView.setAdapter(albumsAdapter);

        albumsViewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);
        albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), albumsAdapter::setAlbums);
        albumsViewModel.loadAlbums();
    }

    private void onAlbumSelected(Album album) {
        if (urisToMove.isEmpty()) {
            Toast.makeText(requireContext(), "No items selected to move.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        final Context context = requireContext().getApplicationContext();
        Toast.makeText(requireContext(), "Moving " + urisToMove.size() + " item(s) to " + album.getName(), Toast.LENGTH_SHORT).show();

        new Thread(() -> moveMediaItems(urisToMove, album.getFolderPath(), context)).start();

        dismiss();
    }

    private void moveMediaItems(List<Uri> uris, String destinationPath, Context appContext) {
        ContentResolver contentResolver = appContext.getContentResolver();
        int movedCount = 0;

        for (Uri uri : uris) {
            try {
                MediaMetadata metadata = queryMetadata(contentResolver, uri);
                if (metadata == null || metadata.data == null) {
                    Log.e(TAG, "Skipping move, failed to retrieve metadata for URI: " + uri);
                    continue;
                }

                File oldFile = new File(metadata.data);
                if (!oldFile.exists()) continue;

                String fileName = oldFile.getName();
                File newFolder = new File(destinationPath);
                if (!newFolder.exists()) {
                    newFolder.mkdirs();
                }
                File newFile = new File(newFolder, fileName);

                if (oldFile.renameTo(newFile)) {
                    contentResolver.delete(uri, null, null);

                    Uri collection = metadata.mimeType != null && metadata.mimeType.startsWith("video")
                            ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DATA, newFile.getAbsolutePath());
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, newFile.getName());
                    values.put(MediaStore.MediaColumns.MIME_TYPE, metadata.mimeType);
                    values.put(MediaStore.MediaColumns.SIZE, newFile.length());
                    values.put(MediaStore.MediaColumns.DATE_ADDED, metadata.dateAdded);
                    values.put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000);
                    values.put(MediaStore.MediaColumns.DATE_TAKEN, metadata.dateTaken);
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0);

                    contentResolver.insert(collection, values);
                    movedCount++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error moving media item: " + uri, e);
            }
        }

        if (movedCount > 0) {
            final int finalMovedCount = movedCount;
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    // Send a result back to the calling fragment
                    Bundle result = new Bundle();
                    result.putBoolean(KEY_MOVE_SUCCESS, true);
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);

                    Toast.makeText(requireContext(), finalMovedCount + " item(s) moved.", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private MediaMetadata queryMetadata(ContentResolver resolver, Uri uri) {
        // ... this method remains the same ...
        final String[] projection = {
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Images.Media.DATE_TAKEN
        };

        try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                MediaMetadata metadata = new MediaMetadata();

                metadata.data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                metadata.mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE));
                metadata.dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED));
                metadata.dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED));

                int dateTakenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                if (dateTakenCol != -1 && !cursor.isNull(dateTakenCol)) {
                    metadata.dateTaken = cursor.getLong(dateTakenCol);
                } else {
                    metadata.dateTaken = metadata.dateModified * 1000;
                }

                return metadata;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying metadata for URI: " + uri, e);
        }
        return null;
    }
}