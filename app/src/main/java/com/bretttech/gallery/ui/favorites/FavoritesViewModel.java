package com.bretttech.gallery.ui.favorites;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bretttech.gallery.data.FavoritesManager;
import com.bretttech.gallery.ui.pictures.Image;
import java.util.List;

public class FavoritesViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Image>> favoriteImages = new MutableLiveData<>();
    private final FavoritesManager favoritesManager;

    public FavoritesViewModel(@NonNull Application application) {
        super(application);
        favoritesManager = new FavoritesManager(application);
    }

    public LiveData<List<Image>> getFavoriteImages() {
        return favoriteImages;
    }

    public void loadFavoriteImages() {
        favoritesManager.getFavoriteImages(images -> favoriteImages.postValue(images));
    }
}