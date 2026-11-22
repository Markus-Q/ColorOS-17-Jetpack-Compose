package dev.irisui.inputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisTheme
import dev.irisui.effects.TouchDragAxes
import dev.irisui.effects.touchMotion
import kotlin.math.PI

private fun response(seconds: Float) = (2.0 * PI / seconds).let { (it * it).toFloat() }

/**
 * Rounded-square checkbox, 18 dp inside a 30 dp touch area, as in the selection mode of the stock apps: a 1.7 dp ring
 * in the faint label color turns into an accent filled square with a white check that draws itself. Like the real one
 * it is silent (no haptic).
 */
@Composable
fun IrisCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = IrisTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val fill by animateColorAsState(if (checked) colors.accent else colors.accent.copy(alpha = 0f), spring(1f, response(0.28f)), label = "checkFill")
    val border by animateColorAsState(if (checked) colors.accent else colors.contentTertiary, spring(1f, response(0.28f)), label = "checkBorder")
    val check by animateFloatAsState(if (checked) 1f else 0f, spring(1f, response(0.34f)), label = "checkProgress")

    Box(
        modifier
            .size(30.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .touchMotion(enabled = enabled && onCheckedChange != null, dragAxes = TouchDragAxes.None, claimAxes = TouchDragAxes.None, highlightAlpha = 0f)
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Checkbox,
                interactionSource = interaction,
                indication = null,
                onValueChange = { onCheckedChange?.invoke(it) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val stroke = 1.67.dp.toPx()
            val radius = CornerRadius(5.dp.toPx())
            drawRoundRect(fill, cornerRadius = radius)
            if (check < 1f) {
                drawRoundRect(
                    border,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = radius,
                    style = Stroke(stroke),
                )
            }
            if (check > 0f) {
                // Measured on the Clock selection mode: points at 22 / 50 %, 44 / 70 % and 81 / 31 % of the box.
                val w = size.width
                val path = Path().apply {
                    moveTo(0.22f * w, 0.50f * w)
                    lineTo(0.44f * w, 0.70f * w)
                    lineTo(0.81f * w, 0.31f * w)
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val partial = Path()
                measure.getSegment(0f, measure.length * check, partial, true)
                drawPath(
                    partial,
                    colors.onAccent,
                    style = Stroke(1.33.dp.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Miter),
                )
            }
        }
    }
}

/**
 * Radio button: 20 dp. Selected it is an accent disc with a white 12 dp dot, unselected a 1.7 dp ring in the faint
 * label color (measured on the ringer mode selector of Settings).
 */
@Composable
fun IrisRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = IrisTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val progress by animateFloatAsState(if (selected) 1f else 0f, spring(1f, response(0.3f)), label = "radio")
    Box(
        modifier
            .size(30.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .touchMotion(enabled = enabled && onClick != null, dragAxes = TouchDragAxes.None, claimAxes = TouchDragAxes.None, highlightAlpha = 0f)
            .toggleable(
                value = selected,
                enabled = enabled && onClick != null,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = null,
                onValueChange = { if (it) onClick?.invoke() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val r = size.width / 2f
            val ring = 1.67.dp.toPx()
            if (progress < 1f) {
                drawCircle(colors.contentTertiary.copy(alpha = colors.contentTertiary.alpha * (1f - progress)), r - ring / 2f, style = Stroke(ring))
            }
            if (progress > 0f) {
                drawCircle(colors.accent.copy(alpha = progress), r * (0.7f + 0.3f * progress))
                drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = progress), 6.dp.toPx() * progress)
            }
        }
    }
}
