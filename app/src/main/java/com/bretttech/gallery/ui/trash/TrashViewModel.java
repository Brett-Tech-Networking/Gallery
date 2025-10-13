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
            Log.d(TAG, "Starting to load trashed images/videos with new query method..."); // UPDATED log
            List<Image> trashedImageList = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ContentResolver contentResolver = getApplication().getContentResolver();
                Cursor cursor = null;
                try {
                    Uri queryUri = MediaStore.Files.getContentUri("external");
                    String[] projection = {MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE};

                    Bundle queryArgs = new Bundle();

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
                            int mediaType = cursor.getInt(mediaTypeColumn);
                            // UPDATED: Check for both image and video
                            if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ||
                                    mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                                long id = cursor.getLong(idColumn);

                                Uri contentUri;
                                // NEW: Determine URI based on media type
                                if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                                    contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                                } else {
                                    contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                                }

                                Log.d(TAG, "Found trashed media (Type: " + mediaType + ") with URI: " + contentUri); // UPDATED log
                                // UPDATED: Pass mediaType to Image constructor
                                trashedImageList.add(new Image(contentUri, mediaType));
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "An exception occurred while querying for trashed media.", e); // UPDATED log
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } else {
                Log.w(TAG, "Device Android version is older than R, trash functionality is not supported."); // UPDATED log
            }
            Log.d(TAG, "Finished loading. Posting " + trashedImageList.size() + " media items to UI."); // UPDATED log
            trashedImages.postValue(trashedImageList);
        });
    }
}