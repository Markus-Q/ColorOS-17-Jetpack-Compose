# iris-navigation

Package `dev.irisui.navigation`. Bottom nav pill, top bar, large title, segmented tabs, page transitions.

[All modules](README.md)

## BackGestureTracking.kt

### IrisBackGestureTracking

Lets the system tell a single-activity app where the finger is during the edge back gesture, so that
`PredictiveBackHandler` can animate with the finger (the page follows it 1:1). `IrisPageHost` calls this by itself;
call it yourself once near the root of your content when you build the back animation on your own navigation:

```
IrisBackGestureTracking()
PredictiveBackHandler(enabled = canGoBack) { events ->
    events.collect { event -> pageOffset = event.travelFromEdge(widthPx) }   // touchX is real now
    goBack()
}
```

**Why it is needed.** Since Android 16 the system leaves the progress of the gesture to the app when the window promises
to compute it from the touches it receives (`BackNavigationInfo.isAppProgressGenerationAllowed`: the window asked for it
and its frame equals the display bounds). ColorOS 17 (checked on a OnePlus 15, Android 17) keeps the touches of the edge
gesture away from the app, so nothing is ever computed: the callback gets the start of the gesture (`touchX` about
10 px, progress 0) and then nothing until the release. The system hands the progress out itself again as soon as the
window frame differs from the display, which is what this does: the window gets one pixel more width, outside of the
screen (`FLAG_LAYOUT_NO_LIMITS`). Checked on the phone: the layout does not move by a single pixel, and every move of
the finger now arrives with its position (about 90 events for a slow swipe across the screen).

It does nothing before Android 16 (the system always sends the progress there), in multi-window and when the window
does not fill the display (the frame differs anyway), and it puts the window back when it leaves the composition.
The app also has to opt in to predictive back (`android:enableOnBackInvokedCallback`); the manifest of this library
already does that for the app it is merged into.

```kotlin
@Composable
fun IrisBackGestureTracking(enabled: Boolean = true)
```

