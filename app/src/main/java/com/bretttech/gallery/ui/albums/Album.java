package com.bretttech.gallery.ui.albums;

import android.net.Uri;

public class Album {
    private final String name;
    private final Uri coverImageUri;
    private final int imageCount;

    public Album(String name, Uri coverImageUri, int imageCount) {
        this.name = name;
        this.coverImageUri = coverImageUri;
        this.imageCount = imageCount;
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
}
