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
        versionCode = 1
        versionName = "1.0.0"
        androidResources.localeFilters += arrayOf("en")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // The demo app is signed with the debug key so that the release build installs without a keystore.
            signingConfig = signingConfigs.getByName("debug")
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
