package com.bretttech.gallery.ui.search;

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
import com.bretttech.gallery.databinding.FragmentSearchBinding;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.ui.pictures.PicturesAdapter;
import java.util.List;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private SearchViewModel searchViewModel;
    private PicturesAdapter picturesAdapter;
    private List<Image> searchResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        setupRecyclerView();

        String query = getArguments() != null ? getArguments().getString("query") : null;
        if (query != null) {
            searchViewModel.search(query);
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        searchViewModel.getSearchResults().observe(getViewLifecycleOwner(), images -> {
            this.searchResults = images;
            picturesAdapter.setImages(images);
        });
    }

    private void setupRecyclerView() {
        picturesAdapter = new PicturesAdapter(
                image -> {
                    if (image.isVideo()) {
                        Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
                        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, image.getUri());
                        startActivity(intent);
                    } else {
                        ImageDataHolder.getInstance().setImageList(searchResults);
                        Intent intent = new Intent(getContext(), PhotoViewActivity.class);
                        intent.putExtra(PhotoViewActivity.EXTRA_IMAGE_POSITION, searchResults.indexOf(image));
                        startActivity(intent);
                    }
                },
                image -> {
                    // Long click not implemented for search results
                }
        );
        binding.recyclerViewSearch.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.recyclerViewSearch.setAdapter(picturesAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}