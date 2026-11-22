package dev.irisui.catalog.demos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.irisui.buttons.IrisSecondaryButton
import dev.irisui.core.IrisIcon
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.IrisHaptic
import dev.irisui.effects.GlassDefaults
import dev.irisui.effects.HapticSource
import dev.irisui.effects.LocalColorOSHaptics
import dev.irisui.effects.ReorderableColumn
import dev.irisui.effects.backdropSource
import dev.irisui.effects.glass
import dev.irisui.effects.persistentGlow
import dev.irisui.effects.TouchMotionMath
import dev.irisui.effects.physicsDrag
import dev.irisui.effects.rememberBackdrop
import dev.irisui.effects.touchGlow
import dev.irisui.effects.touchMotion
import dev.irisui.inputs.IrisSlider
import dev.irisui.inputs.IrisSwitch
import dev.irisui.navigation.IrisBackButton
import dev.irisui.navigation.IrisScaffold
import dev.irisui.navigation.IrisSegmentedTabs
import dev.irisui.navigation.IrisTopBar
import kotlin.math.roundToInt

@Composable
fun BlurBackgroundDemo(onBack: () -> Unit) {
    var kind by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    IrisScaffold(
        topBar = { IrisTopBar(title = "Blur background", navigationIcon = { IrisBackButton(onBack) }) },
    ) { _ ->
        val backdrop = rememberBackdrop()
        Box(Modifier.fillMaxSize()) {
            DemoWallpaper(Modifier.fillMaxSize().backdropSource(backdrop))
            val stock = when (kind) {
                0 -> GlassDefaults.popup()
                1 -> GlassDefaults.dialog()
                2 -> GlassDefaults.bottomSheet()
                else -> GlassDefaults.bottomBar()
            }
            val style = stock.copy(material = stock.material?.let { it.copy(blur = it.blur * scale) })
            val shape = when (kind) {
                0 -> IrisTheme.shapes.popup
                1 -> IrisTheme.shapes.dialog
                2 -> IrisTheme.shapes.sheet
                else -> IrisTheme.shapes.pill
            }
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth()
                    .physicsDrag()
                    .glass(shape, style, backdrop)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IrisText("Glass panel", style = IrisTheme.typography.title)
                Caption("Drag me over the wallpaper. The stock ColorOS materials: blur plus colour layers that keep the hue of what is behind.")
                IrisSegmentedTabs(listOf("Popup", "Dialog", "Sheet", "Pill"), kind, { kind = it })
                IrisText("Blur ×${"%.1f".format(scale)}", style = IrisTheme.typography.bodySmall)
                IrisSlider(scale, { scale = it }, valueRange = 0f..2f)
            }
        }
    }
}

/** A touchable tile that shows the scale ColorOS shrinks an element of this size to. */
@Composable
private fun PressTile(
    label: String,
    width: Dp,
    height: Dp,
    color: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    cancelOnLeave: Boolean = false,
) {
    val density = LocalDensity.current
    val scale = TouchMotionMath.pressedScale(with(density) { width.toPx() }, with(density) { height.toPx() }, density.density)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(width, height)
                .touchMotion(shape = shape, cancelOnLeave = cancelOnLeave)
                .clip(shape)
                .background(color),
        )
        IrisText(
            "$label  ·  ×${"%.3f".format(scale)}",
            style = IrisTheme.typography.caption,
            color = IrisTheme.colors.contentSecondary,
        )
    }
}

