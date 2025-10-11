package com.bretttech.gallery.ui.albums;

import android.app.Application;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Album>> albumsLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public AlbumsViewModel(@NonNull Application application) {
        super(application);
        loadAlbums();
    }

    public LiveData<List<Album>> getAlbums() {
        return albumsLiveData;
    }

    public void loadAlbums() {
        executorService.execute(() -> {
            Map<String, List<Uri>> folderMap = new HashMap<>();

            Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};

            try (Cursor cursor = getApplication().getContentResolver().query(
                    imagesUri,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
            )) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String path = cursor.getString(dataCol);

                        File file = new File(path);
                        if (!file.exists() || file.isDirectory()) continue;

                        String folderPath = file.getParentFile().getAbsolutePath();
                        folderMap.putIfAbsent(folderPath, new ArrayList<>());
                        Uri contentUri = Uri.withAppendedPath(imagesUri, String.valueOf(id));
                        folderMap.get(folderPath).add(contentUri);
                    }
                }
            }

            List<Album> albums = new ArrayList<>();
            for (Map.Entry<String, List<Uri>> entry : folderMap.entrySet()) {
                String folderPath = entry.getKey();
                List<Uri> uris = entry.getValue();
                if (!uris.isEmpty()) {
                    String folderName = new File(folderPath).getName();
                    albums.add(new Album(folderName, uris.get(0), uris.size(), folderPath));
                }
            }
            albumsLiveData.postValue(albums);
        });
    }
}