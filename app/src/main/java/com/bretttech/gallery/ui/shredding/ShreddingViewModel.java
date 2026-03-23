package com.bretttech.gallery.ui.shredding;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.utils.ImageShredder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShreddingViewModel extends AndroidViewModel {

    public interface ShredCallback {
        void onProgress(int current, int total);
        void onComplete(int successCount, int failureCount);
    }

    private final MutableLiveData<List<Image>> images = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ShreddingViewModel(@NonNull Application application) {
        super(application);
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
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATE_ADDED
            };

            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (?, ?)" +
                    " AND " + MediaStore.MediaColumns.IS_TRASHED + " != 1";
            String[] selectionArgs = {
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

            images.postValue(imageList);
        });
    }

    public void shredImages(List<Image> imagesToShred, ShredCallback callback) {
        executorService.execute(() -> {
            int successCount = 0;
            int failureCount = 0;

            for (int i = 0; i < imagesToShred.size(); i++) {
                Image image = imagesToShred.get(i);
                if (ImageShredder.shredImage(getApplication(), image.getUri())) {
                    successCount++;
                } else {
                    failureCount++;
                }

                int progress = i + 1;
                if (callback != null) {
                    callback.onProgress(progress, imagesToShred.size());
                }
            }

            if (callback != null) {
                callback.onComplete(successCount, failureCount);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
