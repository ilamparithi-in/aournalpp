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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import kotlinx.coroutines.delay
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
    var showHiddenFiles by remember { mutableStateOf(prefs.getBoolean("pref_show_hidden_files", false)) }
    var isGridView by remember { mutableStateOf(prefs.getBoolean("pref_is_grid_view", true)) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Directory navigation
    var currentDirectory by remember { mutableStateOf(repository.getRootNotesDirectory()) }
    var folders by remember { mutableStateOf<List<FolderItem>>(emptyList()) }
    var notes by remember { mutableStateOf<List<NoteDocument>>(emptyList()) }
    var trashedNotes by remember { mutableStateOf<List<NoteDocument>>(emptyList()) }
    var isViewingTrash by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    val recentNotes by produceState<List<NoteDocument>>(emptyList(), notes, folders, isViewingTrash) {
        value = if (!isViewingTrash) repository.getAllRecentNotes(10) else emptyList()
    }

    // Multi-Selection State & Drag Selection Tracker
    var isSelectionMode by remember { mutableStateOf(false) }
    var isDragSelecting by remember { mutableStateOf(false) }
    var isInitialEntryDrag by remember { mutableStateOf(false) }
    var selectedNotePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
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

    // Emergency recovery
    var quarantinedEmergencySave by remember { mutableStateOf<File?>(null) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showEmergencySaveNameDialog by remember { mutableStateOf(false) }
    var emergencySaveNameInput by remember { mutableStateOf("") }
    var emergencySaveTargetFolder by remember { mutableStateOf(repository.getRootNotesDirectory()) }

    // Autosave on-open resolution state
    var pendingAutosaveNote by remember { mutableStateOf<NoteDocument?>(null) }
    var pendingSaveAutosaveNote by remember { mutableStateOf<NoteDocument?>(null) }

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

    // SAF Import PDF Launcher
    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = ExternalFileHandler.stageExternalUri(context, uri, env)
                if (result.isSuccess) {
                    val staged = result.getOrThrow()
                    repository.recordNoteOpened(staged.absolutePath)
                    val intent = Intent(context, CanvasActivity::class.java).apply {
                        putExtra(CanvasActivity.EXTRA_NOTE_PATH, staged.absolutePath)
                    }
                    context.startActivity(intent)
                } else {
                    snackbarHostState.showSnackbar("Failed to import PDF: ${result.exceptionOrNull()?.message}")
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

            val emergencyFile = withContext(Dispatchers.IO) { env.checkAndQuarantineEmergencySave() }
            if (emergencyFile != null && emergencyFile.exists() && emergencyFile.length() > 0) {
                if (quarantinedEmergencySave == null && !showEmergencySaveNameDialog) {
                    quarantinedEmergencySave = emergencyFile
                    showEmergencyDialog = true
                }
            }
        }
    }

    fun loadContent() {
        scope.launch { loadContentNow() }
    }

    val localView = LocalView.current
    fun openNoteInCanvas(noteFile: File) {
        repository.recordNoteOpened(noteFile.absolutePath)
        val intent = Intent(context, CanvasActivity::class.java).apply {
            putExtra(CanvasActivity.EXTRA_NOTE_PATH, noteFile.absolutePath)
        }
        val options = ActivityOptionsCompat.makeClipRevealAnimation(
            localView,
            localView.width / 2,
            localView.height / 2,
            localView.width / 4,
            localView.height / 4
        ).toBundle()
        context.startActivity(intent, options)
    }

    // Handle Back Press in Subfolders or Selection Mode
    BackHandler(enabled = isSelectionMode || isViewingTrash || currentDirectory.canonicalPath != repository.getRootNotesDirectory().canonicalPath) {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedNotePaths = emptySet()
        } else if (isViewingTrash) {
            isViewingTrash = false
            loadContent()
        } else if (currentDirectory.canonicalPath != repository.getRootNotesDirectory().canonicalPath) {
            currentDirectory = currentDirectory.parentFile ?: repository.getRootNotesDirectory()
            loadContent()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasStoragePermission(context)
                loadContent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasPermission, showHiddenFiles, searchQuery, currentDirectory, isViewingTrash) {
        // Debounce search input.
        if (searchQuery.isNotEmpty()) delay(SEARCH_DEBOUNCE_MS)
        loadContentNow()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                // Contextual Multi-Selection Top Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "${selectedNotePaths.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val selectedDocs = notes.filter { selectedNotePaths.contains(it.path) }
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
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedNotePaths = emptySet()
                            lastSelectedNotePath = null
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Selection")
                        }
                    },
                    actions = {
                        val allSelected = selectedNotePaths.size == notes.size && notes.isNotEmpty()
                        IconButton(onClick = {
                            if (allSelected) {
                                selectedNotePaths = emptySet()
                            } else {
                                selectedNotePaths = notes.map { it.path }.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deselect All" else "Select All"
                            )
                        }
                        IconButton(onClick = {
                            val allPaths = notes.map { it.path }.toSet()
                            selectedNotePaths = allPaths.minus(selectedNotePaths)
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Invert Selection")
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
                            placeholder = { Text("Search notes and folders...") },
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
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
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
                                text = if (isViewingTrash) "Trash Bin" else "Aournal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        if (isViewingTrash) {
                            IconButton(onClick = {
                                isViewingTrash = false
                                loadContent()
                            }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Notes")
                            }
                        } else if (currentDirectory.canonicalPath != repository.getRootNotesDirectory().canonicalPath) {
                            IconButton(onClick = {
                                currentDirectory = currentDirectory.parentFile ?: repository.getRootNotesDirectory()
                                loadContent()
                            }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up Directory")
                            }
                        }
                    },
                    actions = {
                        if (!isViewingTrash) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                            }

                            IconButton(onClick = {
                                val updated = !isGridView
                                isGridView = updated
                                prefs.edit().putBoolean("pref_is_grid_view", updated).apply()
                            }) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                    contentDescription = "Switch View"
                                )
                            }
                        }

                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            if (!isViewingTrash) {
                                DropdownMenuItem(
                                    text = { Text("Select Notes") },
                                    leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        isSelectionMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New Folder") },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        newFolderNameInput = ""
                                        showNewFolderDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (showHiddenFiles) "Hide Hidden & Backup Files" else "Show Hidden Files") },
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
                                    text = { Text("Trash Bin") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        isViewingTrash = true
                                        loadContent()
                                    }
                                )
                                HorizontalDivider()
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Empty Trash", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showTopMenu = false
                                        scope.launch {
                                            repository.emptyTrash()
                                            snackbarHostState.showSnackbar("Emptied Trash")
                                            loadContent()
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }

                            DropdownMenuItem(
                                text = { Text("Open Source Licenses") },
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
                                text = { Text("Settings") },
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
                FloatingActionButton(
                    onClick = {
                        if (!hasPermission) {
                            showPermissionDialog = true
                        } else {
                            val intent = Intent(context, CanvasActivity::class.java)
                            context.startActivity(intent)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New Note")
                }
            }
        }
    ) { innerPadding ->

        // 1. Storage Permission Prompt Dialog
        if (showPermissionDialog && !hasPermission) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.FolderShared,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text("Storage Permission Required", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Xournal++ saves notes and exports directly to your device storage (${env.getNotesDirectory().absolutePath}). Please grant All Files Access.")
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
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) { Text("Later") }
                }
            )
        }

        // 2. M3 Standard Progress Dialog during PDF Export/Share
        if (isPdfConverting) {
            AlertDialog(
                onDismissRequest = {},
                icon = {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                },
                title = { Text("Processing Document", fontWeight = FontWeight.Bold) },
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

        // 3. Create Folder Dialog
        if (showNewFolderDialog) {
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("New Folder", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newFolderNameInput,
                            onValueChange = { newFolderNameInput = it },
                            label = { Text("Folder Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Folder Icon / Emoji:", style = MaterialTheme.typography.labelMedium)
                            FolderIconPickerRow(
                                selectedEmoji = selectedFolderEmoji,
                                selectedIconType = selectedFolderIconType,
                                onIconSelected = { emoji, iconType ->
                                    selectedFolderEmoji = emoji
                                    selectedFolderIconType = iconType
                                }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Folder Color Accent:", style = MaterialTheme.typography.labelMedium)
                            FolderColorPickerRow(
                                selectedColorHex = selectedFolderColor,
                                onColorSelected = { selectedFolderColor = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newFolderNameInput.isNotBlank()) {
                            showNewFolderDialog = false
                            val result = repository.createFolder(currentDirectory, newFolderNameInput, selectedFolderColor, selectedFolderEmoji, selectedFolderIconType)
                            scope.launch {
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar("Created folder \"${newFolderNameInput.trim()}\"")
                                    loadContent()
                                } else {
                                    snackbarHostState.showSnackbar("Failed: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
                }
            )
        }

        // 4. Edit Folder Appearance Dialog (Icon & Color)
        folderToEdit?.let { folder ->
            AlertDialog(
                onDismissRequest = { folderToEdit = null },
                icon = { Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Customize \"${folder.name}\"", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Folder Icon / Emoji:", style = MaterialTheme.typography.labelMedium)
                            FolderIconPickerRow(
                                selectedEmoji = editFolderSelectedEmoji,
                                selectedIconType = editFolderSelectedIconType,
                                onIconSelected = { emoji, iconType ->
                                    editFolderSelectedEmoji = emoji
                                    editFolderSelectedIconType = iconType
                                }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Folder Color Accent:", style = MaterialTheme.typography.labelMedium)
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
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToEdit = null }) { Text("Cancel") }
                }
            )
        }

        // 4b. Rename Folder Dialog
        folderToRename?.let { folder ->
            AlertDialog(
                onDismissRequest = { folderToRename = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text("Rename Folder", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = renameFolderNameInput,
                            onValueChange = { renameFolderNameInput = it },
                            label = { Text("Folder Name") },
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
                                    snackbarHostState.showSnackbar("Renamed folder to \"${renameFolderNameInput.trim()}\"")
                                    loadContent()
                                } else {
                                    snackbarHostState.showSnackbar("Failed to rename: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    }) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToRename = null }) {
                        Text("Cancel")
                    }
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
                icon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
                title = { Text("Move ${selectedDocs.size} Note(s)", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Select destination folder:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (isCreatingInlineFolder) {
                            OutlinedTextField(
                                value = inlineFolderName,
                                onValueChange = { inlineFolderName = it },
                                label = { Text("New Folder Name") },
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
                                TextButton(onClick = { isCreatingInlineFolder = false }) { Text("Cancel") }
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
                                                snackbarHostState.showSnackbar("Moved $count note(s) to \"${dest.name}\"")
                                                loadContent()
                                            }
                                        }
                                    }
                                }) { Text("Create & Move") }
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
                                                    snackbarHostState.showSnackbar("Moved $count note(s) to Notes Root")
                                                    loadContent()
                                                }
                                            }
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Notes Home", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
                                                    snackbarHostState.showSnackbar("Moved $count note(s) to \"${folder.name}\"")
                                                    loadContent()
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
                                            Text("${folder.itemCount} notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        Text("Create New Folder")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showMoveToFolderDialog = false }) { Text("Cancel") }
                }
            )
        }

        // 6. Rename Dialog
        noteToRename?.let { doc ->
            AlertDialog(
                onDismissRequest = { noteToRename = null },
                title = { Text("Rename Note", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        label = { Text("Title") },
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
                                        snackbarHostState.showSnackbar("Renamed note successfully")
                                        loadContent()
                                    } else {
                                        snackbarHostState.showSnackbar("Rename failed: ${result.exceptionOrNull()?.message}")
                                    }
                                }
                            }
                        }
                    }) { Text("Rename") }
                },
                dismissButton = { TextButton(onClick = { noteToRename = null }) { Text("Cancel") } }
            )
        }

        // 7. Delete / Move to Trash Dialog
        noteToDelete?.let { doc ->
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Move to Trash?", fontWeight = FontWeight.Bold) },
                text = { Text("Move \"${doc.title}\" to Trash? You can restore it later.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            val target = noteToDelete
                            noteToDelete = null
                            target?.let { note ->
                                scope.launch {
                                    repository.deleteNote(note)
                                    snackbarHostState.showSnackbar("Moved \"${note.title}\" to Trash")
                                    loadContent()
                                }
                            }
                        }
                    ) { Text("Move to Trash") }
                },
                dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Cancel") } }
            )
        }

        // 8. Emergency Recovery Launch Dialog
        if (showEmergencyDialog && quarantinedEmergencySave != null) {
            val file = quarantinedEmergencySave!!
            val dateStr = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date(file.lastModified()))

            AlertDialog(
                onDismissRequest = { showEmergencyDialog = false },
                icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
                title = { Text("Unsaved Session Recovered", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Xournal++ closed unexpectedly during a previous session. An emergency recovery copy from $dateStr was saved.")
                },
                confirmButton = {
                    Button(onClick = {
                        showEmergencyDialog = false
                        val staged = repository.openEmergencyRecoverySession(file)
                        quarantinedEmergencySave = null
                        loadContent()
                        openNoteInCanvas(staged)
                    }) { Text("Open Now") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            showEmergencyDialog = false
                            val defaultName = "Recovered_Note_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(file.lastModified()))
                            emergencySaveNameInput = defaultName
                            emergencySaveTargetFolder = currentDirectory
                            showEmergencySaveNameDialog = true
                        }) { Text("Save as Note") }
                        TextButton(onClick = {
                            showEmergencyDialog = false
                            repository.discardEmergencyRecovery()
                            quarantinedEmergencySave = null
                            loadContent()
                        }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
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
                title = "Save Recovered Note",
                subtitle = "Choose a name and destination folder for the recovered note.",
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

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    loadContent()
                    delay(500)
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
                                loadContent()
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
            ) { _ ->
                LazyVerticalGrid(
                    state = gridState,
                    columns = if (isGridView) GridCells.Adaptive(minSize = 200.dp) else GridCells.Fixed(1),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .notesGridDragSelect(
                            lazyGridState = gridState,
                            notes = { if (isViewingTrash) trashedNotes else notes },
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
                    if (isRoot && !isViewingTrash && searchQuery.isBlank() && recentNotes.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "recents_carousel_section") {
                            AnimatedVisibility(
                                visible = !isSelectionMode || isInitialEntryDrag,
                                enter = expandVertically(
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = 380f
                                    )
                                ) + fadeIn(
                                    animationSpec = spring(
                                        dampingRatio = 0.9f,
                                        stiffness = 400f
                                    )
                                ),
                                exit = shrinkVertically(
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = 380f
                                    )
                                ) + fadeOut(
                                    animationSpec = spring(
                                        dampingRatio = 0.9f,
                                        stiffness = 400f
                                    )
                                )
                            ) {
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
                                        scope.launch {
                                            val result = repository.shareNoteAsPdf(context, note, pdfExportManager)
                                            if (result.isFailure) {
                                                snackbarHostState.showSnackbar("Failed to share PDF: ${result.exceptionOrNull()?.message}")
                                            }
                                        }
                                    },
                                    onShareXopp = { note -> repository.shareNoteAsXopp(context, note) },
                                    onDeleteNote = { note -> noteToDelete = note },
                                    onRenameNote = { note ->
                                        noteToRename = note
                                        renameInputText = note.title
                                    }
                                )
                            }
                        }
                    }

                    // Subfolders Section (if any exist)
                    if (folders.isNotEmpty() && !isViewingTrash) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "Folders (${folders.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(folders, key = { it.file.absolutePath }) { folder ->
                            var showFolderMenu by remember { mutableStateOf(false) }
                            val accentColor = folder.colorHex?.let {
                                try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
                            } ?: MaterialTheme.colorScheme.primary

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentDirectory = folder.file
                                        loadContent()
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
                                    } else if (folder.iconType == "emergency" || folder.isEmergencyFolder) {
                                        Icon(
                                            imageVector = Icons.Default.Emergency,
                                            contentDescription = "Emergency Saves",
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
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${folder.itemCount} notes",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Box {
                                        IconButton(onClick = { showFolderMenu = true }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                        androidx.compose.material3.DropdownMenu(
                                            expanded = showFolderMenu,
                                            onDismissRequest = { showFolderMenu = false }
                                        ) {
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
                                                text = { Text("Customize Icon & Color") },
                                                leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                                                onClick = {
                                                    showFolderMenu = false
                                                    editFolderSelectedColor = folder.colorHex ?: (if (folder.isEmergencyFolder) DocumentRepository.EMERGENCY_SAVES_DEFAULT_COLOR else PRESET_FOLDER_COLORS.first())
                                                    editFolderSelectedEmoji = folder.iconEmoji
                                                    editFolderSelectedIconType = folder.iconType ?: (if (folder.isEmergencyFolder) "emergency" else "folder")
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
                                                        snackbarHostState.showSnackbar("Moved folder \"${folder.name}\" to Trash")
                                                        loadContent()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Documents Section
                    val currentNotes = if (isViewingTrash) trashedNotes else notes

                    if (currentNotes.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = if (isViewingTrash) "Trashed Notes (${currentNotes.size})" else "Notes (${currentNotes.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(currentNotes, key = { it.path }) { note ->
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
                                    if (!isViewingTrash) {
                                        if (isSelectionMode && !isSelected && selectedNotePaths.isNotEmpty()) {
                                            val lastPath = lastSelectedNotePath ?: selectedNotePaths.lastOrNull()
                                            val currentList = currentNotes.map { it.path }
                                            val lastIdx = currentList.indexOf(lastPath)
                                            val currentIdx = currentList.indexOf(note.path)
                                            if (lastIdx >= 0 && currentIdx >= 0) {
                                                val start = minOf(lastIdx, currentIdx)
                                                val end = maxOf(lastIdx, currentIdx)
                                                val range = currentNotes.subList(start, end + 1).map { it.path }.toSet()
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
                                    }
                                },
                                onTogglePin = {
                                    repository.togglePinNote(note.file.absolutePath)
                                    loadContent()
                                },
                                onExportPdf = {
                                    pendingExportNote = note
                                    exportPdfLauncher.launch("${note.title}.pdf")
                                },
                                onSharePdf = {
                                    isPdfConverting = true
                                    convertingMessage = "Rendering PDF for sharing..."
                                    scope.launch {
                                        val result = repository.shareNoteAsPdf(context, note, pdfExportManager)
                                        isPdfConverting = false
                                        if (result.isFailure) {
                                            snackbarHostState.showSnackbar("Failed to share PDF: ${result.exceptionOrNull()?.message}")
                                        }
                                    }
                                },
                                onShareXopp = { repository.shareNoteAsXopp(context, note) },
                                onRename = {
                                    noteToRename = note
                                    renameInputText = note.title
                                },
                                onDuplicate = {
                                    scope.launch {
                                        val result = repository.duplicateNote(note)
                                        if (result.isSuccess) {
                                            snackbarHostState.showSnackbar("Duplicated \"${note.title}\"")
                                            loadContent()
                                        }
                                    }
                                },
                                onDelete = { noteToDelete = note },
                                onRestore = {
                                    scope.launch {
                                        val res = repository.restoreFromTrash(note)
                                        if (res.isSuccess) {
                                            snackbarHostState.showSnackbar("Restored \"${note.title}\"")
                                            loadContent()
                                        }
                                    }
                                }
                            )
                        }
                    } else if (folders.isEmpty()) {
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
                                        text = if (isViewingTrash) "Trash is empty" else "No notes found",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isViewingTrash) "Deleted notes will appear here" else "Tap + to create your first note",
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
                    val selectedDocs = notes.filter { selectedNotePaths.contains(it.path) }
                    val allSelectedPinned = selectedDocs.isNotEmpty() && selectedDocs.all { it.isPinned }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pin / Unpin Selected Action
                        IconButton(onClick = {
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
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = if (allSelectedPinned) "Unpin" else "Pin to Home",
                                    tint = if (allSelectedPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Move to Folder Action
                        IconButton(onClick = { showMoveToFolderDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to Folder", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Share as PDF Action
                        IconButton(onClick = {
                            isPdfConverting = true
                            convertingMessage = "Rendering ${selectedDocs.size} PDFs..."
                            scope.launch {
                                val result = repository.shareMultipleNotesAsPdf(context, selectedDocs, pdfExportManager)
                                isPdfConverting = false
                                if (result.isFailure) {
                                    snackbarHostState.showSnackbar("Batch share failed: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Share as PDF")
                            }
                        }

                        // Share as Notes Action
                        IconButton(onClick = {
                            repository.shareMultipleNotesAsXopp(context, selectedDocs)
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Share, contentDescription = "Share Notes")
                            }
                        }

                        // Move to Trash Action
                        IconButton(onClick = {
                            scope.launch {
                                val count = repository.moveToTrash(selectedDocs).getOrDefault(0)
                                snackbarHostState.showSnackbar("Moved $count note(s) to Trash")
                                isSelectionMode = false
                                selectedNotePaths = emptySet()
                                loadContent()
                            }
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Delete, contentDescription = "Trash Selected", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
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
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (!isSelectionMode && !isTrashMode) {
                            Box {
                                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            IconButton(onClick = onRestore, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(note.lastModifiedFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (note.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        }
                    }
                    Text("${note.lastModifiedFormatted} · ${note.sizeFormatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (!isSelectionMode && !isTrashMode) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
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
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
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
                    imageVector = Icons.Default.PushPin,
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
                Button(onClick = onReplaceWithAutosave) {
                    Text("Replace with Autosave")
                }
            } else {
                OutlinedButton(onClick = onReplaceWithAutosave) {
                    Text("Replace with Autosave")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onKeepBoth) {
                    Text("Keep Both")
                }
                TextButton(onClick = onKeepExisting) {
                    Text("Keep Existing")
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
    fun findNotePathAtOffset(point: Offset): String? {
        val rounded = point.round()
        val match = lazyGridState.layoutInfo.visibleItemsInfo.find { itemInfo ->
            itemInfo.size.toIntRect().contains(rounded - itemInfo.offset)
        }
        val key = match?.key as? String ?: return null
        val currentList = notes()
        return if (currentList.any { it.path == key }) key else null
    }

    awaitPointerEventScope {
        while (true) {
            val downEvent = awaitPointerEvent(PointerEventPass.Initial)
            val down = downEvent.changes.firstOrNull { it.pressed } ?: continue
            val downId = down.id
            val downPos = down.position
            val hitPath = findNotePathAtOffset(downPos)

            if (hitPath == null) {
                continue
            }

            var isLongPressed = false

            val longPressTimeout = viewConfiguration.longPressTimeoutMillis
            val touchSlop = viewConfiguration.touchSlop

            val dragCancelled = withTimeoutOrNull(longPressTimeout) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == downId } ?: break
                    if (!change.pressed) {
                        return@withTimeoutOrNull true
                    }
                    val dist = (change.position - downPos).getDistance()
                    if (dist > touchSlop) {
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
                val currentNotesList = notes()
                val currentSelected = selectedPaths()

                val wasSelected = currentSelected.contains(hitPath)
                val initialPath = hitPath
                var lastReportedPath = hitPath
                val baseSnapshot: Set<String>

                if (activeSelection && !wasSelected && currentSelected.isNotEmpty()) {
                    // Shift-click range selection when long-pressing a deselected item in selection mode
                    val anchorPath = lastSelectedPath() ?: currentSelected.lastOrNull()
                    val anchorIdx = currentNotesList.indexOfFirst { it.path == anchorPath }
                    val hitIdx = currentNotesList.indexOfFirst { it.path == hitPath }

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

                        val currentHit = findNotePathAtOffset(currentDragPos)
                        if (currentHit != null && currentHit != lastReportedPath) {
                            val initialIdx = currentNotesList.indexOfFirst { it.path == initialPath }
                            val currentIdx = currentNotesList.indexOfFirst { it.path == currentHit }

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
    onSharePdf: (NoteDocument) -> Unit,
    onShareXopp: (NoteDocument) -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Recent Files",
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

        androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel(
            state = androidx.compose.material3.carousel.rememberCarouselState { recentNotes.size },
            preferredItemWidth = 230.dp,
            itemSpacing = 10.dp,
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) { index ->
            val note = recentNotes[index]
            StandardNoteCard(
                note = note,
                modifier = Modifier
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .fillMaxSize(),
                shape = MaterialTheme.shapes.extraLarge,
                pdfExportManager = pdfExportManager,
                onClick = { onOpenNote(note) },
                onTogglePin = { onTogglePin(note) },
                onSharePdf = { onSharePdf(note) },
                onShareXopp = { onShareXopp(note) },
                onRename = { onRenameNote(note) },
                onDelete = { onDeleteNote(note) }
            )
        }
    }
}
