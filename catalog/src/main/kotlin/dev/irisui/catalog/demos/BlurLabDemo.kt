package dev.irisui.catalog.demos

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irisui.buttons.IrisIconButton
import dev.irisui.buttons.IconButtonStyle
import dev.irisui.content.IrisCard
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.GlassDefaults
import dev.irisui.effects.GlassFade
import dev.irisui.effects.GlassLayer
import dev.irisui.effects.GlassMaterial
import dev.irisui.effects.GlassMaterials
import dev.irisui.effects.GlassStyle
import dev.irisui.effects.ProgressiveBlur
import dev.irisui.effects.backdropSource
import dev.irisui.effects.glass
import dev.irisui.effects.rememberBackdrop
import dev.irisui.effects.verticalFadeMask
import dev.irisui.inputs.IrisSearchBar
import dev.irisui.inputs.IrisSlider
import dev.irisui.inputs.IrisSwitch
import dev.irisui.navigation.IrisBottomNavPill
import dev.irisui.navigation.IrisSegmentedTabs
import dev.irisui.navigation.NavItem
import java.io.File
import kotlin.math.roundToInt

/**
 * Extras of the launching intent for the blur lab (see `BlurLabDemo`):
 * `bg`, `ref` (file names inside `files/lab/`), `layout` (gallery|single), `kind`, `rect` ("l,t,r,b" in px),
 * `dark`, `clean`, `rim`, `fade`, `blur` (px), `c1`, `m1`, `c2`, `m2` (custom material), `split`.
 */
object BlurLabConfig {
    var extras: Bundle? = null
}

private enum class LabBg(val label: String) { Image("Image"), Pattern("Pattern"), Wallpaper("Wallpaper"), Black("Black"), Gray("Gray"), White("White") }

private enum class LabKind(val label: String) { Pill("Pill"), Popup("Popup"), Dialog("Dialog"), Sheet("Sheet"), TopBar("Bar"), Button("Button"), Search("Search") }

private val modeNames = listOf("SrcOver", "Overlay", "Dodge", "Lum")
private val modes = listOf(BlendMode.SrcOver, BlendMode.Overlay, BlendMode.ColorDodge, BlendMode.Luminosity)

private fun parseModeName(s: String?): Int = when (s?.lowercase()) {
    "overlay" -> 1
    "dodge", "colordodge" -> 2
    "lum", "luminosity" -> 3
    else -> 0
}

private fun parseColor(s: String?, default: Color): Color = try {
    if (s == null) default else Color(java.lang.Long.parseLong(s.removePrefix("#"), 16).let { if (s.removePrefix("#").length <= 6) it or 0xFF000000L else it })
} catch (e: NumberFormatException) {
    default
}

private fun defaultRect(kind: LabKind): IntArray = when (kind) {
    LabKind.Pill -> intArrayOf(47, 2145, 1035, 2310)
    LabKind.Popup -> intArrayOf(528, 282, 1032, 570)
    LabKind.Dialog -> intArrayOf(48, 1500, 1032, 2250)
    LabKind.Sheet -> intArrayOf(0, 120, 1080, 2354)
    LabKind.TopBar -> intArrayOf(0, 0, 1080, 260)
    LabKind.Button -> intArrayOf(915, 138, 1035, 258)
    LabKind.Search -> intArrayOf(48, 300, 1032, 420)
}

private fun loadLabImage(context: android.content.Context, name: String?): ImageBitmap? {
    if (name.isNullOrEmpty()) return null
    val file = File(context.filesDir, "lab/$name")
    if (!file.exists()) return null
    val options = BitmapFactory.Options().apply { inScaled = false }
    return BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
}

