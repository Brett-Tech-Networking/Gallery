package com.bretttech.gallery.ui.albums;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bretttech.gallery.ImageDataHolder;
import com.bretttech.gallery.PhotoViewActivity;
import com.bretttech.gallery.VideoPlayerActivity; // NEW IMPORT
import com.bretttech.gallery.databinding.FragmentAlbumDetailBinding;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.ui.pictures.PicturesAdapter;

import java.io.File;
import java.util.List;

public class AlbumDetailFragment extends Fragment {

    private FragmentAlbumDetailBinding binding;
    private AlbumDetailViewModel viewModel;
    private PicturesAdapter picturesAdapter;
    private List<Image> images;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAlbumDetailBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(AlbumDetailViewModel.class);

        setupRecyclerView();

        String albumFolderPath = getArguments() != null ? getArguments().getString("albumFolderPath") : null;
        if (albumFolderPath != null) {
            viewModel.loadImagesFromAlbum(albumFolderPath);

            if (((AppCompatActivity) getActivity()) != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                String albumName = new File(albumFolderPath).getName();
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(albumName);
            }
        }

        viewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            this.images = images;
            picturesAdapter.setImages(images);
        });

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        // UPDATED: Check media type to launch appropriate activity
        picturesAdapter = new PicturesAdapter(image -> {
            if (image.isVideo()) {
                Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, image.getUri());
                startActivity(intent);
            } else if (images != null && !images.isEmpty()) {
                // Original logic for images
                ImageDataHolder.getInstance().setImageList(images);
                Intent intent = new Intent(getContext(), PhotoViewActivity.class);
                intent.putExtra(PhotoViewActivity.EXTRA_IMAGE_POSITION, images.indexOf(image));
                startActivity(intent);
            }
        });
        binding.recyclerViewAlbumDetail.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.recyclerViewAlbumDetail.setAdapter(picturesAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}