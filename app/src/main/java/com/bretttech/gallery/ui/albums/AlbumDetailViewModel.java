package com.bretttech.gallery.ui.albums;

import android.app.Application;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bretttech.gallery.ui.pictures.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> imagesLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public AlbumDetailViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Image>> getImages() {
        return imagesLiveData;
    }

    public void loadImagesFromAlbum(String folderPath) {
        executorService.execute(() -> {
            List<Image> images = new ArrayList<>();

            // UPDATED: Use MediaStore.Files.getContentUri("external") to query for all media types
            Uri queryUri = MediaStore.Files.getContentUri("external");

            // UPDATED: Include MEDIA_TYPE in projection
            String[] projection = {
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DATA
            };

            // MODIFIED Selection: Filter by folder path AND media type (image or video)
            String selection = MediaStore.Files.FileColumns.DATA + " LIKE ? AND " +
                    MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (?, ?)";
            String[] selectionArgs = new String[]{
                    folderPath + File.separator + "%",
                    String.valueOf(Image.MEDIA_TYPE_IMAGE),
                    String.valueOf(Image.MEDIA_TYPE_VIDEO)
            };

            String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

            try (Cursor cursor = getApplication().getContentResolver().query(
                    queryUri, // UPDATED URI
                    projection,
                    selection, // UPDATED SELECTION
                    selectionArgs, // UPDATED SELECTION ARGS
                    sortOrder
            )) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int mediaTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE); // NEW

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        int mediaType = cursor.getInt(mediaTypeCol); // GET MEDIA TYPE

                        Uri contentUri;
                        if (mediaType == Image.MEDIA_TYPE_VIDEO) {
                            contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                        } else {
                            contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        }

                        // UPDATED: Pass mediaType to the Image constructor
                        images.add(new Image(contentUri, mediaType));
                    }
                }
            }
            imagesLiveData.postValue(images);
        });
    }
}