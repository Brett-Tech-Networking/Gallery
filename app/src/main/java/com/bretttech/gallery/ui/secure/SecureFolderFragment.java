package com.bretttech.gallery.ui.secure;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bretttech.gallery.R;
import com.bretttech.gallery.auth.BiometricAuthManager;
import com.bretttech.gallery.databinding.FragmentSecureFolderBinding;
import com.bretttech.gallery.ui.albums.Album;
import com.bretttech.gallery.ui.albums.AlbumsAdapter;

public class SecureFolderFragment extends Fragment {

    private FragmentSecureFolderBinding binding;
    private SecureFolderViewModel secureFolderViewModel;
    private AlbumsAdapter albumsAdapter;
    private NavController navController;

    private final ActivityResultLauncher<Intent> pinSetupLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    authenticate();
                } else {
                    navController.popBackStack();
                }
            });

    private final ActivityResultLauncher<Intent> pinEntryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    loadContent();
                } else {
                    navController.popBackStack();
                }
            });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSecureFolderBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);

        setupRecyclerView();

        secureFolderViewModel = new ViewModelProvider(this).get(SecureFolderViewModel.class);
        secureFolderViewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> {
            albumsAdapter.setAlbums(albums);
        });

        authenticate();

        return binding.getRoot();
    }

    private void authenticate() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("secure_folder_prefs", Context.MODE_PRIVATE);
        if (prefs.contains("pin_hash")) {
            pinEntryLauncher.launch(new Intent(getContext(), PinEntryActivity.class));
            return;
        }

        new BiometricAuthManager((AppCompatActivity) requireActivity(), new BiometricAuthManager.BiometricAuthListener() {
            @Override
            public void onAuthSuccess() {
                Toast.makeText(getContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                loadContent();
            }

            @Override
            public void onAuthError(String errString) {
                Toast.makeText(getContext(), errString, Toast.LENGTH_SHORT).show();
                navController.popBackStack();
            }

            @Override
            public void onAuthFailed() {
                Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoSecurityEnrolled() {
                pinSetupLauncher.launch(new Intent(getContext(), PinSetupActivity.class));
            }
        }).authenticate();
    }

    private void loadContent() {
        secureFolderViewModel.loadAlbumsFromSecureFolder();
    }

    private void setupRecyclerView() {
        albumsAdapter = new AlbumsAdapter(null, this::openAlbum);
        binding.recyclerViewSecureAlbums.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recyclerViewSecureAlbums.setAdapter(albumsAdapter);
    }

    private void openAlbum(Album album) {
        Bundle bundle = new Bundle();
        bundle.putString("albumFolderPath", album.getFolderPath());
        navController.navigate(R.id.action_secureFolderFragment_to_secureAlbumDetailFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}