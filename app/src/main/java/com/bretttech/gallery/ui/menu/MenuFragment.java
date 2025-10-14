package com.bretttech.gallery.ui.menu;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.bretttech.gallery.R;
import com.bretttech.gallery.SettingsActivity;
import com.bretttech.gallery.databinding.FragmentMenuBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MenuFragment extends BottomSheetDialogFragment {

    private FragmentMenuBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = NavHostFragment.findNavController(this);

        binding.menuPopup.menuSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
            dismiss();
        });

        binding.menuPopup.menuTrashBin.setOnClickListener(v -> {
            navController.navigate(R.id.action_navigation_menu_to_trashFragment);
        });

        binding.menuPopup.menuSecureFolder.setOnClickListener(v -> {
            navController.navigate(R.id.action_navigation_menu_to_secureFolderFragment);
        });

        binding.menuPopup.menuHideAlbums.setOnClickListener(v -> {
            navController.navigate(R.id.action_navigation_menu_to_hiddenAlbumsFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}