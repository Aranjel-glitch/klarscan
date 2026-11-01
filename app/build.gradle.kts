plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ch.klarscan"
    compileSdk = 34

    defaultConfig {
        applicationId = "ch.klarscan"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.biometric:biometric:1.1.0")

    // Google ML Kit: Dokumentenscanner (Kamera, Kantenerkennung, Filter, Schatten) läuft auf dem Gerät
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    // Texterkennung, Modell steckt in der App, funktioniert offline
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
}