@Composable
fun PressAnimationDemo(onBack: () -> Unit) {
    val pill = IrisTheme.shapes.pill
    val card = IrisTheme.shapes.card
    DemoPage("Press animation", onBack, subtitle = "Everything shrinks while held, small and wide things the most") {
        demoSection("Compact elements") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                PressTile("40 dp", 40.dp, 40.dp, Color(0xFF3A6BFF), pill)
                PressTile("56 dp", 56.dp, 56.dp, Color(0xFF9B51E0), pill)
                PressTile("92 dp", 92.dp, 92.dp, Color(0xFFFF5C8A), card)
            }
        }
        demoSection("Wide elements") {
            PressTile("328 × 56 dp", 328.dp, 56.dp, Color(0xFF16A085), pill, Modifier.fillMaxWidth())
            PressTile("328 × 160 dp", 328.dp, 160.dp, Color(0xFFFF9F0A), card, Modifier.fillMaxWidth())
        }
        demoCard {
            Caption(
                "Modifier.touchMotion(shape) adds the whole ColorOS touch animation to any element: it shrinks by an amount " +
                    "taken from its size and shape (0.85 for small, 0.97 for large), the fill gets lighter, and on release it " +
                    "springs back with an overshoot. Wide shapes use springs of their own. Put it before clip and background.",
            )
        }
    }
}

@Composable
fun HapticsDemo(onBack: () -> Unit) {
    val haptics = LocalColorOSHaptics.current
    var enabled by remember { mutableStateOf(haptics.enabled) }
    var source by remember { mutableIntStateOf(haptics.source.ordinal) }
    DemoPage("Haptics", onBack, subtitle = "Feel every event on the device motor") {
        demoCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IrisText("Haptics enabled")
                IrisSwitch(enabled, { enabled = it; haptics.enabled = it })
            }
        }
        demoSection("Source") {
            IrisSegmentedTabs(listOf("ColorOS", "System", "Direct"), source, { source = it; haptics.source = HapticSource.entries[it] })
            Caption("ColorOS fires the haptic constants the stock apps use (302 for toggles and the add button, 307 for picker steps) and stays silent where they are silent. System uses View.performHapticFeedback with the standard constants and follows the system touch feedback setting. Direct sends predefined effects (CLICK, TICK, HEAVY_CLICK, THUD, POP) straight to the vibrator.")
        }
        demoSection("ColorOS constants") {
            val view = androidx.compose.ui.platform.LocalView.current
            listOf(
                "300 long" to 300, "301 keyboard" to 301, "302 granular" to 302, "303 short" to 303,
                "304 triple" to 304, "305 stepping" to 305, "306 edge" to 306, "307 edge list" to 307,
                "308 granular sync" to 308, "14 squeeze" to 14,
            ).chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { (label, constant) ->
                        IrisSecondaryButton(label, { view.performHapticFeedback(constant) }, Modifier.weight(1f))
                    }
                }
            }
        }
        demoSection("Events") {
            IrisHaptic.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { event ->
                        IrisSecondaryButton(event.name, { haptics.perform(event) }, Modifier.weight(1f))
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun TouchGlowDemo(onBack: () -> Unit) {
    DemoPage("Touch glow", onBack, subtitle = "A light follows your finger and fades out") {
        demoSection("Surfaces") {
            listOf(
                "White" to Color.White.copy(alpha = 0.22f),
                "Accent" to IrisTheme.colors.accent.copy(alpha = 0.55f),
                "Warm" to Color(0xFFFFB020).copy(alpha = 0.5f),
            ).forEach { (label, glow) ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(IrisTheme.shapes.card)
                        .background(IrisTheme.colors.surface)
                        .touchGlow(color = glow, radius = 150.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    IrisText("$label glow, touch and drag", color = IrisTheme.colors.contentSecondary)
                }
            }
        }
        demoCard {
            Caption("Modifier.touchGlow(color, radius). Apply clip(shape) before it so the light stays inside the shape. It only observes touches and never blocks clicks.")
        }
    }
}

@Composable
fun PersistentGlowDemo(onBack: () -> Unit) {
    val palette = listOf(Color(0xFF1A8CFF), Color(0xFFFFB800), Color(0xFF34C759), Color(0xFFFF375F), Color(0xFFAF52DE))
    var selected by remember { mutableIntStateOf(0) }
    var alpha by remember { mutableFloatStateOf(0.6f) }
    var spread by remember { mutableFloatStateOf(0.5f) }
    val color = palette[selected]
    DemoPage("Persistent glow", onBack, subtitle = "The light that always sits under a button") {
        demoSection("Preview") {
            Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .persistentGlow(color, (spread * 60f).dp, 6.dp, alpha)
                            .clip(IrisTheme.shapes.pill)
                            .background(color),
                        contentAlignment = Alignment.Center,
                    ) { IrisIcon(IrisIcons.Plus, null, tint = Color.White, size = 26.dp) }
                    Box(
                        Modifier
                            .size(150.dp, 48.dp)
                            .persistentGlow(color, (spread * 60f).dp, 6.dp, alpha)
                            .clip(IrisTheme.shapes.pill)
                            .background(color),
                        contentAlignment = Alignment.Center,
                    ) { IrisText("Button", color = Color.White, style = IrisTheme.typography.button) }
                }
            }
        }
        demoCard("Color") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                palette.forEachIndexed { index, c ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(IrisTheme.shapes.pill)
                            .background(c)
                            .border(
                                BorderStroke(2.dp, if (index == selected) IrisTheme.colors.content else Color.Transparent),
                                IrisTheme.shapes.pill,
                            )
                            .clickable { selected = index },
                    )
                }
            }
        }
        demoCard("Intensity") { IrisSlider(alpha, { alpha = it }) }
        demoCard("Spread") { IrisSlider(spread, { spread = it }) }
        demoCard {
            Caption("Modifier.persistentGlow(color, spread, offsetY, alpha, intensity). Draw it behind the button; the optional intensity lambda lets it bloom while pressed without recomposing.")
        }
    }
}

