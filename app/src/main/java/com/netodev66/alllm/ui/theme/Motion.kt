package com.netodev66.alllm.ui.theme

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
    val smooth: SpringSpec,
    val screenEnterMillis: Int,
    val screenExitMillis: Int,
) {
    /** Press-scale spring used by [com.netodev66.alllm.ui.theme.bounceClick]. */
    val press: SpringSpec get() = snappy

    /** Reveal spring used by expand/collapse and staggered list entrances. */
    val reveal: SpringSpec get() = bouncy

    /**
     * Slide spring for surfaces that travel a long distance (the nav drawer).
     * Critically damped on purpose: a full-height panel that overshoots reads as a
     * glitch rather than as bounce, so distance is what separates this from [snappy].
     */
    val drawer: SpringSpec get() = smooth
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
    // Damping is kept just under 1.0: the overshoot still reads as a bounce, but a
    // single small rebound instead of the multi-wobble that a low ratio produces.
    // Lower ratios look lively in a screenshot and feel seasick in a scrolling list.
    bouncy = SpringSpec(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
    bouncyStrong = SpringSpec(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow),
    settle = SpringSpec(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium),
    snappy = SpringSpec(dampingRatio = 0.72f, stiffness = Spring.StiffnessHigh),
    gentle = SpringSpec(dampingRatio = 1f, stiffness = Spring.StiffnessLow),
    smooth = SpringSpec(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow),
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
