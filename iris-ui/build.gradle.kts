plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.irisui.ui"
}

// Umbrella module: add this single dependency to get the whole kit.
dependencies {
    api(project(":iris-core"))
    api(project(":iris-effects"))
    api(project(":iris-navigation"))
    api(project(":iris-buttons"))
    api(project(":iris-inputs"))
    api(project(":iris-content"))
    api(project(":iris-popups"))
    implementation(libs.androidx.core.ktx)
}
