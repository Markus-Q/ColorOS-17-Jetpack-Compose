package dev.irisui.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisIcon
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.IrisHaptic
import dev.irisui.effects.GlassDefaults
import dev.irisui.effects.TouchMotionDefaults
import dev.irisui.effects.irisClickable
import dev.irisui.effects.glass
import dev.irisui.effects.persistentGlow
import dev.irisui.effects.touchGlow
import dev.irisui.effects.touchMotion

private fun accentGradient(color: Color) = Brush.verticalGradient(
    listOf(lerp(color, Color.White, 0.04f), lerp(color, Color.White, 0.16f)),
)

/**
 * Fill of the round accent buttons: a slight vertical gradient that gets lighter and more cyan towards the
 * bottom. For the default accent (#1A8CFF) it is exactly the Clock FAB (#27A0FE at the top, #35AFFA at the bottom).
 */
private fun fabGradient(color: Color): Brush {
    fun shift(dr: Int, dg: Int, db: Int) = Color(
        red = (color.red + dr / 255f).coerceIn(0f, 1f),
        green = (color.green + dg / 255f).coerceIn(0f, 1f),
        blue = (color.blue + db / 255f).coerceIn(0f, 1f),
        alpha = color.alpha,
    )
    return Brush.verticalGradient(listOf(shift(13, 20, -1), shift(27, 35, -5)))
}

/**
 * Filled accent pill with the persistent colored glow underneath. Like every ColorOS button it
 * shrinks while held, follows a dragging finger with rubber-band resistance and springs back
 * with an overshoot.
 */
@Composable
fun IrisPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    color: Color = IrisTheme.colors.accent,
    glow: Boolean = true,
) {
    val colors = IrisTheme.colors
    val shape = IrisTheme.shapes.pill
    Box(
        modifier
            .defaultMinSize(minHeight = 50.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .touchMotion(
                enabled = enabled,
                shape = shape,
                highlight = TouchMotionDefaults.AccentHighlight,
                highlightAlpha = TouchMotionDefaults.AccentHighlightAlpha,
            )
            .then(if (glow && enabled) Modifier.persistentGlow(color, alpha = 0.55f) else Modifier)
            .clip(shape)
            .background(accentGradient(color))
            .touchGlow(color = colors.onAccent.copy(alpha = 0.28f))
            .irisClickable(onClick, enabled)
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (leadingIcon != null) IrisIcon(leadingIcon, null, tint = colors.onAccent, size = 22.dp)
            IrisText(text, style = IrisTheme.typography.button, color = colors.onAccent, textAlign = TextAlign.Center)
        }
    }
}

