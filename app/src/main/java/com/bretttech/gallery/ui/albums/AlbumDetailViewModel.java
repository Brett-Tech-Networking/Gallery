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

public class AlbumDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> imagesLiveData = new MutableLiveData<>();

    public AlbumDetailViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Image>> getImages() {
        return imagesLiveData;
    }

    // Filter images strictly by folder path
    public void loadImagesFromAlbum(String folderPath) {
        List<Image> images = new ArrayList<>();

        Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };

        Cursor cursor = getApplication().getContentResolver().query(
                imagesUri,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );

        if (cursor != null) {
            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String path = cursor.getString(dataCol);

                File file = new File(path);
                if (!file.exists() || file.isDirectory()) continue;

                String currentFolder = file.getParentFile().getAbsolutePath();
                if (!currentFolder.equals(folderPath)) continue;

                Uri contentUri = Uri.withAppendedPath(imagesUri, String.valueOf(id));
                images.add(new Image(contentUri));
            }

            cursor.close();
        }

        imagesLiveData.postValue(images);
    }
}
