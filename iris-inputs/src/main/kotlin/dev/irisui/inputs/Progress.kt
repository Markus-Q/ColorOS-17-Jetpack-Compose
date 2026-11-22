package dev.irisui.inputs

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisTheme

/**
 * Horizontal progress bar, measured on the storage card of the Files app: 8 dp high, fully
 * rounded track in the same tone as the empty part of a slider, the accent fill ends in a straight edge. Changes of
 * [progress] glide instead of jumping.
 */
@Composable
fun IrisProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = IrisTheme.colors.accent,
    trackColor: Color = IrisTheme.colors.trackEmpty,
) {
    val shown by animateFloatAsState(progress.coerceIn(0f, 1f), tween(300), label = "progress")
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        val corner = CornerRadius(size.height / 2f)
        drawRoundRect(trackColor, size = size, cornerRadius = corner)
        val track = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, corner)) }
        clipPath(track) { drawRect(color, size = Size(size.width * shown, size.height)) }
    }
}

/** Progress bar that does not know how far it is: a quarter of the bar sweeps back and forth. */
@Composable
fun IrisProgressBar(
    modifier: Modifier = Modifier,
    color: Color = IrisTheme.colors.accent,
    trackColor: Color = IrisTheme.colors.trackEmpty,
) {
    val transition = rememberInfiniteTransition(label = "indeterminate")
    val position by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        val corner = CornerRadius(size.height / 2f)
        drawRoundRect(trackColor, size = size, cornerRadius = corner)
        val track = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, corner)) }
        val segment = size.width * 0.3f
        val x = -segment + (size.width + segment) * position
        clipPath(track) { drawRect(color, topLeft = Offset(x, 0f), size = Size(segment, size.height)) }
    }
}

/** Round loading indicator: an accent arc of 270 degrees turning once a second. */
@Composable
fun IrisLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = IrisTheme.colors.accent,
    strokeWidth: Dp = 2.5.dp,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "turn",
    )
    Canvas(modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        drawArc(
            color = color,
            startAngle = angle - 90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(this.size.width - stroke, this.size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

/** Ring that fills like a progress bar. */
@Composable
fun IrisCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = IrisTheme.colors.accent,
    trackColor: Color = IrisTheme.colors.trackEmpty,
    strokeWidth: Dp = 4.dp,
) {
    val shown by animateFloatAsState(progress.coerceIn(0f, 1f), tween(300), label = "ringProgress")
    Canvas(modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(color, -90f, 360f * shown, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
