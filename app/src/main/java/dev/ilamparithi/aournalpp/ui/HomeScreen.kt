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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.ui.theme.ArchShape
import dev.ilamparithi.aournalpp.ui.theme.AsymmetricCardShape
import dev.ilamparithi.aournalpp.ui.theme.CloverShape
import dev.ilamparithi.aournalpp.ui.theme.ExpressiveSprings
import dev.ilamparithi.aournalpp.ui.theme.ScallopShape
import dev.ilamparithi.aournalpp.ui.theme.SunnyShape
import dev.ilamparithi.aournalpp.utils.ExternalFileHandler
import dev.ilamparithi.aournalpp.utils.ThumbnailManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

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
    val lastOpenedPath = remember(prefs) { prefs.getString("pref_last_opened_note_path", null) }

    var recentNotes by remember { mutableStateOf<List<NoteDocument>>(emptyList()) }
    var continueNote by remember { mutableStateOf<NoteDocument?>(null) }
    var totalNotesCount by remember { mutableStateOf(0) }
    var totalFoldersCount by remember { mutableStateOf(0) }

    // Dialog states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedFolderColor by remember { mutableStateOf("#4CAF50") }

    // Speed Dial FAB state
    var isFabExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val isScrolled by remember { derivedStateOf { scrollState.value > 100 } }

    fun loadHomeData() {
        val recents = repository.getAllRecentNotes(12)
        recentNotes = recents
        totalNotesCount = repository.getAllRecentNotes(500).size
        totalFoldersCount = repository.scanDirectory(repository.getRootNotesDirectory()).first.size

        continueNote = if (lastOpenedPath != null) {
            val f = File(lastOpenedPath)
            if (f.exists() && !f.absolutePath.contains("/.Trash/")) {
                repository.getNoteDocumentForFile(f)
            } else recents.firstOrNull()
        } else {
            recents.firstOrNull()
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                        onResume = { openNote(note.file) }
                    )
                }

                // 3. M3 Expressive Recent Notes Collage (Not a Carousel! Fills vertical space)
                if (recentNotes.isNotEmpty()) {
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
                                    "Recent Notes Collage",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            TextButton(onClick = onNavigateToFiles) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Open Files Hub", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        ExpressiveRecentCollage(
                            notes = recentNotes,
                            pdfExportManager = pdfExportManager,
                            onNoteClick = { openNote(it.file) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // 4. Expressive Speed Dial Floating Action Menu (Bottom Right)
        AnimatedVisibility(
            visible = isFabExpanded,
            enter = fadeIn(),
            exit = fadeOut()
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
                        Text("Folder Color Accent", style = MaterialTheme.typography.labelMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            folderColors.forEach { colorHex ->
                                val color = Color(android.graphics.Color.parseColor(colorHex))
                                val isSelected = selectedFolderColor == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedFolderColor = colorHex }
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
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
                                            colorHex = selectedFolderColor
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onResume
            ),
        shape = AsymmetricCardShape(topStart = 36.dp, bottomEnd = 36.dp, topEnd = 16.dp, bottomStart = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
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
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }

                // Format pill badge
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                ) {
                    Text(
                        text = ".${note.file.extension}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Info & Expanded Resume Button
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "CONTINUE WHERE YOU LEFT OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "In ${note.folder.ifEmpty { "Notes Home" }} • Modified $relativeTime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume Editing", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * M3 Expressive Recent Notes Collage (Fills vertical & horizontal space with organic shapes)
 */
@Composable
private fun ExpressiveRecentCollage(
    notes: List<NoteDocument>,
    pdfExportManager: PdfExportManager,
    onNoteClick: (NoteDocument) -> Unit
) {
    val shapes: List<Shape> = remember {
        listOf(
            ArchShape(cornerRadiusRatio = 0.45f),
            AsymmetricCardShape(topStart = 36.dp, topEnd = 14.dp, bottomEnd = 36.dp, bottomStart = 14.dp),
            ScallopShape(lobes = 8, depth = 0.07f),
            SunnyShape(vertices = 8, roundness = 0.22f),
            CloverShape(),
            AsymmetricCardShape(topStart = 14.dp, topEnd = 36.dp, bottomEnd = 14.dp, bottomStart = 36.dp)
        )
    }

    // Split notes into 3 vertical columns for an artistic mosaic collage layout
    val col1Notes = notes.filterIndexed { index, _ -> index % 3 == 0 }
    val col2Notes = notes.filterIndexed { index, _ -> index % 3 == 1 }
    val col3Notes = notes.filterIndexed { index, _ -> index % 3 == 2 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Column 1
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            col1Notes.forEachIndexed { i, note ->
                val shape = shapes[(i * 3) % shapes.size]
                val cardHeight = if (i == 0) 240.dp else 170.dp
                CollageItemCard(
                    note = note,
                    shape = shape,
                    cardHeight = cardHeight,
                    pdfExportManager = pdfExportManager,
                    onClick = { onNoteClick(note) }
                )
            }
        }

        // Column 2
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            col2Notes.forEachIndexed { i, note ->
                val shape = shapes[(i * 3 + 1) % shapes.size]
                val cardHeight = if (i == 0) 180.dp else 230.dp
                CollageItemCard(
                    note = note,
                    shape = shape,
                    cardHeight = cardHeight,
                    pdfExportManager = pdfExportManager,
                    onClick = { onNoteClick(note) }
                )
            }
        }

        // Column 3
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            col3Notes.forEachIndexed { i, note ->
                val shape = shapes[(i * 3 + 2) % shapes.size]
                val cardHeight = if (i % 2 == 0) 210.dp else 180.dp
                CollageItemCard(
                    note = note,
                    shape = shape,
                    cardHeight = cardHeight,
                    pdfExportManager = pdfExportManager,
                    onClick = { onNoteClick(note) }
                )
            }
        }
    }
}

@Composable
private fun CollageItemCard(
    note: NoteDocument,
    shape: Shape,
    cardHeight: androidx.compose.ui.unit.Dp,
    pdfExportManager: PdfExportManager,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "collageItemScale"
    )

    val thumbnailFile by produceState<File?>(initialValue = ThumbnailManager.getCachedThumbnailFile(context, note.file), key1 = note.lastModifiedMs) {
        value = ThumbnailManager.getOrCreateThumbnail(context, note.file, pdfExportManager)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .scale(scale)
            .shadow(elevation = 6.dp, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
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
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // Gradient & Title Overlay at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = ".${note.file.extension} • ${note.lastModifiedFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
