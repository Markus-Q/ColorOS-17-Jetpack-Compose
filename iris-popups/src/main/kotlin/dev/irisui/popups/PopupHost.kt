package dev.irisui.popups

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.irisui.core.IrisTheme
import dev.irisui.core.LocalOverlayHost
import dev.irisui.core.OverlayHost
import dev.irisui.core.OverlayHostLayer
import dev.irisui.effects.LocalBackdrop
import dev.irisui.effects.backdropSource
import dev.irisui.effects.rememberBackdrop

/**
 * Wrap the whole app (or one screen) in this host. Dialogs, sheets and menus are drawn in a layer
 * above [content] and blur it through the host's backdrop.
 */
@Composable
fun IrisPopupHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val host = remember { OverlayHost() }
    val backdrop = rememberBackdrop()
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().backdropSource(backdrop).background(IrisTheme.colors.background)) {
            CompositionLocalProvider(LocalOverlayHost provides host) { content() }
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            OverlayHostLayer(host)
        }
    }
}

/**
 * Drives enter/exit of a popup. [progress] goes 0 -> 1 on show and back on hide; [shown] stays
 * true until the exit animation has finished, so the popup can animate out before it is removed.
 */
@Stable
internal class PopupTransition {
    val progress = Animatable(0f)
    var shown by mutableStateOf(false)
}

@Composable
internal fun rememberPopupTransition(
    visible: Boolean,
    enterSpec: AnimationSpec<Float>,
    exitSpec: AnimationSpec<Float>,
): PopupTransition {
    val transition = remember { PopupTransition() }
    LaunchedEffect(visible) {
        if (visible) {
            transition.shown = true
            transition.progress.animateTo(1f, enterSpec)
        } else if (transition.shown) {
            transition.progress.animateTo(0f, exitSpec)
            transition.shown = false
        }
    }
    return transition
}

internal typealias PopupProgress = Animatable<Float, AnimationVector1D>
