package dev.irisui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView

/**
 * Lets the system tell a single-activity app where the finger is during the edge back gesture, so that
 * `PredictiveBackHandler` can animate with the finger (the page follows it 1:1). [IrisPageHost] calls this by itself;
 * call it yourself once near the root of your content when you build the back animation on your own navigation:
 *
 * ```
 * IrisBackGestureTracking()
 * PredictiveBackHandler(enabled = canGoBack) { events ->
 *     events.collect { event -> pageOffset = event.travelFromEdge(widthPx) }   // touchX is real now
 *     goBack()
 * }
 * ```
 *
 * **Why it is needed.** Since Android 16 the system leaves the progress of the gesture to the app when the window promises
 * to compute it from the touches it receives (`BackNavigationInfo.isAppProgressGenerationAllowed`: the window asked for it
 * and its frame equals the display bounds). ColorOS 17 (checked on a OnePlus 15, Android 17) keeps the touches of the edge
 * gesture away from the app, so nothing is ever computed: the callback gets the start of the gesture (`touchX` about
 * 10 px, progress 0) and then nothing until the release. The system hands the progress out itself again as soon as the
 * window frame differs from the display, which is what this does: the window gets one pixel more width, outside of the
 * screen (`FLAG_LAYOUT_NO_LIMITS`). Checked on the phone: the layout does not move by a single pixel, and every move of
 * the finger now arrives with its position (about 90 events for a slow swipe across the screen).
 *
 * It does nothing before Android 16 (the system always sends the progress there), in multi-window and when the window
 * does not fill the display (the frame differs anyway), and it puts the window back when it leaves the composition.
 * The app also has to opt in to predictive back (`android:enableOnBackInvokedCallback`); the manifest of this library
 * already does that for the app it is merged into.
 */
@Composable
fun IrisBackGestureTracking(enabled: Boolean = true) {
    val view = LocalView.current
    val configuration = LocalConfiguration.current // a new size or window mode applies it again
    DisposableEffect(view, configuration, enabled) {
        val restore = if (enabled && !view.isInEditMode) widenWindow(view) else null
        onDispose { restore?.invoke() }
    }
}

/**
 * How far the finger has come from the edge this gesture started at, in px: `touchX` for a swipe from the left edge,
 * `width - touchX` for one from the right edge. ColorOS slides the page out to the right either way, so the page offset
 * is this distance whichever side the finger comes from (the page of the stock apps is not mirrored for the right edge).
 */
fun BackEventCompat.travelFromEdge(width: Float): Float =
    if (swipeEdge == BackEventCompat.EDGE_RIGHT) width - touchX else touchX

private fun widenWindow(view: View): (() -> Unit)? {
    if (Build.VERSION.SDK_INT < 36) return null
    val activity = view.context.findActivity() ?: return null
    if (activity.isInMultiWindowMode) return null
    val windowManager = activity.windowManager
    val display = windowManager.maximumWindowMetrics.bounds
    if (windowManager.currentWindowMetrics.bounds != display) return null

    val window = activity.window
    val before = window.attributes
    val oldWidth = before.width
    val hadNoLimits = before.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS != 0
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    window.setLayout(display.width() + 1, before.height)
    return {
        window.setLayout(oldWidth, window.attributes.height)
        if (!hadNoLimits) window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
