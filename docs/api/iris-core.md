# iris-core

Package `dev.irisui.core`. Theme, colors, typography, shapes, motion, icons, overlay host.

[All modules](README.md)

## IrisColors.kt

### IrisColors

Color tokens of the ColorOS 17 look.

The values match ColorOS 17 (dark and light variants) and were checked against screenshots of the real apps on a
OnePlus 15. Most text and surface colors are translucent white (dark) or black (light) on purpose: they blend with
whatever is behind them (page, sheet, glass), like the system does.

```kotlin
@Immutable
class IrisColors(
    val isDark: Boolean,
    /** Page background. */
    val background: Color,
    /** Cards and grouped lists that sit on [background] (10 % white in dark mode). */
    val surface: Color,
    /** Cards that sit on a sheet or dialog. */
    val surfaceElevated: Color,
    /** Chips, secondary buttons, segmented track, round icon buttons. */
    val surfaceVariant: Color,
    /** Text field and search bar fill. */
    val field: Color,
    /** Tint of glass surfaces (bottom pill, popup menu, top bar). */
    val glass: Color,
    val sheet: Color,
    val dialog: Color,
    /** Primary text and icons (90 % white / black). */
    val content: Color,
    /** Summaries, section headers, values (54 % in dark mode). */
    val contentSecondary: Color,
    /** Hints and disabled text (30 % in dark mode). */
    val contentTertiary: Color,
    /** List dividers (20 % white in dark mode, one pixel high). */
    val divider: Color,
    val accent: Color,
    val onAccent: Color,
    val destructive: Color,
    /** Switch track while off (drawn over the card). */
    val switchTrackOff: Color,
    /** Overlay drawn on a row while it is pressed. */
    val pressOverlay: Color,
    val scrim: Color,
    /** Highlight on the top edge of glass surfaces. */
    val rimHighlight: Color,
    /** Faint edge on the bottom of glass surfaces. */
    val rimShade: Color,
    /** Accent for text and text buttons ("Add", links): lighter than [accent] in dark mode. */
    val accentText: Color = accent,
    /** Second level of text on glass and tab bars (72 %). */
    val contentVariant: Color = content,
    /** Second level of the secondary text on glass and tab bars (40 %). */
    val contentSecondaryVariant: Color = contentSecondary,
    /** Faintest label level. */
    val contentQuaternary: Color = contentTertiary,
    /** Empty part of sliders and progress bars. */
    val trackEmpty: Color = divider,
    /** Highlight of the list row that a popup menu is anchored to (15 % white). */
    val anchorHighlight: Color = pressOverlay,
    /** Red badge dots and error containers. */
    val destructiveContainer: Color = destructive,
) {
    fun copy(
        accent: Color = this.accent,
        onAccent: Color = this.onAccent,
        destructive: Color = this.destructive,
        background: Color = this.background,
        surface: Color = this.surface,
        accentText: Color = if (accent == this.accent) this.accentText else accent,
    )
}
```

