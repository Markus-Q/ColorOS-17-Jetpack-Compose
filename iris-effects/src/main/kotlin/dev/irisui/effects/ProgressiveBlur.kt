package dev.irisui.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisTheme

/**
 * The blur under a top bar. Measured on the Settings app: the content that scrolls under the bar
 * is blurred with a radius that falls linearly from [maxBlur] at the top edge to nothing at the bottom edge, and it is
 * covered by the page colour with an alpha that falls linearly from [tintAlpha] to 0. Together they make content fade
 * out smoothly instead of ending at a hard line.
 *
 * The variable radius is built from [steps] stacked blurs of increasing radius, each fading in over its own slice
 * of the bar, which is indistinguishable from a true variable blur at five steps.
 *
 * Give it the height of the status bar plus the toolbar. [backdrop] is the page content, like for [glass].
 */
@Composable
fun ProgressiveBlur(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalBackdrop.current,
    /** Blur radius at the top edge (a Gaussian sigma; the stock value is 26 px at 480 dpi). */
    maxBlur: Dp = (26f / 3f).dp,
    steps: Int = 5,
    tint: Color = IrisTheme.colors.background,
    tintAlpha: Float = 0.4f,
) {
    Box(modifier) {
        for (k in 1..steps) {
            // Layer k holds the full radius k / steps of maxBlur above the slice it fades in over.
            val sigma = maxBlur * (k.toFloat() / steps)
            val fullUntil = 1f - k.toFloat() / steps
            val goneFrom = 1f - (k - 1).toFloat() / steps
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(0f to Color.Black, fullUntil.coerceAtLeast(0.0001f) to Color.Black, goneFrom to Color.Transparent, 1f to Color.Transparent),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                    .glass(RectangleShape, GlassStyle(material = GlassMaterial(sigma, emptyList()), fallbackBackdrop = tint), backdrop),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to tint.copy(alpha = tintAlpha), 1f to tint.copy(alpha = 0f))),
        )
    }
}
