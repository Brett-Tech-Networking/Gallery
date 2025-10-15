package com.bretttech.gallery.data;

import java.util.ArrayList;
import java.util.List;

public class ImageDetails {
    private List<String> tags;

    public ImageDetails() {
        this.tags = new ArrayList<>();
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }
}