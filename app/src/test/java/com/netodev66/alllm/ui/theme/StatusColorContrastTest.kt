package com.netodev66.alllm.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Guards the legibility of the status palette.
 *
 * The palette replaced 23 hand-written greens and seven `luminance() < 0.5f`
 * theme guesses, several of which painted dark green on a dark background. A
 * colour that fails WCAG AA is not a compile error and does not break a test that
 * only checks the UI builds, so without this file the next "nicer green" would
 * silently reintroduce the problem it took a pass to remove.
 *
 * Ratio is computed per WCAG 2.1 relative luminance. The bar is 4.5:1, the AA
 * threshold for normal-size text — and most of these colours are used at 10-12sp,
 * which is well below the "large text" exemption.
 */
class StatusColorContrastTest {

    private companion object {
        const val MIN_RATIO = 4.5
        /** Pane fill from [DarkDesign] and [LightDesign]; the usual status backdrop. */
        val DARK_PANE = Color(0xFF151A23)
        val DARK_BG = Color(0xFF0B0E14)
        val LIGHT_PANE = Color(0xFFFFFFFF)
        val LIGHT_BG = Color(0xFFF6F8FB)
    }

    private fun channel(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun assertLegible(label: String, fg: Color, bg: Color) {
        val r = ratio(fg, bg)
        assertTrue(
            "$label contrast is %.2f:1, below the %.1f:1 AA floor".format(r, MIN_RATIO),
            r >= MIN_RATIO,
        )
    }

    private fun foregrounds(t: StatusColors) = listOf(
        "success" to t.success,
        "warning" to t.warning,
        "info" to t.info,
        "danger" to t.danger,
    )

    private fun containerPairs(t: StatusColors) = listOf(
        "successContainer" to (t.onSuccessContainer to t.successContainer),
        "warningContainer" to (t.onWarningContainer to t.warningContainer),
        "infoContainer" to (t.onInfoContainer to t.infoContainer),
        "dangerContainer" to (t.onDangerContainer to t.dangerContainer),
    )

    @Test
    fun `dark status foregrounds are legible on both pane and background`() {
        foregrounds(DarkStatusColors).forEach { (name, color) ->
            assertLegible("dark $name on pane", color, DARK_PANE)
            assertLegible("dark $name on background", color, DARK_BG)
        }
    }

    @Test
    fun `light status foregrounds are legible on both pane and background`() {
        foregrounds(LightStatusColors).forEach { (name, color) ->
            assertLegible("light $name on pane", color, LIGHT_PANE)
            assertLegible("light $name on background", color, LIGHT_BG)
        }
    }

    @Test
    fun `every container carries legible text in both themes`() {
        listOf("dark" to DarkStatusColors, "light" to LightStatusColors).forEach { (theme, tokens) ->
            containerPairs(tokens).forEach { (name, pair) ->
                val (on, container) = pair
                assertLegible("$theme $name text", on, container)
            }
        }
    }

    @Test
    fun `an on-color is legible on the solid fill it belongs to`() {
        val pairs = listOf(
            "dark onSuccess" to (DarkStatusColors.onSuccess to DarkStatusColors.success),
            "dark onWarning" to (DarkStatusColors.onWarning to DarkStatusColors.warning),
            "dark onInfo" to (DarkStatusColors.onInfo to DarkStatusColors.info),
            "dark onDanger" to (DarkStatusColors.onDanger to DarkStatusColors.danger),
            "light onSuccess" to (LightStatusColors.onSuccess to LightStatusColors.success),
            "light onWarning" to (LightStatusColors.onWarning to LightStatusColors.warning),
            "light onInfo" to (LightStatusColors.onInfo to LightStatusColors.info),
            "light onDanger" to (LightStatusColors.onDanger to LightStatusColors.danger),
        )
        pairs.forEach { (name, pair) ->
            val (on, fill) = pair
            assertLegible(name, on, fill)
        }
    }

    @Test
    fun `the retired dark green is no longer usable as status text`() {
        // 0xFF2E7D32 was the hardcoded "success" green. It fails on a dark pane,
        // which is exactly why it must not come back.
        val retired = Color(0xFF2E7D32)
        assertTrue(
            "the old hardcoded green should fail on a dark pane, so this test still means something",
            ratio(retired, DARK_PANE) < MIN_RATIO,
        )
    }

    @Test
    fun `every fill maps to a text colour legible on it`() {
        // statusOn() picks the text for a solid status fill. Its first version
        // returned the *container* on-colour for the solid fill, so a light green
        // fill got light green text — the exact defect the palette was meant to
        // remove. Each solid fill and each container must resolve to something
        // legible, and the two must differ (a solid fill is not a container).
        listOf("dark" to DarkStatusColors, "light" to LightStatusColors).forEach { (theme, t) ->
            val fills = listOf(
                "success" to (t.success to t.onSuccess),
                "warning" to (t.warning to t.onWarning),
                "info" to (t.info to t.onInfo),
                "danger" to (t.danger to t.onDanger),
                "successContainer" to (t.successContainer to t.onSuccessContainer),
                "warningContainer" to (t.warningContainer to t.onWarningContainer),
                "infoContainer" to (t.infoContainer to t.onInfoContainer),
                "dangerContainer" to (t.dangerContainer to t.onDangerContainer),
            )
            fills.forEach { (name, pair) ->
                val (fill, expectedOn) = pair
                assertLegible("$theme statusOn($name)", expectedOn, fill)
            }
            assertTrue(
                "$theme: a solid fill and its container must not share a text colour",
                t.onSuccess != t.onSuccessContainer &&
                    t.onWarning != t.onWarningContainer &&
                    t.onInfo != t.onInfoContainer &&
                    t.onDanger != t.onDangerContainer,
            )
        }
    }
}
