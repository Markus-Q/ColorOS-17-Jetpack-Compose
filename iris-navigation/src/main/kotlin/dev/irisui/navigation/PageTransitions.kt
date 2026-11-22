package dev.irisui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisMotion
import dev.irisui.core.IrisTheme
import dev.irisui.core.SmoothRoundedCornerShape
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** What the last navigation did: a page was pushed or popped (see [PageStack.lastAction]). */
enum class PageAction { Push, Pop }

internal enum class Phase { Entering, Idle, Leaving }

/** A page of the stack together with the animation state of the host: [enter] is 0 off screen, 1 settled on top. */
@Stable
internal class PageEntry<T : Any>(val id: Long, val page: T, entering: Boolean) {
    val enter = Animatable(if (entering) 0f else 1f)

    /** Share (0..1) of the dimming scrim this page casts on the page below it, for the animated (not dragged) case. */
    val scrim = Animatable(if (entering) 0f else 1f)
    var phase by mutableStateOf(if (entering) Phase.Entering else Phase.Idle)

    /** True while the back gesture drags this page, and during the release animation that follows. */
    var viaGesture by mutableStateOf(false)
}

/** Minimal back stack for [IrisPageHost]. Pages can be any value (enum, sealed class, ...). */
@Stable
class PageStack<T : Any>(initial: T) {
    private var nextId = 0L
    internal val entries = mutableStateListOf(PageEntry(nextId++, initial, entering = false))

    /** Popped pages that are still sliding out. */
    internal val ghosts = mutableStateListOf<PageEntry<T>>()

    val current: T get() = entries.last().page
    val canPop: Boolean get() = entries.size > 1
    val depth: Int get() = entries.size

    /** What happened last; kept for apps that want to react to the direction of the last transition. */
    var lastAction by mutableStateOf(PageAction.Push)
        private set

    fun push(page: T) {
        lastAction = PageAction.Push
        entries.add(PageEntry(nextId++, page, entering = true))
    }

    fun pop(): Boolean = pop(viaGesture = false)

    internal fun pop(viaGesture: Boolean): Boolean {
        if (!canPop) return false
        lastAction = PageAction.Pop
        val entry = entries.removeAt(entries.lastIndex)
        entry.viaGesture = viaGesture
        entry.phase = Phase.Leaving
        ghosts.add(entry)
        return true
    }
}

/**
 * Remembers a [PageStack] that starts with [initial]. The stack lives in memory only (a plain `remember`): keep it alive over
 * configuration changes yourself or rebuild it from your own saved state.
 */
@Composable
fun <T : Any> rememberPageStack(initial: T): PageStack<T> = remember { PageStack(initial) }

/** While a page is dragged away by the back gesture the page below drifts only a quarter of the width. */
private const val GesturePageParallax = 0.25f

/** The display corner radius: the page that follows the finger is clipped to the rounded corners of the screen. */
private val GestureCorners = SmoothRoundedCornerShape(42.dp, 0.dp, 0.dp, 42.dp, smoothing = 0.6f)

/**
 * Hosts the pages of a [PageStack] with the ColorOS page transition and the back gesture, measured on the Settings app
 * of a OnePlus 15:
 *
 *  * **Push:** the new page slides in from the right on a spring (damping 0.8, response 0.33 s) while the page below
 *    drifts 30 % of the screen width to the left on the same progress and is darkened by a black scrim that ramps up to
 *    50 % within 185 ms.
 *  * **Pop:** the same motion backwards, but the spring is critically damped (response 0.35 s) and the scrim fades out
 *    immediately.
 *  * **Back gesture:** the top page follows the finger 1:1 and is clipped to the rounded display corners; the page
 *    below drifts a quarter of the width and its scrim fades out linearly with the distance. Releasing past the
 *    system threshold (45 dp) slides the page out, releasing earlier springs it back, both critically damped with a
 *    response of 0.31 s. The page always moves from left to right, also when the gesture starts at the right edge:
 *    the further the finger has travelled from its edge, the further the page is out.
 *
 * Pages below the top one are not kept in the composition, but their `rememberSaveable` state (scroll positions, text
 * of fields) survives while they are covered. The system back button pops the stack.
 *
 * [trackBackGesture] makes the system report the finger position during the back gesture to a single-activity app
 * (see [IrisBackGestureTracking]); without it ColorOS only tells the app where the gesture started and the page can
 * just be animated after the release.
 */
