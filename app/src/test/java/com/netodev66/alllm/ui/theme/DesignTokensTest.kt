package com.netodev66.alllm.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The glass look has one rule that is easy to break by accident: a pane that holds
 * body text must be readable, so only chrome may be genuinely translucent. These
 * assertions fail if a future tweak makes the editor, chat or settings surfaces
 * see-through again.
 */
class DesignTokensTest {

    private val allTokens = listOf("dark" to DarkDesign, "light" to LightDesign)

    @Test
    fun `content panes are opaque enough to read text on`() {
        allTokens.forEach { (name, tokens) ->
            // 0xF2 alpha ≈ 95%: the backdrop is effectively invisible behind text.
            assertTrue(
                "$name fill is too translucent for body text: ${tokens.fill}",
                tokens.fill.alpha >= 0.94f,
            )
            assertTrue(
                "$name fillStrong is too translucent for a raised pane: ${tokens.fillStrong}",
                tokens.fillStrong.alpha >= 0.94f,
            )
        }
    }

    @Test
    fun `reading surfaces are fully opaque`() {
        allTokens.forEach { (name, tokens) ->
            assertEquals(
                "$name readingSurface must be opaque: the editor and terminal need a still background",
                1f,
                tokens.readingSurface.alpha,
                0f,
            )
        }
    }

    @Test
    fun `modal bodies stay readable over the dimmed app behind them`() {
        allTokens.forEach { (name, tokens) ->
            assertTrue(
                "$name modalSurface is too translucent: ${tokens.modalSurface}",
                tokens.modalSurface.alpha >= 0.96f,
            )
        }
    }

    @Test
    fun `only chrome is genuinely translucent`() {
        allTokens.forEach { (name, tokens) ->
            assertTrue(
                "$name chromeFill should let the backdrop through: ${tokens.chromeFill}",
                tokens.chromeFill.alpha < 0.8f,
            )
        }
    }

    @Test
    fun `glass edges carry a light and a shadow side`() {
        allTokens.forEach { (name, tokens) ->
            assertTrue("$name needs a visible edge light", tokens.edgeLight.alpha > 0.15f)
            assertTrue("$name needs a visible edge shadow", tokens.edgeShadow.alpha > 0.05f)
            assertTrue("$name needs a specular sheen", tokens.specular.alpha > 0.05f)
        }
    }

    @Test
    fun `the backdrop has enough stops and glows to read as depth`() {
        allTokens.forEach { (name, tokens) ->
            assertTrue("$name backdrop needs at least three stops", tokens.backdrop.size >= 3)
            assertTrue("$name backdrop needs accent glows", tokens.backdropGlow.isNotEmpty())
        }
    }
}
