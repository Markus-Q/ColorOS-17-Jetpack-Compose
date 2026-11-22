plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.irisui.content"
}

dependencies {
    api(project(":iris-core"))
    api(project(":iris-effects"))
}
