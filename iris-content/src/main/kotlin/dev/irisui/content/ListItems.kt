package dev.irisui.content

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisIcon
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.TouchDragAxes
import dev.irisui.effects.TouchMotionDefaults
import dev.irisui.effects.irisClickable
import dev.irisui.effects.touchMotion

/**
 * A row as used throughout ColorOS Settings: optional leading icon, title with an optional
 * summary, a trailing value / chevron / custom control. Rows with [onClick] show the press highlight.
 *
 * Metrics are those of the stock preference row: 54 dp minimum height, 10 dp vertical and 16 dp horizontal padding,
 * the summary directly under the title, 16 dp between the text and a trailing switch.
 */
@Composable
fun IrisListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    showChevron: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    /** Shows the small up / down arrows of a row that opens a choice menu instead of a chevron. */
    showDropdown: Boolean = false,
    /** Lights the row while a popup menu opened from it is showing (15 % white over the card). */
    highlighted: Boolean = false,
) {
    val colors = IrisTheme.colors
    val clickable = if (onClick != null) {
        // The flat press highlight shows immediately (no tap-timeout delay inside lists), like Settings rows.
        // Rows are menu entries, not glass buttons: they neither shrink (measured) nor follow a dragging finger, and they
        // never claim a drag, so the page scrolls from a row.
        Modifier
            .touchMotion(
                enabled = enabled,
                pressedScale = 1f,
                dragAxes = TouchDragAxes.None,
                claimAxes = TouchDragAxes.None,
                highlight = colors.pressOverlay.copy(alpha = 1f),
                highlightAlpha = colors.pressOverlay.alpha,
                highlightIn = TouchMotionDefaults.RowHighlightIn,
                highlightOut = TouchMotionDefaults.RowHighlightOut,
            )
            .irisClickable(onClick, enabled, haptic = null, indication = null)
    } else {
        Modifier
    }
    val anchorColor by animateColorAsState(
        if (highlighted) colors.anchorHighlight else Color.Transparent,
        spring(dampingRatio = 1f, stiffness = 1366f),
        label = "rowAnchor",
    )
    Row(
        modifier
            .fillMaxWidth()
            .then(clickable)
            .drawBehind { drawRect(anchorColor) }
            .alpha(if (enabled) 1f else 0.4f)
            .heightIn(min = 54.dp)
            // Rows with a summary sit 2 px higher in the stock layout (27 px above, 33 px below at 480 dpi).
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (supporting != null) 9.33.dp else 10.dp,
                bottom = if (supporting != null) 10.67.dp else 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            IrisText(headline, style = IrisTheme.typography.body)
            if (supporting != null) {
                IrisText(
                    supporting,
                    style = IrisTheme.typography.bodySmall,
                    color = colors.contentSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            IrisText(value, style = IrisTheme.typography.bodySmall, color = colors.contentSecondary, maxLines = 1)
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
        if (showChevron) {
            Spacer(Modifier.width(if (value != null) 2.dp else 4.dp))
            RowChevron(colors.contentTertiary)
        }
        if (showDropdown) {
            Spacer(Modifier.width(if (value != null) 2.dp else 4.dp))
            RowDropdownArrows(colors.contentTertiary)
        }
    }
}

/** The 14 dp wide "go to" chevron at the end of a row (ink 7.3 x 13.3 dp, right aligned, 30 % label color). */
@Composable
private fun RowChevron(color: Color) {
    Canvas(Modifier.size(width = 14.dp, height = 24.dp)) {
        val w = 7.3.dp.toPx()
        val h = 13.3.dp.toPx()
        val stroke = 1.3.dp.toPx()
        val x1 = size.width - 0.7.dp.toPx() - stroke / 2f
        val x0 = x1 - w + stroke
        val y0 = (size.height - h) / 2f + stroke / 2f
        val path = Path().apply {
            moveTo(x0, y0)
            lineTo(x1, size.height / 2f)
            lineTo(x0, y0 + h - stroke)
        }
        drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** The small up / down arrows of a row that opens a choice menu (ink 10.7 x 15.3 dp, 30 % label color). */
@Composable
private fun RowDropdownArrows(color: Color) {
    Canvas(Modifier.size(width = 14.dp, height = 24.dp)) {
        // Two separate 45 degree chevrons: arms 4.67 dp, 4.67 dp between the arm ends (measured ink 32 x 46 px).
        val stroke = 1.33.dp.toPx()
        val arm = 4.67.dp.toPx()
        val gap = 4.67.dp.toPx()
        val cx = size.width - 0.3.dp.toPx() - stroke / 2f - arm
        val top = (size.height - (3f * arm)) / 2f
        val up = Path().apply {
            moveTo(cx - arm, top + arm)
            lineTo(cx, top)
            lineTo(cx + arm, top + arm)
        }
        val down = Path().apply {
            moveTo(cx - arm, top + arm + gap)
            lineTo(cx, top + 2f * arm + gap)
            lineTo(cx + arm, top + arm + gap)
        }
        val style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(up, color, style = style)
        drawPath(down, color, style = style)
    }
}

/** Free-standing colored glyph as used in Settings rows (24 dp box: the text then starts 56 dp from the card edge). */
@Composable
fun IrisListIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(24.dp), contentAlignment = Alignment.Center) {
        IrisIcon(icon, null, tint = tint, size = 24.dp)
    }
}

/** Glyph on a rounded colored tile, as used in the Files app. */
@Composable
fun IrisIconTile(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shape: Shape = IrisTheme.shapes.small,
) {
    Box(
        modifier.size(size).clip(shape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        IrisIcon(icon, null, tint = Color.White, size = size * 0.6f)
    }
}

/** Scope of the content of [IrisGroupedList]: add the rows with [item]. */
@Stable
class GroupedListScope internal constructor() {
    internal val items = mutableListOf<@Composable () -> Unit>()

    /** Adds one row to the group; dividers are inserted between rows automatically. */
    fun item(content: @Composable () -> Unit) {
        items += content
    }
}

/**
 * A card that holds several rows separated by inset dividers. [dividerStart] should match where
 * the row text begins (56.dp with a leading icon, 16.dp without).
 */
@Composable
fun IrisGroupedList(
    modifier: Modifier = Modifier,
    dividerStart: Dp = 16.dp,
    color: Color = IrisTheme.colors.surface,
    content: GroupedListScope.() -> Unit,
) {
    val scope = GroupedListScope().apply(content)
    Column(
        modifier
            .fillMaxWidth()
            .clip(IrisTheme.shapes.card)
            .background(color),
    ) {
        scope.items.forEachIndexed { index, item ->
            if (index > 0) IrisDivider(start = dividerStart, end = 16.dp)
            item()
        }
    }
}
