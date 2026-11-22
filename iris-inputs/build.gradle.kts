plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.irisui.inputs"
}

dependencies {
    api(project(":iris-core"))
    api(project(":iris-effects"))
    implementation(project(":iris-buttons"))
}
