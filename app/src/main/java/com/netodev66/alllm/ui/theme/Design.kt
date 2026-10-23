package com.netodev66.alllm.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Solid design tokens.
 *
 * Panes are opaque surfaces with a real hairline border and a real elevation
 * shadow, which reads cleaner over the dense developer UI than a translucent
 * veil and costs no per-frame blur.
 *
 * The token set is deliberately small: a pane fill, a raised fill, an inset fill,
 * two border weights, a shadow tint, and the near-opaque reading surface used by
 * the code editor and terminal.
 */
@Immutable
data class DesignTokens(
    /** Default pane fill. Opaque; nothing shows through it. */
    val fill: Color,
    /** Raised fill for containers that sit above a pane (menus, sheets, dialogs). */
    val fillStrong: Color,
    /** Inset fill for wells and tracks that sit below a pane. */
    val fillSubtle: Color,
    /** Hairline border around a pane. */
    val border: Color,
    /** Stronger border for focused or raised containers. */
    val borderStrong: Color,
    /** Tint of the ambient drop shadow; a neutral black reads as a flat hole on dark. */
    val ambientTint: Color,
    /**
     * Near-opaque fill for text-dense panes (code editor, terminal). Kept dark in
     * both themes because the syntax palette is tuned for dark.
     */
    val readingSurface: Color,
    val isDark: Boolean,
)

internal val DarkDesign = DesignTokens(
    fill = Color(0xFF151A23),
    fillStrong = Color(0xFF1D2431),
    fillSubtle = Color(0xFF10141B),
    border = Color(0xFF2A3240),
    borderStrong = Color(0xFF3C4759),
    ambientTint = Color(0xFF000000),
    readingSurface = Color(0xFF0C1017),
    isDark = true,
)

internal val LightDesign = DesignTokens(
    fill = Color(0xFFFFFFFF),
    fillStrong = Color(0xFFFFFFFF),
    fillSubtle = Color(0xFFF1F4F9),
    border = Color(0xFFE1E6EF),
    borderStrong = Color(0xFFC6CFDD),
    ambientTint = Color(0xFF2A3446),
    readingSurface = Color(0xFF10141B),
    isDark = false,
)

internal val LocalDesignTokens = staticCompositionLocalOf { DarkDesign }

/** Design tokens for the current theme. */
val pocketTokens: DesignTokens
    @Composable @ReadOnlyComposable get() = LocalDesignTokens.current

/** Corner radii used across the app, from chips up to sheets. */
object PocketRadii {
    val xs = RoundedCornerShape(10.dp)
    val sm = RoundedCornerShape(14.dp)
    val md = RoundedCornerShape(18.dp)
    val lg = RoundedCornerShape(24.dp)
    val xl = RoundedCornerShape(32.dp)
    val pill = RoundedCornerShape(percent = 50)

    fun forElevation(level: Int): Shape = when {
        level <= 0 -> sm
        level == 1 -> md
        level == 2 -> lg
        else -> xl
    }
}

/**
 * The standard pane treatment in one modifier: opaque fill, hairline border.
 *
 * Fills are applied before the border so the rim always sits on top. Use this
 * instead of hand-layering `.background()`/`.border()` calls, which is how panes
 * drifted out of sync before.
 */
fun Modifier.pocketPane(
    tokens: DesignTokens,
    shape: Shape,
    base: Color = tokens.fill,
    borderWidth: Dp = 1.dp,
    borderColor: Color = tokens.border,
): Modifier = this
    .clip(shape)
    .background(base)
    .then(
        if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier,
    )

/** Convenience: the pane fill colour, optionally the raised variant. */
@Composable
fun pocketFill(strong: Boolean = false): Color =
    if (strong) pocketTokens.fillStrong else pocketTokens.fill

/**
 * Fill for text-dense panes (code editor, terminal). See [DesignTokens.readingSurface].
 */
@Composable
fun pocketReadingFill(): Color = pocketTokens.readingSurface
