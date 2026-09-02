package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.MappingSet
import dev.ilamparithi.aournalpp.backup.model.MappingTemplateItem
import dev.ilamparithi.aournalpp.utils.FormatUtils
import java.util.UUID

@Composable
fun MappingSetsDialog(
    currentServiceId: String,
    currentServiceName: String,
    currentMappings: List<CustomFolderMapping>,
    existingSets: List<MappingSet>,
    onDismissRequest: () -> Unit,
    onSaveSet: (MappingSet) -> Unit,
    onDeleteSet: (String) -> Unit,
    onApplySet: (set: MappingSet, replace: Boolean) -> Unit
) {
    var showCreateSetDialog by remember { mutableStateOf(false) }
    var setPendingApply by remember { mutableStateOf<MappingSet?>(null) }
    var setPendingDelete by remember { mutableStateOf<MappingSet?>(null) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.title_mapping_sets),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.desc_mapping_sets),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar: Save Current Mappings as Set
                if (currentMappings.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showCreateSetDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_save_as_set))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Sets List
                if (existingSets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved mapping sets yet. Save your current mappings as a template to reuse them anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(existingSets, key = { it.id }) { set ->
                            MappingSetCard(
                                set = set,
                                onApply = { setPendingApply = set },
                                onDelete = { setPendingDelete = set }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // Dialog to name and save new set
    if (showCreateSetDialog) {
        var setName by remember { mutableStateOf("") }
        var setDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateSetDialog = false },
            title = { Text(stringResource(R.string.title_save_mapping_set), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = setName,
                        onValueChange = { setName = it },
                        label = { Text("Set Name") },
                        placeholder = { Text(stringResource(R.string.hint_set_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = setDesc,
                        onValueChange = { setDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (setName.isNotBlank()) {
                            val items = currentMappings.map { m ->
                                MappingTemplateItem(
                                    name = m.name,
                                    localFolderPath = m.localFolderPath,
                                    remoteFolderPath = m.remoteFolderPath,
                                    isEnabled = m.isEnabled
                                )
                            }
                            val newSet = MappingSet(
                                id = UUID.randomUUID().toString(),
                                name = setName.trim(),
                                description = setDesc.trim(),
                                createdAtEpochMs = System.currentTimeMillis(),
                                items = items
                            )
                            onSaveSet(newSet)
                            showCreateSetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog to choose Replace vs Append when applying set
    setPendingApply?.let { set ->
        AlertDialog(
            onDismissRequest = { setPendingApply = null },
            title = { Text(stringResource(R.string.title_apply_mapping_set, set.name), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.desc_apply_mapping_set, currentServiceName))
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onApplySet(set, false) // append
                            setPendingApply = null
                        }
                    ) {
                        Text(stringResource(R.string.action_append_mappings))
                    }
                    Button(
                        onClick = {
                            onApplySet(set, true) // replace
                            setPendingApply = null
                        }
                    ) {
                        Text(stringResource(R.string.action_replace_mappings))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { setPendingApply = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog to confirm delete set
    setPendingDelete?.let { set ->
        AlertDialog(
            onDismissRequest = { setPendingDelete = null },
            title = { Text("Delete Mapping Set", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${set.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSet(set.id)
                        setPendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { setPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MappingSetCard(
    set: MappingSet,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = set.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (set.description.isNotBlank()) {
                        Text(
                            text = set.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${set.items.size} mapped folders • Created ${FormatUtils.formatDateMedium(set.createdAtEpochMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Set",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = onApply,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_apply_set))
                    }
                }
            }

            // Preview items
            if (set.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    set.items.take(3).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${item.name.ifBlank { "Folder" }}: ${item.localFolderPath} → ${item.remoteFolderPath}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    if (set.items.size > 3) {
                        Text(
                            text = "+ ${set.items.size - 3} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
