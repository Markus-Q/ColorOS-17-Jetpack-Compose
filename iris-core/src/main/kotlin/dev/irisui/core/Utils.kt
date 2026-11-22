package dev.irisui.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.math.abs
import kotlin.math.sign

/**
 * Rubber band resistance. Maps an unbounded [offset] to a value that approaches [limit]
 * asymptotically, the way overscroll and over-drag feel on ColorOS.
 */
fun rubberBand(offset: Float, limit: Float, coefficient: Float = 0.55f): Float {
    if (limit <= 0f) return 0f
    val x = abs(offset)
    return sign(offset) * (1f - 1f / (x * coefficient / limit + 1f)) * limit
}

/** Click without any indication; callers draw their own feedback. */
@Composable
fun Modifier.plainClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    onClick = onClick,
)
