package com.bretttech.gallery.ui.albums;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;

import java.util.ArrayList;

public class AlbumsFragment extends Fragment {

    private AlbumsViewModel albumsViewModel;
    private AlbumsAdapter albumsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_albums, container, false);

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view_albums);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        albumsAdapter = new AlbumsAdapter(new ArrayList<>(), album -> openAlbum(album));
        recyclerView.setAdapter(albumsAdapter);

        albumsViewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);
        albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> albumsAdapter.setAlbums(albums));

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh albums whenever this fragment is shown
        albumsViewModel.loadAlbums();
    }

    private void openAlbum(Album album) {
        NavController navController = NavHostFragment.findNavController(this);
        Bundle bundle = new Bundle();
        // Pass folderPath, not name, to strictly filter images
        bundle.putString("albumFolderPath", album.getFolderPath());
        navController.navigate(R.id.action_navigation_albums_to_albumDetailFragment, bundle);
    }
}
