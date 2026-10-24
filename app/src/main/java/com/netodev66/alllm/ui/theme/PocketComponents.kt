package com.netodev66.alllm.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Root background layer.
 *
 * A plain opaque fill of the theme background. Solid surfaces need something flat
 * to sit on, and a drifting gradient behind them only made the borders look
 * unsteady.
 */
@Composable
fun PocketBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

/** Static grouping panel with an optional title row. */
@Composable
fun PocketPanel(
    title: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = PocketRadii.md,
    strong: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = pocketTokens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (strong) 8.dp else 2.dp,
                shape = shape,
                clip = false,
                ambientColor = tokens.ambientTint.copy(alpha = if (tokens.isDark) 0.5f else 0.12f),
            )
            .pocketPane(
                tokens,
                shape,
                base = if (strong) tokens.fillStrong else tokens.fill,
                borderColor = if (strong) tokens.borderStrong else tokens.border,
            )
            .padding(contentPadding),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
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
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
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
    val tokens = pocketTokens
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
                    ambientColor = tokens.ambientTint.copy(alpha = if (tokens.isDark) 0.55f else 0.16f),
                )
            } else {
                Modifier
            },
        )
}

/**
 * Bouncy press plus click handling in one modifier, for layers that own no click
 * handler of their own. Components that already handle clicks must use
 * [bounceScale] and share their interaction source instead, or the action fires twice.
 */
@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.955f,
    interactionSource: MutableInteractionSource? = null,
    pressedShadowBoost: Dp = 0.dp,
    shadowShape: Shape = RectangleShape,
    onClick: () -> Unit,
): Modifier {
    if (!enabled) return this
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    return this
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
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * Reveal animation for content that appears in place. Scales up through the app's
 * bounce spring so an appearing panel has the same physical pop as a tap.
 */
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

/**
 * App bar with a solid container and a bottom hairline. Mirrors Material3's
 * [TopAppBar] signature so call sites swap without touching their arguments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    Column(modifier) {
        TopAppBar(
            title = title,
            modifier = Modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = expandedHeight,
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(pocketTokens.border)
                .then(Modifier.padding(top = 1.dp)),
        )
    }
}

/**
 * Bottom bar that keeps Material3's [NavigationBar] layout and item semantics but
 * renders an opaque container with a top hairline.
 */
@Composable
fun PocketBottomBar(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(pocketTokens.border)
                .then(Modifier.padding(top = 1.dp)),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = containerColor ?: MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                content = content,
            )
        }
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

/**
 * Drop-in replacement for Material3's clickable `Surface`, with a solid fill and a
 * bouncy press. Parameter-compatible with the overload it replaces so call sites can
 * be swapped without other edits.
 */
@Composable
fun PocketClickableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = PocketRadii.md,
    color: Color = pocketTokens.fill,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = BorderStroke(1.dp, pocketTokens.border),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val tokens = pocketTokens
    Box(
        modifier = modifier
            .bounceClick(
                enabled = enabled,
                pressedShadowBoost = 8.dp,
                shadowShape = shape,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .pocketPane(
                tokens,
                shape,
                base = color,
                borderWidth = if (border != null) 1.dp else 0.dp,
                borderColor = border?.brush?.let { tokens.border } ?: tokens.border,
            ),
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            content = content,
        )
    }
}

/**
 * Drop-in replacement for Material3's clickable `Card`, with a solid container and a
 * bouncy press.
 */
@Composable
fun PocketClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = PocketRadii.md,
    colors: androidx.compose.material3.CardColors = pocketCardColors(),
    elevation: androidx.compose.material3.CardElevation = pocketCardElevation(),
    border: BorderStroke? = BorderStroke(1.dp, pocketTokens.border),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = pocketTokens
    Column(
        modifier = modifier
            .bounceClick(
                enabled = enabled,
                pressedShadowBoost = 8.dp,
                shadowShape = shape,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .pocketPane(
                tokens,
                shape,
                base = colors.containerColor,
                borderWidth = if (border != null) 1.dp else 0.dp,
            ),
        content = content,
    )
}

/** Card container colours for the solid language. */
@Composable
fun pocketCardColors(): androidx.compose.material3.CardColors =
    androidx.compose.material3.CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

/** Card elevation: flat, the border and the press lift do the work. */
@Composable
fun pocketCardElevation(): androidx.compose.material3.CardElevation =
    androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)

/**
 * Hamburger button that opens [PocketNavDrawer]. Lives in the top bar of every root
 * destination, which is what replaced the always-visible navigation rail.
 */
@Composable
fun PocketHamburgerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Open navigation menu",
) {
    PocketIconButton(onClick, modifier = modifier) {
        Icon(Icons.Default.Menu, contentDescription = contentDescription)
    }
}

/**
 * Navigation drawer opened from [PocketHamburgerButton].
 *
 * Every root destination lives here instead of in a permanent rail, so the content
 * area gets the full width. The panel slides in from the start edge on the
 * [MotionTokens.drawer] spring; that spring is critically damped because a panel this
 * tall which overshoots reads as a glitch rather than as bounce.
 *
 * Renders nothing while [open] is false, and the scrim is the only part that accepts
 * taps, so the content underneath stays interactive whenever the drawer is closed.
 */
@Composable
fun PocketNavDrawer(
    open: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    drawerWidth: Dp = 300.dp,
    header: @Composable ColumnScope.() -> Unit = {},
    items: @Composable ColumnScope.() -> Unit,
) {
    val motionTokens = motion
    val slideSpec = motionTokens.drawer.asSpec<IntOffset>()
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(animationSpec = motionTokens.enterFade()),
            exit = fadeOut(animationSpec = motionTokens.exitFade()),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )
        }
        AnimatedVisibility(
            visible = open,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = slideSpec),
            exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = slideSpec),
        ) {
            Surface(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight(),
                // A drawer is a modal surface, so it uses the raised fill rather than the
                // base pane fill: nav labels must stay readable over what the scrim dims.
                color = pocketTokens.fillStrong,
                tonalElevation = 0.dp,
                shadowElevation = 24.dp,
            ) {
                Column(
                    Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 14.dp),
                ) {
                    header()
                    Spacer(Modifier.height(10.dp))
                    items()
                }
            }
        }
    }
}

/**
 * One destination row inside [PocketNavDrawer]. Selected rows get a primary-tinted
 * pill and the icon pops via [BouncyNavIcon], matching the tap feedback elsewhere.
 */
@Composable
fun PocketNavDrawerItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(PocketRadii.sm)
            .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent)
            .bounceClick(
                interactionSource = interaction,
                pressedScale = 0.97f,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BouncyNavIcon(
            selected = selected,
            icon = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** Section label between groups of drawer rows. */
@Composable
fun PocketNavDrawerSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(start = 26.dp, top = 16.dp, bottom = 6.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
