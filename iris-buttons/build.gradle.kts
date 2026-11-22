plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.irisui.buttons"
}

dependencies {
    api(project(":iris-core"))
    api(project(":iris-effects"))
}
