package dev.irisui.inputs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisIcon
import dev.irisui.core.IrisTheme
import dev.irisui.effects.IrisHaptic
import dev.irisui.effects.LocalColorOSHaptics
import dev.irisui.effects.TouchDragAxes
import dev.irisui.effects.touchMotion
import kotlinx.coroutines.channels.Channel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt

/** A spring described by its [response] in seconds and its [bounce] (0 = critically damped). */
private fun responseSpring(response: Float, bounce: Float = 0f): SpringSpec<Float> = spring(
    dampingRatio = 1f - bounce,
    stiffness = (2.0 * Math.PI / response).let { (it * it).toFloat() },
    visibilityThreshold = 0.0002f,
)

// The thumb swells while it is held and shrinks smoothly (no bounce) when it is let go. Measured on the Icons page of
// the stock theme app by recording the screen: 36 px to 46 px, 90 % after 150 ms; back to rest in about 300 ms.
private val DotPressIn = responseSpring(0.25f)
private val DotPressOut = responseSpring(0.45f)

/** The light spot under the finger fades in within 100 ms and out over about 350 ms (measured on the same page). */
private val LightIn = responseSpring(0.14f)
private val LightOut = responseSpring(0.45f)

/** `ClickAnimationHandler`: a tap on a continuous bar slides the thumb to the tapped spot (response 0.3 s, no bounce). */
private const val ClickResponse = 0.3f

/** `FlexibleFollowHandHandler`: while it is dragged, the thumb trails the finger (response 0.15 s, no bounce). */
private const val FollowResponse = 0.15f

/** Slide of a stepped slider to a mark (after release or a tap): an ease-in-out of 130 to 200 ms, longer for longer trips. */
private val SnapEasing = CubicBezierEasing(0.37f, 0.04f, 0.63f, 1f)

/** Between two marks the thumb of a stepped slider covers only 61.5 % of what the finger does, and catches up at every mark. */
private const val MarkDragRatio = 0.615f

/**
 * Position of the thumb (0..1 along its travel), moved by a critically damped spring that can be retargeted at any time
 * without restarting (a finger delivers a new target every few milliseconds, more often than frames are drawn, which
 * would starve an `Animatable` that is restarted on every event) or by a short tween.
 */
private class ThumbMotion(initial: Float) {
    var value by mutableFloatStateOf(initial)
        private set
    var target = initial
        private set
    private var velocity = 0f
    private var response = FollowResponse
    private var tween: Tween? = null
    private val wake = Channel<Unit>(Channel.CONFLATED)

    private class Tween(val from: Float, val to: Float, val millis: Int, val easing: Easing, var start: Long = -1L)

    fun springTo(newTarget: Float, responseSeconds: Float) {
        tween = null
        target = newTarget
        response = responseSeconds
        wake.trySend(Unit)
    }

    fun tweenTo(newTarget: Float, millis: Int, easing: Easing) {
        target = newTarget
        velocity = 0f
        tween = Tween(value, newTarget, millis, easing)
        wake.trySend(Unit)
    }

