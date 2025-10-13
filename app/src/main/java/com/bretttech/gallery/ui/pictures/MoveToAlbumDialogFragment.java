package com.bretttech.gallery.ui.pictures;

import android.content.ContentResolver;
import android.content.ContentValues;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MoveToAlbumDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "MoveToAlbumDialog";
    public static final String REQUEST_KEY = "move_complete_request";
    public static final String KEY_MOVE_SUCCESS = "move_success";
    public static final String KEY_MOVED_URIS = "moved_uris";

    private static final String ARG_URIS = "uris_to_move";
    private static final String ARG_IS_SECURE_MOVE = "is_secure_move";

    private AlbumsViewModel albumsViewModel;
    private List<Uri> urisToMove;
    private boolean isSecureMove = false;

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
            isSecureMove = getArguments().getBoolean(ARG_IS_SECURE_MOVE);
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

        if (isSecureMove) {
            moveToSecureFolder();
            return;
        }

        RecyclerView recyclerView = view.findViewById(R.id.albums_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        AlbumsAdapter albumsAdapter = new AlbumsAdapter(new ArrayList<>(), this::onAlbumSelected);
        recyclerView.setAdapter(albumsAdapter);

        albumsViewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);
        albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), albumsAdapter::setAlbums);
        albumsViewModel.loadAlbums();
    }

    private void onAlbumSelected(Album album) {
        moveUrisTo(album.getFolderPath());
        dismiss();
    }

    private void moveToSecureFolder() {
        File secureDir = new File(requireContext().getFilesDir(), "secure");
        if (!secureDir.exists()) {
            secureDir.mkdirs();
        }
        moveUrisTo(secureDir.getAbsolutePath());
        dismiss();
    }

    private void moveUrisTo(String destinationPath) {
        if (urisToMove == null || urisToMove.isEmpty()) {
            Toast.makeText(getContext(), "No items to move.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            boolean allFilesMoved = true;
            for (Uri uri : urisToMove) {
                boolean success = false;
                String scheme = uri.getScheme();
                if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                    // Moving FROM MediaStore (public) TO a file path (private/secure)
                    success = moveFromMediaStoreToPath(uri, destinationPath);
                } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                    // Moving FROM a file path (private/secure) TO MediaStore (public)
                    success = moveFromFileToMediaStore(new File(uri.getPath()), destinationPath) != null;
                }

                if (!success) {
                    allFilesMoved = false;
                    break;
                }
            }

            boolean finalAllFilesMoved = allFilesMoved;
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), finalAllFilesMoved ? "Move successful" : "Move failed", Toast.LENGTH_SHORT).show();
                    Bundle result = new Bundle();
                    result.putBoolean(KEY_MOVE_SUCCESS, finalAllFilesMoved);
                    result.putParcelableArrayList(KEY_MOVED_URIS, new ArrayList<>(urisToMove));
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                });
            }
        }).start();
    }

    private boolean moveFromMediaStoreToPath(Uri sourceUri, String destinationPath) {
        Context context = getContext();
        if (context == null) return false;

        String fileName = getFileName(sourceUri);
        if (fileName == null) return false;

        File destinationFile = new File(destinationPath, fileName);

        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(destinationFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            // Now that copy is successful, delete the original
            context.getContentResolver().delete(sourceUri, null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Uri moveFromFileToMediaStore(File sourceFile, String destinationAlbumPath) {
        Context context = getContext();
        if (context == null) return null;

        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, sourceFile.getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); // This can be improved to detect MIME type

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Correctly form the relative path
            String relativePath = getRelativePathFromAbsolute(destinationAlbumPath);
            if (relativePath == null) return null; // Could not determine relative path
            values.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        } else {
            File destDir = new File(destinationAlbumPath);
            if (!destDir.exists() && !destDir.mkdirs()) {
                return null;
            }
            values.put(MediaStore.Images.Media.DATA, new File(destDir, sourceFile.getName()).getAbsolutePath());
        }

        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = resolver.openOutputStream(uri);
                 InputStream in = new FileInputStream(sourceFile)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (IOException e) {
                resolver.delete(uri, null, null); // Clean up failed insert
                return null;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            }
            // Now that copy is successful, delete the original
            sourceFile.delete();
            return uri;
        }
        return null;
    }

    private String getRelativePathFromAbsolute(String absolutePath) {
        String[] storageRoots = {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getParentFile().getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getParentFile().getAbsolutePath(),
                Environment.getExternalStorageDirectory().getAbsolutePath()
        };

        for (String root : storageRoots) {
            if (absolutePath.startsWith(root)) {
                String relativePath = absolutePath.substring(root.length());
                if (relativePath.startsWith(File.separator)) {
                    relativePath = relativePath.substring(1);
                }
                return relativePath;
            }
        }
        return null; // Fallback
    }


    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}