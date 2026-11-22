package dev.irisui.effects

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A spring described by a `bounce` and a `duration` (the time of one natural oscillation, in seconds).
 *
 * `dampingRatio = 1 - bounce` and `stiffness = (2π / duration)²`. The numbers in [TouchMotionDefaults] were matched to
 * ColorOS 17 and checked against frame-by-frame recordings of the real system.
 */
@Stable
class TouchSpring(val bounce: Float, val duration: Float) {
    /** Never below 0.1: a spring that is (nearly) undamped would swing for ever and never come to rest. */
    val dampingRatio: Float get() = (1f - bounce).coerceIn(0.1f, 1f)
    val omega: Float get() = (2.0 * PI / duration).toFloat()
    val stiffness: Float get() = omega * omega
}

/** Which axes follow the finger while the element is held. */
enum class TouchDragAxes(internal val x: Boolean, internal val y: Boolean) {
    None(false, false),
    Horizontal(true, false),
    Vertical(false, true),
    Both(true, true),
}

/** The constants of the ColorOS 17 touch animation. */
object TouchMotionDefaults {
    // Press (shrink). Round and compact elements use the compact springs, elongated ones (aspect ratio > 2) the wide springs.
    val CompactPress = TouchSpring(bounce = 0f, duration = 0.25f)
    val CompactRelease = TouchSpring(bounce = 0.6f, duration = 0.5f)
    val WidePress = TouchSpring(bounce = 0f, duration = 0.3f)
    val WideRelease = TouchSpring(bounce = 0.5f, duration = 0.5f)

    // Finger follow and hand-up of the rubber-band drag.
    val DragFollow = TouchSpring(bounce = 0.15f, duration = 0.15f)
    val HandUpSmall = TouchSpring(bounce = 0.65f, duration = 0.42f)
    val HandUpLarge = TouchSpring(bounce = 0.65f, duration = 0.45f)

    // The fill gets lighter while pressed: quick in, slow out (buttons)...
    val HighlightIn = TouchSpring(bounce = 0f, duration = 0.15f)
    val HighlightOut = TouchSpring(bounce = 0f, duration = 0.46f)

    // ...and the flat highlight of list rows and switches: it comes in with the press spring and fades fast.
    val RowHighlightIn = TouchSpring(bounce = 0f, duration = 0.3f)
    val CompactHighlightIn = TouchSpring(bounce = 0f, duration = 0.25f)
    val RowHighlightOut = TouchSpring(bounce = 0f, duration = 0.21f)

    /** Neutral surfaces get this much white mixed in while pressed (26 -> 52 on the Clock pill). */
    const val HighlightAlpha = 0.125f

    /** Accent-filled surfaces turn lighter and a bit more cyan (28,150,251 -> 42,189,253 on the Clock FAB). */
    val AccentHighlight = Color(0xFF3FF7FF)
    const val AccentHighlightAlpha = 0.4f

    internal const val MinAreaDp = 20f
    internal const val MaxAreaDp = 150f
    internal const val ScaleMin = 0.85f
    internal const val ScaleMax = 0.97f
    internal const val AspectThreshold = 2f
    internal const val AspectExtremeRatio = 15f
    internal const val AspectBaseScale = 0.85f
    internal const val AspectExtremeScale = 1f

    internal const val MaxStretchDp = 28f
    internal const val RubberRatio = 0.05f
    internal const val MaxDeformDp = 150f
    internal const val DeformRatio = 0.55f
    internal const val DeformMaxScale = 1.2f
    internal const val DeadZonePx = 5f
    internal const val MaxInitialVelocity = 1000f

    /**
     * The finger's release velocity reaches the hand-up spring as px per *millisecond* times 0.3 (the velocity is asked
     * in units of 1 and then multiplied by 0.3). That makes it almost nothing (a hard flick of
     * 4400 px/s arrives as about 0.4 px/s), so on the real phone the element simply springs back with the base bounce
     * 0.65: a first overshoot of about 31 % of the offset, two or three swings, at rest after well under a second
     * (measured on the add button of the Photos app, slow release and hard flick alike). An earlier version fed px/s,
     * which saturated the formula, gave a bounce of 1 (no damping at all) and made buttons swing for ever.
     */
    internal const val ReleaseVelocityScale = 0.3f

