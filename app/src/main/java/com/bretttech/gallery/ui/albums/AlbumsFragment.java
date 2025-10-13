package com.bretttech.gallery.ui.albums;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.bretttech.gallery.ui.albums.AlbumsViewModel.SortOrder; // NEW IMPORT

import java.util.ArrayList;

public class AlbumsFragment extends Fragment {

    private AlbumsViewModel albumsViewModel;
    private AlbumsAdapter albumsAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // NEW: Enable options menu for this fragment
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_albums, container, false);

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view_albums);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        albumsAdapter = new AlbumsAdapter(new ArrayList<>(), this::openAlbum);
        recyclerView.setAdapter(albumsAdapter);

        albumsViewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);
        albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> albumsAdapter.setAlbums(albums));

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        albumsViewModel.loadAlbums();
    }

    // NEW: Inflate the new menu for the action bar
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.albums_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // NEW: Handle clicks on the menu items
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // Handle sort options
        if (itemId == R.id.sort_date_desc) {
            albumsViewModel.sortAlbums(SortOrder.DATE_DESC);
            item.setChecked(true);
            return true;
        } else if (itemId == R.id.sort_date_asc) {
            albumsViewModel.sortAlbums(SortOrder.DATE_ASC);
            item.setChecked(true);
            return true;
        } else if (itemId == R.id.sort_name_asc) {
            albumsViewModel.sortAlbums(SortOrder.NAME_ASC);
            item.setChecked(true);
            return true;
        } else if (itemId == R.id.sort_name_desc) {
            albumsViewModel.sortAlbums(SortOrder.NAME_DESC);
            item.setChecked(true);
            return true;
        } else if (itemId == R.id.sort_count_desc) {
            albumsViewModel.sortAlbums(SortOrder.COUNT_DESC);
            item.setChecked(true);
            return true;
        } else if (itemId == R.id.sort_count_asc) {
            albumsViewModel.sortAlbums(SortOrder.COUNT_ASC);
            item.setChecked(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Note: onPrepareOptionsMenu could be used here to dynamically check the current sort order item.
    // However, since the default is DATE_DESC and it is set to checked in the XML, this is sufficient for initial state.

    private void openAlbum(Album album) {
        NavController navController = NavHostFragment.findNavController(this);
        Bundle bundle = new Bundle();
        bundle.putString("albumFolderPath", album.getFolderPath());
        navController.navigate(R.id.action_navigation_albums_to_albumDetailFragment, bundle);
    }
}