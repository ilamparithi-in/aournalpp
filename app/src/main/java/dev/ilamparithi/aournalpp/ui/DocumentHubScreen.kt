package dev.ilamparithi.aournalpp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.LicensesActivity
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.SettingsActivity
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.ui.cloud.CustomMappingDialog
import dev.ilamparithi.aournalpp.ui.dialog.AutosaveResolutionDialog
import dev.ilamparithi.aournalpp.ui.dialog.EmergencySaveNameDialog
import dev.ilamparithi.aournalpp.ui.hub.DocumentHubFab
import dev.ilamparithi.aournalpp.ui.hub.DocumentHubFabScrim
import dev.ilamparithi.aournalpp.ui.hub.DocumentHubSelectionBar
import dev.ilamparithi.aournalpp.ui.hub.DocumentHubTopBar
import dev.ilamparithi.aournalpp.ui.hub.DocumentHubViewModel
import dev.ilamparithi.aournalpp.ui.hub.DynamicRecentsCarousel
import dev.ilamparithi.aournalpp.ui.hub.ExpressiveNoteCard
import dev.ilamparithi.aournalpp.ui.hub.FolderCard
import dev.ilamparithi.aournalpp.ui.hub.dialog.AutoloadOverrideDialog
import dev.ilamparithi.aournalpp.ui.hub.dialog.BatchDeletePermanentDialog
import dev.ilamparithi.aournalpp.ui.hub.dialog.DeleteNoteDialog
import dev.ilamparithi.aournalpp.ui.hub.dialog.EditFolderAppearanceDialog
import dev.ilamparithi.aournalpp.ui.hub.dialog.EmptyTrashConfirmDialog
import dev.ilamparithi.aournalpp.ui.hub.dialog.EmergencyRecoveryDialog
import dev.ilamparithi.aournalpp.ui.hub.dialog.MoveToFolderDialog
import dev.ilamparithi.aournalpp.ui.hub.dialog.RenameFolderDialog
import dev.ilamparithi.aournalpp.ui.hub.dialog.RenameNoteDialog
import dev.ilamparithi.aournalpp.ui.hub.dialog.StoragePermissionPromptDialog
import dev.ilamparithi.aournalpp.ui.hub.notesGridDragSelect
import dev.ilamparithi.aournalpp.utils.ExternalFileHandler
import dev.ilamparithi.aournalpp.utils.FileNameTemplateEngine
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import dev.ilamparithi.aournalpp.utils.a11yHeading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SEARCH_DEBOUNCE_MS = 250L

@Volatile
private var cachedHasStoragePermission: Boolean? = null

private fun hasStoragePermission(context: Context): Boolean {
    cachedHasStoragePermission?.let { return it }
    val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val readGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        readGranted && writeGranted
    }
    if (granted) {
        cachedHasStoragePermission = true
    }
    return granted
}

