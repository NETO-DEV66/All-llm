package com.netodev66.alllm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colours for status, kept separate from the brand palette.
 *
 * Material3 gives an app an `error` role but no `success` one, so every "this
 * worked" surface in this app had grown its own green: four different shades
 * across 23 call sites, several of them dark green (`0xFF2E7D32`) painted on a
 * dark background, which is close to unreadable. Seven more call sites guessed
 * the theme with `background.luminance() < 0.5f` to pick a green, which breaks
 * the moment a caller sits on a surface other than `background`.
 *
 * Two rules make these tokens safe to drop anywhere:
 *  - the plain colour ([success], [warning], [info], [danger]) is legible on the
 *    pane fills and on the app background, so it is the one to use for text and
 *    icons;
 *  - the `*Container` / `on*Container` pair is for a filled badge or chip, where
 *    the container is the fill and the `on` colour is the text.
 *
 * [StatusColorContrastTest] computes the WCAG ratio of every pair below and fails
 * if one drops under 4.5:1, so a future "nicer green" cannot quietly regress
 * legibility.
 */
@Immutable
data class StatusColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
)

internal val DarkStatusColors = StatusColors(
    success = Color(0xFF6FCF97),
    onSuccess = Color(0xFF08210F),
    successContainer = Color(0xFF1E4630),
    onSuccessContainer = Color(0xFFC8F0D8),
    warning = Color(0xFFFFC46B),
    onWarning = Color(0xFF2B1700),
    warningContainer = Color(0xFF4A3208),
    onWarningContainer = Color(0xFFFFDFB3),
    info = Color(0xFF9DBEFF),
    onInfo = Color(0xFF001A3D),
    infoContainer = Color(0xFF203A6B),
    onInfoContainer = Color(0xFFD9E3FF),
    danger = Color(0xFFFF8A80),
    onDanger = Color(0xFF3A0A0A),
    dangerContainer = Color(0xFF8C1D18),
    onDangerContainer = Color(0xFFF9DEDC),
)

internal val LightStatusColors = StatusColors(
    success = Color(0xFF1E6B2E),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD7F2DE),
    onSuccessContainer = Color(0xFF05230F),
    warning = Color(0xFFA85700),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFE8CC),
    onWarningContainer = Color(0xFF331D00),
    info = Color(0xFF0B57D0),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFD7E3FF),
    onInfoContainer = Color(0xFF001945),
    danger = Color(0xFFB3261E),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFF9DEDC),
    onDangerContainer = Color(0xFF410E0B),
)

internal val LocalStatusColors = staticCompositionLocalOf { DarkStatusColors }

/** Semantic status colours for the current theme. */
val statusColors: StatusColors
    @Composable @ReadOnlyComposable get() = LocalStatusColors.current

/**
 * The colour a filled status badge should paint its text in, given its fill.
 *
 * Call sites used to hardcode `Color.White` on a solid status fill, which is
 * correct only while the fill stays dark. Reading the choice from the palette
 * keeps white-on-dark and near-black-on-light both legible.
 *
 * The solid and container fills need different text colours: in the dark theme
 * [StatusColors.success] is a *light* green, so its text must be near-black,
 * whereas [StatusColors.successContainer] is dark and wants light text. Both
 * directions are asserted by [StatusColorContrastTest].
 */
@Composable
fun statusOn(container: Color): Color {
    val tokens = statusColors
    return when (container) {
        tokens.success -> tokens.onSuccess
        tokens.successContainer -> tokens.onSuccessContainer
        tokens.warning -> tokens.onWarning
        tokens.warningContainer -> tokens.onWarningContainer
        tokens.info -> tokens.onInfo
        tokens.infoContainer -> tokens.onInfoContainer
        tokens.danger -> tokens.onDanger
        tokens.dangerContainer -> tokens.onDangerContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
}
