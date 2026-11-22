plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.irisui.navigation"
}

dependencies {
    api(project(":iris-core"))
    api(project(":iris-effects"))
    api(project(":iris-buttons"))
    // api: BackEventCompat is part of the public API (BackEventCompat.travelFromEdge), apps that build their own back
    // animation need it on their compile classpath.
    api(libs.androidx.activity.compose)
}
