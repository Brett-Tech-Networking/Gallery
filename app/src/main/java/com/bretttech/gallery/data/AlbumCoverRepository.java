package com.bretttech.gallery.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumCoverRepository {
    private static final String PREFS_NAME = "album_covers_prefs";

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
            coverCache.putAll((Map<String, String>) (Map) sharedPreferences.getAll());
        });
    }

    public void setCustomCover(String albumPath, Uri coverUri) {
        executor.execute(() -> {
            String uriString = coverUri.toString();
            sharedPreferences.edit().putString(albumPath, uriString).apply();
            coverCache.put(albumPath, uriString);
        });
    }

    public Uri getCustomCover(String albumPath) {
        String uriString = coverCache.get(albumPath);
        return uriString != null ? Uri.parse(uriString) : null;
    }

    public void removeCustomCover(String albumPath) {
        executor.execute(() -> {
            sharedPreferences.edit().remove(albumPath).apply();
            coverCache.remove(albumPath);
        });
    }
}