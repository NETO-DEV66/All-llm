package com.netodev66.alllm.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * App-wide press feedback: a spring "bounce" plus a soft highlight.
 *
 * Provided through [androidx.compose.foundation.LocalIndication] by [PocketTheme],
 * so every `clickable` in the app picks it up without touching call sites — the
 * same way Material's ripple would. Components that already animate themselves
 * ([bounceClick], [PocketButton]) pass `indication = null` and are unaffected, so
 * nothing scales twice.
 *
 * The press is deliberately gentler than [bounceClick]'s 0.94–0.96: this one can
 * land on full-width list rows, where a larger scale would read as a glitch.
 */
object BouncyIndication : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        BouncyIndicationNode(interactionSource)

    // Stateless singleton: all animation state lives in the per-node instance, so
    // sharing one factory across the composition is safe.
    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = -1
}

private class BouncyIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {

    private val scale = Animatable(1f)
    private val highlight = Animatable(0f)

    // Read the token table directly: `motion` is @Composable and this node is not.
    private val bounceSpec = DefaultMotion.bouncy.asSpec<Float>()

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        launch { scale.animateTo(PressedScale, tween(90, easing = FastOutSlowInEasing)) }
                        launch { highlight.animateTo(1f, tween(120)) }
                    }
                    is PressInteraction.Release -> {
                        // Springs back past 1.0 — the bounce.
                        launch { scale.animateTo(1f, bounceSpec) }
                        launch { highlight.animateTo(0f, tween(260)) }
                    }
                    is PressInteraction.Cancel -> {
                        launch { scale.animateTo(1f, tween(140)) }
                        launch { highlight.animateTo(0f, tween(180)) }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        // `scale`'s lambda receiver is a plain DrawScope, so capture the
        // ContentDrawScope to keep calling its drawContent().
        val content = this
        scale(scale.value, pivot = center) { content.drawContent() }

        val alpha = highlight.value
        if (alpha <= 0.001f) return
        // Radius from the short edge: pills and circles round fully, larger panels
        // stay subtly rounded. A soft highlight reads correctly on both.
        val radius = (size.minDimension / 2f).coerceAtMost(24.dp.toPx())
        drawRoundRect(
            color = Color.White.copy(alpha = HighlightAlpha * alpha),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(radius, radius),
        )
    }

    private companion object {
        const val PressedScale = 0.98f
        const val HighlightAlpha = 0.07f
    }
}
