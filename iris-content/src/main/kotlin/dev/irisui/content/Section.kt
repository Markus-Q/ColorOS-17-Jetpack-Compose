package dev.irisui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme

/**
 * Small gray caption above a group of rows ("Volume", "Sound effects"). Same metrics as the stock category title:
 * text aligned with the row text of the cards (32 dp from the screen edge), 16 dp gap above (when a card precedes it)
 * plus 8 dp padding, 8 dp padding below.
 */
@Composable
fun IrisSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    /** Space above the text: 23.33 dp after a card, 18 dp when it is the first thing under the top bar. */
    topPadding: Dp = 23.33.dp,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = topPadding, bottom = 8.67.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IrisText(
            text,
            style = IrisTheme.typography.sectionHeader,
            color = IrisTheme.colors.contentSecondary,
        )
        trailing?.invoke()
    }
}

/**
 * Hairline divider. Rows inside a group use [start] to line up with the text after the icon. The default thickness is
 * one device pixel, like the real dividers (20 % white on dark, 12 % black on light).
 */
@Composable
fun IrisDivider(
    modifier: Modifier = Modifier,
    start: Dp = 0.dp,
    end: Dp = 0.dp,
    thickness: Dp = Dp.Unspecified,
) {
    val resolved = if (thickness == Dp.Unspecified) with(LocalDensity.current) { 1f.toDp() } else thickness
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = start, end = end)
            .height(resolved)
            .background(IrisTheme.colors.divider),
    )
}