/** A test pattern with ramps, saturated colours, thin lines, hard edges and text sized shapes. */
private fun DrawScope.drawLabPattern() {
    val w = size.width
    val h = size.height
    drawRect(Color(0xFF101010))
    // 1. grey ramp, 16 steps
    for (i in 0 until 16) drawRect(Color(i / 15f, i / 15f, i / 15f), Offset(w * i / 16f, 0f), Size(w / 16f + 1f, h * 0.07f))
    // 2. saturated colours
    val bars = listOf(Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.Yellow, Color.White, Color(0xFFFF8800))
    for (i in bars.indices) drawRect(bars[i], Offset(w * i / bars.size, h * 0.07f), Size(w / bars.size + 1f, h * 0.07f))
    // 3. hue sweep
    for (i in 0 until 90) {
        val c = Color.hsv(i * 4f, 1f, 1f)
        drawRect(c, Offset(w * i / 90f, h * 0.14f), Size(w / 90f + 1f, h * 0.04f))
    }
    // 4. vertical lines of 1, 2, 4, 8, 16 px
    var x = w * 0.04f
    val top = h * 0.19f
    for (lw in listOf(1f, 2f, 4f, 8f, 16f, 32f)) {
        drawRect(Color.White, Offset(x, top), Size(lw, h * 0.1f))
        x += lw + w * 0.07f
    }
    // 5. checkerboards 8 px and 32 px
    for (j in 0 until 12) for (i in 0 until 12) if ((i + j) % 2 == 0) drawRect(Color.White, Offset(w * 0.06f + i * 8f, h * 0.31f + j * 8f), Size(8f, 8f))
    for (j in 0 until 5) for (i in 0 until 5) if ((i + j) % 2 == 0) drawRect(Color.White, Offset(w * 0.4f + i * 32f, h * 0.31f + j * 32f), Size(32f, 32f))
    // 6. big steps: white | mid grey | black blocks and coloured discs
    drawRect(Color.White, Offset(0f, h * 0.40f), Size(w * 0.33f, h * 0.12f))
    drawRect(Color(0xFF808080), Offset(w * 0.33f, h * 0.40f), Size(w * 0.34f, h * 0.12f))
    drawRect(Color.Black, Offset(w * 0.67f, h * 0.40f), Size(w * 0.33f, h * 0.12f))
    drawCircle(Color(0xFFFF3B30), w * 0.09f, Offset(w * 0.2f, h * 0.60f))
    drawCircle(Color(0xFF34C759), w * 0.09f, Offset(w * 0.5f, h * 0.60f))
    drawCircle(Color(0xFF0A84FF), w * 0.09f, Offset(w * 0.8f, h * 0.60f))
    // 7. warm / cool gradients like a wallpaper, bottom part
    drawRect(
        androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFFF9500), Color(0xFFFF2D55), Color(0xFF5E5CE6), Color(0xFF30B0C7))),
        Offset(0f, h * 0.70f), Size(w, h * 0.30f),
    )
    for (i in 0 until 14) drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.04f + i * w * 0.07f, h * 0.76f), Size(w * 0.02f, h * 0.18f))
}

/** Background of the lab at 1:1 pixels, so a screenshot lines up with a screenshot of the real UI. */
@Composable
private fun LabBackground(mode: LabBg, image: ImageBitmap?, modifier: Modifier = Modifier) {
    when {
        mode == LabBg.Image && image != null -> Canvas(modifier) { drawImage(image) }
        mode == LabBg.Image || mode == LabBg.Pattern -> Canvas(modifier.clip(RectangleShape)) { drawLabPattern() }
        mode == LabBg.Wallpaper -> DemoWallpaper(modifier)
        mode == LabBg.Black -> Box(modifier.background(Color.Black))
        mode == LabBg.Gray -> Box(modifier.background(Color(0xFF808080)))
        else -> Box(modifier.background(Color.White))
    }
}

