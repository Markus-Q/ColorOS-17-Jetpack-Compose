plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.irisui.popups"
}

dependencies {
    api(project(":iris-core"))
    api(project(":iris-effects"))
    api(project(":iris-buttons"))
    implementation(libs.androidx.activity.compose)
}
