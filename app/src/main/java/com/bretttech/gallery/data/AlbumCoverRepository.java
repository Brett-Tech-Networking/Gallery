package com.bretttech.gallery.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.bretttech.gallery.ui.pictures.Image;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumCoverRepository {
    private static final String PREFS_NAME = "album_covers_prefs";
    private static final String TYPE_SUFFIX = "_type";

    private final SharedPreferences sharedPreferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, String> coverCache = new HashMap<>();

    public AlbumCoverRepository(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadCoversIntoCache();
    }

    private void loadCoversIntoCache() {
        executor.execute(() -> {
            coverCache.clear();
            // This is just loading URI strings, the integer media type is read directly from sharedPreferences as needed.
            Map<String, ?> allEntries = sharedPreferences.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                if (!entry.getKey().endsWith(TYPE_SUFFIX) && entry.getValue() instanceof String) {
                    coverCache.put(entry.getKey(), (String) entry.getValue());
                }
            }
        });
    }

    // MODIFIED: Added mediaType parameter
    public void setCustomCover(String albumPath, Uri coverUri, int mediaType) {
        executor.execute(() -> {
            String uriString = coverUri.toString();
            sharedPreferences.edit()
                    .putString(albumPath, uriString)
                    .putInt(albumPath + TYPE_SUFFIX, mediaType) // NEW: Store media type
                    .apply();
            coverCache.put(albumPath, uriString);
        });
    }

    public Uri getCustomCover(String albumPath) {
        String uriString = coverCache.get(albumPath);
        return uriString != null ? Uri.parse(uriString) : null;
    }

    // NEW: Method to get the custom cover media type
    public int getCustomCoverMediaType(String albumPath) {
        // Default to Image media type if no custom type is found
        return sharedPreferences.getInt(albumPath + TYPE_SUFFIX, Image.MEDIA_TYPE_IMAGE);
    }

    // MODIFIED: Updated to remove both URI and TYPE preference
    public void removeCustomCover(String albumPath) {
        executor.execute(() -> {
            sharedPreferences.edit()
                    .remove(albumPath)
                    .remove(albumPath + TYPE_SUFFIX) // NEW: Remove type
                    .apply();
            coverCache.remove(albumPath);
        });
    }
}