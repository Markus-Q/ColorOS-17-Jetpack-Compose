package dev.irisui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisTheme
import dev.irisui.effects.TouchDragAxes
import dev.irisui.effects.TouchMotionDefaults
import dev.irisui.effects.irisClickable
import dev.irisui.effects.touchMotion

/**
 * Rounded surface for grouping content. With [onClick] it behaves like a touchable ColorOS surface: it shrinks a little
 * while held (large cards barely, small ones more) and gets lighter. A card is not a glass button, so it does not follow
 * a dragging finger and never claims a drag: a page scrolls from a card.
 */
@Composable
fun IrisCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = IrisTheme.colors.surface,
    shape: Shape = IrisTheme.shapes.card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val touchable = if (onClick != null) {
        val overlay = IrisTheme.colors.pressOverlay
        modifier
            // Cards light up like list rows: a flat 20 % white overlay that comes in with the press spring.
            .touchMotion(
                shape = shape,
                dragAxes = TouchDragAxes.None,
                claimAxes = TouchDragAxes.None,
                highlight = overlay.copy(alpha = 1f),
                highlightAlpha = overlay.alpha,
                highlightIn = TouchMotionDefaults.RowHighlightIn,
                highlightOut = TouchMotionDefaults.RowHighlightOut,
            )
            .clip(shape)
            .background(color)
            .irisClickable(onClick, haptic = null, role = null)
    } else {
        modifier.clip(shape).background(color)
    }
    Column(touchable.padding(contentPadding), content = content)
}
