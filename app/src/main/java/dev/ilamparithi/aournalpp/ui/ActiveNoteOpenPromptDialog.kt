package dev.ilamparithi.aournalpp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.runtime.ActiveNotesTracker
import dev.ilamparithi.aournalpp.utils.FormatUtils
import dev.ilamparithi.aournalpp.utils.a11yHeading
import java.io.File

/**
 * Dialog displayed when opening a note that is already open in the active canvas session.
 * Prompts the user with three explicit choices:
 * - "View Existing Window": Brings the open window to focus in the canvas session
 * - "Open in New Window": Opens a new window for the note in the existing session
 * - "Cancel": Closes dialog and aborts opening
 */
@Composable
fun ActiveNoteOpenPromptDialog(
    file: File,
    activeMatch: ActiveNotesTracker.ActiveNoteMatch,
    onDismiss: () -> Unit,
    onViewExistingWindow: () -> Unit,
    onOpenInNewWindow: () -> Unit
) {
    val formattedSize = remember(file.length()) {
        FormatUtils.formatFileSize(file.length())
    }
    val formattedDate = remember(file.lastModified()) {
        FormatUtils.formatDateTimeMedium(file.lastModified())
    }
    val fileExt = remember(file.name) {
        file.extension.uppercase().ifEmpty { "NOTE" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Window,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.note_already_open_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.a11yHeading()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                var headerInteractionTimestamp by remember { mutableStateOf(0L) }

                // File info banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(file.path) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val hasTouch = event.changes.any { it.pressed || it.positionChanged() }
                                    if (hasTouch || event.type == PointerEventType.Enter || event.type == PointerEventType.Move) {
                                        headerInteractionTimestamp = System.currentTimeMillis()
                                    }
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (fileExt) {
                                "PDF" -> MaterialTheme.colorScheme.errorContainer
                                "XOPP" -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = fileExt,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = when (fileExt) {
                                    "PDF" -> MaterialTheme.colorScheme.onErrorContainer
                                    "XOPP" -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            InteractiveMarqueeText(
                                text = file.nameWithoutExtension,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                externalTrigger = headerInteractionTimestamp
                            )
                            Text(
                                text = "$formattedSize • $formattedDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.note_already_open_prompt, file.nameWithoutExtension),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: View Existing Window
                PromptOptionCard(
                    icon = Icons.Default.FlipToFront,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = stringResource(R.string.note_open_view_existing_title),
                    subtitle = stringResource(R.string.note_open_view_existing_subtitle),
                    onClick = {
                        onDismiss()
                        onViewExistingWindow()
                    }
                )

                // Option 2: Open in New Window
                PromptOptionCard(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = stringResource(R.string.note_open_in_new_window_title),
                    subtitle = stringResource(R.string.note_open_in_new_window_subtitle),
                    onClick = {
                        onDismiss()
                        onOpenInNewWindow()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
private fun PromptOptionCard(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconContainerColor,
                modifier = Modifier.size(42.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
