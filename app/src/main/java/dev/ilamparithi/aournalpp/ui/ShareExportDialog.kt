package dev.ilamparithi.aournalpp.ui

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.DocumentRepository.ShareExportFormat
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.ui.theme.ScallopShape
import dev.ilamparithi.aournalpp.ui.theme.SunnyShape
import dev.ilamparithi.aournalpp.utils.FileNameTemplateEngine

/**
 * Unified Share/Export Dialog for a single document.
 * Allows renaming, choosing format (PDF vs XOPP based on document type rules),
 * and choosing between Save (SAF picker) or Share (Android share sheet).
 */
@Composable
fun SingleShareExportDialog(
    note: NoteDocument,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (sanitizedName: String, format: ShareExportFormat) -> Unit,
    onShare: (sanitizedName: String, format: ShareExportFormat) -> Unit
) {
    val isSourcePdf = remember(note.file.name) {
        note.file.extension.equals("pdf", ignoreCase = true)
    }
    val isSourceXoj = remember(note.file.name) {
        note.file.extension.equals("xoj", ignoreCase = true)
    }

    // PDF files cannot be exported as XOPP, and XOJ cannot be converted to XOPP headlessly.
    val canExportAsXopp = !isSourcePdf && !isSourceXoj

    var selectedFormat by remember {
        mutableStateOf(if (canExportAsXopp) ShareExportFormat.PDF else ShareExportFormat.PDF)
    }

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(0, initialName.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val sanitized = FileNameTemplateEngine.sanitizeFileName(textFieldValue.text)
    val isError = textFieldValue.text.isNotBlank() && sanitized.isEmpty()
    val isActionEnabled = sanitized.isNotBlank() && !isError

    val currentExtension = when (selectedFormat) {
        ShareExportFormat.PDF -> ".pdf"
        ShareExportFormat.XOPP -> ".xopp"
        ShareExportFormat.ORIGINAL -> if (isSourcePdf) ".pdf" else ".xopp"
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
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_share_export_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InteractiveMarqueeText(
                    text = stringResource(R.string.dialog_share_export_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. File Name Input
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    label = { Text(stringResource(R.string.dialog_note_name_hint)) },
                    suffix = {
                        Text(
                            text = currentExtension,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    trailingIcon = {
                        if (textFieldValue.text.isNotEmpty()) {
                            IconButton(onClick = { textFieldValue = TextFieldValue("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.action_clear),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("File name cannot be empty or contain invalid symbols") }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors()
                )

                // 2. Format Selection
                Text(
                    text = "File Format",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PDF Option (Always Available)
                    FormatOptionCard(
                        title = stringResource(R.string.format_pdf_title),
                        description = stringResource(R.string.format_pdf_desc),
                        icon = Icons.Default.PictureAsPdf,
                        isSelected = selectedFormat == ShareExportFormat.PDF,
                        onClick = { selectedFormat = ShareExportFormat.PDF }
                    )

                    // XOPP Option (Available only if original is XOPP)
                    if (canExportAsXopp) {
                        FormatOptionCard(
                            title = stringResource(R.string.format_xopp_title),
                            description = stringResource(R.string.format_xopp_desc),
                            icon = Icons.Default.Description,
                            isSelected = selectedFormat == ShareExportFormat.XOPP,
                            onClick = { selectedFormat = ShareExportFormat.XOPP }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save Button (SAF picker)
                FilledTonalButton(
                    onClick = {
                        if (isActionEnabled) {
                            onSave(sanitized, selectedFormat)
                        }
                    },
                    enabled = isActionEnabled,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_save_picker))
                }

                // Share Button (System share sheet)
                Button(
                    onClick = {
                        if (isActionEnabled) {
                            onShare(sanitized, selectedFormat)
                        }
                    },
                    enabled = isActionEnabled,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share_sheet))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Unified Share/Export Dialog for multiple documents (batch actions).
 * Renaming is omitted. Users select the target format:
 * - PDF (converts all to PDF)
 * - XOPP (available if all selected files are XOPP)
 * - Original Formats (preserves each file as-is)
 * and choose between Save (SAF folder picker) or Share (Android share sheet).
 */
@Composable
fun BatchShareExportDialog(
    selectedNotes: List<NoteDocument>,
    onDismiss: () -> Unit,
    onSaveBatch: (format: ShareExportFormat) -> Unit,
    onShareBatch: (format: ShareExportFormat) -> Unit
) {
    val totalCount = selectedNotes.size
    val allXopp = remember(selectedNotes) {
        selectedNotes.all { it.file.extension.equals("xopp", ignoreCase = true) }
    }
    val hasMultipleTypes = remember(selectedNotes) {
        val distinctExts = selectedNotes.map { it.file.extension.lowercase() }.distinct()
        distinctExts.size > 1
    }

    var selectedFormat by remember {
        mutableStateOf(
            if (allXopp) ShareExportFormat.XOPP
            else if (hasMultipleTypes) ShareExportFormat.PDF
            else ShareExportFormat.PDF
        )
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
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_batch_share_export_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val subtitleText = "$totalCount items selected"
                InteractiveMarqueeText(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Select Export Format",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Convert all to PDF
                    FormatOptionCard(
                        title = "Convert all to PDF",
                        description = "Generates standard PDF documents for all selected notes",
                        icon = Icons.Default.PictureAsPdf,
                        isSelected = selectedFormat == ShareExportFormat.PDF,
                        onClick = { selectedFormat = ShareExportFormat.PDF }
                    )

                    // Option 2: XOPP (Only if all selected are XOPP)
                    if (allXopp) {
                        FormatOptionCard(
                            title = stringResource(R.string.format_xopp_title),
                            description = "Keep notes in native editable Xournal++ format",
                            icon = Icons.Default.Description,
                            isSelected = selectedFormat == ShareExportFormat.XOPP,
                            onClick = { selectedFormat = ShareExportFormat.XOPP }
                        )
                    }

                    // Option 3: Original Formats (As is)
                    FormatOptionCard(
                        title = stringResource(R.string.format_original_title),
                        description = stringResource(R.string.format_original_desc),
                        icon = Icons.Default.Share,
                        isSelected = selectedFormat == ShareExportFormat.ORIGINAL,
                        onClick = { selectedFormat = ShareExportFormat.ORIGINAL }
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save Button (Folder picker)
                FilledTonalButton(
                    onClick = { onSaveBatch(selectedFormat) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_save_picker))
                }

                // Share Button (Multiple attachments)
                Button(
                    onClick = { onShareBatch(selectedFormat) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share_sheet))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Clean selectable card for format options.
 */
@Composable
private fun FormatOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            RadioButton(
                selected = isSelected,
                onClick = null
            )
        }
    }
}

/**
 * Expressive hero animation adapted from the Linux environment bootstrap screen,
 * featuring a rotating SunnyShape, counter-rotating ScallopShape, and a pulsing PDF icon.
 */
@Composable
fun PdfHeroAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    ExpressiveHeroSpinner(
        modifier = modifier,
        size = size,
        icon = Icons.Default.PictureAsPdf,
        iconDescription = "PDF Icon"
    )
}

/**
 * Expressive M3 progress dialog shown during PDF conversion / export / sharing.
 * Replaces the basic spinner with the expressive animated element from the Linux environment screen.
 */
@Composable
fun PdfConversionProgressDialog(
    message: String,
    onDismissRequest: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = {
            PdfHeroAnimation(size = 80.dp)
        },
        title = {
            Text(
                androidx.compose.ui.res.stringResource(R.string.title_processing_document),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {}
    )
}

