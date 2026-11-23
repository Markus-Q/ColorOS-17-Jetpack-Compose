<p align="center"><img src="docs/media/logo.png" width="120" alt="Iris UI Kit logo"></p>

# Iris UI Kit (Jetpack Compose, ColorOS-style)

A modular Jetpack Compose component library in the style of **ColorOS 17** (matched on a
OnePlus 15, ColorOS 17.0.1 / Android 17), plus a **catalog app** that shows every component.
The project layout mirrors the reference catalog from
[Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass): library modules, then one
catalog app with a home list of sections and one screen per component. Only the *layout* was taken
as inspiration; no code or effects of that project are used.

Keywords: Iris, ColorOS, ColorOS 17, ColorOS 17 style, ColorOS 17 UI, ColorOS-style Jetpack Compose UI kit, Android UI library.

> Not affiliated with or endorsed by OPPO or OnePlus. ColorOS is a trademark of OPPO; the name is only used to describe
> the visual style this kit imitates. The repository contains no fonts, icons, images or other assets of OPPO.

**Try it:** download the catalog app (alpha, APK) from the [latest release](../../releases/latest) or [all releases](../../releases) and install it on an Android phone (Android 8+, blur needs Android 12+). Allow installs from this source when asked.

## Documentation

| | |
| --- | --- |
| [Getting started](docs/getting-started.md) | Requirements, adding the modules to a project, a first screen, running the catalog |
| [Catalog tour](docs/catalog.md) | Every screen of the catalog in the dark and the light theme, with the kit APIs it uses |
| [Recordings](docs/recordings.md) | A recording of every catalog screen, all on one page |
| [API reference](docs/api/README.md) | Every public declaration with its documentation and signature, generated from the source |
| Guides | [Theming](docs/theming.md) · [Touch, drag, haptics and overscroll](docs/touch-and-drag.md) · [Glass and blur](docs/glass-and-blur.md) · [Navigation](docs/navigation.md) · [Popups](docs/popups.md) · [FAQ](docs/faq.md) |

## Screenshots

Taken on the phone (OnePlus 15, ColorOS 17.0.1) from the catalog app: the same screens in the dark and in the light theme
(`--ez light true`). The status bar is cropped.

**Dark**

<table>
<tr>
<td align="center"><img src="docs/media/screens/home-dark.png" width="185" alt="Catalog home"><br><sub>Home</sub></td>
<td align="center"><img src="docs/media/screens/nav-pill-dark.png" width="185" alt="Bottom nav pill"><br><sub>Bottom nav pill</sub></td>
<td align="center"><img src="docs/media/screens/icon-button-dark.png" width="185" alt="Icon buttons"><br><sub>Icon buttons</sub></td>
<td align="center"><img src="docs/media/screens/slider-dark.png" width="185" alt="Sliders"><br><sub>Sliders</sub></td>
</tr>
<tr>
<td align="center"><img src="docs/media/screens/switch-dark.png" width="185" alt="Switches"><br><sub>Switches</sub></td>
<td align="center"><img src="docs/media/screens/popup-menu-dark.png" width="185" alt="Popup menu"><br><sub>Popup menu</sub></td>
<td align="center"><img src="docs/media/screens/dialog-dark.png" width="185" alt="Dialog"><br><sub>Dialog</sub></td>
<td align="center"><img src="docs/media/screens/bottom-sheet-dark.png" width="185" alt="Bottom sheet"><br><sub>Bottom sheet</sub></td>
</tr>
</table>

**Light**

