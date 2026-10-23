package com.netodev66.alllm.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay





/**
 * Full-bleed animated backdrop for the liquid-glass language.
 *
 * Draws a slow-drifting multi-stop gradient, two soft accent glows and a specular
 * sweep. Every translucent pane in the app composites over this, so this layer is
 * what makes the glass read as glass rather than as flat grey.
 *
 * Performance: the layer is built so that a running animation never recomposes the
 * content above it. All four animated values are read only inside `graphicsLayer`
 * blocks, which Compose evaluates in the layer-update phase — a frame invalidates
 * these layers, not the composition. The brushes themselves are `remember`ed per
 * theme, so no per-frame allocation happens either. [blurred] costs one full-screen
 * GPU blur pass and is therefore applied to the glow layer only, never to content.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    blurred: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = glassTokens
    val transition = rememberInfiniteTransition(label = "glass-backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (animated) 22_000 else 1,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (animated) 9_000 else 1,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    val backdropBrush = remember(tokens) { glassBackdropBrush(tokens) }
    val primaryGlow = remember(tokens) { glassGlowBrush(tokens, 0.9f) }
    val secondaryGlow = remember(tokens) { glassGlowBrush(tokens, 0.7f) }
    val sweepBrush = remember(tokens) { glassSweepBrush(tokens) }
    val glowModifier = remember(tokens, blurred) {
        if (blurred) Modifier.glassBlur(tokens.panelBlurRadius, RectangleShape) else Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backdropBrush),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(glowModifier)
                .background(primaryGlow)
                .graphicsLayer {
                    alpha = 0.55f + 0.35f * drift
                    translationX = size.width * (drift - 0.5f) * 0.15f
                },
        )
        // Deliberately not blurred: a radial gradient is already smooth, so a second
        // full-screen blur pass would cost GPU time and change nothing visible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(secondaryGlow)
                .graphicsLayer {
                    alpha = 0.30f + 0.30f * (1f - drift)
                    translationX = size.width * (drift - 0.5f) * 0.4f
                    translationY = size.height * (0.5f - drift) * 0.3f
                },
        )
        // A single diagonal band of light that crosses the screen and repeats. This
        // is the detail that separates "translucent panel" from "sheet of glass".
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 0.5f
                    translationX = size.width * (1.4f * sweep - 1.2f)
                    translationY = size.height * (1.4f * sweep - 1.2f) * 0.35f
                }
                .background(sweepBrush),
        )
        content()
    }
}

/**
 * A raised sheet of glass: translucent fill, specular top highlight, hairline rim
 * and a soft ambient shadow. The workhorse container of the design language.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.md,
    strong: Boolean = false,
    tint: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = glassTokens
    val base = tint?.let { it.copy(alpha = if (strong) 0.26f else 0.16f) }
        ?: if (strong) tokens.fillStrong else tokens.fill

    Column(
        modifier = modifier
            .shadow(elevation, shape, clip = false, ambientColor = tokens.ambientTint.copy(alpha = 0.45f))
            .glassPane(tokens, shape, base = base, borderWidth = borderWidth)
            .padding(contentPadding),
        content = content,
    )
}

/** Non-column glass surface: a bare pane you can place anything inside. */
@Composable
fun GlassPane(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.md,
    strong: Boolean = false,
    tint: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = glassTokens
    val base = tint?.let { it.copy(alpha = if (strong) 0.26f else 0.16f) }
        ?: if (strong) tokens.fillStrong else tokens.fill

    Box(
        modifier = modifier
            .shadow(elevation, shape, clip = false, ambientColor = tokens.ambientTint.copy(alpha = 0.45f))
            .glassPane(tokens, shape, base = base, borderWidth = borderWidth),
        content = content,
    )
}

/**
 * Clickable glass tile with a bouncy press. Drop-in replacement for a bare
 * `Card(onClick = ...)`.
 */
@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.md,
    strong: Boolean = false,
    tint: Color? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = glassTokens
    val base = tint?.let { it.copy(alpha = if (strong) 0.26f else 0.16f) }
        ?: if (strong) tokens.fillStrong else tokens.fill

    Column(
        modifier = modifier
            .bounceClick(
                enabled = enabled,
                pressedShadowBoost = 10.dp,
                shadowShape = shape,
                onClick = onClick,
            )
            .shadow(12.dp, shape, clip = false, ambientColor = tokens.ambientTint.copy(alpha = 0.45f))
            .glassPane(tokens, shape, base = base, innerGlow = false)
            .padding(contentPadding),
        content = content,
    )
}

