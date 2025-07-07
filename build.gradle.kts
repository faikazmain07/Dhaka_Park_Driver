// Top-level build file

// Define repositories for the build script itself
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Explicitly define the classpath for the safeargs plugin
        // NOTE: This line references a hardcoded version. We removed the safeargs problem earlier.
        // Let's remove this buildscript block if it's still causing problems with other plugins.
        // Based on your current error, the safeargs plugin itself is not the problem source.
        // Let's try removing this buildscript block entirely for a cleaner setup if possible.
        // For now, let's keep it as is from previous instructions, but focus on the main plugin conflict.
    }
}

plugins {
    // --- FIXED: Reference version from libs.versions.toml ---
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false

}