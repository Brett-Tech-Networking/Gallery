package com.bretttech.gallery.ui.albums;

import android.app.Application;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> imagesLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public AlbumDetailViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Image>> getImages() {
        return imagesLiveData;
    }

    public void loadImagesFromAlbum(String folderPath) {
        executorService.execute(() -> {
            List<Image> images = new ArrayList<>();

            Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Images.Media._ID};
            // Filter by folder path
            String selection = MediaStore.Images.Media.DATA + " LIKE ?";
            String[] selectionArgs = new String[]{folderPath + "/%"};

            try (Cursor cursor = getApplication().getContentResolver().query(
                    imagesUri,
                    projection,
                    selection,
                    selectionArgs,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
            )) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        Uri contentUri = Uri.withAppendedPath(imagesUri, String.valueOf(id));
                        images.add(new Image(contentUri));
                    }
                }
            }
            imagesLiveData.postValue(images);
        });
    }
}