    suspend fun run() {
        for (signal in wake) {
            var last = 0L
            var settled = false
            while (!settled) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    last = now
                    val running = tween
                    if (running != null) {
                        if (running.start < 0L) running.start = now
                        val p = ((now - running.start) / 1_000_000f / running.millis).coerceIn(0f, 1f)
                        value = running.from + (running.to - running.from) * running.easing.transform(p)
                        if (p >= 1f) {
                            value = running.to
                            tween = null
                            settled = true
                        }
                    } else {
                        // exact step of a critically damped spring towards the current target
                        val omega = (2.0 * PI / response).toFloat()
                        val offset = value - target
                        val c = velocity + omega * offset
                        val decay = exp(-omega * dt)
                        val newOffset = (offset + c * dt) * decay
                        velocity = (velocity - omega * c * dt) * decay
                        value = target + newOffset
                        if (abs(newOffset) < 0.0002f && abs(velocity) < 0.002f) {
                            value = target
                            velocity = 0f
                            settled = true
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thick ColorOS slider, rebuilt from recordings of the volume rows of Settings and the Icons page of the theme app:
 *
 *  * The bar is 20 dp high inside a 28 dp touch area. The thumb is a white 12 dp dot whose center travels from 10 dp
 *    inside the left end to 10 dp inside the right end; the accent fill always reaches 10 dp past it.
 *  * While it is held the thumb swells to 16 dp and a soft light spot (a color dodge of up to 54 % that falls
 *    off over about 60 dp) follows it, brightening the fill and the track. Both ease back after release.
 *  * **Continuous** (steps = 0): a tap slides the thumb to the spot with a critically damped spring of 0.3 s, while it
 *    is dragged the thumb trails the finger with a 0.15 s spring.
 *  * **Stepped** (steps > 0, with [showMarks] the Rounded Corner style): a dot marks every step. Pressing the thumb
 *    and dragging moves it by 61.5 % of the finger's way, and each time the finger crosses a mark the thumb catches up
 *    with it. On release the thumb slides to the mark nearest to it, a tap on the bar slides it to the mark nearest
 *    to the tap. Without [showMarks] (the Icon size style) the thumb follows the finger 1:1 and only snaps on release.
 *
 * Every step crossed while the finger is down fires the slider haptic of the stock apps (constant 305), for a
 * continuous slider every whole percent.
 *
 * A press on the thumb starts dragging at once. A press elsewhere on the bar of a continuous slider only picks the
 * value once the finger moves sideways or lifts. The whole slider also follows a dragging finger with the rubber-band
 * physics of every touchable element (stretching along the drag, springing back with an overshoot); a swipe that goes
 * up or down from a slider belongs to the slider, not to a scrolling page behind it, and does not change the value.
 */
@Composable
fun IrisSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    hapticFeedback: Boolean = true,
    showMarks: Boolean = true,
) {
    val colors = IrisTheme.colors
    val haptics = LocalColorOSHaptics.current
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentFinished by rememberUpdatedState(onValueChangeFinished)
    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var interacting by remember { mutableStateOf(false) }
    val dotGrowth by animateFloatAsState(
        if (pressed) 1f else 0f,
        if (pressed) DotPressIn else DotPressOut,
        label = "sliderDot",
    )
    val span = valueRange.endInclusive - valueRange.start
    val stepped = steps > 0
    val gaps = steps + 1
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    // The drawn thumb position (0..1 along the travel of the thumb) and the strength of the light spot.
    val thumb = remember { ThumbMotion(fraction) }
    val light = remember { Animatable(0f) }
    LaunchedEffect(thumb) { thumb.run() }
    LaunchedEffect(pressed) {
        if (pressed) light.animateTo(1f, LightIn) else light.animateTo(0f, LightOut)
    }
    // An outside change of the value (not caused by a finger) moves the thumb: slowly for a continuous slider, with the
    // slide to a mark for a stepped one.
    LaunchedEffect(fraction, interacting) {
        if (!interacting && abs(thumb.target - fraction) > 1e-4f) {
            if (stepped) thumb.tweenTo(fraction, 160, SnapEasing) else thumb.springTo(fraction, ClickResponse)
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            // The whole slider follows a dragging finger with the rubber-band physics of every touchable element. The thumb
            // keeps its own sideways drag; a drag up or down belongs to the slider too, so a list behind it does not scroll.
            .touchMotion(
                enabled = enabled,
                pressedScale = 1f,
                claimAxes = TouchDragAxes.Vertical,
                highlightAlpha = 0f,
                yieldToConsumers = false,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (startIcon != null) IrisIcon(startIcon, null, tint = colors.content, size = 24.dp)
        Box(
            Modifier
                .weight(1f)
                .height(28.dp)
                .pointerInput(enabled, valueRange, steps, hapticFeedback, showMarks) {
                    if (!enabled) return@pointerInput
                    val inset = 4.dp.toPx()
                    val half = 10.dp.toPx() // the thumb center stays this far inside the ends of the bar
                    val reach = 20.dp.toPx()

                    fun left() = inset + half
                    fun right() = size.width - inset - half
                    fun xOf(f: Float) = left() + f * (right() - left())
                    fun fOf(x: Float) = ((x - left()) / (right() - left())).coerceIn(0f, 1f)
                    fun markIndex(f: Float) = (f * gaps).roundToInt().coerceIn(0, gaps)
                    fun valueAtMark(i: Int) = valueRange.start + span * i / gaps
                    fun step(f: Float) = if (stepped) markIndex(f) else (f * 100f).roundToInt()

                    fun slideTo(f: Float) {
                        val distance = abs(xOf(f) - xOf(thumb.value))
                        val millis = (90f + 0.28f * distance).coerceIn(128f, 200f).roundToInt()
                        thumb.tweenTo(f, millis, SnapEasing)
                    }

                    awaitEachGesture {
                        var lastStep = 0
                        var anchorFinger = 0f
                        var anchorThumb = 0f
                        var lastFinger = 0f
                        var targetF = 0f

                        fun tick(newStep: Int) {
                            if (newStep != lastStep) {
                                lastStep = newStep
                                if (hapticFeedback) haptics.perform(IrisHaptic.SliderStep)
                            }
                        }

                        // continuous: the value follows the finger
                        fun updateContinuous(x: Float) {
                            val f = fOf(x)
                            targetF = f
                            val v = valueRange.start + f * span
                            tick(step(f))
                            // dragged past an end the thumb creeps up to 5 px beyond it and springs back on release
                            val beyond = 5.dp.toPx() / (right() - left())
                            val visual = ((x - left()) / (right() - left())).coerceIn(-beyond, 1f + beyond)
                            thumb.springTo(visual, if (dragging) FollowResponse else ClickResponse)
                            if (v != currentValue) currentOnChange(v)
                        }

                        // stepped: the thumb moves relative to where the finger went down
                        fun updateStepped(x: Float) {
                            if (showMarks) {
                                for (k in 0..gaps) {
                                    val m = xOf(k / gaps.toFloat())
                                    if ((lastFinger - m) * (x - m) < 0f) {
                                        // the finger crossed a mark: the thumb catches up with it
                                        anchorFinger = x
                                        anchorThumb = x.coerceIn(left(), right())
                                    }
                                }
                                lastFinger = x
                                val thumbX = (anchorThumb + MarkDragRatio * (x - anchorFinger)).coerceIn(left(), right())
                                targetF = fOf(thumbX)
                            } else {
                                lastFinger = x
                                val thumbX = (anchorThumb + (x - anchorFinger)).coerceIn(left(), right())
                                targetF = fOf(thumbX)
                            }
                            thumb.springTo(targetF, FollowResponse)
                            val index = markIndex(targetF)
                            tick(index)
                            val v = valueAtMark(index)
                            if (v != currentValue) currentOnChange(v)
                        }

                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Where the thumb rests now (a stepped slider may still be sliding to its mark).
                        val startFraction = if (stepped) thumb.target else ((currentValue - valueRange.start) / span).coerceIn(0f, 1f)
                        lastStep = step(startFraction)
                        anchorThumb = xOf(startFraction)
                        targetF = startFraction
                        pressed = true
                        dragging = false
                        interacting = true
                        val onThumb = abs(down.position.x - xOf(startFraction)) <= reach
                        anchorFinger = down.position.x
                        lastFinger = down.position.x

                        if (stepped) {
                            if (onThumb) {
                                dragging = true
                                drag(down.id) { change ->
                                    updateStepped(change.position.x)
                                    change.consume()
                                }
                            } else {
                                val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                                    dragging = true
                                    updateStepped(change.position.x)
                                    change.consume()
                                }
                                if (slop != null) {
                                    horizontalDrag(down.id) { change ->
                                        updateStepped(change.position.x)
                                        change.consume()
                                    }
                                } else {
                                    val last = currentEvent.changes.firstOrNull { it.id == down.id }
                                    if (last != null && !last.pressed && !last.isConsumed) {
                                        // tap on the bar: the thumb goes to the mark nearest to the tap
                                        targetF = markIndex(fOf(down.position.x)) / gaps.toFloat()
                                        tick(markIndex(targetF))
                                    }
                                }
                            }
                            // let go: slide to the mark nearest to the thumb
                            val index = markIndex(targetF)
                            slideTo(index / gaps.toFloat())
                            val v = valueAtMark(index)
                            if (v != currentValue) currentOnChange(v)
                        } else if (onThumb) {
                            // Grabbed the thumb: follow the finger from the first pixel.
                            dragging = true
                            updateContinuous(down.position.x)
                            drag(down.id) { change ->
                                updateContinuous(change.position.x)
                                change.consume()
                            }
                        } else {
                            // Pressed on the bar: pick the value when the finger goes sideways or lifts.
                            val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                                dragging = true
                                updateContinuous(change.position.x)
                                change.consume()
                            }
                            if (slop != null) {
                                horizontalDrag(down.id) { change ->
                                    updateContinuous(change.position.x)
                                    change.consume()
                                }
                            } else {
                                // null: the finger lifted (a tap on the bar) or a scrolling parent took the gesture over
                                val last = currentEvent.changes.firstOrNull { it.id == down.id }
                                if (last != null && !last.pressed && !last.isConsumed) updateContinuous(down.position.x)
                            }
                        }
                        if (!stepped) thumb.springTo(targetF, ClickResponse) // back from beyond an end
                        pressed = false
                        dragging = false
                        interacting = false
                        currentFinished?.invoke()
                    }
                },
        ) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            ) {
                // The real seek bar is a 28 dp high view whose 20 dp bar is inset by 4 dp on both sides.
                val inset = 4.dp.toPx()
                val h = 20.dp.toPx()
                val top = (size.height - h) / 2f
                val centerY = size.height / 2f
                val barLeft = inset
                val barRight = size.width - inset
                val travelLeft = barLeft + h / 2f
                val travelRight = barRight - h / 2f
                val thumbX = travelLeft + thumb.value.coerceIn(-0.05f, 1.05f) * (travelRight - travelLeft)
                val fillEnd = min(barRight, thumbX + h / 2f)
                val corner = CornerRadius(h / 2f)
                val barSize = Size(barRight - barLeft, h)

                drawRoundRect(colors.trackEmpty, Offset(barLeft, top), barSize, corner)
                val markRadius = 3.dp.toPx()
                val marks = if (stepped && showMarks) (0..gaps).map { travelLeft + it / gaps.toFloat() * (travelRight - travelLeft) } else emptyList()
                // marks on the empty part of the bar
                marks.forEach { x -> if (x >= fillEnd - markRadius) drawCircle(colors.contentTertiary, markRadius, Offset(x, centerY)) }
                drawRoundRect(colors.accent, Offset(barLeft, top), Size(fillEnd - barLeft, h), corner)
                // marks on the fill
                marks.forEach { x -> if (x < fillEnd - markRadius) drawCircle(Color.White, markRadius, Offset(x, centerY)) }

                // The light spot: a color dodge that is strongest at the thumb and falls off like a gaussian.
                val spot = light.value
                if (spot > 0.003f) {
                    val radius = 160.dp.toPx()
                    val dodge = Color(0xFF595959)
                    val bar = Path().apply { addRoundRect(RoundRect(barLeft, top, barRight, top + h, corner)) }
                    clipPath(bar) {
                        drawRect(
                            Brush.radialGradient(
                                colorStops = arrayOf(
                                    0f to dodge.copy(alpha = 1f),
                                    0.125f to dodge.copy(alpha = 0.95f),
                                    0.25f to dodge.copy(alpha = 0.81f),
                                    0.375f to dodge.copy(alpha = 0.62f),
                                    0.5f to dodge.copy(alpha = 0.43f),
                                    0.625f to dodge.copy(alpha = 0.27f),
                                    0.78f to dodge.copy(alpha = 0.13f),
                                    0.94f to dodge.copy(alpha = 0.04f),
                                    1f to dodge.copy(alpha = 0f),
                                ),
                                center = Offset(thumbX, centerY),
                                radius = radius,
                            ),
                            alpha = spot,
                            blendMode = BlendMode.ColorDodge,
                        )
                    }
                }

                // 12 dp white dot, 16 dp while held (diameter x1.33), with the thumb shadow of the stock sliders.
                val dot = 6.dp.toPx() * (1f + 0.333f * dotGrowth)
                val shadowColor = if (colors.isDark) Color(0x40000000) else Color(0x1A000000)
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        // thumb shadow: 2 dp blur, 2 dp down
                        setShadowLayer(2.dp.toPx(), 0f, 2.dp.toPx(), shadowColor.toArgb())
                    }
                    canvas.nativeCanvas.drawCircle(thumbX, centerY, dot, paint)
                }
            }
        }
        if (endIcon != null) IrisIcon(endIcon, null, tint = colors.contentSecondary, size = 24.dp)
    }
}
