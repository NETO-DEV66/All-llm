package com.netodev66.alllm.ui.theme

import android.app.Activity
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val PocketOrange = Color(0xFFF28C52)
val PocketBlue = Color(0xFF8EA8FF)
val PocketGreen = Color(0xFF69D69E)
val PocketBackground = Color(0xFF0B0E14)
val PocketSurface = Color(0xFF131821)
val PocketSurfaceVariant = Color(0xFF1B222D)
val PocketOutline = Color(0xFF2A3240)

/**
 * Dark scheme in liquid-glass form.
 *
 * `background` is transparent so [PocketBackdrop]'s drifting gradient shows
 * through, and the surface roles are veils over it. The veils are deliberately
 * heavy — a `surface` that carried body text would otherwise have to be read
 * against a moving gradient. Chrome that holds no text uses `chromeFill` and
 * stays genuinely translucent.
 *
 * Foreground roles (`onSurface`, `onBackground`) stay opaque, so text contrast is
 * unaffected by the translucency.
 */
private val DarkColors = darkColorScheme(
    primary = PocketOrange,
    onPrimary = Color(0xFF241107),
    primaryContainer = Color(0xFF42281D),
    onPrimaryContainer = Color(0xFFFFDDCC),
    secondary = PocketBlue,
    onSecondary = Color(0xFF001F58),
    secondaryContainer = Color(0xFF202B47),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = PocketGreen,
    onTertiary = Color(0xFF00391E),
    tertiaryContainer = Color(0xFF17351F),
    onTertiaryContainer = Color(0xFFC7F2D8),
    background = Color(0x00000000),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xF2131821),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xED1B222D),
    onSurfaceVariant = Color(0xFFA8B0BC),
    surfaceContainerLowest = Color(0xE60D1117),
    surfaceContainerLow = Color(0xED12161E),
    surfaceContainer = Color(0xF2171C25),
    surfaceContainerHigh = Color(0xF71D2431),
    surfaceContainerHighest = Color(0xFA232B39),
    surfaceBright = Color(0xFF2A3240),
    surfaceDim = Color(0xE60A0D13),
    inverseSurface = Color(0xFFE6EDF3),
    inverseOnSurface = Color(0xFF131821),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1FFFFFFF),
    scrim = Color(0x99000000),
)

/**
 * Light scheme in liquid-glass form. Same structure as the dark scheme: transparent
 * background, heavy veils where text lives, opaque foregrounds. The veils are white
 * rather than grey so a pane brightens the backdrop instead of muddying it.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFFD85A20),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE0D2),
    onPrimaryContainer = Color(0xFF451A08),
    secondary = Color(0xFF3366CC),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE4FF),
    onSecondaryContainer = Color(0xFF00174D),
    tertiary = Color(0xFF1B8A5A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3F5E1),
    onTertiaryContainer = Color(0xFF00291A),
    background = Color(0x00000000),
    onBackground = Color(0xFF1F2328),
    surface = Color(0xF7FFFFFF),
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xF2F1F4F9),
    onSurfaceVariant = Color(0xFF4A525C),
    surfaceContainerLowest = Color(0xF7FFFFFF),
    surfaceContainerLow = Color(0xFAFAFBFD),
    surfaceContainer = Color(0xF7F6F8FB),
    surfaceContainerHigh = Color(0xFAF1F4F9),
    surfaceContainerHighest = Color(0xFFEBEFF5),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xF2E8ECF2),
    inverseSurface = Color(0xFF1F2328),
    inverseOnSurface = Color(0xFFF6F8FA),
    outline = Color(0x59101820),
    outlineVariant = Color(0x26101820),
    scrim = Color(0x66101820),
)

/** Corner radii shared by Material3 components (sheets, menus, chips, text fields). */
private val PocketShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

enum class AppThemeMode { SYSTEM, DARK, LIGHT }

@Composable
fun PocketTheme(themeMode: AppThemeMode = AppThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Sync the system status/navigation bar icons with the ACTUAL app theme.
    // The XML theme is Theme.Material.Light, so without this the system draws
    // dark icons even when the app renders dark (invisible on dark background).
    // Light icons in dark theme, dark icons in light theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        typography = PocketTypography,
        shapes = PocketShapes,
    ) {
        CompositionLocalProvider(
            LocalDesignTokens provides if (isDark) DarkDesign else LightDesign,
            LocalMotionTokens provides DefaultMotion,
            // One seam gives every `clickable` in the app the signature bounce.
            // Components that animate themselves opt out via `indication = null`.
            LocalIndication provides BouncyIndication,
            content = content,
        )
    }
}
