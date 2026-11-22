# iris-effects

Package `dev.irisui.effects`. Blur backdrop, press, haptics, touch glow, persistent glow, drag physics.

[All modules](README.md)

## Backdrop.kt

### Backdrop

The content that glass surfaces blur. Mark the page content with `backdropSource` and put the
glass surfaces next to it (as siblings that are drawn later), never inside it.

```kotlin
@Stable
class Backdrop internal constructor(internal val layer: GraphicsLayer)
```

<sub>Source: [Backdrop.kt:53](../../iris-effects/src/main/kotlin/dev/irisui/effects/Backdrop.kt#L53)</sub>

### rememberBackdrop

Creates and remembers a `Backdrop`: mark the content to blur with `backdropSource` and draw glass surfaces above it with `glass`.
`IrisApp` and the scaffolds do this for you.

```kotlin
@Composable
fun rememberBackdrop(): Backdrop
```

<sub>Source: [Backdrop.kt:71](../../iris-effects/src/main/kotlin/dev/irisui/effects/Backdrop.kt#L71)</sub>

### LocalBackdrop

Backdrop that glass surfaces use when none is passed explicitly. Scaffolds provide it.

```kotlin
val LocalBackdrop
```

<sub>Source: [Backdrop.kt:77](../../iris-effects/src/main/kotlin/dev/irisui/effects/Backdrop.kt#L77)</sub>

### Modifier.backdropSource

Records this content into `backdrop` (and still draws it normally).

```kotlin
fun Modifier.backdropSource(backdrop: Backdrop): Modifier
```

<sub>Source: [Backdrop.kt:80](../../iris-effects/src/main/kotlin/dev/irisui/effects/Backdrop.kt#L80)</sub>

### GlassFade

Direction in which a surface fades out (progressive blur for top and bottom bars).

```kotlin
enum class GlassFade {
    TowardsBottom,
    TowardsTop,
}
```

<sub>Source: [Backdrop.kt:113](../../iris-effects/src/main/kotlin/dev/irisui/effects/Backdrop.kt#L113)</sub>

### Modifier.verticalFadeMask

Fades the content of this element out along the vertical axis. Put it before `glass` in the
modifier chain to get a progressive blur: full strength at the bar, gone at the far edge.

```kotlin
fun Modifier.verticalFadeMask(fade: GlassFade): Modifier
```

<sub>Source: [Backdrop.kt:119](../../iris-effects/src/main/kotlin/dev/irisui/effects/Backdrop.kt#L119)</sub>

### GlassStyle

Everything that makes up a glass surface: the `material` (blur plus colour layers, see `GlassMaterial`) and the edge
decoration (rim light and the soft light along the inner bottom edge). Without a `material` the older recipe applies:
a plain Gaussian `blur` covered by a flat `tint`.

```kotlin
@Immutable
data class GlassStyle(
    val blur: Dp = 24.dp,
    /** Flat colour drawn over the blurred backdrop, only used when there is no [material]. */
    val tint: Color = Color.Transparent,
    val rimHighlight: Color = Color.Transparent,
    val rimShade: Color = Color.Transparent,
    val rimWidth: Dp = 1.dp,
    /**
     * Soft light that spills inwards from the edge of the glass (white with a small alpha). It is strongest at the
     * bottom edge ([edgeGlowBottom]) and weaker at the top ([edgeGlowTop]); the two colours are blended in between and
     * the light fades out with a Gaussian of [edgeGlowRadius].
     */
    val edgeGlowTop: Color = Color.Transparent,
    val edgeGlowBottom: Color = Color.Transparent,
    val edgeGlowRadius: Dp = 8.dp,
    /** Extra light along the bottom edge that reaches further inwards than the rest ([bottomGlowRadius]). */
    val bottomGlow: Color = Color.Transparent,
    val bottomGlowRadius: Dp = 12.dp,
    /**
     * Light that spills out below the glass (the pill and the round toolbar buttons have it): the outline of the shape,
     * moved down by [outerGlowOffset] and blurred with [outerGlowRadius], drawn behind the glass.
     */
    val outerGlow: Color = Color.Transparent,
    val outerGlowOffset: Dp = 2.dp,
    val outerGlowRadius: Dp = 1.2.dp,
    val material: GlassMaterial? = null,
    /** What the page behind the glass looks like where no real backdrop can be blurred (no backdrop, API < 31). */
    val fallbackBackdrop: Color = Color.Black,
)
```

<sub>Source: [Backdrop.kt:140](../../iris-effects/src/main/kotlin/dev/irisui/effects/Backdrop.kt#L140)</sub>

### GlassDefaults

Ready-made `GlassStyle`s for the glass surfaces of ColorOS 17 (bottom pill, toolbar buttons, popup menu, dialog, bottom sheet, search bar)
and builders for your own.

```kotlin
object GlassDefaults {
    // Floating surfaces with a flat tint: round glass buttons in custom layouts, demo panels.
    fun surface(blur: Dp = 22.dp, tintAlpha: Float = 0.78f): GlassStyle
    // Any glass surface built from a `GlassMaterial`, with the rim of the floating surfaces.
    fun surface(material: GlassMaterial): GlassStyle
    // The floating bottom navigation pill.
    fun bottomBar(): GlassStyle
    // Round glass buttons in toolbars.
    fun toolbarButton(): GlassStyle
    // Popup menus: a brighter rim, strongest at the bottom edge, and no light along the inner edge.
    fun popup(): GlassStyle
    // Dialog panel (strong blur, translucent colour layers).
    fun dialog(): GlassStyle
    // Bottom sheet panel: the strongest blur of the system.
    fun bottomSheet(): GlassStyle
    // Search field: the top bar colours without any blur.
    fun searchBar(): GlassStyle
}
```

<sub>Source: [Backdrop.kt:174](../../iris-effects/src/main/kotlin/dev/irisui/effects/Backdrop.kt#L174)</sub>

### Modifier.glass

Turns this element into a glass surface: it draws a blurred copy of `backdrop` clipped to
`shape`, runs it through the colour layers of the material and adds a light rim. Without a backdrop (or below
API 31) the material is drawn as the flat colour it has over `GlassStyle.fallbackBackdrop`.

```kotlin
@Composable
fun Modifier.glass(
    shape: Shape,
    style: GlassStyle = GlassDefaults.bottomBar(),
    backdrop: Backdrop? = LocalBackdrop.current,
): Modifier
```

<sub>Source: [Backdrop.kt:278](../../iris-effects/src/main/kotlin/dev/irisui/effects/Backdrop.kt#L278)</sub>

## DragPhysics.kt

### PhysicsDragState

State of a freely draggable element. It follows the finger with rubber band resistance,
leans into the movement, lifts while held and springs back with the release velocity.

```kotlin
@Stable
class PhysicsDragState internal constructor(private val scope: CoroutineScope) {
    val offset
    val scale
    val rotation
    var isDragging
}
```

<sub>Source: [DragPhysics.kt:48](../../iris-effects/src/main/kotlin/dev/irisui/effects/DragPhysics.kt#L48)</sub>

### rememberPhysicsDragState

Remembers the `PhysicsDragState` of one element that uses `physicsDrag`.

```kotlin
@Composable
fun rememberPhysicsDragState(): PhysicsDragState
```

<sub>Source: [DragPhysics.kt:92](../../iris-effects/src/main/kotlin/dev/irisui/effects/DragPhysics.kt#L92)</sub>

### Modifier.physicsDrag

Makes the element draggable in any direction with physics: rubber band past `maxDistance`,
a lift while held, tilt from the drag direction and a springy return. Haptics fire on grab and drop.

```kotlin
@Composable
fun Modifier.physicsDrag(
    state: PhysicsDragState = rememberPhysicsDragState(),
    maxDistance: Dp = 140.dp,
    liftScale: Float = 1.06f,
    maxTilt: Float = 9f,
): Modifier
```

<sub>Source: [DragPhysics.kt:102](../../iris-effects/src/main/kotlin/dev/irisui/effects/DragPhysics.kt#L102)</sub>

### ReorderableColumn

A column whose items can be re-ordered by dragging their `handle`. Items shift out of the way
with springs, the dragged item lifts and follows the finger, and it settles into its slot on release.

```kotlin
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, isDragging: Boolean, handle: Modifier) -> Unit,
)
```

<sub>Source: [DragPhysics.kt:148](../../iris-effects/src/main/kotlin/dev/irisui/effects/DragPhysics.kt#L148)</sub>

## GlassMaterial.kt

### GlassLayer

One colour layer of a glass material. `color` (with its alpha) is blended onto what is below it with `blendMode`.
ColorOS only uses `BlendMode.SrcOver`, `BlendMode.Overlay`, `BlendMode.ColorDodge` and `BlendMode.Luminosity`.

```kotlin
@Immutable
data class GlassLayer(val color: Color, val blendMode: BlendMode)
```

<sub>Source: [GlassMaterial.kt:21](../../iris-effects/src/main/kotlin/dev/irisui/effects/GlassMaterial.kt#L21)</sub>

### GlassMaterial

The recipe of every glass surface of ColorOS 17, matched on a OnePlus 15: the backdrop is blurred with a Gaussian of
`blur`, then passed through `layers`, each one a
colour blended with its own blend mode. Nothing else is involved, in particular there is no flat translucent tint:
Overlay / ColorDodge keep the colours of what is behind the glass, Luminosity keeps its hue but takes the colour's
brightness.

```kotlin
@Immutable
data class GlassMaterial(
    /** Standard deviation of the Gaussian. The stock values are pixels on a 480 dpi screen, so 3 px = 1 dp. */
    val blur: Dp,
    val layers: List<GlassLayer>,
    /**
     * Window level materials (popup, dialog, sheet) are blurred by the compositor, which also sees the content just
     * outside the glass. View level materials (bars, buttons) only see what is behind the view and mirror at its edges.
     */
    val sampleOutside: Boolean = false,
) {
    // The colour this material produces over a uniform, opaque background.
    fun over(background: Color): Color
}
```

<sub>Source: [GlassMaterial.kt:31](../../iris-effects/src/main/kotlin/dev/irisui/effects/GlassMaterial.kt#L31)</sub>

### GlassMaterials

The materials of ColorOS 17, matched on a OnePlus 15: view level materials (bars, buttons) and window level
materials (popup menu, dialog, bottom sheet).

All blur values are pixels at 480 dpi in the system, converted to dp here (3 px = 1 dp). The window level radius
units of the system (160 popup, 200 dialog, 400 sheet) are 6 units per pixel of Gaussian sigma.

```kotlin
object GlassMaterials {
    // Floating bottom navigation pill.
    fun bottomBar(dark: Boolean)
    // Top bar, toolbar buttons and the search bar (which uses it without blur).
    fun topBar(dark: Boolean, blur: Boolean = true)
    // Search bar: the top bar colours without blur (light uses slightly stronger layers).
    fun searchBar(dark: Boolean)
    // Secondary button on top of blurred content: a plain white veil.
    fun secondaryButton(dark: Boolean)
    fun transparentButton()
    fun segmentButton(dark: Boolean)
    fun chipTab(dark: Boolean, selected: Boolean)
    // Popup menu (system blur radius 160, blend mode "glow overlay"): the mix colour is blended with Luminosity, so the colours behind the popup stay vivid, then the second colour is laid over it with Overlay.
    fun popup(dark: Boolean)
    // Dialog (system blur radius 200): plain mix colour, then Overlay (dark) / ColorDodge (light).
    fun dialog(dark: Boolean)
    // Bottom sheet (system blur radius 400, the strongest blur in the system).
    fun bottomSheet(dark: Boolean)
}
```

<sub>Source: [GlassMaterial.kt:132](../../iris-effects/src/main/kotlin/dev/irisui/effects/GlassMaterial.kt#L132)</sub>

## Glow.kt

### Modifier.persistentGlow

The always-on colored light that sits under ColorOS buttons (round plus button, primary
button, glass icon buttons). Drawn behind the element, so give the parent some room and do
not clip it. `intensity` is read while drawing, so animating it never recomposes.

```kotlin
fun Modifier.persistentGlow(
    color: Color,
    spread: Dp = 26.dp,
    offsetY: Dp = 8.dp,
    alpha: Float = 0.6f,
    intensity: () -> Float = { 1f },
    /** Half width of the glow; by default it follows the element width. */
    halfWidth: Dp? = null,
): Modifier
```

<sub>Source: [Glow.kt:29](../../iris-effects/src/main/kotlin/dev/irisui/effects/Glow.kt#L29)</sub>

### Modifier.touchGlow

Soft light that follows the finger inside the element while it is touched and fades out
after release. Apply `clip(shape)` before this modifier so the light stays inside the shape.
It only observes touches; it never consumes them. The defaults are the glow of the Clock
bottom pill: 13.5 % white at the finger, fading out over about 140 dp.

```kotlin
@Composable
fun Modifier.touchGlow(
    color: Color = Color.White.copy(alpha = 0.135f),
    radius: Dp = 140.dp,
    enabled: Boolean = true,
): Modifier
```

<sub>Source: [Glow.kt:66](../../iris-effects/src/main/kotlin/dev/irisui/effects/Glow.kt#L66)</sub>

## Haptics.kt

### IrisHaptic

Semantic haptic events. Components fire these; the engine maps them to the device motor.

```kotlin
enum class IrisHaptic {
    Tap,
    Tick,
    SliderStep,
    Selection,
    Confirm,
    Reject,
    ToggleOn,
    ToggleOff,
    DragStart,
    DragEnd,
    LongPress,
    HeavyClick,
    Thud,
    Pop,
    DoubleClick,
}
```

<sub>Source: [Haptics.kt:23](../../iris-effects/src/main/kotlin/dev/irisui/effects/Haptics.kt#L23)</sub>

### IrisHapticConstants

The haptic constants of ColorOS, accepted by `View.performHapticFeedback` on ColorOS / OxygenOS devices. Which one
the stock apps use where was read from the vibrator log of a OnePlus 15: switch toggle and round add button 302,
number picker steps 307; sliders fire 305 per step.

```kotlin
object IrisHapticConstants {
    const val LONG_VIBRATE
    const val KEYBOARD_TOUCH_FEEDBACK
    const val GRANULAR_SHORT_VIBRATE
    const val SHORT_VIBRATE
    const val SHORT_TRIPLE_VIBRATE
    const val STEPPING_SHORT_VIBRATE
    const val EDGE_SHORT_VIBRATE
    const val EDGE_LIST_VIBRATE
    const val GRANULAR_SHORT_VIBRATE_SYNC
    const val EDGE_SQUEEZE
}
```

<sub>Source: [Haptics.kt:53](../../iris-effects/src/main/kotlin/dev/irisui/effects/Haptics.kt#L53)</sub>

### HapticSource

Where haptics come from.

```kotlin
enum class HapticSource {
    ColorOS,
    System,
    Direct,
}
```

<sub>Source: [Haptics.kt:67](../../iris-effects/src/main/kotlin/dev/irisui/effects/Haptics.kt#L67)</sub>

### IrisHaptics

Plays `IrisHaptic` events on the device. `enabled` switches all of them off, `source` chooses how they reach the vibration motor.

```kotlin
@Stable
interface IrisHaptics {
    var enabled: Boolean
    var source: HapticSource
    fun perform(event: IrisHaptic)
}
```

<sub>Source: [Haptics.kt:85](../../iris-effects/src/main/kotlin/dev/irisui/effects/Haptics.kt#L85)</sub>

### NoOpHaptics

Haptics that do nothing: the default outside `IrisApp`, and what previews and tests use.

```kotlin
object NoOpHaptics : IrisHaptics
```

<sub>Source: [Haptics.kt:92](../../iris-effects/src/main/kotlin/dev/irisui/effects/Haptics.kt#L92)</sub>

### LocalColorOSHaptics

The haptics the components fire their events through: `NoOpHaptics` unless `IrisApp` or your own provider supplies real ones.

```kotlin
val LocalColorOSHaptics
```

<sub>Source: [Haptics.kt:101](../../iris-effects/src/main/kotlin/dev/irisui/effects/Haptics.kt#L101)</sub>

### rememberColorOSHaptics

Creates `AndroidColorOSHaptics` for the current view and remembers it. `IrisApp` does this and provides the result through
`LocalColorOSHaptics`.

```kotlin
@Composable
fun rememberColorOSHaptics(): IrisHaptics
```

<sub>Source: [Haptics.kt:108](../../iris-effects/src/main/kotlin/dev/irisui/effects/Haptics.kt#L108)</sub>

### AndroidColorOSHaptics

`IrisHaptics` on the vibrator of the device. `source` decides how an event is played first (the ColorOS constants on ColorOS / OxygenOS,
standard `performHapticFeedback` constants, or predefined `VibrationEffect`s); when that is not possible the next way is tried.
The source starts as the ColorOS constants on OPPO / OnePlus / realme devices and as the standard constants elsewhere.

```kotlin
@SuppressLint("InlinedApi", "NewApi")
class AndroidColorOSHaptics(
    private val view: View,
    context: Context,
) : IrisHaptics
```

<sub>Source: [Haptics.kt:120](../../iris-effects/src/main/kotlin/dev/irisui/effects/Haptics.kt#L120)</sub>

### isOplusDevice

True on ColorOS / OxygenOS devices (OPPO, OnePlus, realme), which accept the ColorOS haptic constants.

```kotlin
fun isOplusDevice(): Boolean
```

<sub>Source: [Haptics.kt:222](../../iris-effects/src/main/kotlin/dev/irisui/effects/Haptics.kt#L222)</sub>

## Overscroll.kt

### IrisOverscrollFactory

Overscroll of the stock apps instead of the Android stretch: when a list is pulled past its end the content simply
translates, and springs back when the finger lets go. Measured on Settings by recording the screen:

 * the content follows 24 % of a slow pull (0.5 px per ms at 480 dpi) and up to 34 % of a fast one, the offset stays
   where it is while the finger rests,
 * after the release it returns on an ease-out of about 0.7 s (cubic-bezier 0.2, 0.62, 0.17, 1.05); a fling that hits the
   end throws the content out by up to about 280 px first, and the same curve brings it back.

What the effect promises (the contract of `OverscrollEffect`):

 * It only ever reacts to what the list could *not* take. A fling that runs out on its own in the middle of a list
   leaves nothing, so nothing bounces; `performFling` answers with the velocity the list *used*, and the bounce is built
   from the rest (an earlier version read it the other way round and threw the content out after every fling).
 * A finger on the content always wins: it stops a spring-back where it is and drags on from there.
 * The spring-back can never leave the content displaced, not even when the scope that runs it is torn down.
 * `performScroll` and `performFling` are each called exactly once per event, so nested scrolling parents keep working.

Provide it for a whole app with `CompositionLocalProvider(LocalOverscrollFactory provides IrisOverscrollFactory)`
(`IrisApp` does).

```kotlin
object IrisOverscrollFactory : OverscrollFactory
```

<sub>Source: [Overscroll.kt:52](../../iris-effects/src/main/kotlin/dev/irisui/effects/Overscroll.kt#L52)</sub>

## PressEffects.kt

### Modifier.irisClickable

Click with ColorOS feedback: an optional indication and a haptic event. The shrink / drag
motion of a press is separate, add `touchMotion` in front of this modifier for it.
Use `indication = LocalIndication.current` for rows that should show the flat highlight.

```kotlin
@Composable
fun Modifier.irisClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    // The stock apps are quiet: buttons, tabs, chips, cards and rows do not vibrate (measured with the vibrator log);
    // only toggles, the round add button and picker steps do, so no haptic is the default.
    haptic: IrisHaptic? = null,
    indication: Indication? = null,
    role: Role? = Role.Button,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Modifier
```

<sub>Source: [PressEffects.kt:17](../../iris-effects/src/main/kotlin/dev/irisui/effects/PressEffects.kt#L17)</sub>

## ProgressiveBlur.kt

### ProgressiveBlur

The blur under a top bar. Measured on the Settings app: the content that scrolls under the bar
is blurred with a radius that falls linearly from `maxBlur` at the top edge to nothing at the bottom edge, and it is
covered by the page colour with an alpha that falls linearly from `tintAlpha` to 0. Together they make content fade
out smoothly instead of ending at a hard line.

The variable radius is built from `steps` stacked blurs of increasing radius, each fading in over its own slice
of the bar, which is indistinguishable from a true variable blur at five steps.

Give it the height of the status bar plus the toolbar. `backdrop` is the page content, like for `glass`.

```kotlin
@Composable
fun ProgressiveBlur(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalBackdrop.current,
    /** Blur radius at the top edge (a Gaussian sigma; the stock value is 26 px at 480 dpi). */
    maxBlur: Dp = (26f / 3f).dp,
    steps: Int = 5,
    tint: Color = IrisTheme.colors.background,
    tintAlpha: Float = 0.4f,
)
```

<sub>Source: [ProgressiveBlur.kt:31](../../iris-effects/src/main/kotlin/dev/irisui/effects/ProgressiveBlur.kt#L31)</sub>

## TouchMotion.kt

### TouchSpring

A spring described by a `bounce` and a `duration` (the time of one natural oscillation, in seconds).

`dampingRatio = 1 - bounce` and `stiffness = (2π / duration)²`. The numbers in `TouchMotionDefaults` were matched to
ColorOS 17 and checked against frame-by-frame recordings of the real system.

```kotlin
@Stable
class TouchSpring(val bounce: Float, val duration: Float) {
    // Never below 0.1: a spring that is (nearly) undamped would swing for ever and never come to rest.
    val dampingRatio: Float
    val omega: Float
    val stiffness: Float
}
```

<sub>Source: [TouchMotion.kt:50](../../iris-effects/src/main/kotlin/dev/irisui/effects/TouchMotion.kt#L50)</sub>

### TouchDragAxes

Which axes follow the finger while the element is held.

```kotlin
enum class TouchDragAxes(internal val x: Boolean, internal val y: Boolean) {
    None(false, false),
    Horizontal(true, false),
    Vertical(false, true),
    Both(true, true),
}
```

<sub>Source: [TouchMotion.kt:58](../../iris-effects/src/main/kotlin/dev/irisui/effects/TouchMotion.kt#L58)</sub>

### TouchMotionDefaults

The constants of the ColorOS 17 touch animation.

```kotlin
object TouchMotionDefaults {
    val CompactPress
    val CompactRelease
    val WidePress
    val WideRelease
    val DragFollow
    val HandUpSmall
    val HandUpLarge
    val HighlightIn
    val HighlightOut
    val RowHighlightIn
    val CompactHighlightIn
    val RowHighlightOut
    // Neutral surfaces get this much white mixed in while pressed (26 -> 52 on the Clock pill).
    const val HighlightAlpha
    // Accent-filled surfaces turn lighter and a bit more cyan (28,150,251 -> 42,189,253 on the Clock FAB).
    val AccentHighlight
    const val AccentHighlightAlpha
}
```

<sub>Source: [TouchMotion.kt:66](../../iris-effects/src/main/kotlin/dev/irisui/effects/TouchMotion.kt#L66)</sub>

### TouchMotionMath

The formulas behind the touch animation. Public so apps can drive their own visuals with them.

```kotlin
object TouchMotionMath {
    // True for elongated elements (aspect ratio above 2), which use the wide springs instead of the compact ones.
    fun usesWideSprings(widthPx: Float, heightPx: Float): Boolean
    // Scale an element of the given size shrinks to while held.
    fun pressedScale(widthPx: Float, heightPx: Float, density: Float): Float
    // `max * (1 - 1 / (ratio * |x| / max + 1))`, signed like `x`.
    fun rubber(x: Float, ratio: Float, max: Float): Float
    // Translation of the element for a finger offset of `offsetPx` along one axis.
    fun translation(offsetPx: Float, density: Float): Float
    // Largest stretch an element of this size is allowed (1.2 for small ones, less for big ones).
    fun maxDeformScale(widthPx: Float, heightPx: Float): Float
    // Stretch / squeeze for a finger offset: the element grows along the drag direction and shrinks across it, keeping its area.
    fun deform(dxPx: Float, dyPx: Float, widthPx: Float, heightPx: Float, density: Float): Pair<Float, Float>
    // Hand-up spring of the drag; bigger elements settle a little slower.
    fun handUp(widthPx: Float, heightPx: Float, density: Float): TouchSpring
}
```

<sub>Source: [TouchMotion.kt:128](../../iris-effects/src/main/kotlin/dev/irisui/effects/TouchMotion.kt#L128)</sub>

### TouchMotionState

Animation state of one touchable element: the shrink of the press, the rubber-band
translation and the stretch / squeeze while dragged, and the lighter fill. Create it with
`rememberTouchMotionState` to read `isPressed` / `pressProgress` from outside.

```kotlin
@Stable
class TouchMotionState internal constructor(private val scope: CoroutineScope) {
    // True from touch down until the finger lifts or the touch is cancelled.
    var isPressed
    // True once the finger has moved past the dead zone and the element follows it.
    var isDragging
    // 0 at rest, 1 fully pressed (follows the shrink spring, so it overshoots on release).
    val pressProgress: Float
    // 0..1 amount of the lighter fill.
    val highlight: Float
}
```

<sub>Source: [TouchMotion.kt:316](../../iris-effects/src/main/kotlin/dev/irisui/effects/TouchMotion.kt#L316)</sub>

### rememberTouchMotionState

Remembers the `TouchMotionState` of one element. Pass it to `touchMotion` to read the animation (for example to draw something that
follows the press) or to drive it yourself.

```kotlin
@Composable
fun rememberTouchMotionState(): TouchMotionState
```

<sub>Source: [TouchMotion.kt:446](../../iris-effects/src/main/kotlin/dev/irisui/effects/TouchMotion.kt#L446)</sub>

### Modifier.touchMotion

The ColorOS 17 touch animation.

While the element is touched it **shrinks** (by an amount that depends on its size and shape),
its fill gets lighter and it follows the finger with rubber-band resistance, stretching along the
drag and squeezing across it. When the finger lifts it springs back with an overshoot.

The modifier does not consume the touch while the finger only rests or moves a little (a tap still clicks), so
combine it with `clickable` or any gesture detector. Apply it **before** `clip` / `background` so those move with
it, and pass the element's `shape` so the lighter fill stays inside it.

**Who owns a drag.** Once the finger has moved a touch slop along an axis of `claimAxes` the element *claims* the
gesture: the click handler inside it and every scrolling parent let go, so a list does not scroll while a button
is dragged. The claim is made after the handlers inside the element have had their say (a slider or a switch that
drags its own thumb keeps the touch), and before the parents get it. Only the Liquid Glass buttons of ColorOS drag
like this. List rows, cards, chips and checkboxes pass `dragAxes = TouchDragAxes.None, claimAxes = TouchDragAxes.None`:
they shrink and light up while held, but they neither follow the finger nor take the touch, so the page scrolls when a
drag starts on them.

**Parameters**

- `dragAxes`: axes along which the element follows the finger (`TouchDragAxes.None` for a pure shrink).
- `claimAxes`: axes whose drags this element takes over (see above); `TouchDragAxes.None` leaves every drag to whoever else wants it.
- `cancelOnLeave`: spring back as soon as the finger leaves the element (the real toolbar buttons do this, floating buttons keep their pressed state however far the finger moves).
- `pressedScale`: fixed pressed scale; by default it is derived from the element's size like ColorOS does.
- `highlightAlpha`: how much of `highlight` is mixed over the fill while pressed (0 for transparent elements).
- `yieldToConsumers`: cancel the motion as soon as another gesture handler (a scrolling parent, a slider...) consumes the touch before this element claims it. Turn it off for elements that handle the drag themselves, like a tab bar or a slider.

```kotlin
@Composable
fun Modifier.touchMotion(
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    dragAxes: TouchDragAxes = TouchDragAxes.Both,
    claimAxes: TouchDragAxes = TouchDragAxes.Both,
    cancelOnLeave: Boolean = false,
    pressedScale: Float = Float.NaN,
    highlight: Color = Color.White,
    highlightAlpha: Float = TouchMotionDefaults.HighlightAlpha,
    yieldToConsumers: Boolean = true,
    highlightIn: TouchSpring = TouchMotionDefaults.HighlightIn,
    highlightOut: TouchSpring = TouchMotionDefaults.HighlightOut,
    state: TouchMotionState = rememberTouchMotionState(),
): Modifier
```

<sub>Source: [TouchMotion.kt:482](../../iris-effects/src/main/kotlin/dev/irisui/effects/TouchMotion.kt#L482)</sub>

