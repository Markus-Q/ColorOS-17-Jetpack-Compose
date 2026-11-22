package dev.irisui.popups

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisEasing
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.core.OverlayPortal
import dev.irisui.effects.IrisHaptic
import dev.irisui.effects.GlassDefaults
import dev.irisui.effects.LocalColorOSHaptics
import dev.irisui.effects.irisClickable
import dev.irisui.effects.glass
import dev.irisui.effects.touchMotion
import androidx.compose.foundation.interaction.MutableInteractionSource

/**
 * Confirmation dialog. It docks near the bottom of the screen like on ColorOS: a dimmed page,
 * a rounded glass card, a centered title and two pill buttons side by side.
 */
@Composable
fun IrisDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissText: String? = "Cancel",
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    /** Greys the confirm button out (for example while a text field is still empty). */
    confirmEnabled: Boolean = true,
) {
    val colors = IrisTheme.colors
    IrisDialogContainer(visible, onDismissRequest, modifier) {
        // Metrics of the stock dialog (480 dpi: title box 68 px under the top edge, 6 dp to the message, field 4 dp
        // below it, 22 dp to the buttons, buttons 44 dp high with 8 dp between them and 16 dp around).
        Column(
            Modifier.padding(top = 22.67.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IrisText(
                title,
                Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                style = IrisTheme.typography.dialogTitle,
                textAlign = TextAlign.Center,
            )
            if (message != null) {
                IrisText(
                    message,
                    Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp).fillMaxWidth(),
                    style = IrisTheme.typography.bodySmall,
                    color = colors.contentSecondary,
                )
            }
            if (content != null) {
                Spacer(Modifier.height(4.dp))
                Column(Modifier.padding(horizontal = 24.dp).fillMaxWidth(), content = content)
            }
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (dismissText != null) {
                    DialogButton(dismissText, colors.content, Modifier.weight(1f), onClick = onDismissRequest)
                }
                DialogButton(
                    confirmText,
                    if (destructive) colors.destructive else colors.content,
                    Modifier.weight(1f),
                    enabled = confirmEnabled,
                ) {
                    onConfirm()
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, color: Color, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = IrisTheme.colors
    // Measured in the Settings "device name" dialog: 44 dp pills, the active one 25 % white, a disabled one about half
    // of that with dimmed text.
    val tint = if (colors.isDark) Color.White else Color.Black
    Box(
        modifier
            .height(44.dp)
            .touchMotion(enabled = enabled, shape = IrisTheme.shapes.pill, highlight = colors.content)
            .clip(IrisTheme.shapes.pill)
            .background(tint.copy(alpha = if (enabled) 0.25f else 0.14f))
            .irisClickable(onClick, enabled),
        contentAlignment = Alignment.Center,
    ) {
        IrisText(text, style = IrisTheme.typography.body, color = if (enabled) color else colors.contentTertiary)
    }
}

/**
 * Dialog shell without any content, for fully custom dialogs. The card rides above the navigation bar and the keyboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IrisDialogContainer(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = IrisTheme.colors
    val transition = rememberPopupTransition(
        visible,
        enterSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        exitSpec = tween(170, easing = IrisEasing.Accelerate),
    )
    if (!transition.shown) return
    val haptics = LocalColorOSHaptics.current

    OverlayPortal {
        BackHandler(enabled = visible) { onDismissRequest() }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = transition.progress.value.coerceIn(0f, 1f) }
                    .background(colors.scrim)
                    .pointerInput(Unit) { detectTapGestures { onDismissRequest() } },
            )
            Column(
                modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom),
                    )
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .graphicsLayer {
                        val p = transition.progress.value
                        alpha = p.coerceIn(0f, 1f)
                        translationY = (1f - p) * 56.dp.toPx()
                        val s = 0.9f + 0.1f * p
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures { } }
                    .glass(IrisTheme.shapes.dialog, GlassDefaults.dialog())
                    .clip(IrisTheme.shapes.dialog),
                content = content,
            )
        }
    }
}
