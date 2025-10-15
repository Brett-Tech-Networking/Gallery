package com.bretttech.gallery.ui.pictures;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public class Image implements Parcelable {
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 3;

    private final Uri uri;
    private final int mediaType;
    private final String displayName; // Added for filename
    private final long dateAdded;     // Added for date

    public Image(Uri uri, int mediaType, String displayName, long dateAdded) {
        this.uri = uri;
        this.mediaType = mediaType;
        this.displayName = displayName;
        this.dateAdded = dateAdded;
    }

    protected Image(Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        mediaType = in.readInt();
        displayName = in.readString();
        dateAdded = in.readLong();
    }

    public static final Creator<Image> CREATOR = new Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel in) {
            return new Image(in);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

    public Uri getUri() {
        return uri;
    }

    public boolean isVideo() {
        return mediaType == MEDIA_TYPE_VIDEO;
    }

    public int getMediaType() {
        return mediaType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getDateAdded() {
        return dateAdded;
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeInt(mediaType);
        dest.writeString(displayName);
        dest.writeLong(dateAdded);
    }
}