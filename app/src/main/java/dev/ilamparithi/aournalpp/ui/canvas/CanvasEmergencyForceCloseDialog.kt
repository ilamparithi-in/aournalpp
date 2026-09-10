package dev.ilamparithi.aournalpp.ui.canvas

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.promptWidth

@Composable
fun CanvasEmergencyForceCloseDialog(
    onConfirmForceClose: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Non-dismissible on tap outside or back press */ },
        properties = AppDialogDefaults.properties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        modifier = Modifier.promptWidth(),
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Force Close Session?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Xournal++ is not responding to normal close requests. Force terminating will immediately exit the canvas.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = onConfirmForceClose
            ) {
                Text("Force Close")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Wait / Cancel")
            }
        }
    )
}
