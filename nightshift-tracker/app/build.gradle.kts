plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.nightshift.tracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nightshift.tracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "2.2.0"
        vectorDrawables { useSupportLibrary = true }
    }

    // Two apps from one codebase. They install side by side and share nothing
    // on-device (separate applicationIds, separate databases and backups).
    //
    // NOTE: no room.schemaLocation here. KSP arguments are global, not
    // per-flavor, so both flavors' KSP tasks resolved to the same schema file
    // and raced each other writing it ("Empty schema file", intermittently).
    // Schema export is off instead (see @Database) — every migration in this
    // project is hand-written and nothing consumes the exported JSON.
    flavorDimensions += "app"
    productFlavors {
        create("nightshift") {
            dimension = "app"
            resValue("string", "app_name", "Nightshift")
            buildConfigField("boolean", "URO", "false")
        }
        create("uroday") {
            dimension = "app"
            applicationIdSuffix = ".uro"
            resValue("string", "app_name", "UroDay")
            buildConfigField("boolean", "URO", "true")
        }
    }

    signingConfigs {
        create("shared") {
            // Committed keystore for a personal, sideloaded, never-on-a-store
            // app: every CI rebuild carries the same signature, so new builds
            // install straight over the old one without uninstalling.
            storeFile = rootProject.file("signing/nightshift.keystore")
            storePassword = "nightshift"
            keyAlias = "nightshift"
            keyPassword = "nightshift"
        }
    }

    buildTypes {
        release {
            // Minification off: nothing to shrink that matters, and it keeps
            // Gson reflection on the backup models guaranteed-safe.
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes +=
                setOf(
                    "META-INF/DEPENDENCIES",
                    "META-INF/INDEX.LIST",
                    "META-INF/LICENSE*",
                    "META-INF/NOTICE*",
                    "META-INF/*.md",
                )
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.google.code.gson:gson:2.11.0")
}
