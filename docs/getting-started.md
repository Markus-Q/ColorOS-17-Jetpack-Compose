# Getting started

## Requirements

| | |
| --- | --- |
| Android | `minSdk` 26. Real blur needs Android 12 (API 31), the finger-following back swipe Android 16 (API 36); older versions get the fallbacks listed [below](#what-needs-which-android-version) |
| Build | JDK 17 or newer to run Gradle (Android Studio's bundled JBR works), Android SDK platform 37 (`compileSdk 37`) |
| Versions of this repository | Android Gradle plugin 9.3.2, Kotlin 2.4.21, Compose BOM 2026.09.00, activity-compose 1.13.0, core-ktx 1.19.0 |

The kit is written for edge-to-edge apps: the scaffolds draw under the status bar and the bottom pill floats over the
content. Call `enableEdgeToEdge()` in the activity.

## Adding the kit to a project

The modules are not published to a Maven repository. Copy the `iris-*` folders into your project (or add this
repository as a Git submodule), include them in `settings.gradle.kts` and depend on the umbrella module:

```kotlin
// settings.gradle.kts
include(
    ":iris-core", ":iris-effects", ":iris-navigation", ":iris-buttons",
    ":iris-inputs", ":iris-content", ":iris-popups", ":iris-ui",
)
```

```kotlin
// build.gradle.kts of your app
dependencies {
    implementation(project(":iris-ui"))   // everything; or single modules such as ":iris-buttons"
}
```

The modules share their build settings through the root project of this repository, so take two things with them:

* the `subprojects { ... }` block of the root [build.gradle.kts](../build.gradle.kts) (it sets `compileSdk`, `minSdk 26`, the
  Java 17 target, applies the Compose compiler plugin and adds the Compose BOM and libraries as `api` dependencies), and
* the entries of [gradle/libs.versions.toml](../gradle/libs.versions.toml) the modules refer to (`android-library`,
  `kotlin-compose`, `androidx-compose-bom`, `compose-*`, `androidx-activity-compose`, `androidx-core-ktx`).

Which module needs which is shown in the [API reference](api/README.md#modules). `iris-core` has no dependencies,
everything else builds on `iris-core` and `iris-effects`.

## A first screen

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            IrisApp {                                   // theme, haptics, popup layer, overscroll
                var selected by remember { mutableIntStateOf(0) }
                val items = listOf(NavItem("Home", IrisIcons.Home), NavItem("Explore", IrisIcons.Search))
                val listState = rememberLazyListState()
                IrisLargeTitleScaffold(
                    title = "Settings",
                    scrollOffset = listState.titleScrollOffset(),
                    bottomBar = { IrisBottomNavPill(items, selected, { selected = it }) },
                    floatingActionButton = { IrisRoundPlusButton(onClick = { }) },
                ) { padding ->
                    LazyColumn(state = listState, contentPadding = padding) {
                        item { LargeTitle() }
                        item { IrisGroupedList { item { IrisListItem("Wi-Fi", showChevron = true) } } }
                    }
                }
            }
        }
    }
}
```

`IrisApp` is the one root you need: it provides the theme ([Theming](theming.md)), the haptics engine
([Touch](touch-and-drag.md#haptics)), the layer that dialogs, sheets and menus are drawn in ([Popups](popups.md)) and the
ColorOS bounce at the end of lists. It also tells the system bars which icon color to use. Its parameters:

| Parameter | Meaning |
| --- | --- |
| `darkTheme` | Defaults to the system setting |
| `colors` | A `IrisColors`; defaults to the dark or the light palette |
| `overscrollFactory` | `IrisOverscrollFactory` (default), `null` for no overscroll at all, or any other factory |

For several screens with the ColorOS push and pop animation and the back swipe, put the screens in a `IrisPageHost`
([Navigation](navigation.md)). What the kit offers, grouped by what you build, is in the [catalog tour](catalog.md).

## What needs which Android version

| Feature | Needs | Below that |
| --- | --- | --- |
| Blur of glass surfaces (bottom pill, popups, dialogs, sheets, top bar) | API 31 (`RenderEffect`) | The material is drawn as the flat color it has over a black page (`GlassStyle.fallbackBackdrop`), see [Glass and blur](glass-and-blur.md#when-nothing-can-be-blurred) |
| Page that follows the finger during the back swipe | API 36 | Before Android 16 the system always sends the progress itself; `IrisBackGestureTracking` does nothing there |
| Haptic events | A vibrator; predefined `VibrationEffect`s need API 29 | No vibration |

Nothing in the manifest has to be changed: the manifest of `iris-navigation` brings
`android:enableOnBackInvokedCallback="true"`, which the system needs before it sends a back animation any progress (see
[Navigation](navigation.md#the-back-swipe)).

## The catalog app

```powershell
.\gradlew :catalog:installDebug
```

installs the catalog on a connected device. Every screen can be opened directly, which is handy to look at one component:

```powershell
adb shell am start -n dev.irisui.catalog/.MainActivity --es destination Slider
```

| Extra | Effect |
| --- | --- |
| `--es destination NAME` | Opens that screen (the entries of `CatalogDestination`, listed in the [catalog tour](catalog.md)) |
| `--ez light true` | Light theme whatever the system theme is (`false` forces the dark one) |
| `--ez touches true` | Draws a circle under the finger, for screen recordings |

The catalog is also the best source of usage examples: every demo is a short file in
[catalog/.../demos](../catalog/src/main/kotlin/dev/irisui/catalog/demos), and the [catalog tour](catalog.md) links each
screen to the code that builds it.
