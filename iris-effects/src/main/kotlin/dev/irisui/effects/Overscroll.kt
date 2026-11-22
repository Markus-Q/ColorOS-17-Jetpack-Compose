package dev.irisui.effects

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.tanh

/**
 * Overscroll of the stock apps instead of the Android stretch: when a list is pulled past its end the content simply
 * translates, and springs back when the finger lets go. Measured on Settings by recording the screen:
 *
 *  * the content follows 24 % of a slow pull (0.5 px per ms at 480 dpi) and up to 34 % of a fast one, the offset stays
 *    where it is while the finger rests,
 *  * after the release it returns on an ease-out of about 0.7 s (cubic-bezier 0.2, 0.62, 0.17, 1.05); a fling that hits the
 *    end throws the content out by up to about 280 px first, and the same curve brings it back.
 *
 * What the effect promises (the contract of [OverscrollEffect]):
 *
 *  * It only ever reacts to what the list could *not* take. A fling that runs out on its own in the middle of a list
 *    leaves nothing, so nothing bounces; `performFling` answers with the velocity the list *used*, and the bounce is built
 *    from the rest (an earlier version read it the other way round and threw the content out after every fling).
 *  * A finger on the content always wins: it stops a spring-back where it is and drags on from there.
 *  * The spring-back can never leave the content displaced, not even when the scope that runs it is torn down.
 *  * `performScroll` and `performFling` are each called exactly once per event, so nested scrolling parents keep working.
 *
 * Provide it for a whole app with `CompositionLocalProvider(LocalOverscrollFactory provides IrisOverscrollFactory)`
 * (`IrisApp` does).
 */
object IrisOverscrollFactory : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = IrisOverscrollEffect()

    override fun hashCode(): Int = 17

    override fun equals(other: Any?): Boolean = other === this
}

internal class IrisOverscrollEffect : OverscrollEffect {
    /** Offsets of the content in px; positive means the content has been pulled down / to the right. */
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set

    /** Pixels per dp, handed over by the node when it is attached (the constants below are measured at 3 px per dp). */
    var density = 3f

    private var lastEventNanos = 0L
    private var pullSpeed = 0.5f // px per ms, smoothed

    /** The running throw / spring-back, and a counter that tells whether it is still the one in charge. */
    private var animation: Job? = null
    private var generation = 0

    override val isInProgress: Boolean
        get() = offsetX != 0f || offsetY != 0f

    override val node: DelegatableNode = OverscrollNode(this)

    /** Share of the finger movement that reaches the content: 24 % for a slow pull, up to 34 % for a fast one. */
    private fun gain(): Float {
        val speed = (pullSpeed / density).coerceAtLeast(0f) // dp per ms
        return (0.238f + 0.108f * tanh((speed - 0.167f).coerceAtLeast(0f) / 0.33f))
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        // The frames of a fling and programmatic scrolling belong to the list. When a fling runs into the end the list
        // takes less than it was offered, DefaultFlingBehavior stops there, and applyToFling turns what is left into the
        // bounce.
        if (source != NestedScrollSource.UserInput) return performScroll(delta)

        // A finger on the content takes over from a running throw / spring-back, which simply stops where it is.
        stopAnimation()

        val now = System.nanoTime()
        if (lastEventNanos != 0L) {
            val ms = ((now - lastEventNanos) / 1_000_000f).coerceAtLeast(1f)
            pullSpeed = 0.6f * pullSpeed + 0.4f * (hypot(delta.x, delta.y) / ms)
        }
        lastEventNanos = now
        val g = gain()

        var restX = delta.x
        var restY = delta.y
        // Moving back against an existing pull first lets the pull go, and only what is left of the movement scrolls.
        if (offsetX != 0f && restX != 0f && sign(restX) != sign(offsetX)) {
            val next = offsetX + restX * g
            if (sign(next) == sign(offsetX)) {
                offsetX = next
                restX = 0f
            } else {
                restX = next / g
                offsetX = 0f
            }
        }
        if (offsetY != 0f && restY != 0f && sign(restY) != sign(offsetY)) {
            val next = offsetY + restY * g
            if (sign(next) == sign(offsetY)) {
                offsetY = next
                restY = 0f
            } else {
                restY = next / g
                offsetY = 0f
            }
        }

        // The list is asked exactly once, even when there is nothing left to scroll with.
        val consumed = performScroll(Offset(restX, restY))

        // What the list could not take is pulled into the overscroll (rounding noise is not).
        val overX = restX - consumed.x
        val overY = restY - consumed.y
        if (abs(overX) > Noise) offsetX += overX * g
        if (abs(overY) > Noise) offsetY += overY * g
        return delta
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        lastEventNanos = 0L
        pullSpeed = 0.5f
        stopAnimation()

        if (abs(offsetX) > Rest || abs(offsetY) > Rest) {
            // Let go while the content is pulled out: there is nothing to fling, it only springs back. The list still
            // gets its (empty) fling so that the scrolling parents see the end of the gesture.
            performFling(Velocity.Zero)
            settle(0f, 0f)
            return
        }
        offsetX = 0f
        offsetY = 0f

        // performFling answers with the velocity the list *used*. What it could not use (the fling ran into the end of the
        // list) is what bounces; a fling that fades out inside the list leaves nothing.
        val used = performFling(velocity)
        val left = velocity - used
        if (abs(left.x) * FlingToOffset > Rest || abs(left.y) * FlingToOffset > Rest) settle(left.x, left.y)
    }

