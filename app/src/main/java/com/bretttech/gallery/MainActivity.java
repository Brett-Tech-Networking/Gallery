package com.bretttech.gallery;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bretttech.gallery.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // This listener programmatically adds padding to the fragment container
        // to avoid content drawing under the system's status bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragmentActivityMain, (v, windowInsets) -> {
            int insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, insets, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });


        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_pictures, R.id.navigation_albums, R.id.navigation_menu)
                .build();

        // This is the new, robust way to get the NavController.
        // It retrieves the NavHostFragment first and then gets its controller.
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }
}

