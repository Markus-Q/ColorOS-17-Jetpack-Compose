# FAQ and troubleshooting

## Nothing blurs, the glass looks flat

Real blur needs Android 12 (API 31). Below that, and wherever a surface has no backdrop, `Modifier.glass` draws the material as
the flat color it has over `GlassStyle.fallbackBackdrop` (black by default). On Android 12 and newer check that

* the page is marked with `Modifier.backdropSource(backdrop)` and has an **opaque** background, otherwise the blurred copy is
  translucent and the sharp page shows through,
* the glass surface is drawn **above** the page (a later sibling), not inside the source,
* the surface can find the backdrop: inside `IrisApp` and the scaffolds `LocalBackdrop` is provided, otherwise pass
  `backdrop` to `glass` yourself.

See [Glass and blur](glass-and-blur.md).

## The back swipe does not follow the finger

The page only follows the finger when the system reports its position during the gesture.

* Android 16 or newer is needed for this to matter; before that the system always sends the progress itself.
* The app has to opt in to predictive back: `android:enableOnBackInvokedCallback="true"`. The manifest of `iris-navigation`
  sets it, so it is merged into your app, unless your own manifest sets it to `false` (then add
  `tools:replace="android:enableOnBackInvokedCallback"`).
* On ColorOS 17 the window has to be one pixel wider than the display for the position to arrive.
  `IrisPageHost` does that, or call `IrisBackGestureTracking()` once near the root when you build the back animation on
  your own navigation. In multi-window mode it is not applied.

See [Navigation](navigation.md#the-back-swipe).

## A list does not scroll when my finger starts on a button

That is the behavior of the Liquid Glass buttons of ColorOS: once the finger has moved a touch slop the button *claims* the
gesture and follows it, and nothing scrolls while it is dragged. List rows, cards, chips and checkboxes do not claim anything,
so the list scrolls from them. For your own element pass `dragAxes = TouchDragAxes.None, claimAxes = TouchDragAxes.None` to
`Modifier.touchMotion`. See [Who owns a drag](touch-and-drag.md#who-owns-a-drag).

## A dialog, sheet or menu is drawn inside my layout instead of above the page

Popups need the popup host. Put the content under `IrisApp` (or `IrisPopupHost`); without one a popup is composed in
place, in the composable that calls it. See [Popups](popups.md#they-need-a-host).

## There is no vibration

* Outside `IrisApp` the haptics default to `NoOpHaptics`. Provide real ones with `IrisApp` or
  `CompositionLocalProvider(LocalColorOSHaptics provides rememberColorOSHaptics())`.
* `haptics.enabled` must be true and the device needs a vibrator.
* The stock apps are quiet: buttons, tabs, chips, cards and rows do not vibrate, only toggles, the round add button and picker
  steps do. The kit follows that on purpose, so a click has no haptic unless you pass one to `irisClickable`.

See [Haptics](touch-and-drag.md#haptics).

## Why does my text look different from the screenshots?

The kit does not bundle a font. Text uses the system font, and the screenshots were taken on a ColorOS 17 phone whose system
font is a variable font: the weights in `IrisTypography` are the weights that font is *drawn at* in the stock apps
(titles around 575, summaries 400). On a device with another system font the same sizes and weights look different.

## How do I change the accent color?

Per component: the buttons take a `color` (`IrisPrimaryButton("Save", onClick, color = ...)`). For the whole app pass your
own `IrisColors` to `IrisApp(colors = ...)`; copy `irisDarkColors()` or `irisLightColors()` and change the values.
See [Theming](theming.md#changing-colors).

## How do I turn the bounce at the end of lists off?

`IrisApp(overscrollFactory = null)` turns overscroll off for the whole app, any other `OverscrollFactory` replaces it, and
`CompositionLocalProvider(LocalOverscrollFactory provides null) { ... }` switches it off for one subtree.
See [Overscroll](touch-and-drag.md#overscroll).

## My pages are gone after a rotation

`rememberPageStack` is a plain `remember`. Keep the activity alive with `android:configChanges` as the catalog does, or rebuild
the stack from your own saved state. See [Navigation](navigation.md#pages-push-pop-and-the-stack).

## Is it published on Maven?

No. The modules are meant to be copied into a project or used as a Git submodule, see
[Getting started](getting-started.md#adding-the-kit-to-a-project).

## On which phones does it work?

The kit is built on standard Compose and Android APIs and has fallbacks for older Android versions (no blur below API 31, no
tracking of the back swipe before Android 16) and for haptics off ColorOS (the standard constants). It has been tested on one
device only: a OnePlus 15 (CPH2747) with ColorOS 17.0.1 on Android 17. The numbers behind the look were measured there; sizes
are expressed in dp and sp.

## How exact is it, and how was it measured?

The constants of the touch animation were matched to ColorOS 17, and the behaviors were recorded frame by frame on the phone
and compared with the same gesture on the catalog. The table is in the
[README](../README.md#how-close-is-it-to-the-real-thing).

## How were the screenshots and recordings made?

With the scripts in `tools/`: `tour.sh` takes a screenshot of every catalog screen in both themes, `gif.sh` records the screen
while a scripted touch plays and turns it into a GIF (the catalog draws a circle under the finger with `--ez touches true`),
and `docs.sh` regenerates the [API reference](api/README.md) and the [catalog tour](catalog.md) from the source.
