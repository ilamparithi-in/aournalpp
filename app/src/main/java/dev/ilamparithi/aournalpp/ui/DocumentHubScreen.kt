package dev.ilamparithi.aournalpp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.utils.FileNameTemplateEngine
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import dev.ilamparithi.aournalpp.ui.util.AccessibilityUtils
import dev.ilamparithi.aournalpp.ui.util.a11yHeading
import dev.ilamparithi.aournalpp.ui.util.minTouchTarget
import dev.ilamparithi.aournalpp.ui.util.AppIconButton
import dev.ilamparithi.aournalpp.ui.util.AppTooltipBox
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toIntRect
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.LicensesActivity
import dev.ilamparithi.aournalpp.SettingsActivity
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.utils.ExternalFileHandler
import dev.ilamparithi.aournalpp.ui.preview.floatingPreviewLongPress
import dev.ilamparithi.aournalpp.utils.ThumbnailManager
import dev.ilamparithi.aournalpp.utils.FormatUtils
import dev.ilamparithi.aournalpp.utils.NoteOpenAction
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import dev.ilamparithi.aournalpp.ui.NoteOpenActionDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val PRESET_FOLDER_COLORS = listOf(
    "#3F51B5", // Indigo
    "#009688", // Teal
    "#4CAF50", // Emerald Green
    "#FF9800", // Amber
    "#E91E63", // Pink
    "#9C27B0", // Purple
    "#00BCD4", // Cyan
    "#F44336"  // Coral Red
)

val PRESET_FOLDER_EMOJIS = listOf(
    "📁", "📝", "📚", "🎨", "💡", "🔬", "📐", "💼", "🏠", "⭐", "🚀", "🧪", "📓", "🏷️", "🎯", "🌿", "💻", "☕"
)

private fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 4..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..22 -> "Good evening"
        else -> "Welcome back"
    }
}

private const val SEARCH_DEBOUNCE_MS = 250L

private val FileSaver = Saver<File, String>(
    save = { it.absolutePath },
    restore = { File(it) }
)