<table>
<tr>
<td align="center"><img src="docs/media/screens/home-light.png" width="185" alt="Catalog home, light"><br><sub>Home</sub></td>
<td align="center"><img src="docs/media/screens/nav-pill-light.png" width="185" alt="Bottom nav pill, light"><br><sub>Bottom nav pill</sub></td>
<td align="center"><img src="docs/media/screens/icon-button-light.png" width="185" alt="Icon buttons, light"><br><sub>Icon buttons</sub></td>
<td align="center"><img src="docs/media/screens/slider-light.png" width="185" alt="Sliders, light"><br><sub>Sliders</sub></td>
</tr>
<tr>
<td align="center"><img src="docs/media/screens/switch-light.png" width="185" alt="Switches, light"><br><sub>Switches</sub></td>
<td align="center"><img src="docs/media/screens/popup-menu-light.png" width="185" alt="Popup menu, light"><br><sub>Popup menu</sub></td>
<td align="center"><img src="docs/media/screens/dialog-light.png" width="185" alt="Dialog, light"><br><sub>Dialog</sub></td>
<td align="center"><img src="docs/media/screens/bottom-sheet-light.png" width="185" alt="Bottom sheet, light"><br><sub>Bottom sheet</sub></td>
</tr>
</table>

## In motion

Recorded on the phone itself (dark mode). The grey circle under the finger is drawn by the catalog app
(`--ez touches true`); the system "show touches" setting was left alone. These are highlights: every screen of
the catalog has its own recording, see [Recordings](docs/recordings.md).

### Press, drag and spring

<table>
<tr>
<td align="center" valign="top"><img src="docs/media/drag-physics.gif" width="250" alt="Drag physics"><br><sub><b>Drag physics</b><br>shrinks while held, follows the finger with rubber-band resistance, springs back with an overshoot</sub></td>
<td align="center" valign="top"><img src="docs/media/press-animation.gif" width="250" alt="Press animation"><br><sub><b>Press animation</b><br>everything shrinks while held, small and wide things by different amounts</sub></td>
</tr>
<tr>
<td align="center" valign="top"><img src="docs/media/slider.gif" width="250" alt="Sliders"><br><sub><b>Sliders</b><br>a light follows the thumb; stepped sliders trail the finger and catch up at every dot</sub></td>
<td align="center" valign="top"><img src="docs/media/switch.gif" width="250" alt="Switches"><br><sub><b>Switches</b><br>hold to light the track, tap or drag the thumb; it stretches while it travels</sub></td>
<td align="center" valign="top"><img src="docs/media/segmented-tabs.gif" width="250" alt="Segmented tabs"><br><sub><b>Segmented tabs</b><br>tap a segment or slide the thumb</sub></td>
</tr>
<tr>
<td align="center" valign="top"><img src="docs/media/popup-menu.gif" width="250" alt="Popup menu"><br><sub><b>Popup menu</b><br>glass panel scaling out of the corner nearest its anchor, rows light up while pressed</sub></td>
<td align="center" valign="top"><img src="docs/media/checkbox.gif" width="250" alt="Checkbox"><br><sub><b>Checkbox</b><br>the tick draws itself, "select all" follows the rows</sub></td>
</tr>
<tr>
<td align="center" valign="top"><img src="docs/media/touch-glow.gif" width="250" alt="Touch glow"><br><sub><b>Touch glow</b><br>a light follows the finger and fades out</sub></td>
<td align="center" valign="top"><img src="docs/media/progress.gif" width="250" alt="Progress"><br><sub><b>Progress</b><br>bar, indeterminate bar, ring and spinner</sub></td>
</tr>
</table>

### Whole screens

<table>
<tr>
<td align="center" valign="top"><img src="docs/media/home-scroll.gif" width="190" alt="Scrolling and bounce"><br><sub><b>Scroll and bounce</b><br>ColorOS overscroll at both ends</sub></td>
<td align="center" valign="top"><img src="docs/media/large-title.gif" width="190" alt="Collapsing large title"><br><sub><b>Large title</b><br>collapses into the glass top bar while the list scrolls under it</sub></td>
<td align="center" valign="top"><img src="docs/media/page-back-swipe.gif" width="190" alt="Page transition and back swipe"><br><sub><b>Push and back swipe</b><br>the page follows the finger from the edge; a short swipe cancels, a long one pops (the edge swipe belongs to the system, so no circle)</sub></td>
<td align="center" valign="top"><img src="docs/media/bottom-nav-pill.gif" width="190" alt="Bottom nav pill"><br><sub><b>Bottom pill</b><br>tap or slide across it, the list blurs beneath the glass, the plus button drags</sub></td>
</tr>
<tr>
<td align="center" valign="top"><img src="docs/media/dialog.gif" width="190" alt="Dialog"><br><sub><b>Dialog</b><br>docked glass card over a dimmed page, its buttons drag</sub></td>
<td align="center" valign="top"><img src="docs/media/bottom-sheet.gif" width="190" alt="Bottom sheet"><br><sub><b>Bottom sheet</b><br>rubber band when pulled up, springs back unless it was pulled past half its height</sub></td>
<td align="center" valign="top"><img src="docs/media/drag-free-reorder.gif" width="190" alt="Free drag and re-order"><br><sub><b>Free drag and re-order</b><br><code>physicsDrag()</code> lifts and tilts, <code>ReorderableColumn</code> moves the others out of the way</sub></td>
</tr>
</table>

