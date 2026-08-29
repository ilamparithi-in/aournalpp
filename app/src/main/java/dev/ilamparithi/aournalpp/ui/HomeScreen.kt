package dev.ilamparithi.aournalpp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.FileProvider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.utils.NoteOpenAction
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import dev.ilamparithi.aournalpp.ui.NoteOpenActionDialog
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.mutableLongStateOf
import dev.ilamparithi.aournalpp.ui.collage.CollageCardView
import dev.ilamparithi.aournalpp.ui.collage.CreativeEmptyCollageState
import dev.ilamparithi.aournalpp.ui.collage.OrganicCollageView
import kotlinx.coroutines.delay
import dev.ilamparithi.aournalpp.ui.theme.ArchShape
import dev.ilamparithi.aournalpp.ui.theme.AsymmetricCardShape
import dev.ilamparithi.aournalpp.ui.theme.CloverShape
import dev.ilamparithi.aournalpp.ui.theme.ExpressiveSprings
import dev.ilamparithi.aournalpp.ui.theme.ScallopShape
import dev.ilamparithi.aournalpp.ui.theme.SunnyShape
import dev.ilamparithi.aournalpp.utils.ExternalFileHandler
import dev.ilamparithi.aournalpp.ui.preview.floatingPreviewLongPress
import dev.ilamparithi.aournalpp.utils.ThumbnailManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFiles: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { DocumentRepository(context) }
    val env = remember { repository.getLinuxEnvironment() }
    val supervisor = remember { ProcessSupervisor(env) }
    val pdfExportManager = remember { PdfExportManager(env, supervisor) }
    val snackbarHostState = remember { SnackbarHostState() }

    val prefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    var viewMode by remember { mutableStateOf(prefs.getString("pref_home_view_mode", "EXPRESSIVE") ?: "EXPRESSIVE") }

    var recentNotes by remember { mutableStateOf<List<NoteDocument>>(emptyList()) }
    var continueNote by remember { mutableStateOf<NoteDocument?>(null) }
    var totalNotesCount by remember { mutableStateOf(0) }
    var totalFoldersCount by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshSeed by remember { mutableLongStateOf(0L) }
    val pullRefreshState = rememberPullToRefreshState()

    // Emergency recovery state
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

    // Dialog states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedFolderColor by remember { mutableStateOf("#4CAF50") }
    var selectedFolderEmoji by remember { mutableStateOf<String?>(null) }
    var selectedFolderIconType by remember { mutableStateOf<String?>("folder") }

    // Speed Dial FAB state
    var isFabExpanded by remember { mutableStateOf(false) }
    var showNewNoteDialog by remember { mutableStateOf(false) }
    var newNoteDefaultName by remember { mutableStateOf("") }
    var allFoldersForNewNote by remember { mutableStateOf<List<FolderItem>>(emptyList()) }

    val scrollState = rememberScrollState()
    val isScrolled by remember { derivedStateOf { scrollState.value > 100 } }

    suspend fun loadHomeDataNow() {
        val homeNotes = repository.getHomeNotes(16)
        recentNotes = homeNotes
        totalNotesCount = repository.countAllNotes()
        totalFoldersCount = repository.scanDirectory(repository.getRootNotesDirectory()).first.size

        continueNote = repository.getLastOpenedOrModifiedNote()

        val emergencyFile = withContext(Dispatchers.IO) { env.checkAndQuarantineEmergencySave() }
        if (emergencyFile != null && emergencyFile.exists() && emergencyFile.length() > 0) {
            if (quarantinedEmergencySave == null && !showEmergencySaveNameDialog) {
                quarantinedEmergencySave = emergencyFile
                showEmergencyDialog = true
            }
        }

        val autoloadOverridden = withContext(Dispatchers.IO) { env.checkAndOverrideAutoloadPreference() }
        if (autoloadOverridden || env.hasPendingAutoloadOverrideNotification()) {
            showAutoloadOverrideDialog = true
        }
    }

    fun loadHomeData() {
        scope.launch { loadHomeDataNow() }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                loadHomeData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        loadHomeData()
    }

    // Note action & dialog states
    var noteToRename by remember { mutableStateOf<NoteDocument?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var noteToDelete by remember { mutableStateOf<NoteDocument?>(null) }
    var isPdfConverting by remember { mutableStateOf(false) }
    var convertingMessage by remember { mutableStateOf("") }
    var noteForActionDialog by remember { mutableStateOf<File?>(null) }
    val localView = LocalView.current

    fun handleNoteOpen(file: File) {
        NoteOpenManager.handleFileOpen(
            context = context,
            file = file,
            pdfExportManager = pdfExportManager,
            scope = scope,
            repository = repository,
            localView = localView,
            onShowPrompt = { noteForActionDialog = it },
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
    }

    fun openNote(file: File) {
        handleNoteOpen(file)
    }

    // File Import Launcher (Supports PDF, XOPP, XOJ)
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val staged = ExternalFileHandler.stageExternalUri(context, uri, repository.getLinuxEnvironment())
                if (staged.isSuccess) {
                    val file = staged.getOrThrow()
                    loadHomeData()
                    handleNoteOpen(file)
                } else {
                    snackbarHostState.showSnackbar("Failed to import file: ${staged.exceptionOrNull()?.message}")
                }
            }
        }
    }

    val onTogglePin: (NoteDocument) -> Unit = { note ->
        scope.launch {
            repository.togglePinNote(note.path)
            loadHomeData()
        }
    }

    val onDuplicate: (NoteDocument) -> Unit = { note ->
        scope.launch {
            val result = repository.duplicateNote(note)
            if (result.isSuccess) {
                loadHomeData()
                snackbarHostState.showSnackbar("Duplicated \"${note.title}\"")
            } else {
                snackbarHostState.showSnackbar("Failed to duplicate: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    val onShareXopp: (NoteDocument) -> Unit = { note ->
        repository.shareNoteAsXopp(context, note)
    }

    val onSharePdf: (NoteDocument) -> Unit = { note ->
        scope.launch {
            isPdfConverting = true
            convertingMessage = "Rendering PDF for \"${note.title}\"..."
            val result = repository.shareNoteAsPdf(context, note, pdfExportManager)
            isPdfConverting = false
            if (result.isFailure) {
                snackbarHostState.showSnackbar("PDF Export failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    val onExportPdf: (NoteDocument) -> Unit = { note ->
        scope.launch {
            isPdfConverting = true
            convertingMessage = "Exporting \"${note.title}\" to PDF..."
            val exportDir = File(repository.getRootNotesDirectory(), "Exports").apply { mkdirs() }
            val destPdf = File(exportDir, "${note.title}.pdf")
            val result = pdfExportManager.convertXoppToPdf(note.file, destPdf)
            isPdfConverting = false
            if (result.isSuccess) {
                val pdfFile = result.getOrThrow()
                snackbarHostState.showSnackbar("Exported to Exports/${pdfFile.name}")
            } else {
                snackbarHostState.showSnackbar("PDF Export failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    val onRename: (NoteDocument) -> Unit = { note ->
        noteToRename = note
        renameInputText = note.file.nameWithoutExtension
    }

    val onDelete: (NoteDocument) -> Unit = { note ->
        noteToDelete = note
    }

    fun promptNewNote() {
        newNoteDefaultName = SimpleDateFormat("yyyy-MM-dd-'Note'-HH-mm", Locale.getDefault()).format(Date())
        scope.launch {
            allFoldersForNewNote = withContext(Dispatchers.IO) {
                repository.getAllFolders()
            }
        }
        showNewNoteDialog = true
    }

    fun startNewNote() {
        val intent = Intent(context, CanvasActivity::class.java)
        val options = ActivityOptionsCompat.makeClipRevealAnimation(
            localView,
            localView.width / 2,
            localView.height / 2,
            localView.width / 4,
            localView.height / 4
        ).toBundle()
        context.startActivity(intent, options)
    }

    // Dynamic greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Welcome back"
        }
    }

    // Dynamic fun subhero phrase
    val funSubhero = remember(recentNotes.size, totalNotesCount) {
        when {
            totalNotesCount == 0 -> "✨ Ready to sketch your first idea?"
            recentNotes.size == 1 -> "✨ 1 note active • Ideas ready to flow"
            else -> "✨ ${recentNotes.size} notes in studio • Ideas ready to flow"
        }
    }

    // FAB Rotation Animation
    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 135f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fabRotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "A",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Aournal",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Dynamic fun subhero badge appearing in top bar when scrolled up
                            AnimatedVisibility(
                                visible = isScrolled,
                                enter = fadeIn() + slideInHorizontally { it / 2 },
                                exit = fadeOut() + slideOutHorizontally { it / 2 }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = funSubhero,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        refreshSeed = System.currentTimeMillis()
                        loadHomeData()
                        delay(600)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // 1. Dynamic Hero Header & Stats
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = funSubhero,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Expressive Stats Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "$totalNotesCount notes",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "$totalFoldersCount folders",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 2. Enlarged "Continue where you left off" Section
                    continueNote?.let { note ->
                        EnlargedContinueHeroSection(
                            note = note,
                            pdfExportManager = pdfExportManager,
                            onResume = {
                                if (note.autosaveInfo != null) {
                                    pendingAutosaveNote = note
                                } else {
                                    openNote(note.file)
                                }
                            }
                        )
                    }

                    // 3. M3 Expressive Studio Notes (Collage vs Gallery)
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Studio Notes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // View Mode Toggle (Expressive vs Normal)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Row(modifier = Modifier.padding(3.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (viewMode == "EXPRESSIVE") MaterialTheme.colorScheme.primary else Color.Transparent,
                                            modifier = Modifier.clickable {
                                                viewMode = "EXPRESSIVE"
                                                prefs.edit().putString("pref_home_view_mode", "EXPRESSIVE").apply()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = "Expressive Collage",
                                                    tint = if (viewMode == "EXPRESSIVE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Collage",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (viewMode == "EXPRESSIVE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (viewMode == "NORMAL") MaterialTheme.colorScheme.primary else Color.Transparent,
                                            modifier = Modifier.clickable {
                                                viewMode = "NORMAL"
                                                prefs.edit().putString("pref_home_view_mode", "NORMAL").apply()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.GridView,
                                                    contentDescription = "Normal Gallery",
                                                    tint = if (viewMode == "NORMAL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Gallery",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (viewMode == "NORMAL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                TextButton(onClick = onNavigateToFiles) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Files Hub", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        if (recentNotes.isEmpty()) {
                            CreativeEmptyCollageState(onNewNoteClick = { promptNewNote() })
                        } else if (viewMode == "EXPRESSIVE") {
                            OrganicCollageView(
                                notes = recentNotes,
                                pdfExportManager = pdfExportManager,
                                onNoteClick = { note ->
                                    if (note.autosaveInfo != null) {
                                        pendingAutosaveNote = note
                                    } else {
                                        openNote(note.file)
                                    }
                                },
                                onNewNoteClick = { promptNewNote() },
                                refreshSeed = refreshSeed,
                                onTogglePin = onTogglePin,
                                onExportPdf = onExportPdf,
                                onSharePdf = onSharePdf,
                                onShareXopp = onShareXopp,
                                onRename = onRename,
                                onDuplicate = onDuplicate,
                                onDelete = onDelete
                            )
                        } else {
                            NormalHomeGalleryView(
                                notes = recentNotes,
                                pdfExportManager = pdfExportManager,
                                onNoteClick = { note ->
                                    if (note.autosaveInfo != null) {
                                        pendingAutosaveNote = note
                                    } else {
                                        openNote(note.file)
                                    }
                                },
                                onTogglePin = onTogglePin,
                                onExportPdf = onExportPdf,
                                onSharePdf = onSharePdf,
                                onShareXopp = onShareXopp,
                                onRename = onRename,
                                onDuplicate = onDuplicate,
                                onDelete = onDelete
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }

        // 4. Expressive Speed Dial Floating Action Menu (Bottom Right)
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

        // Speed Dial Action Items + Main FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
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
                    label = "New Folder",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = {
                        isFabExpanded = false
                        newFolderName = ""
                        showCreateFolderDialog = true
                    }
                )

                SpeedDialActionItem(
                    progress = pdfItemSpring,
                    icon = Icons.Default.FileOpen,
                    label = "Import File",
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
                    label = "New Note",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        isFabExpanded = false
                        promptNewNote()
                    }
                )

                // Main Speed Dial FAB with spring physics on press & rotate
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
                        contentDescription = "Expand Actions",
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(fabRotation)
                    )
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            title = "Create New Folder",
            confirmButtonLabel = "Create",
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name, colorHex, iconEmoji, iconType ->
                showCreateFolderDialog = false
                scope.launch {
                    val res = repository.createFolder(
                        parentDir = repository.getRootNotesDirectory(),
                        name = name,
                        colorHex = colorHex,
                        iconEmoji = iconEmoji,
                        iconType = iconType
                    )
                    if (res.isSuccess) {
                        snackbarHostState.showSnackbar("Created folder \"$name\"")
                        loadHomeData()
                    } else {
                        snackbarHostState.showSnackbar("Failed to create folder: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
        )
    }

    // Emergency Recovery Launch Dialog
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
                    loadHomeData()
                    openNote(staged)
                }) { Text("Open Now") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showEmergencyDialog = false
                        val defaultName = "Recovered_Note_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(file.lastModified()))
                        emergencySaveNameInput = defaultName
                        emergencySaveTargetFolder = repository.getRootNotesDirectory()
                        showEmergencySaveNameDialog = true
                    }) { Text("Save as Note") }
                    TextButton(onClick = {
                        showEmergencyDialog = false
                        repository.discardEmergencyRecovery()
                        quarantinedEmergencySave = null
                        loadHomeData()
                    }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }


    // New Note Dialog
    if (showNewNoteDialog) {
        SaveAsNoteDialog(
            title = "New Note",
            subtitle = "Choose a name and destination folder for your new note.",
            icon = Icons.Default.Edit,
            initialName = newNoteDefaultName,
            initialFolder = repository.getRootNotesDirectory(),
            availableFolders = allFoldersForNewNote,
            rootFolder = repository.getRootNotesDirectory(),
            confirmButtonLabel = "Create & Open",
            onDismiss = { showNewNoteDialog = false },
            onSkip = {
                showNewNoteDialog = false
                startNewNote()
            },
            onSave = { name, targetFolder ->
                showNewNoteDialog = false
                scope.launch {
                    val result = repository.createBlankNote(name, targetFolder)
                    if (result.isSuccess) {
                        val file = result.getOrThrow()
                        loadHomeData()
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
                    allFoldersForNewNote = allFoldersForNewNote + dev.ilamparithi.aournalpp.model.FolderItem(
                        file = newFolder,
                        name = newFolder.name,
                        colorHex = colorHex,
                        iconEmoji = iconEmoji,
                        iconType = iconType,
                        isEmergencyFolder = false
                    )
                    loadHomeData()
                }
                result
            }
        )
    }

    // Autoload Preference Conflict Overridden Dialog
    if (showAutoloadOverrideDialog) {

        AlertDialog(
            onDismissRequest = {
                showAutoloadOverrideDialog = false
                env.clearPendingAutoloadOverrideNotification()
            },
            icon = {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Startup Preference Overridden", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "In Xournal++ Preferences > Load/Save, \"Enable autoloading of most recent file on application startup\" was detected and has been cleared to \"false\".\n\n" +
                    "This setting conflicts with Aournal++'s \"Continue where you left off\" workspace control. You can continue launching recent notes directly from your Home Screen."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAutoloadOverrideDialog = false
                        env.clearPendingAutoloadOverrideNotification()
                    }
                ) {
                    Text("Understood")
                }
            }
        )
    }

    // Save Emergency Recovery Name Dialog
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
                loadHomeData()
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
                    loadHomeData()
                }
                result
            }
        )
    }

    // Autosave Resolution Dialog
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
                    loadHomeData()
                    openNote(target)
                },
                onKeepBoth = {
                    pendingAutosaveNote = null
                    pendingSaveAutosaveNote = note
                },
                onKeepExisting = {
                    val target = repository.discardAutosave(note)
                    pendingAutosaveNote = null
                    loadHomeData()
                    openNote(target)
                }
            )
        }
    }

    // Save Autosave as Note Dialog
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
                    loadHomeData()
                    openNote(note.file)
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
                        loadHomeData()
                    }
                    result
                }
            )
        }
    }

    // Note Open Action Prompt Dialog (View as PDF / Edit in Xournal++)
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

    // PDF Converting Progress Dialog
    if (isPdfConverting) {
        AlertDialog(
            onDismissRequest = {},
            icon = { CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp) },
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

    // Rename Note Dialog
    noteToRename?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToRename = null },
            icon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Rename Note", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        label = { Text("Note Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = noteToRename
                    noteToRename = null
                    if (target != null && renameInputText.isNotBlank()) {
                        scope.launch {
                            val result = repository.renameNote(target, renameInputText)
                            if (result.isSuccess) {
                                loadHomeData()
                                snackbarHostState.showSnackbar("Renamed to \"${renameInputText.trim()}\"")
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
                TextButton(onClick = { noteToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Note Confirmation Dialog
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("Move to Trash?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to move \"${note.title}\" to Trash? You can restore it later from Files Hub.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = noteToDelete
                        noteToDelete = null
                        if (target != null) {
                            scope.launch {
                                val result = repository.moveToTrash(listOf(target))
                                if (result.isSuccess) {
                                    loadHomeData()
                                    val action = snackbarHostState.showSnackbar(
                                        message = "Moved \"${target.title}\" to Trash",
                                        actionLabel = "Undo",
                                        duration = androidx.compose.material3.SnackbarDuration.Short
                                    )
                                    if (action == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                        val trashed = repository.scanTrash().find { it.title == target.title }
                                        if (trashed != null) {
                                            repository.restoreFromTrash(trashed)
                                            loadHomeData()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}



/**
 * Enlarged "Continue where you left off" Hero Section
 */
@Composable
private fun EnlargedContinueHeroSection(
    note: NoteDocument,
    pdfExportManager: PdfExportManager,
    onResume: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "heroScale"
    )

    val thumbnailImage by produceState<ImageBitmap?>(
        initialValue = ThumbnailManager.getCachedThumbnail(note.file),
        key1 = note.lastModifiedMs
    ) {
        value = ThumbnailManager.getOrCreateThumbnailBitmap(context, note.file, pdfExportManager)
    }
    val thumbnailFile = remember(thumbnailImage) { ThumbnailManager.getCachedThumbnailFile(note.file) }

    val relativeTime = remember(note.lastModifiedMs) {
        val diff = System.currentTimeMillis() - note.lastModifiedMs
        val mins = diff / (1000L * 60L)
        val hours = mins / 60L
        val days = hours / 24L
        when {
            mins < 1L -> "Just now"
            mins < 60L -> "$mins min ago"
            hours < 24L -> "$hours hr ago"
            else -> "$days d ago"
        }
    }

    val heroFolderAccent = note.folderColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onResume
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = heroFolderAccent.copy(alpha = 0.14f)
        ),
        border = BorderStroke(1.dp, heroFolderAccent.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large Thumbnail preview
            Box(
                modifier = Modifier
                    .size(130.dp, 155.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .floatingPreviewLongPress(
                        note = note,
                        thumbnailFile = thumbnailFile,
                        folderColor = heroFolderAccent,
                        initialCornerRadiusDp = 20f,
                        onClick = onResume
                    )
            ) {
                if (thumbnailImage != null) {
                    Image(
                        bitmap = thumbnailImage!!,
                        contentDescription = note.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (note.fileType == NoteFileType.PDF) Icons.Default.PictureAsPdf else Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = heroFolderAccent.copy(alpha = 0.7f)
                        )
                    }
                }

                // Format pill badge
                FileTypePill(
                    fileType = note.fileType,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                )
            }

            // Info & Expanded Resume Button
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "CONTINUE WHERE YOU LEFT OFF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = heroFolderAccent,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val folderDisplayName = if (note.folder.isBlank() || note.folder == "Notes Home") "Notes Home" else "In ${note.folder}"
                val openedText = note.fuzzyLastOpened?.let { "Opened $it" }
                val modifiedText = "Modified ${note.fuzzyLastModified}"
                val metadataSubtitle = if (openedText != null) {
                    "$folderDisplayName • $openedText • $modifiedText"
                } else {
                    "$folderDisplayName • $modifiedText"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.folder.isBlank() || note.folder == "Notes Home") {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = heroFolderAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        if (!note.folderIconEmoji.isNullOrBlank()) {
                            Text(
                                text = note.folderIconEmoji,
                                fontSize = 14.sp
                            )
                        } else if (note.folderIconType == "emergency" || note.folder.equals("Emergency Saves", ignoreCase = true)) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = null,
                                tint = heroFolderAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = heroFolderAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = metadataSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val resumeBtnTextColor = remember(heroFolderAccent) {
                    val lum = 0.299f * heroFolderAccent.red + 0.587f * heroFolderAccent.green + 0.114f * heroFolderAccent.blue
                    if (lum > 0.55f) Color(0xFF191C1D) else Color.White
                }

                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = heroFolderAccent,
                        contentColor = resumeBtnTextColor
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp), tint = resumeBtnTextColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume Editing", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = resumeBtnTextColor)
                    }
                }
            }
        }
    }
}

/**
 * Normal Gallery View for Home Screen (with Pinned section on top and folder palette detail pills).
 */
@Composable
private fun NormalHomeGalleryView(
    notes: List<NoteDocument>,
    pdfExportManager: PdfExportManager,
    onNoteClick: (NoteDocument) -> Unit,
    onTogglePin: ((NoteDocument) -> Unit)? = null,
    onExportPdf: ((NoteDocument) -> Unit)? = null,
    onSharePdf: ((NoteDocument) -> Unit)? = null,
    onShareXopp: ((NoteDocument) -> Unit)? = null,
    onRename: ((NoteDocument) -> Unit)? = null,
    onDuplicate: ((NoteDocument) -> Unit)? = null,
    onDelete: ((NoteDocument) -> Unit)? = null
) {
    val pinnedNotes = notes.filter { it.isPinned }
    val regularNotes = notes.filter { !it.isPinned }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Pinned Notes Section (if any)
        if (pinnedNotes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Pinned Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val chunkedPinned = pinnedNotes.chunked(2)
                chunkedPinned.forEach { rowNotes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowNotes.forEach { note ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(240.dp)
                            ) {
                                CollageCardView(
                                    note = note,
                                    shape = RoundedCornerShape(18.dp),
                                    pdfExportManager = pdfExportManager,
                                    onClick = { onNoteClick(note) },
                                    onTogglePin = onTogglePin?.let { { it(note) } },
                                    onExportPdf = onExportPdf?.let { { it(note) } },
                                    onSharePdf = onSharePdf?.let { { it(note) } },
                                    onShareXopp = onShareXopp?.let { { it(note) } },
                                    onRename = onRename?.let { { it(note) } },
                                    onDuplicate = onDuplicate?.let { { it(note) } },
                                    onDelete = onDelete?.let { { it(note) } }
                                )
                            }
                        }
                        if (rowNotes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 2. Recent Notes Section
        if (regularNotes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pinnedNotes.isNotEmpty()) {
                    Text(
                        "Recent Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val chunked = regularNotes.chunked(2)
                chunked.forEach { rowNotes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowNotes.forEach { note ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(230.dp)
                            ) {
                                CollageCardView(
                                    note = note,
                                    shape = RoundedCornerShape(16.dp),
                                    pdfExportManager = pdfExportManager,
                                    onClick = { onNoteClick(note) },
                                    onTogglePin = onTogglePin?.let { { it(note) } },
                                    onExportPdf = onExportPdf?.let { { it(note) } },
                                    onSharePdf = onSharePdf?.let { { it(note) } },
                                    onShareXopp = onShareXopp?.let { { it(note) } },
                                    onRename = onRename?.let { { it(note) } },
                                    onDuplicate = onDuplicate?.let { { it(note) } },
                                    onDelete = onDelete?.let { { it(note) } }
                                )
                            }
                        }
                        if (rowNotes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