/** Static glass grouping panel with an optional title row. */
@Composable
fun GlassPanel(
    title: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.md,
    strong: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        strong = strong,
        contentPadding = contentPadding,
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        content()
    }
}

/**
 * Adds only the bouncy press-scale to a layer, without consuming clicks. Use this
 * when the component already handles its own clicks (e.g. Material3 buttons) and
 * needs the press feedback but not a second click handler.
 */
@Composable
fun Modifier.bounceScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.955f,
    /** Extra shadow spread added while pressed; 0 keeps the layer purely a scale. */
    pressedShadowBoost: Dp = 0.dp,
    shadowShape: Shape = RectangleShape,
): Modifier {
    if (!enabled) return this
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) {
            spring(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
        } else {
            motion.bouncy.asSpec()
        },
        label = "bounce-scale",
    )
    val shadow by animateFloatAsState(
        targetValue = if (pressed) 0f else pressedShadowBoost.value,
        animationSpec = if (pressed) {
            spring(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
        } else {
            motion.bouncy.asSpec()
        },
        label = "bounce-shadow",
    )
    val tokens = glassTokens
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (pressedShadowBoost > 0.dp) {
                Modifier.shadow(
                    elevation = shadow.dp,
                    shape = shadowShape,
                    clip = false,
                    ambientColor = tokens.ambientTint.copy(alpha = 0.5f),
                )
            } else {
                Modifier
            },
        )
}

/**
 * Press feedback plus the click handler. The scale springs back past 1.0 on
 * release, which is the app's signature "bounce".
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.955f,
    interactionSource: MutableInteractionSource? = null,
    pressedShadowBoost: Dp = 0.dp,
    shadowShape: Shape = RectangleShape,
    onClick: () -> Unit,
): Modifier = composed {
    if (!enabled) return@composed this
    val interaction = interactionSource ?: remember { MutableInteractionSource() }

    this
        .bounceScale(
            interaction,
            enabled = enabled,
            pressedScale = pressedScale,
            pressedShadowBoost = pressedShadowBoost,
            shadowShape = shadowShape,
        )
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = true,
            onClick = onClick,
        )
}

/** Bouncy mount animation for content that appears on screen. */
@Composable
fun BouncyAppear(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = motion.enterFade()) +
            scaleIn(initialScale = 0.88f, animationSpec = motion.bouncy.asSpec()),
        exit = fadeOut(animationSpec = motion.exitFade()),
        modifier = modifier,
    ) {
        content()
    }
}

/**
 * Reveal animation used by expand/collapse sections. The height opens with the
 * bounce spring and the content fades in, so disclosure feels physical.
 */
@Composable
fun bouncyExpandEnter(expandFrom: Alignment.Vertical = Alignment.Top): EnterTransition =
    expandVertically(
        expandFrom = expandFrom,
        animationSpec = motion.bouncy.asSpec(),
    ) + fadeIn(animationSpec = motion.enterFade())

@Composable
fun bouncyExpandExit(shrinkTowards: Alignment.Vertical = Alignment.Top): ExitTransition =
    shrinkVertically(
        shrinkTowards = shrinkTowards,
        animationSpec = motion.bouncy.asSpec(),
    ) + fadeOut(animationSpec = motion.exitFade())

/**
 * Bounce-in transition for a whole screen.
 *
 * Animates only this container's layer, so switching screens does not compose the
 * outgoing and incoming screens at the same time. That matters here: several
 * screens own expensive resources (WebView, running terminal sessions), and
 * double-composition during a crossfade would duplicate them.
 *
 * Re-runs whenever [key] changes, which is how tab and stage switches get their pop.
 */
@Composable
fun Modifier.bouncyEntry(
    key: Any?,
    fromScale: Float = 0.965f,
    fromAlpha: Float = 0f,
): Modifier {
    val anim = remember { Animatable(1f) }
    val spec = motion.bouncy.asSpec<Float>()
    LaunchedEffect(key) {
        anim.snapTo(fromScale)
        anim.animateTo(1f, spec)
    }
    return this.graphicsLayer {
        val value = anim.value
        scaleX = value
        scaleY = value
        alpha = fromAlpha + (1f - fromAlpha) *
            ((value - fromScale) / (1f - fromScale)).coerceIn(0f, 1f)
    }
}

