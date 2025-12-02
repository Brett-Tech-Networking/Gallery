package com.bretttech.gallery.ui.albums;

import android.net.Uri;
import com.bretttech.gallery.ui.pictures.Image;

public class Album {
    private final String name;
    public Uri coverImageUri; // Made public for easier modification
    private int imageCount;
    private final String folderPath;
    private final long bucketId; // NEW: Store bucket ID
    private int coverMediaType;
    private final long dateAdded;
    private long cacheBusterId = 0;

    public Album(String name, Uri coverImageUri, int imageCount, String folderPath, long bucketId, int coverMediaType,
            long dateAdded) {
        this.name = name;
        this.coverImageUri = coverImageUri;
        this.imageCount = imageCount;
        this.folderPath = folderPath;
        this.bucketId = bucketId;
        this.coverMediaType = coverMediaType;
        this.dateAdded = dateAdded;
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

    public long getBucketId() {
        return bucketId;
    }

    public int getCoverMediaType() {
        return coverMediaType;
    }

    // NEW: Getter for cache buster ID
    public long getCacheBusterId() {
        return cacheBusterId;
    }

    // NEW: Setter for cache buster ID
    public void setCacheBusterId(long cacheBusterId) {
        this.cacheBusterId = cacheBusterId;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public boolean isCoverVideo() {
        return coverMediaType == Image.MEDIA_TYPE_VIDEO;
    }

    public void incrementImageCount() {
        this.imageCount++;
    }

    public void setCoverImageUri(Uri uri) {
        this.coverImageUri = uri;
    }

    public void setCoverMediaType(int mediaType) {
        this.coverMediaType = mediaType;
    }
}