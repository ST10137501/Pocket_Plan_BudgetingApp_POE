// Top-level build file
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0") // Required for Firebase
    }
}

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
}
