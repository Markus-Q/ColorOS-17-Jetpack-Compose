package dev.irisui.core

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Color tokens of the ColorOS 17 look.
 *
 * The values match ColorOS 17 (dark and light variants) and were checked against screenshots of the real apps on a
 * OnePlus 15. Most text and surface colors are translucent white (dark) or black (light) on purpose: they blend with
 * whatever is behind them (page, sheet, glass), like the system does.
 */
@Immutable
class IrisColors(
    val isDark: Boolean,
    /** Page background. */
    val background: Color,
    /** Cards and grouped lists that sit on [background] (10 % white in dark mode). */
    val surface: Color,
    /** Cards that sit on a sheet or dialog. */
    val surfaceElevated: Color,
    /** Chips, secondary buttons, segmented track, round icon buttons. */
    val surfaceVariant: Color,
    /** Text field and search bar fill. */
    val field: Color,
    /** Tint of glass surfaces (bottom pill, popup menu, top bar). */
    val glass: Color,
    val sheet: Color,
    val dialog: Color,
    /** Primary text and icons (90 % white / black). */
    val content: Color,
    /** Summaries, section headers, values (54 % in dark mode). */
    val contentSecondary: Color,
    /** Hints and disabled text (30 % in dark mode). */
    val contentTertiary: Color,
    /** List dividers (20 % white in dark mode, one pixel high). */
    val divider: Color,
    val accent: Color,
    val onAccent: Color,
    val destructive: Color,
    /** Switch track while off (drawn over the card). */
    val switchTrackOff: Color,
    /** Overlay drawn on a row while it is pressed. */
    val pressOverlay: Color,
    val scrim: Color,
    /** Highlight on the top edge of glass surfaces. */
    val rimHighlight: Color,
    /** Faint edge on the bottom of glass surfaces. */
    val rimShade: Color,
    /** Accent for text and text buttons ("Add", links): lighter than [accent] in dark mode. */
    val accentText: Color = accent,
    /** Second level of text on glass and tab bars (72 %). */
    val contentVariant: Color = content,
    /** Second level of the secondary text on glass and tab bars (40 %). */
    val contentSecondaryVariant: Color = contentSecondary,
    /** Faintest label level. */
    val contentQuaternary: Color = contentTertiary,
    /** Empty part of sliders and progress bars. */
    val trackEmpty: Color = divider,
    /** Highlight of the list row that a popup menu is anchored to (15 % white). */
    val anchorHighlight: Color = pressOverlay,
    /** Red badge dots and error containers. */
    val destructiveContainer: Color = destructive,
) {
    fun copy(
        accent: Color = this.accent,
        onAccent: Color = this.onAccent,
        destructive: Color = this.destructive,
        background: Color = this.background,
        surface: Color = this.surface,
        accentText: Color = if (accent == this.accent) this.accentText else accent,
    ) = IrisColors(
        isDark = isDark,
        background = background,
        surface = surface,
        surfaceElevated = surfaceElevated,
        surfaceVariant = surfaceVariant,
        field = field,
        glass = glass,
        sheet = sheet,
        dialog = dialog,
        content = content,
        contentSecondary = contentSecondary,
        contentTertiary = contentTertiary,
        divider = divider,
        accent = accent,
        onAccent = onAccent,
        destructive = destructive,
        switchTrackOff = switchTrackOff,
        pressOverlay = pressOverlay,
        scrim = scrim,
        rimHighlight = rimHighlight,
        rimShade = rimShade,
        accentText = accentText,
        contentVariant = contentVariant,
        contentSecondaryVariant = contentSecondaryVariant,
        contentQuaternary = contentQuaternary,
        trackEmpty = trackEmpty,
        anchorHighlight = anchorHighlight,
        destructiveContainer = destructiveContainer,
    )
}

/** ColorOS blue (dark variant). Settings uses a slightly different blue (#428AF7). */
val IrisBlue = Color(0xFF1A8CFF)

/** The blue of caret and underline in the text fields of the stock dialogs (measured: #428AF7). */
val IrisSettingsBlue = Color(0xFF428AF7)

/** The dark palette of ColorOS 17. Pass it, or your own [IrisColors], to [IrisTheme]. */
fun irisDarkColors() = IrisColors(
    isDark = true,
    background = Color(0xFF000000),
    surface = Color(0x1AFFFFFF),
    surfaceElevated = Color(0xFF2B2B2B),
    surfaceVariant = Color(0xFF303030),
    field = Color(0xFF1F1F1F),
    glass = Color(0xFF1B1B1B),
    sheet = Color(0xFF171717),
    dialog = Color(0xFF232323),
    content = Color(0xE6FFFFFF),
    contentSecondary = Color(0x8AFFFFFF),
    contentTertiary = Color(0x4DFFFFFF),
    divider = Color(0x33FFFFFF),
    accent = IrisBlue,
    onAccent = Color.White,
    destructive = Color(0xFFFF6C61),
    switchTrackOff = Color(0x40FFFFFF),
    pressOverlay = Color(0x33FFFFFF),
    scrim = Color(0x99000000),
    rimHighlight = Color(0x33FFFFFF),
    rimShade = Color(0x26FFFFFF),
    accentText = Color(0xFF5C9DFF),
    contentVariant = Color(0xB8FFFFFF),
    contentSecondaryVariant = Color(0x66FFFFFF),
    contentQuaternary = Color(0x26FFFFFF),
    trackEmpty = Color(0x33FFFFFF),
    anchorHighlight = Color(0x26FFFFFF),
    destructiveContainer = Color(0xFFEB493D),
)

/** The light palette of ColorOS 17. Pass it, or your own [IrisColors], to [IrisTheme]. */
fun irisLightColors() = IrisColors(
    isDark = false,
    background = Color(0xFFF0F1F2),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8E8EC),
    field = Color(0xFFE6E6EA),
    glass = Color(0xFFFFFFFF),
    sheet = Color(0xFFF5F5F8),
    dialog = Color(0xFFFFFFFF),
    content = Color(0xE6000000),
    contentSecondary = Color(0x8A000000),
    contentTertiary = Color(0x42000000),
    divider = Color(0x1F000000),
    accent = Color(0xFF0080FF),
    onAccent = Color.White,
    destructive = Color(0xFFDB382C),
    switchTrackOff = Color(0x29000000),
    pressOverlay = Color(0x1F000000),
    scrim = Color(0x66000000),
    rimHighlight = Color(0xCCFFFFFF),
    rimShade = Color(0x14000000),
    accentText = Color(0xFF0080FF),
    contentVariant = Color(0xB8000000),
    contentSecondaryVariant = Color(0x66000000),
    contentQuaternary = Color(0x14000000),
    trackEmpty = Color(0x1F000000),
    anchorHighlight = Color(0x14000000),
    destructiveContainer = Color(0xFFEB3B2F),
)
