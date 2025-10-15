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
import com.bretttech.gallery.ui.pictures.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> images = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public AlbumDetailViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Image>> getImages() {
        return images;
    }

    public void loadImagesFromAlbum(String folderPath) {
        executorService.execute(() -> {
            List<Image> imageList = new ArrayList<>();
            // Query for images
            queryMedia(imageList, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, folderPath, Image.MEDIA_TYPE_IMAGE);
            // Query for videos
            queryMedia(imageList, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, folderPath, Image.MEDIA_TYPE_VIDEO);

            // Sort by date added, newest first
            imageList.sort(Comparator.comparingLong(Image::getDateAdded).reversed());

            images.postValue(imageList);
        });
    }

    private void queryMedia(List<Image> images, Uri contentUri, String folderPath, int mediaType) {
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED
        };
        String selection = MediaStore.MediaColumns.DATA + " LIKE ?";
        String[] selectionArgs = new String[]{folderPath + File.separator + "%"};

        try (Cursor cursor = getApplication().getContentResolver().query(contentUri, projection, selection, selectionArgs, null)) {
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