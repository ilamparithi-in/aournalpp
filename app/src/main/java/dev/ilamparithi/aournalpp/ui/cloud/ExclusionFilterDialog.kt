package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.DialogProperties
import dev.ilamparithi.aournalpp.backup.model.ExclusionFilterConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExclusionFilterDialog(
    initialFilter: ExclusionFilterConfig,
    onDismissRequest: () -> Unit,
    onSaveFilter: (ExclusionFilterConfig) -> Unit
) {
    var skipDefaultTransient by remember { mutableStateOf(initialFilter.skipDefaultTransient) }
    var regexList by remember { mutableStateOf(initialFilter.regexPatterns) }
    var excludedExtSet by remember { mutableStateOf(initialFilter.excludedExtensions) }
    var excludedFolderSet by remember { mutableStateOf(initialFilter.excludedFolderPaths) }

    var newRegexInput by remember { mutableStateOf("") }
    var newExtInput by remember { mutableStateOf("") }
    var newFolderInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 16.dp),
        title = {
            Text(
                text = "Configurable Exclusion Filters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Default transient skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Skip Transient & Lock Files", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Automatically ignores .autosave.xopp, .tmp, .sock, .swp, .X0-lock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = skipDefaultTransient,
                        onCheckedChange = { skipDefaultTransient = it }
                    )
                }

                // File Name Regex Patterns
                Text("Filename Regex Exclusions", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newRegexInput,
                        onValueChange = { newRegexInput = it },
                        placeholder = { Text("^.*_draft\\.xopp$") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            if (newRegexInput.isNotBlank() && newRegexInput !in regexList) {
                                regexList = regexList + newRegexInput.trim()
                                newRegexInput = ""
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add regex")
                    }
                }

                if (regexList.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        regexList.forEach { regex ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(regex) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(2.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Excluded Extensions
                Text("Excluded File Extensions", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newExtInput,
                        onValueChange = { newExtInput = it },
                        placeholder = { Text("bak, old, tmp") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            val ext = newExtInput.trim().removePrefix(".").lowercase()
                            if (ext.isNotBlank()) {
                                excludedExtSet = excludedExtSet + ext
                                newExtInput = ""
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add extension")
                    }
                }

                if (excludedExtSet.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        excludedExtSet.forEach { ext ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    excludedExtSet = excludedExtSet - ext
                                },
                                label = { Text(".$ext") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Excluded Folder Paths
                Text("Excluded Folder Paths", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newFolderInput,
                        onValueChange = { newFolderInput = it },
                        placeholder = { Text("/path/to/ignore") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            if (newFolderInput.isNotBlank()) {
                                excludedFolderSet = excludedFolderSet + newFolderInput.trim()
                                newFolderInput = ""
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add folder")
                    }
                }

                if (excludedFolderSet.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        excludedFolderSet.forEach { folder ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    excludedFolderSet = excludedFolderSet - folder
                                },
                                label = { Text(folder) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val config = ExclusionFilterConfig(
                        regexPatterns = regexList,
                        excludedExtensions = excludedExtSet,
                        excludedFolderPaths = excludedFolderSet,
                        skipDefaultTransient = skipDefaultTransient
                    )
                    onSaveFilter(config)
                    onDismissRequest()
                }
            ) {
                Text("Save Filters")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val def = ExclusionFilterConfig.DEFAULT
                        skipDefaultTransient = def.skipDefaultTransient
                        regexList = def.regexPatterns
                        excludedExtSet = def.excludedExtensions
                        excludedFolderSet = def.excludedFolderPaths
                    }
                ) {
                    Text("Reset Defaults")
                }
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        }
    )
}
