package com.bretttech.gallery;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

// This is a required class for Glide to work optimally.
// The @GlideModule annotation is what tells Glide to use this class for configuration.
@GlideModule
public final class MyAppGlideModule extends AppGlideModule {
    // We can leave this class empty for now.
    // Its presence is enough to improve Glide's performance and remove the warning.
}