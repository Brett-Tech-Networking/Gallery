package com.bretttech.gallery.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bretttech.gallery.ImageDataHolder;
import com.bretttech.gallery.PhotoViewActivity;
import com.bretttech.gallery.VideoPlayerActivity;
import com.bretttech.gallery.databinding.FragmentFavoritesBinding;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.ui.pictures.PicturesAdapter;

import java.util.List;

public class FavoritesFragment extends Fragment {

    private FragmentFavoritesBinding binding;
    private FavoritesViewModel favoritesViewModel;
    private PicturesAdapter picturesAdapter;
    private List<Image> favoriteImages;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        setupRecyclerView();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        favoritesViewModel.getFavoriteImages().observe(getViewLifecycleOwner(), images -> {
            this.favoriteImages = images;
            picturesAdapter.setImages(images);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        favoritesViewModel.loadFavoriteImages();
    }

    private void setupRecyclerView() {
        picturesAdapter = new PicturesAdapter(
                image -> {
                    if (image.isVideo()) {
                        Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
                        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, image.getUri());
                        startActivity(intent);
                    } else {
                        ImageDataHolder.getInstance().setImageList(favoriteImages);
                        Intent intent = new Intent(getContext(), PhotoViewActivity.class);
                        intent.putExtra(PhotoViewActivity.EXTRA_IMAGE_POSITION, favoriteImages.indexOf(image));
                        startActivity(intent);
                    }
                },
                image -> {
                    // Long click not implemented for favorites
                }
        );
        binding.recyclerViewFavorites.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.recyclerViewFavorites.setAdapter(picturesAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}