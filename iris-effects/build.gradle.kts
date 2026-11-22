plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.irisui.effects"
}

dependencies {
    api(project(":iris-core"))
}
