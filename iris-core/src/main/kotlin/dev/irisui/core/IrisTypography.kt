package dev.irisui.core

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Line height the way the stock apps lay it out on ColorOS 17: the extra space of the
 * line box sits above the glyphs (measured: the ink starts 16 px below a 65 px title box at 480 dpi).
 */
private val ExtraAbove = LineHeightStyle(alignment = LineHeightStyle.Alignment.Bottom, trim = LineHeightStyle.Trim.None)

private fun style(size: Float, lineHeight: Float, weight: Int, letterSpacing: TextUnit = TextUnit.Unspecified) = TextStyle(
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = letterSpacing,
    lineHeightStyle = ExtraAbove,
)

/**
 * Text styles. Color is intentionally left unspecified; text picks it up from LocalContentColor.
 *
 * Sizes follow the type scale of ColorOS 17; line heights are the measured
 * line boxes of the real apps on the OnePlus 15 (font size x font metrics 1.17 x line spacing multiplier), and
 * weights are the *effective* weights found by comparing glyph coverage with the real screens (the system font is a
 * variable font, titles are drawn at about 575, summaries at 400).
 */
@Immutable
class IrisTypography(
    /** Collapsing large title of in-app pages ("Alarm", "My Files"): 32 sp, drawn at weight 850. */
    val largeTitle: TextStyle = style(32f, 38f, 850),
    /** Large title of top level pages such as the Settings home page: 40 sp, white. */
    val largeTitleXL: TextStyle = style(40f, 47f, 850),
    /** Clock style digits (alarm time, timer lists): 30 sp at weight 580. */
    val numeric: TextStyle = style(30f, 42f, 580),
    /** Second line under a large title ("Next alarm in ...", 14 sp). */
    val subtitleSmall: TextStyle = style(14f, 19f, 400),
    /** Dialog and sheet titles. */
    val title: TextStyle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    /** Title of dialogs (18 sp, drawn at weight 700 like the top bar title). */
    val dialogTitle: TextStyle = style(18f, 24.06f, 700),
    /** Title of the compact top bar (18 sp). */
    val topBarTitle: TextStyle = style(18f, 24.06f, 708).copy(
        lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None),
    ),
    val headline: TextStyle = style(16f, 21.66f, 575),
    /** List item titles (drawn at weight 575). */
    val body: TextStyle = style(16f, 21.66f, 575),
    /** Dialog message, list item summary (13 sp). */
    val bodySmall: TextStyle = style(13f, 17.84f, 400),
    val subtitle: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    val label: TextStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    val button: TextStyle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    /** Labels of the bottom pill (10 sp). */
    val tabLabel: TextStyle = style(10f, 14f, 500),
    /** Chip labels (14 sp). */
    val chip: TextStyle = style(14f, 20.05f, 500),
    /** Rows of popup menus (15 sp, regular weight). */
    val menuItem: TextStyle = style(15f, 19.87f, 400),
    /** Group header above cards ("Volume", drawn at weight 575). */
    val sectionHeader: TextStyle = style(12f, 16.05f, 575),
    /** Small secondary text such as the line under a sheet title (12 sp). */
    val caption: TextStyle = style(12f, 16.05f, 400),
)