@Composable
fun <T : Any> IrisPageHost(
    stack: PageStack<T>,
    modifier: Modifier = Modifier,
    trackBackGesture: Boolean = true,
    content: @Composable (T) -> Unit,
) {
    val motion = IrisTheme.motion
    val background = IrisTheme.colors.background
    val saveable = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    IrisBackGestureTracking(enabled = trackBackGesture)

    BoxWithConstraints(modifier.fillMaxSize().background(background)) {
        val width by rememberUpdatedState(constraints.maxWidth.toFloat())

        PredictiveBackHandler(enabled = stack.canPop) { events ->
            val entry = stack.entries.last()
            entry.phase = Phase.Idle // stops a push that is still running
            entry.viaGesture = true
            try {
                events.collect { event ->
                    // How far the finger has come from the edge it started at; the page goes right whichever edge that is.
                    entry.enter.snapTo(1f - (event.travelFromEdge(width) / width).coerceIn(0f, 1f))
                }
                stack.pop(viaGesture = true) // committed: slide out from where the finger let go
            } catch (cancelled: CancellationException) {
                scope.launch {
                    entry.enter.animateTo(1f, motion.pageGestureSpec())
                    entry.viaGesture = false
                }
                throw cancelled
            }
        }

        // The page below only has to exist while the top page is not settled yet.
        val showBelow by remember(stack) {
            derivedStateOf { stack.entries.size >= 2 && stack.entries.last().enter.value < 0.9995f }
        }
        val layers = buildList {
            if (showBelow) add(stack.entries[stack.entries.size - 2])
            add(stack.entries.last())
            addAll(stack.ghosts)
        }
        layers.forEachIndexed { index, entry ->
            key(entry.id) {
                PageLayer(
                    entry = entry,
                    cover = layers.getOrNull(index + 1),
                    motion = motion,
                    background = background,
                    onGone = {
                        stack.ghosts.remove(entry)
                        saveable.removeState(entry.id)
                    },
                    content = { saveable.SaveableStateProvider(entry.id) { content(entry.page) } },
                )
            }
        }
    }
}

@Composable
private fun <T : Any> PageLayer(
    entry: PageEntry<T>,
    cover: PageEntry<T>?,
    motion: IrisMotion,
    background: Color,
    onGone: () -> Unit,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(entry, entry.phase) {
        when (entry.phase) {
            Phase.Entering -> {
                launch {
                    entry.scrim.animateTo(
                        1f,
                        tween(motion.pageDimDurationMillis, motion.pagePushDimDelayMillis, LinearEasing),
                    )
                }
                entry.enter.animateTo(1f, motion.pageSpec())
                entry.phase = Phase.Idle
            }
            Phase.Leaving -> {
                if (!entry.viaGesture) {
                    launch { entry.scrim.animateTo(0f, tween(motion.pageDimDurationMillis, 0, LinearEasing)) }
                }
                entry.enter.animateTo(0f, if (entry.viaGesture) motion.pageGestureSpec() else motion.pagePopSpec())
                entry.viaGesture = false
                onGone()
            }
            Phase.Idle -> Unit
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                var x = (1f - entry.enter.value) * size.width
                if (cover != null) {
                    val parallax = if (cover.viaGesture) GesturePageParallax else motion.pageParallax
                    x -= parallax * size.width * cover.enter.value
                }
                translationX = x
                if (entry.viaGesture && entry.enter.value < 1f) {
                    clip = true
                    shape = GestureCorners
                } else {
                    clip = false
                    shape = RectangleShape
                }
            }
            .background(background)
            .drawWithContent {
                drawContent()
                if (cover != null) {
                    val share = if (cover.viaGesture) cover.enter.value else cover.scrim.value
                    val alpha = motion.pageDimAlpha * share
                    if (alpha > 0.001f) drawRect(Color.Black.copy(alpha = alpha))
                }
            },
    ) {
        content()
        // Pages that are covered or on their way out do not take touches.
        if (cover != null || entry.phase == Phase.Leaving) {
            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                },
            )
        }
    }
}