    /** Safety net only: with the velocity in the real unit the bounce stays at its base value of 0.65. */
    internal const val MaxBounce = 0.8f
    internal const val HandUpSmallAreaDp = 56f
    internal const val HandUpLargeAreaDp = 100f
}

/** The formulas behind the touch animation. Public so apps can drive their own visuals with them. */
object TouchMotionMath {
    /** Elements smaller than this (px², about 196 x 196 px) may be stretched by the full [TouchMotionDefaults.DeformMaxScale]. */
    private const val SmallElementArea = 38416

    /** How quickly the allowed stretch falls with the size (per natural-log step of the area) and with the elongation. */
    private const val SizeReduction = 0.35f
    private const val ShapeReduction = 0.015f

    /** No element is stretched less than this. */
    private const val DeformFloor = 1.03f

    private val handUpAreaEasing = CubicBezierEasing(0.550265f, 0f, 0.891534f, 0.187831f)

    /** Where [value] lies between [start] and [end], from 0 to 1. */
    private fun fractionBetween(value: Float, start: Float, end: Float): Float =
        ((value - start) / (end - start)).coerceIn(0f, 1f)

    /** [from] moved towards [to] by [fraction]. */
    private fun mix(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

    /** cubic-bezier(0, 0, 0, 1) in closed form: x(s) = s³, y(s) = 3s² − 2s³. */
    private fun easeOutStrong(t: Float): Float {
        if (t <= 0f) return 0f
        if (t >= 1f) return 1f
        val cbrt = Math.cbrt(t.toDouble()).toFloat()
        return 3f * cbrt * cbrt - 2f * t
    }

    /** True for elongated elements (aspect ratio above 2), which use the wide springs instead of the compact ones. */
    fun usesWideSprings(widthPx: Float, heightPx: Float): Boolean {
        val shortSide = min(widthPx, heightPx)
        if (shortSide <= 0f) return false
        return max(widthPx, heightPx) / shortSide > TouchMotionDefaults.AspectThreshold
    }

    /**
     * Scale an element of the given size shrinks to while held. Small and elongated elements
     * shrink more (down to 0.85), large ones barely (up to 0.97).
     */
    fun pressedScale(widthPx: Float, heightPx: Float, density: Float): Float {
        if (widthPx <= 0f || heightPx <= 0f) return 1f
        val lowSide = (TouchMotionDefaults.MinAreaDp * density).roundToInt().toFloat()
        val highSide = (TouchMotionDefaults.MaxAreaDp * density).roundToInt().toFloat()
        // The size sets the base scale ...
        val sizeFraction = fractionBetween(widthPx * heightPx, lowSide * lowSide, highSide * highSide)
        val bySize = mix(TouchMotionDefaults.ScaleMin, TouchMotionDefaults.ScaleMax, easeOutStrong(sizeFraction))
        val elongation = max(widthPx / heightPx, heightPx / widthPx)
        if (elongation <= TouchMotionDefaults.AspectThreshold) return bySize.coerceIn(0.4f, 1f)
        // ... and an elongated shape pulls it halfway towards its own scale.
        val shapeFraction = fractionBetween(elongation, TouchMotionDefaults.AspectThreshold, TouchMotionDefaults.AspectExtremeRatio)
        val byShape = mix(TouchMotionDefaults.AspectBaseScale, TouchMotionDefaults.AspectExtremeScale, easeOutStrong(shapeFraction))
        return ((bySize + byShape) / 2f).coerceIn(0.4f, 1f)
    }

    /** `max * (1 - 1 / (ratio * |x| / max + 1))`, signed like [x]. */
    fun rubber(x: Float, ratio: Float, max: Float): Float {
        if (max <= 0f) return 0f
        return sign(x) * max * (1f - 1f / (abs(x) * ratio / max + 1f))
    }

    /** Translation of the element for a finger offset of [offsetPx] along one axis. */
    fun translation(offsetPx: Float, density: Float): Float =
        rubber(offsetPx, TouchMotionDefaults.RubberRatio, TouchMotionDefaults.MaxStretchDp * density)

    /** Largest stretch an element of this size is allowed (1.2 for small ones, less for big ones). */
    fun maxDeformScale(widthPx: Float, heightPx: Float): Float {
        val full = TouchMotionDefaults.DeformMaxScale
        val area = (widthPx * heightPx).toInt()
        if (area <= SmallElementArea) return full
        val longSide = max(widthPx, heightPx)
        val shortSide = max(1f, min(widthPx, heightPx))
        // The bigger and the more elongated the element, the closer the allowed stretch comes to the floor.
        val reduction = min(1f, SizeReduction * ln(area / SmallElementArea.toFloat()) + ShapeReduction * (longSide / shortSide - 1f))
        return max(DeformFloor, mix(full, DeformFloor, reduction))
    }

    /**
     * Stretch / squeeze for a finger offset: the element grows along the drag direction and
     * shrinks across it, keeping its area. Returns (scaleX, scaleY).
     */
    fun deform(dxPx: Float, dyPx: Float, widthPx: Float, heightPx: Float, density: Float): Pair<Float, Float> {
        val adx = abs(dxPx)
        val ady = abs(dyPx)
        if (adx + ady <= 0f) return 1f to 1f
        val reach = TouchMotionDefaults.MaxDeformDp * density
        val headroom = maxDeformScale(widthPx, heightPx) - 1f
        // Growth along one axis for a finger offset along it: from 1 up to 1 + headroom, eased like a rubber band.
        fun growth(offset: Float): Float = 1f + headroom * (rubber(offset, TouchMotionDefaults.DeformRatio, reach) / reach)
        val xShare = adx / (adx + ady)
        // In logs, stretching along x squeezes y by the same factor, so the area stays; a drag along y does the opposite and a
        // diagonal drag blends the two by how much of it points along x.
        val stretch = xShare * ln(growth(adx)) - (1f - xShare) * ln(growth(ady))
        return exp(stretch) to exp(-stretch)
    }

    /** Hand-up spring of the drag; bigger elements settle a little slower. */
    fun handUp(widthPx: Float, heightPx: Float, density: Float): TouchSpring {
        val small = TouchMotionDefaults.HandUpSmall
        val large = TouchMotionDefaults.HandUpLarge
        val area = widthPx * heightPx
        val lowArea = (TouchMotionDefaults.HandUpSmallAreaDp * density).let { it * it }
        val highArea = (TouchMotionDefaults.HandUpLargeAreaDp * density).let { it * it }
        if (area <= 0f || highArea <= lowArea) return small
        val eased = handUpAreaEasing.transform(fractionBetween(area, lowArea, highArea))
        return TouchSpring(
            bounce = mix(small.bounce, large.bounce, eased),
            duration = mix(small.duration, large.duration, eased),
        )
    }

    /**
     * What an offset that is let go starts with: the finger's release velocity (see
     * [TouchMotionDefaults.ReleaseVelocityScale]) scaled by how far the element is displaced, and a bounce that rises above the
     * base bounce of the hand-up spring only when that velocity is large. Returns (velocity, bounce).
     */
    internal fun releaseKick(offsetPx: Float, fingerVelocity: Float, reachPx: Float, baseBounce: Float): Pair<Float, Float> {
        val limit = TouchMotionDefaults.MaxInitialVelocity
        val kick = (fingerVelocity / reachPx * abs(offsetPx)).coerceIn(-limit, limit)
        val bounce = max(abs(kick) / limit, baseBounce).coerceAtMost(TouchMotionDefaults.MaxBounce)
        return kick to bounce
    }
}

/** One spring-driven value stepped analytically, so retargeting while it moves keeps its velocity. */
private class SpringValue(initial: Float, private val threshold: Float) {
    private val state = mutableFloatStateOf(initial)
    val value: Float get() = state.floatValue
    var velocity = 0f
    var target = initial
        private set
    var active = false
        private set
    private var omega = 25f
    private var zeta = 1f

