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
import com.bretttech.gallery.data.ImageDetailsManager;
import com.bretttech.gallery.ui.pictures.Image;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AlbumDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> images = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ImageDetailsManager imageDetailsManager;
    private List<Image> allImages = new ArrayList<>();
    private String currentSearchQuery = null;

    public AlbumDetailViewModel(@NonNull Application application) {
        super(application);
        imageDetailsManager = new ImageDetailsManager(application);
    }

    public LiveData<List<Image>> getImages() {
        return images;
    }

    public void searchImages(String query) {
        currentSearchQuery = query;
        filterImages();
    }

    public void loadImagesFromAlbum(String folderPath, long bucketId) {
        executorService.execute(() -> {
            List<Image> imageList = new ArrayList<>();

            // Use BUCKET_ID if available (preferred), otherwise fallback to path matching
            if (bucketId != 0 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                queryMediaByBucketId(imageList, MediaStore.Files.getContentUri("external"), bucketId);
            } else {
                // Fallback for older devices or missing bucketId
                queryMediaByPath(imageList, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, folderPath,
                        Image.MEDIA_TYPE_IMAGE);
                queryMediaByPath(imageList, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, folderPath,
                        Image.MEDIA_TYPE_VIDEO);
            }

            // Sort by date added, newest first
            imageList.sort(Comparator.comparingLong(Image::getDateAdded).reversed());

            allImages = imageList;
            filterImages();
        });
    }

    private void filterImages() {
        executorService.execute(() -> {
            List<Image> filteredList = new ArrayList<>(allImages);

            if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                String lowerCaseQuery = currentSearchQuery.toLowerCase();
                List<Image> tagMatchingImages = new ArrayList<>();
                CountDownLatch latch = new CountDownLatch(filteredList.size());

                // Check all images for tag matches
                for (Image image : filteredList) {
                    imageDetailsManager.getImageDetails(image.getUri(), details -> {
                        boolean tagMatch = details.getTags().stream()
                                .anyMatch(tag -> tag.toLowerCase().contains(lowerCaseQuery));
                        if (tagMatch) {
                            synchronized (tagMatchingImages) {
                                tagMatchingImages.add(image);
                            }
                        }
                        latch.countDown();
                    });
                }

                try {
                    latch.await(); // Wait for all tag lookups to complete
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                // Filter by filename, date, OR tag match
                filteredList = filteredList.stream()
                        .filter(image -> {
                            // Check if matches by filename
                            if (image.getDisplayName() != null
                                    && image.getDisplayName().toLowerCase().contains(lowerCaseQuery)) {
                                return true;
                            }
                            // Check if matches by date
                            String formattedDate = sdf.format(new Date(image.getDateAdded() * 1000L));
                            if (formattedDate.contains(lowerCaseQuery)) {
                                return true;
                            }
                            // Check if matches by tag
                            return tagMatchingImages.contains(image);
                        })
                        .collect(Collectors.toList());
            }

            images.postValue(filteredList);
        });
    }

    private void queryMediaByBucketId(List<Image> images, Uri contentUri, long bucketId) {
        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE
        };

        String selection = MediaStore.Files.FileColumns.BUCKET_ID + " = ? AND " +
                MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (?, ?)";

        // Add IS_TRASHED filter for Android R+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            selection += " AND " + MediaStore.MediaColumns.IS_TRASHED + " != 1";
        }

        String[] selectionArgs = {
                String.valueOf(bucketId),
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        };

        try (Cursor cursor = getApplication().getContentResolver().query(contentUri, projection, selection,
                selectionArgs, null)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);
                int mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String displayName = cursor.getString(displayNameColumn);
                    long dateAdded = cursor.getLong(dateAddedColumn);
                    int mediaType = cursor.getInt(mediaTypeColumn);

                    Uri contentUriWithId = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                            ? ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                            : ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    images.add(new Image(contentUriWithId, mediaType, displayName, dateAdded));
                }
            }
        }
    }

    private void queryMediaByPath(List<Image> images, Uri contentUri, String folderPath, int mediaType) {
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED
        };
        String selection = MediaStore.MediaColumns.DATA + " LIKE ?";
        String[] selectionArgs = new String[] { folderPath + File.separator + "%" };

        try (Cursor cursor = getApplication().getContentResolver().query(contentUri, projection, selection,
                selectionArgs, null)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String displayName = cursor.getString(displayNameColumn);
                    long dateAdded = cursor.getLong(dateAddedColumn);

                    Uri contentUriWithId = ContentUris.withAppendedId(contentUri, id);
                    images.add(new Image(contentUriWithId, mediaType, displayName, dateAdded));
                }
            }
        }
    }
}