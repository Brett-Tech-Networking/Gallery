package com.bretttech.gallery.ui.trash;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bretttech.gallery.ui.pictures.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrashViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> trashedImages = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public TrashViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Image>> getTrashedImages() {
        return trashedImages;
    }

    public void loadTrashedImages() {
        executorService.execute(() -> {
            List<Image> trashedImageList = new ArrayList<>();
            // The system trash feature was added in Android 10 (API 29).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver contentResolver = getApplication().getContentResolver();
                Cursor cursor = null;
                try {
                    Uri queryUri = MediaStore.Files.getContentUri("external");
                    String[] projection = {MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE};

                    Bundle queryArgs = new Bundle();

                    // This selection will find items that are in the trash.
                    // It is the key to fixing the "false positives" issue.
                    String selection = MediaStore.Files.FileColumns.IS_TRASHED + " = ?";
                    String[] selectionArgs = {"1"};

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // For Android 11+, we use the modern query arguments.
                        // The literal string values are used here to prevent the compile-time error.
                        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection);
                        queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs);
                        cursor = contentResolver.query(queryUri, projection, queryArgs, null);
                    } else {
                        // For Android 10, we use the older selection method.
                        cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
                    }

                    if (cursor != null) {
                        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                        while (cursor.moveToNext()) {
                            long id = cursor.getLong(idColumn);
                            Uri contentUri = ContentUris.withAppendedId(queryUri, id);
                            trashedImageList.add(new Image(contentUri));
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            // For older versions, post an empty list as there is no system trash.
            trashedImages.postValue(trashedImageList);
        });
    }
}