    fun retarget(target: Float, spring: TouchSpring) {
        this.target = target
        omega = spring.omega
        zeta = spring.dampingRatio
        active = true
    }

    fun snapTo(v: Float) {
        state.floatValue = v
        velocity = 0f
        target = v
        active = false
    }

    fun step(dt: Float) {
        if (!active) return
        val x0 = state.floatValue - target
        val v0 = velocity
        val w = omega
        val z = zeta
        val nx: Float
        val nv: Float
        if (z < 0.9999f) {
            val wd = w * sqrt(1f - z * z)
            val e = exp(-z * w * dt)
            val c = cos(wd * dt)
            val s = sin(wd * dt)
            val b = (v0 + z * w * x0) / wd
            nx = e * (x0 * c + b * s)
            nv = e * (-z * w * (x0 * c + b * s) + (-x0 * wd * s + b * wd * c))
        } else {
            val e = exp(-w * dt)
            val b = v0 + w * x0
            nx = e * (x0 + b * dt)
            nv = e * (v0 - w * b * dt)
        }
        if (abs(nx) < threshold && abs(nv) < threshold * 62.5f) {
            state.floatValue = target
            velocity = 0f
            active = false
        } else {
            state.floatValue = target + nx
            velocity = nv
        }
    }
}

/**
 * Animation state of one touchable element: the shrink of the press, the rubber-band
 * translation and the stretch / squeeze while dragged, and the lighter fill. Create it with
 * [rememberTouchMotionState] to read [isPressed] / [pressProgress] from outside.
 */
@Stable
class TouchMotionState internal constructor(private val scope: CoroutineScope) {
    private val press = SpringValue(1f, 0.0005f)
    private val moveX = SpringValue(0f, 0.5f)
    private val moveY = SpringValue(0f, 0.5f)
    private val stretchX = SpringValue(1f, 0.0005f)
    private val stretchY = SpringValue(1f, 0.0005f)
    private val light = SpringValue(0f, 0.002f)
    private val all = listOf(press, moveX, moveY, stretchX, stretchY, light)

