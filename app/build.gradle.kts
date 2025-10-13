plugins {
    alias(libs.plugins.android.application)
    // This correctly applies the Safe Args plugin to your app module
    alias(libs.plugins.androidx.navigation.safeargs)
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Add the Glide library for efficient image loading
    implementation(libs.glide)
    // Add pinch zoom in gallery
    implementation(libs.photoview)
    implementation(libs.ucrop)
    implementation(libs.photoeditor)
}

