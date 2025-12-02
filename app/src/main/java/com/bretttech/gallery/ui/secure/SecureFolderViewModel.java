package com.bretttech.gallery.ui.secure;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bretttech.gallery.ui.albums.Album;
import com.bretttech.gallery.ui.pictures.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecureFolderViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Album>> albumsLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SecureFolderViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Album>> getAlbums() {
        return albumsLiveData;
    }

    public void loadAlbumsFromSecureFolder() {
        executorService.execute(() -> {
            List<Album> albums = new ArrayList<>();
            File secureFolder = new File(getApplication().getFilesDir(), "secure");
            if (secureFolder.exists() && secureFolder.isDirectory()) {
                File[] albumDirs = secureFolder.listFiles(File::isDirectory);
                if (albumDirs != null) {
                    for (File albumDir : albumDirs) {
                        File[] images = albumDir.listFiles();
                        if (images != null && images.length > 0) {
                            // Use the first image as the cover
                            Uri coverUri = Uri.fromFile(images[0]);
                            albums.add(new Album(albumDir.getName(), coverUri, images.length,
                                    albumDir.getAbsolutePath(), 0, Image.MEDIA_TYPE_IMAGE, albumDir.lastModified()));
                        }
                    }
                }
            }
            albumsLiveData.postValue(albums);
        });
    }
}