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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Album>> albums = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public AlbumsViewModel(@NonNull Application application) {
        super(application);
        loadAlbums();
    }

    public LiveData<List<Album>> getAlbums() {
        return albums;
    }

    private void loadAlbums() {
        executorService.execute(() -> {
            Map<String, Album> albumMap = new LinkedHashMap<>();

            String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            };
            String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

            try (Cursor cursor = getApplication().getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
            )) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        String bucketName = cursor.getString(bucketNameColumn);
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                        // If album already exists, increment count. Otherwise, create new album.
                        if (albumMap.containsKey(bucketName)) {
                            Album existingAlbum = albumMap.get(bucketName);
                            if (existingAlbum != null) {
                                albumMap.put(bucketName, new Album(
                                        existingAlbum.getName(),
                                        existingAlbum.getCoverImageUri(), // Keep the first (most recent) image as cover
                                        existingAlbum.getImageCount() + 1
                                ));
                            }
                        } else {
                            albumMap.put(bucketName, new Album(bucketName, contentUri, 1));
                        }
                    }
                }
            }
            albums.postValue(new ArrayList<>(albumMap.values()));
        });
    }
}
