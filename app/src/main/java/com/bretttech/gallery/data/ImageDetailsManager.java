package com.bretttech.gallery.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import com.google.gson.Gson;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageDetailsManager {

    private static final String PREFS_NAME = "gallery_image_details";
    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ImageDetailsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void getImageDetails(Uri imageUri, DetailsCallback callback) {
        executor.execute(() -> {
            String json = sharedPreferences.getString(imageUri.toString(), null);
            ImageDetails details;
            if (json == null) {
                details = new ImageDetails();
            } else {
                details = gson.fromJson(json, ImageDetails.class);
            }
            callback.onDetailsLoaded(details);
        });
    }

    public void saveImageDetails(Uri imageUri, ImageDetails details) {
        executor.execute(() -> {
            String json = gson.toJson(details);
            sharedPreferences.edit().putString(imageUri.toString(), json).apply();
        });
    }

    public interface DetailsCallback {
        void onDetailsLoaded(ImageDetails details);
    }
}