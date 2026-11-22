package dev.irisui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The always-on colored light that sits under ColorOS buttons (round plus button, primary
 * button, glass icon buttons). Drawn behind the element, so give the parent some room and do
 * not clip it. [intensity] is read while drawing, so animating it never recomposes.
 */
fun Modifier.persistentGlow(
    color: Color,
    spread: Dp = 26.dp,
    offsetY: Dp = 8.dp,
    alpha: Float = 0.6f,
    intensity: () -> Float = { 1f },
    /** Half width of the glow; by default it follows the element width. */
    halfWidth: Dp? = null,
): Modifier = this.drawBehind {
    val strength = (alpha * intensity()).coerceIn(0f, 1f)
    if (strength <= 0f) return@drawBehind
    val spreadPx = spread.toPx()
    val rx = halfWidth?.toPx() ?: (size.width / 2f * 0.92f + spreadPx * 0.35f)
    val ry = (spreadPx * 0.95f).coerceAtLeast(1f)
    val center = Offset(size.width / 2f, size.height - ry * 0.25f + offsetY.toPx())
    withTransform({ scale(1f, ry / rx, center) }) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color.copy(alpha = strength),
                0.5f to color.copy(alpha = strength * 0.42f),
                1f to Color.Transparent,
                center = center,
                radius = rx,
            ),
            radius = rx,
            center = center,
        )
    }
}

/**
 * Soft light that follows the finger inside the element while it is touched and fades out
 * after release. Apply `clip(shape)` before this modifier so the light stays inside the shape.
 * It only observes touches; it never consumes them. The defaults are the glow of the Clock
 * bottom pill: 13.5 % white at the finger, fading out over about 140 dp.
 */
@Composable
fun Modifier.touchGlow(
    color: Color = Color.White.copy(alpha = 0.135f),
    radius: Dp = 140.dp,
    enabled: Boolean = true,
): Modifier {
    val alpha = remember { Animatable(0f) }
    var position by remember { mutableStateOf(Offset.Unspecified) }
    val scope = rememberCoroutineScope()
    if (!enabled) return this
    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: continue
                    if (change.pressed) {
                        position = change.position
                        if (alpha.targetValue != 1f) scope.launch { alpha.animateTo(1f, tween(110)) }
                    } else if (alpha.targetValue != 0f) {
                        scope.launch { alpha.animateTo(0f, tween(480)) }
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            val a = alpha.value
            if (a > 0f && position != Offset.Unspecified) {
                val r = radius.toPx()
                val peak = color.alpha * a
                // Gaussian-like falloff (sigma = 0.56 radius), as measured on the Clock pill.
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to color.copy(alpha = peak),
                        0.25f to color.copy(alpha = peak * 0.815f),
                        0.5f to color.copy(alpha = peak * 0.45f),
                        0.7f to color.copy(alpha = peak * 0.21f),
                        1f to Color.Transparent,
                        center = position,
                        radius = r,
                    ),
                    radius = r,
                    center = position,
                )
            }
        }
}
