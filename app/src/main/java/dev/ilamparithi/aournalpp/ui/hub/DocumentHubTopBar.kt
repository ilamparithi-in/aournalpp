package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ViewList
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
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import dev.ilamparithi.aournalpp.ui.common.AppDropdownMenu
import dev.ilamparithi.aournalpp.ui.common.AppDropdownMenuItem
import dev.ilamparithi.aournalpp.ui.common.AppMenuDefaults
import dev.ilamparithi.aournalpp.ui.common.AppSearchBar
import dev.ilamparithi.aournalpp.ui.common.NoteFileTypeFilterRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.ui.cloud.QuickSyncButton
import dev.ilamparithi.aournalpp.ui.common.AppIconButton
import java.io.File

@Composable
fun DocumentHubTopBar(
    isSelectionMode: Boolean,
    isSearchActive: Boolean = false,
    isViewingTrash: Boolean,
    isSubfolder: Boolean,
    currentFolderItem: FolderItem?,
    currentDisplayNotes: List<NoteDocument>,
    currentDisplayFolders: List<FolderItem>,
    selectedNotePaths: Set<String>,
    selectedFolderPaths: Set<String>,
    searchQuery: String,
    isGridView: Boolean,
    showHiddenFiles: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCloseSelection: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onInvertSelection: () -> Unit,
    onCloseSearch: () -> Unit = {},
    onBackClick: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onToggleGridView: () -> Unit,
    onOpenTrash: () -> Unit,
    onQuickSyncMessage: (String) -> Unit,
    onStartSelectionMode: () -> Unit,
    onNewFolderClick: () -> Unit,
    onToggleShowHiddenFiles: () -> Unit,
    onEmptyTrashClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    enabledFileTypes: Set<NoteFileType> = emptySet(),
    onToggleFileType: (NoteFileType) -> Unit = {},
    currentDirectory: File = File(""),
    rootDirectory: File = File(""),
    onNavigateToFolder: (File) -> Unit = {},
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
        val folderCount = selectedFolderPaths.size

        val summaryList = mutableListOf<String>()
        if (folderCount > 0) summaryList.add(pluralStringResource(R.plurals.hub_selected_folders_count, folderCount, folderCount))
        if (noteCount > 0) summaryList.add(pluralStringResource(R.plurals.hub_selected_notes_count, noteCount, noteCount))
        if (pdfCount > 0) summaryList.add(pluralStringResource(R.plurals.hub_selected_pdfs_count, pdfCount, pdfCount))

        val summary = summaryList.joinToString(", ")
        val totalSelected = selectedNotePaths.size + selectedFolderPaths.size

        TopAppBar(
            title = {
                Column {
                    Text(
                        text = pluralStringResource(
                            R.plurals.hub_selected_count,
                            totalSelected,
                            totalSelected
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
                val allSelected = (selectedNotePaths.size == currentDisplayNotes.size && selectedFolderPaths.size == currentDisplayFolders.size) && (currentDisplayNotes.isNotEmpty() || currentDisplayFolders.isNotEmpty())
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
    } else if (isViewingTrash) {
        var showTopMenu by remember { mutableStateOf(false) }

        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.hub_menu_trash),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                val backLabel = stringResource(R.string.action_back)
                AppIconButton(
                    onClick = onBackClick,
                    tooltip = backLabel
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel)
                }
            },
            actions = {
                QuickSyncButton(onSyncFinished = onQuickSyncMessage)

                val detailsLabel = stringResource(R.string.action_details)
                AppIconButton(
                    onClick = { showTopMenu = true },
                    tooltip = detailsLabel
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = detailsLabel)
                }

                AppDropdownMenu(
                    expanded = showTopMenu,
                    onDismissRequest = { showTopMenu = false }
                ) {
                    if (currentDisplayNotes.isNotEmpty() || currentDisplayFolders.isNotEmpty()) {
                        AppDropdownMenuItem(
                            text = { Text(stringResource(R.string.hub_menu_select_notes)) },
                            leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                onStartSelectionMode()
                            }
                        )
                    }
                    AppDropdownMenuItem(
                        text = { Text(stringResource(R.string.hub_menu_empty_trash), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showTopMenu = false
                            onEmptyTrashClick()
                        }
                    )
                    AppMenuDefaults.InsetDivider()
                    AppDropdownMenuItem(
                        text = { Text(stringResource(R.string.pref_category_licenses)) },
                        leadingIcon = { Icon(Icons.Default.Gavel, contentDescription = null) },
                        onClick = {
                            showTopMenu = false
                            onNavigateToLicenses()
                        }
                    )
                    AppDropdownMenuItem(
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
    } else {
        // Normal notes browsing: Notes Home or Subfolder
        var showTopMenu by remember { mutableStateOf(false) }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWideScreen = maxWidth >= 720.dp
            val showSearchInline = isWideScreen && !isSubfolder

            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        if (isSubfolder) {
                            // "for the address bar that would be visible when going inside the folder, replace the top bar text with the address bar"
                            FolderBreadcrumbsBar(
                                currentDirectory = currentDirectory,
                                rootDirectory = rootDirectory,
                                onNavigateTo = onNavigateToFolder,
                                enabled = true,
                                showBackButton = false,
                                backgroundColor = Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (showSearchInline) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.title_notes_home),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                AppSearchBar(
                                    query = searchQuery,
                                    onQueryChange = onSearchQueryChange,
                                    placeholderText = stringResource(R.string.hub_search_hint),
                                    modifier = Modifier
                                        .weight(1f)
                                        .widthIn(max = 560.dp)
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.title_notes_home),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        if (isSubfolder) {
                            val backLabel = stringResource(R.string.action_back)
                            AppIconButton(
                                onClick = onBackClick,
                                tooltip = backLabel
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel)
                            }
                        }
                    },
                    actions = {
                        val viewModeLabel = if (isGridView) stringResource(R.string.hub_view_list)
                        else stringResource(R.string.hub_view_grid)
                        AppIconButton(
                            onClick = onToggleGridView,
                            tooltip = viewModeLabel
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
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

                        QuickSyncButton(onSyncFinished = onQuickSyncMessage)

                        val detailsLabel = stringResource(R.string.action_details)
                        AppIconButton(
                            onClick = { showTopMenu = true },
                            tooltip = detailsLabel
                        ) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = detailsLabel)
                        }

                        AppDropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            if (isSubfolder && currentFolderItem != null) {
                                val isPinned = currentFolderItem.isPinned
                                AppDropdownMenuItem(
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
                                AppDropdownMenuItem(
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
                                AppDropdownMenuItem(
                                    text = { Text("Rename Folder") },
                                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        onRenameSubfolder?.invoke()
                                    }
                                )
                                AppDropdownMenuItem(
                                    text = { Text("Map to Cloud...") },
                                    leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showTopMenu = false
                                        onMapToCloudSubfolder?.invoke()
                                    }
                                )
                                AppDropdownMenuItem(
                                    text = { Text("Customize Icon & Color") },
                                    leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        onCustomizeSubfolder?.invoke()
                                    }
                                )
                                AppDropdownMenuItem(
                                    text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showTopMenu = false
                                        onDeleteSubfolder?.invoke()
                                    }
                                )
                                AppMenuDefaults.InsetDivider()
                            }

                            AppDropdownMenuItem(
                                text = { Text(stringResource(R.string.hub_menu_select_notes)) },
                                leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onStartSelectionMode()
                                }
                            )
                            AppDropdownMenuItem(
                                text = { Text(stringResource(R.string.hub_menu_new_folder)) },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onNewFolderClick()
                                }
                            )
                            AppDropdownMenuItem(
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
                            AppDropdownMenuItem(
                                text = { Text(stringResource(R.string.hub_menu_trash)) },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onOpenTrash()
                                }
                            )
                            AppMenuDefaults.InsetDivider()

                            AppDropdownMenuItem(
                                text = { Text(stringResource(R.string.pref_category_licenses)) },
                                leadingIcon = { Icon(Icons.Default.Gavel, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onNavigateToLicenses()
                                }
                            )
                            AppDropdownMenuItem(
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

                // Line 2: Search bar below top bar (if not inline wide screen)
                if (!showSearchInline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppSearchBar(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            placeholderText = stringResource(R.string.hub_search_hint),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 640.dp)
                        )
                    }
                }

                // Line 3: In-place file type filter row for all folders in notes home
                if (enabledFileTypes.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NoteFileTypeFilterRow(
                            enabledFileTypes = enabledFileTypes,
                            onToggleFileType = onToggleFileType
                        )
                    }
                }
            }
        }
    }
}
