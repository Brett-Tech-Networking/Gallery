package com.bretttech.gallery.ui.albums;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bretttech.gallery.databinding.FragmentAlbumsBinding;

public class AlbumsFragment extends Fragment {

    private FragmentAlbumsBinding binding;
    private AlbumsViewModel albumsViewModel;
    private AlbumsAdapter albumsAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        albumsViewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);

        binding = FragmentAlbumsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();

        albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> {
            if (albums != null) {
                albumsAdapter.setAlbums(albums);
            }
        });

        return root;
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewAlbums;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columns for albums
        albumsAdapter = new AlbumsAdapter();
        recyclerView.setAdapter(albumsAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
