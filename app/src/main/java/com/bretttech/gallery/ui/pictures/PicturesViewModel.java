package com.bretttech.gallery.ui.pictures;

import android.app.Application;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PicturesViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> images = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final String TAG = "PicturesViewModel";

    public PicturesViewModel(Application application) {
        super(application);
    }

    public LiveData<List<Image>> getImages() {
        return images;
    }

    public void loadImages() {
        executorService.execute(() -> {
            List<Image> imageList = new ArrayList<>();
            // UPDATED: Use MediaStore.Files.getContentUri("external") to query for all media types
            Uri queryUri = MediaStore.Files.getContentUri("external");

            // UPDATED: Include MEDIA_TYPE in projection
            String[] projection = {
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.DATE_TAKEN
            };

            // NEW: Selection to filter for only images and videos
            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (?, ?) AND " + MediaStore.Files.FileColumns.DATA + " NOT LIKE ?";
            String[] selectionArgs = new String[]{
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                    "%" + getApplication().getFilesDir().getAbsolutePath() + "/secure%"
            };

            String sortOrder = MediaStore.Files.FileColumns.DATE_TAKEN + " DESC";

            try (Cursor cursor = getApplication().getContentResolver().query(
                    queryUri, // UPDATED URI
                    projection, // UPDATED PROJECTION
                    selection,  // ADDED SELECTION
                    selectionArgs, // ADDED SELECTION ARGS
                    sortOrder
            )) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE); // NEW COLUMN INDEX

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        int mediaType = cursor.getInt(mediaTypeColumn); // GET MEDIA TYPE

                        // Use the correct collection URI to form the content URI based on media type
                        Uri contentUri;
                        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                            contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                        } else {
                            contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        }

                        // UPDATED: Pass mediaType to the Image constructor
                        imageList.add(new Image(contentUri, mediaType));
                    }
                }
            }
            images.postValue(imageList);
        });
    }
}