## Modules

Each module can be used on its own. `iris-ui` depends on all of them.

| Module | Package | Contents |
| --- | --- | --- |
| [`iris-core`](docs/api/iris-core.md) | `dev.irisui.core` | Theme (colors, typography, shapes with squircle corners, motion), icons, press indication, overlay host |
| [`iris-effects`](docs/api/iris-effects.md) | `dev.irisui.effects` | Blur backdrop + glass, press animation, haptics (ColorOS constants), touch glow, persistent glow, drag physics, ColorOS overscroll |
| [`iris-navigation`](docs/api/iris-navigation.md) | `dev.irisui.navigation` | Bottom nav pill, top bar, scaffolds, collapsing large title, segmented tabs, page transitions |
| [`iris-buttons`](docs/api/iris-buttons.md) | `dev.irisui.buttons` | Primary, secondary, icon, round plus, text button |
| [`iris-inputs`](docs/api/iris-inputs.md) | `dev.irisui.inputs` | Switch, text field (filled and the underlined one of dialogs), search bar, checkbox, radio button, slider (continuous, stepped with dots, stepped without), number picker, progress bar, ring and spinner |
| [`iris-content`](docs/api/iris-content.md) | `dev.irisui.content` | Card, list item, grouped list, section header, divider, red dot and badge |
| [`iris-popups`](docs/api/iris-popups.md) | `dev.irisui.popups` | Dialog, bottom sheet, popup menu, popup host |
| [`iris-ui`](docs/api/iris-ui.md) | `dev.irisui.ui` | Umbrella module with `IrisApp` (theme + haptics + popup layer) |
| `catalog` | `dev.irisui.catalog` | The demo app |

## Quick start

```kotlin
// build.gradle.kts of your app
dependencies { implementation(project(":iris-ui")) }
```

```kotlin
setContent {
    IrisApp {                                   // theme, haptics, popup layer
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
```

Use `enableEdgeToEdge()` in the activity: the scaffolds draw under the system bars.

Every list, row and pager inside `IrisApp` gets the translating bounce of the stock apps at its ends (instead of the
Android stretch). `IrisApp(overscrollFactory = null)` turns overscroll off for the whole app, any other
`OverscrollFactory` replaces it, and `CompositionLocalProvider(LocalOverscrollFactory provides null) { ... }` switches it
off for one subtree (the number wheel does that).

## Effects

