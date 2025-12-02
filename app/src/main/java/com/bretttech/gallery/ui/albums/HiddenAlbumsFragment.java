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
import com.bretttech.gallery.SharedViewModel; // REQUIRED IMPORT
import com.bretttech.gallery.data.AlbumVisibilityManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HiddenAlbumsFragment extends BottomSheetDialogFragment
        implements HiddenAlbumsAdapter.OnAlbumToggleListener {

    private RecyclerView recyclerView;
    private AlbumsViewModel albumsViewModel;
    private HiddenAlbumsAdapter adapter;
    private SharedViewModel sharedViewModel;
    private java.util.Set<String> currentHiddenPaths = new java.util.HashSet<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        albumsViewModel = new ViewModelProvider(requireActivity()).get(AlbumsViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class); // INIT SharedViewModel
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hidden_albums, container, false);
        recyclerView = root.findViewById(R.id.recycler_view_hidden_albums);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String secureFolderPath = new File(requireContext().getFilesDir(), "secure").getAbsolutePath();

        // Initialize with empty set, will be updated by LiveData
        adapter = new HiddenAlbumsAdapter(new java.util.HashSet<>(), secureFolderPath, this);
        recyclerView.setAdapter(adapter);

        android.widget.ImageView btnHideAll = view.findViewById(R.id.btn_hide_all);

        albumsViewModel.getHiddenAlbums().observe(getViewLifecycleOwner(), hiddenPaths -> {
            currentHiddenPaths = hiddenPaths;
            if (adapter != null) {
                adapter.updateHiddenPaths(hiddenPaths);
            }
            updateHideAllButtonState(btnHideAll, adapter != null ? adapter.getItemCount() : 0);
        });

        albumsViewModel.getAllAlbumsUnfiltered().observe(getViewLifecycleOwner(), albums -> {
            List<Album> publicAlbums = filterSecureAlbums(albums, secureFolderPath);

            adapter.setAlbums(albums, currentHiddenPaths);
            updateHideAllButtonState(btnHideAll, publicAlbums.size());

            setDynamicSheetHeight(publicAlbums.size());
        });

        btnHideAll.setOnClickListener(v -> {
            if (adapter == null || adapter.getItemCount() == 0)
                return;

            List<Album> albums = adapter.getAlbums();
            List<String> allPaths = new ArrayList<>();
            boolean anyVisible = false;

            for (Album album : albums) {
                allPaths.add(album.getFolderPath());
                if (!currentHiddenPaths.contains(album.getFolderPath())) {
                    anyVisible = true;
                }
            }

            // If any album is visible, we hide all. Otherwise (all are hidden), we unhide
            // all.
            boolean shouldHide = anyVisible;

            // Step 1: Trigger batch persistence and optimistic update
            albumsViewModel.setAllAlbumsVisibility(allPaths, shouldHide);

            // Step 2: Trigger refresh
            sharedViewModel.requestRefresh();
        });

        albumsViewModel.loadAlbums();
    }

    private void updateHideAllButtonState(android.widget.ImageView btn, int totalCount) {
        if (totalCount == 0) {
            btn.setVisibility(View.GONE);
            return;
        }
        btn.setVisibility(View.VISIBLE);

        if (adapter == null)
            return;

        List<Album> albums = adapter.getAlbums();
        boolean allHidden = true;
        for (Album album : albums) {
            if (!currentHiddenPaths.contains(album.getFolderPath())) {
                allHidden = false;
                break;
            }
        }

        // If all are hidden, show "Eye" (to unhide).
        // If some are visible, show "Eye Off" (to hide).
        btn.setImageResource(allHidden ? R.drawable.ic_visible : R.drawable.ic_hidden);
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
        if (getActivity() == null || getDialog() == null)
            return;

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
        // Step 1: Trigger synchronous persistence on a background thread.
        albumsViewModel.setAlbumVisibility(album.getFolderPath(), isHidden);

        // Step 2: Trigger the live refresh on the main Albums fragment immediately
        // (User's requirement).
        sharedViewModel.requestRefresh();
    }
}