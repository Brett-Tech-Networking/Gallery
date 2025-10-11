package com.bretttech.gallery.ui.albums;

import android.net.Uri;

public class Album {
    private final String name;
    private final Uri coverImageUri;
    private final int imageCount;
    private final String folderPath; // <-- new field

    public Album(String name, Uri coverImageUri, int imageCount, String folderPath) {
        this.name = name;
        this.coverImageUri = coverImageUri;
        this.imageCount = imageCount;
        this.folderPath = folderPath; // <-- store folder path
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
        return folderPath; // <-- getter for folder path
    }
}