| Effect | API |
| --- | --- |
| Blur background | `rememberBackdrop()` + `Modifier.backdropSource(backdrop)` on the content, `Modifier.glass(shape, style, backdrop)` on a sibling drawn above it. The look comes from `GlassMaterial` (blur plus two colour layers with Overlay / Luminosity / ColorDodge blend modes, exactly the recipe ColorOS uses): `GlassDefaults.bottomBar()`, `popup()`, `dialog()`, `bottomSheet()`, `toolbarButton()`, `searchBar()`, or your own `GlassMaterial`. `ProgressiveBlur` is the top bar blur. Scaffolds wire this up for their slots. |
| Press animation | `Modifier.touchMotion(shape, dragAxes, claimAxes, cancelOnLeave)`: the ColorOS 17 touch animation. The element **shrinks** while held (0.85 for small, 0.97 for large, the amount comes from its size and aspect ratio), the fill gets lighter, it follows a dragging finger with rubber-band resistance (stretching along the drag, squeezing across it) and springs back with an overshoot. Once the finger has moved a touch slop along a `claimAxes` axis the element owns the drag: the click handler inside it and every scrolling parent let go, so a list does not scroll while a button is dragged. Elements that are not glass buttons (list rows, cards, chips, checkboxes) pass `dragAxes = None, claimAxes = None`: they shrink or light up but neither follow the finger nor take the touch, so the page scrolls from them. Combine with `Modifier.irisClickable(onClick)` for the click and haptic. |
| Haptics | `IrisHaptic` events through `LocalColorOSHaptics`. Source `System` (`performHapticFeedback`, ColorOS maps it to its own waveforms) or `Direct` (predefined `VibrationEffect`s) |
| Touch glow | `Modifier.clip(shape).touchGlow(color, radius)` |
| Persistent glow | `Modifier.persistentGlow(color, spread, offsetY, alpha, intensity)` drawn behind a button |
| Drag physics | `Modifier.touchMotion()` on the Liquid Glass buttons: round icon buttons (the top bar buttons too), the bottom pill, the add button, the pill buttons, the Cancel pill of the search bar, the buttons of dialogs, the segmented control, the popup menu panel, sliders and the large title with its subtitle and the top bar title. List rows (menu entries), cards, chips, checkboxes, switches and the windows of dialogs and sheets do not drag. `Modifier.physicsDrag()` (free drag with rubber band, tilt, lift, spring return) and `ReorderableColumn` |

Blur needs API 31+. On older versions glass falls back to its tint.

## Build and run

