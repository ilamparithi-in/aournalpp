package dev.ilamparithi.aournalpp.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.utils.FormatUtils
import dev.ilamparithi.aournalpp.utils.NoteOpenAction
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import java.io.File

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget

/**
 * Material 3 standard prompt dialog displayed when a note is opened and default action is set to "Ask every time".
 * Allows selecting "View as PDF" or "Edit in Xournal++", with a "Don't ask again" preference checkbox.
 */
@Composable
fun NoteOpenActionDialog(
    file: File,
    isImport: Boolean = false,
    onDismiss: () -> Unit,
    onViewAsPdf: () -> Unit,
    onEditInCanvas: () -> Unit
) {
    val context = LocalContext.current
    var dontAskAgain by remember { mutableStateOf(false) }

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
                color = if (isImport) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isImport) Icons.Default.FileDownload else Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = if (isImport) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = androidx.compose.ui.res.stringResource(
                    if (isImport) dev.ilamparithi.aournalpp.R.string.note_import_title
                    else dev.ilamparithi.aournalpp.R.string.note_open_title
                ),
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

                if (isImport) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiary
                            ) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.note_import_pill),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.note_import_banner_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.note_open_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val isPdf = remember(file) { NoteOpenManager.isPdf(file) }
                val defaultAction = remember(file) { NoteOpenManager.getDefaultAction(context, file) }

                // Option 1: View as PDF
                ActionOptionCard(
                    icon = Icons.Default.PictureAsPdf,
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                    title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.note_open_view_pdf_title),
                    subtitle = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.note_open_view_pdf_subtitle),
                    isDefault = defaultAction == NoteOpenAction.VIEW,
                    onClick = {
                        if (dontAskAgain) {
                            NoteOpenManager.setDefaultAction(context, file, NoteOpenAction.VIEW)
                            val toastMsg = if (isPdf) {
                                context.getString(dev.ilamparithi.aournalpp.R.string.msg_default_action_set_view_pdf)
                            } else {
                                context.getString(dev.ilamparithi.aournalpp.R.string.msg_default_action_set_view_xopp)
                            }
                            Toast.makeText(
                                context,
                                toastMsg,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        onDismiss()
                        onViewAsPdf()
                    }
                )

                // Option 2: Edit in Xournal++
                ActionOptionCard(
                    icon = Icons.Default.Edit,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.note_open_edit_canvas_title),
                    subtitle = androidx.compose.ui.res.stringResource(
                        if (isImport) dev.ilamparithi.aournalpp.R.string.note_import_edit_canvas_subtitle
                        else dev.ilamparithi.aournalpp.R.string.note_open_edit_canvas_subtitle
                    ),
                    isDefault = defaultAction == NoteOpenAction.EDIT,
                    onClick = {
                        if (dontAskAgain) {
                            NoteOpenManager.setDefaultAction(context, file, NoteOpenAction.EDIT)
                            val toastMsg = if (isPdf) {
                                context.getString(dev.ilamparithi.aournalpp.R.string.msg_default_action_set_edit_pdf)
                            } else {
                                context.getString(dev.ilamparithi.aournalpp.R.string.msg_default_action_set_edit_xopp)
                            }
                            Toast.makeText(
                                context,
                                toastMsg,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        onDismiss()
                        onEditInCanvas()
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))

                // "Set as default" checkbox row
                val stateSelected = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.state_selected)
                val stateNotSelected = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.state_not_selected)
                val setAsDefaultLabel = if (isPdf) {
                    androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.note_open_set_as_default_pdf)
                } else {
                    androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.note_open_set_as_default_xopp)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .minTouchTarget()
                        .semantics(mergeDescendants = true) {
                            role = Role.Checkbox
                            stateDescription = if (dontAskAgain) stateSelected else stateNotSelected
                        }
                        .clickable { dontAskAgain = !dontAskAgain }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dontAskAgain,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = setAsDefaultLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
private fun ActionOptionCard(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    isDefault: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDefault) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isDefault) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isDefault) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.note_open_default_badge),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
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
                tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
