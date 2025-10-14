package com.bretttech.gallery.ui.albums;

import android.app.Application;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
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
    private final MutableLiveData<List<Album>> allAlbumsUnfilteredLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AlbumCoverRepository albumCoverRepository;
    private final AlbumVisibilityManager visibilityManager;

    private List<Album> allAlbums = new ArrayList<>();
    private SortOrder currentSortOrder = SortOrder.NAME_ASC;

    public enum SortOrder {
        DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, COUNT_DESC, COUNT_ASC
    }

    public AlbumsViewModel(@NonNull Application application) {
        super(application);
        albumCoverRepository = new AlbumCoverRepository(application.getApplicationContext());
        visibilityManager = new AlbumVisibilityManager(application.getApplicationContext());
        loadAlbums();
    }

    public LiveData<List<Album>> getAlbums() {
        return albumsLiveData;
    }

    public LiveData<List<Album>> getAllAlbumsUnfiltered() {
        return allAlbumsUnfilteredLiveData;
    }

    public void setAlbumVisibility(String albumPath, boolean isHidden) {
        executorService.execute(() -> {
            visibilityManager.setAlbumHidden(albumPath, isHidden);
        });
    }

    public void sortAlbums(SortOrder sortOrder) {
        currentSortOrder = sortOrder;
        sortAndPostAlbums();
    }

    public void setAlbumCover(String albumPath, Uri coverUri, int mediaType) {
        for (Album album : allAlbums) {
            if (album.getFolderPath().equals(albumPath)) {
                album.setCacheBusterId(System.currentTimeMillis());
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

    public void addEmptyAlbum(String albumName, String albumPath) {
        for (Album album : allAlbums) {
            if (album.getFolderPath().equals(albumPath)) {
                return;
            }
        }
        Album newAlbum = new Album(albumName, null, 0, albumPath, Image.MEDIA_TYPE_IMAGE, System.currentTimeMillis() / 1000);
        allAlbums.add(newAlbum);
        sortAndPostAlbums();
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

            final Set<String> hiddenPaths = visibilityManager.getHiddenAlbumPaths();
            sortedList.removeIf(album -> hiddenPaths.contains(album.getFolderPath()));

            albumsLiveData.postValue(sortedList);
        });
    }

    public void loadAlbums() {
        executorService.execute(() -> {
            Map<String, Album> albumMap = new HashMap<>();

            File publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (publicPicturesDir != null && publicPicturesDir.exists() && publicPicturesDir.isDirectory()) {
                File[] subdirectories = publicPicturesDir.listFiles(File::isDirectory);
                if (subdirectories != null) {
                    for (File dir : subdirectories) {
                        // **BUG FIX**: Ignore hidden folders like .thumbnails
                        if (dir.getName().startsWith(".")) {
                            continue;
                        }
                        Album album = new Album(dir.getName(), null, 0, dir.getAbsolutePath(), Image.MEDIA_TYPE_IMAGE, dir.lastModified() / 1000);
                        albumMap.put(dir.getAbsolutePath(), album);
                    }
                }
            }

            String securePathPrefix = getApplication().getFilesDir().getAbsolutePath() + File.separator + "secure";
            String[] projection = {
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DATE_ADDED
            };
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
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                        File parentFile = new File(path).getParentFile();
                        if (parentFile == null || parentFile.getAbsolutePath() == null || parentFile.getName().startsWith(".")) {
                            continue; // Also ignore media inside hidden folders
                        }

                        String folderPath = parentFile.getAbsolutePath();
                        Album album = albumMap.get(folderPath);

                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
                        int mediaType = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE));
                        Uri coverUri = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                                ? ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                                : ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                        if (album == null) {
                            String albumName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME));
                            long dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED));
                            album = new Album(albumName, coverUri, 1, folderPath, mediaType, dateAdded);
                            albumMap.put(folderPath, album);
                        } else {
                            album.incrementImageCount();
                            if (album.getImageCount() == 1) {
                                album.setCoverImageUri(coverUri);
                                album.setCoverMediaType(mediaType);
                            }
                        }
                    }
                }
            }

            List<Album> finalAlbums = new ArrayList<>(albumMap.values());

            for (Album album : finalAlbums) {
                Uri customCover = albumCoverRepository.getCustomCover(album.getFolderPath());
                if (customCover != null) {
                    album.setCoverImageUri(customCover);
                    album.setCoverMediaType(albumCoverRepository.getCustomCoverMediaType(album.getFolderPath()));
                }
            }

            allAlbums = finalAlbums;
            allAlbumsUnfilteredLiveData.postValue(new ArrayList<>(allAlbums));
            sortAndPostAlbums();
        });
    }
}