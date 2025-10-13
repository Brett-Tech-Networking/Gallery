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
import android.webkit.MimeTypeMap;
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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static MoveToAlbumDialogFragment newInstance(List<Uri> uris, boolean isSecureMove) {
        MoveToAlbumDialogFragment fragment = new MoveToAlbumDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_URIS, new ArrayList<>(uris));
        args.putBoolean(ARG_IS_SECURE_MOVE, isSecureMove);
        fragment.setArguments(args);
        return fragment;
    }

    // other methods...
}