private fun requestStoragePermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            context.startActivity(fallback)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentHubScreen(
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToLicenses: (() -> Unit)? = null,
    viewModel: DocumentHubViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val localView = LocalView.current

    val repository = viewModel.repository
    val pdfExportManager = viewModel.pdfExportManager

    // ViewModel states
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val trashedNotes by viewModel.trashedNotes.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState()
    val isViewingTrash by viewModel.isViewingTrash.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val showHiddenFiles by viewModel.showHiddenFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedNotePaths by viewModel.selectedNotePaths.collectAsState()
    val lastSelectedNotePath by viewModel.lastSelectedNotePath.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isPdfConverting by viewModel.isPdfConverting.collectAsState()
    val convertingMessage by viewModel.convertingMessage.collectAsState()
    val quarantinedEmergencySave by viewModel.quarantinedEmergencySave.collectAsState()
    val showEmergencyDialog by viewModel.showEmergencyDialog.collectAsState()
    val showAutoloadOverrideDialog by viewModel.showAutoloadOverrideDialog.collectAsState()

    var hasPermission by remember { mutableStateOf(hasStoragePermission(context)) }
    var showPermissionDialog by remember { mutableStateOf(!hasPermission) }

    // Dialog and UI interaction states
    var noteToRename by remember { mutableStateOf<NoteDocument?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteDocument?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<FolderItem?>(null) }
    var folderToRename by remember { mutableStateOf<FolderItem?>(null) }
    var showMoveToFolderDialog by remember { mutableStateOf(false) }
    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }
    var showBatchDeletePermanentDialog by remember { mutableStateOf(false) }
    var folderToMapToCloud by remember { mutableStateOf<FolderItem?>(null) }
    var showEmergencySaveNameDialog by remember { mutableStateOf(false) }
    var emergencySaveNameInput by remember { mutableStateOf("") }
    var emergencySaveTargetFolder by remember { mutableStateOf(repository.getRootNotesDirectory()) }
    var pendingAutosaveNote by remember { mutableStateOf<NoteDocument?>(null) }
    var pendingSaveAutosaveNote by remember { mutableStateOf<NoteDocument?>(null) }
    var noteForActionDialog by remember { mutableStateOf<File?>(null) }

    // Export states
    data class PendingSingleExport(
        val note: NoteDocument,
        val format: DocumentRepository.ShareExportFormat,
        val customName: String
    )
    var shareExportNote by remember { mutableStateOf<NoteDocument?>(null) }
    var pendingSingleExport by remember { mutableStateOf<PendingSingleExport?>(null) }
    var showBatchShareExportDialog by remember { mutableStateOf(false) }
    var pendingBatchExportFormat by remember { mutableStateOf<DocumentRepository.ShareExportFormat?>(null) }
    var pendingBatchExportDocs by remember { mutableStateOf<List<NoteDocument>>(emptyList()) }

    // Speed Dial FAB State
    var isFabExpanded by remember { mutableStateOf(false) }
    var showNewNoteDialog by remember { mutableStateOf(false) }
    var newNoteDefaultName by remember { mutableStateOf("") }
    var allFoldersForNewNote by remember { mutableStateOf<List<FolderItem>>(emptyList()) }

    // Drag-select states
    var isDragSelecting by remember { mutableStateOf(false) }
    var isInitialEntryDrag by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }

    fun showUndoSnackbar(message: String, onUndo: suspend () -> Unit) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                try {
                    onUndo()
                    viewModel.loadContentNow()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to undo: ${e.message}")
                }
            }
        }
    }

    fun handleNoteOpen(noteFile: File) {
        NoteOpenManager.handleFileOpen(
            context = context,
            file = noteFile,
            pdfExportManager = pdfExportManager,
            scope = scope,
            repository = repository,
            localView = localView,
            onShowPrompt = { noteForActionDialog = it },
            onConvertingState = { isConverting ->
                viewModel.setPdfConverting(
                    isConverting,
                    if (isConverting) "Rendering PDF for \"${noteFile.nameWithoutExtension}\"..." else ""
                )
            },
            onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
        )
    }

    // Launchers
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = hasStoragePermission(context)
    }

    val singleSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val pending = pendingSingleExport
        pendingSingleExport = null
        if (uri != null && pending != null) {
            viewModel.setPdfConverting(true, "Saving \"${pending.customName}\"...")
            scope.launch {
                val result = repository.exportDocumentToUri(
                    context = context,
                    doc = pending.note,
                    format = pending.format,
                    destUri = uri,
                    pdfExportManager = pdfExportManager
                )
                viewModel.setPdfConverting(false)
                if (result.isSuccess) {
                    val ext = if (pending.format == DocumentRepository.ShareExportFormat.PDF) "pdf" else "xopp"
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_exported_success, "${pending.customName}.$ext"))
                } else {
                    snackbarHostState.showSnackbar("Export failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    val batchSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val format = pendingBatchExportFormat
        val docs = pendingBatchExportDocs
        pendingBatchExportFormat = null
        pendingBatchExportDocs = emptyList()

        if (uri != null && format != null && docs.isNotEmpty()) {
            viewModel.setPdfConverting(true, "Saving ${docs.size} files to folder...")
            scope.launch {
                val result = repository.exportDocumentsToDirectory(
                    context = context,
                    docs = docs,
                    format = format,
                    treeUri = uri,
                    pdfExportManager = pdfExportManager,
                    onProgress = { current, total, name ->
                        viewModel.setPdfConverting(true, context.getString(R.string.msg_batch_exporting_progress, current, total, name))
                    }
                )
                viewModel.setPdfConverting(false)
                if (result.isSuccess) {
                    val count = result.getOrThrow()
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_batch_exported_success, count))
                    viewModel.setSelectionMode(false)
                } else {
                    snackbarHostState.showSnackbar("Batch export failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = ExternalFileHandler.importUriToDirectory(context, uri, currentDirectory)
                if (result.isSuccess) {
                    val imported = result.getOrThrow()
                    viewModel.loadContentNow()
                    val folderName = if (currentDirectory.canonicalPath == repository.getRootNotesDirectory().canonicalPath) "Notes" else currentDirectory.name
                    snackbarHostState.showSnackbar("Imported \"${imported.name}\" to $folderName")
                    handleNoteOpen(imported)
                } else {
                    snackbarHostState.showSnackbar("Failed to import file: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasStoragePermission(context)
                if (hasPermission) {
                    showPermissionDialog = false
                    scope.launch {
                        delay(250)
                        viewModel.loadContentNow()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentDirectory, showHiddenFiles, isViewingTrash, searchQuery) {
        if (!hasPermission) {
            hasPermission = hasStoragePermission(context)
            if (!hasPermission) return@LaunchedEffect
        }
        if (searchQuery.isNotEmpty()) {
            delay(SEARCH_DEBOUNCE_MS)
        } else {
            // Give the entering spring animation (duration ~300ms) uninterrupted UI thread time
            // before scanning disk and updating folders/notes state.
            delay(250)
        }
        viewModel.loadContentNow()
    }

    val isRootDirectory = currentDirectory.canonicalPath == repository.getRootNotesDirectory().canonicalPath
    val currentFolderItem = remember(currentDirectory, isRootDirectory) {
        if (!isRootDirectory) repository.getFolderItem(currentDirectory) else null
    }

    BackHandler(enabled = isSelectionMode || isViewingTrash || !isRootDirectory) {
        if (isSelectionMode) {
            viewModel.setSelectionMode(false)
        } else if (isViewingTrash) {
            viewModel.setViewingTrash(false)
        } else if (!isRootDirectory) {
            viewModel.setCurrentDirectory(currentDirectory.parentFile ?: repository.getRootNotesDirectory())
        }
    }

    val pullRefreshState = rememberPullToRefreshState()
    val currentDisplayNotes = if (isViewingTrash) trashedNotes else notes

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DocumentHubTopBar(
                isSelectionMode = isSelectionMode,
                isSearchActive = isSearchActive,
                isViewingTrash = isViewingTrash,
                isSubfolder = !isRootDirectory,
                currentFolderItem = currentFolderItem,
                currentDisplayNotes = currentDisplayNotes,
                selectedNotePaths = selectedNotePaths,
                searchQuery = searchQuery,
                isGridView = isGridView,
                showHiddenFiles = showHiddenFiles,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onCloseSelection = { viewModel.setSelectionMode(false) },
                onSelectAllToggle = {
                    val allSelected = selectedNotePaths.size == currentDisplayNotes.size && currentDisplayNotes.isNotEmpty()
                    viewModel.setSelectedNotePaths(if (allSelected) emptySet() else currentDisplayNotes.map { it.path }.toSet())
                },
                onInvertSelection = {
                    val allPaths = currentDisplayNotes.map { it.path }.toSet()
                    viewModel.setSelectedNotePaths(allPaths.minus(selectedNotePaths))
                },
                onCloseSearch = {
                    viewModel.setSearchActive(false)
                    viewModel.setSearchQuery("")
                },
                onBackClick = {
                    if (isViewingTrash) {
                        viewModel.setViewingTrash(false)
                        viewModel.loadContent()
                    } else if (!isRootDirectory) {
                        viewModel.setCurrentDirectory(currentDirectory.parentFile ?: repository.getRootNotesDirectory())
                        viewModel.loadContent()
                    }
                },
                onOpenSearch = { viewModel.setSearchActive(true) },
                onToggleGridView = { viewModel.toggleGridView() },
                onOpenTrash = {
                    viewModel.setViewingTrash(true)
                    viewModel.loadContent()
                },
                onQuickSyncMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                onStartSelectionMode = { viewModel.setSelectionMode(true) },
                onNewFolderClick = { showNewFolderDialog = true },
                onToggleShowHiddenFiles = { viewModel.toggleShowHiddenFiles() },
                onEmptyTrashClick = { showEmptyTrashConfirmDialog = true },
                onNavigateToSettings = {
                    if (onNavigateToSettings != null) onNavigateToSettings()
                    else context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                onNavigateToLicenses = {
                    if (onNavigateToLicenses != null) onNavigateToLicenses()
                    else context.startActivity(Intent(context, LicensesActivity::class.java))
                },
                onTogglePinSubfolder = currentFolderItem?.let { folder ->
                    {
                        val nowPinned = repository.togglePinFolder(folder)
                        viewModel.loadContent()
                        showUndoSnackbar(if (nowPinned) "Pinned \"${folder.name}\"" else "Unpinned \"${folder.name}\"") {
                            repository.togglePinFolder(folder)
                        }
                    }
                },
                onToggleExcludeRecentsSubfolder = currentFolderItem?.let { folder ->
                    {
                        val newExcluded = !folder.isExcludedFromRecents
                        repository.setFolderExcludeFromRecents(folder.file, newExcluded)
                        viewModel.loadContent()
                        showUndoSnackbar(if (newExcluded) "Excluded \"${folder.name}\" from Recents" else "Included \"${folder.name}\" in Recents") {
                            repository.setFolderExcludeFromRecents(folder.file, !newExcluded)
                        }
                    }
                },
                onRenameSubfolder = currentFolderItem?.let { folder ->
                    { folderToRename = folder }
                },
                onMapToCloudSubfolder = currentFolderItem?.let { folder ->
                    { folderToMapToCloud = folder }
                },
                onCustomizeSubfolder = currentFolderItem?.let { folder ->
                    { folderToEdit = folder }
                },
                onDeleteSubfolder = currentFolderItem?.let { folder ->
                    {
                        val parentDir = currentDirectory.parentFile ?: repository.getRootNotesDirectory()
                        viewModel.setCurrentDirectory(parentDir)
                        scope.launch {
                            val res = repository.moveFolderToTrash(folder.file)
                            viewModel.loadContentNow()
                            if (res.isSuccess) {
                                val trashName = res.getOrNull()
                                showUndoSnackbar("Moved folder \"${folder.name}\" to Trash") {
                                    trashName?.let {
                                        val restored = repository.restoreFolderFromTrash(it).getOrNull()
                                        if (restored != null) viewModel.setCurrentDirectory(restored)
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isViewingTrash && !isSelectionMode) {
                DocumentHubFab(
                    isExpanded = isFabExpanded,
                    onToggleExpanded = { isFabExpanded = !isFabExpanded },
                    onCreateFolderClick = {
                        isFabExpanded = false
                        showNewFolderDialog = true
                    },
                    onOpenFileClick = {
                        isFabExpanded = false
                        importFileLauncher.launch(arrayOf("*/*", "application/pdf", "application/x-xopp", "application/x-xoj", "application/octet-stream"))
                    },
                    onCreateNoteClick = {
                        isFabExpanded = false
                        if (!hasPermission) {
                            showPermissionDialog = true
                        } else {
                            newNoteDefaultName = FileNameTemplateEngine.evaluate(
                                FileNameTemplateEngine.getNewFileTemplate(context),
                                context
                            )
                            scope.launch {
                                allFoldersForNewNote = withContext(Dispatchers.IO) { repository.getAllFolders() }
                            }
                            showNewNoteDialog = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Breadcrumbs
                    if (!isRootDirectory && !isViewingTrash) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Notes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.setCurrentDirectory(repository.getRootNotesDirectory())
                                    }
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(horizontal = 2.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                if (!currentFolderItem?.iconEmoji.isNullOrBlank()) {
                                    Text(text = currentFolderItem!!.iconEmoji!!, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else if (currentFolderItem?.iconType == "emergency" || repository.isEmergencySavesFolder(currentDirectory)) {
                                    Icon(
                                        imageVector = Icons.Default.Emergency,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = currentFolderItem?.colorHex?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null } } ?: MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = currentDirectory.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Main Content Grid
                    AnimatedContent(
                        targetState = currentDirectory.canonicalPath to isViewingTrash,
                        transitionSpec = {
                            if (targetState.first.length > initialState.first.length) {
                                (slideInHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f), initialOffsetX = { it / 3 }) + fadeIn())
                                    .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f), targetOffsetX = { -it / 3 }) + fadeOut())
                            } else {
                                (slideInHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f), initialOffsetX = { -it / 3 }) + fadeIn())
                                    .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f), targetOffsetX = { it / 3 }) + fadeOut())
                            }
                        },
                        label = "folderNavigationTransition",
                        modifier = Modifier.weight(1f)
                    ) { (targetPath, targetIsTrash) ->
                        val displayFolders = remember(targetPath, targetIsTrash, folders) {
                            if (targetIsTrash) emptyList()
                            else if (targetPath == currentDirectory.canonicalPath) folders
                            else repository.getCachedDirectory(File(targetPath), searchQuery, showHiddenFiles)?.first ?: emptyList()
                        }
                        val displayNotes = remember(targetPath, targetIsTrash, notes, trashedNotes) {
                            if (targetIsTrash) trashedNotes
                            else if (targetPath == currentDirectory.canonicalPath) notes
                            else repository.getCachedDirectory(File(targetPath), searchQuery, showHiddenFiles)?.second ?: emptyList()
                        }

                        val pageGridState = rememberSaveable(targetPath, saver = LazyGridState.Saver) { LazyGridState() }
                        val isPageRoot = targetPath == repository.getRootNotesDirectory().canonicalPath

                        LaunchedEffect(autoScrollSpeed) {
                            if (autoScrollSpeed != 0f) {
                                while (isActive) {
                                    pageGridState.scrollBy(autoScrollSpeed)
                                    delay(10)
                                }
                            }
                        }

                        LazyVerticalGrid(
                            state = pageGridState,
                            columns = if (isGridView) GridCells.Adaptive(minSize = 200.dp) else GridCells.Fixed(1),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .notesGridDragSelect(
                                    lazyGridState = pageGridState,
                                    notes = { displayNotes },
                                    selectedPaths = { selectedNotePaths },
                                    setSelectedPaths = { viewModel.setSelectedNotePaths(it) },
                                    lastSelectedPath = { lastSelectedNotePath },
                                    setLastSelectedPath = { viewModel.setLastSelectedNotePath(it) },
                                    isSelectionMode = { isSelectionMode },
                                    setIsSelectionMode = { viewModel.setSelectionMode(it) },
                                    setIsDragSelecting = { isDragSelecting = it },
                                    setIsInitialEntryDrag = { isInitialEntryDrag = it },
                                    hapticFeedback = hapticFeedback,
                                    autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() },
                                    setAutoScrollSpeed = { autoScrollSpeed = it }
                                )
                        ) {
                            // Dynamic Recents Carousel
                            if (isPageRoot && !targetIsTrash && searchQuery.isBlank() && recentNotes.isNotEmpty() && !isSelectionMode) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "recents_carousel_section") {
                                    DynamicRecentsCarousel(
                                        recentNotes = recentNotes,
                                        pdfExportManager = pdfExportManager,
                                        onOpenNote = { note ->
                                            if (note.autosaveInfo != null) pendingAutosaveNote = note
                                            else handleNoteOpen(note.file)
                                        },
                                        onTogglePin = { note ->
                                            repository.togglePinNote(note.file.absolutePath)
                                            viewModel.loadContent()
                                        },
                                        onShareExport = { note -> shareExportNote = note },
                                        onDuplicate = { note ->
                                            scope.launch {
                                                val result = repository.duplicateNote(note)
                                                if (result.isSuccess) {
                                                    val duplicated = result.getOrNull()
                                                    viewModel.loadContent()
                                                    showUndoSnackbar("Duplicated note \"${note.title}\"") { duplicated?.delete() }
                                                } else {
                                                    snackbarHostState.showSnackbar("Failed to duplicate note: ${result.exceptionOrNull()?.message}")
                                                }
                                            }
                                        },
                                        onDeleteNote = { note -> noteToDelete = note },
                                        onRenameNote = { note -> noteToRename = note }
                                    )
                                }
                            }

                            // Subfolders
                            if (displayFolders.isNotEmpty() && !targetIsTrash) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = pluralStringResource(R.plurals.hub_section_folders_count, displayFolders.size, displayFolders.size),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.a11yHeading()
                                    )
                                }

                                items(displayFolders, key = { it.file.absolutePath }) { folder ->
                                    FolderCard(
                                        folder = folder,
                                        onClick = { viewModel.setCurrentDirectory(folder.file) },
                                        onTogglePin = {
                                            val nowPinned = repository.togglePinFolder(folder)
                                            viewModel.loadContent()
                                            showUndoSnackbar(if (nowPinned) "Pinned \"${folder.name}\"" else "Unpinned \"${folder.name}\"") {
                                                repository.togglePinFolder(folder)
                                            }
                                        },
                                        onToggleExcludeRecents = {
                                            val newExcluded = !folder.isExcludedFromRecents
                                            repository.setFolderExcludeFromRecents(folder.file, newExcluded)
                                            viewModel.loadContent()
                                            showUndoSnackbar(if (newExcluded) "Excluded \"${folder.name}\" from Recents" else "Included \"${folder.name}\" in Recents") {
                                                repository.setFolderExcludeFromRecents(folder.file, !newExcluded)
                                            }
                                        },
                                        onRename = { folderToRename = folder },
                                        onMapToCloud = { folderToMapToCloud = folder },
                                        onCustomize = { folderToEdit = folder },
                                        onDelete = {
                                            scope.launch {
                                                val res = repository.moveFolderToTrash(folder.file)
                                                viewModel.loadContentNow()
                                                if (res.isSuccess) {
                                                    val trashName = res.getOrNull()
                                                    showUndoSnackbar("Moved folder \"${folder.name}\" to Trash") {
                                                        trashName?.let { repository.restoreFolderFromTrash(it) }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            // Notes Section Header
                            if (displayFolders.isNotEmpty() && !targetIsTrash) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = if (targetIsTrash) "Trashed Notes (${displayNotes.size})" else pluralStringResource(R.plurals.hub_section_notes_count, displayNotes.size, displayNotes.size),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.a11yHeading()
                                    )
                                }
                            }

                            // Note Cards
                            if (displayNotes.isNotEmpty()) {
                                items(displayNotes, key = { it.path }) { note ->
                                    val isSelected = selectedNotePaths.contains(note.path)

                                    ExpressiveNoteCard(
                                        note = note,
                                        isGridView = isGridView,
                                        isSelected = isSelected,
                                        isSelectionMode = isSelectionMode,
                                        isTrashMode = isViewingTrash,
                                        pdfExportManager = pdfExportManager,
                                        onClick = {
                                            if (isSelectionMode) {
                                                val updated = if (isSelected) selectedNotePaths.minus(note.path) else selectedNotePaths.plus(note.path)
                                                viewModel.setSelectedNotePaths(updated)
                                                viewModel.setLastSelectedNotePath(note.path)
                                            } else if (!isViewingTrash) {
                                                if (!hasPermission) showPermissionDialog = true
                                                else if (note.autosaveInfo != null) pendingAutosaveNote = note
                                                else handleNoteOpen(note.file)
                                            }
                                        },
                                        onLongClick = {
                                            if (isSelectionMode && !isSelected && selectedNotePaths.isNotEmpty()) {
                                                val lastPath = lastSelectedNotePath ?: selectedNotePaths.lastOrNull()
                                                val currentList = displayNotes.map { it.path }
                                                val lastIdx = currentList.indexOf(lastPath)
                                                val currentIdx = currentList.indexOf(note.path)
                                                if (lastIdx >= 0 && currentIdx >= 0) {
                                                    val start = minOf(lastIdx, currentIdx)
                                                    val end = maxOf(lastIdx, currentIdx)
                                                    val range = displayNotes.subList(start, end + 1).map { it.path }.toSet()
                                                    viewModel.setSelectedNotePaths(selectedNotePaths + range)
                                                } else {
                                                    viewModel.setSelectedNotePaths(selectedNotePaths + note.path)
                                                }
                                            } else {
                                                viewModel.setSelectionMode(true)
                                                viewModel.setSelectedNotePaths(selectedNotePaths + note.path)
                                            }
                                            viewModel.setLastSelectedNotePath(note.path)
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onTogglePin = {
                                            repository.togglePinNote(note.file.absolutePath)
                                            viewModel.loadContent()
                                        },
                                        onShareExport = { shareExportNote = note },
                                        onRename = { noteToRename = note },
                                        onDuplicate = {
                                            scope.launch {
                                                val result = repository.duplicateNote(note)
                                                if (result.isSuccess) {
                                                    val duplicated = result.getOrNull()
                                                    viewModel.loadContentNow()
                                                    showUndoSnackbar("Duplicated \"${note.title}\"") { duplicated?.delete() }
                                                }
                                            }
                                        },
                                        onDelete = { noteToDelete = note },
                                        onRestore = {
                                            scope.launch {
                                                val res = repository.restoreFromTrash(note)
                                                if (res.isSuccess) {
                                                    viewModel.loadContentNow()
                                                    snackbarHostState.showSnackbar("Restored \"${note.title}\"")
                                                }
                                            }
                                        }
                                    )
                                }
                            } else if (displayFolders.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isViewingTrash) Icons.Default.DeleteSweep else Icons.Default.Description,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = if (isViewingTrash) stringResource(R.string.hub_trash_empty_title)
                                                else stringResource(R.string.hub_empty_state_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isViewingTrash) stringResource(R.string.hub_trash_empty_subtitle)
                                                else stringResource(R.string.hub_empty_state_subtitle),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selection Floating Bottom Bar
                    DocumentHubSelectionBar(
                        isVisible = isSelectionMode,
                        isViewingTrash = isViewingTrash,
                        selectedDocs = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) },
                        onRestoreSelected = {
                            scope.launch {
                                val selected = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }
                                val count = repository.restoreMultipleFromTrash(selected).getOrDefault(0)
                                viewModel.setSelectionMode(false)
                                viewModel.loadContentNow()
                                snackbarHostState.showSnackbar(
                                    context.resources.getQuantityString(R.plurals.msg_restored_items_from_trash, count, count)
                                )
                            }
                        },
                        onDeletePermanentlySelected = { showBatchDeletePermanentDialog = true },
                        onTogglePinSelected = {
                            val selected = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }
                            val allSelectedPinned = selected.isNotEmpty() && selected.all { it.isPinned }
                            selected.forEach { doc ->
                                if (allSelectedPinned) repository.unpinNote(doc.file.absolutePath)
                                else repository.pinNote(doc.file.absolutePath)
                            }
                            viewModel.loadContent()
                            showUndoSnackbar(if (allSelectedPinned) "Unpinned selected notes" else "Pinned selected notes") {
                                selected.forEach { doc ->
                                    if (allSelectedPinned) repository.pinNote(doc.file.absolutePath)
                                    else repository.unpinNote(doc.file.absolutePath)
                                }
                            }
                        },
                        onMoveToFolderSelected = { showMoveToFolderDialog = true },
                        onShareExportSelected = {
                            val selected = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }
                            if (selected.size == 1) shareExportNote = selected.first()
                            else if (selected.size > 1) showBatchShareExportDialog = true
                        },
                        onMoveToTrashSelected = {
                            scope.launch {
                                val selected = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }
                                val res = repository.moveToTrash(selected)
                                viewModel.setSelectionMode(false)
                                viewModel.loadContentNow()
                                if (res.isSuccess) {
                                    val count = res.getOrNull()?.movedCount ?: 0
                                    val receipt = res.getOrNull()
                                    showUndoSnackbar(context.resources.getQuantityString(R.plurals.msg_moved_notes_to_trash, count, count)) {
                                        receipt?.let { repository.restoreTrashItems(it.trashFileNames) }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Scrim for Speed Dial
            DocumentHubFabScrim(
                isExpanded = isFabExpanded,
                onDismiss = { isFabExpanded = false }
            )
        }

        // Dialogs
        if (showPermissionDialog && !hasPermission) {
            StoragePermissionPromptDialog(
                onDismiss = { showPermissionDialog = false },
                onRequestPermission = {
                    showPermissionDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        requestStoragePermission(context)
                    } else {
                        legacyPermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        )
                    }
                }
            )
        }

        if (isPdfConverting) {
            PdfConversionProgressDialog(message = convertingMessage)
        }

        if (showNewFolderDialog) {
            val isRoot = currentDirectory.canonicalPath == repository.getRootNotesDirectory().canonicalPath
            CreateFolderDialog(
                parentFolder = currentDirectory,
                title = if (isRoot) stringResource(R.string.dialog_new_folder_title) else "New Folder in \"${currentDirectory.name}\"",
                confirmButtonLabel = stringResource(R.string.dialog_create_button),
                onDismiss = { showNewFolderDialog = false },
                onCreate = { name, colorHex, iconEmoji, iconType ->
                    showNewFolderDialog = false
                    scope.launch {
                        val result = repository.createFolder(currentDirectory, name, colorHex, iconEmoji, iconType)
                        if (result.isSuccess) {
                            viewModel.loadContentNow()
                            snackbarHostState.showSnackbar("Created folder \"$name\"")
                        } else {
                            snackbarHostState.showSnackbar("Failed to create folder: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            )
        }

        folderToEdit?.let { folder ->
            EditFolderAppearanceDialog(
                folder = folder,
                onDismiss = { folderToEdit = null },
                onSave = { color, emoji, iconType ->
                    folderToEdit = null
                    repository.updateFolderMeta(folder.file, color, emoji, iconType)
                    viewModel.loadContent()
                }
            )
        }

        folderToRename?.let { folder ->
            RenameFolderDialog(
                folder = folder,
                onDismiss = { folderToRename = null },
                onRename = { newName ->
                    folderToRename = null
                    val oldName = folder.name
                    val wasCurrentDir = currentDirectory.canonicalPath == folder.file.canonicalPath
                    scope.launch {
                        val result = repository.renameFolder(folder.file, newName)
                        if (result.isSuccess) {
                            val renamedDir = result.getOrNull()
                            if (wasCurrentDir && renamedDir != null) {
                                viewModel.setCurrentDirectory(renamedDir)
                            }
                            viewModel.loadContentNow()
                            showUndoSnackbar("Renamed folder to \"$newName\"") {
                                renamedDir?.let {
                                    val undoResult = repository.renameFolder(it, oldName)
                                    if (wasCurrentDir && undoResult.isSuccess) {
                                        undoResult.getOrNull()?.let { viewModel.setCurrentDirectory(it) }
                                    }
                                }
                            }
                        } else {
                            snackbarHostState.showSnackbar("Failed to rename: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            )
        }

        folderToMapToCloud?.let { folder ->
            val vault = remember { dev.ilamparithi.aournalpp.backup.security.CredentialsVault.getInstance(context) }
            val services = remember { vault.getAllServices() }
            CustomMappingDialog(
                services = services,
                initialLocalPath = folder.file.absolutePath,
                onDismissRequest = { folderToMapToCloud = null },
                onSaveMapping = { targetServiceId, mapping ->
                    val srv = services.firstOrNull { it.id == targetServiceId }
                    if (srv != null) {
                        val updatedMappings = srv.customMappings.filterNot { it.id == mapping.id } + mapping
                        vault.saveService(srv.copy(customMappings = updatedMappings))
                        scope.launch {
                            snackbarHostState.showSnackbar("Mapped \"${folder.name}\" to \"${mapping.remoteFolderPath}\" on ${srv.name}")
                        }
                    }
                    folderToMapToCloud = null
                }
            )
        }

        if (showMoveToFolderDialog) {
            val selectedDocs = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }
            val allAvailableFolders by produceState<List<FolderItem>>(emptyList()) { value = repository.getAllFolders() }

            MoveToFolderDialog(
                selectedDocs = selectedDocs,
                availableFolders = allAvailableFolders,
                onDismiss = { showMoveToFolderDialog = false },
                onMoveToRoot = {
                    val rootDir = repository.getRootNotesDirectory()
                    val origFolders = selectedDocs.map { it.file.name to it.file.parentFile }
                    scope.launch {
                        val count = repository.moveNotesToFolder(selectedDocs, rootDir).getOrDefault(0)
                        showMoveToFolderDialog = false
                        viewModel.setSelectionMode(false)
                        viewModel.loadContentNow()
                        showUndoSnackbar(context.resources.getQuantityString(R.plurals.msg_moved_notes_to_root, count, count)) {
                            for ((name, origDir) in origFolders) {
                                if (origDir != null) {
                                    val currentFile = File(rootDir, name)
                                    if (currentFile.exists()) currentFile.renameTo(File(origDir, name))
                                }
                            }
                        }
                    }
                },
                onMoveToFolder = { destFolder ->
                    val origFolders = selectedDocs.map { it.file.name to it.file.parentFile }
                    scope.launch {
                        val count = repository.moveNotesToFolder(selectedDocs, destFolder.file).getOrDefault(0)
                        showMoveToFolderDialog = false
                        viewModel.setSelectionMode(false)
                        viewModel.loadContentNow()
                        showUndoSnackbar(context.resources.getQuantityString(R.plurals.msg_moved_notes_to_folder, count, count, destFolder.name)) {
                            for ((name, origDir) in origFolders) {
                                if (origDir != null) {
                                    val currentFile = File(destFolder.file, name)
                                    if (currentFile.exists()) currentFile.renameTo(File(origDir, name))
                                }
                            }
                        }
                    }
                },
                onCreateInlineFolderAndMove = { inlineName, inlineColor ->
                    val created = repository.createFolder(repository.getRootNotesDirectory(), inlineName, inlineColor)
                    if (created.isSuccess) {
                        val dest = created.getOrThrow()
                        val origFolders = selectedDocs.map { it.file.name to it.file.parentFile }
                        scope.launch {
                            val count = repository.moveNotesToFolder(selectedDocs, dest).getOrDefault(0)
                            showMoveToFolderDialog = false
                            viewModel.setSelectionMode(false)
                            viewModel.loadContentNow()
                            showUndoSnackbar(context.resources.getQuantityString(R.plurals.msg_moved_notes_to_folder, count, count, dest.name)) {
                                for ((name, origDir) in origFolders) {
                                    if (origDir != null) {
                                        val currentFile = File(dest, name)
                                        if (currentFile.exists()) currentFile.renameTo(File(origDir, name))
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        noteToRename?.let { doc ->
            RenameNoteDialog(
                note = doc,
                onDismiss = { noteToRename = null },
                onRename = { newTitle ->
                    noteToRename = null
                    scope.launch {
                        val oldTitle = doc.title
                        val result = repository.renameNote(doc, newTitle)
                        if (result.isSuccess) {
                            val renamedFile = result.getOrNull()
                            viewModel.loadContentNow()
                            showUndoSnackbar("Renamed note successfully") {
                                renamedFile?.let {
                                    repository.renameNote(doc.copy(file = it, title = it.nameWithoutExtension), oldTitle)
                                }
                            }
                        } else {
                            snackbarHostState.showSnackbar("Rename failed: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            )
        }

        noteToDelete?.let { doc ->
            DeleteNoteDialog(
                note = doc,
                isViewingTrash = isViewingTrash,
                onDismiss = { noteToDelete = null },
                onConfirmDelete = {
                    noteToDelete = null
                    scope.launch {
                        if (isViewingTrash) {
                            repository.deletePermanently(listOf(doc))
                            viewModel.loadContentNow()
                            snackbarHostState.showSnackbar("Permanently deleted \"${doc.title}\"")
                        } else {
                            val result = repository.moveToTrash(listOf(doc))
                            viewModel.loadContentNow()
                            if (result.isSuccess) {
                                val receipt = result.getOrNull()
                                showUndoSnackbar("Moved \"${doc.title}\" to Trash") {
                                    receipt?.let { repository.restoreTrashItems(it.trashFileNames) }
                                }
                            }
                        }
                    }
                }
            )
        }

        if (showBatchDeletePermanentDialog) {
            val selectedDocs = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }
            BatchDeletePermanentDialog(
                selectedCount = selectedDocs.size,
                onDismiss = { showBatchDeletePermanentDialog = false },
                onConfirmDelete = {
                    showBatchDeletePermanentDialog = false
                    scope.launch {
                        val count = repository.deletePermanently(selectedDocs).getOrDefault(0)
                        viewModel.setSelectionMode(false)
                        viewModel.loadContentNow()
                        snackbarHostState.showSnackbar(
                            context.resources.getQuantityString(R.plurals.msg_permanently_deleted_items, count, count)
                        )
                    }
                }
            )
        }

        if (showEmptyTrashConfirmDialog) {
            EmptyTrashConfirmDialog(
                onDismiss = { showEmptyTrashConfirmDialog = false },
                onConfirmEmpty = {
                    showEmptyTrashConfirmDialog = false
                    scope.launch {
                        repository.emptyTrash()
                        viewModel.loadContentNow()
                        snackbarHostState.showSnackbar("Emptied Trash")
                    }
                }
            )
        }

        if (showEmergencyDialog && quarantinedEmergencySave != null) {
            val file = quarantinedEmergencySave!!
            EmergencyRecoveryDialog(
                emergencyFile = file,
                onDismiss = { viewModel.dismissEmergencyDialog() },
                onOpenNow = {
                    viewModel.dismissEmergencyDialog()
                    val staged = repository.openEmergencyRecoverySession(file)
                    viewModel.clearQuarantinedEmergencySave()
                    viewModel.loadContent()
                    handleNoteOpen(staged)
                },
                onSaveAsNote = {
                    viewModel.dismissEmergencyDialog()
                    emergencySaveNameInput = "Recovered_Note_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(file.lastModified()))
                    emergencySaveTargetFolder = currentDirectory
                    showEmergencySaveNameDialog = true
                },
                onDiscard = {
                    viewModel.dismissEmergencyDialog()
                    repository.discardEmergencyRecovery()
                    viewModel.clearQuarantinedEmergencySave()
                    viewModel.loadContent()
                }
            )
        }

        if (showAutoloadOverrideDialog) {
            AutoloadOverrideDialog(onDismiss = { viewModel.dismissAutoloadOverrideDialog() })
        }

        if (showEmergencySaveNameDialog && quarantinedEmergencySave != null) {
            EmergencySaveNameDialog(
                file = quarantinedEmergencySave!!,
                initialName = emergencySaveNameInput,
                initialFolder = emergencySaveTargetFolder,
                repository = repository,
                onDismiss = { showEmergencySaveNameDialog = false },
                onSaveSuccess = { savedFile ->
                    showEmergencySaveNameDialog = false
                    viewModel.clearQuarantinedEmergencySave()
                    viewModel.loadContent()
                    scope.launch { snackbarHostState.showSnackbar("Saved recovered note as \"${savedFile.name}\"") }
                },
                onFolderCreated = { viewModel.loadContent() }
            )
        }

        pendingAutosaveNote?.let { note ->
            val autoInfo = note.autosaveInfo
            if (autoInfo != null) {
                AutosaveResolutionDialog(
                    note = note,
                    autosaveInfo = autoInfo,
                    onDismiss = { pendingAutosaveNote = null },
                    onReplaceWithAutosave = {
                        val target = repository.replaceWithAutosave(note)
                        pendingAutosaveNote = null
                        viewModel.loadContent()
                        handleNoteOpen(target)
                    },
                    onKeepBoth = {
                        pendingAutosaveNote = null
                        pendingSaveAutosaveNote = note
                    },
                    onKeepExisting = {
                        val target = repository.discardAutosave(note)
                        pendingAutosaveNote = null
                        viewModel.loadContent()
                        handleNoteOpen(target)
                    }
                )
            }
        }

        pendingSaveAutosaveNote?.let { note ->
            val autoInfo = note.autosaveInfo
            if (autoInfo != null) {
                val allAvailableFolders by produceState<List<FolderItem>>(emptyList(), pendingSaveAutosaveNote) {
                    value = repository.getAllFolders()
                }

                SaveAsNoteDialog(
                    title = "Save Autosave as Note",
                    subtitle = "Save a separate copy of the autosaved version with your chosen name and folder.",
                    icon = Icons.Default.Description,
                    initialName = "${note.title} (Autosave)",
                    initialFolder = note.file.parentFile ?: repository.getRootNotesDirectory(),
                    availableFolders = allAvailableFolders,
                    rootFolder = repository.getRootNotesDirectory(),
                    onDismiss = { pendingSaveAutosaveNote = null },
                    onSave = { name, targetFolder ->
                        val savedFile = repository.saveAutosaveAsNote(autoInfo, name, targetFolder)
                        pendingSaveAutosaveNote = null
                        viewModel.loadContent()
                        handleNoteOpen(note.file)
                        scope.launch { snackbarHostState.showSnackbar("Saved autosave copy as \"${savedFile.name}\"") }
                    },
                    onCreateFolder = { name, colorHex, iconEmoji, iconType ->
                        val result = repository.createFolder(
                            parentDir = repository.getRootNotesDirectory(),
                            name = name,
                            colorHex = colorHex,
                            iconEmoji = iconEmoji,
                            iconType = iconType
                        )
                        if (result.isSuccess) viewModel.loadContent()
                        result
                    }
                )
            }
        }

        noteForActionDialog?.let { file ->
            NoteOpenActionDialog(
                file = file,
                onDismiss = { noteForActionDialog = null },
                onViewAsPdf = {
                    noteForActionDialog = null
                    NoteOpenManager.openAsPdf(
                        context = context,
                        file = file,
                        pdfExportManager = pdfExportManager,
                        scope = scope,
                        repository = repository,
                        onConvertingState = { isConverting ->
                            viewModel.setPdfConverting(isConverting, if (isConverting) "Rendering PDF for \"${file.nameWithoutExtension}\"..." else "")
                        },
                        onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                    )
                },
                onEditInCanvas = {
                    noteForActionDialog = null
                    NoteOpenManager.openInCanvas(
                        context = context,
                        file = file,
                        repository = repository,
                        localView = localView
                    )
                }
            )
        }

        if (showNewNoteDialog) {
            SaveAsNoteDialog(
                title = stringResource(R.string.dialog_new_note_title),
                subtitle = stringResource(R.string.dialog_new_note_subtitle),
                icon = Icons.Default.Add,
                initialName = newNoteDefaultName,
                initialFolder = currentDirectory,
                availableFolders = allFoldersForNewNote,
                rootFolder = repository.getRootNotesDirectory(),
                confirmButtonLabel = stringResource(R.string.action_create_and_open),
                onDismiss = { showNewNoteDialog = false },
                onSkip = {
                    showNewNoteDialog = false
                    val intent = Intent(context, CanvasActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (context is android.app.Activity && context.isInMultiWindowMode) {
                            addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                        }
                    }
                    context.startActivity(intent)
                },
                onSave = { name, targetFolder ->
                    showNewNoteDialog = false
                    scope.launch {
                        val result = repository.createBlankNote(name, targetFolder)
                        if (result.isSuccess) {
                            val file = result.getOrThrow()
                            viewModel.loadContentNow()
                            NoteOpenManager.openInCanvas(
                                context = context,
                                file = file,
                                repository = repository,
                                localView = localView
                            )
                        } else {
                            snackbarHostState.showSnackbar("Failed to create note: ${result.exceptionOrNull()?.message}")
                        }
                    }
                },
                onCreateFolder = { name, colorHex, iconEmoji, iconType ->
                    val result = repository.createFolder(
                        parentDir = repository.getRootNotesDirectory(),
                        name = name,
                        colorHex = colorHex,
                        iconEmoji = iconEmoji,
                        iconType = iconType
                    )
                    if (result.isSuccess) {
                        val newFolder = result.getOrThrow()
                        allFoldersForNewNote = allFoldersForNewNote + FolderItem(
                            file = newFolder,
                            name = newFolder.name,
                            colorHex = colorHex,
                            iconEmoji = iconEmoji,
                            iconType = iconType,
                            isEmergencyFolder = false
                        )
                        viewModel.loadContent()
                    }
                    result
                }
            )
        }

        shareExportNote?.let { note ->
            val defaultName = remember(note.file.path) { note.file.nameWithoutExtension }
            SingleShareExportDialog(
                note = note,
                initialName = defaultName,
                onDismiss = { shareExportNote = null },
                onSave = { sanitizedName, format ->
                    shareExportNote = null
                    viewModel.setSelectionMode(false)
                    pendingSingleExport = PendingSingleExport(note, format, sanitizedName)
                    val ext = if (format == DocumentRepository.ShareExportFormat.PDF) "pdf" else "xopp"
                    singleSaveLauncher.launch("$sanitizedName.$ext")
                },
                onShare = { sanitizedName, format ->
                    shareExportNote = null
                    viewModel.setSelectionMode(false)
                    viewModel.setPdfConverting(true, "Preparing to share \"$sanitizedName\"...")
                    scope.launch {
                        val result = repository.shareUnifiedDocuments(
                            context = context,
                            docs = listOf(note),
                            format = format,
                            customNameForSingle = sanitizedName,
                            pdfExportManager = pdfExportManager
                        )
                        viewModel.setPdfConverting(false)
                        if (result.isFailure) {
                            snackbarHostState.showSnackbar("Failed to share: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            )
        }

        if (showBatchShareExportDialog && selectedNotePaths.isNotEmpty()) {
            val batchSelectedDocs = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }
            if (batchSelectedDocs.size == 1) {
                showBatchShareExportDialog = false
                shareExportNote = batchSelectedDocs.first()
            } else if (batchSelectedDocs.size > 1) {
                BatchShareExportDialog(
                    selectedNotes = batchSelectedDocs,
                    onDismiss = { showBatchShareExportDialog = false },
                    onSaveBatch = { format ->
                        showBatchShareExportDialog = false
                        viewModel.setSelectionMode(false)
                        pendingBatchExportFormat = format
                        pendingBatchExportDocs = batchSelectedDocs
                        batchSaveLauncher.launch(null)
                    },
                    onShareBatch = { format ->
                        showBatchShareExportDialog = false
                        viewModel.setSelectionMode(false)
                        viewModel.setPdfConverting(true, "Preparing ${batchSelectedDocs.size} documents to share...")
                        scope.launch {
                            val result = repository.shareUnifiedDocuments(
                                context = context,
                                docs = batchSelectedDocs,
                                format = format,
                                pdfExportManager = pdfExportManager
                            )
                            viewModel.setPdfConverting(false)
                            if (result.isFailure) {
                                snackbarHostState.showSnackbar("Failed to share: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    }
                )
            } else {
                showBatchShareExportDialog = false
            }
        }
    }
}
