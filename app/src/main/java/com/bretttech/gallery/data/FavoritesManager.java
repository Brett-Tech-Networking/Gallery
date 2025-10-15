package com.bretttech.gallery.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.bretttech.gallery.ui.pictures.Image;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoritesManager {

    private static final String PREFS_NAME = "gallery_favorites";
    private static final String KEY_FAVORITES = "favorite_images";
    private static final String TAG = "FavoritesManager";


    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FavoritesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Register the custom UriAdapter
        gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriAdapter())
                .create();
    }

    public void getFavoriteImages(FavoritesCallback callback) {
        executor.execute(() -> {
            callback.onFavoritesLoaded(getFavoritesSync());
        });
    }

    public void addFavorite(Image image) {
        executor.execute(() -> {
            List<Image> favorites = getFavoritesSync();
            if (!favorites.contains(image)) {
                favorites.add(image);
                saveFavorites(favorites);
            }
        });
    }

    public void removeFavorite(Image image) {
        executor.execute(() -> {
            List<Image> favorites = getFavoritesSync();
            favorites.remove(image);
            saveFavorites(favorites);
        });
    }

    public void isFavorite(Image image, IsFavoriteCallback callback) {
        executor.execute(() -> {
            List<Image> favorites = getFavoritesSync();
            callback.onResult(favorites.contains(image));
        });
    }

    private List<Image> getFavoritesSync() {
        String json = sharedPreferences.getString(KEY_FAVORITES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<ArrayList<Image>>() {}.getType();
            List<Image> favorites = gson.fromJson(json, type);
            return favorites != null ? favorites : new ArrayList<>();
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing favorites, clearing invalid data", e);
            // Clear the corrupted data and return an empty list
            sharedPreferences.edit().remove(KEY_FAVORITES).apply();
            return new ArrayList<>();
        }
    }

    private void saveFavorites(List<Image> favorites) {
        String json = gson.toJson(favorites);
        sharedPreferences.edit().putString(KEY_FAVORITES, json).apply();
    }

    // Callbacks to return results from the background thread
    public interface FavoritesCallback {
        void onFavoritesLoaded(List<Image> favorites);
    }

    public interface IsFavoriteCallback {
        void onResult(boolean isFavorite);
    }
}