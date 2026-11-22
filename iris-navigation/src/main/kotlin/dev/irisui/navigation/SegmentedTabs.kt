package dev.irisui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.touchMotion
import kotlinx.coroutines.launch

private fun edgeSpring(response: Float) = spring<Float>(
    dampingRatio = 0.85f,
    stiffness = (2.0 * Math.PI / response).let { (it * it).toFloat() },
)

/** The edge of the thumb that leads the move is a little quicker than the one that trails (measured on the Battery page). */
private val LeadingEdge = edgeSpring(0.405f)
private val TrailingEdge = edgeSpring(0.47f)

/**
 * Segmented control of the Battery page: a 40 dp track, a neutral 32 dp thumb 4 dp inside it, and labels that keep one
 * color. The two edges of the thumb are springs of their own (damping 0.85, response 0.41 s for the edge in front, 0.47 s
 * for the one behind), so the thumb stretches by about 10 % on the way. The whole control shrinks to 96 % while it is
 * held. A selection change is silent, like on the Battery page (checked with the vibrator log).
 */
@Composable
fun IrisSegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = IrisTheme.colors.content.copy(alpha = 0.135f),
    thumbColor: Color = IrisTheme.colors.content.copy(alpha = 0.22f),
    selectedContentColor: Color = IrisTheme.colors.content,
    unselectedContentColor: Color = selectedContentColor,
) {
    val shape = IrisTheme.shapes.pill
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnChange by rememberUpdatedState(onSelectedChange)
    val left = remember { Animatable(selectedIndex.toFloat()) }
    val right = remember { Animatable(selectedIndex + 1f) }
    LaunchedEffect(selectedIndex) {
        val towardsEnd = selectedIndex + 1f > right.targetValue
        launch { left.animateTo(selectedIndex.toFloat(), if (towardsEnd) TrailingEdge else LeadingEdge) }
        launch { right.animateTo(selectedIndex + 1f, if (towardsEnd) LeadingEdge else TrailingEdge) }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            // The whole control shrinks while held; the thumb itself still follows the finger across the segments.
            .touchMotion(shape = shape, yieldToConsumers = false)
            .clip(shape)
            .drawBehind {
                drawRoundRect(trackColor, cornerRadius = CornerRadius(size.height / 2f))
                val inset = 4.dp.toPx()
                val segment = (size.width - 2f * inset) / options.size
                val x0 = inset + left.value * segment
                val x1 = inset + right.value * segment
                drawRoundRect(
                    thumbColor,
                    topLeft = Offset(x0, inset),
                    size = Size(x1 - x0, size.height - 2f * inset),
                    cornerRadius = CornerRadius((size.height - 2f * inset) / 2f),
                )
            }
            .pointerInput(options.size) {
                awaitEachGesture {
                    fun indexAt(x: Float) =
                        (x / (size.width.toFloat() / options.size)).toInt().coerceIn(0, options.lastIndex)

                    fun commit(index: Int) {
                        if (index != currentSelected) {
                            currentOnChange(index)
                        }
                    }

                    val down = awaitFirstDown(requireUnconsumed = false)
                    commit(indexAt(down.position.x))
                    drag(down.id) { change ->
                        commit(indexAt(change.position.x))
                        change.consume()
                    }
                }
            },
    ) {
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, label ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    IrisText(
                        label,
                        style = IrisTheme.typography.label,
                        color = if (index == selectedIndex) selectedContentColor else unselectedContentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
