package dev.ilamparithi.aournalpp.ui

import android.content.Context
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.ilamparithi.aournalpp.data.X11Preferences
import kotlin.math.abs
import kotlin.math.roundToInt

data class ToolbarPresetOption(
    val id: String,
    val label: String,
    val normX: Float,
    val normY: Float
)

val STANDARD_TOOLBAR_PRESETS = listOf(
    ToolbarPresetOption("top_center", "Top Center", 0.5f, 0.0f),
    ToolbarPresetOption("top_left", "Top Left", 0.0f, 0.0f),
    ToolbarPresetOption("top_right", "Top Right", 1.0f, 0.0f),
    ToolbarPresetOption("bottom_center", "Bottom Center", 0.5f, 1.0f),
    ToolbarPresetOption("bottom_left", "Bottom Left", 0.0f, 1.0f),
    ToolbarPresetOption("bottom_right", "Bottom Right", 1.0f, 1.0f)
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ToolbarPositionEditorScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current

    // Make dialog window truly fullscreen edge-to-edge across display cutouts
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, view)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        }
        onDispose {}
    }

    // Display rotation & safe area insets
    val currentRotation = remember(configuration.orientation, configuration.screenWidthDp, configuration.screenHeightDp) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }
    }

    val customSafeArea = remember { x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_CUSTOM_EDGES, false) }
    val safeMarginAll = remember { x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_MARGIN_ALL, 0) }
    val safeLeft = remember { if (customSafeArea) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_LEFT, 0) else safeMarginAll }
    val safeTop = remember { if (customSafeArea) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_TOP, 0) else safeMarginAll }
    val safeRight = remember { if (customSafeArea) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_RIGHT, 0) else safeMarginAll }
    val safeBottom = remember { if (customSafeArea) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_BOTTOM, 0) else safeMarginAll }
    val refRotation = remember { x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_REF_ROTATION, Surface.ROTATION_0) }

    val rotatedInsets = remember(safeLeft, safeTop, safeRight, safeBottom, refRotation, currentRotation) {
        getRotatedSafeAreaInsets(
            calibrated = SafeAreaInsets(safeLeft, safeTop, safeRight, safeBottom),
            refRotation = refRotation,
            currentRotation = currentRotation
        )
    }

    val initialPreset = remember { x11Prefs.getString(X11Preferences.KEY_TOOLBAR_POSITION_PRESET, "top_center") ?: "top_center" }
    val initialNormX = remember { x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_X_RATIO, 0.5f) }
    val initialNormY = remember { x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_Y_RATIO, 0.0f) }
    val initialCenterWithinBounds = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, false) }

    var selectedPreset by rememberSaveable { mutableStateOf(initialPreset) }
    var normX by rememberSaveable { mutableFloatStateOf(initialNormX) }
    var normY by rememberSaveable { mutableFloatStateOf(initialNormY) }
    var centerWithinBounds by rememberSaveable { mutableStateOf(initialCenterWithinBounds) }

    // Active snapping guides state
    var isSnapVerticalCenter by remember { mutableStateOf(false) }
    var isSnapHorizontalCenter by remember { mutableStateOf(false) }
    var isSnapCutoutHorizontal by remember { mutableStateOf(false) }
    var isSnapCutoutVertical by remember { mutableStateOf(false) }

    val handleBack = { onNavigateBack() }
    BackHandler(enabled = true) { handleBack() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF14151B))
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val screenWPx = with(density) { screenW.toPx() }
        val screenHPx = with(density) { screenH.toPx() }

        // Physical Cutout metrics for snapping
        var cutoutRects by remember { mutableStateOf<List<CutoutRect>>(emptyList()) }
        DisposableEffect(view) {
            val rootInsets = ViewCompat.getRootWindowInsets(view)
            val androidRects = rootInsets?.displayCutout?.boundingRects ?: emptyList()
            cutoutRects = androidRects.map { CutoutRect(it.left, it.top, it.right, it.bottom) }
            onDispose {}
        }
        val mainCutout = cutoutRects.firstOrNull()

        // Safe Area Bounds
        val safeLeftPx = with(density) { rotatedInsets.left.dp.toPx() }
        val safeTopPx = with(density) { rotatedInsets.top.dp.toPx() }
        val safeRightPx = with(density) { rotatedInsets.right.dp.toPx() }
        val safeBottomPx = with(density) { rotatedInsets.bottom.dp.toPx() }

        val showStylusClickOverride = remember { x11Prefs.getBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, false) }
        val showTouchStylus = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, true) }
        val showTitle = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, true) }
        val showBack = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_BACK, true) }
        val showClose = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, true) }
        val showWindowSwitcher = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER, true) }
        val showKeyboard = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, true) }
        val showDragHandle = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, true) }
        val showCut = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CUT, true) }
        val showCopy = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_COPY, true) }
        val showPaste = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, true) }
        val showImage = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_IMAGE, true) }
        val pinButtonMode = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE, true) }

        var measuredToolbarWPx by remember { mutableFloatStateOf(0f) }
        var measuredToolbarHPx by remember { mutableFloatStateOf(0f) }
        val toolbarWidthPx = if (measuredToolbarWPx > 0f) measuredToolbarWPx else with(density) { 380.dp.toPx() }
        val toolbarHeightPx = if (measuredToolbarHPx > 0f) measuredToolbarHPx else with(density) { 48.dp.toPx() }

        val cutoutPlacement = rememberCutoutPlacement()
        val cutoutTopOffsetPx = with(density) { cutoutPlacement.topOffsetPx.toDp().toPx() }

        val contentLeftPx = if (centerWithinBounds) safeLeftPx else 0f
        val contentTopPx = if (centerWithinBounds) safeTopPx else if (cutoutPlacement.hasCenterCutout) cutoutTopOffsetPx else 0f
        val contentRightPx = if (centerWithinBounds) screenWPx - safeRightPx else screenWPx
        val contentBottomPx = if (centerWithinBounds) screenHPx - safeBottomPx else screenHPx

        val marginPadPx = with(density) { 8.dp.toPx() }
        val minX = contentLeftPx + marginPadPx
        val maxX = (contentRightPx - toolbarWidthPx - marginPadPx).coerceAtLeast(minX)
        val minY = contentTopPx + marginPadPx
        val maxY = (contentBottomPx - toolbarHeightPx - marginPadPx).coerceAtLeast(minY)

        val currentX = (minX + (maxX - minX) * normX.coerceIn(0f, 1f))
        val currentY = (minY + (maxY - minY) * normY.coerceIn(0f, 1f))

        // 1. Fullscreen Xournal++ Skeletal Wireframe
        XournalSkeletalCanvas(
            screenWidth = screenWPx,
            screenHeight = screenHPx,
            safeInsets = rotatedInsets,
            density = density
        )

        // 3. Dynamic Magnetic Snapping Guide Lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            )
            val guideColor = Color(0xFF64B5F6) // Material 3 Cyan/Blue guideline

            // Screen Vertical Center Guide
            if (isSnapVerticalCenter) {
                drawLine(
                    color = guideColor,
                    start = Offset(screenWPx / 2f, 0f),
                    end = Offset(screenWPx / 2f, screenHPx),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = stroke.pathEffect
                )
            }

            // Screen Horizontal Center Guide
            if (isSnapHorizontalCenter) {
                drawLine(
                    color = guideColor,
                    start = Offset(0f, screenHPx / 2f),
                    end = Offset(screenWPx, screenHPx / 2f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = stroke.pathEffect
                )
            }

            // Cutout Horizontal Guide
            if (isSnapCutoutHorizontal && mainCutout != null) {
                val cy = mainCutout.centerY
                drawLine(
                    color = Color(0xFFFFB74D), // Amber accent for cutout
                    start = Offset(0f, cy),
                    end = Offset(screenWPx, cy),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = stroke.pathEffect
                )
            }

            // Cutout Vertical Guide
            if (isSnapCutoutVertical && mainCutout != null) {
                val cx = mainCutout.centerX
                drawLine(
                    color = Color(0xFFFFB74D),
                    start = Offset(cx, 0f),
                    end = Offset(cx, screenHPx),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = stroke.pathEffect
                )
            }
        }

        // 4. Interactive Movable Preview Toolbar (Actual Canvas Top Bar Style)
        var dragAccumulatorX by remember { mutableFloatStateOf(0f) }
        var dragAccumulatorY by remember { mutableFloatStateOf(0f) }
        var dragStartX by remember { mutableFloatStateOf(0f) }
        var dragStartY by remember { mutableFloatStateOf(0f) }

        val snapThresholdPx = with(density) { 18.dp.toPx() }
        val hysteresisBufferPx = with(density) { 28.dp.toPx() }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(currentX.roundToInt(), currentY.roundToInt())
                }
                .onGloballyPositioned { coordinates ->
                    val w = coordinates.size.width.toFloat()
                    val h = coordinates.size.height.toFloat()
                    if (measuredToolbarWPx != w || measuredToolbarHPx != h) {
                        measuredToolbarWPx = w
                        measuredToolbarHPx = h
                    }
                }
                .pointerInput(maxX, maxY, screenWPx, screenHPx, mainCutout, toolbarWidthPx, toolbarHeightPx) {
                    detectDragGestures(
                        onDragStart = {
                            dragStartX = currentX
                            dragStartY = currentY
                            dragAccumulatorX = 0f
                            dragAccumulatorY = 0f
                            selectedPreset = "custom"
                            try { haptics.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulatorX += dragAmount.x
                            dragAccumulatorY += dragAmount.y

                            var targetX = (dragStartX + dragAccumulatorX).coerceIn(minX, maxX)
                            var targetY = (dragStartY + dragAccumulatorY).coerceIn(minY, maxY)

                            val toolbarCenterX = targetX + toolbarWidthPx / 2f
                            val toolbarCenterY = targetY + toolbarHeightPx / 2f

                            // --- Magnetic Snapping Calculations with Hysteresis ---
                            val screenCenterX = screenWPx / 2f
                            val screenCenterY = screenHPx / 2f

                            // 1. Vertical Screen Center Snap
                            val distV = abs(toolbarCenterX - screenCenterX)
                            if (isSnapVerticalCenter) {
                                if (distV > hysteresisBufferPx) {
                                    isSnapVerticalCenter = false
                                } else {
                                    targetX = screenCenterX - toolbarWidthPx / 2f
                                }
                            } else if (distV < snapThresholdPx) {
                                isSnapVerticalCenter = true
                                targetX = screenCenterX - toolbarWidthPx / 2f
                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                            }

                            // 2. Horizontal Screen Center Snap
                            val distH = abs(toolbarCenterY - screenCenterY)
                            if (isSnapHorizontalCenter) {
                                if (distH > hysteresisBufferPx) {
                                    isSnapHorizontalCenter = false
                                } else {
                                    targetY = screenCenterY - toolbarHeightPx / 2f
                                }
                            } else if (distH < snapThresholdPx) {
                                isSnapHorizontalCenter = true
                                targetY = screenCenterY - toolbarHeightPx / 2f
                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                            }

                            val availableSpanX = (maxX - minX).coerceAtLeast(1f)
                            val availableSpanY = (maxY - minY).coerceAtLeast(1f)
                            normX = ((targetX - minX) / availableSpanX).coerceIn(0f, 1f)
                            normY = ((targetY - minY) / availableSpanY).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isSnapVerticalCenter = false
                            isSnapHorizontalCenter = false
                            isSnapCutoutHorizontal = false
                            isSnapCutoutVertical = false
                        },
                        onDragCancel = {
                            isSnapVerticalCenter = false
                            isSnapHorizontalCenter = false
                            isSnapCutoutHorizontal = false
                            isSnapCutoutVertical = false
                        }
                    )
                }
        ) {
            Surface(
                modifier = Modifier
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 6.dp
            ) {
                FloatingToolbarLayout(
                    modifier = Modifier
                        .widthIn(max = with(density) { (screenWPx - 16.dp.toPx()).toDp() })
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    mainContent = {
                        if (showBack) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(36.dp),
                                enabled = false
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (showClose) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        if (showWindowSwitcher) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        if (showTitle) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .height(36.dp)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                InteractiveMarqueeText(
                                    text = "Physics_Lecture.xopp",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    minWidth = 90.dp,
                                    maxWidth = 220.dp
                                )
                            }
                        }

                        if (showStylusClickOverride) {
                            val modes = listOf("L", "M", "R")
                            val itemWidth = 26.dp
                            val itemHeight = 24.dp
                            val spacing = 2.dp
                            val padding = 2.dp
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Box(modifier = Modifier.padding(padding)) {
                                    Surface(
                                        modifier = Modifier.size(itemWidth, itemHeight),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        shadowElevation = 1.dp
                                    ) {}
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(spacing),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        modes.forEachIndexed { idx, label ->
                                            Box(
                                                modifier = Modifier.size(itemWidth, itemHeight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (idx == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showTouchStylus) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Draw,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (showCut || showCopy || showPaste || showImage) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    if (showCut) {
                                        IconButton(onClick = {}, modifier = Modifier.size(32.dp), enabled = false) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCut,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (showCopy) {
                                        IconButton(onClick = {}, modifier = Modifier.size(32.dp), enabled = false) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (showPaste) {
                                        IconButton(onClick = {}, modifier = Modifier.size(32.dp), enabled = false) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (showImage) {
                                        IconButton(onClick = {}, modifier = Modifier.size(32.dp), enabled = false) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showKeyboard) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(36.dp),
                                enabled = false
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    trailingContent = {
                        if (pinButtonMode) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(36.dp),
                                enabled = false
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(36.dp),
                                enabled = false
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandLess,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (showDragHandle) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
        }

        // 5. Floating Bottom Calibration HUD Control Panel
        ElevatedCard(
            modifier = if (screenH < 500.dp) {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(0.92f)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
                    .widthIn(max = 620.dp)
                    .fillMaxWidth()
            },
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Toolbar Position Placement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (selectedPreset == "custom") "Custom (X:${(normX * 100).roundToInt()}%, Y:${(normY * 100).roundToInt()}%)"
                            else STANDARD_TOOLBAR_PRESETS.firstOrNull { it.id == selectedPreset }?.label ?: "Top Center",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Drag the floating toolbar anywhere on screen (with magnetic guide snapping) or pick a preset standard anchor position.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Presets Horizontal Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    STANDARD_TOOLBAR_PRESETS.forEach { preset ->
                        val isSelected = selectedPreset == preset.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = preset.id
                                normX = preset.normX
                                normY = preset.normY
                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                            },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }

                // Center Within Safe Area Bounds Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .toggleable(
                            value = centerWithinBounds,
                            role = Role.Checkbox,
                            onValueChange = { centerWithinBounds = it }
                        )
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Checkbox(
                        checked = centerWithinBounds,
                        onCheckedChange = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confine & Center within Safe Area Bounds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Action Buttons (Reset to Default, Cancel, Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            selectedPreset = "top_center"
                            normX = 0.5f
                            normY = 0.0f
                            try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }

                    Spacer(Modifier.width(8.dp))

                    OutlinedButton(onClick = { handleBack() }) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val editor = x11Prefs.edit()
                            editor.putString(X11Preferences.KEY_TOOLBAR_POSITION_PRESET, selectedPreset)
                            editor.putFloat(X11Preferences.KEY_TOOLBAR_POS_X_RATIO, normX)
                            editor.putFloat(X11Preferences.KEY_TOOLBAR_POS_Y_RATIO, normY)
                            editor.putBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, centerWithinBounds)
                            editor.apply()

                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_POSITION_PRESET)
                            onNavigateBack()
                        }
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save & Apply")
                    }
                }
            }
        }
    }
}

/**
 * Renders a stylized, skeletal blueprint representation of Xournal++ canvas
 * (menu bar, tool palette, page list sidebar, ruled paper, status bar).
 */
@Composable
private fun XournalSkeletalCanvas(
    screenWidth: Float,
    screenHeight: Float,
    safeInsets: SafeAreaInsets,
    density: androidx.compose.ui.unit.Density
) {
    val safeLeftPx = with(density) { safeInsets.left.dp.toPx() }
    val safeTopPx = with(density) { safeInsets.top.dp.toPx() }
    val safeRightPx = with(density) { safeInsets.right.dp.toPx() }
    val safeBottomPx = with(density) { safeInsets.bottom.dp.toPx() }

    val safeW = (screenWidth - safeLeftPx - safeRightPx).coerceAtLeast(100f)
    val safeH = (screenHeight - safeTopPx - safeBottomPx).coerceAtLeast(100f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Outer dark chassis / safe area letterboxing
        val scrimColor = Color.Black.copy(alpha = 0.55f)
        if (safeTopPx > 0) drawRect(scrimColor, Offset.Zero, Size(screenWidth, safeTopPx))
        if (safeBottomPx > 0) drawRect(scrimColor, Offset(0f, screenHeight - safeBottomPx), Size(screenWidth, safeBottomPx))
        if (safeLeftPx > 0) drawRect(scrimColor, Offset(0f, safeTopPx), Size(safeLeftPx, safeH))
        if (safeRightPx > 0) drawRect(scrimColor, Offset(screenWidth - safeRightPx, safeTopPx), Size(safeRightPx, safeH))

        // Xournal++ Canvas Background (Light slate / blueprint paper)
        val canvasTopLeft = Offset(safeLeftPx, safeTopPx)
        val canvasSize = Size(safeW, safeH)
        drawRect(
            color = Color(0xFF1E2028),
            topLeft = canvasTopLeft,
            size = canvasSize
        )

        // 1. GTK Top Menu Bar Skeleton (File, Edit, View, Page, Tools, Help)
        val menuHeight = 22.dp.toPx()
        drawRect(
            color = Color(0xFF282A36),
            topLeft = canvasTopLeft,
            size = Size(safeW, menuHeight)
        )
        // Menu item skeleton pills
        var menuX = safeLeftPx + 12.dp.toPx()
        val pillH = 10.dp.toPx()
        val pillY = safeTopPx + (menuHeight - pillH) / 2f
        listOf(28.dp.toPx(), 24.dp.toPx(), 26.dp.toPx(), 28.dp.toPx(), 32.dp.toPx(), 24.dp.toPx()).forEach { pillW ->
            drawRoundRect(
                color = Color(0xFF44475A),
                topLeft = Offset(menuX, pillY),
                size = Size(pillW, pillH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            menuX += pillW + 10.dp.toPx()
        }

        // 2. GTK Main Tool Palette Skeleton (Pen, Highlighter, Eraser, Select, Text, Shape, Color swatches)
        val toolbarHeight = 36.dp.toPx()
        val toolbarTop = safeTopPx + menuHeight
        drawRect(
            color = Color(0xFF242630),
            topLeft = Offset(safeLeftPx, toolbarTop),
            size = Size(safeW, toolbarHeight)
        )
        var toolX = safeLeftPx + 12.dp.toPx()
        val toolIconSize = 18.dp.toPx()
        val toolY = toolbarTop + (toolbarHeight - toolIconSize) / 2f
        // Draw 8 tool icons placeholders
        for (i in 0..7) {
            val toolColor = if (i == 0) Color(0xFF6272A4) else Color(0xFF383A48)
            drawRoundRect(
                color = toolColor,
                topLeft = Offset(toolX, toolY),
                size = Size(toolIconSize, toolIconSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            toolX += toolIconSize + 8.dp.toPx()
        }

        // Color swatches skeleton
        toolX += 16.dp.toPx()
        val swatchColors = listOf(Color(0xFFFF5555), Color(0xFF50FA7B), Color(0xFF8BE9FD), Color(0xFFBD93F9), Color(0xFFF1FA8C))
        for (c in swatchColors) {
            drawCircle(color = c.copy(alpha = 0.7f), radius = 6.dp.toPx(), center = Offset(toolX, toolbarTop + toolbarHeight / 2f))
            toolX += 16.dp.toPx()
        }

        // 3. Left Page Thumbnail Sidebar Skeleton
        val sidebarWidth = (safeW * 0.14f).coerceIn(60.dp.toPx(), 130.dp.toPx())
        val bodyTop = toolbarTop + toolbarHeight
        val bodyHeight = safeH - menuHeight - toolbarHeight - 20.dp.toPx()
        drawRect(
            color = Color(0xFF21232D),
            topLeft = Offset(safeLeftPx, bodyTop),
            size = Size(sidebarWidth, bodyHeight)
        )

        // Thumbnail mini-page wireframes
        var thumbY = bodyTop + 14.dp.toPx()
        val thumbW = sidebarWidth - 20.dp.toPx()
        val thumbH = thumbW * 1.3f
        for (p in 0..2) {
            if (thumbY + thumbH <= bodyTop + bodyHeight) {
                drawRect(
                    color = Color(0xFF2E313E),
                    topLeft = Offset(safeLeftPx + 10.dp.toPx(), thumbY),
                    size = Size(thumbW, thumbH)
                )
                drawRect(
                    color = Color(0xFF4F5368),
                    topLeft = Offset(safeLeftPx + 10.dp.toPx(), thumbY),
                    size = Size(thumbW, thumbH),
                    style = Stroke(width = 1.dp.toPx())
                )
                thumbY += thumbH + 12.dp.toPx()
            }
        }

        // 4. Main Note Paper Canvas with Ruled Grid Lines
        val paperLeft = safeLeftPx + sidebarWidth + 16.dp.toPx()
        val paperTop = bodyTop + 12.dp.toPx()
        val paperW = (safeW - sidebarWidth - 32.dp.toPx()).coerceAtLeast(80f)
        val paperH = (bodyHeight - 24.dp.toPx()).coerceAtLeast(80f)

        drawRect(
            color = Color(0xFF252733),
            topLeft = Offset(paperLeft, paperTop),
            size = Size(paperW, paperH)
        )
        drawRect(
            color = Color(0xFF383C4F),
            topLeft = Offset(paperLeft, paperTop),
            size = Size(paperW, paperH),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Ruled lines on note paper
        val lineSpacing = 24.dp.toPx()
        var lineY = paperTop + lineSpacing
        while (lineY < paperTop + paperH - lineSpacing) {
            drawLine(
                color = Color(0xFF323646),
                start = Offset(paperLeft + 12.dp.toPx(), lineY),
                end = Offset(paperLeft + paperW - 12.dp.toPx(), lineY),
                strokeWidth = 1.dp.toPx()
            )
            lineY += lineSpacing
        }

        // Sample organic sketch path on note paper
        val sketchPath = Path().apply {
            moveTo(paperLeft + 40.dp.toPx(), paperTop + 50.dp.toPx())
            cubicTo(
                paperLeft + 90.dp.toPx(), paperTop + 30.dp.toPx(),
                paperLeft + 140.dp.toPx(), paperTop + 90.dp.toPx(),
                paperLeft + 200.dp.toPx(), paperTop + 60.dp.toPx()
            )
        }
        drawPath(
            path = sketchPath,
            color = Color(0xFF8BE9FD).copy(alpha = 0.55f),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // 5. Bottom Status Bar Skeleton
        val statusH = 20.dp.toPx()
        val statusTop = safeTopPx + safeH - statusH
        drawRect(
            color = Color(0xFF282A36),
            topLeft = Offset(safeLeftPx, statusTop),
            size = Size(safeW, statusH)
        )
        // Status indicator pill ("Page 1 of 1")
        drawRoundRect(
            color = Color(0xFF44475A),
            topLeft = Offset(safeLeftPx + 12.dp.toPx(), statusTop + 4.dp.toPx()),
            size = Size(48.dp.toPx(), 12.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        // Zoom pill ("100%")
        drawRoundRect(
            color = Color(0xFF44475A),
            topLeft = Offset(safeLeftPx + safeW - 44.dp.toPx(), statusTop + 4.dp.toPx()),
            size = Size(32.dp.toPx(), 12.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        // Safe Area Boundary Dashed Guide
        drawRect(
            color = Color(0xFF6272A4).copy(alpha = 0.6f),
            topLeft = canvasTopLeft,
            size = canvasSize,
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
        )
    }
}
