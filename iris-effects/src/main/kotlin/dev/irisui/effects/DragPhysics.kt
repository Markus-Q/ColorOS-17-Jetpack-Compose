package dev.irisui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.irisui.core.IrisTheme
import dev.irisui.core.rubberBand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State of a freely draggable element. It follows the finger with rubber band resistance,
 * leans into the movement, lifts while held and springs back with the release velocity.
 */
@Stable
class PhysicsDragState internal constructor(private val scope: CoroutineScope) {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    val scale = Animatable(1f)
    val rotation = Animatable(0f)

    var isDragging by mutableStateOf(false)
        private set

    private var raw = Offset.Zero

    internal fun start(liftScale: Float) {
        isDragging = true
        raw = offset.value
        scope.launch { offset.stop() }
        scope.launch { scale.animateTo(liftScale, spring(dampingRatio = 0.6f, stiffness = 500f)) }
    }

    internal fun drag(delta: Offset, limitPx: Float, maxTilt: Float) {
        raw += delta
        val target = Offset(rubberBand(raw.x, limitPx), rubberBand(raw.y, limitPx))
        val tiltTarget = (delta.x * 1.4f).coerceIn(-maxTilt, maxTilt)
        scope.launch {
            offset.snapTo(target)
            rotation.snapTo(rotation.value + (tiltTarget - rotation.value) * 0.25f)
        }
    }

    internal fun release(velocity: Velocity) {
        isDragging = false
        raw = Offset.Zero
        scope.launch {
            offset.animateTo(
                Offset.Zero,
                spring(dampingRatio = 0.52f, stiffness = 210f),
                initialVelocity = Offset(velocity.x, velocity.y),
            )
        }
        scope.launch { scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f)) }
        scope.launch { rotation.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 160f)) }
    }
}

/** Remembers the [PhysicsDragState] of one element that uses [physicsDrag]. */
@Composable
fun rememberPhysicsDragState(): PhysicsDragState {
    val scope = rememberCoroutineScope()
    return remember(scope) { PhysicsDragState(scope) }
}

/**
 * Makes the element draggable in any direction with physics: rubber band past [maxDistance],
 * a lift while held, tilt from the drag direction and a springy return. Haptics fire on grab and drop.
 */
@Composable
fun Modifier.physicsDrag(
    state: PhysicsDragState = rememberPhysicsDragState(),
    maxDistance: Dp = 140.dp,
    liftScale: Float = 1.06f,
    maxTilt: Float = 9f,
): Modifier {
    val haptics = LocalColorOSHaptics.current
    val limit = with(LocalDensity.current) { maxDistance.toPx() }
    // pointerInput sits outside the graphicsLayer so positions are not affected by our own translation.
    return this
        .pointerInput(state, limit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                val slop = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                if (slop != null) {
                    state.start(liftScale)
                    haptics.perform(IrisHaptic.DragStart)
                    tracker.addPosition(slop.uptimeMillis, slop.position)
                    drag(slop.id) { change ->
                        // Read the movement before consuming: positionChange() is zero for a consumed change
                        val delta = change.position - change.previousPosition
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        state.drag(delta, limit, maxTilt)
                    }
                    state.release(tracker.calculateVelocity())
                    haptics.perform(IrisHaptic.DragEnd)
                }
            }
        }
        .graphicsLayer {
            translationX = state.offset.value.x
            translationY = state.offset.value.y
            scaleX = state.scale.value
            scaleY = state.scale.value
            rotationZ = state.rotation.value
        }
}

