package dev.ilamparithi.aournalpp.ui.hub.dialog

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.utils.FormatUtils
import java.io.File

@Composable
fun StoragePermissionPromptDialog(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = {
            Icon(
                imageVector = Icons.Default.FolderShared,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(stringResource(R.string.onboarding_storage_title), fontWeight = FontWeight.Bold) },
        text = {
            Text(stringResource(R.string.onboarding_storage_desc))
        },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.onboarding_storage_grant_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.onboarding_storage_skip_action))
            }
        }
    )
}

@Composable
fun EmergencyRecoveryDialog(
    emergencyFile: File,
    onDismiss: () -> Unit,
    onOpenNow: () -> Unit,
    onSaveAsNote: () -> Unit,
    onDiscard: () -> Unit
) {
    val dateStr = FormatUtils.formatDateTimeMedium(emergencyFile.lastModified())

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = {
            Icon(
                Icons.Default.Restore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(stringResource(R.string.dialog_emergency_recovery_title), fontWeight = FontWeight.Bold) },
        text = {
            Text(stringResource(R.string.dialog_emergency_recovery_body, dateStr))
        },
        confirmButton = {
            Button(onClick = onOpenNow) {
                Text(stringResource(R.string.action_open_now))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSaveAsNote) {
                    Text(stringResource(R.string.action_save_as_note))
                }
                TextButton(onClick = onDiscard) {
                    Text(stringResource(R.string.action_discard), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@Composable
fun AutoloadOverrideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = {
            Icon(
                Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(stringResource(R.string.dialog_autoload_override_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Text(stringResource(R.string.dialog_autoload_override_body))
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_got_it))
            }
        }
    )
}
