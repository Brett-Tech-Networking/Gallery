package com.bretttech.gallery.ui.pictures;

import android.net.Uri;

public class Image {
    private final Uri uri;

    public Image(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }
}

