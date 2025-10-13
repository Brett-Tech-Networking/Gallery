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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Album>> albumsLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private List<Album> allAlbums = new ArrayList<>();
    private SortOrder currentSortOrder = SortOrder.NAME_ASC; // Default to A-Z as requested

    public enum SortOrder {
        DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, COUNT_DESC, COUNT_ASC
    }

    public AlbumsViewModel(@NonNull Application application) {
        super(application);
        loadAlbums();
    }

    public LiveData<List<Album>> getAlbums() {
        return albumsLiveData;
    }

    public void sortAlbums(SortOrder sortOrder) {
        currentSortOrder = sortOrder;
        sortAndPostAlbums();
    }

    private void sortAndPostAlbums() {
        executorService.execute(() -> {
            List<Album> sortedList = new ArrayList<>(allAlbums);
            Comparator<Album> secondaryComparator;

            // 1. Define the secondary comparator (user preference)
            switch (currentSortOrder) {
                case NAME_ASC:
                    secondaryComparator = Comparator.comparing(Album::getName, String.CASE_INSENSITIVE_ORDER);
                    break;
                case NAME_DESC:
                    secondaryComparator = Comparator.comparing(Album::getName, String.CASE_INSENSITIVE_ORDER).reversed();
                    break;
                case COUNT_DESC:
                    secondaryComparator = Comparator.comparing(Album::getImageCount).reversed();
                    break;
                case COUNT_ASC:
                    secondaryComparator = Comparator.comparing(Album::getImageCount);
                    break;
                case DATE_ASC:
                    secondaryComparator = Comparator.comparing(Album::getDateAdded);
                    break;
                case DATE_DESC:
                default:
                    secondaryComparator = Comparator.comparing(Album::getDateAdded).reversed();
                    break;
            }

            // 2. Primary Comparator to prioritize the "Camera" album (Pin feature)
            Comparator<Album> primaryComparator = (a1, a2) -> {
                boolean isA1Camera = a1.getName().equalsIgnoreCase("Camera");
                boolean isA2Camera = a2.getName().equalsIgnoreCase("Camera");

                if (isA1Camera && !isA2Camera) {
                    return -1; // A1 (Camera) comes before A2
                }
                if (!isA1Camera && isA2Camera) {
                    return 1;  // A2 (Camera) comes before A1
                }
                return 0;      // Neither or both are 'Camera'
            };

            // 3. Combine: Pin the Camera album first, then apply selected sort to the rest
            Comparator<Album> combinedComparator = primaryComparator.thenComparing(secondaryComparator);

            Collections.sort(sortedList, combinedComparator);
            albumsLiveData.postValue(sortedList);
        });
    }

    public void loadAlbums() {
        executorService.execute(() -> {
            Map<String, List<Uri>> folderMap = new HashMap<>();
            Map<String, AlbumCoverInfo> coverMap = new HashMap<>();

            Uri queryUri = MediaStore.Files.getContentUri("external");
            String[] projection = {
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DATE_ADDED
            };

            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (?, ?) AND " + MediaStore.Files.FileColumns.DATA + " NOT LIKE ?";
            String[] selectionArgs = new String[]{
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                    "%" + getApplication().getFilesDir().getAbsolutePath() + "/secure%"
            };

            try (Cursor cursor = getApplication().getContentResolver().query(
                    queryUri,
                    projection,
                    selection,
                    selectionArgs,
                    null
            )) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    int mediaTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
                    int dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String path = cursor.getString(dataCol);
                        int mediaType = cursor.getInt(mediaTypeCol);
                        long dateAdded = cursor.getLong(dateAddedCol);

                        File file = new File(path);
                        if (!file.exists() || file.isDirectory()) continue;

                        String folderPath = file.getParentFile().getAbsolutePath();
                        folderMap.putIfAbsent(folderPath, new ArrayList<>());

                        Uri contentUri;
                        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                            contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                        } else {
                            contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        }

                        folderMap.get(folderPath).add(contentUri);

                        // Keep track of the latest item's URI, type, and DATE as the album cover
                        if (!coverMap.containsKey(folderPath) || dateAdded > coverMap.get(folderPath).dateAdded) {
                            coverMap.put(folderPath, new AlbumCoverInfo(contentUri, mediaType, dateAdded));
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
                    albums.add(new Album(folderName, coverInfo.uri, uris.size(), folderPath, coverInfo.mediaType, coverInfo.dateAdded));
                }
            }

            allAlbums = albums;
            sortAndPostAlbums();
        });
    }

    private static class AlbumCoverInfo {
        final Uri uri;
        final int mediaType;
        final long dateAdded;

        AlbumCoverInfo(Uri uri, int mediaType, long dateAdded) {
            this.uri = uri;
            this.mediaType = mediaType;
            this.dateAdded = dateAdded;
        }
    }
}