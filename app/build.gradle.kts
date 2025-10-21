plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.navigation.safeargs)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bretttech.gallery"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bretttech.gallery"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.androidx.lifecycle.process)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Add the Glide library
    implementation(libs.glide)
    ksp(libs.glide.ksp) // Use the correct KSP dependency

    // Add pinch zoom in gallery
    implementation(libs.photoview)
    implementation(libs.ucrop)
    implementation(libs.photoeditor)

    // Add Biometric library
    implementation(libs.androidx.biometric)

    // Add GSON library for Favorites feature
    implementation("com.google.code.gson:gson:2.8.9")

    implementation("com.github.Dhaval2404:ColorPicker:2.3")
}