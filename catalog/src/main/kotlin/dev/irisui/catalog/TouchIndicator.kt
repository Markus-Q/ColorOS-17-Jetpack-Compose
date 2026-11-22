package dev.irisui.catalog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Draws a soft circle under the finger, above everything (popups included), so screen recordings show where the finger is
 * (`adb shell am start ... --ez touches true`). It only watches the touches in the first pass and never consumes one.
 * The edge gesture of the system is not delivered to the app, so a back swipe from the edge shows no circle.
 */
@Composable
fun TouchIndicator(content: @Composable () -> Unit) {
    var position by remember { mutableStateOf(Offset.Unspecified) }
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var wasDown = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            position = down.position
                            if (!wasDown) {
                                scope.launch {
                                    scale.snapTo(1.25f)
                                    alpha.snapTo(1f)
                                    scale.animateTo(1f, tween(120))
                                }
                            }
                            wasDown = true
                        } else if (wasDown) {
                            wasDown = false
                            scope.launch { alpha.animateTo(0f, tween(260)) }
                        }
                    }
                }
            }
            .drawWithContent {
                drawContent()
                val p = position
                if (p != Offset.Unspecified && alpha.value > 0.01f) {
                    val r = 24.dp.toPx() * scale.value
                    drawCircle(Color.White.copy(alpha = 0.32f * alpha.value), r, p)
                    drawCircle(Color.White.copy(alpha = 0.75f * alpha.value), r, p, style = Stroke(2.dp.toPx()))
                }
            },
    ) {
        content()
    }
}