<sub>Source: [BackGestureTracking.kt:43](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/BackGestureTracking.kt#L43)</sub>

### BackEventCompat.travelFromEdge

How far the finger has come from the edge this gesture started at, in px: `touchX` for a swipe from the left edge,
`width - touchX` for one from the right edge. ColorOS slides the page out to the right either way, so the page offset
is this distance whichever side the finger comes from (the page of the stock apps is not mirrored for the right edge).

```kotlin
fun BackEventCompat.travelFromEdge(width: Float): Float
```

<sub>Source: [BackGestureTracking.kt:57](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/BackGestureTracking.kt#L57)</sub>

## BottomNavPill.kt

### NavItem

One tab of `IrisBottomNavPill`.

```kotlin
@Immutable
class NavItem(
    val label: String,
    val icon: ImageVector,
    /** Icon shown while selected (ColorOS fills the icon of the active tab). */
    val selectedIcon: ImageVector = icon,
)
```

<sub>Source: [BottomNavPill.kt:49](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/BottomNavPill.kt#L49)</sub>

### IrisBottomNavPill

The floating glass pill at the bottom of ColorOS apps. Tap or slide a finger across it to
switch tabs: the pill shrinks while touched (and follows the finger with rubber-band
resistance) and a soft light follows the finger. There is no capsule behind the selected tab: it just
switches to its filled icon and full-strength tint, like the Clock and Files pills.

```kotlin
@Composable
fun IrisBottomNavPill(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalBackdrop.current,
)
```

<sub>Source: [BottomNavPill.kt:63](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/BottomNavPill.kt#L63)</sub>

## Chips.kt

### IrisChip

Chip of a single choice row (repeat type of an alarm, filters): 32 dp high pill, 14 sp label, 12 dp padding. The
selected chip is accent filled with a white label, the others a 10 % tint of the label color. `badge` puts the red
dot of a "new" feature on its top end.

```kotlin
@Composable
fun IrisChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
    enabled: Boolean = true,
)
```

<sub>Source: [Chips.kt:42](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/Chips.kt#L42)</sub>

### IrisChipRow

Horizontally scrolling row of chips: 20 dp from the screen edge, 8 dp between chips, the end fades out like the real
row does when more chips are hidden behind the edge.

```kotlin
@Composable
fun IrisChipRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: LazyListScope.() -> Unit,
)
```

<sub>Source: [Chips.kt:99](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/Chips.kt#L99)</sub>

## PageTransitions.kt

### PageAction

What the last navigation did: a page was pushed or popped (see `PageStack.lastAction`).

```kotlin
enum class PageAction {
    Push,
    Pop,
}
```

<sub>Source: [PageTransitions.kt:41](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/PageTransitions.kt#L41)</sub>

### PageStack

Minimal back stack for `IrisPageHost`. Pages can be any value (enum, sealed class, ...).

```kotlin
@Stable
class PageStack<T : Any>(initial: T) {
    val current: T
    val canPop: Boolean
    val depth: Int
    // What happened last; kept for apps that want to react to the direction of the last transition.
    var lastAction
    fun push(page: T)
    fun pop(): Boolean
}
```

<sub>Source: [PageTransitions.kt:60](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/PageTransitions.kt#L60)</sub>

### rememberPageStack

Remembers a `PageStack` that starts with `initial`. The stack lives in memory only (a plain `remember`): keep it alive over
configuration changes yourself or rebuild it from your own saved state.

```kotlin
@Composable
fun <T : Any> rememberPageStack(initial: T): PageStack<T>
```

<sub>Source: [PageTransitions.kt:98](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/PageTransitions.kt#L98)</sub>

### IrisPageHost

Hosts the pages of a `PageStack` with the ColorOS page transition and the back gesture, measured on the Settings app
of a OnePlus 15:

 * **Push:** the new page slides in from the right on a spring (damping 0.8, response 0.33 s) while the page below
   drifts 30 % of the screen width to the left on the same progress and is darkened by a black scrim that ramps up to
   50 % within 185 ms.
 * **Pop:** the same motion backwards, but the spring is critically damped (response 0.35 s) and the scrim fades out
   immediately.
 * **Back gesture:** the top page follows the finger 1:1 and is clipped to the rounded display corners; the page
   below drifts a quarter of the width and its scrim fades out linearly with the distance. Releasing past the
   system threshold (45 dp) slides the page out, releasing earlier springs it back, both critically damped with a
   response of 0.31 s. The page always moves from left to right, also when the gesture starts at the right edge:
   the further the finger has travelled from its edge, the further the page is out.

Pages below the top one are not kept in the composition, but their `rememberSaveable` state (scroll positions, text
of fields) survives while they are covered. The system back button pops the stack.

`trackBackGesture` makes the system report the finger position during the back gesture to a single-activity app
(see `IrisBackGestureTracking`); without it ColorOS only tells the app where the gesture started and the page can
just be animated after the release.

```kotlin
@Composable
fun <T : Any> IrisPageHost(
    stack: PageStack<T>,
    modifier: Modifier = Modifier,
    trackBackGesture: Boolean = true,
    content: @Composable (T) -> Unit,
)
```

<sub>Source: [PageTransitions.kt:129](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/PageTransitions.kt#L129)</sub>

## Scaffold.kt

### IrisScaffold

Page scaffold. The content fills the whole screen and scrolls under a progressive blur at the
top and a floating glass pill / round button at the bottom. Everything in the slots that uses
glass automatically blurs the page content through `LocalBackdrop`.

The content lambda receives padding that already accounts for the bars, so lists should use it
as `contentPadding`.

```kotlin
@Composable
fun IrisScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    topBlurAlpha: () -> Float = { 1f },
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    /** Put the round button centered above the pill (Clock style) instead of beside it (Notes style). */
    fabAbovePill: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
)
```

<sub>Source: [Scaffold.kt:55](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/Scaffold.kt#L55)</sub>

### LargeTitleScope

Gives pages a `LargeTitle` to place as the first item of their scrolling content.

```kotlin
@Stable
class LargeTitleScope internal constructor(
    private val title: String,
    private val subtitle: String?,
    private val scrollOffset: () -> Float = { 0f },
) {
    // The expanded title: 32 sp, 16 dp from the screen edge, with an optional 14 sp subtitle.
    fun LargeTitle(modifier: Modifier = Modifier)
}
```

<sub>Source: [Scaffold.kt:128](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/Scaffold.kt#L128)</sub>

### IrisLargeTitleScaffold

Collapsing large title. Place `LargeTitle()` as the first item of the scrolling content and
hand over how far the content is scrolled: the small title fades into the top bar once the large
one has scrolled away, and the blur under the bar fades in as soon as content passes below it.

```kotlin
@Composable
fun IrisLargeTitleScaffold(
    title: String,
    scrollOffset: () -> Float,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    fabAbovePill: Boolean = false,
    content: @Composable LargeTitleScope.(PaddingValues) -> Unit,
)
```

<sub>Source: [Scaffold.kt:192](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/Scaffold.kt#L192)</sub>

### LazyListState.titleScrollOffset

Scroll offset (px) for a lazy list, for `IrisLargeTitleScaffold`.

```kotlin
fun LazyListState.titleScrollOffset(): () -> Float
```

<sub>Source: [Scaffold.kt:229](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/Scaffold.kt#L229)</sub>

## SegmentedTabs.kt

### IrisSegmentedTabs

Segmented control of the Battery page: a 40 dp track, a neutral 32 dp thumb 4 dp inside it, and labels that keep one
color. The two edges of the thumb are springs of their own (damping 0.85, response 0.41 s for the edge in front, 0.47 s
for the one behind), so the thumb stretches by about 10 % on the way. The whole control shrinks to 96 % while it is
held. A selection change is silent, like on the Battery page (checked with the vibrator log).

```kotlin
@Composable
fun IrisSegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = IrisTheme.colors.content.copy(alpha = 0.135f),
    thumbColor: Color = IrisTheme.colors.content.copy(alpha = 0.22f),
    selectedContentColor: Color = IrisTheme.colors.content,
    unselectedContentColor: Color = selectedContentColor,
)
```

<sub>Source: [SegmentedTabs.kt:49](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/SegmentedTabs.kt#L49)</sub>

## TopBar.kt

### IrisTopBarDefaults

Measures of the top bar.

```kotlin
object IrisTopBarDefaults {
    // Toolbar height below the status bar (52 dp, measured 156 px at 480 dpi).
    val Height
}
```

<sub>Source: [TopBar.kt:28](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/TopBar.kt#L28)</sub>

### IrisTopBar

Compact top bar: round back button, a title that sits next to it and round action buttons.
Transparent on purpose; `IrisScaffold` puts the progressive blur behind it.
`titleAlpha` is read while drawing, so it can follow scrolling without recomposition.

```kotlin
@Composable
fun IrisTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleAlpha: () -> Float = { 1f },
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
)
```

<sub>Source: [TopBar.kt:39](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/TopBar.kt#L39)</sub>

### IrisBackButton

Round glass back button for `IrisTopBar`.

```kotlin
@Composable
fun IrisBackButton(onClick: () -> Unit, modifier: Modifier = Modifier)
```

<sub>Source: [TopBar.kt:83](../../iris-navigation/src/main/kotlin/dev/irisui/navigation/TopBar.kt#L83)</sub>

