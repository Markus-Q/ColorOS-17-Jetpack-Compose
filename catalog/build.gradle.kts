plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.irisui.catalog"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "dev.irisui.catalog"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.1-alpha"
        androidResources.localeFilters += arrayOf("en")
    }

    // The release key comes from the environment (the release workflow sets it from GitHub secrets). Without it the
    // release build is signed with the debug key, so that it still installs.
    val releaseKeystore = System.getenv("RELEASE_KEYSTORE_FILE")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    lint {
        // The optimized build is meant for trying the kit out; a lint warning must not stop it.
        checkReleaseBuilds = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += arrayOf("META-INF/*.version", "META-INF/**/LICENSE.txt")
        }
    }
}

dependencies {
    implementation(project(":iris-ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}
