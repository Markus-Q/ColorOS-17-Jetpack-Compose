package dev.irisui.popups

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisEasing
import dev.irisui.core.IrisTheme
import dev.irisui.core.OverlayPortal
import dev.irisui.core.rubberBand
import dev.irisui.effects.IrisHaptic
import dev.irisui.effects.GlassDefaults
import dev.irisui.effects.LocalColorOSHaptics
import dev.irisui.effects.glass
import kotlinx.coroutines.launch

/**
 * Bottom sheet. It slides up from the bottom, can be dragged down to dismiss (with rubber band
 * resistance upwards and a springy return) and dims the page behind it.
 */
@Composable
fun IrisBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    fullHeight: Boolean = false,
    showHandle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = IrisTheme.colors
    val transition = rememberPopupTransition(
        visible,
        // Measured on the Clock sheets: both ways are critically damped springs (opening 196, closing 272)
        enterSpec = spring(dampingRatio = 1f, stiffness = 196f),
        exitSpec = spring(dampingRatio = 1f, stiffness = 272f),
    )
    if (!transition.shown) return

    val haptics = LocalColorOSHaptics.current
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    var sheetHeight by remember { mutableFloatStateOf(0f) }
    var rawDrag by remember { mutableFloatStateOf(0f) }
    val overscrollLimit = with(LocalDensity.current) { 72.dp.toPx() }

    OverlayPortal {
        BackHandler(enabled = visible) { onDismissRequest() }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val dragFraction = if (sheetHeight > 0f) (dragOffset.value / sheetHeight).coerceIn(0f, 1f) else 0f
                        alpha = (transition.progress.value * (1f - dragFraction)).coerceIn(0f, 1f)
                    }
                    .background(colors.scrim)
                    .pointerInput(Unit) { detectTapGestures { onDismissRequest() } },
            )
            Column(
                modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .then(if (fullHeight) Modifier.fillMaxHeight().statusBarsPadding() else Modifier.statusBarsPadding().padding(top = 24.dp))
                    .onSizeChanged { sheetHeight = it.height.toFloat() }
                    .graphicsLayer {
                        val h = if (sheetHeight > 0f) sheetHeight else 2400f
                        translationY = (1f - transition.progress.value) * h + dragOffset.value
                    }
                    // The real sheet is glass: the page behind it blurred with the strongest radius of the system.
                    .glass(IrisTheme.shapes.sheet, GlassDefaults.bottomSheet())
                    .clip(IrisTheme.shapes.sheet)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            val slop = awaitVerticalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                            if (slop != null) {
                                rawDrag = dragOffset.value
                                verticalDrag(slop.id) { change ->
                                    // Read the movement before consuming: positionChange() is zero for a consumed change
                                    val dy = change.position.y - change.previousPosition.y
                                    change.consume()
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    rawDrag += dy
                                    val shown = if (rawDrag >= 0f) rawDrag else -rubberBand(-rawDrag, overscrollLimit)
                                    scope.launch { dragOffset.snapTo(shown) }
                                }
                                val velocity = tracker.calculateVelocity().y
                                // Real sheets only dismiss by position: 39 % of the height springs back, 60 % goes, and not even an 11 px/ms fling does
                                val shouldDismiss = dragOffset.value > sheetHeight * 0.5f
                                scope.launch {
                                    if (shouldDismiss) {
                                        onDismissRequest()
                                    } else {
                                        dragOffset.animateTo(
                                            0f,
                                            spring(dampingRatio = 1.2f, stiffness = 169f),
                                            initialVelocity = velocity,
                                        )
                                    }
                                }
                            }
                        }
                    },
            ) {
                if (showHandle) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 10.dp, bottom = 6.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(IrisTheme.shapes.pill)
                            .background(colors.content.copy(alpha = 0.22f)),
                    )
                }
                Column(Modifier.navigationBarsPadding(), content = content)
            }
        }
    }
}
