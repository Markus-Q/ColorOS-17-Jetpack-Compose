pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "IrisUI"

// Library modules (each one can be consumed on its own by other apps)
include(":iris-core")        // theme, colors, typography, shapes, motion, icons, overlay host
include(":iris-effects")     // blur backdrop, press, haptics, touch glow, persistent glow, drag physics
include(":iris-navigation")  // bottom nav pill, top bar, large title, segmented tabs, page transitions
include(":iris-buttons")     // primary, secondary, icon, round plus, text
include(":iris-inputs")      // switch, text field, search bar, checkbox, slider
include(":iris-content")     // card, list item, grouped list, section header, divider
include(":iris-popups")      // dialog, bottom sheet, popup menu
include(":iris-ui")          // umbrella: depends on every module above

// Catalog app (demo of every component, mirrors the layout of the reference catalog)
include(":catalog")
