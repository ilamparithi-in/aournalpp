package dev.ilamparithi.aournalpp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.LicensesActivity
import dev.ilamparithi.aournalpp.SettingsActivity
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentHubScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf<List<NoteDocument>>(emptyList()) }
    var showMenu by remember { mutableStateOf(false) }

    // Headless PDF conversion states
    var isConvertingPdf by remember { mutableStateOf(false) }
    var convertingNoteTitle by remember { mutableStateOf("") }
    var pendingExportNote by remember { mutableStateOf<NoteDocument?>(null) }

    // Dialog states for card operations
    var noteToRename by remember { mutableStateOf<NoteDocument?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var noteToDelete by remember { mutableStateOf<NoteDocument?>(null) }

    // Emergency recovery state
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
            isConvertingPdf = true
            convertingNoteTitle = note.title
            scope.launch {
                val result = pdfExportManager.exportPdfToUri(context, note.file, uri)
                isConvertingPdf = false
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("Exported ${note.title}.pdf successfully")
                } else {
                    snackbarHostState.showSnackbar("PDF Export failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun loadNotes() {
        if (!hasPermission) return
        notes = repository.scanDocuments(query = searchQuery, showHidden = showHiddenFiles)
        val emergencyFile = env.checkAndQuarantineEmergencySave()
        if (emergencyFile != null && emergencyFile.exists() && emergencyFile.length() > 0) {
            quarantinedEmergencySave = emergencyFile
            showEmergencyDialog = true
        }
    }

    fun openNoteInCanvas(noteFile: File) {
        val intent = Intent(context, CanvasActivity::class.java).apply {
            putExtra(CanvasActivity.EXTRA_NOTE_PATH, noteFile.absolutePath)
        }
        context.startActivity(intent)
    }

    // Refresh when screen is resumed or permissions / search changes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasStoragePermission(context)
                loadNotes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasPermission, showHiddenFiles, searchQuery) {
        loadNotes()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search notes by name...") },
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
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Search"
                            )
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
                        Text(
                            "Xournal++",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Notes"
                            )
                        }

                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (showHiddenFiles) "Hide Backup & Hidden Files" else "Show Hidden Files")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    val updated = !showHiddenFiles
                                    showHiddenFiles = updated
                                    prefs.edit().putBoolean("pref_show_hidden_files", updated).apply()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Open Source Licenses") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(context, LicensesActivity::class.java)
                                    context.startActivity(intent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(context, SettingsActivity::class.java)
                                    context.startActivity(intent)
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        floatingActionButton = {
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Note")
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
                title = {
                    Text(
                        text = "Storage Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Xournal++ saves notes and exports directly to your device storage (${env.getNotesDirectory().absolutePath}). Please grant All Files Access to proceed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                requestStoragePermission(context)
                            } else {
                                legacyPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("Later")
                    }
                }
            )
        }

        // 2. Emergency Recovery Launch Dialog
        if (showEmergencyDialog && quarantinedEmergencySave != null) {
            val file = quarantinedEmergencySave!!
            val dateStr = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date(file.lastModified()))

            AlertDialog(
                onDismissRequest = { showEmergencyDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Unsaved Session Recovered",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Xournal++ closed unexpectedly during a previous session. An emergency recovery copy from $dateStr was saved.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "What would you like to do with this recovered session?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEmergencyDialog = false
                            val stagedNote = repository.openEmergencyRecoverySession(file)
                            quarantinedEmergencySave = null
                            loadNotes()
                            openNoteInCanvas(stagedNote)
                        }
                    ) {
                        Text("Open Now")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                showEmergencyDialog = false
                                val defaultName = "Recovered_Note_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(file.lastModified()))
                                emergencySaveNameInput = defaultName
                                showEmergencySaveNameDialog = true
                            }
                        ) {
                            Text("Save to Notes")
                        }
                        TextButton(
                            onClick = {
                                showEmergencyDialog = false
                                repository.discardEmergencyRecovery()
                                quarantinedEmergencySave = null
                                loadNotes()
                            }
                        ) {
                            Text("Discard", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }

        // 2b. Name input prompt for saving emergency note
        if (showEmergencySaveNameDialog && quarantinedEmergencySave != null) {
            val file = quarantinedEmergencySave!!
            AlertDialog(
                onDismissRequest = { showEmergencySaveNameDialog = false },
                title = {
                    Text("Save Recovered Note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter a filename for the recovered note:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = emergencySaveNameInput,
                            onValueChange = { emergencySaveNameInput = it },
                            singleLine = true,
                            label = { Text("Note Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (emergencySaveNameInput.isNotBlank()) {
                                showEmergencySaveNameDialog = false
                                repository.saveEmergencyRecoveryToNotes(file, emergencySaveNameInput.trim())
                                quarantinedEmergencySave = null
                                loadNotes()
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmergencySaveNameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 3. Rename Note Dialog
        noteToRename?.let { doc ->
            AlertDialog(
                onDismissRequest = { noteToRename = null },
                title = {
                    Text("Rename Note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter new name for \"${doc.title}\":", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = renameInputText,
                            onValueChange = { renameInputText = it },
                            singleLine = true,
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameInputText.isNotBlank()) {
                                val target = noteToRename
                                noteToRename = null
                                target?.let { note ->
                                    scope.launch {
                                        val result = repository.renameNote(note, renameInputText)
                                        if (result.isSuccess) {
                                            snackbarHostState.showSnackbar("Renamed note successfully")
                                            loadNotes()
                                        } else {
                                            snackbarHostState.showSnackbar("Rename failed: ${result.exceptionOrNull()?.message}")
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteToRename = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 4. Delete Confirmation Dialog
        noteToDelete?.let { doc ->
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                icon = {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                title = {
                    Text("Delete Note?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Are you sure you want to delete \"${doc.title}\"? This action cannot be undone.", style = MaterialTheme.typography.bodyMedium)
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            val target = noteToDelete
                            noteToDelete = null
                            target?.let { note ->
                                scope.launch {
                                    val result = repository.deleteNote(note)
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("Deleted \"${note.title}\"")
                                        loadNotes()
                                    } else {
                                        snackbarHostState.showSnackbar("Delete failed: ${result.exceptionOrNull()?.message}")
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 5. On-Open Autosave Resolution Dialog
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
                        loadNotes()
                        openNoteInCanvas(target)
                    },
                    onKeepBoth = {
                        val target = repository.keepBoth(note)
                        pendingAutosaveNote = null
                        loadNotes()
                        openNoteInCanvas(target)
                    },
                    onKeepExisting = {
                        val target = repository.discardAutosave(note)
                        pendingAutosaveNote = null
                        loadNotes()
                        openNoteInCanvas(target)
                    }
                )
            }
        }

        // Background PDF conversion floating progress indicator
        AnimatedVisibility(
            visible = isConvertingPdf,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                        Text(
                            text = "Exporting \"$convertingNoteTitle\" to PDF...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Storage Notice Banner
            if (!hasPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text("All Files Access Required", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Text("To save notes directly into ${env.getNotesDirectory().absolutePath}, please grant storage management permission.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                requestStoragePermission(context)
                            } else {
                                legacyPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderShared, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Storage Access")
                    }
                }
            }
        }

        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No notes match \"$searchQuery\"" else "No notes yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try a different search term" else "Tap + to create a new note in ${env.getNotesDirectory().name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notes, key = { it.path }) { note ->
                    NoteCard(
                        note = note,
                        onClick = {
                            if (!hasPermission) {
                                showPermissionDialog = true
                            } else if (note.autosaveInfo != null) {
                                pendingAutosaveNote = note
                            } else {
                                openNoteInCanvas(note.file)
                            }
                        },
                        onExportPdf = {
                            pendingExportNote = note
                            exportPdfLauncher.launch("${note.title}.pdf")
                        },
                        onSharePdf = {
                            isConvertingPdf = true
                            convertingNoteTitle = note.title
                            scope.launch {
                                val result = repository.shareNoteAsPdf(context, note, pdfExportManager)
                                isConvertingPdf = false
                                if (result.isFailure) {
                                    snackbarHostState.showSnackbar("Failed to share PDF: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        },
                        onShareXopp = {
                            repository.shareNoteAsXopp(context, note)
                        },
                        onRename = {
                            noteToRename = note
                            renameInputText = note.title
                        },
                        onDuplicate = {
                            scope.launch {
                                val result = repository.duplicateNote(note)
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar("Duplicated \"${note.title}\"")
                                    loadNotes()
                                } else {
                                    snackbarHostState.showSnackbar("Duplicate failed: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        },
                        onDelete = {
                            noteToDelete = note
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun NoteCard(
    note: NoteDocument,
    onClick: () -> Unit,
    onExportPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onShareXopp: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showCardMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    note.file.extension.equals("pdf", ignoreCase = true) -> MaterialTheme.colorScheme.tertiaryContainer
                    note.isHidden -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            note.file.extension.equals("pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
                            note.isHidden -> Icons.Default.Description
                            else -> Icons.Default.Edit
                        },
                        contentDescription = null,
                        tint = when {
                            note.file.extension.equals("pdf", ignoreCase = true) -> MaterialTheme.colorScheme.onTertiaryContainer
                            note.isHidden -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.lastModifiedFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = note.sizeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Material 3 Standard Assist Badges for Autosave or Hidden File
                if (note.autosaveInfo != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Autosave Available",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (note.isHidden) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = "Hidden File",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Card Overflow Menu Button
            Box {
                IconButton(onClick = { showCardMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Note Actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showCardMenu,
                    onDismissRequest = { showCardMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Export to PDF") },
                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                        onClick = {
                            showCardMenu = false
                            onExportPdf()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share as PDF") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                        onClick = {
                            showCardMenu = false
                            onSharePdf()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share as .xopp") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            showCardMenu = false
                            onShareXopp()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            showCardMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            showCardMenu = false
                            onDuplicate()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showCardMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
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

                // Relative freshness banner (Material 3 tonal surface)
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

                // Comparison Card
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
                        // Current saved file
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("${autosaveInfo.mainModifiedFormatted} (${autosaveInfo.mainSizeFormatted})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider()

                        // Autosaved copy
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
