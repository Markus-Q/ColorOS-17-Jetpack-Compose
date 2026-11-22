package dev.irisui.effects

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * Click with ColorOS feedback: an optional indication and a haptic event. The shrink / drag
 * motion of a press is separate, add [touchMotion] in front of this modifier for it.
 * Use `indication = LocalIndication.current` for rows that should show the flat highlight.
 */
@Composable
fun Modifier.irisClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    // The stock apps are quiet: buttons, tabs, chips, cards and rows do not vibrate (measured with the vibrator log);
    // only toggles, the round add button and picker steps do, so no haptic is the default.
    haptic: IrisHaptic? = null,
    indication: Indication? = null,
    role: Role? = Role.Button,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Modifier {
    val haptics = LocalColorOSHaptics.current
    return this.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        role = role,
        onClick = {
            if (haptic != null) haptics.perform(haptic)
            onClick()
        },
    )
}
