package com.bretttech.gallery.ui.albums;

import android.net.Uri;
import com.bretttech.gallery.ui.pictures.Image; // Import Image for constants

public class Album {
    private final String name;
    private final Uri coverImageUri;
    private final int imageCount;
    private final String folderPath;
    private final int coverMediaType; // <-- NEW field

    // UPDATED constructor
    public Album(String name, Uri coverImageUri, int imageCount, String folderPath, int coverMediaType) {
        this.name = name;
        this.coverImageUri = coverImageUri;
        this.imageCount = imageCount;
        this.folderPath = folderPath;
        this.coverMediaType = coverMediaType; // <-- store cover media type
    }

    public String getName() {
        return name;
    }

    public Uri getCoverImageUri() {
        return coverImageUri;
    }

    public int getImageCount() {
        return imageCount;
    }

    public String getFolderPath() {
        return folderPath;
    }

    // NEW getter
    public int getCoverMediaType() {
        return coverMediaType;
    }

    // NEW helper method
    public boolean isCoverVideo() {
        return coverMediaType == Image.MEDIA_TYPE_VIDEO;
    }
}