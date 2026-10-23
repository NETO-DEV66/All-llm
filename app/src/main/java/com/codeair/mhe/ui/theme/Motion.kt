package com.codeair.mhe.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntOffset

/**
 * Motion tokens for the app's bouncy, physical feel.
 *
 * Every interactive surface uses one of these springs so that taps, reveals and
 * screen changes share a single rhythm. [bouncy] overshoots slightly (the "bounce"),
 * [settle] is critically damped for content that must not wobble, and [snappy] is
 * for small affordances such as icon rotation and ripple.
 */
@Immutable
data class MotionTokens(
    val bouncy: SpringSpec,
    val bouncyStrong: SpringSpec,
    val settle: SpringSpec,
    val snappy: SpringSpec,
    val gentle: SpringSpec,
    val screenEnterMillis: Int,
    val screenExitMillis: Int,
) {
    /** Press-scale spring used by [com.codeair.mhe.ui.theme.bounceClick]. */
    val press: SpringSpec get() = snappy

    /** Reveal spring used by expand/collapse and staggered list entrances. */
    val reveal: SpringSpec get() = bouncy
}

/** A spring described in the form Compose's `animate*AsState` overloads accept. */
@Immutable
data class SpringSpec(
    val dampingRatio: Float,
    val stiffness: Float,
) {
    fun <T> asSpec(): FiniteAnimationSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

    /** `IntOffset` variant, needed by slide-in/out transitions. */
    fun asOffsetSpec(): FiniteAnimationSpec<IntOffset> = spring(dampingRatio = dampingRatio, stiffness = stiffness)
}

internal val DefaultMotion = MotionTokens(
    bouncy = SpringSpec(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
    bouncyStrong = SpringSpec(dampingRatio = 0.34f, stiffness = Spring.StiffnessLow),
    settle = SpringSpec(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
    snappy = SpringSpec(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh),
    gentle = SpringSpec(dampingRatio = 1f, stiffness = Spring.StiffnessLow),
    screenEnterMillis = 320,
    screenExitMillis = 220,
)

internal val LocalMotionTokens = staticCompositionLocalOf { DefaultMotion }

/** Motion tokens for the current composition. */
val motion: MotionTokens
    @Composable @ReadOnlyComposable get() = LocalMotionTokens.current

/** Fade spec shared by screen transitions. */
fun MotionTokens.enterFade(): FiniteAnimationSpec<Float> = tween(durationMillis = screenEnterMillis)

fun MotionTokens.exitFade(): FiniteAnimationSpec<Float> = tween(durationMillis = screenExitMillis)
