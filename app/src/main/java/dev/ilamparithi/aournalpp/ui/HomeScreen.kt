package dev.ilamparithi.aournalpp.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.ui.platform.LocalConfiguration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.widthIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker
import dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import dev.ilamparithi.aournalpp.ui.util.a11yHeading
import dev.ilamparithi.aournalpp.ui.util.minTouchTarget
import dev.ilamparithi.aournalpp.ui.util.AccessibilityUtils
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.utils.FileNameTemplateEngine
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.compositeOver
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
import dev.ilamparithi.aournalpp.utils.FormatUtils
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

    val activeSession by ActiveSessionTracker.activeSessionFlow(context, env)
        .collectAsStateWithLifecycle(initialValue = null)

    var recentNotes by remember { mutableStateOf<List<NoteDocument>>(repository.getCachedHomeNotes(16) ?: emptyList()) }
    var continueNote by remember { mutableStateOf<NoteDocument?>(repository.getCachedContinueNote()) }
    var totalNotesCount by remember { mutableStateOf(repository.getCachedTotalNotesCount() ?: 0) }
    var totalFoldersCount by remember { mutableStateOf(repository.getCachedTotalFoldersCount() ?: 0) }
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
        val payload = withContext(Dispatchers.IO) {
            repository.getHomeData(16)
        }
        recentNotes = payload.notes
        totalNotesCount = payload.totalNotesCount
        totalFoldersCount = payload.totalFoldersCount
        continueNote = payload.continueNote

        // Prefetch thumbnails off the main thread
        ThumbnailManager.prefetchThumbnails(context, payload.notes, pdfExportManager, scope)

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
                scope.launch {
                    kotlinx.coroutines.delay(1000)
                    loadHomeDataNow()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    loadHomeDataNow()
                }
            }
        }
        val filter = android.content.IntentFilter("dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED")
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
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

    data class SingleFileActionPrompt(
        val note: NoteDocument,
        val actionType: FileActionPromptType,
        val defaultName: String
    )
    var activeFilePrompt by remember { mutableStateOf<SingleFileActionPrompt?>(null) }

    val onShareXopp: (NoteDocument) -> Unit = { note ->
        val defaultName = FileNameTemplateEngine.evaluate(
            FileNameTemplateEngine.getShareXoppTemplate(context),
            context,
            note.file
        )
        activeFilePrompt = SingleFileActionPrompt(note, FileActionPromptType.SHARE_XOPP, defaultName)
    }

    val onSharePdf: (NoteDocument) -> Unit = { note ->
        val defaultName = FileNameTemplateEngine.evaluate(
            FileNameTemplateEngine.getSharePdfTemplate(context),
            context,
            note.file
        )
        activeFilePrompt = SingleFileActionPrompt(note, FileActionPromptType.SHARE_PDF, defaultName)
    }

    val onExportPdf: (NoteDocument) -> Unit = { note ->
        val defaultName = FileNameTemplateEngine.evaluate(
            FileNameTemplateEngine.getExportPdfTemplate(context),
            context,
            note.file
        )
        activeFilePrompt = SingleFileActionPrompt(note, FileActionPromptType.EXPORT_PDF, defaultName)
    }

    val onRename: (NoteDocument) -> Unit = { note ->
        noteToRename = note
        renameInputText = note.file.nameWithoutExtension
    }

    val onDelete: (NoteDocument) -> Unit = { note ->
        noteToDelete = note
    }

    fun promptNewNote() {
        val template = FileNameTemplateEngine.getNewFileTemplate(context)
        newNoteDefaultName = FileNameTemplateEngine.evaluate(template, context)
        scope.launch {
            allFoldersForNewNote = withContext(Dispatchers.IO) {
                repository.getAllFolders()
            }
        }
        showNewNoteDialog = true
    }

    fun startNewNote() {
        val intent = Intent(context, CanvasActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
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

    // Dynamic greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> context.getString(dev.ilamparithi.aournalpp.R.string.greeting_morning)
            in 12..16 -> context.getString(dev.ilamparithi.aournalpp.R.string.greeting_afternoon)
            in 17..21 -> context.getString(dev.ilamparithi.aournalpp.R.string.greeting_evening)
            else -> context.getString(dev.ilamparithi.aournalpp.R.string.greeting_welcome_back)
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

    val reduceAnimations = remember { prefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false) }

    // FAB Rotation Animation
    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 135f else 0f,
        animationSpec = if (reduceAnimations) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fabRotation"
    )

    val configuration = LocalConfiguration.current
    val isWideOrLandscape = configuration.screenWidthDp >= 600 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                                // Suppress the "A" logo badge in landscape/wide mode (where the navigation rail is on the left)
                                if (!isWideOrLandscape) {
                                    AppLogoBadge(
                                        size = 36.dp,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.app_name),
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
                    actions = {
                        AnimatedVisibility(
                            visible = activeSession?.isRunning == true,
                            enter = fadeIn() + slideInHorizontally { it / 2 },
                            exit = fadeOut() + slideOutHorizontally { it / 2 }
                        ) {
                            activeSession?.let { session ->
                                ReturnToActiveSessionButton(
                                    sessionInfo = session,
                                    onClick = {
                                        val intent = Intent(context, CanvasActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                        }
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }

                        dev.ilamparithi.aournalpp.ui.cloud.QuickSyncButton(
                            onSyncFinished = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Dynamic Hero Header & Stats
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.a11yHeading()
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
                                        androidx.compose.ui.res.pluralStringResource(
                                            dev.ilamparithi.aournalpp.R.plurals.home_stat_notes_count,
                                            totalNotesCount,
                                            totalNotesCount
                                        ),
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
                                        androidx.compose.ui.res.pluralStringResource(
                                            dev.ilamparithi.aournalpp.R.plurals.home_stat_folders_count,
                                            totalFoldersCount,
                                            totalFoldersCount
                                        ),
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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        @Composable
                        fun ViewModeToggle(modifier: Modifier = Modifier) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = modifier
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
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_view_mode_collage),
                                                tint = if (viewMode == "EXPRESSIVE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_view_mode_collage),
                                                style = MaterialTheme.typography.labelMedium,
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
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.GridView,
                                                contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_view_mode_grid),
                                                tint = if (viewMode == "NORMAL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_view_mode_grid),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (viewMode == "NORMAL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (isWideOrLandscape) {
                            // Tablet or Landscape: Single Row with Title on Left, ViewModeToggle + Files Hub Button on Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_title_recent_notes),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ViewModeToggle()

                                    TextButton(onClick = onNavigateToFiles) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.tab_files), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            // Mobile / Portrait: Row 1 has Title + Files Hub Button, Row 2 has ViewModeToggle
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
                                        androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_title_recent_notes),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                TextButton(onClick = onNavigateToFiles) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.tab_files), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            ViewModeToggle(modifier = Modifier.align(Alignment.Start))
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

                    Spacer(modifier = Modifier.height(12.dp))
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
                    label = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_create_folder),
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
                        contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.cd_expand_doc_actions),
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
            title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_new_folder_title),
            confirmButtonLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_create_button),
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
        val dateStr = FormatUtils.formatDateTimeMedium(file.lastModified())

        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            properties = AppDialogDefaults.Properties,
            modifier = Modifier.promptWidth(),
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.emergency_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.emergency_dialog_desc, dateStr))
            },
            confirmButton = {
                Button(onClick = {
                    showEmergencyDialog = false
                    val staged = repository.openEmergencyRecoverySession(file)
                    quarantinedEmergencySave = null
                    loadHomeData()
                    openNote(staged)
                }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_open_now)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showEmergencyDialog = false
                        val defaultName = "Recovered_Note_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(file.lastModified()))
                        emergencySaveNameInput = defaultName
                        emergencySaveTargetFolder = repository.getRootNotesDirectory()
                        showEmergencySaveNameDialog = true
                    }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_save_as_note)) }
                    TextButton(onClick = {
                        showEmergencyDialog = false
                        repository.discardEmergencyRecovery()
                        quarantinedEmergencySave = null
                        loadHomeData()
                    }) { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_discard), color = MaterialTheme.colorScheme.error) }
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
            properties = AppDialogDefaults.Properties,
            modifier = Modifier.promptWidth(),
            icon = { CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp) },
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

    // Rename Note Dialog
    noteToRename?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToRename = null },
            properties = AppDialogDefaults.Properties,
            modifier = Modifier.promptWidth(),
            icon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_rename_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        label = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_note_name_hint)) },
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
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToRename = null }) {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel))
                }
            }
        )
    }

    // Delete Note Confirmation Dialog
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            properties = AppDialogDefaults.Properties,
            modifier = Modifier.promptWidth(),
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_delete_note_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_delete_note_body, note.title))
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
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel))
                }
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
                        scope.launch {
                            isPdfConverting = true
                            convertingMessage = "Exporting \"$customName\" to PDF..."
                            val exportDir = File(repository.getRootNotesDirectory(), "Exports").apply { mkdirs() }
                            val destPdf = File(exportDir, "$customName.pdf")
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
                    FileActionPromptType.SHARE_PDF -> {
                        scope.launch {
                            isPdfConverting = true
                            convertingMessage = "Rendering PDF for \"$customName\"..."
                            val result = repository.shareNoteAsPdf(context, note, pdfExportManager, customName = customName)
                            isPdfConverting = false
                            if (result.isFailure) {
                                snackbarHostState.showSnackbar("PDF Export failed: ${result.exceptionOrNull()?.message}")
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
        initialValue = ThumbnailManager.getCachedThumbnail(note.file, note.lastModifiedMs),
        key1 = note.lastModifiedMs
    ) {
        value = ThumbnailManager.getOrCreateThumbnailBitmap(context, note.file, pdfExportManager, note.lastModifiedMs)
    }
    val thumbnailFile = remember(thumbnailImage) { ThumbnailManager.getCachedThumbnailFile(note.file, note.lastModifiedMs) }

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

    val resumeActionLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_hero_resume_action)
    val a11yHeroDescription = remember(note) {
        AccessibilityUtils.buildNoteCardA11yDescription(
            title = note.title,
            fileType = note.fileType,
            folderName = note.folder,
            lastModified = note.fuzzyLastModified,
            isPinned = note.isPinned
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = a11yHeroDescription
                customActions = listOf(
                    CustomAccessibilityAction(resumeActionLabel) {
                        onResume()
                        true
                    }
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onResume
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = heroFolderAccent.copy(alpha = 0.14f)
                .compositeOver(MaterialTheme.colorScheme.surface)
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
                    text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_hero_continue_header),
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
                    modifier = Modifier.basicMarquee()
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
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
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_hero_resume_action), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = resumeBtnTextColor)
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.a11yHeading()
                ) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_section_pinned),
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
                        androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_section_recent),
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

@Composable
private fun ReturnToActiveSessionButton(
    sessionInfo: ActiveSessionInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
        shadowElevation = 3.dp,
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .semantics {
                contentDescription = "Return to active session: ${sessionInfo.documentTitle ?: "Note"}"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Glowing pulse indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                        shape = CircleShape
                    )
            )

            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Return to Active Session",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            val cleanTitle = sessionInfo.documentTitle?.removePrefix("*")?.removeSuffix("*")?.trim()
            if (!cleanTitle.isNullOrBlank() && cleanTitle != "New Note" && cleanTitle != "Unsaved Document" && cleanTitle != "Preferences") {
                Text(
                    text = "• $cleanTitle",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 110.dp)
                )
            }

            if (sessionInfo.openWindowCount > 1) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${sessionInfo.openWindowCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

