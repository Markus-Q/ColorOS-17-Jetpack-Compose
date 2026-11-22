package dev.irisui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.irisui.buttons.IrisIconButton
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.touchMotion

/** Measures of the top bar. */
object IrisTopBarDefaults {
    /** Toolbar height below the status bar (52 dp, measured 156 px at 480 dpi). */
    val Height = 52.dp
}

/**
 * Compact top bar: round back button, a title that sits next to it and round action buttons.
 * Transparent on purpose; [IrisScaffold] puts the progressive blur behind it.
 * [titleAlpha] is read while drawing, so it can follow scrolling without recomposition.
 */
@Composable
fun IrisTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleAlpha: () -> Float = { 1f },
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(IrisTopBarDefaults.Height)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigationIcon != null) {
            navigationIcon()
            // gap between the back button and the title (ColorOS 17): 10 dp
            Spacer(Modifier.width(10.dp))
        }
        if (title != null) {
            // The title is touchable like the buttons next to it (as long as it is visible), it follows a dragging finger.
            val titleVisible by remember(titleAlpha) { derivedStateOf { titleAlpha() > 0.5f } }
            Box(Modifier.weight(1f)) {
                IrisText(
                    title,
                    Modifier
                        .graphicsLayer { alpha = titleAlpha() }
                        .touchMotion(enabled = titleVisible, shape = IrisTheme.shapes.small, highlightAlpha = 0f),
                    style = IrisTheme.typography.topBarTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

/** Round glass back button for [IrisTopBar]. */
@Composable
fun IrisBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IrisIconButton(IrisIcons.Back, onClick, modifier, contentDescription = "Back")
}
