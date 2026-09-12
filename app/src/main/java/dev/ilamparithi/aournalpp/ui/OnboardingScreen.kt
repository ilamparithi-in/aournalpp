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
import androidx.compose.material.icons.filled.Timer
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
import dev.ilamparithi.aournalpp.backup.model.ConfigSyncStatus
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.FileVersionItem
import dev.ilamparithi.aournalpp.ui.cloud.ConfigDiffActivity
import dev.ilamparithi.aournalpp.ui.cloud.ConflictDialogMode
import dev.ilamparithi.aournalpp.ui.cloud.FolderBrowserDialog
import dev.ilamparithi.aournalpp.ui.cloud.FolderBrowserMode
import dev.ilamparithi.aournalpp.ui.cloud.MultiServiceConflictDialog
import dev.ilamparithi.aournalpp.ui.cloud.ServiceConfigDialog
import dev.ilamparithi.aournalpp.utils.a11yHeading
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

import dev.ilamparithi.aournalpp.ui.onboarding.*

@Composable
fun OnboardingScreen(
    bootstrapState: BootstrapState,
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

    // Live storage permission state with lifecycle resume observer
    var isPermissionGranted by remember { mutableStateOf(checkStoragePermissionGranted(context)) }
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isPermissionGranted = checkStoragePermissionGranted(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isPermissionGranted = checkStoragePermissionGranted(context)
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
                        .widthIn(max = 500.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .onGloballyPositioned { checkCircleCoordinates = it }
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRestorationComplete) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(54.dp)
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(54.dp),
                                    strokeWidth = 4.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = if (isRestorationComplete) androidx.compose.ui.res.stringResource(R.string.msg_onboarding_restoring_done)
                                   else androidx.compose.ui.res.stringResource(R.string.title_onboarding_restoring),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.a11yHeading()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = restoringStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
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
                                isGranted = isPermissionGranted,
                                onRequestPermission = {
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
                                    isRestoringSettings = true
                                    restoringStatusText = context.getString(
                                        R.string.desc_onboarding_restoring_folder,
                                        localFolder.name
                                    )
                                    scope.launch {
                                        delay(400.milliseconds)
                                        env.setNotesDirectoryPathOnly(localFolder.absolutePath)
                                        NotesHomeConfigManager.restoreSettingsFromNotesHome(localFolder, context, env)
                                        NotesHomeConfigManager.sync(context, env)
                                        delay(600.milliseconds)
                                        isRestorationComplete = true
                                        delay(500.milliseconds)
                                        triggerRevealAnimation()
                                    }
                                },
                                onRestoreCloud = { service, remotePath, localFolder, skipDownload, conflictPolicy ->
                                    isRestoringSettings = true
                                    restoringStatusText = context.getString(
                                        R.string.desc_onboarding_restoring_cloud,
                                        service.name
                                    )
                                    scope.launch {
                                        env.setNotesDirectoryPathOnly(localFolder.absolutePath)
                                        if (!skipDownload) {
                                            val engine = BackupEngine(context, env, CredentialsVault(context))
                                            engine.performRestore(
                                                service.copy(remoteBasePath = remotePath),
                                                conflictPolicy
                                            )
                                        }
                                        NotesHomeConfigManager.restoreSettingsFromNotesHome(localFolder, context, env)
                                        NotesHomeConfigManager.sync(context, env)
                                        delay(600.milliseconds)
                                        isRestorationComplete = true
                                        delay(500.milliseconds)
                                        triggerRevealAnimation()
                                    }
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
