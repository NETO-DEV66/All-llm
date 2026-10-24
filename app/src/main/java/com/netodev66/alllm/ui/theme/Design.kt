package com.netodev66.alllm.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid-glass design tokens.
 *
 * Panes are translucent veils that composite over [PocketBackdrop]'s drifting
 * gradient. The extra edge tokens ([edgeLight], [edgeShadow], [innerGlow],
 * [specular]) are what separate a convincing pane of glass from a flat translucent
 * rectangle: real glass catches a bright sliver of light on the edge nearest the
 * light source, darkens the opposite edge where the thickness occludes the
 * backdrop, and lifts a faint inner glow just inside the rim.
 *
 * Two fields are deliberately opaque. [readingSurface] backs the code editor and
 * terminal, where 13sp monospace over a moving gradient is unreadable; [scrim]
 * dims content behind a dialog, so it must not let that content show through.
 */
@Immutable
data class DesignTokens(
    /** Default pane fill. Translucent so the backdrop reads through. */
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
    /** Bright 1px sliver on the top/left edge, where light enters the pane. */
    val edgeLight: Color,
    /** Faint darkening on the bottom/right edge, where the pane's thickness reads. */
    val edgeShadow: Color,
    /** Soft glow just inside the rim that keeps the glass from looking like a sticker. */
    val innerGlow: Color,
    /** Vertical specular gradient laid over [fill]: bright top edge fading to nothing. */
    val specular: Color,
    /** Faint secondary sheen near the bottom edge of a pane. */
    val sheen: Color,
    /** Gradient stops painted behind the whole app. */
    val backdrop: List<Color>,
    /** Accent glows drifting over [backdrop]. */
    val backdropGlow: List<Color>,
    /**
     * Near-opaque fill for a modal container (dialog, bottom sheet). A modal lives in
     * its own window over the dimmed app, so its container must carry its own body —
     * a thin veil would let the content behind compete with the modal's text.
     */
    val modalSurface: Color,
    /** Dimmer behind modal content; opaque enough to hide the backdrop. */
    val scrim: Color,
    /** Blur radius for decorative layers only, never for a pane that holds text. */
    val panelBlurRadius: Dp,
    val isDark: Boolean,
)

internal val DarkDesign = DesignTokens(
    fill = Color(0x1FFFFFFF),
    fillStrong = Color(0x33FFFFFF),
    fillSubtle = Color(0x0FFFFFFF),
    border = Color(0x2BFFFFFF),
    borderStrong = Color(0x47FFFFFF),
    ambientTint = Color(0xFF04060A),
    readingSurface = Color(0xF20A0E15),
    edgeLight = Color(0x59FFFFFF),
    edgeShadow = Color(0x66000000),
    innerGlow = Color(0x14FFFFFF),
    specular = Color(0x2EFFFFFF),
    sheen = Color(0x14FFFFFF),
    backdrop = listOf(
        Color(0xFF0A0D13),
        Color(0xFF0D1420),
        Color(0xFF141019),
        Color(0xFF070A0F),
    ),
    backdropGlow = listOf(
        Color(0x4DF28C52),
        Color(0x338EA8FF),
        Color(0x2E69D69E),
    ),
    modalSurface = Color(0xF0151A23),
    scrim = Color(0xC7080B11),
    panelBlurRadius = 28.dp,
    isDark = true,
)

internal val LightDesign = DesignTokens(
    fill = Color(0x9EFFFFFF),
    fillStrong = Color(0xD9FFFFFF),
    fillSubtle = Color(0x66FFFFFF),
    border = Color(0x59FFFFFF),
    borderStrong = Color(0x8CFFFFFF),
    ambientTint = Color(0xFF2A3446),
    readingSurface = Color(0xF5FBFCFE),
    edgeLight = Color(0xE6FFFFFF),
    edgeShadow = Color(0x1F0B1226),
    innerGlow = Color(0x1AFFFFFF),
    specular = Color(0x99FFFFFF),
    sheen = Color(0x40FFFFFF),
    backdrop = listOf(
        Color(0xFFF7F9FC),
        Color(0xFFEDF2FA),
        Color(0xFFFBF6F1),
        Color(0xFFF2F5F9),
    ),
    backdropGlow = listOf(
        Color(0x33F28C52),
        Color(0x268EA8FF),
        Color(0x1F69D69E),
    ),
    modalSurface = Color(0xF7FFFFFF),
    scrim = Color(0x52101820),
    panelBlurRadius = 20.dp,
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
    val md = RoundedCornerShape(20.dp)
    val lg = RoundedCornerShape(28.dp)
    val xl = RoundedCornerShape(36.dp)
    val pill = RoundedCornerShape(percent = 50)

    fun forElevation(level: Int): Shape = when {
        level <= 0 -> sm
        level == 1 -> md
        level == 2 -> lg
        else -> xl
    }
}

/**
 * Vertical specular gradient laid over a glass fill: a bright top edge that fades to
 * near-transparent, which is what makes a flat translucent colour read as a pane.
 */