    private var pressedTarget = 1f
    private var widthPx = 0f
    private var heightPx = 0f
    private var density = 1f
    private var wide = false
    private var dragging = false
    internal var highlightIn = TouchMotionDefaults.HighlightIn
    internal var highlightOut = TouchMotionDefaults.HighlightOut

    /** True from touch down until the finger lifts or the touch is cancelled. */
    var isPressed by mutableStateOf(false)
        private set

    /** True once the finger has moved past the dead zone and the element follows it. */
    var isDragging by mutableStateOf(false)
        private set

    /** 0 at rest, 1 fully pressed (follows the shrink spring, so it overshoots on release). */
    val pressProgress: Float
        get() = if (pressedTarget >= 1f) 0f else ((1f - press.value) / (1f - pressedTarget))

    /** 0..1 amount of the lighter fill. */
    val highlight: Float get() = light.value.coerceIn(0f, 1f)

    internal val scaleX: Float get() = press.value * stretchX.value
    internal val scaleY: Float get() = press.value * stretchY.value
    internal val translationX: Float get() = moveX.value
    internal val translationY: Float get() = moveY.value

    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var loop: Job? = null

    private fun poke() {
        if (loop == null) {
            loop = scope.launch {
                while (true) {
                    wake.receive()
                    var last = withFrameNanos { it }
                    while (all.any { it.active }) {
                        val now = withFrameNanos { it }
                        val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                        last = now
                        for (s in all) s.step(dt)
                    }
                }
            }
        }
        wake.trySend(Unit)
    }

    internal fun down(width: Int, height: Int, density: Float, fixedScale: Float) {
        this.widthPx = width.toFloat()
        this.heightPx = height.toFloat()
        this.density = density
        wide = TouchMotionMath.usesWideSprings(widthPx, heightPx)
        pressedTarget = if (fixedScale.isNaN()) TouchMotionMath.pressedScale(widthPx, heightPx, density) else fixedScale
        dragging = false
        isPressed = true
        isDragging = false
        press.retarget(pressedTarget, if (wide) TouchMotionDefaults.WidePress else TouchMotionDefaults.CompactPress)
        light.retarget(1f, highlightIn)
        // A new touch while the last release is still bouncing: translation and stretch keep going.
        poke()
    }

