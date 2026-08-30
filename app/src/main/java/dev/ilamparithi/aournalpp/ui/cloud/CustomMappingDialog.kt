package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomMappingDialog(
    services: List<ServiceConfig>,
    initialServiceId: String? = null,
    initialMapping: CustomFolderMapping? = null,
    initialLocalPath: String = "",
    onDismissRequest: () -> Unit,
    onSaveMapping: (serviceId: String, CustomFolderMapping) -> Unit
) {
    val context = LocalContext.current
    val env = remember { LinuxEnvironment(context) }
    val notesDir = remember { env.getNotesDirectory() }

    var selectedServiceId by remember {
        mutableStateOf(initialServiceId ?: initialMapping?.serviceId ?: services.firstOrNull()?.id ?: "")
    }
    var isServiceDropdownExpanded by remember { mutableStateOf(false) }

    var localPath by remember {
        mutableStateOf(initialMapping?.localFolderPath ?: initialLocalPath.ifBlank { notesDir.absolutePath })
    }
    var remotePath by remember {
        mutableStateOf(initialMapping?.remoteFolderPath ?: if (initialLocalPath.isNotBlank()) File(initialLocalPath).name else "")
    }
    var isEnabled by remember { mutableStateOf(initialMapping?.isEnabled ?: true) }

    var showLocalBrowser by remember { mutableStateOf(false) }
    var showRemoteBrowser by remember { mutableStateOf(false) }

    val currentSelectedService = services.firstOrNull { it.id == selectedServiceId }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f),
        title = {
            Text(
                text = if (initialMapping == null) "Add Custom Folder Mapping" else "Edit Folder Mapping",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Target Cloud Service Selector
                if (services.size > 1 && initialMapping == null && initialServiceId == null) {
                    ExposedDropdownMenuBox(
                        expanded = isServiceDropdownExpanded,
                        onExpandedChange = { isServiceDropdownExpanded = !isServiceDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentSelectedService?.name ?: "Select Cloud Service",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Cloud Service") },
                            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isServiceDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isServiceDropdownExpanded,
                            onDismissRequest = { isServiceDropdownExpanded = false }
                        ) {
                            services.forEach { srv ->
                                DropdownMenuItem(
                                    text = { Text("${srv.name} (${srv.providerType.displayName})") },
                                    onClick = {
                                        selectedServiceId = srv.id
                                        isServiceDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Local Folder Selector with Browse Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = localPath,
                        onValueChange = { localPath = it },
                        label = { Text("Local Folder Path") },
                        placeholder = { Text(notesDir.absolutePath) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showLocalBrowser = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Browse Local Folder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Remote Destination Folder with Browse Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = remotePath,
                        onValueChange = { remotePath = it },
                        label = { Text("Remote Destination Folder") },
                        placeholder = { Text("Notes/Math") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showRemoteBrowser = true },
                        enabled = currentSelectedService != null,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Browse Remote Folder",
                            tint = if (currentSelectedService != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Mapping", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
                    if (selectedServiceId.isNotBlank() && localPath.isNotBlank() && remotePath.isNotBlank()) {
                        val mapping = CustomFolderMapping(
                            id = initialMapping?.id ?: UUID.randomUUID().toString(),
                            serviceId = selectedServiceId,
                            localFolderPath = localPath.trim(),
                            remoteFolderPath = remotePath.trim(),
                            isEnabled = isEnabled
                        )
                        onSaveMapping(selectedServiceId, mapping)
                        onDismissRequest()
                    }
                },
                enabled = selectedServiceId.isNotBlank() && localPath.isNotBlank() && remotePath.isNotBlank()
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

    // Local Folder Browser Dialog
    if (showLocalBrowser) {
        FolderBrowserDialog(
            mode = FolderBrowserMode.LOCAL,
            title = "Browse Local Notes Folder",
            initialPath = localPath,
            rootDirectory = notesDir,
            onFolderSelected = { selected -> localPath = selected },
            onDismissRequest = { showLocalBrowser = false }
        )
    }

    // Remote Folder Browser Dialog
    if (showRemoteBrowser && currentSelectedService != null) {
        FolderBrowserDialog(
            mode = FolderBrowserMode.REMOTE,
            title = "Browse Remote Cloud Folders",
            initialPath = remotePath,
            serviceConfig = currentSelectedService,
            onFolderSelected = { selected -> remotePath = selected },
            onDismissRequest = { showRemoteBrowser = false }
        )
    }
}