private val StringSetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentHubScreen(
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToLicenses: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val repository = remember { DocumentRepository(context) }
    val env = remember { repository.getLinuxEnvironment() }
    val supervisor = remember { ProcessSupervisor(env) }
    val pdfExportManager = remember { PdfExportManager(env, supervisor) }
    val prefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }

    var hasPermission by remember { mutableStateOf(hasStoragePermission(context)) }
    var showPermissionDialog by remember { mutableStateOf(!hasPermission) }
    var showHiddenFiles by rememberSaveable { mutableStateOf(prefs.getBoolean("pref_show_hidden_files", false)) }
    var isGridView by rememberSaveable { mutableStateOf(prefs.getBoolean("pref_is_grid_view", true)) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val initialCached = remember {
        repository.getCachedDirectory(repository.getRootNotesDirectory(), "", false)
    }
    var currentDirectory by rememberSaveable(stateSaver = FileSaver) {
        mutableStateOf(repository.getRootNotesDirectory())
    }
    var folders by remember { mutableStateOf<List<FolderItem>>(initialCached?.first ?: emptyList()) }
    var notes by remember { mutableStateOf<List<NoteDocument>>(initialCached?.second ?: emptyList()) }
    var trashedNotes by remember { mutableStateOf<List<NoteDocument>>(emptyList()) }
    var isViewingTrash by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    var recentNotes by remember {
        mutableStateOf<List<NoteDocument>>(repository.getCachedRecentNotes(10) ?: emptyList())
    }

    // Multi-Selection State & Drag Selection Tracker
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var isDragSelecting by remember { mutableStateOf(false) }
    var isInitialEntryDrag by remember { mutableStateOf(false) }
    var selectedNotePaths by rememberSaveable(stateSaver = StringSetSaver) {
        mutableStateOf(emptySet<String>())
    }
    var lastSelectedNotePath by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    var autoScrollSpeed by remember { mutableStateOf(0f) }

    LaunchedEffect(autoScrollSpeed) {
        if (autoScrollSpeed != 0f) {
            while (isActive) {
                gridState.scrollBy(autoScrollSpeed)
                delay(10)
            }
        }
    }

    var showTopMenu by remember { mutableStateOf(false) }

    // Progress & Dialog States
    var isPdfConverting by remember { mutableStateOf(false) }
    var convertingMessage by remember { mutableStateOf("") }
    var pendingExportNote by remember { mutableStateOf<NoteDocument?>(null) }

    // Dialog states
    var noteToRename by remember { mutableStateOf<NoteDocument?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var noteToDelete by remember { mutableStateOf<NoteDocument?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    var selectedFolderColor by remember { mutableStateOf(PRESET_FOLDER_COLORS.first()) }
    var selectedFolderEmoji by remember { mutableStateOf<String?>(null) }
    var selectedFolderIconType by remember { mutableStateOf<String?>("folder") }
    var folderToEdit by remember { mutableStateOf<FolderItem?>(null) }
    var editFolderSelectedColor by remember { mutableStateOf(PRESET_FOLDER_COLORS.first()) }
    var editFolderSelectedEmoji by remember { mutableStateOf<String?>(null) }
    var editFolderSelectedIconType by remember { mutableStateOf<String?>("folder") }
    var folderToRename by remember { mutableStateOf<FolderItem?>(null) }
    var renameFolderNameInput by remember { mutableStateOf("") }
    var showMoveToFolderDialog by remember { mutableStateOf(false) }
    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }
    var showBatchDeletePermanentDialog by remember { mutableStateOf(false) }
    var folderToMapToCloud by remember { mutableStateOf<FolderItem?>(null) }

    // Emergency recovery
    var quarantinedEmergencySave by remember { mutableStateOf<File?>(null) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showEmergencySaveNameDialog by remember { mutableStateOf(false) }
    var emergencySaveNameInput by remember { mutableStateOf("") }
    var emergencySaveTargetFolder by remember { mutableStateOf(repository.getRootNotesDirectory()) }

    // Autosave on-open resolution state
    var pendingAutosaveNote by remember { mutableStateOf<NoteDocument?>(null) }
    var pendingSaveAutosaveNote by remember { mutableStateOf<NoteDocument?>(null) }

    // Autoload override conflict notification state
    var showAutoloadOverrideDialog by remember { mutableStateOf(false) }

    data class SingleFileActionPrompt(
        val note: NoteDocument,
        val actionType: FileActionPromptType,
        val defaultName: String
    )
    var activeFilePrompt by remember { mutableStateOf<SingleFileActionPrompt?>(null) }

    val aournalPrefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    val reduceAnimations = remember { aournalPrefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false) }

    // Speed Dial FAB State
    var isFabExpanded by remember { mutableStateOf(false) }
    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 135f else 0f,
        animationSpec = if (reduceAnimations) snap() else spring(dampingRatio = 0.65f, stiffness = 300f),
        label = "fabRotation"
    )

    // New Note dialog state
    var showNewNoteDialog by remember { mutableStateOf(false) }
    var newNoteDefaultName by remember { mutableStateOf("") }
    var allFoldersForNewNote by remember { mutableStateOf<List<FolderItem>>(emptyList()) }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = hasStoragePermission(context)
    }

    // SAF PDF Export Launcher
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val note = pendingExportNote
        pendingExportNote = null
        if (uri != null && note != null) {
            isPdfConverting = true
            convertingMessage = "Exporting \"${note.title}\" to PDF..."
            scope.launch {
                val result = pdfExportManager.exportPdfToUri(context, note.file, uri)
                isPdfConverting = false
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("Exported ${note.title}.pdf successfully")
                } else {
                    snackbarHostState.showSnackbar("PDF Export failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    suspend fun loadContentNow() {
        if (!hasPermission) return
        if (isViewingTrash) {
            trashedNotes = repository.scanTrash()
        } else {
            val (fList, nList) = repository.scanDirectory(
                targetDir = currentDirectory,
                query = searchQuery,
                showHidden = showHiddenFiles
            )
            folders = fList
            notes = nList

            if (currentDirectory.canonicalPath == repository.getRootNotesDirectory().canonicalPath) {
                recentNotes = repository.getAllRecentNotes(10)
            }

            ThumbnailManager.prefetchThumbnails(context, nList, pdfExportManager, scope)

            val emergencyFile = withContext(Dispatchers.IO) { env.checkAndQuarantineEmergencySave() }
            if (emergencyFile != null && emergencyFile.exists() && emergencyFile.length() > 0) {
                if (quarantinedEmergencySave == null && !showEmergencySaveNameDialog) {
                    quarantinedEmergencySave = emergencyFile
                    showEmergencyDialog = true
                }
            }
        }

        val autoloadOverridden = withContext(Dispatchers.IO) { env.checkAndOverrideAutoloadPreference() }
        if (autoloadOverridden || env.hasPendingAutoloadOverrideNotification()) {
            showAutoloadOverrideDialog = true
        }
    }

    fun loadContent() {
        scope.launch { loadContentNow() }
    }

    var noteForActionDialog by remember { mutableStateOf<File?>(null) }
    val localView = LocalView.current

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
                isPdfConverting = isConverting
                if (isConverting) {
                    convertingMessage = "Rendering PDF for \"${noteFile.nameWithoutExtension}\"..."
                }
            },
            onError = { err ->
                scope.launch { snackbarHostState.showSnackbar(err) }
            }
        )
    }

    fun openNoteInCanvas(noteFile: File) {
        handleNoteOpen(noteFile)
    }

    // SAF Import Document / Note Launcher (imports directly to current folder)
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = ExternalFileHandler.importUriToDirectory(context, uri, currentDirectory)
                if (result.isSuccess) {
                    val imported = result.getOrThrow()
                    loadContentNow()
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
                    loadContent()
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
        // Debounce search input.
        if (searchQuery.isNotEmpty()) delay(SEARCH_DEBOUNCE_MS)
        loadContentNow()
    }

    BackHandler(enabled = isSelectionMode || isViewingTrash || currentDirectory.canonicalPath != repository.getRootNotesDirectory().canonicalPath) {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedNotePaths = emptySet()
        } else if (isViewingTrash) {
            isViewingTrash = false
        } else if (currentDirectory.canonicalPath != repository.getRootNotesDirectory().canonicalPath) {
            currentDirectory = currentDirectory.parentFile ?: repository.getRootNotesDirectory()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                val currentDisplayNotes = if (isViewingTrash) trashedNotes else notes
                // Contextual Multi-Selection Top Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = androidx.compose.ui.res.pluralStringResource(
                                    dev.ilamparithi.aournalpp.R.plurals.hub_selected_count,
                                    selectedNotePaths.size,
                                    selectedNotePaths.size
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val selectedDocs = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }
                            val pdfCount = selectedDocs.count { it.fileType == NoteFileType.PDF }
                            val noteCount = selectedDocs.size - pdfCount
                            val summary = listOfNotNull(
                                "$noteCount note${if (noteCount != 1) "s" else ""}".takeIf { noteCount > 0 },
                                "$pdfCount PDF${if (pdfCount != 1) "s" else ""}".takeIf { pdfCount > 0 }
                            ).joinToString(", ")

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
                        val cancelLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel)
                        AppIconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedNotePaths = emptySet()
                                lastSelectedNotePath = null
                            },
                            tooltip = cancelLabel
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = cancelLabel
                            )
                        }
                    },
                    actions = {
                        val allSelected = selectedNotePaths.size == currentDisplayNotes.size && currentDisplayNotes.isNotEmpty()
                        val selectAllLabel = if (allSelected) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_deselect_all)
                        else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_select_all)

                        AppIconButton(
                            onClick = {
                                if (allSelected) {
                                    selectedNotePaths = emptySet()
                                } else {
                                    selectedNotePaths = currentDisplayNotes.map { it.path }.toSet()
                                }
                            },
                            tooltip = selectAllLabel
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = selectAllLabel
                            )
                        }

                        val invertLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_invert_selection)
                        AppIconButton(
                            onClick = {
                                val allPaths = currentDisplayNotes.map { it.path }.toSet()
                                selectedNotePaths = allPaths.minus(selectedNotePaths)
                            },
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
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_search_hint)) },
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
                        val backLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_back)
                        AppIconButton(
                            onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            },
                            tooltip = backLabel
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = backLabel
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            val clearLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_clear)
                            AppIconButton(
                                onClick = { searchQuery = "" },
                                tooltip = clearLabel
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = clearLabel
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isViewingTrash) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_menu_trash) else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        val backLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_back)
                        if (isViewingTrash) {
                            AppIconButton(
                                onClick = {
                                    isViewingTrash = false
                                    loadContent()
                                },
                                tooltip = backLabel
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = backLabel
                                )
                            }
                        } else if (currentDirectory.canonicalPath != repository.getRootNotesDirectory().canonicalPath) {
                            AppIconButton(
                                onClick = {
                                    currentDirectory = currentDirectory.parentFile ?: repository.getRootNotesDirectory()
                                    loadContent()
                                },
                                tooltip = backLabel
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = backLabel
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isViewingTrash) {
                            val searchLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_search)
                            AppIconButton(
                                onClick = { isSearchActive = true },
                                tooltip = searchLabel
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = searchLabel
                                )
                            }

                            val viewModeLabel = if (isGridView) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_view_mode_grid)
                            else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_view_mode_collage)
                            AppIconButton(
                                onClick = {
                                    val updated = !isGridView
                                    isGridView = updated
                                    prefs.edit().putBoolean("pref_is_grid_view", updated).apply()
                                },
                                tooltip = viewModeLabel
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                    contentDescription = viewModeLabel
                                )
                            }
                        }

                        dev.ilamparithi.aournalpp.ui.cloud.QuickSyncButton(
                            onSyncFinished = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )

                        val detailsLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_details)
                        AppIconButton(
                            onClick = { showTopMenu = true },
                            tooltip = detailsLabel
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = detailsLabel
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            if (!isViewingTrash) {
                                DropdownMenuItem(
                                    text = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_menu_select_notes)) },
                                    leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        isSelectionMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_menu_new_folder)) },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        newFolderNameInput = ""
                                        showNewFolderDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (showHiddenFiles) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_menu_hide_hidden)
                                            else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_menu_show_hidden)
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
                                        val updated = !showHiddenFiles
                                        showHiddenFiles = updated
                                        prefs.edit().putBoolean("pref_show_hidden_files", updated).apply()
                                        loadContent()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_menu_trash)) },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        isViewingTrash = true
                                        loadContent()
                                    }
                                )
                                HorizontalDivider()
                            } else {
                                if (trashedNotes.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_menu_select_notes)) },
                                        leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                                        onClick = {
                                            showTopMenu = false
                                            isSelectionMode = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_menu_empty_trash), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showTopMenu = false
                                        showEmptyTrashConfirmDialog = true
                                    }
                                )
                                HorizontalDivider()
                            }

                            DropdownMenuItem(
                                text = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pref_category_licenses)) },
                                leadingIcon = { Icon(Icons.Default.Gavel, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    if (onNavigateToLicenses != null) {
                                        onNavigateToLicenses()
                                    } else {
                                        context.startActivity(Intent(context, LicensesActivity::class.java))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.tab_settings)) },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    if (onNavigateToSettings != null) {
                                        onNavigateToSettings()
                                    } else {
                                        context.startActivity(Intent(context, SettingsActivity::class.java))
                                    }
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
        },
        floatingActionButton = {
            if (!isViewingTrash && !isSelectionMode) {
                val folderItemSpring by animateFloatAsState(
                    targetValue = if (isFabExpanded) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
                    label = "folderItemSpring"
                )
                val pdfItemSpring by animateFloatAsState(
                    targetValue = if (isFabExpanded) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f),
                    label = "pdfItemSpring"
                )
                val noteItemSpring by animateFloatAsState(
                    targetValue = if (isFabExpanded) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
                    label = "noteItemSpring"
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Staggered Spring Action Items
                    SpeedDialActionItem(
                        progress = folderItemSpring,
                        icon = Icons.Default.CreateNewFolder,
                        label = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_create_folder),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = {
                            isFabExpanded = false
                            newFolderNameInput = ""
                            showNewFolderDialog = true
                        }
                    )

                    SpeedDialActionItem(
                        progress = pdfItemSpring,
                        icon = Icons.Default.FileOpen,
                        label = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_open),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            isFabExpanded = false
                            importFileLauncher.launch(arrayOf("*/*", "application/pdf", "application/x-xopp", "application/x-xoj", "application/octet-stream"))
                        }
                    )

                    SpeedDialActionItem(
                        progress = noteItemSpring,
                        icon = Icons.Default.Edit,
                        label = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_create_note),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            isFabExpanded = false
                            if (!hasPermission) {
                                showPermissionDialog = true
                            } else {
                                newNoteDefaultName = FileNameTemplateEngine.evaluate(
                                    FileNameTemplateEngine.getNewFileTemplate(context),
                                    context
                                )
                                scope.launch {
                                    allFoldersForNewNote = withContext(Dispatchers.IO) {
                                        repository.getAllFolders()
                                    }
                                }
                                showNewNoteDialog = true
                            }
                        }
                    )

                    // Main Speed Dial FAB
                    val fabInteractionSource = remember { MutableInteractionSource() }
                    val isFabPressed by fabInteractionSource.collectIsPressedAsState()
                    val fabPressScale by animateFloatAsState(
                        targetValue = if (isFabPressed) 0.90f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "fabPressScale"
                    )

                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        interactionSource = fabInteractionSource,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .size(64.dp)
                            .scale(fabPressScale)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.cd_expand_doc_actions),
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(fabRotation)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        // 0. New Note Dialog
        if (showNewNoteDialog) {
            SaveAsNoteDialog(
                title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_new_note_title),
                subtitle = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_new_note_subtitle),
                icon = Icons.Default.Add,
                initialName = newNoteDefaultName,
                initialFolder = currentDirectory,
                availableFolders = allFoldersForNewNote,
                rootFolder = repository.getRootNotesDirectory(),
                confirmButtonLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_create_and_open),
                onDismiss = { showNewNoteDialog = false },
                onSkip = {
                    showNewNoteDialog = false
                    val intent = Intent(context, CanvasActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    context.startActivity(intent)
                },
                onSave = { name, targetFolder ->
                    showNewNoteDialog = false
                    scope.launch {
                        val result = repository.createBlankNote(name, targetFolder)
                        if (result.isSuccess) {
                            val file = result.getOrThrow()
                            loadContentNow()
                            NoteOpenManager.openInCanvas(
                                context = context,
                                file = file,
                                repository = repository,
                                localView = localView
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                "Failed to create note: ${result.exceptionOrNull()?.message}"
                            )
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
                        loadContent()
                    }
                    result
                }
            )
        }

        // Single-File Action Name Prompt Dialog (Export as PDF, Share as PDF, Share as XOPP)
        activeFilePrompt?.let { prompt ->
            val title: String
            val subtitle: String
            val ext: String
            val icon: androidx.compose.ui.graphics.vector.ImageVector
            val btnText: String

            when (prompt.actionType) {
                FileActionPromptType.EXPORT_PDF -> {
                    title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_export_pdf)
                    subtitle = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_export_pdf_subtitle)
                    ext = ".pdf"
                    icon = Icons.Default.FileDownload
                    btnText = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_export_button)
                }
                FileActionPromptType.SHARE_PDF -> {
                    title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_share_pdf_title)
                    subtitle = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_share_pdf_subtitle)
                    ext = ".pdf"
                    icon = Icons.Default.PictureAsPdf
                    btnText = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_share)
                }
                FileActionPromptType.SHARE_XOPP -> {
                    title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_share_note_title)
                    subtitle = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_share_note_subtitle)
                    ext = ".xopp"
                    icon = Icons.Default.Share
                    btnText = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_share)
                }
            }

            FileNamePromptDialog(
                title = title,
                subtitle = subtitle,
                extension = ext,
                icon = icon,
                initialName = prompt.defaultName,
                confirmButtonText = btnText,
                onDismiss = { activeFilePrompt = null },
                onConfirm = { customName ->
                    val note = prompt.note
                    val actionType = prompt.actionType
                    activeFilePrompt = null
                    when (actionType) {
                        FileActionPromptType.EXPORT_PDF -> {
                            pendingExportNote = note
                            exportPdfLauncher.launch("$customName.pdf")
                        }
                        FileActionPromptType.SHARE_PDF -> {
                            isPdfConverting = true
                            convertingMessage = "Rendering PDF for sharing..."
                            scope.launch {
                                val result = repository.shareNoteAsPdf(context, note, pdfExportManager, customName = customName)
                                isPdfConverting = false
                                if (result.isFailure) {
                                    snackbarHostState.showSnackbar("Failed to share PDF: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                        FileActionPromptType.SHARE_XOPP -> {
                            repository.shareNoteAsXopp(context, note, customName = customName)
                        }
                    }
                }
            )
        }

        // 1. Storage Permission Prompt Dialog
        if (showPermissionDialog && !hasPermission) {

            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = {
                    Icon(
                        imageVector = Icons.Default.FolderShared,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_storage_title), fontWeight = FontWeight.Bold) },
                text = {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_storage_desc))
                },
                confirmButton = {
                    Button(onClick = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            requestStoragePermission(context)
                        } else {
                            legacyPermissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            )
                        }
                    }) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_storage_grant_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_storage_skip_action)) }
                }
            )
        }

        // 2. M3 Standard Progress Dialog during PDF Export/Share
        if (isPdfConverting) {
            AlertDialog(
                onDismissRequest = {},
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                },
                title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.title_processing_document), fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(convertingMessage, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {}
            )
        }

        // 3. Create Folder Dialog (Modular)
        if (showNewFolderDialog) {
            val isRootFolder = currentDirectory.canonicalPath == repository.getRootNotesDirectory().canonicalPath
            CreateFolderDialog(
                parentFolder = currentDirectory,
                title = if (isRootFolder) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_new_folder_title)
                else "New Folder in \"${currentDirectory.name}\"",
                confirmButtonLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_create_button),
                initialColorHex = selectedFolderColor,
                initialEmoji = selectedFolderEmoji,
                initialIconType = selectedFolderIconType,
                onDismiss = { showNewFolderDialog = false },
                onCreate = { name, colorHex, iconEmoji, iconType ->
                    showNewFolderDialog = false
                    scope.launch {
                        val result = repository.createFolder(currentDirectory, name, colorHex, iconEmoji, iconType)
                        if (result.isSuccess) {
                            loadContentNow()
                            snackbarHostState.showSnackbar("Created folder \"$name\"")
                        } else {
                            snackbarHostState.showSnackbar("Failed to create folder: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            )
        }

        // 4. Edit Folder Appearance Dialog (Icon & Color)
        folderToEdit?.let { folder ->
            AlertDialog(
                onDismissRequest = { folderToEdit = null },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = { Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Customize \"${folder.name}\"", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_change_emoji), style = MaterialTheme.typography.labelMedium)
                            FolderIconPickerRow(
                                selectedEmoji = editFolderSelectedEmoji,
                                selectedIconType = editFolderSelectedIconType,
                                defaultRoleIconType = folder.role ?: (if (folder.isEmergencyFolder) "emergency" else null),
                                onIconSelected = { emoji, iconType ->
                                    editFolderSelectedEmoji = emoji
                                    editFolderSelectedIconType = iconType
                                }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_change_color), style = MaterialTheme.typography.labelMedium)
                            FolderColorPickerRow(
                                selectedColorHex = editFolderSelectedColor,
                                onColorSelected = { editFolderSelectedColor = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val target = folderToEdit
                        folderToEdit = null
                        target?.let {
                            repository.updateFolderMeta(it.file, editFolderSelectedColor, editFolderSelectedEmoji, editFolderSelectedIconType)
                            loadContent()
                        }
                    }) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToEdit = null }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel)) }
                }
            )
        }

        // 4b. Rename Folder Dialog
        folderToRename?.let { folder ->
            AlertDialog(
                onDismissRequest = { folderToRename = null },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = {
                    Icon(
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_rename_folder_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = renameFolderNameInput,
                            onValueChange = { renameFolderNameInput = it },
                            label = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_folder_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val target = folderToRename
                        folderToRename = null
                        if (target != null && renameFolderNameInput.isNotBlank()) {
                            scope.launch {
                                val result = repository.renameFolder(target.file, renameFolderNameInput)
                                if (result.isSuccess) {
                                    loadContentNow()
                                    snackbarHostState.showSnackbar("Renamed folder to \"${renameFolderNameInput.trim()}\"")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to rename: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    }) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_rename))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToRename = null }) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel))
                    }
                }
            )
        }

        // Folder Cloud Mapping Dialog
        folderToMapToCloud?.let { folder ->
            val vault = remember { dev.ilamparithi.aournalpp.backup.security.CredentialsVault(context) }
            val services = remember { vault.getAllServices() }
            dev.ilamparithi.aournalpp.ui.cloud.CustomMappingDialog(
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

        // 5. Move to Folder Dialog
        if (showMoveToFolderDialog) {
            val selectedDocs = notes.filter { selectedNotePaths.contains(it.path) }
            val allAvailableFolders by produceState<List<FolderItem>>(emptyList()) { value = repository.getAllFolders() }
            var isCreatingInlineFolder by remember { mutableStateOf(false) }
            var inlineFolderName by remember { mutableStateOf("") }
            var inlineFolderColor by remember { mutableStateOf(PRESET_FOLDER_COLORS.first()) }

            AlertDialog(
                onDismissRequest = { showMoveToFolderDialog = false },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
                title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_move_notes_title, selectedDocs.size), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_move_destination_header), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (isCreatingInlineFolder) {
                            OutlinedTextField(
                                value = inlineFolderName,
                                onValueChange = { inlineFolderName = it },
                                label = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_new_folder_title)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(PRESET_FOLDER_COLORS) { colorHex ->
                                    val color = Color(android.graphics.Color.parseColor(colorHex))
                                    val isSelected = colorHex == inlineFolderColor
                                    Surface(
                                        shape = CircleShape,
                                        color = color,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { inlineFolderColor = colorHex }
                                            .border(width = if (isSelected) 2.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                                    ) {
                                        if (isSelected) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { isCreatingInlineFolder = false }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel)) }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(onClick = {
                                    if (inlineFolderName.isNotBlank()) {
                                        val created = repository.createFolder(repository.getRootNotesDirectory(), inlineFolderName, inlineFolderColor)
                                        if (created.isSuccess) {
                                            val dest = created.getOrThrow()
                                            scope.launch {
                                                val count = repository.moveNotesToFolder(selectedDocs, dest).getOrDefault(0)
                                                showMoveToFolderDialog = false
                                                isSelectionMode = false
                                                selectedNotePaths = emptySet()
                                                loadContentNow()
                                                snackbarHostState.showSnackbar("Moved $count note(s) to \"${dest.name}\"")
                                            }
                                        }
                                    }
                                }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_create_and_move)) }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Root folder option
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    val count = repository.moveNotesToFolder(selectedDocs, repository.getRootNotesDirectory()).getOrDefault(0)
                                                    showMoveToFolderDialog = false
                                                    isSelectionMode = false
                                                    selectedNotePaths = emptySet()
                                                    loadContentNow()
                                                    snackbarHostState.showSnackbar("Moved $count note(s) to Notes Root")
                                                }
                                            }
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_root_folder_name), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                items(allAvailableFolders, key = { it.file.absolutePath }) { folder ->
                                    val fColor = folder.colorHex?.let {
                                        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
                                    } ?: MaterialTheme.colorScheme.primary

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = fColor.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    val count = repository.moveNotesToFolder(selectedDocs, folder.file).getOrDefault(0)
                                                    showMoveToFolderDialog = false
                                                    isSelectionMode = false
                                                    selectedNotePaths = emptySet()
                                                    loadContentNow()
                                                    snackbarHostState.showSnackbar("Moved $count note(s) to \"${folder.name}\"")
                                                }
                                            }
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (!folder.iconEmoji.isNullOrBlank()) {
                                                Text(folder.iconEmoji, fontSize = 18.sp)
                                            } else {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = fColor, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(folder.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            Text(
                                                androidx.compose.ui.res.pluralStringResource(
                                                    dev.ilamparithi.aournalpp.R.plurals.home_stat_notes_count,
                                                    folder.itemCount,
                                                    folder.itemCount
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                item {
                                    OutlinedButton(
                                        onClick = { isCreatingInlineFolder = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_create_folder))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showMoveToFolderDialog = false }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel)) }
                }
            )
        }

        // 6. Rename Dialog
        noteToRename?.let { doc ->
            AlertDialog(
                onDismissRequest = { noteToRename = null },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_rename_title), fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        label = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_note_name_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (renameInputText.isNotBlank()) {
                            val target = noteToRename
                            noteToRename = null
                            target?.let { note ->
                                scope.launch {
                                    val result = repository.renameNote(note, renameInputText)
                                    if (result.isSuccess) {
                                        loadContentNow()
                                        snackbarHostState.showSnackbar("Renamed note successfully")
                                    } else {
                                        snackbarHostState.showSnackbar("Rename failed: ${result.exceptionOrNull()?.message}")
                                    }
                                }
                            }
                        }
                    }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_rename)) }
                },
                dismissButton = { TextButton(onClick = { noteToRename = null }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel)) } }
            )
        }

        // 7. Delete / Move to Trash / Permanent Delete Dialog
        noteToDelete?.let { doc ->
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = {
                    Text(
                        if (isViewingTrash) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_delete_permanent_title)
                        else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_delete_note_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        if (isViewingTrash)
                            "Permanently delete \"${doc.title}\"? This action cannot be undone."
                        else
                            androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_delete_note_body, doc.title)
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            val target = noteToDelete
                            noteToDelete = null
                            target?.let { note ->
                                scope.launch {
                                    if (isViewingTrash) {
                                        repository.deletePermanently(listOf(note))
                                        loadContentNow()
                                        snackbarHostState.showSnackbar("Permanently deleted \"${note.title}\"")
                                    } else {
                                        repository.deleteNote(note)
                                        loadContentNow()
                                        snackbarHostState.showSnackbar("Moved \"${note.title}\" to Trash")
                                    }
                                }
                            }
                        }
                    ) {
                        Text(
                            if (isViewingTrash) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_delete_permanent)
                            else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_delete)
                        )
                    }
                },
                dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel)) } }
            )
        }

        // 7b. Batch Delete Permanent Confirmation Dialog
        if (showBatchDeletePermanentDialog) {
            val currentDisplayNotes = if (isViewingTrash) trashedNotes else notes
            val selectedDocs = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }

            AlertDialog(
                onDismissRequest = { showBatchDeletePermanentDialog = false },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_delete_permanent_title), fontWeight = FontWeight.Bold) },
                text = {
                    Text("Permanently delete ${selectedDocs.size} item(s)? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            showBatchDeletePermanentDialog = false
                            scope.launch {
                                val count = repository.deletePermanently(selectedDocs).getOrDefault(0)
                                isSelectionMode = false
                                selectedNotePaths = emptySet()
                                loadContentNow()
                                snackbarHostState.showSnackbar("Permanently deleted $count item(s)")
                            }
                        }
                    ) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_delete_permanent)) }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeletePermanentDialog = false }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel)) }
                }
            )
        }

        // 7c. Empty Trash Confirmation Dialog
        if (showEmptyTrashConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyTrashConfirmDialog = false },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_empty_trash_title), fontWeight = FontWeight.Bold) },
                text = {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_empty_trash_body))
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            showEmptyTrashConfirmDialog = false
                            scope.launch {
                                repository.emptyTrash()
                                loadContentNow()
                                snackbarHostState.showSnackbar("Emptied Trash")
                            }
                        }
                    ) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_menu_empty_trash)) }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashConfirmDialog = false }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel)) }
                }
            )
        }

        // 8. Emergency Recovery Launch Dialog
        if (showEmergencyDialog && quarantinedEmergencySave != null) {
            val file = quarantinedEmergencySave!!
            val dateStr = FormatUtils.formatDateTimeMedium(file.lastModified())

            AlertDialog(
                onDismissRequest = { showEmergencyDialog = false },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
                title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_emergency_recovery_title), fontWeight = FontWeight.Bold) },
                text = {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_emergency_recovery_body, dateStr))
                },
                confirmButton = {
                    Button(onClick = {
                        showEmergencyDialog = false
                        val staged = repository.openEmergencyRecoverySession(file)
                        quarantinedEmergencySave = null
                        loadContent()
                        openNoteInCanvas(staged)
                    }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_open_now)) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            showEmergencyDialog = false
                            val defaultName = "Recovered_Note_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(file.lastModified()))
                            emergencySaveNameInput = defaultName
                            emergencySaveTargetFolder = currentDirectory
                            showEmergencySaveNameDialog = true
                        }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_save_as_note)) }
                        TextButton(onClick = {
                            showEmergencyDialog = false
                            repository.discardEmergencyRecovery()
                            quarantinedEmergencySave = null
                            loadContent()
                        }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_discard), color = MaterialTheme.colorScheme.error) }
                    }
                }
            )
        }

        // 8a. Autoload Preference Conflict Overridden Dialog
        if (showAutoloadOverrideDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAutoloadOverrideDialog = false
                    env.clearPendingAutoloadOverrideNotification()
                },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_autoload_override_title), fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_autoload_override_body))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAutoloadOverrideDialog = false
                            env.clearPendingAutoloadOverrideNotification()
                        }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_got_it))
                    }
                }
            )
        }

        // 8b. Save Emergency Recovery Name Dialog
        if (showEmergencySaveNameDialog && quarantinedEmergencySave != null) {
            val file = quarantinedEmergencySave!!
            val allAvailableFolders by produceState<List<FolderItem>>(emptyList(), showEmergencySaveNameDialog) {
                value = repository.getAllFolders()
            }

            SaveAsNoteDialog(
                title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_save_recovered_title),
                subtitle = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_save_recovered_subtitle),
                icon = Icons.Default.Emergency,
                initialName = emergencySaveNameInput,
                initialFolder = emergencySaveTargetFolder,
                availableFolders = allAvailableFolders,
                rootFolder = repository.getRootNotesDirectory(),
                onDismiss = { showEmergencySaveNameDialog = false },
                onSave = { name, targetFolder ->
                    showEmergencySaveNameDialog = false
                    val savedFile = repository.saveEmergencyRecoveryToNotes(
                        file,
                        name,
                        targetFolder
                    )
                    quarantinedEmergencySave = null
                    loadContent()
                    scope.launch {
                        snackbarHostState.showSnackbar("Saved recovered note as \"${savedFile.name}\"")
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
                        loadContent()
                    }
                    result
                }
            )
        }

        // 9. Autosave Resolution Dialog
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
                        loadContent()
                        openNoteInCanvas(target)
                    },
                    onKeepBoth = {
                        pendingAutosaveNote = null
                        pendingSaveAutosaveNote = note
                    },
                    onKeepExisting = {
                        val target = repository.discardAutosave(note)
                        pendingAutosaveNote = null
                        loadContent()
                        openNoteInCanvas(target)
                    }
                )
            }
        }

        // 9b. Save Autosave as Note Dialog
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
                        loadContent()
                        openNoteInCanvas(note.file)
                        scope.launch {
                            snackbarHostState.showSnackbar("Saved autosave copy as \"${savedFile.name}\"")
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
                            loadContent()
                        }
                        result
                    }
                )
            }
        }

        // 10. Note Open Action Prompt Dialog (View as PDF / Edit in Xournal++)
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
                            isPdfConverting = isConverting
                            if (isConverting) {
                                convertingMessage = "Rendering PDF for \"${file.nameWithoutExtension}\"..."
                            }
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
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

        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        loadContentNow()
                        delay(400)
                        isRefreshing = false
                    }
                },
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Interactive Breadcrumbs Bar (if in subfolder)
            val isRoot = currentDirectory.canonicalPath == repository.getRootNotesDirectory().canonicalPath
            if (!isRoot && !isViewingTrash) {
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
                                currentDirectory = repository.getRootNotesDirectory()
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
                        val isRoot = currentDirectory.canonicalPath == repository.getRootNotesDirectory().canonicalPath
                        val currentFolderMeta = remember(currentDirectory) {
                            if (!isRoot) repository.getFolderMeta(currentDirectory) else null
                        }
                        if (!currentFolderMeta?.iconEmoji.isNullOrBlank()) {
                            Text(
                                text = currentFolderMeta!!.iconEmoji!!,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (currentFolderMeta?.iconType == "emergency" || repository.isEmergencySavesFolder(currentDirectory)) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = currentFolderMeta?.colorHex?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null } } ?: MaterialTheme.colorScheme.error
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

            // Main Content Grid with Drag Selection Listener
            AnimatedContent(
                targetState = currentDirectory.canonicalPath to isViewingTrash,
                transitionSpec = {
                    if (targetState.first.length > initialState.first.length) {
                        (slideInHorizontally(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                            initialOffsetX = { it / 3 }
                        ) + fadeIn(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                                    targetOffsetX = { -it / 3 }
                                ) + fadeOut(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f))
                            )
                    } else {
                        (slideInHorizontally(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                            initialOffsetX = { -it / 3 }
                        ) + fadeIn(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                                    targetOffsetX = { it / 3 }
                                ) + fadeOut(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f))
                            )
                    }
                },
                label = "folderNavigationTransition",
                modifier = Modifier.weight(1f)
            ) { (targetPath, targetIsTrash) ->
                val displayFolders = remember(targetPath, targetIsTrash, folders) {
                    if (targetIsTrash) {
                        emptyList()
                    } else if (targetPath == currentDirectory.canonicalPath) {
                        folders
                    } else {
                        repository.getCachedDirectory(File(targetPath), searchQuery, showHiddenFiles)?.first ?: emptyList()
                    }
                }
                val displayNotes = remember(targetPath, targetIsTrash, notes, trashedNotes) {
                    if (targetIsTrash) {
                        trashedNotes
                    } else if (targetPath == currentDirectory.canonicalPath) {
                        notes
                    } else {
                        repository.getCachedDirectory(File(targetPath), searchQuery, showHiddenFiles)?.second ?: emptyList()
                    }
                }

                val pageGridState = rememberSaveable(targetPath, saver = LazyGridState.Saver) {
                    LazyGridState()
                }

                val isPageRoot = targetPath == repository.getRootNotesDirectory().canonicalPath

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
                            setSelectedPaths = { selectedNotePaths = it },
                            lastSelectedPath = { lastSelectedNotePath },
                            setLastSelectedPath = { lastSelectedNotePath = it },
                            isSelectionMode = { isSelectionMode },
                            setIsSelectionMode = { isSelectionMode = it },
                            setIsDragSelecting = { isDragSelecting = it },
                            setIsInitialEntryDrag = { isInitialEntryDrag = it },
                            hapticFeedback = hapticFeedback,
                            autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() },
                            setAutoScrollSpeed = { autoScrollSpeed = it }
                        )
                ) {
                    // Dynamic Multi-Browse Recents Carousel
                    if (isPageRoot && !targetIsTrash && searchQuery.isBlank() && recentNotes.isNotEmpty() && !isSelectionMode) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "recents_carousel_section") {
                            DynamicRecentsCarousel(
                                recentNotes = recentNotes,
                                pdfExportManager = pdfExportManager,
                                onOpenNote = { note ->
                                    if (note.autosaveInfo != null) {
                                        pendingAutosaveNote = note
                                    } else {
                                        openNoteInCanvas(note.file)
                                    }
                                },
                                onTogglePin = { note ->
                                    repository.togglePinNote(note.file.absolutePath)
                                    loadContent()
                                },
                                onSharePdf = { note ->
                                    val defaultName = FileNameTemplateEngine.evaluate(
                                        FileNameTemplateEngine.getSharePdfTemplate(context),
                                        context,
                                        note.file
                                    )
                                    activeFilePrompt = SingleFileActionPrompt(note, FileActionPromptType.SHARE_PDF, defaultName)
                                },
                                onShareXopp = { note ->
                                    val defaultName = FileNameTemplateEngine.evaluate(
                                        FileNameTemplateEngine.getShareXoppTemplate(context),
                                        context,
                                        note.file
                                    )
                                    activeFilePrompt = SingleFileActionPrompt(note, FileActionPromptType.SHARE_XOPP, defaultName)
                                },
                                onExportPdf = { note ->
                                    val defaultName = FileNameTemplateEngine.evaluate(
                                        FileNameTemplateEngine.getExportPdfTemplate(context),
                                        context,
                                        note.file
                                    )
                                    activeFilePrompt = SingleFileActionPrompt(note, FileActionPromptType.EXPORT_PDF, defaultName)
                                },
                                onDuplicate = { note ->
                                    scope.launch {
                                        val result = repository.duplicateNote(note)
                                        if (result.isSuccess) {
                                            loadContent()
                                            snackbarHostState.showSnackbar("Duplicated note \"${note.title}\"")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to duplicate note: ${result.exceptionOrNull()?.message}")
                                        }
                                    }
                                },
                                onDeleteNote = { note -> noteToDelete = note },
                                onRenameNote = { note ->
                                    noteToRename = note
                                    renameInputText = note.title
                                }
                            )
                        }
                    }

                    // Subfolders Section (if any exist)
                    if (displayFolders.isNotEmpty() && !targetIsTrash) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = pluralStringResource(dev.ilamparithi.aournalpp.R.plurals.hub_section_folders_count, displayFolders.size, displayFolders.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.a11yHeading()
                            )
                        }

                        items(displayFolders, key = { it.file.absolutePath }) { folder ->
                            var showFolderMenu by remember { mutableStateOf(false) }
                            var folderInteractionTimestamp by remember { mutableStateOf(0L) }
                            val accentColor = folder.colorHex?.let {
                                try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
                            } ?: MaterialTheme.colorScheme.primary

                            val a11yFolderDescription = remember(folder) {
                                AccessibilityUtils.buildFolderCardA11yDescription(
                                    folderName = folder.name,
                                    noteCount = folder.itemCount,
                                    isPinned = folder.isPinned || folder.isVirtuallyPinned,
                                    isExcludedFromRecents = folder.isExcludedFromRecents,
                                    role = folder.role ?: folder.iconType
                                )
                            }

                            val openFolderActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_open_folder)
                            val isPinnedOrVirtual = folder.isPinned || folder.isVirtuallyPinned
                            val pinFolderActionLabel = if (isPinnedOrVirtual) {
                                androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_unpin_folder)
                            } else {
                                androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_pin_folder)
                            }
                            val renameFolderActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_rename_folder)
                            val customizeFolderActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_customize_folder)
                            val deleteFolderActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_delete_folder)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(folder.file.absolutePath) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                                val hasTouch = event.changes.any { it.pressed || it.positionChanged() }
                                                if (hasTouch || event.type == PointerEventType.Enter || event.type == PointerEventType.Move) {
                                                    folderInteractionTimestamp = System.currentTimeMillis()
                                                }
                                            }
                                        }
                                    }
                                    .semantics(mergeDescendants = true) {
                                        role = Role.Button
                                        this.contentDescription = a11yFolderDescription
                                        customActions = listOf(
                                            CustomAccessibilityAction(openFolderActionLabel) {
                                                currentDirectory = folder.file
                                                true
                                            },
                                            CustomAccessibilityAction(pinFolderActionLabel) {
                                                val nowPinned = repository.togglePinFolder(folder)
                                                loadContent()
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (nowPinned) "Pinned \"${folder.name}\"" else "Unpinned \"${folder.name}\""
                                                    )
                                                }
                                                true
                                            },
                                            CustomAccessibilityAction(renameFolderActionLabel) {
                                                renameFolderNameInput = folder.name
                                                folderToRename = folder
                                                true
                                            },
                                            CustomAccessibilityAction(customizeFolderActionLabel) {
                                                editFolderSelectedColor = folder.colorHex ?: (if (folder.isEmergencyFolder) DocumentRepository.EMERGENCY_SAVES_DEFAULT_COLOR else PRESET_FOLDER_COLORS.first())
                                                editFolderSelectedEmoji = folder.iconEmoji
                                                editFolderSelectedIconType = folder.iconType ?: (if (folder.isEmergencyFolder) "emergency" else (folder.role ?: "folder"))
                                                folderToEdit = folder
                                                true
                                            },
                                            CustomAccessibilityAction(deleteFolderActionLabel) {
                                                scope.launch {
                                                    repository.moveFolderToTrash(folder.file)
                                                    loadContentNow()
                                                    snackbarHostState.showSnackbar("Moved folder \"${folder.name}\" to Trash")
                                                }
                                                true
                                            }
                                        )
                                    }
                                    .clickable {
                                        currentDirectory = folder.file
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = accentColor.copy(alpha = 0.12f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (!folder.iconEmoji.isNullOrBlank()) {
                                        Text(
                                            text = folder.iconEmoji,
                                            fontSize = 28.sp
                                        )
                                    } else if (folder.iconType == "emergency" || folder.isEmergencyFolder || folder.role == "emergency") {
                                        Icon(
                                            imageVector = Icons.Default.Emergency,
                                            contentDescription = "Emergency Saves",
                                            tint = accentColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else if (folder.iconType == "import" || folder.iconType == "imported" || folder.role == "import") {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = "Imported Folder",
                                            tint = accentColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else if (folder.iconType == "audio" || folder.role == "audio") {
                                        Icon(
                                            imageVector = Icons.Default.AudioFile,
                                            contentDescription = "Audio Folder",
                                            tint = accentColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            InteractiveMarqueeText(
                                                text = folder.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                externalTrigger = folderInteractionTimestamp,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (folder.isPinned || folder.isVirtuallyPinned) {
                                                Icon(
                                                    imageVector = if (folder.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                                    contentDescription = "Pinned Folder",
                                                    tint = accentColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            if (folder.isExcludedFromRecents) {
                                                Icon(
                                                    imageVector = Icons.Default.VisibilityOff,
                                                    contentDescription = "Excluded from Recents",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${folder.itemCount} notes",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Box {
                                        val folderOptionsLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_customize_folder)
                                        AppIconButton(
                                            onClick = { showFolderMenu = true },
                                            tooltip = folderOptionsLabel,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.MoreVert, contentDescription = folderOptionsLabel, modifier = Modifier.size(18.dp))
                                        }
                                        androidx.compose.material3.DropdownMenu(
                                            expanded = showFolderMenu,
                                            onDismissRequest = { showFolderMenu = false }
                                        ) {
                                            val isPinnedOrVirtual = folder.isPinned || folder.isVirtuallyPinned
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text(if (isPinnedOrVirtual) "Unpin Folder" else "Pin Folder") },
                                                leadingIcon = {
                                                    Icon(
                                                        if (isPinnedOrVirtual) Icons.Outlined.PushPin else Icons.Default.PushPin,
                                                        contentDescription = null,
                                                        tint = if (isPinnedOrVirtual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                onClick = {
                                                    showFolderMenu = false
                                                    val nowPinned = repository.togglePinFolder(folder)
                                                    loadContent()
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            if (nowPinned) "Pinned \"${folder.name}\"" else "Unpinned \"${folder.name}\""
                                                        )
                                                    }
                                                }
                                            )
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text(if (folder.isExcludedFromRecents) "Include in Recents" else "Exclude from Recents") },
                                                leadingIcon = {
                                                    Icon(
                                                        if (folder.isExcludedFromRecents) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                        contentDescription = null,
                                                        tint = if (folder.isExcludedFromRecents) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                onClick = {
                                                    showFolderMenu = false
                                                    val newExcluded = !folder.isExcludedFromRecents
                                                    repository.setFolderExcludeFromRecents(folder.file, newExcluded)
                                                    loadContent()
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            if (newExcluded) "Excluded \"${folder.name}\" from Recents" else "Included \"${folder.name}\" in Recents"
                                                        )
                                                    }
                                                }
                                            )
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text("Rename Folder") },
                                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                                onClick = {
                                                    showFolderMenu = false
                                                    renameFolderNameInput = folder.name
                                                    folderToRename = folder
                                                }
                                            )
                                            androidx.compose.material3.DropdownMenuItem(
                                                 text = { Text("Map to Cloud...") },
                                                 leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                 onClick = {
                                                     showFolderMenu = false
                                                     folderToMapToCloud = folder
                                                 }
                                             )
                                             androidx.compose.material3.DropdownMenuItem(
                                                 text = { Text("Customize Icon & Color") },
                                                 leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                                                 onClick = {
                                                     showFolderMenu = false
                                                     editFolderSelectedColor = folder.colorHex ?: (if (folder.isEmergencyFolder) DocumentRepository.EMERGENCY_SAVES_DEFAULT_COLOR else PRESET_FOLDER_COLORS.first())
                                                     editFolderSelectedEmoji = folder.iconEmoji
                                                     editFolderSelectedIconType = folder.iconType ?: (if (folder.isEmergencyFolder) "emergency" else (folder.role ?: "folder"))
                                                     folderToEdit = folder
                                                 }
                                             )
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    showFolderMenu = false
                                                    scope.launch {
                                                        repository.moveFolderToTrash(folder.file)
                                                        loadContentNow()
                                                        snackbarHostState.showSnackbar("Moved folder \"${folder.name}\" to Trash")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notes Grid Section
                    if (displayNotes.isNotEmpty()) {
                        if (displayFolders.isNotEmpty() && !targetIsTrash) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = if (targetIsTrash) "Trashed Notes (${displayNotes.size})" else pluralStringResource(dev.ilamparithi.aournalpp.R.plurals.hub_section_notes_count, displayNotes.size, displayNotes.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.a11yHeading()
                                )
                            }
                        }

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
                                        selectedNotePaths = if (isSelected) selectedNotePaths.minus(note.path) else selectedNotePaths.plus(note.path)
                                        lastSelectedNotePath = note.path
                                    } else if (isViewingTrash) {
                                        // Trashed item tap
                                    } else if (!hasPermission) {
                                        showPermissionDialog = true
                                    } else if (note.autosaveInfo != null) {
                                        pendingAutosaveNote = note
                                    } else {
                                        openNoteInCanvas(note.file)
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
                                            selectedNotePaths = selectedNotePaths + range
                                        } else {
                                            selectedNotePaths = selectedNotePaths + note.path
                                        }
                                    } else {
                                        isSelectionMode = true
                                        selectedNotePaths = selectedNotePaths + note.path
                                    }
                                    lastSelectedNotePath = note.path
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onTogglePin = {
                                    repository.togglePinNote(note.file.absolutePath)
                                    loadContent()
                                },
                                onExportPdf = {
                                    val defaultName = FileNameTemplateEngine.evaluate(
                                        FileNameTemplateEngine.getExportPdfTemplate(context),
                                        context,
                                        note.file
                                    )
                                    activeFilePrompt = SingleFileActionPrompt(note, FileActionPromptType.EXPORT_PDF, defaultName)
                                },
                                onSharePdf = {
                                    val defaultName = FileNameTemplateEngine.evaluate(
                                        FileNameTemplateEngine.getSharePdfTemplate(context),
                                        context,
                                        note.file
                                    )
                                    activeFilePrompt = SingleFileActionPrompt(note, FileActionPromptType.SHARE_PDF, defaultName)
                                },
                                onShareXopp = {
                                    val defaultName = FileNameTemplateEngine.evaluate(
                                        FileNameTemplateEngine.getShareXoppTemplate(context),
                                        context,
                                        note.file
                                    )
                                    activeFilePrompt = SingleFileActionPrompt(note, FileActionPromptType.SHARE_XOPP, defaultName)
                                },
                                onRename = {
                                    noteToRename = note
                                    renameInputText = note.title
                                },
                                onDuplicate = {
                                    scope.launch {
                                        val result = repository.duplicateNote(note)
                                        if (result.isSuccess) {
                                            loadContentNow()
                                            snackbarHostState.showSnackbar("Duplicated \"${note.title}\"")
                                        }
                                    }
                                },
                                onDelete = { noteToDelete = note },
                                onRestore = {
                                    scope.launch {
                                        val res = repository.restoreFromTrash(note)
                                        if (res.isSuccess) {
                                            loadContentNow()
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
                                        text = if (isViewingTrash) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_trash_empty_title)
                                        else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_empty_state_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isViewingTrash) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_trash_empty_subtitle)
                                        else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_empty_state_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Multi-Select Floating Bottom Action Bar
            AnimatedVisibility(
                visible = isSelectionMode && selectedNotePaths.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 10.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val currentDisplayNotes = if (isViewingTrash) trashedNotes else notes
                    val selectedDocs = currentDisplayNotes.filter { selectedNotePaths.contains(it.path) }

                    if (isViewingTrash) {
                        // TRASH BIN MULTI-SELECTION ACTIONS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Restore Selected Action
                            Button(
                                onClick = {
                                    scope.launch {
                                        val count = repository.restoreMultipleFromTrash(selectedDocs).getOrDefault(0)
                                        isSelectionMode = false
                                        selectedNotePaths = emptySet()
                                        loadContentNow()
                                        snackbarHostState.showSnackbar("Restored $count item(s) from Trash")
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_restore),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_restore)} (${selectedDocs.size})",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 2. Delete Permanently Selected Action
                            OutlinedButton(
                                onClick = {
                                    showBatchDeletePermanentDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_delete_permanent),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_delete),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        // NORMAL MULTI-SELECTION ACTIONS
                        val allSelectedPinned = selectedDocs.isNotEmpty() && selectedDocs.all { it.isPinned }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pin / Unpin Selected Action
                            val pinActionLabel = if (allSelectedPinned) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_unpin)
                            else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_pin)
                            AppIconButton(
                                onClick = {
                                    selectedDocs.forEach { doc ->
                                        if (allSelectedPinned) {
                                            repository.unpinNote(doc.file.absolutePath)
                                        } else {
                                            repository.pinNote(doc.file.absolutePath)
                                        }
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(if (allSelectedPinned) "Unpinned selected notes from Home" else "Pinned selected notes to Home")
                                    }
                                    loadContent()
                                },
                                tooltip = pinActionLabel
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.PushPin,
                                        contentDescription = pinActionLabel,
                                        tint = if (allSelectedPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Move to Folder Action
                            val moveActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_move)
                            AppIconButton(
                                onClick = { showMoveToFolderDialog = true },
                                tooltip = moveActionLabel
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.DriveFileMove,
                                        contentDescription = moveActionLabel,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Share as PDF Action
                            val exportPdfLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_export_pdf)
                            AppIconButton(
                                onClick = {
                                    isPdfConverting = true
                                    convertingMessage = "Rendering ${selectedDocs.size} PDFs..."
                                    scope.launch {
                                        val result = repository.shareMultipleNotesAsPdf(context, selectedDocs, pdfExportManager)
                                        isPdfConverting = false
                                        if (result.isFailure) {
                                            snackbarHostState.showSnackbar("Batch share failed: ${result.exceptionOrNull()?.message}")
                                        }
                                    }
                                },
                                tooltip = exportPdfLabel
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = exportPdfLabel
                                    )
                                }
                            }

                            // Share as Notes Action
                            val shareLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_share)
                            AppIconButton(
                                onClick = {
                                    repository.shareMultipleNotesAsXopp(context, selectedDocs)
                                },
                                tooltip = shareLabel
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = shareLabel
                                    )
                                }
                            }

                            // Move to Trash Action
                            val deleteLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_delete)
                            AppIconButton(
                                onClick = {
                                    scope.launch {
                                        val count = repository.moveToTrash(selectedDocs).getOrDefault(0)
                                        isSelectionMode = false
                                        selectedNotePaths = emptySet()
                                        loadContentNow()
                                        snackbarHostState.showSnackbar("Moved $count note(s) to Trash")
                                    }
                                },
                                tooltip = deleteLabel
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = deleteLabel,
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

    // Speed Dial Scrim Overlay
    AnimatedVisibility(
        visible = isFabExpanded,
        enter = fadeIn(animationSpec = spring(stiffness = 400f)),
        exit = fadeOut(animationSpec = spring(stiffness = 400f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isFabExpanded = false }
        )
    }
}
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveNoteCard(
    note: NoteDocument,
    isGridView: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isTrashMode: Boolean,
    pdfExportManager: PdfExportManager,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTogglePin: () -> Unit,
    onExportPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onShareXopp: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val thumbnailImage by produceState<ImageBitmap?>(
        initialValue = ThumbnailManager.getCachedThumbnail(note.file),
        key1 = note.lastModifiedMs
    ) {
        value = ThumbnailManager.getOrCreateThumbnailBitmap(context, note.file, pdfExportManager)
    }

    val cardShape = RoundedCornerShape(16.dp)
    val cardFolderAccent = note.folderColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.primary

    var cardInteractionTimestamp by remember { mutableStateOf(0L) }

    val openActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_open_note)
    val pinActionLabel = if (note.isPinned) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_unpin_note) else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_pin_note)
    val exportPdfActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_export_pdf)
    val shareActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_share_note)
    val renameActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_rename)
    val duplicateActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_keep_both)
    val deleteActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_delete)
    val restoreActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_restore_note)

    val customActionsList = remember(note, isTrashMode, isSelectionMode) {
        buildList {
            add(CustomAccessibilityAction(openActionLabel) { onClick(); true })
            if (!isSelectionMode) {
                add(CustomAccessibilityAction(pinActionLabel) { onTogglePin(); true })
                add(CustomAccessibilityAction(exportPdfActionLabel) { onExportPdf(); true })
                add(CustomAccessibilityAction(shareActionLabel) { onShareXopp(); true })
                add(CustomAccessibilityAction(renameActionLabel) { onRename(); true })
                add(CustomAccessibilityAction(duplicateActionLabel) { onDuplicate(); true })
                add(CustomAccessibilityAction(deleteActionLabel) { onDelete(); true })
            }
            if (isTrashMode) {
                add(CustomAccessibilityAction(restoreActionLabel) { onRestore(); true })
            }
        }
    }

    val a11yNoteDescription = remember(note, isSelected, isSelectionMode) {
        AccessibilityUtils.buildNoteCardA11yDescription(
            title = note.title,
            fileType = note.fileType,
            folderName = note.folder,
            lastModified = note.fuzzyLastModified,
            isPinned = note.isPinned,
            isSelected = if (isSelectionMode) isSelected else null
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .pointerInput(note.path) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val hasTouch = event.changes.any { it.pressed || it.positionChanged() }
                        if (hasTouch || event.type == PointerEventType.Enter || event.type == PointerEventType.Move) {
                            cardInteractionTimestamp = System.currentTimeMillis()
                        }
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = a11yNoteDescription
                customActions = customActionsList
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = cardShape
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                .compositeOver(MaterialTheme.colorScheme.surface)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                .compositeOver(MaterialTheme.colorScheme.surface)
        )
    ) {
        if (isGridView) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Thumbnail Preview Header (4:3 aspect ratio)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.35f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailImage != null) {
                        Image(
                            bitmap = thumbnailImage!!,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = when (note.fileType) {
                                NoteFileType.PDF -> Icons.Default.PictureAsPdf
                                NoteFileType.XOJ -> Icons.Default.History
                                else -> Icons.Default.Edit
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Format Badge Overlay
                    FileTypePill(
                        fileType = note.fileType,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )

                    // Pinned Badge Overlay (Top Right, when not selecting)
                    if (note.isPinned && !isSelectionMode) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned Note",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Selection Checkbox Badge Overlay
                    if (isSelectionMode) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(26.dp)
                                .border(
                                    width = if (isSelected) 0.dp else 1.5.dp,
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                    // Autosave Available Badge (Bottom of Thumbnail, above Info section)
                    if (note.autosaveInfo != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Autosave Available",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Card Body
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InteractiveMarqueeText(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            externalTrigger = cardInteractionTimestamp,
                            modifier = Modifier.weight(1f)
                        )

                        if (!isSelectionMode && !isTrashMode) {
                            Box {
                                val moreOptionsLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_details)
                                AppIconButton(
                                    onClick = { showMenu = true },
                                    tooltip = moreOptionsLabel,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = moreOptionsLabel, modifier = Modifier.size(18.dp))
                                }
                                StandardNoteActionDropdown(
                                    expanded = showMenu,
                                    isPinned = note.isPinned,
                                    onDismiss = { showMenu = false },
                                    onTogglePin = onTogglePin,
                                    onExportPdf = onExportPdf,
                                    onSharePdf = onSharePdf,
                                    onShareXopp = onShareXopp,
                                    onRename = onRename,
                                    onDuplicate = onDuplicate,
                                    onDelete = onDelete
                                )
                            }
                        } else if (isTrashMode && onRestore != null) {
                            val restoreLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_restore)
                            AppIconButton(
                                onClick = onRestore,
                                tooltip = restoreLabel,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = restoreLabel, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InteractiveMarqueeText(
                            text = note.lastModifiedFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            externalTrigger = cardInteractionTimestamp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(note.sizeFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            // Horizontal List Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isSelectionMode) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(26.dp)
                            .border(
                                width = if (isSelected) 0.dp else 1.5.dp,
                                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (note.fileType) {
                                    NoteFileType.PDF -> Icons.Default.PictureAsPdf
                                    NoteFileType.XOJ -> Icons.Default.History
                                    else -> Icons.Default.Edit
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InteractiveMarqueeText(
                            text = note.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            externalTrigger = cardInteractionTimestamp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (note.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InteractiveMarqueeText(
                            text = note.lastModifiedFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            externalTrigger = cardInteractionTimestamp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(note.sizeFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (!isSelectionMode && !isTrashMode) {
                    Box {
                        val moreOptionsLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_details)
                        AppIconButton(
                            onClick = { showMenu = true },
                            tooltip = moreOptionsLabel
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = moreOptionsLabel)
                        }
                        StandardNoteActionDropdown(
                            expanded = showMenu,
                            isPinned = note.isPinned,
                            onDismiss = { showMenu = false },
                            onTogglePin = onTogglePin,
                            onExportPdf = onExportPdf,
                            onSharePdf = onSharePdf,
                            onShareXopp = onShareXopp,
                            onRename = onRename,
                            onDuplicate = onDuplicate,
                            onDelete = onDelete
                        )
                    }
                } else if (isTrashMode && onRestore != null) {
                    val restoreLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_restore)
                    AppIconButton(
                        onClick = onRestore,
                        tooltip = restoreLabel
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = restoreLabel, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteActionDropdown(
    expanded: Boolean,
    isPinned: Boolean = false,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onExportPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onShareXopp: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (isPinned) "Unpin from Home" else "Pin to Home") },
            leadingIcon = {
                Icon(
                    imageVector = if (isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                    contentDescription = null,
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = { onDismiss(); onTogglePin() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Export to PDF") },
            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
            onClick = { onDismiss(); onExportPdf() }
        )
        DropdownMenuItem(
            text = { Text("Share as PDF") },
            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
            onClick = { onDismiss(); onSharePdf() }
        )
        DropdownMenuItem(
            text = { Text("Share as Note") },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
            onClick = { onDismiss(); onShareXopp() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
            onClick = { onDismiss(); onRename() }
        )
        DropdownMenuItem(
            text = { Text("Duplicate") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            onClick = { onDismiss(); onDuplicate() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}

@Composable
fun AutosaveResolutionDialog(
    note: NoteDocument,
    autosaveInfo: dev.ilamparithi.aournalpp.model.AutosaveInfo,
    onDismiss: () -> Unit,
    onReplaceWithAutosave: () -> Unit,
    onKeepBoth: () -> Unit,
    onKeepExisting: () -> Unit
) {
    val isNewer = autosaveInfo.isAutosaveNewer

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Autosave Detected",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "An autosaved version was found for \"${note.title}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isNewer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (isNewer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isNewer) Icons.Default.Check else Icons.Default.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = autosaveInfo.timeDiffFormatted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("${autosaveInfo.mainModifiedFormatted} (${autosaveInfo.mainSizeFormatted})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Autosave", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isNewer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            Text("${autosaveInfo.autosaveModifiedFormatted} (${autosaveInfo.autosaveSizeFormatted})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isNewer) {
                Button(
                    onClick = onReplaceWithAutosave,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Replace with Autosave", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onKeepExisting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Keep Existing", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onKeepBoth) {
                    Text("Keep Both")
                }
                if (isNewer) {
                    TextButton(onClick = onKeepExisting) {
                        Text("Keep Existing")
                    }
                } else {
                    OutlinedButton(
                        onClick = onReplaceWithAutosave,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Replace with Autosave", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    )
}

fun Modifier.notesGridDragSelect(
    lazyGridState: LazyGridState,
    notes: () -> List<NoteDocument>,
    selectedPaths: () -> Set<String>,
    setSelectedPaths: (Set<String>) -> Unit,
    lastSelectedPath: () -> String?,
    setLastSelectedPath: (String?) -> Unit,
    isSelectionMode: () -> Boolean,
    setIsSelectionMode: (Boolean) -> Unit,
    setIsDragSelecting: (Boolean) -> Unit = {},
    setIsInitialEntryDrag: (Boolean) -> Unit = {},
    hapticFeedback: HapticFeedback,
    autoScrollThreshold: Float,
    setAutoScrollSpeed: (Float) -> Unit
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val downEvent = awaitPointerEvent(PointerEventPass.Initial)
            val down = downEvent.changes.firstOrNull { it.pressed } ?: continue
            val downId = down.id
            val downPos = down.position

            val initialList = notes()
            val initialPathMap = initialList.mapIndexed { idx, doc -> doc.path to idx }.toMap()

            fun findNotePathAt(point: Offset): String? {
                val rounded = point.round()
                val match = lazyGridState.layoutInfo.visibleItemsInfo.find { itemInfo ->
                    itemInfo.size.toIntRect().contains(rounded - itemInfo.offset)
                }
                val key = match?.key as? String ?: return null
                return if (initialPathMap.containsKey(key)) key else null
            }

            val hitPath = findNotePathAt(downPos) ?: continue

            var isLongPressed = false
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis
            val touchSlop = viewConfiguration.touchSlop
            val pointerType = down.type
            val effectiveSlop = if (pointerType == PointerType.Stylus || pointerType == PointerType.Eraser) {
                touchSlop * 2.5f
            } else {
                touchSlop
            }

            val dragCancelled = withTimeoutOrNull(longPressTimeout) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == downId } ?: break
                    if (!change.pressed) {
                        return@withTimeoutOrNull true
                    }
                    val dist = (change.position - downPos).getDistance()
                    if (dist > effectiveSlop) {
                        return@withTimeoutOrNull true
                    }
                }
                true
            }

            if (dragCancelled == null) {
                isLongPressed = true
            }

            if (isLongPressed) {
                val activeSelection = isSelectionMode()
                val isFirstEntry = !activeSelection
                if (isFirstEntry) {
                    setIsInitialEntryDrag(true)
                }
                setIsDragSelecting(true)
                val currentNotesList = initialList
                val pathToIndex = initialPathMap
                val currentSelected = selectedPaths()

                val wasSelected = currentSelected.contains(hitPath)
                val initialPath = hitPath
                var lastReportedPath = hitPath
                val baseSnapshot: Set<String>

                if (activeSelection && !wasSelected && currentSelected.isNotEmpty()) {
                    // Shift-click range selection when long-pressing a deselected item in selection mode
                    val anchorPath = lastSelectedPath() ?: currentSelected.lastOrNull()
                    val anchorIdx = pathToIndex[anchorPath] ?: -1
                    val hitIdx = pathToIndex[hitPath] ?: -1

                    val rangePaths = if (anchorIdx >= 0 && hitIdx >= 0) {
                        val start = minOf(anchorIdx, hitIdx)
                        val end = maxOf(anchorIdx, hitIdx)
                        currentNotesList.subList(start, end + 1).map { it.path }.toSet()
                    } else {
                        setOf(hitPath)
                    }

                    val newSelection = currentSelected + rangePaths
                    setSelectedPaths(newSelection)
                    setLastSelectedPath(hitPath)
                    baseSnapshot = newSelection
                } else {
                    // Standard selection mode entry / start drag
                    setIsSelectionMode(true)
                    val newSelection = currentSelected + hitPath
                    setSelectedPaths(newSelection)
                    setLastSelectedPath(hitPath)
                    baseSnapshot = currentSelected
                }

                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                try {
                    while (true) {
                        val dragEvent = awaitPointerEvent(PointerEventPass.Initial)
                        val dragChange = dragEvent.changes.firstOrNull { it.id == downId } ?: break
                        if (!dragChange.pressed) {
                            dragChange.consume()
                            break
                        }
                        dragChange.consume()
                        val currentDragPos = dragChange.position

                        val viewportHeight = lazyGridState.layoutInfo.viewportSize.height
                        val distFromBottom = viewportHeight - currentDragPos.y
                        val distFromTop = currentDragPos.y
                        setAutoScrollSpeed(
                            when {
                                distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                                distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                                else -> 0f
                            }
                        )

                        val currentHit = findNotePathAt(currentDragPos)
                        if (currentHit != null && currentHit != lastReportedPath) {
                            val initialIdx = pathToIndex[initialPath] ?: -1
                            val currentIdx = pathToIndex[currentHit] ?: -1

                            if (initialIdx >= 0 && currentIdx >= 0) {
                                val start = minOf(initialIdx, currentIdx)
                                val end = maxOf(initialIdx, currentIdx)
                                val dragRange = currentNotesList.subList(start, end + 1).map { it.path }.toSet()

                                val updatedSelection = baseSnapshot + dragRange
                                setSelectedPaths(updatedSelection)
                                setLastSelectedPath(currentHit)
                                lastReportedPath = currentHit
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                } finally {
                    setAutoScrollSpeed(0f)
                    setIsDragSelecting(false)
                    setIsInitialEntryDrag(false)
                }
            }
        }
    }
}

/**
 * Dynamic horizontal multi-browse carousel.
 * Powered by Material 3 HorizontalMultiBrowseCarousel for authentic, adaptive responsive cards,
 * buttery smooth gesture physics, and zero rubberband scroll contention.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicRecentsCarousel(
    recentNotes: List<NoteDocument>,
    pdfExportManager: PdfExportManager,
    onOpenNote: (NoteDocument) -> Unit,
    onTogglePin: (NoteDocument) -> Unit,
    onExportPdf: (NoteDocument) -> Unit,
    onSharePdf: (NoteDocument) -> Unit,
    onShareXopp: (NoteDocument) -> Unit,
    onDuplicate: ((NoteDocument) -> Unit)? = null,
    onDeleteNote: (NoteDocument) -> Unit,
    onRenameNote: (NoteDocument) -> Unit
) {
    if (recentNotes.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.a11yHeading()
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_title_recent_notes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "${recentNotes.size} recent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            items(recentNotes, key = { it.path }) { note ->
                Box(
                    modifier = Modifier
                        .width(230.dp)
                        .fillMaxHeight()
                ) {
                    StandardNoteCard(
                        note = note,
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.extraLarge,
                        pdfExportManager = pdfExportManager,
                        onClick = { onOpenNote(note) },
                        onTogglePin = { onTogglePin(note) },
                        onExportPdf = { onExportPdf(note) },
                        onSharePdf = { onSharePdf(note) },
                        onShareXopp = { onShareXopp(note) },
                        onRename = { onRenameNote(note) },
                        onDuplicate = onDuplicate?.let { { it(note) } },
                        onDelete = { onDeleteNote(note) }
                    )
                }
            }
        }
    }
}
