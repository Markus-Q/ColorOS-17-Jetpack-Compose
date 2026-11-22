package dev.irisui.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import kotlinx.coroutines.launch

/**
 * Indication for clickable surfaces that are not list rows (menu items, custom clickables): a flat
 * highlight of the press overlay (20 % white in dark mode, measured on ColorOS Settings rows). It comes in
 * with a critically damped spring (response 0.3 s) and fades out in about 0.2 s.
 */
object IrisIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressHighlightNode(interactionSource)

    override fun hashCode(): Int = -1
    override fun equals(other: Any?): Boolean = other === this
}

private fun responseSpring(response: Float) =
    spring<Float>(dampingRatio = 1f, stiffness = (2.0 * Math.PI / response).let { (it * it).toFloat() })

private val PressIn = responseSpring(0.3f)
private val PressOut = responseSpring(0.21f)

private class PressHighlightNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val alpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> launch { alpha.animateTo(1f, PressIn) }
                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> launch { alpha.animateTo(0f, PressOut) }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val a = alpha.value
        if (a > 0f) {
            val overlay = currentValueOf(LocalColorOSColors).pressOverlay
            drawRect(overlay.copy(alpha = overlay.alpha * a))
        }
    }
}
