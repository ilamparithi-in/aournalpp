package dev.ilamparithi.aournalpp.ui

import dev.ilamparithi.aournalpp.ui.dialog.AutosaveResolutionDialog
import dev.ilamparithi.aournalpp.ui.home.HomeTopAppBar
import dev.ilamparithi.aournalpp.ui.home.HomeFloatingActionMenu
import dev.ilamparithi.aournalpp.MainActivity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalConfiguration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import dev.ilamparithi.aournalpp.ui.animation.AppAnimatedVisibility
import dev.ilamparithi.aournalpp.ui.animation.appAnimateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.widthIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ilamparithi.aournalpp.runtime.ActiveNotesTracker
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
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import dev.ilamparithi.aournalpp.utils.AccessibilityUtils
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.view.View
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ilamparithi.aournalpp.ui.home.HomeViewModel
import dev.ilamparithi.aournalpp.ui.home.HomeDialogState
import dev.ilamparithi.aournalpp.ui.home.HomeUiState
import androidx.activity.result.ActivityResultLauncher

data class PendingSingleExport(
    val note: NoteDocument,
    val format: dev.ilamparithi.aournalpp.data.DocumentRepository.ShareExportFormat,
    val customName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFiles: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = viewModel.repository
    val env = viewModel.env
    val supervisor = viewModel.supervisor
    val pdfExportManager = viewModel.pdfExportManager
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeSession by ActiveSessionTracker.activeSessionFlow(context, env)
        .collectAsStateWithLifecycle(initialValue = null)

    val pullRefreshState = rememberPullToRefreshState()
    val scrollState = rememberScrollState()
    val isScrolled by remember { derivedStateOf { scrollState.value > 100 } }
    val localView = LocalView.current

    LaunchedEffect(uiState.quarantinedEmergencyFile) {
        val emergencyFile = uiState.quarantinedEmergencyFile
        if (emergencyFile != null && emergencyFile.exists() && emergencyFile.length() > 0) {
            val mainActivity = context as? MainActivity
            mainActivity?.quarantinedEmergencySave?.value = emergencyFile
            viewModel.clearQuarantinedEmergencyFile()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onAppResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun handleNoteOpen(file: File) {
        NoteOpenManager.handleFileOpen(
            context = context,
            file = file,
            pdfExportManager = pdfExportManager,
            scope = scope,
            repository = repository,
            localView = localView,
            onShowPrompt = { viewModel.setDialogState(HomeDialogState.NoteAction(it)) },
            onShowActiveNotePrompt = { targetFile, match ->
                viewModel.setDialogState(HomeDialogState.ActiveSessionPrompt(targetFile, match))
            },
            onConvertingState = { isConverting ->
                if (isConverting) {
                    viewModel.setDialogState(
                        HomeDialogState.PdfConverting("Rendering PDF for \"${file.nameWithoutExtension}\"...")
                    )
                } else if (uiState.dialogState is HomeDialogState.PdfConverting) {
                    viewModel.dismissDialog()
                }
            },
            onError = { err ->
                scope.launch { snackbarHostState.showSnackbar(err) }
            }
        )
    }

    val onNoteClick: (NoteDocument) -> Unit = { note ->
        viewModel.onNoteClick(note, context) { file ->
            handleNoteOpen(file)
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val rootNotesDir = repository.getRootNotesDirectory()
                val existingNote = ExternalFileHandler.resolveIfInNotesDirectory(context, uri, rootNotesDir)
                if (existingNote != null) {
                    handleNoteOpen(existingNote)
                    return@launch
                }

                val staged = ExternalFileHandler.stageExternalUri(context, uri, repository.getLinuxEnvironment())
                if (staged.isSuccess) {
                    val file = staged.getOrThrow()
                    viewModel.loadHomeData()
                    handleNoteOpen(file)
                } else {
                    snackbarHostState.showSnackbar("Failed to import file: ${staged.exceptionOrNull()?.message}")
                }
            }
        }
    }

    val onTogglePin: (NoteDocument) -> Unit = { note ->
        viewModel.togglePin(note)
    }

    val onDuplicate: (NoteDocument) -> Unit = { note ->
        viewModel.duplicateNote(note) { result ->
            if (result.isSuccess) {
                scope.launch { snackbarHostState.showSnackbar("Duplicated \"${note.title}\"") }
            } else {
                scope.launch { snackbarHostState.showSnackbar("Failed to duplicate: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    var pendingSingleExport by remember { mutableStateOf<PendingSingleExport?>(null) }

    val singleSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val pending = pendingSingleExport
        pendingSingleExport = null
        if (uri != null && pending != null) {
            viewModel.setDialogState(HomeDialogState.PdfConverting("Saving \"${pending.customName}\"..."))
            scope.launch {
                val result = repository.exportDocumentToUri(
                    context = context,
                    doc = pending.note,
                    format = pending.format,
                    destUri = uri,
                    pdfExportManager = pdfExportManager
                )
                viewModel.dismissDialog()
                if (result.isSuccess) {
                    val ext = if (pending.format == dev.ilamparithi.aournalpp.data.DocumentRepository.ShareExportFormat.PDF) "pdf" else "xopp"
                    snackbarHostState.showSnackbar(context.getString(dev.ilamparithi.aournalpp.R.string.msg_exported_success, "${pending.customName}.$ext"))
                } else {
                    snackbarHostState.showSnackbar("Export failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    val onOpenAs: (NoteDocument) -> Unit = { note ->
        viewModel.setDialogState(HomeDialogState.NoteAction(note.file))
    }

    val onShareExport: (NoteDocument) -> Unit = { note ->
        viewModel.setDialogState(HomeDialogState.ShareExport(note))
    }

    val onRename: (NoteDocument) -> Unit = { note ->
        viewModel.setDialogState(HomeDialogState.RenameNote(note))
    }

    val onDelete: (NoteDocument) -> Unit = { note ->
        viewModel.setDialogState(HomeDialogState.DeleteNote(note))
    }

    fun startNewNote() {
        val intent = Intent(context, CanvasActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context is android.app.Activity && context.isInMultiWindowMode) {
                addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }
        }
        val options = ActivityOptionsCompat.makeCustomAnimation(context, 0, 0).toBundle()
        context.startActivity(intent, options)
        val activity = context as? android.app.Activity
        if (activity != null) {
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                activity.overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
        }
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
    val funSubhero = remember(uiState.recentNotes.size, uiState.totalNotesCount) {
        when {
            uiState.totalNotesCount == 0 -> "✨ Ready to sketch your first idea?"
            uiState.recentNotes.size == 1 -> "✨ 1 note active • Ideas ready to flow"
            else -> "✨ ${uiState.recentNotes.size} notes in studio • Ideas ready to flow"
        }
    }

    val reduceAnimations = dev.ilamparithi.aournalpp.ui.animation.LocalMotionPreferences.current.reduceAnimations

    val configuration = LocalConfiguration.current
    val isWideOrLandscape = configuration.screenWidthDp >= 600 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                HomeTopAppBar(
                    isWideOrLandscape = isWideOrLandscape,
                    isScrolled = isScrolled,
                    funSubhero = funSubhero,
                    onSyncFinished = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                )
            }
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
                        isRefreshing = uiState.isRefreshing,
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
                                            uiState.totalNotesCount,
                                            uiState.totalNotesCount
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
                                            uiState.totalFoldersCount,
                                            uiState.totalFoldersCount
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 2. Enlarged "Continue where you left off" Section
                    uiState.continueNote?.let { note ->
                        EnlargedContinueHeroSection(
                            note = note,
                            pdfExportManager = pdfExportManager,
                            onResume = { onNoteClick(note) }
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
                                        color = if (uiState.viewMode == "EXPRESSIVE") MaterialTheme.colorScheme.primary else Color.Transparent,
                                        modifier = Modifier.clickable {
                                            viewModel.setViewMode("EXPRESSIVE")
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_view_mode_collage),
                                                tint = if (uiState.viewMode == "EXPRESSIVE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_view_mode_collage),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (uiState.viewMode == "EXPRESSIVE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (uiState.viewMode == "NORMAL") MaterialTheme.colorScheme.primary else Color.Transparent,
                                        modifier = Modifier.clickable {
                                            viewModel.setViewMode("NORMAL")
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.GridView,
                                                contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_view_mode_grid),
                                                tint = if (uiState.viewMode == "NORMAL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.home_view_mode_grid),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (uiState.viewMode == "NORMAL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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

                        if (uiState.recentNotes.isEmpty()) {
                            CreativeEmptyCollageState(onNewNoteClick = { viewModel.promptNewNote(context) })
                        } else if (uiState.viewMode == "EXPRESSIVE") {
                            OrganicCollageView(
                                notes = uiState.recentNotes,
                                pdfExportManager = pdfExportManager,
                                onNoteClick = onNoteClick,
                                onNewNoteClick = { viewModel.promptNewNote(context) },
                                refreshSeed = uiState.refreshSeed,
                                onOpenAs = onOpenAs,
                                onTogglePin = onTogglePin,
                                onShareExport = onShareExport,
                                onRename = onRename,
                                onDuplicate = onDuplicate,
                                onDelete = onDelete
                            )
                        } else {
                            NormalHomeGalleryView(
                                notes = uiState.recentNotes,
                                pdfExportManager = pdfExportManager,
                                onNoteClick = onNoteClick,
                                onOpenAs = onOpenAs,
                                onTogglePin = onTogglePin,
                                onShareExport = onShareExport,
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
        HomeFloatingActionMenu(
            isExpanded = uiState.isFabExpanded,
            onExpandedChange = { viewModel.setFabExpanded(it) },
            onCreateFolderClick = {
                viewModel.setDialogState(HomeDialogState.CreateFolder)
            },
            onOpenFileClick = {
                importFileLauncher.launch(arrayOf("*/*", "application/pdf", "application/x-xopp", "application/x-xoj", "application/octet-stream"))
            },
            onCreateNoteClick = {
                viewModel.promptNewNote(context)
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }

    HomeDialogHost(
        dialogState = uiState.dialogState,
        viewModel = viewModel,
        pdfExportManager = pdfExportManager,
        snackbarHostState = snackbarHostState,
        localView = localView,
        singleSaveLauncher = singleSaveLauncher,
        onSavePendingExport = { pendingSingleExport = it },
        onStartNewNote = { startNewNote() },
        onDirectOpen = { file -> handleNoteOpen(file) }
    )
}

@Composable
private fun HomeDialogHost(
    dialogState: HomeDialogState,
    viewModel: HomeViewModel,
    pdfExportManager: PdfExportManager,
    snackbarHostState: SnackbarHostState,
    localView: View,
    singleSaveLauncher: ActivityResultLauncher<String>,
    onSavePendingExport: (PendingSingleExport) -> Unit,
    onStartNewNote: () -> Unit,
    onDirectOpen: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    when (dialogState) {
        HomeDialogState.None -> Unit

        HomeDialogState.CreateFolder -> {
            CreateFolderDialog(
                title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_new_folder_title),
                confirmButtonLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_create_button),
                onDismiss = { viewModel.dismissDialog() },
                onCreate = { name, colorHex, iconEmoji, iconType ->
                    viewModel.dismissDialog()
                    viewModel.createFolder(name, colorHex, iconEmoji, iconType) { res ->
                        if (res.isSuccess) {
                            scope.launch { snackbarHostState.showSnackbar("Created folder \"$name\"") }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Failed to create folder: ${res.exceptionOrNull()?.message}") }
                        }
                    }
                }
            )
        }

        is HomeDialogState.NewNote -> {
            var allFolders by remember(dialogState) { mutableStateOf(dialogState.folders) }
            SaveAsNoteDialog(
                title = "New Note",
                subtitle = "Choose a name and destination folder for your new note.",
                icon = Icons.Default.Edit,
                initialName = dialogState.defaultName,
                initialFolder = viewModel.repository.getRootNotesDirectory(),
                availableFolders = allFolders,
                rootFolder = viewModel.repository.getRootNotesDirectory(),
                confirmButtonLabel = "Create & Open",
                onDismiss = { viewModel.dismissDialog() },
                onSkip = {
                    viewModel.dismissDialog()
                    onStartNewNote()
                },
                onSave = { name, targetFolder ->
                    viewModel.dismissDialog()
                    viewModel.createBlankNote(name, targetFolder) { result ->
                        if (result.isSuccess) {
                            val file = result.getOrThrow()
                            NoteOpenManager.openInCanvas(
                                context = context,
                                file = file,
                                repository = viewModel.repository,
                                localView = localView
                            )
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Failed to create note: ${result.exceptionOrNull()?.message}"
                                )
                            }
                        }
                    }
                },
                onCreateFolder = { name, colorHex, iconEmoji, iconType ->
                    val result = viewModel.repository.createFolder(
                        parentDir = viewModel.repository.getRootNotesDirectory(),
                        name = name,
                        colorHex = colorHex,
                        iconEmoji = iconEmoji,
                        iconType = iconType
                    )
                    if (result.isSuccess) {
                        val newFolder = result.getOrThrow()
                        allFolders = allFolders + dev.ilamparithi.aournalpp.model.FolderItem(
                            file = newFolder,
                            name = newFolder.name,
                            colorHex = colorHex,
                            iconEmoji = iconEmoji,
                            iconType = iconType,
                            isEmergencyFolder = false
                        )
                        viewModel.loadHomeData(force = true)
                    }
                    result
                }
            )
        }

        HomeDialogState.AutoloadOverride -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAutoloadOverride() },
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
                        onClick = { viewModel.dismissAutoloadOverride() }
                    ) {
                        Text("Understood")
                    }
                }
            )
        }

        is HomeDialogState.AutosavePrompt -> {
            val note = dialogState.note
            val autoInfo = note.autosaveInfo
            if (autoInfo != null) {
                AutosaveResolutionDialog(
                    note = note,
                    autosaveInfo = autoInfo,
                    onDismiss = { viewModel.dismissDialog() },
                    onReplaceWithAutosave = {
                        val target = viewModel.replaceWithAutosave(note)
                        viewModel.dismissDialog()
                        onDirectOpen(target)
                    },
                    onKeepBoth = {
                        viewModel.setDialogState(HomeDialogState.SaveAutosavePrompt(note))
                    },
                    onKeepExisting = {
                        val target = viewModel.discardAutosave(note)
                        viewModel.dismissDialog()
                        onDirectOpen(target)
                    }
                )
            }
        }

        is HomeDialogState.SaveAutosavePrompt -> {
            val note = dialogState.note
            val autoInfo = note.autosaveInfo
            if (autoInfo != null) {
                val allAvailableFolders by produceState<List<FolderItem>>(emptyList(), note) {
                    value = viewModel.repository.getAllFolders()
                }

                SaveAsNoteDialog(
                    title = "Save Autosave as Note",
                    subtitle = "Save a separate copy of the autosaved version with your chosen name and folder.",
                    icon = Icons.Default.Description,
                    initialName = "${note.title} (Autosave)",
                    initialFolder = note.file.parentFile ?: viewModel.repository.getRootNotesDirectory(),
                    availableFolders = allAvailableFolders,
                    rootFolder = viewModel.repository.getRootNotesDirectory(),
                    onDismiss = { viewModel.dismissDialog() },
                    onSave = { name, targetFolder ->
                        val savedFile = viewModel.saveAutosaveAsNote(autoInfo, name, targetFolder)
                        viewModel.dismissDialog()
                        onDirectOpen(note.file)
                        scope.launch {
                            snackbarHostState.showSnackbar("Saved autosave copy as \"${savedFile.name}\"")
                        }
                    },
                    onCreateFolder = { name, colorHex, iconEmoji, iconType ->
                        val result = viewModel.repository.createFolder(
                            parentDir = viewModel.repository.getRootNotesDirectory(),
                            name = name,
                            colorHex = colorHex,
                            iconEmoji = iconEmoji,
                            iconType = iconType
                        )
                        if (result.isSuccess) {
                            viewModel.loadHomeData(force = true)
                        }
                        result
                    }
                )
            }
        }

        is HomeDialogState.NoteAction -> {
            val file = dialogState.file
            NoteOpenActionDialog(
                file = file,
                onDismiss = { viewModel.dismissDialog() },
                onViewAsPdf = {
                    viewModel.dismissDialog()
                    NoteOpenManager.openAsPdf(
                        context = context,
                        file = file,
                        pdfExportManager = pdfExportManager,
                        scope = scope,
                        repository = viewModel.repository,
                        onConvertingState = { isConverting ->
                            if (isConverting) {
                                viewModel.setDialogState(
                                    HomeDialogState.PdfConverting("Rendering PDF for \"${file.nameWithoutExtension}\"...")
                                )
                            } else {
                                viewModel.dismissDialog()
                            }
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                },
                onEditInCanvas = {
                    viewModel.dismissDialog()
                    NoteOpenManager.openInCanvasWithActiveCheck(
                        context = context,
                        file = file,
                        repository = viewModel.repository,
                        localView = localView,
                        onActiveSessionFound = { _, activeMatch ->
                            viewModel.setDialogState(HomeDialogState.ActiveSessionPrompt(file, activeMatch))
                        }
                    )
                }
            )
        }

        is HomeDialogState.ActiveSessionPrompt -> {
            val file = dialogState.file
            val match = dialogState.match
            val pendingNote = dialogState.pendingNote
            ActiveNoteOpenPromptDialog(
                file = file,
                activeMatch = match,
                onDismiss = { viewModel.dismissDialog() },
                onViewExistingWindow = {
                    viewModel.dismissDialog()
                    NoteOpenManager.viewExistingWindow(context, match.windowId)
                },
                onOpenInNewWindow = {
                    viewModel.dismissDialog()
                    if (pendingNote?.autosaveInfo != null) {
                        viewModel.setDialogState(HomeDialogState.AutosavePrompt(pendingNote))
                    } else {
                        NoteOpenManager.openInCanvas(
                            context = context,
                            file = file,
                            repository = viewModel.repository,
                            localView = localView
                        )
                    }
                }
            )
        }

        is HomeDialogState.PdfConverting -> {
            PdfConversionProgressDialog(message = dialogState.message)
        }

        is HomeDialogState.RenameNote -> {
            val note = dialogState.note
            var renameInputText by remember(note) { mutableStateOf(note.file.nameWithoutExtension) }
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = {
                    Icon(
                        Icons.Default.DriveFileRenameOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_rename_title),
                        fontWeight = FontWeight.Bold
                    )
                },
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
                        viewModel.dismissDialog()
                        if (renameInputText.isNotBlank()) {
                            viewModel.renameNote(note, renameInputText) { result ->
                                if (result.isSuccess) {
                                    scope.launch { snackbarHostState.showSnackbar("Renamed to \"${renameInputText.trim()}\"") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Failed to rename: ${result.exceptionOrNull()?.message}") }
                                }
                            }
                        }
                    }) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_rename))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel))
                    }
                }
            )
        }

        is HomeDialogState.DeleteNote -> {
            val note = dialogState.note
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                properties = AppDialogDefaults.Properties,
                modifier = Modifier.promptWidth(),
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_delete_note_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_delete_note_body, note.title))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissDialog()
                            viewModel.deleteNote(note) { result ->
                                if (result.isSuccess) {
                                    scope.launch {
                                        val action = snackbarHostState.showSnackbar(
                                            message = "Moved \"${note.title}\" to Trash",
                                            actionLabel = "Undo",
                                            duration = androidx.compose.material3.SnackbarDuration.Short
                                        )
                                        if (action == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            viewModel.restoreFromTrash(note)
                                        }
                                    }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Failed to delete: ${result.exceptionOrNull()?.message}") }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel))
                    }
                }
            )
        }

        is HomeDialogState.ShareExport -> {
            val note = dialogState.note
            val defaultName = remember(note.file.path) {
                note.file.nameWithoutExtension
            }
            SingleShareExportDialog(
                note = note,
                initialName = defaultName,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { sanitizedName, format ->
                    viewModel.dismissDialog()
                    onSavePendingExport(PendingSingleExport(note, format, sanitizedName))
                    val ext = if (format == dev.ilamparithi.aournalpp.data.DocumentRepository.ShareExportFormat.PDF) "pdf" else "xopp"
                    singleSaveLauncher.launch("$sanitizedName.$ext")
                },
                onShare = { sanitizedName, format ->
                    viewModel.setDialogState(HomeDialogState.PdfConverting("Preparing to share \"$sanitizedName\"..."))
                    scope.launch {
                        val result = viewModel.repository.shareUnifiedDocuments(
                            context = context,
                            docs = listOf(note),
                            format = format,
                            customNameForSingle = sanitizedName,
                            pdfExportManager = pdfExportManager
                        )
                        viewModel.dismissDialog()
                        if (result.isFailure) {
                            snackbarHostState.showSnackbar("Failed to share: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            )
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

    val cardScale by appAnimateFloatAsState(
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
        try { Color(it.toColorInt()) } catch (e: Exception) { null }
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
    onOpenAs: ((NoteDocument) -> Unit)? = null,
    onTogglePin: ((NoteDocument) -> Unit)? = null,
    onShareExport: ((NoteDocument) -> Unit)? = null,
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
                                    onOpenAs = onOpenAs?.let { { it(note) } },
                                    onTogglePin = onTogglePin?.let { { it(note) } },
                                    onShareExport = onShareExport?.let { { it(note) } },
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
                                    onOpenAs = onOpenAs?.let { { it(note) } },
                                    onTogglePin = onTogglePin?.let { { it(note) } },
                                    onShareExport = onShareExport?.let { { it(note) } },
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


