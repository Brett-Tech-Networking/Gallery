package com.bretttech.gallery.ui.secure;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bretttech.gallery.ui.pictures.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecureAlbumDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> images = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SecureAlbumDetailViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Image>> getImages() {
        return images;
    }

    public void loadImages(String albumPath) {
        executorService.execute(() -> {
            List<Image> imageList = new ArrayList<>();
            File albumDir = new File(albumPath);
            if (albumDir.exists() && albumDir.isDirectory()) {
                File[] files = albumDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        Uri uri = Uri.fromFile(file);
                        imageList.add(new Image(uri, Image.MEDIA_TYPE_IMAGE, file.getName(), file.lastModified() / 1000));
                    }
                }
            }
            imageList.sort(Comparator.comparingLong(Image::getDateAdded).reversed());
            images.postValue(imageList);
        });
    }
}