<sub>Source: [IrisColors.kt:14](../../iris-core/src/main/kotlin/dev/irisui/core/IrisColors.kt#L14)</sub>

### IrisBlue

ColorOS blue (dark variant). Settings uses a slightly different blue (#428AF7).

```kotlin
val IrisBlue
```

<sub>Source: [IrisColors.kt:105](../../iris-core/src/main/kotlin/dev/irisui/core/IrisColors.kt#L105)</sub>

### IrisSettingsBlue

The blue of caret and underline in the text fields of the stock dialogs (measured: #428AF7).

```kotlin
val IrisSettingsBlue
```

<sub>Source: [IrisColors.kt:108](../../iris-core/src/main/kotlin/dev/irisui/core/IrisColors.kt#L108)</sub>

### irisDarkColors

The dark palette of ColorOS 17. Pass it, or your own `IrisColors`, to `IrisTheme`.

```kotlin
fun irisDarkColors()
```

<sub>Source: [IrisColors.kt:111](../../iris-core/src/main/kotlin/dev/irisui/core/IrisColors.kt#L111)</sub>

### irisLightColors

The light palette of ColorOS 17. Pass it, or your own `IrisColors`, to `IrisTheme`.

```kotlin
fun irisLightColors()
```

<sub>Source: [IrisColors.kt:143](../../iris-core/src/main/kotlin/dev/irisui/core/IrisColors.kt#L143)</sub>

## IrisIcons.kt

### IrisIcons

A small stroke-based icon set so the kit has no dependency on an icon library.

```kotlin
object IrisIcons {
    // Back arrow with a long shaft, as in the ColorOS toolbar (ink 20 x 16 dp inside the 24 dp box).
    val Back: ImageVector
    val ChevronRight: ImageVector
    val Plus: ImageVector
    val Close: ImageVector
    val Check: ImageVector
    val Search: ImageVector
    val More: ImageVector
    val Sun: ImageVector
    val Home: ImageVector
    val HomeFilled: ImageVector
    val Heart: ImageVector
    val HeartFilled: ImageVector
    val Person: ImageVector
    val PersonFilled: ImageVector
    val Bell: ImageVector
    val BellFilled: ImageVector
    val Grid: ImageVector
    val GridFilled: ImageVector
    val Tune: ImageVector
    val Star: ImageVector
    val StarFilled: ImageVector
    val Alarm: ImageVector
    // Alarm clock with a solid face, the selected state of the Alarm tab.
    val AlarmFilled: ImageVector
    val Globe: ImageVector
    val Stopwatch: ImageVector
    val Hourglass: ImageVector
    // Speaker with a slash: media volume at zero.
    val SpeakerMuted: ImageVector
    // Music note with vibration waves, the ringtone row.
    val MusicVibrate: ImageVector
    val Trash: ImageVector
}
```

<sub>Source: [IrisIcons.kt:46](../../iris-core/src/main/kotlin/dev/irisui/core/IrisIcons.kt#L46)</sub>

## IrisIndication.kt

### IrisIndication

Indication for clickable surfaces that are not list rows (menu items, custom clickables): a flat
highlight of the press overlay (20 % white in dark mode, measured on ColorOS Settings rows). It comes in
with a critically damped spring (response 0.3 s) and fades out in about 0.2 s.

```kotlin
object IrisIndication : IndicationNodeFactory
```

<sub>Source: [IrisIndication.kt:21](../../iris-core/src/main/kotlin/dev/irisui/core/IrisIndication.kt#L21)</sub>

## IrisMotion.kt

### IrisEasing

Easing curves used by the transitions of the kit. `Decelerate` starts fast and lands softly (page pushes and popups).

```kotlin
object IrisEasing {
    // Fast start, long soft landing.
    val Decelerate
    val Standard
    val Accelerate
}
```

<sub>Source: [IrisMotion.kt:12](../../iris-core/src/main/kotlin/dev/irisui/core/IrisMotion.kt#L12)</sub>

### IrisMotion

Animation specs shared by every component so the whole kit moves the same way.

```kotlin
@Immutable
class IrisMotion(
    /** Press-down / release scale. Slightly underdamped so a release overshoots a little. */
    val press: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.58f, stiffness = 650f),
    /** Thumbs, indicators and sliding highlights. */
    val indicator: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.72f, stiffness = 520f),
    /** Things that settle softly: sheets, dialogs. */
    val soft: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 380f),
    /** Playful return after a drag. */
    val bouncy: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 260f),
    /** Page push: a spring with damping 0.8 and response 0.33 s, fitted to the Settings page push (rms 0.6 %). */
    val pageDamping: Float = 0.8f,
    val pageResponseSeconds: Float = 0.33f,
    /** Page pop: critically damped with a response of 0.35 s (fitted to the Settings back transition, rms 0.3 %). */
    val pagePopDamping: Float = 1.0f,
    val pagePopResponseSeconds: Float = 0.35f,
    /** Release of the back gesture (slide out after the threshold, spring back before it): critically damped, 0.31 s. */
    val pageGestureResponseSeconds: Float = 0.31f,
    /** How far the page underneath drifts to the left, as a share of the screen width (325 px of 1080 on the OnePlus 15). */
    val pageParallax: Float = 0.30f,
    /** The page underneath is covered by a black scrim that ramps linearly up to this alpha ... */
    val pageDimAlpha: Float = 0.5f,
    /** ... over this long (the scrim starts [pagePushDimDelayMillis] after a push begins, at once on a pop). */
    val pageDimDurationMillis: Int = 185,
    val pagePushDimDelayMillis: Int = 30,
    val fadeDurationMillis: Int = 180,
) {
    // Spring of a page push (new page in, old page out).
    fun <T> pageSpec(): FiniteAnimationSpec<T>
    // Spring of a page pop (top page out, page underneath back in).
    fun <T> pagePopSpec(): FiniteAnimationSpec<T>
    // Spring after the finger lets go of the back gesture.
    fun <T> pageGestureSpec(): FiniteAnimationSpec<T>
    fun <T> fade(): FiniteAnimationSpec<T>
}
```

<sub>Source: [IrisMotion.kt:21](../../iris-core/src/main/kotlin/dev/irisui/core/IrisMotion.kt#L21)</sub>

## IrisShapes.kt

### SmoothRoundedCornerShape

Rounded rectangle with "continuous" (squircle) corners, the corner style ColorOS and iOS use.
`smoothing` 0 gives plain circular corners, ~0.5 gives the soft ColorOS look.
The curve construction follows the well known corner-smoothing algorithm used by design tools.

```kotlin
@Immutable
class SmoothRoundedCornerShape(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
    private val smoothing: Float = 0.55f,
) : Shape
```

<sub>Source: [IrisShapes.kt:31](../../iris-core/src/main/kotlin/dev/irisui/core/IrisShapes.kt#L31)</sub>

### SmoothPillShape

Stadium with squircle ends, for everything that is a "pill" in ColorOS: the bottom nav pill, buttons, tabs, search
bars. A plain `RoundedCornerShape(percent = 50)` has circular ends; here every corner is a quarter of a superellipse
`|x|^n + |y|^n = r^n` with the full radius r = half the short side. `exponent` 2 gives circular ends (the old
pill), 3 the soft squircle ends of ColorOS, 4 and more get boxy. On a square it gives a round squircle.

```kotlin
@Immutable
class SmoothPillShape(private val exponent: Float = 3f) : Shape
```

<sub>Source: [IrisShapes.kt:130](../../iris-core/src/main/kotlin/dev/irisui/core/IrisShapes.kt#L130)</sub>

### IrisShapes

Shape tokens. Radii were measured on a OnePlus 15 running ColorOS 17.

```kotlin
@Immutable
class IrisShapes(
    val small: Shape = SmoothRoundedCornerShape(10.dp),
    val field: Shape = SmoothRoundedCornerShape(16.dp),
    val card: Shape = SmoothRoundedCornerShape(20.dp, smoothing = 0.55f),
    val popup: Shape = SmoothRoundedCornerShape(23.dp),
    val dialog: Shape = SmoothRoundedCornerShape(30.dp),
    val sheet: Shape = SmoothRoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp),
    val pill: Shape = SmoothPillShape(),
)
```

<sub>Source: [IrisShapes.kt:171](../../iris-core/src/main/kotlin/dev/irisui/core/IrisShapes.kt#L171)</sub>

## IrisTheme.kt

### LocalColorOSColors

Colors of the current theme (the dark palette outside a `IrisTheme`); read them through `IrisTheme.colors`.

```kotlin
val LocalColorOSColors
```

<sub>Source: [IrisTheme.kt:29](../../iris-core/src/main/kotlin/dev/irisui/core/IrisTheme.kt#L29)</sub>

### LocalColorOSTypography

Typography of the current theme; read it through `IrisTheme.typography`.

```kotlin
val LocalColorOSTypography
```

<sub>Source: [IrisTheme.kt:31](../../iris-core/src/main/kotlin/dev/irisui/core/IrisTheme.kt#L31)</sub>

### LocalColorOSShapes

Shapes of the current theme; read them through `IrisTheme.shapes`.

```kotlin
val LocalColorOSShapes
```

<sub>Source: [IrisTheme.kt:33](../../iris-core/src/main/kotlin/dev/irisui/core/IrisTheme.kt#L33)</sub>

### LocalColorOSMotion

Motion specs of the current theme; read them through `IrisTheme.motion`.

```kotlin
val LocalColorOSMotion
```

<sub>Source: [IrisTheme.kt:35](../../iris-core/src/main/kotlin/dev/irisui/core/IrisTheme.kt#L35)</sub>

### LocalContentColor

Color text and icons use when they are not given one explicitly.

```kotlin
val LocalContentColor
```

<sub>Source: [IrisTheme.kt:38](../../iris-core/src/main/kotlin/dev/irisui/core/IrisTheme.kt#L38)</sub>

### IrisTheme

Access to the current theme tokens.

```kotlin
object IrisTheme {
    val colors: IrisColors
    val typography: IrisTypography
    val shapes: IrisShapes
    val motion: IrisMotion
}
```

<sub>Source: [IrisTheme.kt:41](../../iris-core/src/main/kotlin/dev/irisui/core/IrisTheme.kt#L41)</sub>

### IrisTheme

Root of the theme. Provides colors, typography, shapes, motion and the press indication.
Use `dev.irisui.core.IrisTheme` on its own, or the umbrella IrisApp which also wires haptics and popups.

```kotlin
@Composable
fun IrisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: IrisColors = remember(darkTheme) { if (darkTheme) irisDarkColors() else irisLightColors() },
    typography: IrisTypography = remember { IrisTypography() },
    shapes: IrisShapes = remember { IrisShapes() },
    motion: IrisMotion = remember { IrisMotion() },
    content: @Composable () -> Unit,
)
```

<sub>Source: [IrisTheme.kt:57](../../iris-core/src/main/kotlin/dev/irisui/core/IrisTheme.kt#L57)</sub>

### IrisText

Text that resolves its color from `LocalContentColor` and its style from the theme.

```kotlin
@Composable
fun IrisText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = IrisTheme.typography.body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
)
```

<sub>Source: [IrisTheme.kt:78](../../iris-core/src/main/kotlin/dev/irisui/core/IrisTheme.kt#L78)</sub>

### IrisIcon

Vector icon tinted with `tint` (default: current content color).

```kotlin
@Composable
fun IrisIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
)
```

<sub>Source: [IrisTheme.kt:99](../../iris-core/src/main/kotlin/dev/irisui/core/IrisTheme.kt#L99)</sub>

## IrisTypography.kt

### IrisTypography

Text styles. Color is intentionally left unspecified; text picks it up from LocalContentColor.

Sizes follow the type scale of ColorOS 17; line heights are the measured
line boxes of the real apps on the OnePlus 15 (font size x font metrics 1.17 x line spacing multiplier), and
weights are the *effective* weights found by comparing glyph coverage with the real screens (the system font is a
variable font, titles are drawn at about 575, summaries at 400).

```kotlin
@Immutable
class IrisTypography(
    /** Collapsing large title of in-app pages ("Alarm", "My Files"): 32 sp, drawn at weight 850. */
    val largeTitle: TextStyle = style(32f, 38f, 850),
    /** Large title of top level pages such as the Settings home page: 40 sp, white. */
    val largeTitleXL: TextStyle = style(40f, 47f, 850),
    /** Clock style digits (alarm time, timer lists): 30 sp at weight 580. */
    val numeric: TextStyle = style(30f, 42f, 580),
    /** Second line under a large title ("Next alarm in ...", 14 sp). */
    val subtitleSmall: TextStyle = style(14f, 19f, 400),
    /** Dialog and sheet titles. */
    val title: TextStyle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    /** Title of dialogs (18 sp, drawn at weight 700 like the top bar title). */
    val dialogTitle: TextStyle = style(18f, 24.06f, 700),
    /** Title of the compact top bar (18 sp). */
    val topBarTitle: TextStyle = style(18f, 24.06f, 708).copy(
        lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None),
    ),
    val headline: TextStyle = style(16f, 21.66f, 575),
    /** List item titles (drawn at weight 575). */
    val body: TextStyle = style(16f, 21.66f, 575),
    /** Dialog message, list item summary (13 sp). */
    val bodySmall: TextStyle = style(13f, 17.84f, 400),
    val subtitle: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    val label: TextStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    val button: TextStyle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    /** Labels of the bottom pill (10 sp). */
    val tabLabel: TextStyle = style(10f, 14f, 500),
    /** Chip labels (14 sp). */
    val chip: TextStyle = style(14f, 20.05f, 500),
    /** Rows of popup menus (15 sp, regular weight). */
    val menuItem: TextStyle = style(15f, 19.87f, 400),
    /** Group header above cards ("Volume", drawn at weight 575). */
    val sectionHeader: TextStyle = style(12f, 16.05f, 575),
    /** Small secondary text such as the line under a sheet title (12 sp). */
    val caption: TextStyle = style(12f, 16.05f, 400),
)
```

<sub>Source: [IrisTypography.kt:33](../../iris-core/src/main/kotlin/dev/irisui/core/IrisTypography.kt#L33)</sub>

## Overlay.kt

### OverlayHost

A "portal" layer for dialogs, sheets and popup menus. Popups register their content here
and the host draws it above the page, so every popup can use the same blurred backdrop and
animate freely instead of living in a separate platform window.

```kotlin
@Stable
class OverlayHost
```

<sub>Source: [Overlay.kt:20](../../iris-core/src/main/kotlin/dev/irisui/core/Overlay.kt#L20)</sub>

### LocalOverlayHost

The `OverlayHost` that popups register with; `null` outside a popup host, where popups are composed in place instead.

```kotlin
val LocalOverlayHost
```

<sub>Source: [Overlay.kt:39](../../iris-core/src/main/kotlin/dev/irisui/core/Overlay.kt#L39)</sub>

### OverlayHostLayer

Draws everything that popups registered with `host`. Place it above the page content.

```kotlin
@Composable
fun OverlayHostLayer(host: OverlayHost)
```

<sub>Source: [Overlay.kt:43](../../iris-core/src/main/kotlin/dev/irisui/core/Overlay.kt#L43)</sub>

### OverlayPortal

Renders `content` in the nearest `OverlayHost`; falls back to inline composition when there is none.
The content stays alive as long as this call stays in the composition, so popups keep their
own exit animation by delaying removal until it finished.

```kotlin
@Composable
fun OverlayPortal(content: @Composable () -> Unit)
```

<sub>Source: [Overlay.kt:55](../../iris-core/src/main/kotlin/dev/irisui/core/Overlay.kt#L55)</sub>

## Utils.kt

### rubberBand

Rubber band resistance. Maps an unbounded `offset` to a value that approaches `limit`
asymptotically, the way overscroll and over-drag feel on ColorOS.

```kotlin
fun rubberBand(offset: Float, limit: Float, coefficient: Float = 0.55f): Float
```

<sub>Source: [Utils.kt:15](../../iris-core/src/main/kotlin/dev/irisui/core/Utils.kt#L15)</sub>

### Modifier.plainClickable

Click without any indication; callers draw their own feedback.

```kotlin
@Composable
fun Modifier.plainClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
): Modifier
```

<sub>Source: [Utils.kt:23](../../iris-core/src/main/kotlin/dev/irisui/core/Utils.kt#L23)</sub>

