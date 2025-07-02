// In build.gradle.kts (app)

// Step 1: Add code to read from local.properties
import java.util.Properties

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}


plugins {
    id("com.google.gms.google-services")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
    // Import the Firebase BoM (Bill of Materials) - manages versions for all Firebase libraries
    // Using a recent, stable version of the BoM.
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))

    // Declare the Firebase library dependencies WITHOUT specifying versions.
    // The BoM will handle selecting the correct, compatible versions for you.
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // AndroidX libraries (from your libs.versions.toml file)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material) // This already includes the Material Design components
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Google Play Services for Maps and Location (these are separate from Firebase BoM)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0") // Updated to a recent stable version

    // Glide for image loading (this is fine as is)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // REMOVED: The separate Material library declaration as it's redundant.
    // implementation("com.google.android.material:material:1.11.0")

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}