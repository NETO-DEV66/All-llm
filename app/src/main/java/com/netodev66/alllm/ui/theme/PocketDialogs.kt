package com.netodev66.alllm.ui.theme

import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Glass alert dialog. Keeps Material3's [AlertDialog] parameter list so call sites
 * only need the name changed, but renders the container as a translucent glass pane
 * with a hairline rim.
 *
 * The rim is drawn through [modifier] because [AlertDialog] exposes no border
 * parameter. The dialog window is a separate window from the app's root, so the
 * app-level [GlassBackdrop] is not behind it; the translucent fill composites over
 * the dialog's own scrim instead, which is why the default fill is the strong one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = GlassShapes.lg,
    containerColor: androidx.compose.ui.graphics.Color = glassTokens.fillStrong,
    iconContentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondary,
    titleContentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation: androidx.compose.ui.unit.Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.border(1.dp, glassTokens.borderStrong, shape),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties,
    )
}
