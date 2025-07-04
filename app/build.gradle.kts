// In build.gradle.kts (app)

// Step 1: Add code to read from local.properties
import java.util.Properties

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}


// App-level build file
plugins {
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs.kotlin) // Use the alias
}

android {
    namespace = "com.example.dhakaparkdriver"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dhakaparkdriver"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Step 2: Create a placeholder for the API key in the manifest
        manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY") ?: ""
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Import a specific, stable Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))

    // Declare Firebase library dependencies WITHOUT versions
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // AndroidX libraries (from libs.versions.toml) - These are usually fine
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // --- KEY CHANGE: Manually specify Material and Play Services versions ---
    // We will use specific versions known for stability instead of relying on defaults.
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Glide for image loading (this is an independent library, its version is fine)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // REMOVED: implementation(libs.material) to avoid potential conflict with the manual version.
}