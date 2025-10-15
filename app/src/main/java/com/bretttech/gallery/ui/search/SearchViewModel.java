package com.bretttech.gallery.ui.search;

import android.app.Application;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bretttech.gallery.data.ImageDetailsManager;
import com.bretttech.gallery.ui.pictures.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> searchResults = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ImageDetailsManager imageDetailsManager;

    public SearchViewModel(@NonNull Application application) {
        super(application);
        imageDetailsManager = new ImageDetailsManager(application);
    }

    public LiveData<List<Image>> getSearchResults() {
        return searchResults;
    }

    public void search(String query) {
        executorService.execute(() -> {
            List<Image> results = new ArrayList<>();
            // Search logic here...
            searchResults.postValue(results);
        });
    }
}