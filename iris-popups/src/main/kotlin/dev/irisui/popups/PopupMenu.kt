package dev.irisui.popups

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisIcon
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.core.OverlayPortal
import dev.irisui.effects.GlassDefaults
import dev.irisui.effects.TouchDragAxes
import dev.irisui.effects.glass
import dev.irisui.effects.touchMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt

private fun response(seconds: Float) = (2.0 * PI / seconds).let { (it * it).toFloat() }

/** Critically damped springs measured on the Clock "more" menu (480 dpi OnePlus 15). */
private val EnterSpring = spring<Float>(dampingRatio = 1f, stiffness = response(0.25f))
private val ExitSpring = spring<Float>(dampingRatio = 1f, stiffness = response(0.16f))
private val LightSpring = spring<Float>(dampingRatio = 1f, stiffness = response(0.17f))
private val PillFadeSpring = spring<Float>(dampingRatio = 1f, stiffness = response(0.30f))
private val PillMoveSpring = spring<Rect>(dampingRatio = 1f, stiffness = response(0.15f))

/**
 * Touch state of one open menu. The real ColorOS popup is not a list of independent buttons: one finger on the panel
 * lights up the whole panel and a single capsule highlight under the finger slides between the rows; the row under the
 * finger when it lifts is the one that gets selected, lifting outside only lets the finger go.
 */
@Stable
internal class MenuController(val dismiss: () -> Unit) {
    private class Entry(var onClick: () -> Unit, var bounds: Rect = Rect.Zero)

    private val entries = LinkedHashMap<Any, Entry>()
    var panel: LayoutCoordinates? = null
    var pressedKey by mutableStateOf<Any?>(null)
    var fingerDown by mutableStateOf(false)

    fun register(key: Any, onClick: () -> Unit) {
        entries[key] = Entry(onClick)
    }

    fun update(key: Any, onClick: () -> Unit) {
        entries[key]?.onClick = onClick
    }

    fun setBounds(key: Any, bounds: Rect) {
        entries[key]?.bounds = bounds
    }

    fun unregister(key: Any) {
        entries.remove(key)
        if (pressedKey == key) pressedKey = null
    }

    fun boundsOf(key: Any?): Rect? = key?.let { entries[it]?.bounds }

    fun keyAt(position: Offset): Any? = entries.entries.firstOrNull { it.value.bounds.contains(position) }?.key

    fun select(key: Any?) {
        val entry = key?.let { entries[it] } ?: return
        entry.onClick()
        dismiss()
    }
}

private val LocalMenuController = compositionLocalOf<MenuController?> { null }

/**
 * Anchors a popup menu to [anchor]. The menu opens as a blurred glass card that pops out of a point near the anchor
 * (79 % across, 33 % down the panel), the way the "more" menus of ColorOS apps do, and closes the same way, faster.
 */
@Composable
fun IrisPopupMenuBox(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * For a menu opened from a full width list row (a dropdown preference): the panel is centered on the screen and
     * its top edge sits at the vertical center of the row, instead of hugging the anchor's edge like a "more" menu.
     */
    centerOnScreen: Boolean = false,
    menu: @Composable ColumnScope.() -> Unit,
    anchor: @Composable () -> Unit,
) {
    var anchorBounds by remember { mutableStateOf(Rect.Zero) }
    Box(modifier.onGloballyPositioned { anchorBounds = it.boundsInRoot() }) { anchor() }

    val transition = rememberPopupTransition(expanded, enterSpec = EnterSpring, exitSpec = ExitSpring)
    if (!transition.shown) return
    val window = LocalWindowInfo.current.containerSize
    val alignEnd = anchorBounds.center.x > window.width / 2f
    val below = anchorBounds.center.y < window.height * 0.62f
    // The real menu grows from a point inside its top-right area (79 % across, 33 % down), not from the corner.
    val origin = TransformOrigin(if (alignEnd) 0.79f else 0.21f, if (below) 0.33f else 0.67f)
    val currentDismiss by rememberUpdatedState(onDismissRequest)
    val controller = remember { MenuController { currentDismiss() } }

    OverlayPortal {
        BackHandler(enabled = expanded) { onDismissRequest() }
        // A touch outside the panel closes the menu right when the finger lands, and is not passed on.
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    // Touches on the panel are consumed by the panel itself, so only outside touches arrive here.
                    val down = awaitFirstDown()
                    down.consume()
                    onDismissRequest()
                }
            },
        ) {
            PopupPositionLayout(anchorBounds, alignEnd, below, centerOnScreen) {
                CompositionLocalProvider(LocalMenuController provides controller) {
                    MenuPanel(controller, transition.progress, origin, menu)
                }
            }
        }
    }
}

