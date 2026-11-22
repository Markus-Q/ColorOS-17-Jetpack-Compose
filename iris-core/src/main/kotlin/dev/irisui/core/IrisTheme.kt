package dev.irisui.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Colors of the current theme (the dark palette outside a [IrisTheme]); read them through [IrisTheme.colors].
 */
val LocalColorOSColors = staticCompositionLocalOf { irisDarkColors() }
/** Typography of the current theme; read it through [IrisTheme.typography]. */
val LocalColorOSTypography = staticCompositionLocalOf { IrisTypography() }
/** Shapes of the current theme; read them through [IrisTheme.shapes]. */
val LocalColorOSShapes = staticCompositionLocalOf { IrisShapes() }
/** Motion specs of the current theme; read them through [IrisTheme.motion]. */
val LocalColorOSMotion = staticCompositionLocalOf { IrisMotion() }

/** Color text and icons use when they are not given one explicitly. */
val LocalContentColor = compositionLocalOf { Color.Unspecified }

/** Access to the current theme tokens. */
object IrisTheme {
    val colors: IrisColors
        @Composable @ReadOnlyComposable get() = LocalColorOSColors.current
    val typography: IrisTypography
        @Composable @ReadOnlyComposable get() = LocalColorOSTypography.current
    val shapes: IrisShapes
        @Composable @ReadOnlyComposable get() = LocalColorOSShapes.current
    val motion: IrisMotion
        @Composable @ReadOnlyComposable get() = LocalColorOSMotion.current
}

/**
 * Root of the theme. Provides colors, typography, shapes, motion and the press indication.
 * Use [dev.irisui.core.IrisTheme] on its own, or the umbrella IrisApp which also wires haptics and popups.
 */
@Composable
fun IrisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: IrisColors = remember(darkTheme) { if (darkTheme) irisDarkColors() else irisLightColors() },
    typography: IrisTypography = remember { IrisTypography() },
    shapes: IrisShapes = remember { IrisShapes() },
    motion: IrisMotion = remember { IrisMotion() },
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColorOSColors provides colors,
        LocalColorOSTypography provides typography,
        LocalColorOSShapes provides shapes,
        LocalColorOSMotion provides motion,
        LocalContentColor provides colors.content,
        LocalIndication provides IrisIndication,
        content = content,
    )
}

/** Text that resolves its color from [LocalContentColor] and its style from the theme. */
@Composable
fun IrisText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = IrisTheme.typography.body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val resolved = color.takeOrElse { LocalContentColor.current.takeOrElse { IrisTheme.colors.content } }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = resolved, textAlign = textAlign),
        overflow = overflow,
        maxLines = maxLines,
    )
}

/** Vector icon tinted with [tint] (default: current content color). */
@Composable
fun IrisIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
) {
    val resolved = tint.takeOrElse { LocalContentColor.current.takeOrElse { IrisTheme.colors.content } }
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(resolved),
    )
}
