import com.android.build.api.dsl.LibraryExtension

// Top-level build file. Shared configuration for every library module lives here so each
// module's own build.gradle.kts only has to declare its namespace and its dependencies.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

subprojects {
    plugins.withId("com.android.library") {
        apply(plugin = "org.jetbrains.kotlin.plugin.compose")

        extensions.configure<LibraryExtension> {
            compileSdk = 37
            buildToolsVersion = "37.0.0"

            defaultConfig {
                minSdk = 26
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            buildFeatures {
                compose = true
            }
        }

        dependencies {
            add("api", platform(libs.androidx.compose.bom))
            add("api", libs.compose.runtime)
            add("api", libs.compose.ui)
            add("api", libs.compose.ui.graphics)
            add("api", libs.compose.foundation)
            add("api", libs.compose.animation)
        }
    }
}
