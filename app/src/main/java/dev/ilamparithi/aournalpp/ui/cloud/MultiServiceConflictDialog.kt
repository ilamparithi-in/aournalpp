package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.ui.util.AccessibilityUtils
import dev.ilamparithi.aournalpp.ui.util.a11yHeading
import dev.ilamparithi.aournalpp.ui.util.minTouchTarget
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionAction
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionReport
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.FileConflictResolution
import dev.ilamparithi.aournalpp.backup.model.FileVersionItem
import dev.ilamparithi.aournalpp.backup.model.FileVersionSource
import dev.ilamparithi.aournalpp.utils.FormatUtils
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

enum class ConflictDialogMode {
    NOTE_CONFLICT,
    CONFIG_CONFLICT
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiServiceConflictDialog(
    conflictGroups: List<FileConflictGroup>,
    engine: BackupEngine? = null,
    mode: ConflictDialogMode = ConflictDialogMode.NOTE_CONFLICT,
    title: String? = null,
    subtitle: String? = null,
    secondaryActionButton: @Composable (() -> Unit)? = null,
    onPreviewDiff: ((FileConflictGroup) -> Unit)? = null,
    initialSelections: Map<String, Set<FileVersionItem>>? = null,
    onDismissRequest: () -> Unit,
    onResolutionComplete: (ConflictResolutionReport) -> Unit = {},
    onApplyConfigResolutions: ((Map<String, FileVersionItem>) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var isResolving by remember { mutableStateOf(false) }

    // Map of conflictGroupId -> set of selected FileVersionItem
    val selectedVersionsMap = remember(conflictGroups, initialSelections) {
        val map = mutableStateMapOf<String, Set<FileVersionItem>>()
        for (group in conflictGroups) {
            val initial = initialSelections?.get(group.id)
            if (initial != null && initial.isNotEmpty()) {
                map[group.id] = initial
            } else {
                // Default selection: newest version checked as Primary Copy (or Cloud version for config)
                val newest = if (mode == ConflictDialogMode.CONFIG_CONFLICT) {
                    group.remoteVersions.firstOrNull() ?: group.allVersions.maxByOrNull { it.lastModifiedEpochMs }
                } else {
                    group.allVersions.maxByOrNull { it.lastModifiedEpochMs }
                }
                if (newest != null) {
                    map[group.id] = setOf(newest)
                } else {
                    map[group.id] = emptySet()
                }
            }
        }
        map
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val dialogWidth = if (isLandscape) 0.70f else 0.94f
    val dialogHeight = if (isLandscape) 0.92f else 0.85f

    Dialog(
        onDismissRequest = { if (!isResolving) onDismissRequest() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(dialogWidth)
                .fillMaxHeight(dialogHeight),
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
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title ?: (if (mode == ConflictDialogMode.CONFIG_CONFLICT) {
                                    stringResource(R.string.title_dialog_config_conflict)
                                } else {
                                    "File Sync Conflict"
                                }),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.a11yHeading()
                            )
                            Text(
                                text = subtitle ?: (if (mode == ConflictDialogMode.CONFIG_CONFLICT) {
                                    "${conflictGroups.size} configuration files differ between local storage and cloud"
                                } else {
                                    "${conflictGroups.size} notes have conflicting versions across storage endpoints"
                                }),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isResolving) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.minTouchTarget()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Batch Quick Action Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (mode == ConflictDialogMode.CONFIG_CONFLICT) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                for (group in conflictGroups) {
                                    val remote = group.remoteVersions.firstOrNull() ?: group.allVersions.firstOrNull()
                                    if (remote != null) selectedVersionsMap[group.id] = setOf(remote)
                                }
                            },
                            label = { Text("Keep All Cloud (Recommended)") },
                            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.minTouchTarget()
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                for (group in conflictGroups) {
                                    val local = group.localVersion ?: group.allVersions.firstOrNull()
                                    if (local != null) selectedVersionsMap[group.id] = setOf(local)
                                }
                            },
                            label = { Text("Keep All Local") },
                            leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.minTouchTarget()
                        )
                    } else {
                        FilterChip(
                            selected = false,
                            onClick = {
                                for (group in conflictGroups) {
                                    val newest = group.allVersions.maxByOrNull { it.lastModifiedEpochMs }
                                    selectedVersionsMap[group.id] = if (newest != null) setOf(newest) else emptySet()
                                }
                            },
                            label = { Text(stringResource(R.string.action_select_all_recent)) },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.minTouchTarget()
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                for (group in conflictGroups) {
                                    selectedVersionsMap[group.id] = group.allVersions.toSet()
                                }
                            },
                            label = { Text(stringResource(R.string.action_select_all_alongside)) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.minTouchTarget()
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                for (group in conflictGroups) {
                                    val local = group.localVersion
                                    selectedVersionsMap[group.id] = if (local != null) setOf(local) else emptySet()
                                }
                            },
                            label = { Text(stringResource(R.string.action_select_all_local)) },
                            leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.minTouchTarget()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Conflicts List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(conflictGroups, key = { it.id }) { group ->
                        ConflictMultiSelectionCard(
                            group = group,
                            mode = mode,
                            selectedVersions = selectedVersionsMap[group.id] ?: emptySet(),
                            onToggleVersion = { version ->
                                if (mode == ConflictDialogMode.CONFIG_CONFLICT) {
                                    selectedVersionsMap[group.id] = setOf(version)
                                } else {
                                    val current = selectedVersionsMap[group.id] ?: emptySet()
                                    val updated = if (current.contains(version)) {
                                        current - version
                                    } else {
                                        current + version
                                    }
                                    selectedVersionsMap[group.id] = updated
                                }
                            },
                            onPreviewDiff = onPreviewDiff
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        secondaryActionButton?.invoke()
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismissRequest,
                            enabled = !isResolving
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                if (mode == ConflictDialogMode.CONFIG_CONFLICT) {
                                    val map = conflictGroups.associate { g ->
                                        g.id to (selectedVersionsMap[g.id]?.firstOrNull() ?: g.allVersions.first())
                                    }
                                    onApplyConfigResolutions?.invoke(map)
                                } else if (engine != null) {
                                    isResolving = true
                                    coroutineScope.launch {
                                        val resolutions = conflictGroups.map { group ->
                                            val selected = selectedVersionsMap[group.id] ?: emptySet()
                                            val action = mapSelectionToAction(group, selected)
                                            FileConflictResolution(
                                                conflictGroupId = group.id,
                                                relativePath = group.relativePath,
                                                action = action
                                            )
                                        }
                                        val report = engine.resolveConflicts(resolutions)
                                        isResolving = false
                                        onResolutionComplete(report)
                                    }
                                }
                            },
                            enabled = !isResolving
                        ) {
                            if (isResolving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.msg_applying_resolutions))
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (mode == ConflictDialogMode.CONFIG_CONFLICT) "Apply Resolution"
                                    else stringResource(R.string.action_apply_resolution_count, conflictGroups.size)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Maps multiple selection state into standard ConflictResolutionAction.
 * - 0 checked -> Skip
 * - 1 checked -> ChoosePrimary
 * - >= 2 checked -> ResolveSelection (Primary is Local or first checked remote; other checked remotes saved alongside)
 */
private fun mapSelectionToAction(group: FileConflictGroup, selected: Set<FileVersionItem>): ConflictResolutionAction {
    if (selected.isEmpty()) return ConflictResolutionAction.Skip

    if (selected.size == 1) {
        return ConflictResolutionAction.ChoosePrimary(selected.first())
    }

    // Multiple selected:
    // Determine primary: if local is selected, local is primary; otherwise the newest selected remote is primary
    val primary = selected.firstOrNull { it.source is FileVersionSource.LOCAL }
        ?: selected.maxByOrNull { it.lastModifiedEpochMs }
        ?: selected.first()

    val alongsideRemotes = selected.filter { it != primary && it.source is FileVersionSource.REMOTE }
    return ConflictResolutionAction.ResolveSelection(
        primaryVersion = primary,
        alongsideVersions = alongsideRemotes
    )
}

@Composable
private fun ConflictMultiSelectionCard(
    group: FileConflictGroup,
    mode: ConflictDialogMode = ConflictDialogMode.NOTE_CONFLICT,
    selectedVersions: Set<FileVersionItem>,
    onToggleVersion: (FileVersionItem) -> Unit,
    onPreviewDiff: ((FileConflictGroup) -> Unit)? = null
) {
    // 1. Order list: Local file on TOP, Cloud services follow ordered from most recently modified to least recent
    val orderedVersions = remember(group) {
        val list = mutableListOf<FileVersionItem>()
        if (group.localVersion != null) {
            list.add(group.localVersion)
        }
        val sortedRemotes = group.remoteVersions.sortedByDescending { it.lastModifiedEpochMs }
        list.addAll(sortedRemotes)
        list
    }

    // Determine primary version in current selection
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
    val remoteVer = group.remoteVersions.firstOrNull()
    val relativeComparison = remember(localVer, remoteVer) {
        if (localVer != null && remoteVer != null) {
            FormatUtils.formatRelativeTimeDifference(
                localVer.lastModifiedEpochMs,
                remoteVer.lastModifiedEpochMs,
                "Local",
                "Cloud"
            )
        } else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge for current card selection
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
                            mode == ConflictDialogMode.CONFIG_CONFLICT -> {
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

            // Description for config files if available
            if (!group.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sub-row: Relative timestamp comparison badge & Diff Preview button
            if (relativeComparison != null || onPreviewDiff != null) {
                Spacer(modifier = Modifier.height(8.dp))
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

                    if (onPreviewDiff != null && (group.localFilePath != null || group.remoteFilePath != null)) {
                        TextButton(
                            onClick = { onPreviewDiff(group) },
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Difference,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_preview_diff),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selection Version List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                orderedVersions.forEach { version ->
                    val isChecked = selectedVersions.contains(version)
                    val isPrimary = isChecked && version == primaryVersion
                    val isAlongside = isChecked && !isPrimary

                    MultiSelectionVersionRow(
                        version = version,
                        isChecked = isChecked,
                        isPrimary = isPrimary,
                        isAlongside = isAlongside,
                        isExclusiveRadio = (mode == ConflictDialogMode.CONFIG_CONFLICT),
                        onToggle = { onToggleVersion(version) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiSelectionVersionRow(
    version: FileVersionItem,
    isChecked: Boolean,
    isPrimary: Boolean,
    isAlongside: Boolean,
    isExclusiveRadio: Boolean = false,
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

    val a11yVersionDesc = remember(version, isPrimary, isAlongside) {
        AccessibilityUtils.buildConflictVersionA11yDescription(
            sourceName = version.source.displayName,
            dateFormatted = formattedDate,
            sizeFormatted = formattedSize,
            isPrimary = isPrimary,
            isAlongside = isAlongside
        )
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
            .semantics(mergeDescendants = true) {
                role = if (isExclusiveRadio) Role.RadioButton else Role.Checkbox
                stateDescription = if (isChecked) "Selected" else "Not selected"
                this.contentDescription = a11yVersionDesc
            }
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isExclusiveRadio) {
            RadioButton(
                selected = isChecked,
                onClick = onToggle,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
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
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

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
                            text = "Local",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Outcome Badge for this specific row
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
                                text = "Primary Copy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else if (isAlongside) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Save Alongside",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Size: $formattedSize • Modified: $formattedDate",
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isPrimary -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    isAlongside -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}
