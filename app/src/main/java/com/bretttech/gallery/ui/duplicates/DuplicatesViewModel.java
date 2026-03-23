package com.bretttech.gallery.ui.duplicates;

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
import com.bretttech.gallery.utils.DuplicateFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DuplicatesViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> allImages = new MutableLiveData<>();
    private final MutableLiveData<List<DuplicateFinder.DuplicateGroup>> duplicateGroups = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public DuplicatesViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Image>> getAllImages() {
        return allImages;
    }

    public LiveData<List<DuplicateFinder.DuplicateGroup>> getDuplicateGroups() {
        return duplicateGroups;
    }

    public void setDuplicateGroups(List<DuplicateFinder.DuplicateGroup> groups) {
        duplicateGroups.postValue(groups);
    }

    public void loadAllImages() {
        executorService.execute(() -> {
            List<Image> images = new ArrayList<>();
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

                        images.add(new Image(contentUri, mediaType, displayName, dateAdded));
                    }
                }
            }

            allImages.postValue(images);
        });
    }

    public void deleteDuplicates(List<Image> images) {
        executorService.execute(() -> {
            ContentResolver contentResolver = getApplication().getContentResolver();
            for (Image image : images) {
                try {
                    contentResolver.delete(image.getUri(), null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