/** Neutral pill for secondary actions. */
@Composable
fun IrisSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    containerColor: Color = IrisTheme.colors.surfaceVariant,
    contentColor: Color = IrisTheme.colors.content,
) {
    val shape = IrisTheme.shapes.pill
    Box(
        modifier
            .defaultMinSize(minHeight = 50.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .touchMotion(enabled = enabled, shape = shape, highlight = contentColor)
            .clip(shape)
            .background(containerColor)
            .touchGlow(color = contentColor.copy(alpha = 0.14f))
            .irisClickable(onClick, enabled)
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (leadingIcon != null) IrisIcon(leadingIcon, null, tint = contentColor, size = 22.dp)
            IrisText(text, style = IrisTheme.typography.button, color = contentColor, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Look of a [IrisIconButton]: `Glass` is the round glass button of the top bars, `Filled` the accent coloured one with its glow,
 * `Plain` is just the icon.
 */
enum class IconButtonStyle {
    /** Dark glass disc with a rim and a faint light underneath (the round buttons in the top bar). */
    Glass,

    /** Solid accent disc with glow. */
    Filled,

    /** Just the icon. */
    Plain,
}

/**
 * Round icon button (the round buttons of the top bar included). Shrinks while held and follows the finger like every
 * ColorOS button, however far it is dragged: the touch belongs to the button, a list behind it does not scroll. (The real
 * toolbar buttons let go as soon as the finger leaves them; here they can be dragged like everything else.)
 */
@Composable
fun IrisIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    style: IconButtonStyle = IconButtonStyle.Glass,
    size: Dp = 40.dp,
    tint: Color = Color.Unspecified,
) {
    val colors = IrisTheme.colors
    val shape = IrisTheme.shapes.pill
    val iconTint = when {
        tint != Color.Unspecified -> tint
        style == IconButtonStyle.Filled -> colors.onAccent
        else -> colors.content
    }
    val base = modifier
        .size(size)
        .alpha(if (enabled) 1f else 0.4f)
    val decorated = when (style) {
        IconButtonStyle.Glass -> base
            // Glass buttons light up with the soft touch glow (13.5 % white at the finger), not a uniform fill.
            .touchMotion(enabled = enabled, shape = shape, highlightAlpha = 0f)
            // Same material as the toolbar of the real apps (over black: #1E1E1E); only a very faint rim (4.5 %).
            .glass(shape, GlassDefaults.toolbarButton())
            .clip(shape)
            .touchGlow(color = colors.content.copy(alpha = 0.135f), radius = 140.dp)

        IconButtonStyle.Filled -> base
            .touchMotion(
                enabled = enabled,
                shape = shape,
                highlight = TouchMotionDefaults.AccentHighlight,
                highlightAlpha = TouchMotionDefaults.AccentHighlightAlpha,
            )
            .persistentGlow(colors.accent, spread = 18.dp, offsetY = 4.dp, alpha = 0.5f)
            .clip(shape)
            .background(fabGradient(colors.accent))
            .touchGlow(color = colors.onAccent.copy(alpha = 0.28f), radius = 50.dp)

        IconButtonStyle.Plain -> base
            .touchMotion(enabled = enabled, shape = shape, highlight = colors.content)
            .clip(shape)
    }
    Box(
        decorated.irisClickable(onClick, enabled),
        contentAlignment = Alignment.Center,
    ) {
        // 24 dp glyph in the 40 dp toolbar disc
        IrisIcon(icon, contentDescription, tint = iconTint, size = size * 0.6f)
    }
}

/**
 * The big round "+" button. Always lit by its colored glow. It shrinks while held and, unlike the
 * toolbar buttons, keeps its pressed state however far the finger is dragged away; it stretches
 * towards the finger and springs back with an overshoot when released.
 */
@Composable
fun IrisRoundPlusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = IrisIcons.Plus,
    contentDescription: String? = "Add",
    color: Color = IrisTheme.colors.accent,
    size: Dp = 56.dp,
) {
    val colors = IrisTheme.colors
    val shape = IrisTheme.shapes.pill
    Box(
        modifier
            .size(size)
            .touchMotion(
                shape = shape,
                cancelOnLeave = false,
                highlight = TouchMotionDefaults.AccentHighlight,
                highlightAlpha = TouchMotionDefaults.AccentHighlightAlpha,
            )
            // The real glow is a short, narrow ellipse right under the button: 0.3 alpha at the bottom edge, gone after about 8 dp.
            .persistentGlow(color, spread = 10.dp, offsetY = 2.dp, alpha = 0.34f, halfWidth = 21.dp * (size.value / 56f))
            .clip(shape)
            .background(fabGradient(color))
            .touchGlow(color = colors.onAccent.copy(alpha = 0.3f), radius = 70.dp)
            // The Clock add button is one of the few things that vibrate on tap.
            .irisClickable(onClick, haptic = IrisHaptic.Tap),
        contentAlignment = Alignment.Center,
    ) {
        IrisIcon(icon, contentDescription, tint = colors.onAccent, size = size * 0.46f)
    }
}

/** Text-only action in the accent color. */
@Composable
fun IrisTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = IrisTheme.colors.accent,
) {
    val shape = IrisTheme.shapes.small
    IrisText(
        text,
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .touchMotion(enabled = enabled, shape = shape, highlight = color)
            .clip(shape)
            .irisClickable(onClick, enabled)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = IrisTheme.typography.button,
        color = color,
    )
}