/** Glass-styled primary action button. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
    onAccent: Color = MaterialTheme.colorScheme.onPrimary,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.bounceScale(interaction, enabled = enabled, pressedScale = 0.94f),
        enabled = enabled,
        interactionSource = interaction,
        shape = GlassShapes.pill,
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = onAccent,
            disabledContainerColor = accent.copy(alpha = 0.32f),
            disabledContentColor = onAccent.copy(alpha = 0.6f),
        ),
        content = content,
    )
}

/** Glass-styled secondary action: translucent fill with a hairline rim. */
@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = glassTokens
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.bounceScale(interaction, enabled = enabled, pressedScale = 0.94f),
        enabled = enabled,
        interactionSource = interaction,
        shape = GlassShapes.pill,
        contentPadding = contentPadding,
        border = BorderStroke(1.dp, tokens.borderStrong),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = tokens.fill,
            contentColor = accent,
        ),
        content = content,
    )
}

@Composable
fun GlassTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = modifier.bounceScale(interaction, enabled = enabled, pressedScale = 0.96f),
        enabled = enabled,
        interactionSource = interaction,
        content = content,
    )
}

/**
 * Glass top app bar: translucent container by default, no elevation shadow, so it
 * floats over the animated backdrop instead of punching an opaque hole in it.
 *
 * Signature mirrors Material3's [TopAppBar] so call sites can be swapped without
 * touching their arguments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = glassTokens.fill,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = expandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}

/** Glass bottom navigation container: rounded top corners, hairline rim. */
@Composable
fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = glassTokens
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor ?: tokens.fillStrong,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, tokens.border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(glassSpecularBrush(tokens, 0.5f))
                .border(
                    1.dp,
                    glassEdgeLightBrush(tokens),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * Bottom bar that keeps Material3's [NavigationBar] layout and item semantics but
 * renders the container as glass, so the animated backdrop shows through.
 */
@Composable
fun GlassBottomBar(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = glassTokens
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor ?: tokens.fillStrong, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.dp, tokens.border, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            content = content,
        )
    }
}

/**
 * Navigation icon with a bouncy pop on selection: the icon scales past 1.0 and
 * settles back, so tapping a tab has the same physical feel as the rest of the app.
 */
@Composable
fun BouncyNavIcon(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = motion.bouncy.asSpec(),
        label = "nav-icon-scale",
    )
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    )
}

/** Glass dialog container used by in-app dialogs and sheets. */
@Composable
fun GlassDialogContainer(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = glassTokens
    Column(
        modifier = modifier
            .shadow(24.dp, shape, clip = false, ambientColor = tokens.ambientTint.copy(alpha = 0.6f))
            .glassPane(tokens, shape, base = tokens.fillStrong)
            .padding(20.dp),
        content = content,
    )
}

/**
 * Drop-in replacement for Material3's clickable `Surface`, with a glass fill and a
 * bouncy press. Parameter-compatible with the overload it replaces so call sites can
 * be swapped without other edits.
 */
@Composable
fun GlassClickableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GlassShapes.md,
    color: Color = glassTokens.fill,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = BorderStroke(1.dp, glassTokens.border),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val tokens = glassTokens
    Box(
        modifier = modifier
            .bounceClick(
                enabled = enabled,
                pressedShadowBoost = 8.dp,
                shadowShape = shape,
                onClick = onClick,
            )
            .glassPane(tokens, shape, base = color, borderWidth = if (border != null) 1.dp else 0.dp, innerGlow = false),
        propagateMinConstraints = true,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
            content = content,
        )
    }
}

/**
 * Drop-in replacement for Material3's clickable `Card`, with a glass container and a
 * bouncy press.
 */
@Composable
fun GlassClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GlassShapes.md,
    colors: androidx.compose.material3.CardColors = glassCardColors(),
    elevation: androidx.compose.material3.CardElevation = glassCardElevation(),
    border: BorderStroke? = BorderStroke(1.dp, glassTokens.border),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = glassTokens
    Column(
        modifier = modifier
            .bounceClick(
                enabled = enabled,
                pressedShadowBoost = 8.dp,
                shadowShape = shape,
                onClick = onClick,
            )
            .glassPane(tokens, shape, base = colors.containerColor, borderWidth = if (border != null) 1.dp else 0.dp, innerGlow = false),
        content = content,
    )
}

/** Glass defaults for Material3 `Card` container colours. */
@Composable
fun glassCardColors(): androidx.compose.material3.CardColors =
    androidx.compose.material3.CardDefaults.cardColors(
        containerColor = glassTokens.fill,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

/** Glass defaults for Material3 `Card` elevation: flat, the rim and shadow do the work. */
@Composable
fun glassCardElevation(): androidx.compose.material3.CardElevation =
    androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