@Composable
fun DragPhysicsDemo(onBack: () -> Unit) {
    val items = remember { mutableStateListOf("Alarm", "Camera", "Clock", "Files", "Gallery", "Notes") }
    DemoPage("Drag physics", onBack, subtitle = "Hold and drag: elements stretch like rubber") {
        demoSection("Elastic touch") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                PressTile("keeps pressing", 56.dp, 56.dp, IrisTheme.colors.accent, IrisTheme.shapes.pill)
                PressTile("cancels outside", 56.dp, 56.dp, Color(0xFF3A3A3C), IrisTheme.shapes.pill, cancelOnLeave = true)
            }
            PressTile("pill", 328.dp, 56.dp, Color(0xFF3A3A3C), IrisTheme.shapes.pill, Modifier.fillMaxWidth())
            PressTile("card", 328.dp, 120.dp, IrisTheme.colors.surface, IrisTheme.shapes.card, Modifier.fillMaxWidth())
            Caption(
                "Hold an element and drag. It follows with rubber-band resistance (at most 28 dp), grows along the drag and " +
                    "squeezes across it, and springs back with an overshoot, carrying the speed you let go with.",
            )
        }
        demoSection("Free drag (lift, tilt)") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(IrisTheme.shapes.card)
                    .background(IrisTheme.colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(112.dp)
                        .physicsDrag()
                        .shadow(10.dp, IrisTheme.shapes.card, ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(IrisTheme.shapes.card)
                        .background(IrisTheme.colors.accent),
                    contentAlignment = Alignment.Center,
                ) { IrisText("Drag me", color = Color.White, style = IrisTheme.typography.headline) }
            }
            Caption("It resists like rubber past the edge, leans into the movement and springs back with the speed you let go with.")
        }
        demoSection("Re-order") {
            ReorderableColumn(
                items = items,
                key = { it },
                onMove = { from, to -> items.add(to, items.removeAt(from)) },
            ) { item, dragging, handle ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .shadow(if (dragging) 14.dp else 0.dp, IrisTheme.shapes.card, ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(IrisTheme.shapes.card)
                        .background(if (dragging) IrisTheme.colors.surfaceElevated else IrisTheme.colors.surface)
                        .padding(start = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IrisText(item, Modifier.weight(1f))
                    Box(handle.size(56.dp), contentAlignment = Alignment.Center) {
                        IrisIcon(IrisIcons.Tune, "Drag to re-order", tint = IrisTheme.colors.contentSecondary)
                    }
                }
            }
        }
    }
}
