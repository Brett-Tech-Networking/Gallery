package com.bretttech.gallery.ui.pictures;

import android.net.Uri;
import android.provider.MediaStore;

public class Image {
    public static final int MEDIA_TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
    public static final int MEDIA_TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

    private final Uri uri;
    private final int mediaType; // NEW FIELD

    // UPDATED constructor
    public Image(Uri uri, int mediaType) {
        this.uri = uri;
        this.mediaType = mediaType;
    }

    public Uri getUri() {
        return uri;
    }

    // NEW getter
    public int getMediaType() {
        return mediaType;
    }

    // NEW helper method
    public boolean isVideo() {
        return mediaType == MEDIA_TYPE_VIDEO;
    }
}