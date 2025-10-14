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
import com.bretttech.gallery.data.AlbumVisibilityManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;

// MODIFIED: Change base class to BottomSheetDialogFragment
public class HiddenAlbumsFragment extends BottomSheetDialogFragment implements HiddenAlbumsAdapter.OnAlbumToggleListener {

    private RecyclerView recyclerView;
    private AlbumsViewModel albumsViewModel;
    private HiddenAlbumsAdapter adapter;
    private AlbumVisibilityManager visibilityManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use requireActivity() to get the shared ViewModel instance
        albumsViewModel = new ViewModelProvider(requireActivity()).get(AlbumsViewModel.class);
        visibilityManager = new AlbumVisibilityManager(requireContext());
    }

    // NEW: Override onStart to set the max height of the bottom sheet
    @Override
    public void onStart() {
        super.onStart();

        // Get screen height
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (getActivity() != null) {
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        }
        int height = displayMetrics.heightPixels;
        int maxHeight = height / 2; // Half the screen height

        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();

            // Get the container of the bottom sheet
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // Set the maximum height using LayoutParams
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                if (layoutParams == null) {
                    layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight);
                } else {
                    layoutParams.height = maxHeight;
                }
                bottomSheet.setLayoutParams(layoutParams);

                // Ensure the content fits the constrained height and is scrollable
                bottomSheet.requestLayout();
            }
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // The fragment's root view. We will manage its containment height in onStart().
        View root = inflater.inflate(R.layout.fragment_hidden_albums, container, false);
        recyclerView = root.findViewById(R.id.recycler_view_hidden_albums);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Note: The title is added in the layout, no need to reference it here.
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the secure folder path for filtering out secure albums in the adapter
        String secureFolderPath = new File(requireContext().getFilesDir(), "secure").getAbsolutePath();

        adapter = new HiddenAlbumsAdapter(visibilityManager.getHiddenAlbumPaths(), secureFolderPath, this);
        recyclerView.setAdapter(adapter);

        // Subscribe to the UNFILTERED list of albums from the ViewModel
        albumsViewModel.getAllAlbumsUnfiltered().observe(getViewLifecycleOwner(), albums -> {
            adapter.setAlbums(albums, visibilityManager.getHiddenAlbumPaths());
        });

        // Ensure the ViewModel fetches the latest list
        albumsViewModel.loadAlbums();
    }

    @Override
    public void onAlbumToggled(String albumPath, boolean isHidden) {
        // The ViewModel will update the visibility status and then reload/re-sort the main album list
        albumsViewModel.setAlbumVisibility(albumPath, isHidden);
    }
}