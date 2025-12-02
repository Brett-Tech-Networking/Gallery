package com.bretttech.gallery.ui.albums;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;
import com.bretttech.gallery.ui.pictures.PicturesAdapter;

public class ChangeCoverActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_PATH = "album_path";
    public static final String RESULT_COVER_URI = "cover_uri";
    public static final String RESULT_COVER_MEDIA_TYPE = "cover_media_type"; // NEW

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_cover);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Select New Cover Photo");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String albumPath = getIntent().getStringExtra(EXTRA_ALBUM_PATH);
        if (albumPath == null) {
            finish();
            return;
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view_change_cover);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        AlbumDetailViewModel viewModel = new ViewModelProvider(this).get(AlbumDetailViewModel.class);
        viewModel.loadImagesFromAlbum(albumPath, 0);
        viewModel.getImages().observe(this, images -> {
            PicturesAdapter adapter = new PicturesAdapter(image -> {
                Intent resultIntent = new Intent();
                resultIntent.setData(image.getUri());
                // MODIFIED: Add media type to the result Intent
                resultIntent.putExtra(RESULT_COVER_MEDIA_TYPE, image.getMediaType());
                setResult(RESULT_OK, resultIntent);
                finish();
            }, null);
            adapter.setImages(images);
            recyclerView.setAdapter(adapter);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}