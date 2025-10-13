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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecureFolderViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> imagesLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SecureFolderViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Image>> getImages() {
        return imagesLiveData;
    }

    public void loadImagesFromSecureFolder() {
        executorService.execute(() -> {
            List<Image> images = new ArrayList<>();
            File secureFolder = new File(getApplication().getFilesDir(), "secure");
            if (secureFolder.exists() && secureFolder.isDirectory()) {
                File[] files = secureFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        // This is a simplified version. A real implementation would need to
                        // distinguish between images and videos and get proper metadata.
                        // For now, we'll just use the file URI.
                        Uri uri = Uri.fromFile(file);
                        // We need to determine the media type. For this example, we'll assume image.
                        images.add(new Image(uri, Image.MEDIA_TYPE_IMAGE));
                    }
                }
            }
            imagesLiveData.postValue(images);
        });
    }
}