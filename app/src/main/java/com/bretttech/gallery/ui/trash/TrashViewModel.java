package com.bretttech.gallery.ui.trash;

import android.app.Application;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bretttech.gallery.ui.pictures.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrashViewModel extends AndroidViewModel {

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
            List<Image> trashedImageList = new ArrayList<>();
            Uri queryUri = MediaStore.Files.getContentUri("external");

            Bundle queryArgs = new Bundle();
            queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY);

            String[] projection = {
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATE_ADDED
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try (Cursor cursor = getApplication().getContentResolver().query(
                        queryUri,
                        projection,
                        queryArgs,
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

                            trashedImageList.add(new Image(contentUri, mediaType, displayName, dateAdded));
                        }
                    }
                }
            }
            trashedImageList.sort(Comparator.comparingLong(Image::getDateAdded).reversed());
            trashedImages.postValue(trashedImageList);
        });
    }
}