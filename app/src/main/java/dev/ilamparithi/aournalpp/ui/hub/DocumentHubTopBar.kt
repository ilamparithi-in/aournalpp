package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.ui.cloud.QuickSyncButton
import dev.ilamparithi.aournalpp.ui.common.AppIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentHubTopBar(
    isSelectionMode: Boolean,
    isSearchActive: Boolean,
    isViewingTrash: Boolean,
    isSubfolder: Boolean,
    currentFolderItem: FolderItem?,
    currentDisplayNotes: List<NoteDocument>,
    selectedNotePaths: Set<String>,
    searchQuery: String,
    isGridView: Boolean,
    showHiddenFiles: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCloseSelection: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onInvertSelection: () -> Unit,
    onCloseSearch: () -> Unit,
    onBackClick: () -> Unit,
    onOpenSearch: () -> Unit,
    onToggleGridView: () -> Unit,
    onOpenTrash: () -> Unit,
    onQuickSyncMessage: (String) -> Unit,
    onStartSelectionMode: () -> Unit,
    onNewFolderClick: () -> Unit,
    onToggleShowHiddenFiles: () -> Unit,
    onEmptyTrashClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onTogglePinSubfolder: (() -> Unit)? = null,
    onToggleExcludeRecentsSubfolder: (() -> Unit)? = null,
    onRenameSubfolder: (() -> Unit)? = null,
    onMapToCloudSubfolder: (() -> Unit)? = null,
    onCustomizeSubfolder: (() -> Unit)? = null,
    onDeleteSubfolder: (() -> Unit)? = null
) {
    if (isSelectionMode) {
        val selectedDocs = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }
        val pdfCount = selectedDocs.count { it.fileType == NoteFileType.PDF }
        val noteCount = selectedDocs.size - pdfCount
        val summary = listOfNotNull(
            pluralStringResource(R.plurals.hub_selected_notes_count, noteCount, noteCount).takeIf { noteCount > 0 },
            pluralStringResource(R.plurals.hub_selected_pdfs_count, pdfCount, pdfCount).takeIf { pdfCount > 0 }
        ).joinToString(", ")

        TopAppBar(
            title = {
                Column {
                    Text(
                        text = pluralStringResource(
                            R.plurals.hub_selected_count,
                            selectedNotePaths.size,
                            selectedNotePaths.size
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (summary.isNotBlank()) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            },
            navigationIcon = {
                val cancelLabel = stringResource(R.string.action_cancel)
                AppIconButton(
                    onClick = onCloseSelection,
                    tooltip = cancelLabel
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = cancelLabel)
                }
            },
            actions = {
                val allSelected = selectedNotePaths.size == currentDisplayNotes.size && currentDisplayNotes.isNotEmpty()
                val selectAllLabel = if (allSelected) stringResource(R.string.action_deselect_all)
                else stringResource(R.string.action_select_all)

                AppIconButton(
                    onClick = onSelectAllToggle,
                    tooltip = selectAllLabel
                ) {
                    Icon(
                        imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = selectAllLabel
                    )
                }

                val invertLabel = stringResource(R.string.action_invert_selection)
                AppIconButton(
                    onClick = onInvertSelection,
                    tooltip = invertLabel
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = invertLabel
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    } else if (isSearchActive) {
        TopAppBar(
            title = {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.hub_search_hint)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            navigationIcon = {
                val backLabel = stringResource(R.string.action_back)
                AppIconButton(
                    onClick = onCloseSearch,
                    tooltip = backLabel
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel)
                }
            },
            actions = {
                if (searchQuery.isNotEmpty()) {
                    val clearLabel = stringResource(R.string.action_clear)
                    AppIconButton(
                        onClick = { onSearchQueryChange("") },
                        tooltip = clearLabel
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = clearLabel)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    } else {
        var showTopMenu by remember { mutableStateOf(false) }

        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isViewingTrash) stringResource(R.string.hub_menu_trash) else stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                val backLabel = stringResource(R.string.action_back)
                if (isViewingTrash || isSubfolder) {
                    AppIconButton(
                        onClick = onBackClick,
                        tooltip = backLabel
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel)
                    }
                }
            },
            actions = {
                if (!isViewingTrash) {
                    val searchLabel = stringResource(R.string.action_search)
                    AppIconButton(
                        onClick = onOpenSearch,
                        tooltip = searchLabel
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = searchLabel)
                    }

                    val viewModeLabel = if (isGridView) stringResource(R.string.home_view_mode_grid)
                    else stringResource(R.string.home_view_mode_collage)
                    AppIconButton(
                        onClick = onToggleGridView,
                        tooltip = viewModeLabel
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = viewModeLabel
                        )
                    }

                    val trashLabel = stringResource(R.string.hub_menu_trash)
                    AppIconButton(
                        onClick = onOpenTrash,
                        tooltip = trashLabel
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = trashLabel)
                    }
                }

                QuickSyncButton(onSyncFinished = onQuickSyncMessage)

                val detailsLabel = stringResource(R.string.action_details)
                AppIconButton(
                    onClick = { showTopMenu = true },
                    tooltip = detailsLabel
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = detailsLabel)
                }

                DropdownMenu(
                    expanded = showTopMenu,
                    onDismissRequest = { showTopMenu = false }
                ) {
                    if (!isViewingTrash) {
                        if (isSubfolder && currentFolderItem != null) {
                            val isPinned = currentFolderItem.isPinned
                            DropdownMenuItem(
                                text = { Text(if (isPinned) "Unpin Folder" else "Pin Folder") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    onTogglePinSubfolder?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (currentFolderItem.isExcludedFromRecents) "Include in Recents" else "Exclude from Recents") },
                                leadingIcon = {
                                    Icon(
                                        if (currentFolderItem.isExcludedFromRecents) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = if (currentFolderItem.isExcludedFromRecents) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    onToggleExcludeRecentsSubfolder?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename Folder") },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onRenameSubfolder?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Map to Cloud...") },
                                leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showTopMenu = false
                                    onMapToCloudSubfolder?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Customize Icon & Color") },
                                leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onCustomizeSubfolder?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showTopMenu = false
                                    onDeleteSubfolder?.invoke()
                                }
                            )
                            HorizontalDivider()
                        }

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hub_menu_select_notes)) },
                            leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                onStartSelectionMode()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hub_menu_new_folder)) },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                onNewFolderClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (showHiddenFiles) stringResource(R.string.hub_menu_hide_hidden)
                                    else stringResource(R.string.hub_menu_show_hidden)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showTopMenu = false
                                onToggleShowHiddenFiles()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hub_menu_trash)) },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                onOpenTrash()
                            }
                        )
                        HorizontalDivider()
                    } else {
                        if (currentDisplayNotes.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.hub_menu_select_notes)) },
                                leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onStartSelectionMode()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hub_menu_empty_trash), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showTopMenu = false
                                onEmptyTrashClick()
                            }
                        )
                        HorizontalDivider()
                    }

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pref_category_licenses)) },
                        leadingIcon = { Icon(Icons.Default.Gavel, contentDescription = null) },
                        onClick = {
                            showTopMenu = false
                            onNavigateToLicenses()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tab_settings)) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            showTopMenu = false
                            onNavigateToSettings()
                        }
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
