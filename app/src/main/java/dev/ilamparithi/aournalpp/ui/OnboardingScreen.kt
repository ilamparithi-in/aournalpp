package dev.ilamparithi.aournalpp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import kotlin.time.Duration.Companion.milliseconds
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import dev.ilamparithi.aournalpp.utils.FormatUtils
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import dev.ilamparithi.aournalpp.ui.animation.AppAnimatedVisibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.backup.security.CustomMappingRepository
import dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.runtime.mutableIntStateOf
import dev.ilamparithi.aournalpp.utils.a11yHeading
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.text.font.FontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import dev.ilamparithi.aournalpp.ui.ExpressiveHeroSpinner
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.ui.theme.CloverShape
import dev.ilamparithi.aournalpp.ui.theme.SunnyShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.hypot

import dev.ilamparithi.aournalpp.AournalppApplication
import dev.ilamparithi.aournalpp.backup.model.FileConflictResolution
import dev.ilamparithi.aournalpp.ui.cloud.ConflictResolutionScreen
import dev.ilamparithi.aournalpp.ui.onboarding.*
import kotlinx.coroutines.Dispatchers

enum class RestoreLogType {
    INFO,
    PROGRESS,
    CONFIG,
    SUCCESS,
    ERROR
}

data class RestoreConsoleLog(
    val timestamp: String,
    val message: String,
    val type: RestoreLogType
)

