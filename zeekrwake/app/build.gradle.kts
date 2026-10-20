plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "au.local.zeekrwake"
    compileSdk = 35

    defaultConfig {
        applicationId = "au.local.zeekrwake"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { buildConfig = true }

    // "plain" installs from a browser; "tap" declares an accessibility service, which
    // Play Protect will not let you sideload, so it needs adb install.
    flavorDimensions += "autotap"
    productFlavors {
        create("plain") { dimension = "autotap" }
        create("tap") {
            dimension = "autotap"
            applicationIdSuffix = ".tap"
        }
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
