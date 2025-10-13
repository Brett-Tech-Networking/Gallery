package com.bretttech.gallery.ui.pictures;

import android.net.Uri;
import android.provider.MediaStore;

import java.util.Objects;

public class Image {
    public static final int MEDIA_TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
    public static final int MEDIA_TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

    private final Uri uri;
    private final int mediaType;

    public Image(Uri uri, int mediaType) {
        this.uri = uri;
        this.mediaType = mediaType;
    }

    public Uri getUri() {
        return uri;
    }

    public int getMediaType() {
        return mediaType;
    }

    public boolean isVideo() {
        return mediaType == MEDIA_TYPE_VIDEO;
    }

    // --- NEW: equals() and hashCode() ---
    // This is crucial for correctly finding and removing items from lists.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return Objects.equals(uri, image.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }
}