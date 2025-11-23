package com.bretttech.gallery.ui.pictures;

import android.app.Application;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bretttech.gallery.data.ImageDetails;
import com.bretttech.gallery.data.ImageDetailsManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PicturesViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> images = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private List<Image> allImages = new ArrayList<>();
    private SortOrder currentSortOrder = SortOrder.DATE_DESC;
    private String currentSearchQuery = null;
    private final ImageDetailsManager imageDetailsManager;

    public enum SortOrder {
        DATE_DESC, DATE_ASC
    }

    public PicturesViewModel(@NonNull Application application) {
        super(application);
        imageDetailsManager = new ImageDetailsManager(application);
    }

    public LiveData<List<Image>> getImages() {
        return images;
    }

    public void loadImages() {
        executorService.execute(() -> {
            List<Image> imageList = new ArrayList<>();
            Uri queryUri = MediaStore.Files.getContentUri("external");

            String[] projection = {
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.DISPLAY_NAME
            };

            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (?, ?)";
            String[] selectionArgs = new String[]{
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            };

            try (Cursor cursor = getApplication().getContentResolver().query(
                    queryUri,
                    projection,
                    selection,
                    selectionArgs,
                    null
            )) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
                    int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);


                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        int mediaType = cursor.getInt(mediaTypeColumn);
                        String displayName = cursor.getString(displayNameColumn);
                        long dateAdded = cursor.getLong(dateAddedColumn);

                        Uri contentUri;
                        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                            contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                        } else {
                            contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        }

                        imageList.add(new Image(contentUri, mediaType, displayName, dateAdded));
                    }
                }
            }
            allImages = imageList;
            filterAndSortImages();
        });
    }

    public void sortImages(SortOrder sortOrder) {
        currentSortOrder = sortOrder;
        filterAndSortImages();
    }

    public void searchImages(String query) {
        currentSearchQuery = query;
        filterAndSortImages();
    }

    private void filterAndSortImages() {
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
                            if (image.getDisplayName() != null && image.getDisplayName().toLowerCase().contains(lowerCaseQuery)) {
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

            if (currentSortOrder == SortOrder.DATE_ASC) {
                filteredList.sort(Comparator.comparingLong(Image::getDateAdded));
            } else {
                filteredList.sort(Comparator.comparingLong(Image::getDateAdded).reversed());
            }

            images.postValue(filteredList);
        });
    }
}