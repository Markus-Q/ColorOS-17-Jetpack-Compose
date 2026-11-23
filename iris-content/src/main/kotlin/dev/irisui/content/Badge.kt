package dev.irisui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme

/** Red dot that hints at something new (no number): 8 dp, the destructive container red, a round squircle. */
@Composable
fun IrisRedDot(modifier: Modifier = Modifier, color: Color = IrisTheme.colors.destructiveContainer) {
    Box(modifier.size(8.dp).background(color, IrisTheme.shapes.pill))
}

/**
 * Red badge with a count: a pill at least 16 dp high with white digits (11 sp); counts above [max] read "max+".
 */
@Composable
fun IrisBadge(
    count: Int,
    modifier: Modifier = Modifier,
    max: Int = 99,
    color: Color = IrisTheme.colors.destructiveContainer,
) {
    val label = if (count > max) "$max+" else count.toString()
    Box(
        modifier
            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
            .background(color, IrisTheme.shapes.pill)
            .padding(horizontal = if (label.length > 1) 5.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        IrisText(
            label,
            style = IrisTheme.typography.tabLabel.copy(fontSize = 11.sp),
            color = Color.White,
            maxLines = 1,
        )
    }
}