fun glassSpecularBrush(tokens: DesignTokens, strength: Float = 1f): Brush = Brush.verticalGradient(
    colors = listOf(
        tokens.specular.copy(alpha = tokens.specular.alpha * strength),
        Color.Transparent,
        Color.Transparent,
        tokens.sheen.copy(alpha = tokens.sheen.alpha * strength * 0.5f),
    ),
)

/** Ambient backdrop gradient, behind the whole app so translucent panes have depth. */
fun glassBackdropBrush(tokens: DesignTokens): Brush = Brush.linearGradient(colors = tokens.backdrop)

fun glassGlowBrush(tokens: DesignTokens, radius: Float = 1f): Brush = Brush.radialGradient(
    colors = tokens.backdropGlow.map { it.copy(alpha = it.alpha * radius) } + Color.Transparent,
)

/**
 * A narrow diagonal band of light, drawn full-bleed and then slid across the screen by
 * [PocketBackdrop]. One gradient, no blur, but it is what makes the backdrop read as a
 * moving sheet of glass rather than a slow colour wash.
 */
fun glassSweepBrush(tokens: DesignTokens): Brush = Brush.linearGradient(
    colors = listOf(
        Color.Transparent,
        Color.Transparent,
        tokens.specular.copy(alpha = tokens.specular.alpha * 0.7f),
        Color.Transparent,
        Color.Transparent,
    ),
)

/**
 * Applies a real GPU blur to this layer. Only meaningful on API 31+, where
 * `RenderEffect` exists; below that it is a no-op and the translucent fill plus
 * specular gradient still read as glass.
 *
 * Intended for decorative layers (backdrops, glows), never for a pane that holds
 * text — blurring a pane blurs its children with it.
 */
fun Modifier.glassBlur(radius: Dp, shape: Shape): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || radius.value <= 0f) {
        return@composed Modifier
    }
    this
        .clip(shape)
        .graphicsLayer {
            renderEffect = RenderEffect
                .createBlurEffect(radius.toPx(), radius.toPx(), Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
}

/**
 * Edge-light gradient stroked inside the rim: bright where the light enters at the top,
 * a faint shadow where the pane's thickness occludes the backdrop at the bottom.
 */
fun glassEdgeLightBrush(tokens: DesignTokens): Brush = Brush.verticalGradient(
    colors = listOf(
        tokens.edgeLight,
        Color.Transparent,
        Color.Transparent,
        tokens.edgeShadow,
    ),
)

/** Soft glow confined to the top sliver of a pane, just inside the rim. */
fun glassInnerGlowBrush(tokens: DesignTokens): Brush = Brush.verticalGradient(
    0f to tokens.innerGlow,
    0.14f to Color.Transparent,
)

/**
 * The complete liquid-glass pane treatment in one modifier: translucent fill, vertical
 * specular sheen, an optional top inner glow, the hairline rim, and the edge-light stroke.
 *
 * Fills are applied before strokes so the rim always sits on top of the glass. Use this
 * instead of hand-layering `.background()`/`.border()` calls, which is how panes drifted
 * out of sync before.
 *
 * [innerGlow] is skipped for panes that repeat inside a scrolling list: the extra
 * full-size gradient pass is not worth it at that count, and the edge-light stroke alone
 * already carries the glass read.
 */
fun Modifier.glassPane(
    tokens: DesignTokens,
    shape: Shape,
    base: Color = tokens.fill,
    borderWidth: Dp = 1.dp,
    borderColor: Color = tokens.border,
    specularStrength: Float = 1f,
    innerGlow: Boolean = true,
    edgeLight: Boolean = true,
): Modifier {
    var modifier = this
        .clip(shape)
        .background(base)
        .background(glassSpecularBrush(tokens, specularStrength))
    if (innerGlow) modifier = modifier.background(glassInnerGlowBrush(tokens))
    if (borderWidth > 0.dp) modifier = modifier.border(borderWidth, borderColor, shape)
    if (edgeLight) modifier = modifier.border(1.dp, glassEdgeLightBrush(tokens), shape)
    return modifier
}

/**
 * The pane treatment under the name the components in this package use. Kept as a
 * thin alias so a call site can pick whichever reads better at that spot.
 */
fun Modifier.pocketPane(
    tokens: DesignTokens,
    shape: Shape,
    base: Color = tokens.fill,
    borderWidth: Dp = 1.dp,
    borderColor: Color = tokens.border,
    innerGlow: Boolean = true,
): Modifier = glassPane(
    tokens,
    shape,
    base = base,
    borderWidth = borderWidth,
    borderColor = borderColor,
    innerGlow = innerGlow,
)

/** Convenience: the pane fill colour, optionally the raised variant. */
@Composable
fun pocketFill(strong: Boolean = false): Color =
    if (strong) pocketTokens.fillStrong else pocketTokens.fill

/** Glass tint derived from a semantic colour (e.g. primary) so accents stay legible. */
@Composable
fun pocketTint(accent: Color = MaterialTheme.colorScheme.primary, alpha: Float = 0.18f): Color =
    accent.copy(alpha = alpha)

/**
 * Fill for text-dense panes (code editor, terminal). See [DesignTokens.readingSurface].
 */
@Composable
fun pocketReadingFill(): Color = pocketTokens.readingSurface
