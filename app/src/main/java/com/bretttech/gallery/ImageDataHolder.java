package com.bretttech.gallery;

import com.bretttech.gallery.ui.pictures.Image;

import java.util.List;

public class ImageDataHolder {
    private static ImageDataHolder instance;
    private List<Image> imageList;

    public static ImageDataHolder getInstance() {
        if (instance == null) {
            instance = new ImageDataHolder();
        }
        return instance;
    }

    public void setImageList(List<Image> imageList) {
        this.imageList = imageList;
    }

    public List<Image> getImageList() {
        return imageList;
    }
}