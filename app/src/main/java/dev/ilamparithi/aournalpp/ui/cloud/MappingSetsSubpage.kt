package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.MappingSet
import dev.ilamparithi.aournalpp.backup.model.MappingTemplateItem
import dev.ilamparithi.aournalpp.backup.security.CustomMappingRepository
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.ui.util.a11yHeading
import java.io.File
import java.util.UUID

/**
 * Dedicated subpage common to all clouds displaying all saved mapping sets.
 * Searchable across set name, description, constituent mapping names, and folder paths.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingSetsSubpage(
    services: List<ServiceConfig>,
    mappingRepo: CustomMappingRepository,
    onNavigateBack: () -> Unit,
    onApplySetToService: (service: ServiceConfig, set: MappingSet, replace: Boolean) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    var allSets by remember { mutableStateOf(mappingRepo.getAllMappingSets()) }
    var searchQuery by remember { mutableStateOf("") }

    var showCreateSetDialog by remember { mutableStateOf(false) }
    var setPendingApply by remember { mutableStateOf<MappingSet?>(null) }
    var setPendingEdit by remember { mutableStateOf<MappingSet?>(null) }
    var setPendingDelete by remember { mutableStateOf<MappingSet?>(null) }

    fun refreshSets() {
        allSets = mappingRepo.getAllMappingSets()
    }

    // Filter sets based on set name, description, constituent mapping names, and folder paths
    val filteredSets = remember(allSets, searchQuery) {
        if (searchQuery.isBlank()) {
            allSets
        } else {
            val q = searchQuery.trim().lowercase()
            allSets.filter { set ->
                set.name.lowercase().contains(q) ||
                        set.description.lowercase().contains(q) ||
                        set.items.any { item ->
                            item.name.lowercase().contains(q) ||
                                    item.localFolderPath.lowercase().contains(q) ||
                                    item.remoteFolderPath.lowercase().contains(q)
                        }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_mapping_sets),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.a11yHeading()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showCreateSetDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_new_set))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.hint_search_sets)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Empty state
            if (filteredSets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) stringResource(R.string.empty_sets_search, searchQuery)
                                else stringResource(R.string.empty_sets_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (searchQuery.isBlank()) {
                                Text(
                                    text = stringResource(R.string.empty_sets_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showCreateSetDialog = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_new_set))
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredSets, key = { it.id }) { set ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Row: Bookmark icon, Name, Count Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = set.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (set.description.isNotBlank()) {
                                            Text(
                                                text = set.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text(
                                        text = "${set.items.size} ${if (set.items.size == 1) "folder" else "folders"}",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Constituent Mappings Short Preview
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                set.items.take(4).forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.name.ifBlank { File(item.localFolderPath).name },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val localName = if (item.localFolderPath.contains('/')) item.localFolderPath.substringAfterLast('/') else item.localFolderPath
                                        Text(
                                            text = "($localName → ${item.remoteFolderPath})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (set.items.size > 4) {
                                    Text(
                                        text = stringResource(R.string.label_more_mappings_overflow, set.items.size - 4),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons: Apply to Cloud, Edit, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FilledTonalButton(
                                    onClick = { setPendingApply = set },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Apply to Cloud…")
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { setPendingEdit = set },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.action_edit))
                                    }

                                    IconButton(onClick = { setPendingDelete = set }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.action_delete),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Apply Set to Cloud Service Dialog
    setPendingApply?.let { set ->
        var selectedServiceId by remember { mutableStateOf(services.firstOrNull()?.id ?: "") }
        var replaceExisting by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { setPendingApply = null },
            title = {
                Text(
                    text = stringResource(R.string.dialog_apply_set_title, set.name),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.label_select_target_cloud),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (services.isEmpty()) {
                        Text(
                            text = "No cloud services configured. Please add a cloud service first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            services.forEach { service ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedServiceId = service.id }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedServiceId == service.id,
                                        onClick = { selectedServiceId = service.id }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${service.name} (${service.providerType.displayName})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Application mode:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { replaceExisting = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = replaceExisting,
                                onClick = { replaceExisting = true }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.label_replace_mappings), style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { replaceExisting = false }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !replaceExisting,
                                onClick = { replaceExisting = false }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.label_append_mappings), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = services.firstOrNull { it.id == selectedServiceId }
                        if (target != null) {
                            onApplySetToService(target, set, replaceExisting)
                            setPendingApply = null
                        }
                    },
                    enabled = services.isNotEmpty() && selectedServiceId.isNotBlank()
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { setPendingApply = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Dialog 2: Create or Edit Mapping Set Dialog
    if (showCreateSetDialog || setPendingEdit != null) {
        val editing = setPendingEdit
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var description by remember { mutableStateOf(editing?.description ?: "") }

        AlertDialog(
            onDismissRequest = {
                showCreateSetDialog = false
                setPendingEdit = null
            },
            title = {
                Text(
                    text = if (editing != null) "Edit Mapping Set" else "New Mapping Set",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Set Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val setToSave = if (editing != null) {
                                editing.copy(name = name.trim(), description = description.trim())
                            } else {
                                MappingSet(
                                    id = UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    description = description.trim(),
                                    createdAtEpochMs = System.currentTimeMillis(),
                                    items = emptyList()
                                )
                            }
                            mappingRepo.saveMappingSet(setToSave)
                            refreshSets()
                            onShowSnackbar("Saved mapping set \"${setToSave.name}\"")
                            showCreateSetDialog = false
                            setPendingEdit = null
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateSetDialog = false
                    setPendingEdit = null
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Dialog 3: Delete Set Confirmation Dialog
    setPendingDelete?.let { set ->
        AlertDialog(
            onDismissRequest = { setPendingDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_multi_title), fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete mapping set \"${set.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        mappingRepo.deleteMappingSet(set.id)
                        refreshSets()
                        onShowSnackbar("Deleted mapping set \"${set.name}\"")
                        setPendingDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { setPendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