@Composable
fun OnboardingScreen(
    bootstrapState: BootstrapState,
    onStartReveal: () -> Unit = {},
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val env = remember { LinuxEnvironment(context) }
    val scope = rememberCoroutineScope()

    val totalPages = 6
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })

    // Minimal extraction details expanded state
    var isExtractionDetailsExpanded by remember { mutableStateOf(false) }

    // Settings restoration overlay state (local workspace or cloud restore)
    var isRestoringSettings by remember { mutableStateOf(false) }
    var restoringStatusText by remember { mutableStateOf("") }
    var isRestorationComplete by remember { mutableStateOf(false) }
    var isRestorationFailed by remember { mutableStateOf(false) }
    var showPostCloudRestoreReminder by remember { mutableStateOf(false) }
    var restoredMappingSetsCount by remember { mutableIntStateOf(0) }
    var isRestorationDetailsExpanded by remember { mutableStateOf(false) }
    val restorationLogs = remember { mutableStateListOf<RestoreConsoleLog>() }
    var restorationProgress by remember { mutableFloatStateOf(-1f) }
    var retryRestorationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Conflict resolution state
    var activeConflicts by remember { mutableStateOf<List<FileConflictGroup>?>(null) }
    var conflictEngine by remember { mutableStateOf<BackupEngine?>(null) }
    var pendingConflictService by remember { mutableStateOf<ServiceConfig?>(null) }
    var pendingConflictRemotePath by remember { mutableStateOf<String?>(null) }
    var pendingConflictLocalFolder by remember { mutableStateOf<File?>(null) }

    val logTimeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    fun addRestoreLog(msg: String) {
        val logType = when {
            msg.contains("fail", ignoreCase = true) || msg.contains("Error", ignoreCase = true) || msg.contains("rejected", ignoreCase = true) -> RestoreLogType.ERROR
            msg.contains("success", ignoreCase = true) || msg.contains("applied", ignoreCase = true) || msg.contains("finished", ignoreCase = true) -> RestoreLogType.SUCCESS
            msg.contains("download", ignoreCase = true) || msg.contains("queued", ignoreCase = true) -> RestoreLogType.PROGRESS
            msg.contains("config", ignoreCase = true) || msg.contains("settings", ignoreCase = true) || msg.contains("preferences", ignoreCase = true) || msg.contains("theme", ignoreCase = true) -> RestoreLogType.CONFIG
            else -> RestoreLogType.INFO
        }
        restorationLogs.add(RestoreConsoleLog(logTimeFormat.format(Date()), msg, logType))
        restoringStatusText = msg
    }

    // Live storage and notification permission states with lifecycle resume observer
    var isPermissionGranted by remember { mutableStateOf(checkStoragePermissionGranted(context)) }
    var isNotificationPermissionGranted by remember { mutableStateOf(checkNotificationPermissionGranted(context)) }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isPermissionGranted = checkStoragePermissionGranted(context)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        isNotificationPermissionGranted = checkNotificationPermissionGranted(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isPermissionGranted = checkStoragePermissionGranted(context)
                isNotificationPermissionGranted = checkNotificationPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Circular Reveal Animation State
    var rootLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var checkCircleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var revealCenter by remember { mutableStateOf<Offset?>(null) }
    var isRevealing by remember { mutableStateOf(false) }
    val revealRadius = remember { Animatable(0f) }

    fun triggerRevealAnimation() {
        if (isRevealing) return
        isRevealing = true
        onStartReveal()
        scope.launch {
            val rootCoords = rootLayoutCoordinates
            val checkCoords = checkCircleCoordinates
            val center = if (rootCoords != null && checkCoords != null &&
                rootCoords.isAttached && checkCoords.isAttached
            ) {
                val pos = rootCoords.localPositionOf(checkCoords, Offset.Zero)
                Offset(
                    pos.x + checkCoords.size.width / 2f,
                    pos.y + checkCoords.size.height / 2f
                )
            } else {
                val w = rootCoords?.size?.width?.toFloat() ?: 1200f
                val h = rootCoords?.size?.height?.toFloat() ?: 800f
                Offset(w / 2f, h / 2f)
            }
            revealCenter = center

            val rootW = rootLayoutCoordinates?.size?.width?.toFloat() ?: 2500f
            val rootH = rootLayoutCoordinates?.size?.height?.toFloat() ?: 1600f
            val maxRadius = maxOf(
                hypot(center.x, center.y),
                hypot(rootW - center.x, center.y),
                hypot(center.x, rootH - center.y),
                hypot(rootW - center.x, rootH - center.y)
            ) * 1.05f

            val initialRadius = (checkCoords?.size?.width?.toFloat() ?: 72f) / 2f
            revealRadius.snapTo(initialRadius)
            revealRadius.animateTo(
                targetValue = maxRadius,
                animationSpec = tween(
                    durationMillis = 750,
                    easing = FastOutSlowInEasing
                )
            )
            onFinish()
        }
    }

    fun performLocalRestore(localFolder: File) {
        isRestoringSettings = true
        isRestorationFailed = false
        isRestorationComplete = false
        showPostCloudRestoreReminder = false
        isRestorationDetailsExpanded = false
        restorationLogs.clear()
        restorationProgress = -1f
        retryRestorationAction = { performLocalRestore(localFolder) }

        scope.launch {
            addRestoreLog("Initializing local workspace restoration for '${localFolder.name}'...")
            delay(300.milliseconds)
            env.setNotesDirectoryPathOnly(localFolder.absolutePath)
            val success = NotesHomeConfigManager.restoreSettingsFromNotesHome(localFolder, context, env) { logMsg ->
                addRestoreLog(logMsg)
            }
            if (success) {
                NotesHomeConfigManager.sync(context, env)
                addRestoreLog("Local workspace configuration synced successfully.")
                delay(400.milliseconds)
                isRestorationComplete = true
                delay(700.milliseconds)
                triggerRevealAnimation()
            } else {
                isRestorationFailed = true
                isRestorationDetailsExpanded = true
                addRestoreLog("Local restoration failed to apply workspace settings.")
            }
        }
    }

    fun performCloudRestore(
        service: ServiceConfig,
        remotePath: String,
        localFolder: File,
        skipDownload: Boolean,
        conflictPolicy: ConflictResolutionPolicy
    ) {
        env.setNotesDirectoryPathOnly(localFolder.absolutePath)

        scope.launch {
            val engine = BackupEngine(context, env, CredentialsVault.getInstance(context))
            val hasConfigs = !skipDownload && engine.hasRemoteConfigs(service, remotePath)

            if (!skipDownload && !hasConfigs) {
                // Background note download case:
                // No device-specific configs exist in cloud; only notes/folders.
                // Enqueue transfers in background applicationScope so user is not blocked.
                AournalppApplication.applicationScope.launch(Dispatchers.IO) {
                    try {
                        engine.performRestore(
                            serviceConfig = service.copy(remoteBasePath = remotePath),
                            conflictPolicy = conflictPolicy
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("OnboardingScreen", "Background notes restore failed", e)
                    }
                }
                isRestoringSettings = false
                pagerState.animateScrollToPage(3)
                return@launch
            }

            // Foreground settings restore case (configs exist or skipDownload local config restore)
            isRestoringSettings = true
            isRestorationFailed = false
            isRestorationComplete = false
            showPostCloudRestoreReminder = false
            isRestorationDetailsExpanded = false
            restorationLogs.clear()
            restorationProgress = -1f
            retryRestorationAction = { performCloudRestore(service, remotePath, localFolder, skipDownload, conflictPolicy) }

            addRestoreLog("Initializing cloud restoration from ${service.name}...")
            var restoreSuccess = true

            var hasRestoredConfigs = false
            if (!skipDownload) {
                val result = engine.performRestore(
                    serviceConfig = service.copy(remoteBasePath = remotePath),
                    conflictPolicy = conflictPolicy,
                    onProgress = { current, total, _ ->
                        if (total > 0) {
                            restorationProgress = current.toFloat() / total.toFloat()
                        }
                    },
                    onLog = { logMsg ->
                        addRestoreLog(logMsg)
                    }
                )
                hasRestoredConfigs = result.hasRestoredConfigs
                if (result.filesFailed > 0 || (result.filesRestored == 0 && result.filesSkipped == 0 && result.totalFilesDiscovered > 0)) {
                    restoreSuccess = false
                }
            } else {
                addRestoreLog("Skipping download phase (using existing local folder).")
                val localOk = NotesHomeConfigManager.restoreSettingsFromNotesHome(localFolder, context, env) { logMsg ->
                    addRestoreLog(logMsg)
                }
                restoreSuccess = localOk
                hasRestoredConfigs = localOk
            }

            if (restoreSuccess) {
                if (hasRestoredConfigs) {
                    NotesHomeConfigManager.sync(context, env)
                    addRestoreLog("Cloud configuration and sync mappings finalized.")
                    // Detect whether mapping sets exist in restored workspace
                    val mappingRepo = CustomMappingRepository(baseDir = context.filesDir, notesHomeDir = localFolder)
                    val sets = try {
                        mappingRepo.getAllMappingSets()
                    } catch (_: Exception) {
                        emptyList()
                    }
                    restoredMappingSetsCount = sets.size
                    isRestorationComplete = true
                    showPostCloudRestoreReminder = true
                } else {
                    addRestoreLog("Notes restore in progress/complete. Proceeding with onboarding setup.")
                    isRestoringSettings = false
                    pagerState.animateScrollToPage(3)
                }
            } else {
                isRestorationFailed = true
                isRestorationDetailsExpanded = true
                addRestoreLog("Cloud restoration finished with errors. You can retry, go back, or continue.")
            }
        }
    }

    fun applyConflictResolutionsAndRestore(
        engine: BackupEngine,
        resolutions: List<FileConflictResolution>,
        service: ServiceConfig,
        remotePath: String,
        localFolder: File
    ) {
        isRestoringSettings = true
        isRestorationFailed = false
        isRestorationComplete = false
        showPostCloudRestoreReminder = false
        isRestorationDetailsExpanded = true
        restorationLogs.clear()
        restorationProgress = -1f

        scope.launch {
            addRestoreLog("Applying conflict resolutions...")
            val report = engine.resolveConflicts(
                resolutions = resolutions,
                onProgress = { cur, tot, file ->
                    if (tot > 0) {
                        restorationProgress = cur.toFloat() / tot.toFloat()
                    }
                },
                onLog = { logMsg ->
                    addRestoreLog(logMsg)
                }
            )

            val hasConfigResolved = resolutions.any { BackupEngine.isConfigFile(it.relativePath) }
            val hasConfigsInCloud = engine.hasRemoteConfigs(service, remotePath)

            if (hasConfigResolved || hasConfigsInCloud) {
                NotesHomeConfigManager.restoreSettingsFromNotesHome(localFolder, context, env) { logMsg ->
                    addRestoreLog(logMsg)
                }
                NotesHomeConfigManager.sync(context, env)
                val result = engine.performRestore(
                    serviceConfig = service.copy(remoteBasePath = remotePath),
                    conflictPolicy = ConflictResolutionPolicy.SKIP_CONFLICTS,
                    onProgress = { current, total, _ ->
                        if (total > 0) {
                            restorationProgress = current.toFloat() / total.toFloat()
                        }
                    },
                    onLog = { logMsg ->
                        addRestoreLog(logMsg)
                    }
                )
                if (result.isSuccess) {
                    if (result.hasRestoredConfigs) {
                        addRestoreLog("Restoration completed successfully.")
                        isRestorationComplete = true
                        showPostCloudRestoreReminder = true
                    } else {
                        addRestoreLog("Notes restore completed. Proceeding with onboarding setup.")
                        isRestoringSettings = false
                        pagerState.animateScrollToPage(3)
                    }
                } else {
                    isRestorationFailed = true
                    addRestoreLog("Restoration finished with errors.")
                }
            } else {
                // Notes-only restore: Enqueue remaining notes download in background!
                AournalppApplication.applicationScope.launch(Dispatchers.IO) {
                    try {
                        engine.performRestore(
                            serviceConfig = service.copy(remoteBasePath = remotePath),
                            conflictPolicy = ConflictResolutionPolicy.SKIP_CONFLICTS
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("OnboardingScreen", "Background notes restore failed", e)
                    }
                }
                isRestoringSettings = false
                pagerState.animateScrollToPage(3)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootLayoutCoordinates = it }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val radius = revealRadius.value
                if (radius > 0f) {
                    val center = revealCenter ?: Offset(size.width / 2f, size.height / 2f)
                    // Punch out the circular hole with transparent interior revealing MainActivity
                    drawCircle(
                        color = Color.Black,
                        radius = radius,
                        center = center,
                        blendMode = BlendMode.Clear
                    )
                    // Subtle glowing rim along expanding edge
                    val ringAlpha = (1f - (radius / (size.maxDimension * 0.9f)).coerceIn(0f, 1f))
                    if (ringAlpha > 0.01f) {
                        drawCircle(
                            color = Color.White.copy(alpha = ringAlpha * 0.6f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        if (activeConflicts != null && conflictEngine != null) {
            ConflictResolutionScreen(
                conflictGroups = activeConflicts!!,
                engine = conflictEngine!!,
                onNavigateBack = {
                    activeConflicts = null
                    conflictEngine = null
                    pendingConflictService = null
                    pendingConflictRemotePath = null
                    pendingConflictLocalFolder = null
                },
                onApplyResolutions = { resolutions ->
                    val srv = pendingConflictService
                    val rPath = pendingConflictRemotePath
                    val lFolder = pendingConflictLocalFolder
                    val engine = conflictEngine!!
                    activeConflicts = null
                    conflictEngine = null
                    pendingConflictService = null
                    pendingConflictRemotePath = null
                    pendingConflictLocalFolder = null

                    if (srv != null && rPath != null && lFolder != null) {
                        applyConflictResolutionsAndRestore(
                            engine = engine,
                            resolutions = resolutions,
                            service = srv,
                            remotePath = rPath,
                            localFolder = lFolder
                        )
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
            // Top Navigation & Step Indicator (Centered Dots, No Right Counter)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    if (pagerState.currentPage > 0 && !isRevealing && !isRestoringSettings) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (!isRestoringSettings) {
                        // True Mathematically Centered Progress Dots
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(totalPages) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(if (isSelected) 24.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Restoring Settings Indicator Overlay OR Main Pager Content
            if (isRestoringSettings) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = 520.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Hero Icon / Expressive Spinner
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .onGloballyPositioned { checkCircleCoordinates = it },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isRestorationComplete -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                                isRestorationFailed -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                                else -> {
                                    // Regular expressive hero spinner with inner gear counter-rotating briskly
                                    ExpressiveHeroSpinner(
                                        size = 80.dp,
                                        icon = Icons.Default.Settings,
                                        rotateIconOpposite = true,
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        iconTint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        // Title & Status
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = when {
                                    isRestorationComplete -> androidx.compose.ui.res.stringResource(R.string.msg_onboarding_restoring_done)
                                    isRestorationFailed -> androidx.compose.ui.res.stringResource(R.string.title_onboarding_restoring_failed)
                                    else -> androidx.compose.ui.res.stringResource(R.string.title_onboarding_restoring)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isRestorationFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.a11yHeading()
                            )

                            Text(
                                text = restoringStatusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Progress Indicator
                        if (!isRestorationComplete && !isRestorationFailed) {
                            if (restorationProgress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { restorationProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        // Expandable Restoration Details
                        if (restorationLogs.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isRestorationDetailsExpanded = !isRestorationDetailsExpanded }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isRestorationDetailsExpanded) {
                                        androidx.compose.ui.res.stringResource(R.string.action_hide_restoration_details)
                                    } else {
                                        androidx.compose.ui.res.stringResource(R.string.action_view_restoration_details)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (isRestorationDetailsExpanded) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AppAnimatedVisibility(
                                visible = isRestorationDetailsExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 60.dp, max = 150.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    val logListState = rememberLazyListState()
                                    LaunchedEffect(restorationLogs.size) {
                                        if (restorationLogs.isNotEmpty()) {
                                            logListState.animateScrollToItem(restorationLogs.size - 1)
                                        }
                                    }

                                    LazyColumn(
                                        state = logListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(restorationLogs) { log ->
                                            Text(
                                                text = log.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = when (log.type) {
                                                    RestoreLogType.ERROR -> MaterialTheme.colorScheme.error
                                                    RestoreLogType.SUCCESS -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Post-cloud-restore Mapping Sets reminder card and "Let me in already!" button
                        if (showPostCloudRestoreReminder) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = androidx.compose.ui.res.stringResource(R.string.title_onboarding_restore_mappings_reminder),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = androidx.compose.ui.res.stringResource(R.string.title_onboarding_restore_mappings_reminder),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )

                                        Text(
                                            text = if (restoredMappingSetsCount > 0) {
                                                androidx.compose.ui.res.stringResource(
                                                    R.string.desc_onboarding_restore_mappings_found,
                                                    restoredMappingSetsCount
                                                )
                                            } else {
                                                androidx.compose.ui.res.stringResource(
                                                    R.string.desc_onboarding_restore_mappings_reminder
                                                )
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { triggerRevealAnimation() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(R.string.action_let_me_in),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Recovery actions if failed
                        if (isRestorationFailed) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            isRestoringSettings = false
                                            isRestorationFailed = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(androidx.compose.ui.res.stringResource(R.string.action_back_to_folder_select))
                                    }

                                    Button(
                                        onClick = {
                                            retryRestorationAction?.invoke()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(androidx.compose.ui.res.stringResource(R.string.action_retry_restoration))
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        isRestoringSettings = false
                                        isRestorationFailed = false
                                        scope.launch { pagerState.animateScrollToPage(3) }
                                    }
                                ) {
                                    Text(
                                        androidx.compose.ui.res.stringResource(R.string.action_continue_without_restoring),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Main Pager Content (Not swipeable like gallery, userScrollEnabled = false, centered max width 500dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = 500.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = false
                    ) { page ->
                        when (page) {
                            0 -> OnboardingWelcomePage(
                                onGetStarted = {
                                    scope.launch { pagerState.animateScrollToPage(1) }
                                }
                            )
                            1 -> OnboardingStoragePermissionPage(
                                isStorageGranted = isPermissionGranted,
                                isNotificationGranted = isNotificationPermissionGranted,
                                onRequestStoragePermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        launchStoragePermissionSettings(context)
                                    } else {
                                        legacyPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                                            )
                                        )
                                    }
                                },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onContinue = {
                                    scope.launch { pagerState.animateScrollToPage(2) }
                                }
                            )
                            2 -> OnboardingChooseFolderPage(
                                env = env,
                                onContinue = {
                                    scope.launch { pagerState.animateScrollToPage(3) }
                                },
                                onRestoreLocal = { localFolder ->
                                    performLocalRestore(localFolder)
                                },
                                onRestoreCloud = { service, remotePath, localFolder, skipDownload, conflictPolicy ->
                                    performCloudRestore(service, remotePath, localFolder, skipDownload, conflictPolicy)
                                },
                                onConflictsDetected = { conflicts, engine, service, remotePath, localFolder ->
                                    conflictEngine = engine
                                    pendingConflictService = service
                                    pendingConflictRemotePath = remotePath
                                    pendingConflictLocalFolder = localFolder
                                    activeConflicts = conflicts
                                }
                            )
                            3 -> OnboardingSettingsPage(
                                context = context,
                                onContinue = {
                                    scope.launch { pagerState.animateScrollToPage(4) }
                                }
                            )
                            4 -> OnboardingCloudBackupPage(
                                context = context,
                                onContinue = {
                                    scope.launch { pagerState.animateScrollToPage(5) }
                                }
                            )
                            5 -> OnboardingCompletionPage(
                                onCheckCoordinates = { checkCircleCoordinates = it },
                                isRevealing = isRevealing,
                                onLetMeIn = {
                                    triggerRevealAnimation()
                                }
                            )
                        }
                    }
                }
            }

            // Minimal Background Extraction Progress Bar & Floating Expandable Ticker (Centered & Responsive)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.widthIn(max = 500.dp)) {
                    OnboardingExtractionBottomPill(
                        state = bootstrapState,
                        isExpanded = isExtractionDetailsExpanded,
                        onToggleExpand = { isExtractionDetailsExpanded = !isExtractionDetailsExpanded }
                    )
                }
            }
        }
    }
}
}
