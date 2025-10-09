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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bretttech.gallery.PhotoViewActivity;
import com.bretttech.gallery.R;
import com.bretttech.gallery.databinding.FragmentAlbumDetailBinding;
import com.bretttech.gallery.ui.pictures.PicturesAdapter;

public class AlbumDetailFragment extends Fragment {

    private FragmentAlbumDetailBinding binding;
    private AlbumDetailViewModel viewModel;
    private PicturesAdapter picturesAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAlbumDetailBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(AlbumDetailViewModel.class);

        setupRecyclerView();

        String albumName = AlbumDetailFragmentArgs.fromBundle(getArguments()).getAlbumName();
        viewModel.loadImagesFromAlbum(albumName);

        // Set the title of the action bar
        if (((AppCompatActivity) getActivity()) != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(albumName);
        }


        viewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            picturesAdapter.setImages(images);
        });

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        picturesAdapter = new PicturesAdapter(image -> {
            Intent intent = new Intent(getContext(), PhotoViewActivity.class);
            intent.putExtra(PhotoViewActivity.EXTRA_IMAGE_URI, image.getUri().toString());
            startActivity(intent);
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
