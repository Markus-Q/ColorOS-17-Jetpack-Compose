package dev.irisui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.TouchDragAxes
import dev.irisui.effects.irisClickable
import dev.irisui.effects.touchMotion
import kotlin.math.PI

/**
 * Chip of a single choice row (repeat type of an alarm, filters): 32 dp high pill, 14 sp label, 12 dp padding. The
 * selected chip is accent filled with a white label, the others a 10 % tint of the label color. [badge] puts the red
 * dot of a "new" feature on its top end.
 */
@Composable
fun IrisChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = IrisTheme.colors
    val tint = if (colors.isDark) Color.White else Color.Black
    val response = (2.0 * PI / 0.28).let { (it * it).toFloat() }
    val fill by animateColorAsState(
        if (selected) colors.accent else tint.copy(alpha = if (colors.isDark) 0.10f else 0.08f),
        spring(1f, response),
        label = "chipFill",
    )
    val label by animateColorAsState(if (selected) colors.onAccent else colors.content, spring(1f, response), label = "chipLabel")
    // A chip is not a glass button: it shrinks while held (the motion sits on the outer box so the red dot of a new feature
    // shrinks with it) but does not follow a dragging finger and never claims a drag, so the page and the chip row scroll.
    Box(
        modifier.touchMotion(
            enabled = enabled,
            shape = IrisTheme.shapes.pill,
            dragAxes = TouchDragAxes.None,
            claimAxes = TouchDragAxes.None,
            highlightAlpha = 0f,
        ),
    ) {
        Box(
            Modifier
                .height(32.dp)
                .clip(IrisTheme.shapes.pill)
                .background(fill)
                .irisClickable(onClick, enabled)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            IrisText(text, style = IrisTheme.typography.chip, color = label, maxLines = 1)
        }
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 2.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(colors.destructiveContainer),
            )
        }
    }
}

/**
 * Horizontally scrolling row of chips: 20 dp from the screen edge, 8 dp between chips, the end fades out like the real
 * row does when more chips are hidden behind the edge.
 */
@Composable
fun IrisChipRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: LazyListScope.() -> Unit,
) {
    LazyRow(
        modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val fade = 28.dp.toPx()
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.Black, Color.Transparent),
                        startX = size.width - fade,
                        endX = size.width,
                    ),
                    topLeft = Offset(size.width - fade, 0f),
                    size = androidx.compose.ui.geometry.Size(fade, size.height),
                    blendMode = BlendMode.DstIn,
                )
            },
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
