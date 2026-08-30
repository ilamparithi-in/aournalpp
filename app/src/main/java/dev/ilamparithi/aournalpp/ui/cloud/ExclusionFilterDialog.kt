package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.ui.util.a11yHeading
import dev.ilamparithi.aournalpp.ui.util.minTouchTarget
import dev.ilamparithi.aournalpp.backup.model.ExclusionFilterConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExclusionFilterDialog(
    initialConfig: ExclusionFilterConfig = ExclusionFilterConfig.DEFAULT,
    onDismissRequest: () -> Unit,
    onSaveFilter: (ExclusionFilterConfig) -> Unit
) {
    var skipDefaultTransient by remember { mutableStateOf(initialConfig.skipDefaultTransient) }
    var regexList by remember { mutableStateOf(initialConfig.regexPatterns) }
    var excludedExtSet by remember { mutableStateOf(initialConfig.excludedExtensions) }
    var excludedFolderSet by remember { mutableStateOf(initialConfig.excludedFolderPaths) }

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
                text = "Configure Exclusion Filters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.a11yHeading()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Transient files toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ignore Transient & Lock Files", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Skips *.autosave.xopp, .X0-lock, .sock, and swap files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = skipDefaultTransient,
                        onCheckedChange = { skipDefaultTransient = it }
                    )
                }

                // Regex Patterns
                Text("Filename Regex Patterns", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newRegexInput,
                        onValueChange = { newRegexInput = it },
                        placeholder = { Text("^.*\\.draft$") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            if (newRegexInput.isNotBlank()) {
                                regexList = regexList + newRegexInput.trim()
                                newRegexInput = ""
                            }
                        },
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_pattern))
                    }
                }

                if (regexList.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        regexList.forEach { pattern ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    regexList = regexList - pattern
                                },
                                label = { Text(pattern) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cd_remove_filter),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.minTouchTarget()
                            )
                        }
                    }
                }

                // Excluded Extensions
                Text("Excluded Extensions", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newExtInput,
                        onValueChange = { newExtInput = it },
                        placeholder = { Text("bak, tmp, log") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            if (newExtInput.isNotBlank()) {
                                val clean = newExtInput.trim().trimStart('.').lowercase()
                                excludedExtSet = excludedExtSet + clean
                                newExtInput = ""
                            }
                        },
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_extension))
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
                                        contentDescription = stringResource(R.string.cd_remove_filter),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.minTouchTarget()
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
                        },
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_folder))
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
                                        contentDescription = stringResource(R.string.cd_remove_filter),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.minTouchTarget()
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ResetDefaultsButton(
                    onReset = {
                        val def = ExclusionFilterConfig.DEFAULT
                        skipDefaultTransient = def.skipDefaultTransient
                        regexList = def.regexPatterns
                        excludedExtSet = def.excludedExtensions
                        excludedFolderSet = def.excludedFolderPaths
                    }
                )

                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun ResetDefaultsButton(onReset: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    var showResetHint by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showResetHint = true
                    },
                    onLongPress = {
                        try {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (_: Exception) {}
                        onReset()
                        showResetHint = false
                    }
                )
            }
    ) {
        Text(
            text = if (showResetHint) "Hold to Reset" else "Reset Defaults",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (showResetHint) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
