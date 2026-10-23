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
 * Everything here resolves per theme mode through [PocketTheme]. Compose Material3
 * only exposes opaque surface roles, so translucent "glass" surfaces need their own
 * token set: the fill, the specular highlight gradient, the hairline rim, and the
 * ambient shadow that makes the pane read as a raised sheet of glass.
 *
 * The extra edge tokens ([edgeLight], [edgeShadow], [innerGlow], [ambientTint])
 * are what separate a convincing pane from a flat translucent rectangle: real glass
 * catches a bright sliver of light on the edge nearest the light source, darkens the
 * opposite edge where the thickness occludes the backdrop, and lifts a faint inner
 * glow just inside the rim.
 */
@Immutable
data class GlassTokens(
    val backdrop: List<Color>,
    val backdropGlow: List<Color>,
    val fill: Color,
    val fillStrong: Color,
    val fillSubtle: Color,
    val border: Color,
    val borderStrong: Color,
    val specular: Color,
    val sheen: Color,
    val scrim: Color,
    /** Bright 1px sliver on the top/left edge, where light enters the pane. */
    val edgeLight: Color,
    /** Faint darkening on the bottom/right edge, where the pane's thickness reads. */
    val edgeShadow: Color,
    /** Soft glow just inside the rim that keeps the glass from looking like a sticker. */
    val innerGlow: Color,
    /** Tint of the ambient drop shadow; a neutral black shadow reads as a flat hole. */
    val ambientTint: Color,
    /**
     * Near-opaque fill for panes that hold dense text (code editor, terminal).
     * The glass language is deliberately translucent, but code needs a stable
     * backdrop: the animated gradient moving behind 13sp monospace is unreadable.
     * Kept dark in both themes because the syntax palette is tuned for dark.
     */
    val readingSurface: Color,
    val isDark: Boolean,
    val backdropBlurRadius: Dp,
    val panelBlurRadius: Dp,
)

internal val DarkGlass = GlassTokens(
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
    fill = Color(0x14FFFFFF),
    fillStrong = Color(0x24FFFFFF),
    fillSubtle = Color(0x0AFFFFFF),
    border = Color(0x1FFFFFFF),
    borderStrong = Color(0x33FFFFFF),
    specular = Color(0x2EFFFFFF),
    sheen = Color(0x14FFFFFF),
    scrim = Color(0xB3080B11),
    edgeLight = Color(0x59FFFFFF),
    edgeShadow = Color(0x66000000),
    innerGlow = Color(0x14FFFFFF),
    ambientTint = Color(0xFF06080C),
    readingSurface = Color(0xF20A0E15),
    isDark = true,
    backdropBlurRadius = 0.dp,
    panelBlurRadius = 28.dp,
)

internal val LightGlass = GlassTokens(
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
    fill = Color(0x8CFFFFFF),
    fillStrong = Color(0xCCFFFFFF),
    fillSubtle = Color(0x5CFFFFFF),
    border = Color(0x33FFFFFF),
    borderStrong = Color(0x66FFFFFF),
    specular = Color(0x99FFFFFF),
    sheen = Color(0x40FFFFFF),
    scrim = Color(0x52101820),
    edgeLight = Color(0xE6FFFFFF),
    edgeShadow = Color(0x1F0B1226),
    innerGlow = Color(0x1AFFFFFF),
    ambientTint = Color(0xFF2A3446),
    readingSurface = Color(0xF5FBFCFE),
    isDark = false,
    backdropBlurRadius = 0.dp,
    panelBlurRadius = 20.dp,
)

internal val LocalGlassTokens = staticCompositionLocalOf { DarkGlass }

/** Liquid-glass tokens for the current theme. */
val glassTokens: GlassTokens
    @Composable @ReadOnlyComposable get() = LocalGlassTokens.current

/** Corner radii used by the glass language, from chips up to sheets. */
object GlassShapes {
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
fun glassSpecularBrush(tokens: GlassTokens, strength: Float = 1f): Brush = Brush.verticalGradient(
    colors = listOf(
        tokens.specular.copy(alpha = tokens.specular.alpha * strength),
        Color.Transparent,
        Color.Transparent,
        tokens.sheen.copy(alpha = tokens.sheen.alpha * strength * 0.5f),
    ),
)

/**
 * Ambient backdrop gradient. Used behind the whole app so the translucent panels
 * always have something with depth to sit on.
 */
fun glassBackdropBrush(tokens: GlassTokens): Brush = Brush.linearGradient(
    colors = tokens.backdrop,
)

fun glassGlowBrush(tokens: GlassTokens, radius: Float = 1f): Brush = Brush.radialGradient(
    colors = tokens.backdropGlow.map { it.copy(alpha = it.alpha * radius) } + Color.Transparent,
)

/**
 * A narrow diagonal band of light, drawn full-bleed and then slid across the screen
 * by [GlassBackdrop]. Cheap to draw (one gradient, no blur) but it is what makes the
 * backdrop read as a moving sheet of glass rather than a slow colour wash.
 */
fun glassSweepBrush(tokens: GlassTokens): Brush = Brush.linearGradient(
    colors = listOf(
        Color.Transparent,
        Color.Transparent,
        tokens.specular.copy(alpha = tokens.specular.alpha * 0.7f),
        Color.Transparent,
        Color.Transparent,
    ),
    // Default start/end (top-left to bottom-right) make the band diagonal.
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

/** Horizontal hairline used as the top rim of a glass pane. */
fun glassRimBrush(tokens: GlassTokens): Brush = Brush.horizontalGradient(
    listOf(
        Color.Transparent,
        tokens.borderStrong,
        tokens.borderStrong,
        Color.Transparent,
    ),
)

/**
 * Edge-light gradient stroked inside the rim: bright where the light enters at the top,
 * a faint shadow where the pane's thickness occludes the backdrop at the bottom.
 */
fun glassEdgeLightBrush(tokens: GlassTokens): Brush = Brush.verticalGradient(
    colors = listOf(
        tokens.edgeLight,
        Color.Transparent,
        Color.Transparent,
        tokens.edgeShadow,
    ),
)

/** Soft glow confined to the top sliver of a pane, just inside the rim. */
fun glassInnerGlowBrush(tokens: GlassTokens): Brush = Brush.verticalGradient(
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
    tokens: GlassTokens,
    shape: Shape,
    base: Color = tokens.fill,
    borderWidth: Dp = 1.dp,
    specularStrength: Float = 1f,
    innerGlow: Boolean = true,
    edgeLight: Boolean = true,
): Modifier {
    var modifier = this
        .clip(shape)
        .background(base)
        .background(glassSpecularBrush(tokens, specularStrength))
    if (innerGlow) modifier = modifier.background(glassInnerGlowBrush(tokens))
    modifier = modifier.border(borderWidth, tokens.border, shape)
    if (edgeLight) modifier = modifier.border(1.dp, glassEdgeLightBrush(tokens), shape)
    return modifier
}

/** Convenience: the glass fill colour, optionally strengthened for focused surfaces. */
@Composable
fun glassFill(strong: Boolean = false): Color =
    if (strong) glassTokens.fillStrong else glassTokens.fill

/** Glass tint derived from a semantic colour (e.g. primary) so accents stay legible. */
@Composable
fun glassTint(accent: Color = MaterialTheme.colorScheme.primary, alpha: Float = 0.18f): Color =
    accent.copy(alpha = alpha)

/**
 * Fill for text-dense panes (code editor, terminal). Near-opaque on purpose: see
 * [GlassTokens.readingSurface].
 */
@Composable
fun glassReadingFill(): Color = glassTokens.readingSurface
