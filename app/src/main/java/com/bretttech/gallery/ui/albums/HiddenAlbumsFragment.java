package com.bretttech.gallery.ui.albums;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bretttech.gallery.R;
import com.bretttech.gallery.SharedViewModel; // NEW IMPORT
import com.bretttech.gallery.data.AlbumVisibilityManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HiddenAlbumsFragment extends BottomSheetDialogFragment implements HiddenAlbumsAdapter.OnAlbumToggleListener {

    private RecyclerView recyclerView;
    private AlbumsViewModel albumsViewModel;
    private HiddenAlbumsAdapter adapter;
    private AlbumVisibilityManager visibilityManager;
    private SharedViewModel sharedViewModel; // NEW FIELD

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        albumsViewModel = new ViewModelProvider(requireActivity()).get(AlbumsViewModel.class);
        visibilityManager = new AlbumVisibilityManager(requireContext());
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class); // NEW INIT
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hidden_albums, container, false);
        recyclerView = root.findViewById(R.id.recycler_view_hidden_albums);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String secureFolderPath = new File(requireContext().getFilesDir(), "secure").getAbsolutePath();

        adapter = new HiddenAlbumsAdapter(visibilityManager.getHiddenAlbumPaths(), secureFolderPath, this);
        recyclerView.setAdapter(adapter);

        albumsViewModel.getAllAlbumsUnfiltered().observe(getViewLifecycleOwner(), albums -> {
            List<Album> publicAlbums = filterSecureAlbums(albums, secureFolderPath);

            adapter.setAlbums(albums, visibilityManager.getHiddenAlbumPaths());

            setDynamicSheetHeight(publicAlbums.size());
        });

        albumsViewModel.loadAlbums();
    }

    private List<Album> filterSecureAlbums(List<Album> allAlbums, String secureFolderPath) {
        List<Album> publicAlbums = new ArrayList<>();
        for (Album album : allAlbums) {
            if (!album.getFolderPath().contains(secureFolderPath)) {
                publicAlbums.add(album);
            }
        }
        return publicAlbums;
    }

    private void setDynamicSheetHeight(int albumCount) {
        if (getActivity() == null || getDialog() == null) return;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;

        int heightPercentage;
        if (albumCount >= 5) {
            heightPercentage = 50;
        } else if (albumCount >= 1) {
            heightPercentage = 25;
        } else {
            heightPercentage = 15;
        }

        int targetHeight = (int) (height * (heightPercentage / 100.0));

        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();

            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                if (layoutParams == null) {
                    layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, targetHeight);
                } else {
                    layoutParams.height = targetHeight;
                }
                bottomSheet.setLayoutParams(layoutParams);
                bottomSheet.requestLayout();
            }
        }
    }


    @Override
    public void onAlbumToggled(Album album, boolean isHidden, int position) {
        albumsViewModel.setAlbumVisibility(album.getFolderPath(), isHidden);

        // NEW LINE: Request a live refresh on the main Albums fragment (Fixes non-live update)
        sharedViewModel.requestRefresh();
    }
}