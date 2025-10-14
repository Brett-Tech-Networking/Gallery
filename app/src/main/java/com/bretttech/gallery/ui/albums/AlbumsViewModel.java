package com.bretttech.gallery.ui.albums;

import android.app.Application;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bretttech.gallery.data.AlbumCoverRepository;
import com.bretttech.gallery.data.AlbumVisibilityManager;
import com.bretttech.gallery.ui.pictures.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Album>> albumsLiveData = new MutableLiveData<>();
    // NEW: LiveData to hold all public albums (for the "Hide Albums" screen)
    private final MutableLiveData<List<Album>> allAlbumsUnfilteredLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AlbumCoverRepository albumCoverRepository;
    private final AlbumVisibilityManager visibilityManager; // NEW FIELD

    private List<Album> allAlbums = new ArrayList<>();
    private SortOrder currentSortOrder = SortOrder.NAME_ASC;

    public enum SortOrder {
        DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, COUNT_DESC, COUNT_ASC
    }

    public AlbumsViewModel(@NonNull Application application) {
        super(application);
        albumCoverRepository = new AlbumCoverRepository(application.getApplicationContext());
        visibilityManager = new AlbumVisibilityManager(application.getApplicationContext()); // NEW INIT
        loadAlbums();
    }

    public LiveData<List<Album>> getAlbums() {
        return albumsLiveData;
    }

    public LiveData<List<Album>> getAllAlbumsUnfiltered() { // NEW GETTER for HideAlbumsFragment
        return allAlbumsUnfilteredLiveData;
    }

    // NEW: Method to update album visibility
    public void setAlbumVisibility(String albumPath, boolean isHidden) {
        executorService.execute(() -> {
            visibilityManager.setAlbumHidden(albumPath, isHidden);
            loadAlbums(); // Reload all albums to update both LiveData objects
        });
    }

    public void sortAlbums(SortOrder sortOrder) {
        currentSortOrder = sortOrder;
        sortAndPostAlbums();
    }

    // MODIFIED: Added mediaType parameter and cache buster logic
    public void setAlbumCover(String albumPath, Uri coverUri, int mediaType) {
        // Find the album in the current list and manually update the cache buster for immediate visual update
        for (Album album : allAlbums) {
            if (album.getFolderPath().equals(albumPath)) {
                album.setCacheBusterId(System.currentTimeMillis()); // NEW: Set unique cache buster ID
                break;
            }
        }
        albumCoverRepository.setCustomCover(albumPath, coverUri, mediaType);
        loadAlbums();
    }

    public void removeCustomCover(String albumPath) {
        albumCoverRepository.removeCustomCover(albumPath);
    }

    public static void scanFile(Context context, Uri uri) {
        if (context == null) return;
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(uri);
        context.sendBroadcast(scanIntent);
    }


    private void sortAndPostAlbums() {
        executorService.execute(() -> {
            List<Album> sortedList = new ArrayList<>(allAlbums);
            Comparator<Album> secondaryComparator;

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

            Comparator<Album> primaryComparator = (a1, a2) -> {
                boolean isA1Camera = a1.getName().equalsIgnoreCase("Camera");
                boolean isA2Camera = a2.getName().equalsIgnoreCase("Camera");
                if (isA1Camera && !isA2Camera) return -1;
                if (!isA1Camera && isA2Camera) return 1;
                return 0;
            };

            Comparator<Album> combinedComparator = primaryComparator.thenComparing(secondaryComparator);
            Collections.sort(sortedList, combinedComparator);

            // NEW: Filter out hidden albums before posting to the main LiveData
            final Set<String> hiddenPaths = visibilityManager.getHiddenAlbumPaths();
            sortedList.removeIf(album -> hiddenPaths.contains(album.getFolderPath()));

            albumsLiveData.postValue(sortedList);
        });
    }

    public void loadAlbums() {
        executorService.execute(() -> {
            List<Album> albums = new ArrayList<>();
            Map<String, Album> albumMap = new HashMap<>();

            String securePathPrefix = getApplication().getFilesDir().getAbsolutePath() + File.separator + "secure";

            String[] projection = {
                    MediaStore.Files.FileColumns.BUCKET_ID,
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DATE_ADDED
            };
            // Ensure we are only loading PUBLIC albums here
            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (?, ?) AND " + MediaStore.Files.FileColumns.DATA + " NOT LIKE ?";
            String[] selectionArgs = {
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                    "%" + securePathPrefix + "%"
            };
            String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

            try (Cursor cursor = getApplication().getContentResolver().query(
                    MediaStore.Files.getContentUri("external"),
                    projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String bucketId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID));
                        Album album = albumMap.get(bucketId);
                        if (album == null) {
                            String albumName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME));
                            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
                            int mediaType = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE));
                            long dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED));

                            File parentFile = new File(path).getParentFile();
                            String folderPath = parentFile != null ? parentFile.getAbsolutePath() : "";
                            Uri coverUri;
                            if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                                coverUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                            } else {
                                coverUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                            }

                            album = new Album(albumName, coverUri, 1, folderPath, mediaType, dateAdded);
                            albumMap.put(bucketId, album);
                            albums.add(album);
                        } else {
                            album.incrementImageCount();
                        }
                    }
                }
            }

            for (Album album : albums) {
                Uri customCover = albumCoverRepository.getCustomCover(album.getFolderPath());
                if (customCover != null) {
                    album.setCoverImageUri(customCover);
                    int customMediaType = albumCoverRepository.getCustomCoverMediaType(album.getFolderPath());
                    album.setCoverMediaType(customMediaType);
                    // NEW: Retrieve the latest cache buster ID from an existing album object if available
                    for (Album existingAlbum : allAlbums) {
                        if (existingAlbum.getFolderPath().equals(album.getFolderPath())) {
                            album.setCacheBusterId(existingAlbum.getCacheBusterId());
                            break;
                        }
                    }
                }
            }

            allAlbums = albums;
            // Post the unfiltered list first
            allAlbumsUnfilteredLiveData.postValue(new ArrayList<>(allAlbums));
            // Then sort and apply visibility filter for the main AlbumsFragment
            sortAndPostAlbums();
        });
    }
}