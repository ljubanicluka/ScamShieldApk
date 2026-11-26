plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "rs.scamshield.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "rs.scamshield.app"
        minSdk = 24
        targetSdk = 36
        versionCode = providers.gradleProperty("versionCode").orElse("1").get().toInt()
        versionName = providers.gradleProperty("versionName").orElse("1.0").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // A persistent key from GitHub Secrets allows updates without uninstalling.
    // Without it, CI uses the runner debug key for test builds only.
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_FILE")
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (System.getenv("KEYSTORE_FILE") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.webkit:webkit:1.11.0")

    // Native QR camera preview + real-time frame analysis.
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")

    // Bundled model: QR decoding works offline immediately after installation.
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("androidx.activity:activity-ktx:1.13.0")

    // JVM unit tests (QrSampling, QrStrings) and device tests (QrImageDecoder with real ML Kit).
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}