/**
 * A column whose items can be re-ordered by dragging their [handle]. Items shift out of the way
 * with springs, the dragged item lifts and follows the finger, and it settles into its slot on release.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, isDragging: Boolean, handle: Modifier) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalColorOSHaptics.current
    val motion = IrisTheme.motion
    val heights = remember { mutableStateMapOf<Any, Int>() }
    val shifts = remember { mutableMapOf<Any, Animatable<Float, AnimationVector1D>>() }
    val dragOffset = remember { Animatable(0f) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    var rawOffset by remember { mutableFloatStateOf(0f) }
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentKey by rememberUpdatedState(key)

    fun heightAt(i: Int): Int = heights[currentKey(currentItems[i])] ?: 0
    fun shiftOf(k: Any) = shifts.getOrPut(k) { Animatable(0f) }

    fun computeTarget(offset: Float): Int {
        val list = currentItems
        val d = draggingIndex
        if (d !in list.indices) return d
        val tops = FloatArray(list.size)
        var top = 0f
        for (i in list.indices) {
            tops[i] = top
            top += heightAt(i)
        }
        val center = tops[d] + heightAt(d) / 2f + offset
        if (center < 0f) return 0
        if (center >= top) return list.lastIndex
        for (i in list.indices) {
            if (center >= tops[i] && center < tops[i] + heightAt(i)) return i
        }
        return d
    }

    fun applyShifts() {
        val d = draggingIndex
        val t = targetIndex
        val hd = if (d in currentItems.indices) heightAt(d).toFloat() else 0f
        currentItems.forEachIndexed { i, item ->
            val desired = when {
                d < 0 || i == d -> 0f
                i in (d + 1)..t -> -hd
                i in t until d -> hd
                else -> 0f
            }
            val anim = shiftOf(currentKey(item))
            scope.launch { anim.animateTo(desired, spring(dampingRatio = 0.8f, stiffness = 500f)) }
        }
    }

    fun settle() {
        val d = draggingIndex
        val t = targetIndex
        if (d < 0) return
        scope.launch {
            val finalOffset = when {
                t > d -> ((d + 1)..t).sumOf { heightAt(it) }.toFloat()
                t < d -> -(t until d).sumOf { heightAt(it) }.toFloat()
                else -> 0f
            }
            dragOffset.animateTo(finalOffset, spring(dampingRatio = 0.68f, stiffness = 420f))
            if (t != d) currentOnMove(d, t)
            // Reset everything in the same frame the new order appears, so nothing jumps.
            shifts.values.forEach { it.snapTo(0f) }
            dragOffset.snapTo(0f)
            draggingIndex = -1
            targetIndex = -1
            rawOffset = 0f
            haptics.perform(IrisHaptic.DragEnd)
        }
    }

    Column(modifier) {
        items.forEachIndexed { index, item ->
            val k = key(item)
            key(k) {
                val dragging = index == draggingIndex
                val lift by animateFloatAsState(if (dragging) 1.035f else 1f, motion.press, label = "reorderLift")
                val shift = shiftOf(k)
                val handle = Modifier.pointerInput(index) {
                    detectDragGestures(
                        onDragStart = {
                            if (draggingIndex < 0) {
                                draggingIndex = index
                                targetIndex = index
                                rawOffset = 0f
                                haptics.perform(IrisHaptic.DragStart)
                                scope.launch { dragOffset.snapTo(0f) }
                            }
                        },
                        onDrag = { change, amount ->
                            if (draggingIndex == index) {
                                change.consume()
                                rawOffset += amount.y
                                val o = rawOffset
                                scope.launch { dragOffset.snapTo(o) }
                                val t = computeTarget(o)
                                if (t != targetIndex) {
                                    targetIndex = t
                                    haptics.perform(IrisHaptic.Selection)
                                    applyShifts()
                                }
                            }
                        },
                        onDragEnd = { if (draggingIndex == index) settle() },
                        onDragCancel = { if (draggingIndex == index) settle() },
                    )
                }
                Box(
                    Modifier
                        .onSizeChanged { heights[k] = it.height }
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) dragOffset.value else shift.value
                            scaleX = lift
                            scaleY = lift
                        },
                ) {
                    itemContent(item, dragging, handle)
                }
            }
        }
    }
}
