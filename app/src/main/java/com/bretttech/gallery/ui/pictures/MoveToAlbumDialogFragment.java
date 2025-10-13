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

        TextView title = view.findViewById(R.id.dialog_title);
        RecyclerView recyclerView = view.findViewById(R.id.albums_recycler_view);
        EditText newAlbumEditText = view.findViewById(R.id.new_album_name);
        Button createAlbumButton = view.findViewById(R.id.create_album_button);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        if (isSecureMove) {
            title.setText("Move to Secure Album");
            newAlbumEditText.setVisibility(View.VISIBLE);
            createAlbumButton.setVisibility(View.VISIBLE);

            File secureRoot = new File(requireContext().getFilesDir(), "secure");
            List<Album> secureAlbums = getAlbumsFromDirectory(secureRoot);

            if (!secureAlbums.isEmpty()) {
                recyclerView.setVisibility(View.VISIBLE);
                AlbumsAdapter adapter = new AlbumsAdapter(secureAlbums, this::onAlbumSelected);
                recyclerView.setAdapter(adapter);
            }

            createAlbumButton.setOnClickListener(v -> {
                String albumName = newAlbumEditText.getText().toString().trim();
                if (albumName.isEmpty()) {
                    albumName = "Moved"; // Default album name
                }
                File newAlbumDir = new File(secureRoot, albumName);
                if (!newAlbumDir.exists()) {
                    newAlbumDir.mkdirs();
                }
                moveUrisTo(newAlbumDir.getAbsolutePath());
                dismiss();
            });

        } else {
            // Moving out of secure or between public albums
            title.setText("Select Album");
            recyclerView.setVisibility(View.VISIBLE);
            AlbumsViewModel albumsViewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);
            albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> {
                AlbumsAdapter adapter = new AlbumsAdapter(albums, this::onAlbumSelected);
                recyclerView.setAdapter(adapter);
            });
            albumsViewModel.loadAlbums();
        }
    }

    private List<Album> getAlbumsFromDirectory(File rootDir) {
        List<Album> albums = new ArrayList<>();
        if (rootDir.exists() && rootDir.isDirectory()) {
            File[] albumDirs = rootDir.listFiles(File::isDirectory);
            if (albumDirs != null) {
                for (File dir : albumDirs) {
                    File[] files = dir.listFiles();
                    int count = files != null ? files.length : 0;
                    if (count > 0) {
                        Uri coverUri = Uri.fromFile(files[0]);
                        albums.add(new Album(dir.getName(), coverUri, count, dir.getAbsolutePath(), Image.MEDIA_TYPE_IMAGE, dir.lastModified()));
                    }
                }
            }
        }
        return albums;
    }


    private void onAlbumSelected(Album album) {
        moveUrisTo(album.getFolderPath());
        dismiss();
    }

    private void moveUrisTo(String destinationPath) {
        if (urisToMove == null || urisToMove.isEmpty()) {
            if (isAdded()) {
                Toast.makeText(getContext(), "No items to move.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        new Thread(() -> {
            boolean allFilesMoved = true;
            for (Uri uri : urisToMove) {
                boolean success;
                String scheme = uri.getScheme();
                if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                    success = moveFromMediaStoreToPath(uri, destinationPath);
                } else {
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
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String relativePath = getRelativePathFromAbsolute(destinationAlbumPath);
            if (relativePath == null) return null;
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
                resolver.delete(uri, null, null);
                return null;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            }
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
        return new File(absolutePath).getName();
    }

    private String getFileName(Uri uri) {
        if (getContext() == null) return null;
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
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