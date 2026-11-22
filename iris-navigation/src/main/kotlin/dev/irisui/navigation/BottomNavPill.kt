package dev.irisui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisIcon
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.Backdrop
import dev.irisui.effects.IrisHaptic
import dev.irisui.effects.GlassDefaults
import dev.irisui.effects.LocalBackdrop
import dev.irisui.effects.LocalColorOSHaptics
import dev.irisui.effects.glass
import dev.irisui.effects.persistentGlow
import dev.irisui.effects.touchGlow
import dev.irisui.effects.touchMotion
import kotlin.math.abs

/** One tab of [IrisBottomNavPill]. */
@Immutable
class NavItem(
    val label: String,
    val icon: ImageVector,
    /** Icon shown while selected (ColorOS fills the icon of the active tab). */
    val selectedIcon: ImageVector = icon,
)

/**
 * The floating glass pill at the bottom of ColorOS apps. Tap or slide a finger across it to
 * switch tabs: the pill shrinks while touched (and follows the finger with rubber-band
 * resistance) and a soft light follows the finger. There is no capsule behind the selected tab: it just
 * switches to its filled icon and full-strength tint, like the Clock and Files pills.
 */
@Composable
fun IrisBottomNavPill(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalBackdrop.current,
) {
    val colors = IrisTheme.colors
    val motion = IrisTheme.motion
    val haptics = LocalColorOSHaptics.current
    val shape = IrisTheme.shapes.pill
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnChange by rememberUpdatedState(onSelectedChange)
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            // The whole pill shrinks while held and follows the finger; sliding across it still switches tabs.
            // Its light is the wide touch glow below, not a uniform brightening.
            .touchMotion(shape = shape, yieldToConsumers = false, highlightAlpha = 0f)
            .glass(shape, GlassDefaults.bottomBar(), backdrop)
            .clip(shape)
            .touchGlow(color = colors.content.copy(alpha = 0.135f), radius = 140.dp)
            .pointerInput(items.size) {
                awaitEachGesture {
                    // The tabs sit 8 dp inside the pill (stock: item centers at 189, 423, 657, 891 px of a 984 px pill).
                    val inset = 8.dp.toPx()
                    fun indexAt(x: Float) =
                        ((x - inset) / ((size.width - 2f * inset) / items.size)).toInt().coerceIn(0, items.lastIndex)

                    fun commit(index: Int) {
                        // The real pill is silent when the tab changes (tap or slide).
                        if (index != currentSelected) currentOnChange(index)
                    }

                    val down = awaitFirstDown(requireUnconsumed = false)
                    commit(indexAt(down.position.x))
                    drag(down.id) { change ->
                        commit(indexAt(change.position.x))
                        change.consume()
                    }
                }
            },
    ) {
        // No capsule behind the selected item (the real pill has none): the selected tab just switches to
        // its filled icon and full-strength tint, and the glow follows the finger.
        Row(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 8.dp)) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val tint by animateColorAsState(
                    // selected: 90 % label color, others 72 % (measured on the Clock pill)
                    if (selected) colors.content else colors.contentVariant,
                    label = "navTint",
                )
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IrisIcon(if (selected) item.selectedIcon else item.icon, item.label, tint = tint, size = 24.dp)
                    IrisText(item.label, style = IrisTheme.typography.tabLabel, color = tint, maxLines = 1)
                }
            }
        }
    }
}