@Composable
private fun MenuPanel(
    controller: MenuController,
    progress: Animatable<Float, *>,
    origin: TransformOrigin,
    menu: @Composable ColumnScope.() -> Unit,
) {
    val colors = IrisTheme.colors
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val pillAlpha = remember { Animatable(0f) }
    val lightAlpha = remember { Animatable(0f) }
    val pillRect = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    val inset = with(density) { 8.dp.toPx() }
    val pressed = controller.pressedKey

    // Panel light, pill fade and pill slide follow the finger: all are springs, the pill hops to a new row quickly.
    LaunchedEffect(pressed) {
        val rect = controller.boundsOf(pressed)
        if (pressed != null && rect != null) {
            val target = Rect(rect.left + inset, rect.top, rect.right - inset, rect.bottom)
            if (pillAlpha.value < 0.02f) pillRect.snapTo(target) else scope.launch { pillRect.animateTo(target, PillMoveSpring) }
            launch { pillAlpha.animateTo(1f, PillFadeSpring) }
        } else {
            launch { pillAlpha.animateTo(0f, LightSpring) }
        }
    }
    LaunchedEffect(controller.fingerDown) {
        lightAlpha.animateTo(if (controller.fingerDown) 1f else 0f, LightSpring)
    }

    val shape = IrisTheme.shapes.popup
    val tint = if (colors.isDark) Color.White else Color.Black
    Column(
        Modifier
            .graphicsLayer {
                val p = progress.value.coerceIn(0f, 1f)
                alpha = p
                scaleX = p
                scaleY = p
                transformOrigin = origin
            }
            .widthIn(min = 168.dp, max = 256.dp)
            .width(IntrinsicSize.Max)
            // The whole panel follows a dragging finger with the rubber-band physics (no shrink: the real panel only lights up).
            // The rows keep the panel's own touch handling, which also marks every move as taken.
            .touchMotion(
                shape = shape,
                pressedScale = 1f,
                claimAxes = TouchDragAxes.None,
                highlightAlpha = 0f,
                yieldToConsumers = false,
            )
            .glass(shape, GlassDefaults.popup())
            .clip(shape)
            .onGloballyPositioned { controller.panel = it }
            .pointerInput(controller) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    controller.fingerDown = true
                    controller.pressedKey = controller.keyAt(down.position)
                    var selected = true
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        val inside = change.position.x in 0f..size.width.toFloat() && change.position.y in 0f..size.height.toFloat()
                        controller.pressedKey = if (inside) controller.keyAt(change.position) else null
                        controller.fingerDown = inside && change.pressed
                        if (!change.pressed) {
                            // Lifting on a row selects it; the highlight is gone at once and the menu closes.
                            val key = controller.pressedKey
                            controller.fingerDown = false
                            controller.pressedKey = null
                            scope.launch { pillAlpha.snapTo(0f) }
                            if (selected && key != null) controller.select(key)
                            break
                        }
                        if (!inside) selected = true
                    }
                    controller.fingerDown = false
                    controller.pressedKey = null
                }
            }
            .drawBehind {
                // 9.5 % tint over the whole panel while a finger is down (Clock menu over black: 13 -> 36), then the 16 %
                // capsule under the finger (36 -> 70).
                if (lightAlpha.value > 0.001f) drawRect(tint.copy(alpha = 0.095f * lightAlpha.value))
                if (pillAlpha.value > 0.001f) {
                    val r = pillRect.value
                    drawRoundRect(
                        tint.copy(alpha = 0.161f * pillAlpha.value),
                        topLeft = Offset(r.left, r.top),
                        size = Size(r.width, r.height),
                        cornerRadius = CornerRadius(r.height / 2f),
                    )
                }
            }
            .padding(vertical = 8.dp),
        content = menu,
    )
}

@Composable
private fun PopupPositionLayout(
    anchor: Rect,
    alignEnd: Boolean,
    below: Boolean,
    centerOnScreen: Boolean,
    content: @Composable () -> Unit,
) {
    val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
    val margin = with(LocalDensity.current) { 16.dp.roundToPx() }
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val placeable = measurables.first().measure(
            Constraints(maxWidth = constraints.maxWidth - margin * 2, maxHeight = constraints.maxHeight - margin * 2),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            val x = when {
                centerOnScreen -> (constraints.maxWidth - placeable.width) / 2
                alignEnd -> anchor.right.roundToInt() - placeable.width
                else -> anchor.left.roundToInt()
            }
            val y = when {
                centerOnScreen -> anchor.center.y.roundToInt()
                below -> anchor.bottom.roundToInt() + gap
                else -> anchor.top.roundToInt() - gap - placeable.height
            }
            placeable.place(
                x.coerceIn(margin, (constraints.maxWidth - margin - placeable.width).coerceAtLeast(margin)),
                y.coerceIn(margin, (constraints.maxHeight - margin - placeable.height).coerceAtLeast(margin)),
            )
        }
    }
}

/**
 * One row of a popup menu: 40 dp high, text 20 dp from the panel edge (15 sp). Pressing, sliding and selecting is
 * handled by the panel, so rows have no touch feedback of their own. Closes the menu after [onClick].
 */
@Composable
fun IrisMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    checked: Boolean = false,
    destructive: Boolean = false,
) {
    val colors = IrisTheme.colors
    val controller = LocalMenuController.current
    val key = remember { Any() }
    val currentOnClick by rememberUpdatedState(onClick)
    if (controller != null) {
        DisposableEffect(controller, key) {
            controller.register(key) { currentOnClick() }
            onDispose { controller.unregister(key) }
        }
    }
    val tint: Color = if (destructive) colors.destructive else colors.content
    Row(
        modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val panel = controller?.panel
                if (controller != null && panel != null && panel.isAttached) {
                    controller.setBounds(key, panel.localBoundingBoxOf(coordinates))
                }
            }
            .defaultMinSize(minHeight = 40.dp)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) IrisIcon(icon, null, tint = tint, size = 24.dp)
        IrisText(text, Modifier.weight(1f), style = IrisTheme.typography.menuItem, color = tint)
        if (checked) {
            Spacer(Modifier.width(8.dp))
            IrisIcon(IrisIcons.Check, null, tint = tint, size = 24.dp)
        }
    }
}

/** Gap between groups of rows: 8 dp high with a thin, 1 dp line in the theme's divider colour (grey in both themes). */
@Composable
fun IrisMenuDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(IrisTheme.colors.divider),
        )
    }
}
