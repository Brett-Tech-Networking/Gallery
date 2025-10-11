package com.bretttech.gallery;

import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bretttech.gallery.ui.pictures.Image;

import java.util.List;
import java.util.stream.Collectors;

public class PhotoViewActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_POSITION = "extra_image_position";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        ViewPager2 viewPager = findViewById(R.id.view_pager);

        List<Image> images = ImageDataHolder.getInstance().getImageList();
        int currentPosition = getIntent().getIntExtra(EXTRA_IMAGE_POSITION, 0);

        if (images != null && !images.isEmpty()) {
            List<Uri> imageUris = images.stream().map(Image::getUri).collect(Collectors.toList());
            PhotoPagerAdapter adapter = new PhotoPagerAdapter(this, imageUris);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(currentPosition, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear the static reference to avoid memory leaks
        ImageDataHolder.getInstance().setImageList(null);
    }
}