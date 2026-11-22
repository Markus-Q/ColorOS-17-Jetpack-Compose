package dev.irisui.core

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Easing curves used by the transitions of the kit. [Decelerate] starts fast and lands softly (page pushes and popups).
 */
object IrisEasing {
    /** Fast start, long soft landing. Used for page pushes and popups. */
    val Decelerate = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Accelerate = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}

/** Animation specs shared by every component so the whole kit moves the same way. */
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
    private fun response(seconds: Float) = (2.0 * Math.PI / seconds).let { (it * it).toFloat() }

    /** Spring of a page push (new page in, old page out). */
    fun <T> pageSpec(): FiniteAnimationSpec<T> = spring(dampingRatio = pageDamping, stiffness = response(pageResponseSeconds))

    /** Spring of a page pop (top page out, page underneath back in). */
    fun <T> pagePopSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = pagePopDamping, stiffness = response(pagePopResponseSeconds))

    /** Spring after the finger lets go of the back gesture. */
    fun <T> pageGestureSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = response(pageGestureResponseSeconds))
    fun <T> fade(): FiniteAnimationSpec<T> = tween(fadeDurationMillis, easing = IrisEasing.Standard)
}
