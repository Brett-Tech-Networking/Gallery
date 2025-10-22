package com.bretttech.gallery;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bretttech.gallery.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this,
                            "Permission denied. Albums and pictures will not load.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragmentActivityMain,
                (v, windowInsets) -> {
                    int insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                    v.setPadding(0, insets, 0, 0);
                    return WindowInsetsCompat.CONSUMED;
                });

        checkStoragePermission();

        BottomNavigationView navView = binding.navView;
        applyBottomNavColor(navView);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_pictures, R.id.navigation_albums, R.id.navigation_menu)
                .build();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        navController = navHostFragment.getNavController();

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() != R.id.secureFolderFragment && destination.getId() != R.id.secureAlbumDetailFragment) {
                GalleryApplication.isSecureFolderUnlocked = false;
            }
        });

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    private void applyBottomNavColor(BottomNavigationView navView) {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        try {
            int color = prefs.getInt(SettingsActivity.KEY_BOTTOM_NAV_COLOR, 0);
            if (color != 0) {
                navView.setItemIconTintList(ColorStateList.valueOf(color));
                navView.setItemTextColor(ColorStateList.valueOf(color));
            }
        } catch (ClassCastException e) {
            prefs.edit().remove(SettingsActivity.KEY_BOTTOM_NAV_COLOR).apply();
        }
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        } else {
            String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}