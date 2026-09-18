package dev.ilamparithi.aournalpp.ui.cloud

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionAction
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionReport
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.FileConflictResolution
import dev.ilamparithi.aournalpp.backup.model.FileVersionItem
import dev.ilamparithi.aournalpp.backup.model.FileVersionSource
import dev.ilamparithi.aournalpp.utils.AccessibilityUtils
import dev.ilamparithi.aournalpp.utils.FormatUtils
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import kotlinx.coroutines.launch
import java.io.File

/**
 * Dedicated full-screen screen for reviewing and resolving cloud synchronization conflicts.
 * Partitions Note conflicts (with rich origin badges and multi-selection) from
 * Configuration conflicts (which require exclusive either/or selection and line diff viewing).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConflictResolutionScreen(
    conflictGroups: List<FileConflictGroup>,
    engine: BackupEngine,
    onNavigateBack: () -> Unit,
    onApplyResolutions: (List<FileConflictResolution>) -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    // Partition groups into Notes and Configs
    val noteConflicts = remember(conflictGroups) {
        conflictGroups.filter { !BackupEngine.isConfigFile(it.relativePath) }
    }
    val configConflicts = remember(conflictGroups) {
        conflictGroups.filter { BackupEngine.isConfigFile(it.relativePath) }
    }

    var selectedTabIndex by remember(noteConflicts, configConflicts) {
        mutableIntStateOf(if (noteConflicts.isNotEmpty()) 0 else 1)
    }

    // Map of conflictGroupId -> set of selected FileVersionItem
    val selectedVersionsMap = remember(conflictGroups) {
        val map = mutableStateMapOf<String, Set<FileVersionItem>>()
        for (group in conflictGroups) {
            val isConfig = BackupEngine.isConfigFile(group.relativePath)
            if (isConfig) {
                // Default config selection: remote version if available, otherwise local
                val defaultChoice = group.remoteVersions.firstOrNull() ?: group.localVersion
                if (defaultChoice != null) {
                    map[group.id] = setOf(defaultChoice)
                }
            } else {
                // Default note selection: newest version as primary
                val newest = group.allVersions.maxByOrNull { it.lastModifiedEpochMs }
                if (newest != null) {
                    map[group.id] = setOf(newest)
                }
            }
        }
        map
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.title_cloud_conflicts),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.a11yHeading()
                        )
                        val noteCount = noteConflicts.size
                        val configCount = configConflicts.size
                        val subtitleText = buildString {
                            if (noteCount > 0) {
                                append(context.resources.getQuantityString(R.plurals.conflict_subtitle_notes, noteCount, noteCount))
                            }
                            if (configCount > 0) {
                                if (isNotEmpty()) append(" • ")
                                append(context.resources.getQuantityString(R.plurals.conflict_subtitle_config_files, configCount, configCount))
                            }
                        }
                        if (subtitleText.isNotEmpty()) {
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showMenu = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Select All Newest") },
                                onClick = {
                                    showMenu = false
                                    for (group in conflictGroups) {
                                        val newest = group.allVersions.maxByOrNull { it.lastModifiedEpochMs }
                                        if (newest != null) selectedVersionsMap[group.id] = setOf(newest)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Keep All Local") },
                                onClick = {
                                    showMenu = false
                                    for (group in conflictGroups) {
                                        group.localVersion?.let { selectedVersionsMap[group.id] = setOf(it) }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Keep All Cloud") },
                                onClick = {
                                    showMenu = false
                                    for (group in conflictGroups) {
                                        val firstRemote = group.remoteVersions.firstOrNull()
                                        if (firstRemote != null) selectedVersionsMap[group.id] = setOf(firstRemote)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val resolvedCount = conflictGroups.count { (selectedVersionsMap[it.id]?.size ?: 0) > 0 }
                    Column {
                        Text(
                            text = "$resolvedCount of ${conflictGroups.size} resolved",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap apply to synchronize changes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val resolutions = conflictGroups.map { group ->
                                val selected = selectedVersionsMap[group.id] ?: emptySet()
                                val action = mapSelectionToAction(group, selected)
                                FileConflictResolution(
                                    conflictGroupId = group.id,
                                    relativePath = group.relativePath,
                                    action = action,
                                    targetLocalPath = group.localFilePath
                                )
                            }
                            onApplyResolutions(resolutions)
                        },
                        enabled = resolvedCount > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Resolutions")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // If both notes and config conflicts exist, show tabs
            if (noteConflicts.isNotEmpty() && configConflicts.isNotEmpty()) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                text = "Notes (${noteConflicts.size})",
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Description, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                text = "Configurations (${configConflicts.size})",
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                }
            }

            val currentDisplayList = when {
                noteConflicts.isNotEmpty() && configConflicts.isNotEmpty() -> {
                    if (selectedTabIndex == 0) noteConflicts else configConflicts
                }
                noteConflicts.isNotEmpty() -> noteConflicts
                else -> configConflicts
            }

            val isConfigTab = currentDisplayList.firstOrNull()?.let { BackupEngine.isConfigFile(it.relativePath) } == true

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (isConfigTab) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Configuration files govern application settings and cannot be merged or kept alongside. Please select either the local or cloud version to keep.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                items(currentDisplayList, key = { it.id }) { group ->
                    val isConfig = BackupEngine.isConfigFile(group.relativePath)
                    val selectedSet = selectedVersionsMap[group.id] ?: emptySet()

                    ConflictResolutionItemCard(
                        group = group,
                        isConfig = isConfig,
                        selectedVersions = selectedSet,
                        onToggleVersion = { version ->
                            if (isConfig) {
                                // Exclusive radio selection for configs: exactly one chosen
                                selectedVersionsMap[group.id] = setOf(version)
                            } else {
                                // Multi-selection for notes: toggling
                                val current = selectedVersionsMap[group.id] ?: emptySet()
                                if (current.contains(version)) {
                                    if (current.size > 1) {
                                        selectedVersionsMap[group.id] = current - version
                                    }
                                } else {
                                    selectedVersionsMap[group.id] = current + version
                                }
                            }
                        },
                        onPreviewDiff = { targetGroup ->
                            context.startActivity(
                                ConfigDiffActivity.createIntent(
                                    context = context,
                                    fileName = targetGroup.fileName,
                                    localPath = targetGroup.localFilePath ?: "",
                                    remotePath = targetGroup.remoteFilePath ?: ""
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictResolutionItemCard(
    group: FileConflictGroup,
    isConfig: Boolean,
    selectedVersions: Set<FileVersionItem>,
    onToggleVersion: (FileVersionItem) -> Unit,
    onPreviewDiff: (FileConflictGroup) -> Unit
) {
    val orderedVersions = remember(group) {
        val list = mutableListOf<FileVersionItem>()
        group.localVersion?.let { list.add(it) }
        list.addAll(group.remoteVersions.sortedByDescending { it.lastModifiedEpochMs })
        list
    }

    val primaryVersion = remember(selectedVersions) {
        if (selectedVersions.isEmpty()) null
        else if (selectedVersions.size == 1) selectedVersions.first()
        else {
            selectedVersions.firstOrNull { it.source is FileVersionSource.LOCAL }
                ?: selectedVersions.maxByOrNull { it.lastModifiedEpochMs }
                ?: selectedVersions.first()
        }
    }

    val localVer = group.localVersion
    val firstRemoteVer = group.remoteVersions.firstOrNull()
    val relativeComparison = remember(localVer, firstRemoteVer) {
        if (localVer != null && firstRemoteVer != null) {
            val diffMs = firstRemoteVer.lastModifiedEpochMs - localVer.lastModifiedEpochMs
            val absDiffSeconds = kotlin.math.abs(diffMs) / 1000
            val absDiffMinutes = absDiffSeconds / 60
            val absDiffHours = absDiffMinutes / 60

            val timeAgoStr = when {
                absDiffHours > 0 -> "$absDiffHours hr${if (absDiffHours > 1) "s" else ""}"
                absDiffMinutes > 0 -> "$absDiffMinutes min${if (absDiffMinutes > 1) "s" else ""}"
                else -> "$absDiffSeconds sec"
            }

            if (diffMs > 2000L) {
                "Cloud version is $timeAgoStr newer"
            } else if (diffMs < -2000L) {
                "Local version is $timeAgoStr newer"
            } else {
                "Timestamps match closely (within 2s)"
            }
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: File Name & Mode Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = group.relativePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        selectedVersions.isEmpty() -> MaterialTheme.colorScheme.surfaceContainerHighest
                        selectedVersions.size == 1 -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    }
                ) {
                    Text(
                        text = when {
                            selectedVersions.isEmpty() -> "Not selected"
                            isConfig -> {
                                val s = selectedVersions.first().source.displayName
                                "$s Selected"
                            }
                            selectedVersions.size == 1 -> "1 Primary Copy"
                            else -> "1 Primary + ${selectedVersions.size - 1} Alongside"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            selectedVersions.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
                            selectedVersions.size == 1 -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Sub-row: Timestamp comparison badge & Preview Diff button
            if (relativeComparison != null || (isConfig && (group.localFilePath != null || group.remoteFilePath != null))) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (relativeComparison != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = relativeComparison,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (isConfig) {
                        TextButton(
                            onClick = { onPreviewDiff(group) },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Difference,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_preview_diff),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Version Options List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                orderedVersions.forEach { version ->
                    val isChecked = selectedVersions.contains(version)
                    val isPrimary = isChecked && version == primaryVersion
                    val isAlongside = isChecked && !isPrimary

                    ConflictVersionRow(
                        version = version,
                        isChecked = isChecked,
                        isPrimary = isPrimary,
                        isAlongside = isAlongside,
                        isExclusiveRadio = isConfig,
                        onToggle = { onToggleVersion(version) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictVersionRow(
    version: FileVersionItem,
    isChecked: Boolean,
    isPrimary: Boolean,
    isAlongside: Boolean,
    isExclusiveRadio: Boolean,
    onToggle: () -> Unit
) {
    val isLocal = version.source is FileVersionSource.LOCAL
    val formattedDate = FormatUtils.formatDateTimeMedium(version.lastModifiedEpochMs)
    val formattedSize = FormatUtils.formatFileSize(version.sizeBytes)

    val backgroundColor = when {
        isPrimary -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        isAlongside -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val borderColor = when {
        isPrimary -> MaterialTheme.colorScheme.primary
        isAlongside -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isChecked) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .minTouchTarget()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isExclusiveRadio) {
            RadioButton(
                selected = isChecked,
                onClick = onToggle,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
        } else {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            )
        }

        Icon(
            imageVector = if (isLocal) Icons.Default.Devices else Icons.Default.Cloud,
            contentDescription = null,
            tint = if (isLocal) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = version.source.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isPrimary -> MaterialTheme.colorScheme.onPrimaryContainer
                        isAlongside -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                if (isLocal) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "This Device",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Origin Badge (Device or App Fingerprint)
                val originLabel = version.originApp ?: version.originDevice
                if (!originLabel.isNullOrBlank() && !isLocal) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (originLabel.contains("Desktop", ignoreCase = true)) Icons.Default.Computer else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = originLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Primary Badge
                if (isPrimary && !isExclusiveRadio) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Primary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }

                // Alongside Badge
                if (isAlongside && !isExclusiveRadio) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        Text(
                            text = "Save Alongside",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$formattedDate • $formattedSize",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun mapSelectionToAction(group: FileConflictGroup, selected: Set<FileVersionItem>): ConflictResolutionAction {
    if (selected.isEmpty()) return ConflictResolutionAction.Skip

    if (selected.size == 1) {
        return ConflictResolutionAction.ChoosePrimary(selected.first())
    }

    val primary = selected.firstOrNull { it.source is FileVersionSource.LOCAL }
        ?: selected.maxByOrNull { it.lastModifiedEpochMs }
        ?: selected.first()

    val alongsideRemotes = selected.filter { it != primary && it.source is FileVersionSource.REMOTE }
    return ConflictResolutionAction.ResolveSelection(
        primaryVersion = primary,
        alongsideVersions = alongsideRemotes
    )
}