    internal fun move(dx: Float, dy: Float, axes: TouchDragAxes) {
        if (!isPressed || !axes.x && !axes.y) return
        if (!dragging) {
            if (abs(dx) < TouchMotionDefaults.DeadZonePx && abs(dy) < TouchMotionDefaults.DeadZonePx) return
            dragging = true
            isDragging = true
        }
        val ax = if (axes.x) dx else 0f
        val ay = if (axes.y) dy else 0f
        val follow = TouchMotionDefaults.DragFollow
        moveX.retarget(TouchMotionMath.translation(ax, density), follow)
        moveY.retarget(TouchMotionMath.translation(ay, density), follow)
        val (sx, sy) = TouchMotionMath.deform(ax, ay, widthPx, heightPx, density)
        stretchX.retarget(sx, follow)
        stretchY.retarget(sy, follow)
        poke()
    }

    internal fun up(velocityX: Float, velocityY: Float) {
        // px/s from the tracker -> px/ms times 0.3, the unit the formulas expect (see ReleaseVelocityScale)
        val releaseX = velocityX / 1000f * TouchMotionDefaults.ReleaseVelocityScale
        val releaseY = velocityY / 1000f * TouchMotionDefaults.ReleaseVelocityScale
        if (!isPressed) return
        isPressed = false
        isDragging = false
        press.retarget(1f, if (wide) TouchMotionDefaults.WideRelease else TouchMotionDefaults.CompactRelease)
        light.retarget(0f, highlightOut)
        if (dragging) {
            val spring = TouchMotionMath.handUp(widthPx, heightPx, density)
            val maxDist = TouchMotionDefaults.MaxStretchDp * density
            fun release(value: SpringValue, fingerVelocity: Float) {
                // The release velocity (px per ms times 0.3, so next to nothing) scaled by the displacement kicks the spring;
                // only a large velocity adds bounce, and it practically never is.
                val (kick, bounce) = TouchMotionMath.releaseKick(value.value, fingerVelocity, maxDist, spring.bounce)
                value.velocity = kick
                value.retarget(0f, TouchSpring(bounce, spring.duration))
            }
            release(moveX, releaseX)
            release(moveY, releaseY)
            stretchX.velocity = 0f
            stretchY.velocity = 0f
            stretchX.retarget(1f, spring)
            stretchY.retarget(1f, spring)
        }
        dragging = false
        poke()
    }

