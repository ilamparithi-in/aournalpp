package dev.ilamparithi.aournalpp.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.mutableLongStateOf
import dev.ilamparithi.aournalpp.ui.collage.CollageCardView
import dev.ilamparithi.aournalpp.ui.collage.CreativeEmptyCollageState
import dev.ilamparithi.aournalpp.ui.collage.FloatingDetailsPill
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

    // Autosave on-open resolution state
    var pendingAutosaveNote by remember { mutableStateOf<NoteDocument?>(null) }

    // Dialog states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedFolderColor by remember { mutableStateOf("#4CAF50") }
    var selectedFolderEmoji by remember { mutableStateOf<String?>("📁") }

    // Speed Dial FAB state
    var isFabExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val isScrolled by remember { derivedStateOf { scrollState.value > 100 } }

    suspend fun loadHomeDataNow() {
        val homeNotes = repository.getHomeNotes(16)
        recentNotes = homeNotes
        totalNotesCount = repository.countAllNotes()
        totalFoldersCount = repository.scanDirectory(repository.getRootNotesDirectory()).first.size

        continueNote = repository.getAllRecentNotes(1).firstOrNull()

        val emergencyFile = withContext(Dispatchers.IO) { env.checkAndQuarantineEmergencySave() }
        if (emergencyFile != null && emergencyFile.exists() && emergencyFile.length() > 0) {
            quarantinedEmergencySave = emergencyFile
            showEmergencyDialog = true
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

    // PDF Import Launcher
    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val staged = ExternalFileHandler.stageExternalUri(context, uri, repository.getLinuxEnvironment())
                if (staged.isSuccess) {
                    val file = staged.getOrThrow()
                    loadHomeData()
                    val intent = Intent(context, CanvasActivity::class.java).apply {
                        putExtra(CanvasActivity.EXTRA_NOTE_PATH, file.absolutePath)
                    }
                    context.startActivity(intent)
                } else {
                    snackbarHostState.showSnackbar("Failed to import PDF")
                }
            }
        }
    }

    val localView = LocalView.current
    fun openNote(file: File) {
        prefs.edit().putString("pref_last_opened_note_path", file.absolutePath).apply()
        val intent = Intent(context, CanvasActivity::class.java).apply {
            putExtra(CanvasActivity.EXTRA_NOTE_PATH, file.absolutePath)
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
                            CreativeEmptyCollageState(onNewNoteClick = { startNewNote() })
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
                                onNewNoteClick = { startNewNote() },
                                refreshSeed = refreshSeed
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
                                }
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
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 240f),
                label = "folderItemSpring"
            )
            val pdfItemSpring by animateFloatAsState(
                targetValue = if (isFabExpanded) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 310f),
                label = "pdfItemSpring"
            )
            val noteItemSpring by animateFloatAsState(
                targetValue = if (isFabExpanded) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 390f),
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
                    icon = Icons.Default.PictureAsPdf,
                    label = "Import PDF",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = {
                        isFabExpanded = false
                        importPdfLauncher.launch(arrayOf("application/pdf"))
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
                        startNewNote()
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
        val folderColors = listOf("#4CAF50", "#3F51B5", "#FF5722", "#FFC107", "#9C27B0", "#00BCD4")
        val folderEmojis = listOf("📁", "📝", "📚", "🎨", "💡", "🔬", "📐", "💼", "🏠", "⭐", "🚀", "🧪", "📓", "🏷️", "🎯", "🌿")

        Dialog(onDismissRequest = { showCreateFolderDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Create New Folder",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        placeholder = { Text("e.g. Physics, Sketches") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Folder Icon / Emoji", style = MaterialTheme.typography.labelMedium)
                        FolderEmojiPickerRow(
                            selectedEmoji = selectedFolderEmoji,
                            onEmojiSelected = { selectedFolderEmoji = it }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Folder Color Accent", style = MaterialTheme.typography.labelMedium)
                        FolderColorPickerRow(
                            selectedColorHex = selectedFolderColor,
                            onColorSelected = { selectedFolderColor = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateFolderDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newFolderName.isNotBlank()) {
                                    showCreateFolderDialog = false
                                    scope.launch {
                                        val res = repository.createFolder(
                                            parentDir = repository.getRootNotesDirectory(),
                                            name = newFolderName.trim(),
                                            colorHex = selectedFolderColor,
                                            iconEmoji = selectedFolderEmoji
                                        )
                                        if (res.isSuccess) {
                                            snackbarHostState.showSnackbar("Created folder \"${newFolderName.trim()}\"")
                                            loadHomeData()
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to create folder")
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
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
                        showEmergencySaveNameDialog = true
                    }) { Text("Save to Notes") }
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

    // Save Emergency Recovery Name Dialog
    if (showEmergencySaveNameDialog && quarantinedEmergencySave != null) {
        val file = quarantinedEmergencySave!!
        AlertDialog(
            onDismissRequest = { showEmergencySaveNameDialog = false },
            icon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Save Recovered Note", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter a name for the recovered note. It will be saved into your Notes folder.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = emergencySaveNameInput,
                        onValueChange = { emergencySaveNameInput = it },
                        label = { Text("Note Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emergencySaveNameInput.isNotBlank()) {
                            showEmergencySaveNameDialog = false
                            val savedFile = repository.saveEmergencyRecoveryToNotes(file, emergencySaveNameInput)
                            quarantinedEmergencySave = null
                            loadHomeData()
                            scope.launch {
                                snackbarHostState.showSnackbar("Saved recovered note as \"${savedFile.name}\"")
                            }
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
                    val target = repository.keepBoth(note)
                    pendingAutosaveNote = null
                    loadHomeData()
                    openNote(target)
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
}

/**
 * Expressive Speed Dial Action Item with Spring Motion Physics
 */
@Composable
private fun SpeedDialActionItem(
    progress: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    if (progress <= 0.01f) return

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "actionPressScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .graphicsLayer {
                scaleX = progress * pressScale
                scaleY = progress * pressScale
                alpha = progress.coerceIn(0f, 1f)
                translationY = (1f - progress) * 28f
            }
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 6.dp,
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        FloatingActionButton(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(18.dp),
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier.size(52.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
        }
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "heroScale"
    )

    val thumbnailFile by produceState<File?>(initialValue = ThumbnailManager.getCachedThumbnailFile(context, note.file), key1 = note.lastModifiedMs) {
        value = ThumbnailManager.getOrCreateThumbnail(context, note.file, pdfExportManager)
    }

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
            .floatingPreviewLongPress(
                note = note,
                thumbnailFile = thumbnailFile,
                folderColor = heroFolderAccent,
                initialCornerRadiusDp = 24f,
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
            ) {
                if (thumbnailFile != null && thumbnailFile!!.exists()) {
                    val bitmap = remember(thumbnailFile) {
                        try { BitmapFactory.decodeFile(thumbnailFile!!.absolutePath) } catch (e: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = note.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.folder.isBlank() || note.folder == "Notes Home") {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = heroFolderAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Notes Home • Modified $relativeTime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        if (!note.folderIconEmoji.isNullOrBlank()) {
                            Text(
                                text = note.folderIconEmoji,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = heroFolderAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "In ${note.folder} • Modified $relativeTime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
    onNoteClick: (NoteDocument) -> Unit
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
                                    onClick = { onNoteClick(note) }
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
                                    onClick = { onNoteClick(note) }
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
