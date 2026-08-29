package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import java.util.UUID

@Composable
fun CustomMappingDialog(
    serviceId: String,
    initialMapping: CustomFolderMapping? = null,
    onDismissRequest: () -> Unit,
    onSaveMapping: (CustomFolderMapping) -> Unit
) {
    var localPath by remember { mutableStateOf(initialMapping?.localFolderPath ?: "") }
    var remotePath by remember { mutableStateOf(initialMapping?.remoteFolderPath ?: "") }
    var isEnabled by remember { mutableStateOf(initialMapping?.isEnabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (initialMapping == null) "Add Custom Folder Mapping" else "Edit Folder Mapping",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = localPath,
                    onValueChange = { localPath = it },
                    label = { Text("Local Folder Path") },
                    placeholder = { Text("/sdcard/Documents/Notes/Math") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    label = { Text("Remote Destination Folder") },
                    placeholder = { Text("Nextcloud_Math_Sync") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Mapping", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (localPath.isNotBlank() && remotePath.isNotBlank()) {
                        val mapping = CustomFolderMapping(
                            id = initialMapping?.id ?: UUID.randomUUID().toString(),
                            serviceId = serviceId,
                            localFolderPath = localPath.trim(),
                            remoteFolderPath = remotePath.trim(),
                            isEnabled = isEnabled
                        )
                        onSaveMapping(mapping)
                        onDismissRequest()
                    }
                },
                enabled = localPath.isNotBlank() && remotePath.isNotBlank()
            ) {
                Text("Save Mapping")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
