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

import com.bretttech.gallery.ui.pictures.Image; // Import Image class for its constants

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Album>> albumsLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public AlbumsViewModel(@NonNull Application application) {
        super(application);
        loadAlbums();
    }

    public LiveData<List<Album>> getAlbums() {
        return albumsLiveData;
    }

    public void loadAlbums() {
        executorService.execute(() -> {
            // Map to store folder path -> list of URIs for that folder
            Map<String, List<Uri>> folderMap = new HashMap<>();
            // Map to store folder path -> cover image URI and its media type
            Map<String, AlbumCoverInfo> coverMap = new HashMap<>(); // NEW MAP

            // UPDATED: Use MediaStore.Files.getContentUri("external") to query for all media types
            Uri queryUri = MediaStore.Files.getContentUri("external");
            String[] projection = {
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.MEDIA_TYPE // NEW: Include media type
            };

            // NEW: Selection to filter for only images and videos
            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (?, ?)";
            String[] selectionArgs = new String[]{
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            };


            try (Cursor cursor = getApplication().getContentResolver().query(
                    queryUri, // UPDATED URI
                    projection,
                    selection, // ADDED SELECTION
                    selectionArgs, // ADDED SELECTION ARGS
                    MediaStore.Files.FileColumns.DATE_ADDED + " DESC" // Use FileColumns for sorting
            )) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    int mediaTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE); // NEW

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String path = cursor.getString(dataCol);
                        int mediaType = cursor.getInt(mediaTypeCol); // GET MEDIA TYPE

                        File file = new File(path);
                        if (!file.exists() || file.isDirectory()) continue;

                        String folderPath = file.getParentFile().getAbsolutePath();
                        folderMap.putIfAbsent(folderPath, new ArrayList<>());

                        // Use the correct collection URI to form the content URI
                        Uri contentUri;
                        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                            contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                        } else {
                            contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        }

                        folderMap.get(folderPath).add(contentUri);

                        // Keep track of the latest item's URI and its media type as the album cover
                        if (!coverMap.containsKey(folderPath)) {
                            coverMap.put(folderPath, new AlbumCoverInfo(contentUri, mediaType));
                        }
                    }
                }
            }

            List<Album> albums = new ArrayList<>();
            for (Map.Entry<String, List<Uri>> entry : folderMap.entrySet()) {
                String folderPath = entry.getKey();
                List<Uri> uris = entry.getValue();
                AlbumCoverInfo coverInfo = coverMap.get(folderPath);

                if (coverInfo != null && !uris.isEmpty()) {
                    String folderName = new File(folderPath).getName();
                    // UPDATED: Pass the cover image's media type
                    albums.add(new Album(folderName, coverInfo.uri, uris.size(), folderPath, coverInfo.mediaType));
                }
            }
            albumsLiveData.postValue(albums);
        });
    }

    // NEW private inner class to hold album cover information
    private static class AlbumCoverInfo {
        final Uri uri;
        final int mediaType;

        AlbumCoverInfo(Uri uri, int mediaType) {
            this.uri = uri;
            this.mediaType = mediaType;
        }
    }
}