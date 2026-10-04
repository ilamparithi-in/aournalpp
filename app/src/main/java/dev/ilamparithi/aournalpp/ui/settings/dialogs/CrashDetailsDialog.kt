package dev.ilamparithi.aournalpp.ui.settings.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import dev.ilamparithi.aournalpp.logging.LogShareHelper
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.logging.CrashReport
import dev.ilamparithi.aournalpp.logging.CrashType
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsIconBadge
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget

/**
 * Responsive modal dialog presenting full crash diagnostics, stack traces,
 * environment specifications, and quick export/share actions.
 *
 * Designed to gracefully handle any length of text across compact phones,
 * large tablets, portrait, and landscape orientations.
 */
@Composable
fun CrashDetailsDialog(
    report: CrashReport,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    val copiedToastMsg = stringResource(R.string.log_manager_copied_toast)
    val shareChooserTitle = stringResource(R.string.log_manager_share_report)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val isCompact = maxWidth < 600.dp
            Surface(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 800.dp)
                    .fillMaxWidth(if (isCompact) 0.95f else 0.85f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(
                            icon = Icons.Default.Warning
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.log_manager_crash_details_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.a11yHeading()
                            )
                            Text(
                                text = "${report.formattedTimestamp} • ${report.processName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Scrollable Content Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = report.errorClass,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.errorMessage.ifBlank { "(No error message)" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (report.exitCode != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Process exit code / signal: ${report.exitCode}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Section 1: Device & Environment
                    Text(
                        text = stringResource(R.string.log_manager_section_environment),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MetadataRow(label = "App Version", value = "${report.deviceMetadata.appVersionName} (${report.deviceMetadata.appVersionCode})")
                            MetadataRow(label = "Device", value = "${report.deviceMetadata.manufacturer} ${report.deviceMetadata.model} (${report.deviceMetadata.device})")
                            MetadataRow(label = "Android OS", value = "${report.deviceMetadata.androidVersion} (API ${report.deviceMetadata.sdkInt})")
                            MetadataRow(label = "ABIs", value = report.deviceMetadata.supportedAbis.joinToString(", "))
                            if (report.deviceMetadata.totalRamMb > 0) {
                                MetadataRow(label = "Memory", value = "Available ${report.deviceMetadata.availableRamMb} MB / Total ${report.deviceMetadata.totalRamMb} MB")
                            }
                            if (!report.threadName.isNullOrBlank()) {
                                MetadataRow(label = "Thread", value = report.threadName)
                            }
                        }
                    }

                    // Section 2: Stack Trace / Output
                    Text(
                        text = stringResource(R.string.log_manager_section_stacktrace),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 320.dp)
                                .horizontalScroll(rememberScrollState())
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    text = report.stackTrace.ifBlank { "(No stack trace recorded)" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Section 3: Context Logcat Tail (if available)
                    if (report.recentLogs.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.log_manager_section_logcat_tail),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 240.dp)
                                    .horizontalScroll(rememberScrollState())
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(
                                        text = report.recentLogs.joinToString("\n"),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Actions Row (Adaptive for wrap on small screens)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onDelete(report.id)
                            onDismiss()
                        },
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.log_manager_delete_crash),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.log_manager_delete_crash),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                LogShareHelper.copyToClipboard(
                                    context = context,
                                    label = "Crash Report",
                                    text = report.toMarkdown(),
                                    toastMessage = copiedToastMsg
                                )
                            },
                            modifier = Modifier.minTouchTarget()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.log_manager_copy_report))
                        }

                        Button(
                            onClick = {
                                LogShareHelper.shareTextFile(
                                    context = context,
                                    fileName = "crash_${report.id}.md",
                                    content = report.toMarkdown(),
                                    subject = "Aournal++ Crash Report - ${report.id}",
                                    chooserTitle = shareChooserTitle
                                )
                            },
                            modifier = Modifier.minTouchTarget()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.log_manager_share_report))
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