Requires JDK 17+ (Android Studio's bundled JBR works), Android SDK platform 37.

```bash
./gradlew :catalog:installDebug
```

Open one component directly (handy while developing):

```bash
adb shell am start -n dev.irisui.catalog/.MainActivity --es destination PrimaryButton
```

Destination names are the entries of `CatalogDestination` (for example `BottomNavPill`, `Dialog`, `DragPhysics`).

## Design tokens measured on the device

| Token | Value |
| --- | --- |
| Page background | `#000000` |
| Card | `#1A1A1A`, radius 20 dp with squircle corners (smoothing 0.55, fitted to the real card outline), 16 dp page margin |
| Accent | `#1A8CFF` (Clock, Files, Notes). Settings itself uses `#428AF7` for its switches and sliders, pass `colors.accent` if you want that |
| Switch | 44 x 24 dp, 18 dp thumb with 3 dp margins, off track `#535353`, track lights up by 20 % white while held |
| Slider | 20 dp bar, white 12 dp dot (16 dp while held) whose centre travels 10 dp inside the ends, a light spot follows it, track = 20 % white, dots 6 dp on stepped sliders |
| List row | min 52 dp, text starts 56 dp from the card edge with an icon, press highlight is a flat 20 % white |
| Search bar | 40 dp pill, `#1F1F1F`, rim light at the top (15 %) and bottom (11 %), none at the sides |
| Round plus button | 56 dp, vertical gradient, colored glow below |
| Bottom pill | 56 dp high, glass (blur sigma 25 px, Overlay + Luminosity layers, 27 over black), no capsule behind the selected tab, wide soft touch glow (13.5 % white, about 140 dp) |
| Icon buttons | 40 dp glass discs |
| Popup menu | glass (blur sigma 26.7 px, Luminosity + Overlay layers, 13 over black), 168 dp minimum width, 40 dp rows, 23 dp corners, rim and edge light |
| Dialog | docked bottom, 16 dp margin, glass (blur sigma 33 px, 35 over black), two pill buttons |
| Sheet | glass (blur sigma 67 px, 21 over black), 32 dp top corners |
| Page transition | push: damping 0.8 / response 0.33 s, pop: critically damped 0.35 s, page below drifts 30 % and gets a 50 % black scrim over 185 ms, like Settings |

## How close is it to the real thing

The touch animation was not guessed: its constants were matched to ColorOS 17, and every behaviour was recorded frame by
frame on the phone and compared against the same gesture on the catalog app.

| Behaviour | Real (Clock / Settings) | Catalog |
| --- | --- | --- |
| 56 dp round button pressed | scale 0.913, 90 % in 144 ms, release peak 1.025 | 0.916, 133 ms, peak 1.026 |
| 328 x 55 dp pill pressed | scale 0.964, release peak 1.005 | 0.966, peak 1.006 |
| Drag of the round button (20 .. 450 px) | translation 0.96 .. 17.8 px, size x1.076 / x0.926 | 1.3 .. 17.9 px, x1.07 / x0.93 |
| Release after a 450 px drag | undershoot 5.5 px (31 %), second swing 1.8 px | 5.6 px, 1.5 px |
| Pill dragged 700 px | follows 24 px, width x1.047 | same model |
| Switch toggle | 10 .. 90 % in about 130 ms, 5 % overshoot, thumb stretches to 69 px | 140 ms, 4.7 %, 68 px |
| List row highlight | 20 % white, comes in over about 300 ms, fades out in 180 ms | same |

The formulas are in `TouchMotion.kt`.

## Blur lab

The catalog screen **Effects > Blur lab** shows every glass surface of the kit on a background you choose (your own
screenshot, a test pattern, the wallpaper, plain colours), in dark or light, with the stock materials or a custom
one (blur radius in px, two colour layers with blend mode). Tap the round button on the right edge for the controls.

It is also the tool used to match the real phone: `adb shell am start` with extras draws one element at exact pixel
coordinates over a screenshot of the real app, so a screenshot of the lab can be compared pixel by pixel with a
screenshot of the real UI.

```bash
# push a background into the app (debug build), then open one element over it
adb push world_A.png /data/local/tmp/lab_world_A.png
adb shell "run-as dev.irisui.catalog sh -c 'mkdir -p files/lab && cp /data/local/tmp/lab_world_A.png files/lab/world_A.png'"
adb shell am start -n dev.irisui.catalog/.MainActivity --es destination BlurLab \
    --es bg world_A.png --es layout single --es kind popup --es rect 528,282,1032,570 --ez clean true
```

Extras: `bg` / `ref` (image names in `files/lab/`), `bgmode` (pattern, black, gray, white, wallpaper), `layout`
(gallery or single), `kind` (pill, popup, dialog, sheet, topbar, button, search), `rect` (`l,t,r,b` in px), `dark`,
`clean` (hides the controls), `component` (single pill: the real component), `rim`, `blur`, `c1` / `m1` / `c2` / `m2` (custom material, `m` is srcover, overlay,
dodge or lum), `tint` (top bar), `split` (reference overlay).

## Catalog screens

Navigation (bottom nav pill, top bar, collapsing large title, segmented tabs, page transitions), buttons, inputs
(switch, text field, search bar, checkbox, slider, progress), content (card, list item, grouped list, section header,
divider, badge), popups (dialog, bottom sheet, popup menu) and effects (blur lab, press animation,
haptics, touch glow, persistent glow, drag physics). The **Example screens** at the end put the components together
into whole pages (a settings page, a habit tracker with a sheet, the type scale). Every demo can be
opened directly with `--es destination NAME`; `--ez light true` shows the kit in its light theme whatever the system theme
is, and `--ez touches true` draws a circle under the finger (for screen recordings). The [catalog tour](docs/catalog.md)
shows a picture of every screen, in both themes, next to the kit APIs it uses.

## Measuring against the phone (`tools/`)

The scripts in `tools/` (bash for Git Bash on Windows, Java single-file analysers in `tools/analysis`) are how the
numbers in this README and in the kit were measured; data and recordings land in `captures/` (not in version control):

* `rec.sh NAME "inject script" SECS W:H:X:Y Analyzer args` records the screen while a touch script plays (`tools/inject`),
  crops the frames and runs an analyser: `PageShift2` (offsets of two pages during a push or pop), `BlobTrack` (slider
  thumb size and position, fill end), `PtColor` and `BoxMean` (colours over time), `RowRuns` (edges along a row),
  `VShift` (scroll or overscroll offset), `ScrollTrack` (position of scrolling content over a whole recording, also from
  a screen recording made by hand: shows flings, bounces and jumps), `BandShift` (how far bands of one screenshot moved
  against another, to prove that a change leaves the layout where it was), `TitleTrack`, `PageDim`, with `Fit1D` (spring
  fit) and `FitBez` (easing fit).
* `gif.sh NAME "inject script" SECS [W:H:X:Y] [width] [fps]` records the screen while a touch script plays and writes a small
  GIF to `docs/media/` (the recordings in this README). Open the screen with `--ez touches true` first, so a circle shows
  where the finger is.
* `tour.sh` takes a screenshot of every catalog screen in both themes (`tour.sh convert` crops and scales them into
  `docs/media/catalog/`), `docs.sh` regenerates `docs/api/` and `docs/catalog.md` from the source (KDoc comments, signatures,
  the catalog demos) and `linkcheck.sh` checks every relative link and anchor of the Markdown files.
* `lib.sh` has the guards for working on a real phone (`need PKG`, `fg PKG`); only navigate and cancel in the stock apps.

## Back swipe and the system

Pages are kept by `PageStack` / `IrisPageHost`: the page below keeps its `rememberSaveable` state (scroll position)
while it is covered, and the host supports the predictive back gesture (`PredictiveBackHandler`). The system decides
whether a swipe commits (45 dp of travel on ColorOS). The page follows the finger 1:1 and always moves from left to right,
also when the swipe starts at the right edge (the distance from that edge counts). On the OnePlus 15 (ColorOS 17) the system
only reports the finger position to a single-activity app when the window is one pixel wider than the display;
`IrisPageHost` does that by itself (`trackBackGesture = true`, see `BackGestureTracking.kt`; layout and look do not
change, nothing happens before Android 16 or in multi-window mode).

### In your own app

All of it is in the library (`iris-navigation`, part of `iris-ui`); the catalog uses exactly what you get:

1. Depend on `:iris-ui` (or `:iris-navigation` alone). **Nothing goes into your manifest**: the library manifest brings
   `android:enableOnBackInvokedCallback="true"`, which the system needs before it sends a back animation any progress. If
   your app sets that attribute to `false` itself, add `tools:replace="android:enableOnBackInvokedCallback"`.
2. A `ComponentActivity` that draws edge to edge, and the host in `IrisApp`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            IrisApp {
                val stack = rememberPageStack<Screen>(Screen.Home)
                IrisPageHost(stack) { screen ->
                    when (screen) {                                   // push(Screen.X) opens a page, pop() or the back gesture closes it
                        Screen.Home -> Home(onOpen = { stack.push(Screen.Detail) })
                        Screen.Detail -> Detail(onBack = { stack.pop() })
                    }
                }
            }
        }
    }
}
```

   Push and pop animations, the page that follows the finger (rounded display corners, page below drifting a quarter of the
   width under a fading scrim) and the finger tracking on ColorOS come with it. `IrisPageHost(stack, trackBackGesture = false)`
   switches only the tracking off. The stack itself lives in memory (`rememberPageStack` is a plain `remember`): when your
   activity can be recreated (rotation, theme or font change) keep it alive with `android:configChanges` like the catalog
   does, or rebuild it from your own saved state.
3. With your own navigation instead of `PageStack`, call `IrisBackGestureTracking()` once near the root of your content and
   use `PredictiveBackHandler` as usual: `BackEventCompat.touchX` is the real finger position then, and
   `event.travelFromEdge(widthPx)` gives the distance from the edge the swipe started at (the page slides to the right for
   both edges).

## Star history

<a href="https://star-history.com/#Markus-Q/ColorOS-17-Jetpack-Compose&Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=Markus-Q/ColorOS-17-Jetpack-Compose&type=Date&theme=dark">
    <img alt="Star history chart" src="https://api.star-history.com/svg?repos=Markus-Q/ColorOS-17-Jetpack-Compose&type=Date" width="600">
  </picture>
</a>

## License

Copyright 2026 Markus-Q. Licensed under the Apache License, Version 2.0, see [LICENSE](LICENSE). ColorOS is a trademark of OPPO (see the note at the top).
