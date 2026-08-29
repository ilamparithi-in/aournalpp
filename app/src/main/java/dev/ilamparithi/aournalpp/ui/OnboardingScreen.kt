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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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

private const val ANDROID_STORAGE_LIMITATIONS_WIKI_URL =
    "https://github.com/ilamparithi-in/aournalpp/wiki/Android-Storage-Permissions"

private fun checkStoragePermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val read = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        read && write
    }
}

private fun launchStoragePermissionSettings(context: Context) {
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
    } else {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // fallback
        }
    }
}

@Composable
fun OnboardingScreen(
    bootstrapState: BootstrapState,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val env = remember { LinuxEnvironment(context) }
    val scope = rememberCoroutineScope()

    val totalPages = 5
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })

    // Minimal extraction details expanded state
    var isExtractionDetailsExpanded by remember { mutableStateOf(false) }

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
                    if (pagerState.currentPage > 0 && !isRevealing) {
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
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

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
                            }
                        )
                        3 -> OnboardingSettingsPage(
                            context = context,
                            onContinue = {
                                scope.launch { pagerState.animateScrollToPage(4) }
                            }
                        )
                        4 -> OnboardingCompletionPage(
                            onCheckCoordinates = { checkCircleCoordinates = it },
                            isRevealing = isRevealing,
                            onLetMeIn = {
                                if (isRevealing) return@OnboardingCompletionPage
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
                        )
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

// -----------------------------------------------------------------------------
// Step 1: Welcome Page (Clean, Focused, Responsive Centered Layout)
// -----------------------------------------------------------------------------
@Composable
private fun OnboardingWelcomePage(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero Graphic
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(SunnyShape(vertices = 8, roundness = 0.3f))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Aournal++",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "The most powerful notetaking app now on Android with an enhanced experience.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Get started",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Step 2: Storage Permission Page (Centered & Responsive)
// -----------------------------------------------------------------------------
@Composable
private fun OnboardingStoragePermissionPage(
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var showExplanationDialog by remember { mutableStateOf(false) }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Why All Files Permission?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Aournal++ runs the desktop Xournal++ engine inside an isolated Linux userland. Standard Android Scoped Storage restricts direct file path access for native C++ POSIX operations and autosaves.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Granting All Files Access allows you to store notes and configs anywhere on device storage without sandbox restrictions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExplanationDialog = false
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ANDROID_STORAGE_LIMITATIONS_WIKI_URL))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Read Wiki")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExplanationDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Storage Permission",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Aournal++ needs a home folder to store notes and config to. All files permission is required due to limitations with android.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Clickable Link to Explanation
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showExplanationDialog = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Learn more about Android storage limitations",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (isGranted)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isGranted) "All Files Access Granted" else "Permission Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isGranted) "Notes and configs will save directly to device storage."
                        else "Grant access in Android Settings to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isGranted) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grant Storage Permission",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            FilledTonalButton(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = if (isGranted) "Continue" else "Continue anyway",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Step 3: Choose Notes Folder Page (Centered & Responsive)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingChooseFolderPage(
    env: LinuxEnvironment,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var selectedPath by remember { mutableStateOf(env.getNotesDirectory().absolutePath) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val rawPath = uri.path ?: ""
            val resolved = if (rawPath.contains("primary:")) {
                val rel = rawPath.substringAfter("primary:").trim('/')
                File(Environment.getExternalStorageDirectory(), rel).absolutePath
            } else {
                rawPath
            }
            env.setNotesDirectory(resolved)
            selectedPath = resolved
            Toast.makeText(context, "Notes folder set to: $resolved", Toast.LENGTH_SHORT).show()
        }
    }

    val defaultNotesPath = remember {
        File(Environment.getExternalStorageDirectory(), "Documents/Notes").absolutePath
    }
    val defaultXournalPath = remember {
        File(Environment.getExternalStorageDirectory(), "Documents/Xournal").absolutePath
    }
    val defaultDownloadPath = remember {
        File(Environment.getExternalStorageDirectory(), "Download").absolutePath
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Notes Storage Folder",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose where your notebooks, documents, and configuration will be saved.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Current Folder Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Notes Folder",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedPath,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse Other Folder")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Chips
        Text(
            text = "Standard presets:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedPath == defaultNotesPath,
                onClick = {
                    env.setNotesDirectory(defaultNotesPath)
                    selectedPath = defaultNotesPath
                },
                label = { Text("Documents/Notes (Default)") }
            )
            FilterChip(
                selected = selectedPath == defaultXournalPath,
                onClick = {
                    env.setNotesDirectory(defaultXournalPath)
                    selectedPath = defaultXournalPath
                },
                label = { Text("Documents/Xournal") }
            )
            FilterChip(
                selected = selectedPath == defaultDownloadPath,
                onClick = {
                    env.setNotesDirectory(defaultDownloadPath)
                    selectedPath = defaultDownloadPath
                },
                label = { Text("Download") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Step 4: Quick Settings Page (Centered & Responsive)
// -----------------------------------------------------------------------------
@Composable
private fun OnboardingSettingsPage(
    context: Context,
    onContinue: () -> Unit
) {
    val aournalPrefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    // 1. Intelligent Session Recovery
    var intelligentRecovery by remember {
        mutableStateOf(aournalPrefs.getBoolean("pref_intelligent_emergency_recovery", true))
    }

    // 2. Fullscreen Canvas
    var fullscreenCanvas by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_FULLSCREEN, false))
    }

    // 3. Reduce Animations
    var reduceAnimations by remember {
        mutableStateOf(aournalPrefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false))
    }

    // 3. Screen Idle Timeout
    var idleTimeout by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, "system") ?: "system")
    }
    var showTimeoutMenu by remember { mutableStateOf(false) }

    val timeoutOptions = listOf(
        "system" to "System default",
        "never" to "Never (Keep screen on)",
        "1" to "1 minute",
        "5" to "5 minutes",
        "15" to "15 minutes"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quick Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Some settings you may want to change before getting started:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Settings Group Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                // Setting 1: Intelligent Session Recovery
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text("Intelligent Session Recovery", fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text("Auto-quarantine and safely restore unsaved crash sessions on relaunch.")
                    },
                    trailingContent = {
                        Switch(
                            checked = intelligentRecovery,
                            onCheckedChange = {
                                intelligentRecovery = it
                                aournalPrefs.edit().putBoolean("pref_intelligent_emergency_recovery", it).apply()
                            }
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Setting 2: Fullscreen Canvas
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text("Fullscreen Canvas", fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text("Hide system navigation and status bars for maximum handwriting area.")
                    },
                    trailingContent = {
                        Switch(
                            checked = fullscreenCanvas,
                            onCheckedChange = {
                                fullscreenCanvas = it
                                x11Prefs.edit().putBoolean(X11Preferences.KEY_FULLSCREEN, it).apply()
                                X11Preferences.notifyChanged(context, X11Preferences.KEY_FULLSCREEN)
                            }
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Setting 3: Screen Idle Timeout
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text("Screen Idle Timeout", fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text("Prevent device screen from turning off while writing.")
                    },
                    trailingContent = {
                        Box {
                            OutlinedButton(
                                onClick = { showTimeoutMenu = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = timeoutOptions.firstOrNull { it.first == idleTimeout }?.second
                                        ?: "System default",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }

                            DropdownMenu(
                                expanded = showTimeoutMenu,
                                onDismissRequest = { showTimeoutMenu = false }
                            ) {
                                timeoutOptions.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            idleTimeout = key
                                            x11Prefs.edit().putString(X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, key).apply()
                                            X11Preferences.notifyChanged(context, X11Preferences.KEY_SCREEN_IDLE_TIMEOUT)
                                            showTimeoutMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Setting 4: Reduce Animations
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text("Reduce Animations", fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text("Disable expressive motion effects to optimize performance on lower-end devices.")
                    },
                    trailingContent = {
                        Switch(
                            checked = reduceAnimations,
                            onCheckedChange = {
                                reduceAnimations = it
                                aournalPrefs.edit().putBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, it).apply()
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "More settings are available in the settings page.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Step 5: Completion Page (Centered & Responsive)
// -----------------------------------------------------------------------------
@Composable
private fun OnboardingCompletionPage(
    onCheckCoordinates: (LayoutCoordinates) -> Unit,
    isRevealing: Boolean,
    onLetMeIn: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(122.dp)
                    .clip(CloverShape())
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .onGloballyPositioned { onCheckCoordinates(it) }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "You're all set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Your digital notebook workspace is ready. Jump in and start creating.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "Quick tour will be available in a future update!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                enabled = !isRevealing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quick tour",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onLetMeIn,
                enabled = !isRevealing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Let me in!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Minimal Background Extraction Progress Pill (10s Auto-Dismiss after Ready)
// -----------------------------------------------------------------------------
@Composable
private fun OnboardingExtractionBottomPill(
    state: BootstrapState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val installingState = state as? BootstrapState.Installing
    val progress = installingState?.progress
    val isReady = state is BootstrapState.Ready
    val isError = state is BootstrapState.Error

    // Auto-dismiss "Linux environment ready" 10 seconds after completion
    var isReadyDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state is BootstrapState.Ready) {
            delay(10_000L)
            isReadyDismissed = true
        } else {
            isReadyDismissed = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncSpin"
    )

    AnimatedVisibility(
        visible = !isReadyDismissed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Collapsed Minimal Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isReady) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isError) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(rotation)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = when {
                                isReady -> "Linux environment ready"
                                isError -> "Initialization issue (tap for details)"
                                progress != null -> "Setting up Linux runtime • ${String.format("%.0f", progress.percentage)}%"
                                else -> "Preparing background environment..."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = if (isExpanded) "Hide" else "Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Compact progress bar under minimal header when not fully ready
                if (!isReady && !isError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (progress != null) {
                        val percentage = (progress.percentage / 100f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                // Expandable Detailed Card
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 10.dp))

                        if (progress != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Extracted Size:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${progress.extractedBytes / (1024 * 1024)} MB",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Current file: ${progress.currentFile}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (isReady) {
                            Text(
                                text = "The Linux runtime has finished extracting successfully and is awaiting app launch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (isError) {
                            val err = (state as BootstrapState.Error).throwable
                            Text(
                                text = "Error: ${err.message ?: "Unknown extraction error"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "Decompressing and setting executable permissions on system libraries...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap bar to collapse",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
