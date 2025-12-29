package com.bretttech.gallery.utils;

import android.content.Context;
import com.bretttech.gallery.R;

public class LocalizationUtils {

    public static String getLocalizedAlbumName(Context context, String originalName) {
        if (originalName == null)
            return "";

        // Normalize checking (case-insensitive for safety, though FS usually
        // case-sensitive/retentive)
        String lowerName = originalName.toLowerCase();

        switch (lowerName) {
            case "camera":
            case "dcim":
                return context.getString(R.string.folder_camera);
            case "screenshots":
                return context.getString(R.string.folder_screenshots);
            case "download":
            case "downloads":
                return context.getString(R.string.folder_download);
            case "movies":
                return context.getString(R.string.folder_movies);
            case "pictures":
                return context.getString(R.string.title_pictures);
            default:
                return originalName;
        }
    }
}
