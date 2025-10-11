package com.bretttech.gallery.ui.trash;

import android.app.Application;
import android.content.ContentUris;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bretttech.gallery.ui.pictures.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrashViewModel extends AndroidViewModel {

    private static final String TAG = "TrashViewModel";

    private final MutableLiveData<List<Image>> trashedImages = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public TrashViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Image>> getTrashedImages() {
        return trashedImages;
    }

    public void loadTrashedImages() {
        executorService.execute(() -> {
            Log.d(TAG, "Starting to load trashed images with new query method...");
            List<Image> trashedImageList = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // This modern method is for Android 11+
                ContentResolver contentResolver = getApplication().getContentResolver();
                Cursor cursor = null;
                try {
                    // This is the URI for all files (including media) on the device.
                    Uri queryUri = MediaStore.Files.getContentUri("external");
                    String[] projection = {MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE};

                    // We will use a Bundle to create the query, which is the modern approach.
                    Bundle queryArgs = new Bundle();

                    // This is the key change: We are now asking for items that are in the "trashed" state.
                    queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY);

                    Log.d(TAG, "Query URI: " + queryUri);
                    Log.d(TAG, "Querying with MediaStore.QUERY_ARG_MATCH_TRASHED");

                    cursor = contentResolver.query(queryUri, projection, queryArgs, null);

                    if (cursor == null) {
                        Log.e(TAG, "Query returned a null cursor.");
                    } else {
                        Log.d(TAG, "Query successful. Found " + cursor.getCount() + " items.");
                        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                        int mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);

                        while (cursor.moveToNext()) {
                            // We need to check if the found item is an image.
                            int mediaType = cursor.getInt(mediaTypeColumn);
                            if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                                long id = cursor.getLong(idColumn);
                                // FIX: Use the specific Images collection URI for the content URI,
                                // which is what MediaStore.createDeleteRequest expects for images.
                                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                                Log.d(TAG, "Found trashed image with URI: " + contentUri);
                                trashedImageList.add(new Image(contentUri));
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "An exception occurred while querying for trashed images.", e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } else {
                Log.w(TAG, "Device Android version is older than R, this method is not supported.");
            }
            Log.d(TAG, "Finished loading. Posting " + trashedImageList.size() + " images to UI.");
            trashedImages.postValue(trashedImageList);
        });
    }
}