package com.bretttech.gallery.ui.albums;

import android.net.Uri;
import com.bretttech.gallery.ui.pictures.Image; // Import Image for constants

public class Album {
    private final String name;
    private final Uri coverImageUri;
    private final int imageCount;
    private final String folderPath;
    private final int coverMediaType;
    private final long dateAdded; // <-- NEW field

    // UPDATED constructor
    public Album(String name, Uri coverImageUri, int imageCount, String folderPath, int coverMediaType, long dateAdded) {
        this.name = name;
        this.coverImageUri = coverImageUri;
        this.imageCount = imageCount;
        this.folderPath = folderPath;
        this.coverMediaType = coverMediaType;
        this.dateAdded = dateAdded; // <-- store cover date
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

    public int getCoverMediaType() {
        return coverMediaType;
    }

    // NEW getter
    public long getDateAdded() {
        return dateAdded;
    }

    public boolean isCoverVideo() {
        return coverMediaType == Image.MEDIA_TYPE_VIDEO;
    }
}