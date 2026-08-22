package dev.ilamparithi.aournalpp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import dev.ilamparithi.aournalpp.utils.ThumbnailManager
import kotlinx.coroutines.launch
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

    // Multi-Selection State & Bounds Tracker for Drag Selection
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNotePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    val cardBoundsMap = remember { mutableStateMapOf<String, Rect>() }

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
    var folderToEditColor by remember { mutableStateOf<FolderItem?>(null) }
    var showMoveToFolderDialog by remember { mutableStateOf(false) }

    // Emergency recovery
    var quarantinedEmergencySave by remember { mutableStateOf<File?>(null) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showEmergencySaveNameDialog by remember { mutableStateOf(false) }
    var emergencySaveNameInput by remember { mutableStateOf("") }

    // Autosave on-open resolution state
    var pendingAutosaveNote by remember { mutableStateOf<NoteDocument?>(null) }

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

    fun loadContent() {
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

            val emergencyFile = env.checkAndQuarantineEmergencySave()
            if (emergencyFile != null && emergencyFile.exists() && emergencyFile.length() > 0) {
                quarantinedEmergencySave = emergencyFile
                showEmergencyDialog = true
            }
        }
    }

    fun openNoteInCanvas(noteFile: File) {
        prefs.edit().putString("pref_last_opened_note_path", noteFile.absolutePath).apply()
        val intent = Intent(context, CanvasActivity::class.java).apply {
            putExtra(CanvasActivity.EXTRA_NOTE_PATH, noteFile.absolutePath)
        }
        context.startActivity(intent)
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
        loadContent()
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
                        Text("Folder Color Accent:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PRESET_FOLDER_COLORS) { colorHex ->
                                val color = Color(android.graphics.Color.parseColor(colorHex))
                                val isSelected = colorHex == selectedFolderColor
                                Surface(
                                    shape = CircleShape,
                                    color = color,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { selectedFolderColor = colorHex }
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                ) {
                                    if (isSelected) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newFolderNameInput.isNotBlank()) {
                            showNewFolderDialog = false
                            val result = repository.createFolder(currentDirectory, newFolderNameInput, selectedFolderColor)
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

        // 4. Edit Folder Color Dialog
        folderToEditColor?.let { folder ->
            AlertDialog(
                onDismissRequest = { folderToEditColor = null },
                icon = { Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Folder Accent Color", fontWeight = FontWeight.Bold) },
                text = {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PRESET_FOLDER_COLORS) { colorHex ->
                            val color = Color(android.graphics.Color.parseColor(colorHex))
                            val isSelected = colorHex == folder.colorHex
                            Surface(
                                shape = CircleShape,
                                color = color,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        val target = folderToEditColor
                                        folderToEditColor = null
                                        target?.let {
                                            repository.setFolderColor(it.file, colorHex)
                                            loadContent()
                                        }
                                    }
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { folderToEditColor = null }) { Text("Cancel") }
                }
            )
        }

        // 5. Move to Folder Dialog
        if (showMoveToFolderDialog) {
            val selectedDocs = notes.filter { selectedNotePaths.contains(it.path) }
            val allAvailableFolders = remember { repository.getAllFolders() }
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
                                            Icon(Icons.Default.Folder, contentDescription = null, tint = fColor, modifier = Modifier.size(20.dp))
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
                            showEmergencySaveNameDialog = true
                        }) { Text("Save to Notes") }
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
                        val target = repository.keepBoth(note)
                        pendingAutosaveNote = null
                        loadContent()
                        openNoteInCanvas(target)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
            val lastOpenedPath = prefs.getString("pref_last_opened_note_path", null)
            val continueNote = remember(lastOpenedPath, notes, folders) {
                if (lastOpenedPath != null) {
                    val f = File(lastOpenedPath)
                    if (f.exists() && !f.absolutePath.contains("/.Trash/")) {
                        repository.getNoteDocumentForFile(f)
                    } else null
                } else {
                    repository.getAllRecentNotes(1).firstOrNull()
                }
            }

            LazyVerticalGrid(
                columns = if (isGridView) GridCells.Adaptive(minSize = 200.dp) else GridCells.Fixed(1),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(notes, isSelectionMode) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                val hit = cardBoundsMap.entries.firstOrNull { it.value.contains(offset) }
                                if (hit != null) {
                                    isSelectionMode = true
                                    selectedNotePaths = selectedNotePaths + hit.key
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            onDrag = { change, _ ->
                                val currentPos = change.position
                                val hit = cardBoundsMap.entries.firstOrNull { it.value.contains(currentPos) }
                                if (hit != null && !selectedNotePaths.contains(hit.key)) {
                                    selectedNotePaths = selectedNotePaths + hit.key
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                    }
            ) {
                // M3E Dynamic Greeting & Quick Actions Header (only in Root & not searching)
                if (isRoot && !isViewingTrash && searchQuery.isBlank() && !isSelectionMode) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text(
                                    text = getGreeting(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ready to create and annotate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Quick Actions Pill Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val intent = Intent(context, CanvasActivity::class.java)
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("New Note", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { importPdfLauncher.launch(arrayOf("application/pdf")) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import PDF", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            newFolderNameInput = ""
                                            showNewFolderDialog = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Folder", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }

                            // Hero Recent Note Resume Card (Global Tracked)
                            continueNote?.let { recent ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { openNoteInCanvas(recent.file) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(14.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Continue Editing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            Text(recent.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${recent.folder} · Modified ${recent.lastModifiedFormatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Button(
                                            onClick = { openNoteInCanvas(recent.file) },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Resume")
                                        }
                                    }
                                }
                            }
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
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(32.dp)
                                )
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
                                    DropdownMenu(
                                        expanded = showFolderMenu,
                                        onDismissRequest = { showFolderMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Change Color") },
                                            leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                                            onClick = {
                                                showFolderMenu = false
                                                folderToEditColor = folder
                                            }
                                        )
                                        DropdownMenuItem(
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
                            onPositionReported = { rect ->
                                cardBoundsMap[note.path] = rect
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedNotePaths = if (isSelected) selectedNotePaths.minus(note.path) else selectedNotePaths.plus(note.path)
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
                                    isSelectionMode = true
                                    selectedNotePaths = setOf(note.path)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveNoteCard(
    note: NoteDocument,
    isGridView: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isTrashMode: Boolean,
    pdfExportManager: PdfExportManager,
    onPositionReported: (Rect) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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

    val thumbnailFile by produceState<File?>(initialValue = ThumbnailManager.getCachedThumbnailFile(context, note.file), key1 = note.lastModifiedMs) {
        value = ThumbnailManager.getOrCreateThumbnail(context, note.file, pdfExportManager)
    }

    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .onGloballyPositioned { coords ->
                onPositionReported(coords.boundsInParent())
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
                    if (thumbnailFile != null && thumbnailFile!!.exists()) {
                        val bitmap = remember(thumbnailFile) {
                            try { BitmapFactory.decodeFile(thumbnailFile!!.absolutePath) } catch (e: Exception) { null }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (note.fileType) {
                            NoteFileType.PDF -> MaterialTheme.colorScheme.secondaryContainer
                            NoteFileType.XOJ -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = note.fileType.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
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
                                NoteActionDropdown(
                                    expanded = showMenu,
                                    onDismiss = { showMenu = false },
                                    onExportPdf = onExportPdf,
                                    onSharePdf = onSharePdf,
                                    onShareXopp = onShareXopp,
                                    onRename = onRename,
                                    onDuplicate = onDuplicate,
                                    onDelete = onDelete
                                )
                            }
                        } else if (isTrashMode) {
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
                    Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${note.lastModifiedFormatted} · ${note.sizeFormatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (!isSelectionMode && !isTrashMode) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        NoteActionDropdown(
                            expanded = showMenu,
                            onDismiss = { showMenu = false },
                            onExportPdf = onExportPdf,
                            onSharePdf = onSharePdf,
                            onShareXopp = onShareXopp,
                            onRename = onRename,
                            onDuplicate = onDuplicate,
                            onDelete = onDelete
                        )
                    }
                } else if (isTrashMode) {
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
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onShareXopp: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
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
