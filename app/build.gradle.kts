// App-level build file

// Block to read from local.properties (keep this for MAPS_API_KEY)
import java.util.Properties

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

plugins {
    id("com.google.gms.google-services") // Apply Google Services plugin
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
        // Placeholder for Google Maps API Key from local.properties
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
        viewBinding = true // Enable View Binding for all layouts
    }
}

dependencies {
    // Firebase Bill of Materials (BoM) - manages versions for all Firebase libraries
    implementation(platform("com.google.firebase:firebase-bom:32.8.1")) // Stable Firebase BoM version

    // Firebase Core libraries
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage") // For photo upload

    // AndroidX Core libraries (managed by libs.versions.toml)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material) // Material Design components
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0") // For DriverDashboard layout

    // Google Play Services for Maps and Location (explicit stable versions)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0") // For Google Sign-In

    // Glide for image loading and its annotation processor
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // QR Code Scanner (zxing-android-embedded)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}