    private fun stopAnimation() {
        generation++
        animation?.cancel()
        animation = null
    }

    /**
     * Throws the content out by what is left of a fling and lets it return. Ends with the content at rest. When a finger
     * takes over in between ([stopAnimation]) the animation just stops and the content stays where the finger has it.
     */
    private suspend fun settle(leftX: Float, leftY: Float) = coroutineScope {
        val mine = ++generation
        val job = launch {
            coroutineScope {
                launch { bounce(offsetX, leftX) { offsetX = it } }
                launch { bounce(offsetY, leftY) { offsetY = it } }
            }
        }
        animation = job
        try {
            job.join()
        } finally {
            if (animation === job) animation = null
            // Finished, or the scope that runs it was torn down: never leave the content displaced.
            if (generation == mine) {
                offsetX = 0f
                offsetY = 0f
            }
        }
    }

    /** Pushes the content out by what is left of a fling (when there is any), then lets it return. */
    private suspend fun bounce(start: Float, leftoverVelocity: Float, set: (Float) -> Unit) {
        var from = start
        val limit = MaxBounceDp * density
        val peak = (leftoverVelocity * FlingToOffset).coerceIn(-limit, limit)
        if (start == 0f && abs(peak) > Rest) {
            animate(0f, peak, animationSpec = tween(RiseMillis, easing = FastOutSlowInEasing)) { v, _ -> set(v) }
            from = peak
        }
        if (from != 0f) animate(from, 0f, animationSpec = tween(ReturnMillis, easing = ReturnEasing)) { v, _ -> set(v) }
        set(0f)
    }

    private companion object {
        /** The way back is an ease-out of about 0.7 s (cubic-bezier 0.2, 0.62, 0.17, 1.05), measured on Settings. */
        val ReturnEasing = CubicBezierEasing(0.2f, 0.62f, 0.17f, 1.05f)
        const val ReturnMillis = 700
        const val RiseMillis = 60

        /** Largest throw of a fling: 107 dp (320 px at 480 dpi). */
        const val MaxBounceDp = 106.7f

        /** Throw per px/s of leftover velocity, in seconds (measured: a hard fling at the end throws about 280 px). */
        const val FlingToOffset = 0.047f

        /** Below this (px) the content counts as at rest, and a leftover this small is not worth a bounce. */
        const val Rest = 0.5f

        /** Rounding noise of the list's scroll accounting (px). */
        const val Noise = 0.05f
    }
}

private class OverscrollNode(private val effect: IrisOverscrollEffect) : Modifier.Node(), LayoutModifierNode {
    override fun onAttach() {
        effect.density = requireDensity().density
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                translationX = effect.offsetX
                translationY = effect.offsetY
            }
        }
    }
}
