package dev.irisui.inputs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisTheme
import dev.irisui.effects.IrisHaptic
import dev.irisui.effects.LocalColorOSHaptics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs

private val ToggleSpring = spring<Float>(dampingRatio = 0.7f, stiffness = (2.0 * PI / 0.4).let { (it * it).toFloat() })
private fun responseSpring(response: Float) =
    spring<Float>(dampingRatio = 1f, stiffness = (2.0 * PI / response).let { (it * it).toFloat() })

private val PressIn = responseSpring(0.25f)
private val PressOut = responseSpring(0.21f)

/** The trailing edge of the thumb starts moving this long after the leading edge (fitted to the real switch: 26-29 ms). */
private const val TRAILING_EDGE_DELAY_MS = 29L

/** Inside scrolling lists the press look waits this long, so a scroll that starts on the switch never flashes it. */
private const val PRESS_VISUAL_DELAY_MS = 100L

/**
 * ColorOS switch: 44 x 24 dp track, 18 dp white thumb with 3 dp margins.
 *
 * A tap flips it: the thumb is a pill between two edges that both follow the toggle spring (bounce 0.3, 0.4 s); the
 * trailing edge starts about 29 ms after the leading one, so the thumb stretches while it travels and squeezes a little
 * when it overshoots. The thumb can also be dragged: it follows the finger one to one, the state flips (with the toggle
 * haptic) when it crosses the middle, and on release it springs to the end of the current state. The track lightens by
 * the press overlay while held and cross-fades from grey to the accent color together with the trailing edge.
 */
@Composable
fun IrisSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = IrisTheme.colors
    val haptics = LocalColorOSHaptics.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val density = LocalDensity.current
    val thumbPx = with(density) { 18.dp.toPx() }
    val travelPx = with(density) { 20.dp.toPx() }
    val left = remember { Animatable(if (checked) travelPx else 0f) }
    val right = remember { Animatable((if (checked) travelPx else 0f) + thumbPx) }
    val light = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)

    LaunchedEffect(checked) {
        // While a finger drags the thumb it already shows the new state; the release settles it.
        if (dragging) return@LaunchedEffect
        val targetLeft = if (checked) travelPx else 0f
        val targetRight = targetLeft + thumbPx
        if (left.targetValue == targetLeft && right.targetValue == targetRight) return@LaunchedEffect
        val v0 = (if (checked) 1f else -1f) * 2f * travelPx
        coroutineScope {
            val leading = if (checked) right else left
            val trailing = if (checked) left else right
            val leadingTarget = if (checked) targetRight else targetLeft
            val trailingTarget = if (checked) targetLeft else targetRight
            launch { leading.animateTo(leadingTarget, ToggleSpring, v0) }
            launch {
                delay(TRAILING_EDGE_DELAY_MS)
                trailing.animateTo(trailingTarget, ToggleSpring, v0)
            }
        }
    }
    LaunchedEffect(pressed) { light.animateTo(if (pressed) 1f else 0f, if (pressed) PressIn else PressOut) }

    val overlay = colors.pressOverlay
    val trackOff = colors.switchTrackOff
    val trackOn = colors.accent
    Canvas(
        modifier
            .size(44.dp, 24.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                if (!enabled || onCheckedChange == null) disabled()
                onClick {
                    currentOnChange?.invoke(!currentChecked)
                    true
                }
            }
            .pointerInput(enabled, onCheckedChange == null) {
                if (!enabled || onCheckedChange == null) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val pressJob = scope.launch {
                        delay(PRESS_VISUAL_DELAY_MS)
                        pressed = true
                    }
                    val startThumb = left.value
                    var isDrag = false
                    var cancelled = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || change.isConsumed) {
                            cancelled = true
                            break
                        }
                        val dx = change.position.x - down.position.x
                        if (!isDrag && abs(dx) > touchSlop) {
                            isDrag = true
                            dragging = true
                            pressed = true
                        }
                        if (isDrag) {
                            // The thumb already followed the finger below the touch slop, so there is no jump here.
                            val thumb = (startThumb + dx).coerceIn(0f, travelPx)
                            scope.launch {
                                left.snapTo(thumb)
                                right.snapTo(thumb + thumbPx)
                            }
                            val target = thumb > travelPx / 2f
                            if (target != currentChecked) {
                                haptics.perform(if (target) IrisHaptic.ToggleOn else IrisHaptic.ToggleOff)
                                currentOnChange?.invoke(target)
                            }
                            change.consume()
                        } else if (abs(dx) > 1.5f) {
                            // Small horizontal wobble: the thumb follows from the first pixel on, like the real one.
                            val thumb = (startThumb + dx).coerceIn(0f, travelPx)
                            scope.launch {
                                left.snapTo(thumb)
                                right.snapTo(thumb + thumbPx)
                            }
                        }
                        if (!change.pressed) {
                            // A tap on the switch itself is handled here, the row around it must not toggle it a second time.
                            change.consume()
                            break
                        }
                    }
                    pressJob.cancel()
                    pressed = false
                    if (isDrag || cancelled) {
                        // Let go (or lose the touch to a scroll): settle at the end of whatever state we are in.
                        scope.launch {
                            val targetLeft = if (currentChecked) travelPx else 0f
                            coroutineScope {
                                launch { left.animateTo(targetLeft, ToggleSpring) }
                                launch { right.animateTo(targetLeft + thumbPx, ToggleSpring) }
                            }
                        }
                        dragging = false
                    } else {
                        haptics.perform(if (!currentChecked) IrisHaptic.ToggleOn else IrisHaptic.ToggleOff)
                        currentOnChange?.invoke(!currentChecked)
                    }
                }
            },
    ) {
        val margin = 3.dp.toPx()
        val corner = CornerRadius(size.height / 2f)
        // The color follows the trailing edge: it is done when the thumb has fully left its old spot.
        val progress = ((if (checked) left.value else right.value - thumbPx) / travelPx).coerceIn(0f, 1f)
        drawRoundRect(lerp(trackOff, trackOn, progress), cornerRadius = corner)
        val pressLevel = light.value
        if (pressLevel > 0f) {
            drawRoundRect(overlay.copy(alpha = (overlay.alpha * pressLevel).coerceIn(0f, 1f)), cornerRadius = corner)
        }
        val tl = margin + left.value
        val tr = margin + right.value
        val width = tr - tl
        // A very soft shadow, as faint as the real thumb's.
        for ((grow, a) in listOf(2.0f to 0.02f, 1.2f to 0.035f, 0.5f to 0.05f)) {
            val g = grow.dp.toPx()
            drawRoundRect(
                Color.Black.copy(alpha = a),
                topLeft = Offset(tl - g, margin - g + 1.dp.toPx()),
                size = Size(width + 2 * g, thumbPx + 2 * g),
                cornerRadius = CornerRadius((thumbPx + 2 * g) / 2f),
            )
        }
        drawRoundRect(
            Color.White,
            topLeft = Offset(tl, margin),
            size = Size(width.coerceAtLeast(1f), thumbPx),
            cornerRadius = CornerRadius(thumbPx / 2f),
        )
    }
}
