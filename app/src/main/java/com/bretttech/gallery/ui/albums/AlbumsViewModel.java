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
import com.bretttech.gallery.data.ImageDetailsManager;
import com.bretttech.gallery.ui.pictures.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AlbumsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Album>> albumsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Album>> allAlbumsUnfilteredLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AlbumCoverRepository albumCoverRepository;
    private final AlbumVisibilityManager visibilityManager;
    private final ImageDetailsManager imageDetailsManager;

    private List<Album> allAlbums = new ArrayList<>();
    private Map<String, List<Uri>> albumImageUrisMap = new HashMap<>(); // Track images in each album
    private SortOrder currentSortOrder = SortOrder.NAME_ASC;
    private String currentSearchQuery = null;

    public enum SortOrder {
        DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, COUNT_DESC, COUNT_ASC
    }

    private final MutableLiveData<Set<String>> hiddenAlbums = new MutableLiveData<>();

    public AlbumsViewModel(@NonNull Application application) {
        super(application);
        albumCoverRepository = new AlbumCoverRepository(application.getApplicationContext());
        visibilityManager = new AlbumVisibilityManager(application.getApplicationContext());
        imageDetailsManager = new ImageDetailsManager(application);
        hiddenAlbums.setValue(visibilityManager.getHiddenAlbumPaths());
        loadAlbums();
    }

    public LiveData<List<Album>> getAlbums() {
        return albumsLiveData;
    }

    public LiveData<List<Album>> getAllAlbumsUnfiltered() {
        return allAlbumsUnfilteredLiveData;
    }

    public LiveData<Set<String>> getHiddenAlbums() {
        return hiddenAlbums;
    }

    public void setAllAlbumsVisibility(List<String> albumPaths, boolean isHidden) {
        // Optimistic update on Main Thread
        Set<String> current = hiddenAlbums.getValue();
        if (current == null)
            current = new HashSet<>();
        Set<String> updated = new HashSet<>(current);

        if (isHidden) {
            updated.addAll(albumPaths);
        } else {
            updated.removeAll(albumPaths);
        }
        hiddenAlbums.setValue(updated);

        // Persist on Background Thread
        executorService.execute(() -> {
            visibilityManager.setAlbumsHidden(albumPaths, isHidden);
        });
    }

    public void setAlbumVisibility(String albumPath, boolean isHidden) {
        // Optimistic update on Main Thread
        Set<String> current = hiddenAlbums.getValue();
        if (current == null)
            current = new HashSet<>();
        Set<String> updated = new HashSet<>(current);
        if (isHidden) {
            updated.add(albumPath);
        } else {
            updated.remove(albumPath);
        }
        hiddenAlbums.setValue(updated);

        executorService.execute(() -> {
            visibilityManager.setAlbumHidden(albumPath, isHidden);
        });
    }

    public void sortAlbums(SortOrder sortOrder) {
        currentSortOrder = sortOrder;
        filterAndSortAlbums();
    }

    public void searchAlbums(String query) {
        currentSearchQuery = query;
        filterAndSortAlbums();
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
        if (context == null)
            return;
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
        Album newAlbum = new Album(albumName, null, 0, albumPath, 0, Image.MEDIA_TYPE_IMAGE,
                System.currentTimeMillis() / 1000);
        allAlbums.add(newAlbum);
        filterAndSortAlbums();
    }

    private void filterAndSortAlbums() {
        executorService.execute(() -> {
            List<Album> processedList = new ArrayList<>(allAlbums);

            // Apply search filter
            if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                String lowerCaseQuery = currentSearchQuery.toLowerCase();
                Set<String> albumsWithMatchingTags = new HashSet<>();

                // Check for tag matches in each album
                AtomicInteger pendingChecks = new AtomicInteger(albumImageUrisMap.size());
                CountDownLatch latch = new CountDownLatch(1);

                for (Map.Entry<String, List<Uri>> entry : albumImageUrisMap.entrySet()) {
                    String albumPath = entry.getKey();
                    List<Uri> imageUris = entry.getValue();

                    if (imageUris.isEmpty()) {
                        if (pendingChecks.decrementAndGet() == 0) {
                            latch.countDown();
                        }
                        continue;
                    }

                    AtomicInteger imageChecks = new AtomicInteger(imageUris.size());

                    for (Uri imageUri : imageUris) {
                        imageDetailsManager.getImageDetails(imageUri, details -> {
                            boolean tagMatch = details.getTags().stream()
                                    .anyMatch(tag -> tag.toLowerCase().contains(lowerCaseQuery));
                            if (tagMatch) {
                                synchronized (albumsWithMatchingTags) {
                                    albumsWithMatchingTags.add(albumPath);
                                }
                            }

                            if (imageChecks.decrementAndGet() == 0) {
                                if (pendingChecks.decrementAndGet() == 0) {
                                    latch.countDown();
                                }
                            }
                        });
                    }
                }

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Filter by album name OR if album contains images with matching tags
                processedList = processedList.stream()
                        .filter(album -> album.getName().toLowerCase().contains(lowerCaseQuery) ||
                                albumsWithMatchingTags.contains(album.getFolderPath()))
                        .collect(Collectors.toList());
            }

            // Apply sorting
            Comparator<Album> secondaryComparator;

            switch (currentSortOrder) {
                case NAME_ASC:
                    secondaryComparator = Comparator.comparing(Album::getName, String.CASE_INSENSITIVE_ORDER);
                    break;
                case NAME_DESC:
                    secondaryComparator = Comparator.comparing(Album::getName, String.CASE_INSENSITIVE_ORDER)
                            .reversed();
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
                if (isA1Camera && !isA2Camera)
                    return -1;
                if (!isA1Camera && isA2Camera)
                    return 1;
                return 0;
            };

            Comparator<Album> combinedComparator = primaryComparator.thenComparing(secondaryComparator);
            Collections.sort(processedList, combinedComparator);

            final Set<String> hiddenPaths = visibilityManager.getHiddenAlbumPaths();
            processedList.removeIf(album -> hiddenPaths.contains(album.getFolderPath()));

            albumsLiveData.postValue(processedList);
        });
    }

    public void loadAlbums() {
        executorService.execute(() -> {
            // Map<BucketId, Album>
            Map<Long, Album> albumMap = new HashMap<>();
            // Map<BucketId, List<Uri>>
            Map<Long, List<Uri>> imageMap = new HashMap<>();
            // Map<BucketId, String> to track folder paths for custom covers
            Map<Long, String> bucketPathMap = new HashMap<>();

            String securePathPrefix = getApplication().getFilesDir().getAbsolutePath() + File.separator + "secure";

            // Use BUCKET_ID for grouping (more reliable than path string)
            String[] projection;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                projection = new String[] {
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                        MediaStore.Files.FileColumns.BUCKET_ID,
                        MediaStore.Files.FileColumns._ID,
                        MediaStore.Files.FileColumns.MEDIA_TYPE,
                        MediaStore.Files.FileColumns.DATE_ADDED
                };
            } else {
                projection = new String[] {
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                        "bucket_id", // Literal string for pre-Q
                        MediaStore.Files.FileColumns._ID,
                        MediaStore.Files.FileColumns.MEDIA_TYPE,
                        MediaStore.Files.FileColumns.DATE_ADDED
                };
            }

            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (?, ?) AND "
                    + MediaStore.Files.FileColumns.DATA + " NOT LIKE ?";

            // Add IS_TRASHED filter for Android R+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                selection += " AND " + MediaStore.MediaColumns.IS_TRASHED + " != 1";
            }

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
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
                    int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);
                    int bucketIdColumn = -1;

                    // Handle bucket_id column index safely
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID);
                    } else {
                        bucketIdColumn = cursor.getColumnIndex("bucket_id");
                    }

                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    int bucketNameColumn = cursor
                            .getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME);

                    while (cursor.moveToNext()) {
                        long bucketId = 0;
                        if (bucketIdColumn != -1) {
                            bucketId = cursor.getLong(bucketIdColumn);
                        } else {
                            // Fallback for very old devices if bucket_id is missing (unlikely)
                            String path = cursor.getString(dataColumn);
                            File parent = new File(path).getParentFile();
                            bucketId = parent != null ? parent.getAbsolutePath().hashCode() : 0;
                        }

                        long id = cursor.getLong(idColumn);
                        int mediaType = cursor.getInt(mediaTypeColumn);
                        Uri coverUri = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                                ? ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                                : ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                        // Track image URIs for each album
                        imageMap.computeIfAbsent(bucketId, k -> new ArrayList<>()).add(coverUri);

                        Album album = albumMap.get(bucketId);
                        if (album == null) {
                            String path = cursor.getString(dataColumn);
                            File parentFile = new File(path).getParentFile();
                            if (parentFile == null)
                                continue;

                            String folderPath = parentFile.getAbsolutePath();
                            bucketPathMap.put(bucketId, folderPath);

                            String albumName = cursor.getString(bucketNameColumn);
                            // Fallback name if bucket name is null
                            if (albumName == null) {
                                albumName = parentFile.getName();
                            }

                            long dateAdded = cursor.getLong(dateAddedColumn);
                            album = new Album(albumName, coverUri, 1, folderPath, bucketId, mediaType, dateAdded);
                            albumMap.put(bucketId, album);
                        } else {
                            album.incrementImageCount();
                            // Since we sort by DATE_ADDED DESC, the first item is the newest (cover).
                            // We don't need to update cover unless we want specific logic.
                            // The original logic updated it if count == 1, which is effectively the first
                            // item.
                        }
                    }
                }
            }

            List<Album> finalAlbums = new ArrayList<>(albumMap.values());

            // Remove empty or invalid albums
            finalAlbums
                    .removeIf(album -> album == null || album.getImageCount() <= 0 || album.getCoverImageUri() == null);

            for (Album album : finalAlbums) {
                Uri customCover = albumCoverRepository.getCustomCover(album.getFolderPath());
                if (customCover != null) {
                    album.setCoverImageUri(customCover);
                    album.setCoverMediaType(albumCoverRepository.getCustomCoverMediaType(album.getFolderPath()));
                }
            }

            allAlbums = finalAlbums;

            // Rebuild albumImageUrisMap using paths as keys (since search uses paths)
            Map<String, List<Uri>> newImageMap = new HashMap<>();
            for (Map.Entry<Long, List<Uri>> entry : imageMap.entrySet()) {
                String path = bucketPathMap.get(entry.getKey());
                if (path != null) {
                    newImageMap.put(path, entry.getValue());
                }
            }
            albumImageUrisMap = newImageMap;

            allAlbumsUnfilteredLiveData.postValue(new ArrayList<>(allAlbums));
            filterAndSortAlbums();
        });
    }
}