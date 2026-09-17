package dev.ilamparithi.aournalpp.ui.workspace

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import dev.ilamparithi.aournalpp.ui.common.AppTooltipBox
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.CanvasCommandReceiver
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker
import dev.ilamparithi.aournalpp.runtime.ActiveWindowEntry
import dev.ilamparithi.aournalpp.runtime.ActiveWorkspaceState
import dev.ilamparithi.aournalpp.runtime.ActiveWorkspaceTracker
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.ui.SingleShareExportDialog
import dev.ilamparithi.aournalpp.ui.canvas.CanvasEmergencyForceCloseDialog
import dev.ilamparithi.aournalpp.ui.snap.SnapLayoutMode
import dev.ilamparithi.aournalpp.utils.WindowPreviewManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import java.io.File

private data class PendingSingleExport(
    val note: NoteDocument,
    val format: DocumentRepository.ShareExportFormat,
    val customName: String
)

/**
 * Active Session Return Portal.
 * Displays the dynamic multi-window workspace stage grid (mimicking physical split layouts)
 * and background stashed windows. Tapping a window tile seamlessly transitions back into CanvasActivity
 * focusing that window, without changing size or snap geometry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkspacePortalScreen(
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val env = remember { LinuxEnvironment(context) }
    val repository = remember { DocumentRepository.getInstance(context) }
    val supervisor = remember { ProcessSupervisor(env) }
    val pdfExportManager = remember { PdfExportManager(env, supervisor) }

    val workspaceState by ActiveWorkspaceTracker.workspaceStateFlow(context, env)
        .collectAsStateWithLifecycle(initialValue = null)

    var lastValidState by remember { mutableStateOf<ActiveWorkspaceState?>(null) }
    LaunchedEffect(workspaceState) {
        if (workspaceState != null && workspaceState!!.windows.isNotEmpty()) {
            lastValidState = workspaceState
        }
    }

    var isClosingSession by remember { mutableStateOf(false) }
    var isSavingForShare by remember { mutableStateOf(false) }
    var savePromptWindow by remember { mutableStateOf<ActiveWindowEntry?>(null) }
    var pendingShareWindow by remember { mutableStateOf<ActiveWindowEntry?>(null) }
    var showEmergencyForceCloseDialog by remember { mutableStateOf(false) }
    val saveAllCloseTapTimestamps = remember { mutableListOf<Long>() }

    // Share Export Dialog state
    var shareExportNote by remember { mutableStateOf<NoteDocument?>(null) }
    var pendingSingleExport by remember { mutableStateOf<PendingSingleExport?>(null) }

    val singleSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val pending = pendingSingleExport
        pendingSingleExport = null
        if (uri != null && pending != null) {
            Toast.makeText(context, "Saving \"${pending.customName}\"...", Toast.LENGTH_SHORT).show()
            scope.launch {
                val result = repository.exportDocumentToUri(
                    context = context,
                    doc = pending.note,
                    format = pending.format,
                    destUri = uri,
                    pdfExportManager = pdfExportManager
                )
                if (result.isSuccess) {
                    val ext = if (pending.format == DocumentRepository.ShareExportFormat.PDF) "pdf" else "xopp"
                    Toast.makeText(
                        context,
                        context.getString(R.string.msg_exported_success, "${pending.customName}.$ext"),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, "Export failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun launchCanvasWindow(targetWindowId: String, previewBitmap: Bitmap? = null) {
        val intent = Intent(context, CanvasActivity::class.java).apply {
            if (targetWindowId.isNotBlank()) {
                putExtra(CanvasActivity.EXTRA_TARGET_WINDOW_ID, targetWindowId)
                val previewFile = WindowPreviewManager.getPreviewFile(env.tmpDir, targetWindowId)
                if (previewFile.exists()) {
                    putExtra(CanvasActivity.EXTRA_ENTRY_SNAPSHOT_PATH, previewFile.absolutePath)
                } else {
                    val isActiveWin = workspaceState?.windows?.firstOrNull { it.id == targetWindowId }?.isActive == true
                    if (isActiveWin) {
                        val compositeFile = WindowPreviewManager.getStageCompositeFile(env.tmpDir)
                        if (compositeFile.exists()) {
                            putExtra(CanvasActivity.EXTRA_ENTRY_SNAPSHOT_PATH, compositeFile.absolutePath)
                        }
                    }
                }
            } else {
                val compositeFile = WindowPreviewManager.getStageCompositeFile(env.tmpDir)
                if (compositeFile.exists()) {
                    putExtra(CanvasActivity.EXTRA_ENTRY_SNAPSHOT_PATH, compositeFile.absolutePath)
                }
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val options = ActivityOptionsCompat.makeCustomAnimation(context, 0, 0).toBundle()
        context.startActivity(intent, options)
        val activity = context as? Activity
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= 34) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
        }
    }

    fun proceedToShare(window: ActiveWindowEntry) {
        val filePath = window.filePath
        val targetFile = if (!filePath.isNullOrBlank() && File(filePath).exists()) {
            File(filePath)
        } else {
            ProcessSupervisor.resolveNoteFile(env.getNotesDirectory(), window.cleanTitle)
                ?: ProcessSupervisor.resolveNoteFile(env.getNotesDirectory(), window.title)
        }

        if (targetFile == null || !targetFile.exists()) {
            Toast.makeText(context, context.getString(R.string.toast_save_unable_to_send), Toast.LENGTH_SHORT).show()
            launchCanvasWindow(window.id)
            return
        }

        val noteDoc = NoteDocument(targetFile)
        shareExportNote = noteDoc
    }

    fun requestSaveAndShare(window: ActiveWindowEntry) {
        isSavingForShare = true
        pendingShareWindow = window
        Toast.makeText(context, "Saving \"${window.cleanTitle}\"...", Toast.LENGTH_SHORT).show()
        val intent = Intent(CanvasCommandReceiver.ACTION_REQUEST_SAVE_WINDOW).apply {
            setPackage(context.packageName)
            putExtra("target_window_id", window.id)
        }
        context.sendBroadcast(intent)
    }

    fun handleShareClick(window: ActiveWindowEntry) {
        if (window.isDirty) {
            savePromptWindow = window
            return
        }
        proceedToShare(window)
    }

    fun handleCloseWindow(window: ActiveWindowEntry) {
        val intent = Intent(CanvasCommandReceiver.ACTION_REQUEST_CLOSE_WINDOW).apply {
            setPackage(context.packageName)
            putExtra("target_window_id", window.id)
        }
        context.sendBroadcast(intent)
    }

    fun triggerSaveAllAndClose() {
        val now = System.currentTimeMillis()
        saveAllCloseTapTimestamps.add(now)
        saveAllCloseTapTimestamps.removeAll { now - it > 2000 }

        val prefs = dev.ilamparithi.aournalpp.data.AppPreferences.getGeneral(context)
        val tripleTapEnabled = prefs.getBoolean("pref_triple_back_force_close", true)

        if (tripleTapEnabled && saveAllCloseTapTimestamps.size >= 3) {
            saveAllCloseTapTimestamps.clear()
            showEmergencyForceCloseDialog = true
            return
        }

        if (isClosingSession) return
        isClosingSession = true

        val broadcastIntent = Intent(CanvasCommandReceiver.ACTION_REQUEST_PARALLEL_CLOSE).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(broadcastIntent)
    }

    // Broadcast receiver for parallel close responses from :canvas process
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    "dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED" -> {
                        val wasClosing = isClosingSession
                        isClosingSession = false
                        if (wasClosing) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_all_saved_closed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        onNavigateHome()
                    }
                    CanvasCommandReceiver.ACTION_PARALLEL_CLOSE_CONFLICT -> {
                        isClosingSession = false
                        val noteName = intent.getStringExtra("conflicting_note") ?: ""
                        val targetWid = intent.getStringExtra("target_wid") ?: ""
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_conflict_detected, noteName),
                            Toast.LENGTH_LONG
                        ).show()
                        launchCanvasWindow(targetWid)
                    }
                    CanvasCommandReceiver.ACTION_PARALLEL_CLOSE_BLOCKING -> {
                        isClosingSession = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_dialog_blocking),
                            Toast.LENGTH_LONG
                        ).show()
                        val fallbackWid = workspaceState?.windows?.firstOrNull()?.id ?: ""
                        launchCanvasWindow(fallbackWid)
                    }
                    CanvasCommandReceiver.ACTION_SAVE_WINDOW_SUCCESS -> {
                        isSavingForShare = false
                        val targetWid = intent.getStringExtra("target_window_id") ?: ""
                        val resolvedFilePath = intent.getStringExtra("file_path")
                        val pending = pendingShareWindow
                        pendingShareWindow = null
                        var winToShare = if (pending != null && pending.id == targetWid) {
                            pending
                        } else {
                            workspaceState?.windows?.firstOrNull { it.id == targetWid } ?: pending
                        }
                        if (winToShare != null) {
                            winToShare = if (!resolvedFilePath.isNullOrBlank()) {
                                winToShare.copy(filePath = resolvedFilePath, isDirty = false)
                            } else {
                                winToShare.copy(isDirty = false)
                            }
                            proceedToShare(winToShare)
                        }
                    }
                    CanvasCommandReceiver.ACTION_SAVE_WINDOW_UNABLE_TO_SEND -> {
                        isSavingForShare = false
                        val targetWid = intent.getStringExtra("target_window_id") ?: ""
                        pendingShareWindow = null
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_save_unable_to_send),
                            Toast.LENGTH_LONG
                        ).show()
                        launchCanvasWindow(targetWid)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED")
            addAction(CanvasCommandReceiver.ACTION_PARALLEL_CLOSE_CONFLICT)
            addAction(CanvasCommandReceiver.ACTION_PARALLEL_CLOSE_BLOCKING)
            addAction(CanvasCommandReceiver.ACTION_SAVE_WINDOW_SUCCESS)
            addAction(CanvasCommandReceiver.ACTION_SAVE_WINDOW_UNABLE_TO_SEND)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Safety timeout in case :canvas process is closed or unresponsive during save
    LaunchedEffect(isSavingForShare) {
        if (isSavingForShare) {
            delay(5000.milliseconds)
            if (isSavingForShare) {
                isSavingForShare = false
                val pending = pendingShareWindow
                pendingShareWindow = null
                if (pending != null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_save_unable_to_send),
                        Toast.LENGTH_LONG
                    ).show()
                    launchCanvasWindow(pending.id)
                }
            }
        }
    }

    // Safety timeout in case :canvas process is closed or unresponsive during parallel close
    LaunchedEffect(isClosingSession) {
        if (isClosingSession) {
            delay(8000.milliseconds)
            if (isClosingSession) {
                isClosingSession = false
                if (!ActiveSessionTracker.isSessionActive(context)) {
                    onNavigateHome()
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isWideOrLandscape = configuration.screenWidthDp >= 600 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Dashboard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.workspace_active_session_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val winCount = workspaceState?.windows?.size ?: 0
                            Text(
                                text = if (winCount == 1) "1 open note" else "$winCount open notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        val activeWid = workspaceState?.windows?.firstOrNull { it.isActive }?.id
                            ?: workspaceState?.windows?.firstOrNull()?.id
                            ?: ""

                        // Save All & Close Action Button (Expanded, on the left)
                        val saveCloseLabel = stringResource(R.string.action_save_and_close_session)
                        FilledTonalButton(
                            onClick = { triggerSaveAllAndClose() },
                            enabled = !isClosingSession,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            if (isClosingSession) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = saveCloseLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        // Return to Canvas Button (Icon button, on the right)
                        val returnCanvasLabel = stringResource(R.string.action_return_to_canvas)
                        AppTooltipBox(tooltipText = returnCanvasLabel) {
                            Button(
                                onClick = { launchCanvasWindow(activeWid) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(38.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = returnCanvasLabel,
                                    modifier = Modifier.size(18.dp)
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
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val currentState = if (workspaceState != null && workspaceState!!.windows.isNotEmpty()) {
                workspaceState
            } else {
                lastValidState
            }
            if (currentState == null || currentState.windows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Dashboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.active_session_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Button(
                            onClick = { launchCanvasWindow("") },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.action_return_to_canvas))
                        }
                    }
                }
            } else {
                // Determine windows on stage vs stashed in the background
                val stagedWindowIds = when (SnapLayoutMode.fromId(currentState.snapMode)) {
                    SnapLayoutMode.SPLIT_TWO -> listOfNotNull(
                        currentState.slotAssignments[0] ?: currentState.windows.getOrNull(0)?.id,
                        currentState.slotAssignments[1] ?: currentState.windows.getOrNull(1)?.id
                    )
                    SnapLayoutMode.GRID_FOUR -> listOfNotNull(
                        currentState.slotAssignments[0] ?: currentState.windows.getOrNull(0)?.id,
                        currentState.slotAssignments[1] ?: currentState.windows.getOrNull(1)?.id,
                        currentState.slotAssignments[2] ?: currentState.windows.getOrNull(2)?.id,
                        currentState.slotAssignments[3] ?: currentState.windows.getOrNull(3)?.id
                    )
                    SnapLayoutMode.SPLIT_THREE -> listOfNotNull(
                        currentState.slotAssignments[0] ?: currentState.windows.getOrNull(0)?.id,
                        currentState.slotAssignments[1] ?: currentState.windows.getOrNull(1)?.id,
                        currentState.slotAssignments[2] ?: currentState.windows.getOrNull(2)?.id
                    )
                    else -> listOfNotNull(
                        currentState.windows.firstOrNull { it.isActive }?.id ?: currentState.windows.firstOrNull()?.id
                    )
                }.toSet()

                val stashedWindows = currentState.windows.filter { it.id !in stagedWindowIds }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Main Stage Tile (mimicking the physical multi-window arrangement)
                    val stageWeight = if (stashedWindows.isNotEmpty()) 0.68f else 1.0f
                    WorkspaceStageTile(
                        workspaceState = currentState,
                        tmpDir = env.tmpDir,
                        onWindowClick = { win, _, bmp ->
                            launchCanvasWindow(win.id, bmp)
                        },
                        onShareClick = { handleShareClick(it) },
                        onCloseClick = { handleCloseWindow(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(stageWeight)
                    )

                    // Stashed / Background Windows Shelf
                    if (stashedWindows.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "${stringResource(R.string.stashed_windows)} (${stashedWindows.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.32f)
                        ) {
                            items(stashedWindows, key = { it.id }) { win ->
                                StagePane(
                                    window = win,
                                    tmpDir = env.tmpDir,
                                    timestamp = currentState.previewTimestamp,
                                    onWindowClick = { clickedWin, _, bmp ->
                                        launchCanvasWindow(clickedWin.id, bmp)
                                    },
                                    onShareClick = { handleShareClick(it) },
                                    onCloseClick = { handleCloseWindow(it) },
                                    modifier = Modifier
                                        .width(240.dp)
                                        .fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Share & Export Dialog (identical to DocumentHubScreen / Files)
    shareExportNote?.let { note ->
        val defaultName = remember(note.file.path) { note.file.nameWithoutExtension }
        SingleShareExportDialog(
            note = note,
            initialName = defaultName,
            onDismiss = { shareExportNote = null },
            onSave = { sanitizedName, format ->
                shareExportNote = null
                pendingSingleExport = PendingSingleExport(note, format, sanitizedName)
                val ext = if (format == DocumentRepository.ShareExportFormat.PDF) "pdf" else "xopp"
                singleSaveLauncher.launch("$sanitizedName.$ext")
            },
            onShare = { sanitizedName, format ->
                shareExportNote = null
                Toast.makeText(context, "Preparing to share \"$sanitizedName\"...", Toast.LENGTH_SHORT).show()
                scope.launch {
                    val result = repository.shareUnifiedDocuments(
                        context = context,
                        docs = listOf(note),
                        format = format,
                        customNameForSingle = sanitizedName,
                        pdfExportManager = pdfExportManager
                    )
                    if (result.isFailure) {
                        Toast.makeText(context, "Failed to share: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Native Material 3 Confirmation Prompt: "Save before sharing? <No> <Yes>"
    savePromptWindow?.let { win ->
        AlertDialog(
            onDismissRequest = { savePromptWindow = null },
            title = {
                Text(
                    text = stringResource(R.string.dialog_save_before_sharing_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_save_before_sharing_message, win.cleanTitle),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        savePromptWindow = null
                        requestSaveAndShare(win)
                    }
                ) {
                    Text(stringResource(R.string.action_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        savePromptWindow = null
                        proceedToShare(win)
                    }
                ) {
                    Text(stringResource(R.string.action_no))
                }
            }
        )
    }

    if (showEmergencyForceCloseDialog) {
        CanvasEmergencyForceCloseDialog(
            onConfirmForceClose = {
                showEmergencyForceCloseDialog = false
                isClosingSession = false
                val intent = Intent(CanvasCommandReceiver.ACTION_REQUEST_FORCE_CLOSE).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(intent)
                onNavigateHome()
            },
            onCancel = {
                showEmergencyForceCloseDialog = false
            }
        )
    }
}