@Composable
fun BlurLabDemo(onBack: () -> Unit) {
    val ex = BlurLabConfig.extras
    val context = LocalContext.current
    val density = LocalDensity.current

    var dark by remember { mutableStateOf(ex?.getBoolean("dark", true) ?: true) }
    val clean = ex?.getBoolean("clean", false) ?: false
    val startBg = if (ex?.getString("bg") != null) LabBg.Image else when (ex?.getString("bgmode")?.lowercase()) {
        "pattern" -> LabBg.Pattern
        "black" -> LabBg.Black
        "gray" -> LabBg.Gray
        "white" -> LabBg.White
        "wallpaper" -> LabBg.Wallpaper
        else -> LabBg.Pattern
    }
    var bgMode by remember { mutableIntStateOf(startBg.ordinal) }
    var single by remember { mutableStateOf(ex?.getString("layout") == "single") }
    var kindIndex by remember {
        mutableIntStateOf(LabKind.entries.firstOrNull { it.name.equals(ex?.getString("kind"), true) }?.ordinal ?: LabKind.Pill.ordinal)
    }
    var showRim by remember { mutableStateOf(ex?.getBoolean("rim", true) ?: true) }
    var showFade by remember { mutableStateOf(ex?.getBoolean("fade", false) ?: false) }
    var custom by remember { mutableStateOf(ex?.getString("c1") != null || ex?.containsKey("blur") == true) }
    var blurPx by remember { mutableFloatStateOf(ex?.getFloat("blur", 25f) ?: 25f) }
    var a1 by remember { mutableFloatStateOf(0.5f) }
    var g1 by remember { mutableFloatStateOf(0.4f) }
    var m1 by remember { mutableIntStateOf(parseModeName(ex?.getString("m1"))) }
    var a2 by remember { mutableFloatStateOf(0.7f) }
    var g2 by remember { mutableFloatStateOf(0.15f) }
    var m2 by remember { mutableIntStateOf(parseModeName(ex?.getString("m2") ?: "lum")) }
    var controlsOpen by remember { mutableStateOf(false) }
    var showRef by remember { mutableStateOf(ex?.getString("ref") != null) }
    var split by remember { mutableFloatStateOf(ex?.getFloat("split", 0.5f) ?: 0.5f) }

    val image = remember { loadLabImage(context, ex?.getString("bg")) }
    val refImage = remember { loadLabImage(context, ex?.getString("ref")) }
    val kind = LabKind.entries[kindIndex]
    val rect = remember(kind) {
        ex?.getString("rect")?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.takeIf { it.size == 4 }?.toIntArray() ?: defaultRect(kind)
    }
    val extraC1 = ex?.getString("c1")
    val emulSigma = ex?.getFloat("emul", 0f) ?: 0f
    val rect2 = ex?.getString("rect2")?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.takeIf { it.size == 4 }?.toIntArray()
    val extraC2 = ex?.getString("c2")

    IrisTheme(darkTheme = dark) {
        val backdrop = rememberBackdrop()
        val colors = IrisTheme.colors
        val shapes = IrisTheme.shapes

        fun customMaterial(): GlassMaterial {
            val c1 = if (extraC1 != null) parseColor(extraC1, Color.Transparent) else Color(g1, g1, g1, a1)
            val c2 = if (extraC2 != null) parseColor(extraC2, Color.Transparent) else Color(g2, g2, g2, a2)
            return GlassMaterial((blurPx / 3f).dp, listOf(GlassLayer(c1, modes[m1]), GlassLayer(c2, modes[m2])))
        }

        @Composable
        fun styleFor(k: LabKind): GlassStyle {
            val base = when {
                custom -> GlassDefaults.surface(customMaterial())
                k == LabKind.Pill -> GlassDefaults.bottomBar()
                k == LabKind.Popup -> GlassDefaults.popup()
                k == LabKind.Dialog -> GlassDefaults.dialog()
                k == LabKind.Sheet -> GlassDefaults.bottomSheet()
                k == LabKind.TopBar -> GlassDefaults.toolbarButton()
                k == LabKind.Button -> GlassDefaults.toolbarButton()
                else -> GlassDefaults.searchBar()
            }
            return if (showRim) base else base.copy(rimHighlight = Color.Transparent, rimShade = Color.Transparent, edgeGlowTop = Color.Transparent, edgeGlowBottom = Color.Transparent, bottomGlow = Color.Transparent)
        }

        fun shapeFor(k: LabKind): Shape = when (k) {
            LabKind.Pill, LabKind.Button, LabKind.Search -> shapes.pill
            LabKind.Popup -> shapes.popup
            LabKind.Dialog -> shapes.dialog
            LabKind.Sheet -> shapes.sheet
            LabKind.TopBar -> RectangleShape
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Box(Modifier.fillMaxSize().backdropSource(backdrop)) {
                LabBackground(LabBg.entries[bgMode], image, Modifier.fillMaxSize())
            }

            val sysRadius = ex?.getInt("sysblur", 0) ?: 0
            if (sysRadius > 0) SystemBlurWindow(sysRadius, rect)
            if (emulSigma > 0f && rect2 != null) {
                // Compose emulation of the same blur (Gaussian, sigma in px) for a side by side look.
                val emulMaterial = GlassMaterial((emulSigma / 3f).dp, emptyList(), sampleOutside = true)
                Box(
                    Modifier
                        .offset { androidx.compose.ui.unit.IntOffset(rect2[0], rect2[1]) }
                        .size(with(density) { (rect2[2] - rect2[0]).toDp() }, with(density) { (rect2[3] - rect2[1]).toDp() })
                        .glass(RectangleShape, GlassStyle(material = emulMaterial), backdrop),
                )
            }

            if (single) {
                if (sysRadius == 0 && emulSigma == 0f && kind == LabKind.Pill && ex?.getBoolean("component", false) == true) {
                    // The real pill component (glass plus the persistent glow under it) at the exact rectangle.
                    var tab by remember { mutableIntStateOf(1) }
                    Box(
                        Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(rect[0], rect[1]) }
                            .size(with(density) { (rect[2] - rect[0]).toDp() }, with(density) { (rect[3] - rect[1]).toDp() }),
                    ) {
                        IrisBottomNavPill(
                            listOf(
                                NavItem("Alarm", IrisIcons.Alarm),
                                NavItem("World clock", IrisIcons.Grid, IrisIcons.GridFilled),
                                NavItem("Stopwatch", IrisIcons.Star, IrisIcons.StarFilled),
                                NavItem("Timer", IrisIcons.Bell),
                            ),
                            tab, { tab = it }, backdrop = backdrop,
                        )
                    }
                } else if (sysRadius == 0 && emulSigma == 0f && kind == LabKind.TopBar) {
                    // The real top bar: progressive blur plus a fading page colour (see ProgressiveBlur).
                    ProgressiveBlur(
                        Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(rect[0], rect[1]) }
                            .size(with(density) { (rect[2] - rect[0]).toDp() }, with(density) { (rect[3] - rect[1]).toDp() }),
                        backdrop = backdrop,
                        maxBlur = ((ex?.getFloat("blur", 26f) ?: 26f) / 3f).dp,
                        tintAlpha = ex?.getFloat("tint", 0.4f) ?: 0.4f,
                    )
                } else if (sysRadius == 0 && emulSigma == 0f) {
                    val style = styleFor(kind)
                    val w = with(density) { (rect[2] - rect[0]).toDp() }
                    val h = with(density) { (rect[3] - rect[1]).toDp() }
                    Box(
                        Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(rect[0], rect[1]) }
                            .size(w, h)
                            .then(if (showFade && kind == LabKind.TopBar) Modifier.verticalFadeMask(GlassFade.TowardsBottom) else Modifier)
                            .glass(shapeFor(kind), style, backdrop)
                            .clip(shapeFor(kind)),
                    )
                }
            } else {
                // Gallery: every glass surface of the kit on top of the chosen background.
                ProgressiveBlur(Modifier.fillMaxWidth().height(112.dp), backdrop = backdrop)
                Row(
                    Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IrisIconButton(IrisIcons.Back, onBack, contentDescription = "Back")
                    IrisText("Blur lab", style = IrisTheme.typography.title, color = Color.White)
                    IrisIconButton(IrisIcons.More, {}, contentDescription = "More")
                }
                Box(Modifier.padding(top = 128.dp, start = 16.dp, end = 16.dp)) {
                    IrisSearchBar("", {}, glass = true, showCancel = false)
                }
                Box(
                    Modifier
                        .offset(16.dp, 200.dp)
                        .width(200.dp)
                        .glass(shapes.popup, styleFor(LabKind.Popup), backdrop)
                        .clip(shapes.popup)
                        .padding(vertical = 8.dp),
                ) {
                    Column {
                        listOf("Edit", "Select all", "Settings").forEach {
                            IrisText(it, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 11.dp), style = IrisTheme.typography.subtitle)
                        }
                    }
                }
                Column(Modifier.offset(236.dp, 204.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    IrisIconButton(IrisIcons.Search, {})
                    IrisIconButton(IrisIcons.Plus, {}, style = IconButtonStyle.Filled)
                }
                Box(
                    Modifier
                        .offset(16.dp, 346.dp)
                        .fillMaxWidth()
                        .padding(end = 32.dp)
                        .height(150.dp)
                        .glass(shapes.dialog, styleFor(LabKind.Dialog), backdrop)
                        .clip(shapes.dialog)
                        .padding(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        IrisText("Dialog", style = IrisTheme.typography.title)
                        IrisText("Window material, strong blur", style = IrisTheme.typography.bodySmall, color = colors.contentSecondary)
                    }
                }
                Box(
                    Modifier
                        .offset(0.dp, 516.dp)
                        .fillMaxWidth()
                        .height(176.dp)
                        .glass(shapes.sheet, styleFor(LabKind.Sheet), backdrop)
                        .clip(shapes.sheet)
                        .padding(24.dp),
                ) {
                    IrisText("Bottom sheet", style = IrisTheme.typography.title)
                }
                Box(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
                    var tab by remember { mutableIntStateOf(0) }
                    IrisBottomNavPill(
                        listOf(
                            NavItem("Alarm", IrisIcons.Alarm),
                            NavItem("World", IrisIcons.Grid, IrisIcons.GridFilled),
                            NavItem("Stopwatch", IrisIcons.Star, IrisIcons.StarFilled),
                            NavItem("Timer", IrisIcons.Bell),
                        ),
                        tab, { tab = it }, backdrop = backdrop,
                    )
                }
            }

            if (showRef && refImage != null) {
                Canvas(Modifier.fillMaxSize()) {
                    clipRect(left = size.width * split) { drawImage(refImage) }
                    drawRect(Color.Magenta, Offset(size.width * split - 1f, 0f), Size(2f, size.height))
                }
            }

            if (!clean) {
                IrisIconButton(
                    IrisIcons.Tune,
                    { controlsOpen = !controlsOpen },
                    Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    style = IconButtonStyle.Filled,
                )
                if (controlsOpen) {
                    IrisCard(Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth().fillMaxHeight(0.62f)) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            IrisText("Background", style = IrisTheme.typography.caption, color = colors.contentSecondary)
                            IrisSegmentedTabs(LabBg.entries.map { it.label }, bgMode, { bgMode = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                IrisText("Dark", Modifier.weight(1f), style = IrisTheme.typography.bodySmall)
                                IrisSwitch(dark, { dark = it })
                                IrisText("Single", Modifier.weight(1f), style = IrisTheme.typography.bodySmall)
                                IrisSwitch(single, { single = it })
                            }
                            if (single) IrisSegmentedTabs(LabKind.entries.map { it.label }, kindIndex, { kindIndex = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                IrisText("Rim", Modifier.weight(1f), style = IrisTheme.typography.bodySmall)
                                IrisSwitch(showRim, { showRim = it })
                                IrisText("Custom", Modifier.weight(1f), style = IrisTheme.typography.bodySmall)
                                IrisSwitch(custom, { custom = it })
                            }
                            if (custom) {
                                IrisText("Blur ${blurPx.roundToInt()} px", style = IrisTheme.typography.bodySmall)
                                IrisSlider(blurPx, { blurPx = it }, valueRange = 0f..120f)
                                IrisText("Layer 1  alpha ${(a1 * 100).roundToInt()} %  grey ${(g1 * 255).roundToInt()}", style = IrisTheme.typography.bodySmall)
                                IrisSlider(a1, { a1 = it })
                                IrisSlider(g1, { g1 = it })
                                IrisSegmentedTabs(modeNames, m1, { m1 = it })
                                IrisText("Layer 2  alpha ${(a2 * 100).roundToInt()} %  grey ${(g2 * 255).roundToInt()}", style = IrisTheme.typography.bodySmall)
                                IrisSlider(a2, { a2 = it })
                                IrisSlider(g2, { g2 = it })
                                IrisSegmentedTabs(modeNames, m2, { m2 = it })
                            }
                            val mat = (if (custom) customMaterial() else when (kind) {
                                LabKind.Pill -> GlassMaterials.bottomBar(dark)
                                LabKind.Popup -> GlassMaterials.popup(dark)
                                LabKind.Dialog -> GlassMaterials.dialog(dark)
                                LabKind.Sheet -> GlassMaterials.bottomSheet(dark)
                                LabKind.TopBar, LabKind.Button -> GlassMaterials.topBar(dark)
                                LabKind.Search -> GlassMaterials.searchBar(dark)
                            })
                            fun hex(c: Color) = "%02X".format((c.red * 255).roundToInt())
                            IrisText(
                                "Over black #${hex(mat.over(Color.Black))}   grey #${hex(mat.over(Color(0xFF808080)))}   white #${hex(mat.over(Color.White))}   blur ${(mat.blur.value * 3f).roundToInt()} px",
                                style = IrisTheme.typography.caption, color = colors.contentSecondary,
                            )
                            if (refImage != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IrisText("Reference", Modifier.weight(1f), style = IrisTheme.typography.bodySmall)
                                    IrisSwitch(showRef, { showRef = it })
                                }
                                if (showRef) IrisSlider(split, { split = it })
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A real compositor blur for comparison: a transparent dialog window whose background is blurred by the system
 * (`Window.setBackgroundBlurRadius`), placed at [rect] (px). On ColorOS this is the same blur the stock popups use
 * (radius units: 160 popup, 200 dialog, 400 bottom sheet) without their colour layers.
 */
@Composable
private fun SystemBlurWindow(radius: Int, rect: IntArray) {
    val density = LocalDensity.current
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (androidx.compose.ui.platform.LocalView.current.parent as androidx.compose.ui.window.DialogWindowProvider).window
        androidx.compose.runtime.SideEffect {
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setDimAmount(0f)
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            )
            window.setGravity(android.view.Gravity.TOP or android.view.Gravity.START)
            val lp = window.attributes
            lp.x = rect[0]
            lp.y = rect[1]
            lp.width = rect[2] - rect[0]
            lp.height = rect[3] - rect[1]
            window.attributes = lp
            window.setBackgroundBlurRadius(radius)
        }
        Box(Modifier.size(with(density) { (rect[2] - rect[0]).toDp() }, with(density) { (rect[3] - rect[1]).toDp() }))
    }
}