    internal fun cancel() = up(0f, 0f)
}

/**
 * Remembers the [TouchMotionState] of one element. Pass it to [touchMotion] to read the animation (for example to draw something that
 * follows the press) or to drive it yourself.
 */
@Composable
fun rememberTouchMotionState(): TouchMotionState {
    val scope = rememberCoroutineScope()
    return remember(scope) { TouchMotionState(scope) }
}

/**
 * The ColorOS 17 touch animation.
 *
 * While the element is touched it **shrinks** (by an amount that depends on its size and shape),
 * its fill gets lighter and it follows the finger with rubber-band resistance, stretching along the
 * drag and squeezing across it. When the finger lifts it springs back with an overshoot.
 *
 * The modifier does not consume the touch while the finger only rests or moves a little (a tap still clicks), so
 * combine it with `clickable` or any gesture detector. Apply it **before** `clip` / `background` so those move with
 * it, and pass the element's [shape] so the lighter fill stays inside it.
 *
 * **Who owns a drag.** Once the finger has moved a touch slop along an axis of [claimAxes] the element *claims* the
 * gesture: the click handler inside it and every scrolling parent let go, so a list does not scroll while a button
 * is dragged. The claim is made after the handlers inside the element have had their say (a slider or a switch that
 * drags its own thumb keeps the touch), and before the parents get it. Only the Liquid Glass buttons of ColorOS drag
 * like this. List rows, cards, chips and checkboxes pass `dragAxes = TouchDragAxes.None, claimAxes = TouchDragAxes.None`:
 * they shrink and light up while held, but they neither follow the finger nor take the touch, so the page scrolls when a
 * drag starts on them.
 *
 * @param dragAxes axes along which the element follows the finger ([TouchDragAxes.None] for a pure shrink).
 * @param claimAxes axes whose drags this element takes over (see above); [TouchDragAxes.None] leaves every drag to
 * whoever else wants it.
 * @param cancelOnLeave spring back as soon as the finger leaves the element (the real toolbar buttons do this,
 * floating buttons keep their pressed state however far the finger moves).
 * @param pressedScale fixed pressed scale; by default it is derived from the element's size like ColorOS does.
 * @param highlightAlpha how much of [highlight] is mixed over the fill while pressed (0 for transparent elements).
 * @param yieldToConsumers cancel the motion as soon as another gesture handler (a scrolling parent, a
 * slider...) consumes the touch before this element claims it. Turn it off for elements that handle the drag
 * themselves, like a tab bar or a slider.
 */
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
): Modifier {
    val density = LocalDensity.current.density
    LaunchedEffect(enabled) { if (!enabled) state.cancel() }
    if (!enabled) return this
    return this
        // pointerInput sits outside the graphicsLayer, so the positions it sees are not moved by our own translation.
        .pointerInput(state, dragAxes, claimAxes, cancelOnLeave, pressedScale, yieldToConsumers, density) {
            val slop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                state.highlightIn = highlightIn
                state.highlightOut = highlightOut
                state.down(size.width, size.height, density, pressedScale)
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                var active = true
                // Once this element has claimed the gesture nobody else sees it any more.
                var claimed = false
                try {
                    while (true) {
                        // Initial pass (parents first, then children): a claimed gesture is taken away from the click
                        // handler inside this element and from every parent, up to the lift of the finger.
                        val initial = awaitPointerEvent(PointerEventPass.Initial)
                        if (claimed) initial.changes.firstOrNull { it.id == down.id }?.consume()
                        // Main pass (children first, then this element, then the parents): the children keep first
                        // call (a slider moving its thumb), what they leave goes to this element before a scroll
                        // container can start.
                        val main = awaitPointerEvent(PointerEventPass.Main)
                        if (active && !claimed && (claimAxes.x || claimAxes.y)) {
                            val c = main.changes.firstOrNull { it.id == down.id }
                            if (c != null && c.pressed && !c.isConsumed) {
                                val dx = c.position.x - down.position.x
                                val dy = c.position.y - down.position.y
                                if (max(abs(dx), abs(dy)) >= slop && (if (abs(dx) >= abs(dy)) claimAxes.x else claimAxes.y)) {
                                    claimed = true
                                    c.consume()
                                }
                            }
                        }
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (active) {
                                val v = tracker.calculateVelocity()
                                state.up(v.x, v.y)
                                active = false
                            }
                            break
                        }
                        if (!active) continue
                        tracker.addPosition(change.uptimeMillis, change.position)
                        val moved = change.position != change.previousPosition
                        // Somebody else (a slider, a scrolling parent that was already dragging...) took over this gesture.
                        if (yieldToConsumers && moved && change.isConsumed && !claimed) {
                            state.cancel()
                            active = false
                            continue
                        }
                        if (cancelOnLeave) {
                            val p = change.position
                            if (p.x < -slop || p.y < -slop || p.x > size.width + slop || p.y > size.height + slop) {
                                state.cancel()
                                active = false
                                continue
                            }
                        }
                        state.move(change.position.x - down.position.x, change.position.y - down.position.y, dragAxes)
                    }
                } finally {
                    if (active) state.cancel()
                }
            }
        }
        .graphicsLayer {
            scaleX = state.scaleX
            scaleY = state.scaleY
            translationX = state.translationX
            translationY = state.translationY
        }
        .then(
            if (highlightAlpha > 0f) {
                Modifier.drawWithContent {
                    drawContent()
                    val level = state.highlight
                    if (level > 0f) {
                        drawOutline(
                            outline = shape.createOutline(size, layoutDirection, this),
                            color = highlight,
                            alpha = (highlightAlpha * level).coerceIn(0f, 1f),
                        )
                    }
                }
            } else {
                Modifier
            },
        )
}
