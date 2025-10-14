package com.bretttech.gallery.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class AlbumVisibilityManager {
    private static final String PREFS_NAME = "album_visibility_prefs";
    private static final String KEY_HIDDEN_ALBUMS = "hidden_album_paths";

    private final SharedPreferences sharedPreferences;

    public AlbumVisibilityManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public Set<String> getHiddenAlbumPaths() {
        // By default, SharedPreferences.getStringSet returns an empty set if the key doesn't exist.
        // This ensures no albums are hidden by default on first install (Requirement 2).
        return new HashSet<>(sharedPreferences.getStringSet(KEY_HIDDEN_ALBUMS, new HashSet<>()));
    }

    public void setAlbumHidden(String albumPath, boolean isHidden) {
        Set<String> hiddenPaths = getHiddenAlbumPaths();
        Set<String> newHiddenPaths = new HashSet<>(hiddenPaths);

        if (isHidden) {
            newHiddenPaths.add(albumPath);
        } else {
            newHiddenPaths.remove(albumPath);
        }

        sharedPreferences.edit()
                .putStringSet(KEY_HIDDEN_ALBUMS, newHiddenPaths)
                .commit(); // MODIFIED: Use .commit() for synchronous write (Fixes toggle revert)
    }
}