package dev.ilamparithi.aournalpp.ui

import android.content.Context
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.ilamparithi.aournalpp.data.X11Preferences
import kotlin.math.roundToInt

private enum class WindowEdge {
    TOP, LEFT, RIGHT, BOTTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenSafeAreaEditorScreen(
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

    // Dynamic display rotation tracking (reacts to orientation changes)
    val currentRotation = remember(configuration.orientation, configuration.screenWidthDp, configuration.screenHeightDp) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }
    }

    // Canonical physical chassis insets (stored in physical device coordinates)
    val initialCustom = remember { x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_CUSTOM_EDGES, false) }
    val initialAll = remember { x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_MARGIN_ALL, 0) }
    val rawLeft = remember { if (initialCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_LEFT, 0) else initialAll }
    val rawTop = remember { if (initialCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_TOP, 0) else initialAll }
    val rawRight = remember { if (initialCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_RIGHT, 0) else initialAll }
    val rawBottom = remember { if (initialCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_BOTTOM, 0) else initialAll }
    val savedRefRotation = remember { x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_REF_ROTATION, Surface.ROTATION_0) }

    val initialPhysical = remember {
        windowToPhysicalInsets(
            SafeAreaInsets(rawLeft, rawTop, rawRight, rawBottom),
            savedRefRotation
        )
    }

    val initialDisableInMulti = remember { x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_DISABLE_IN_MULTIWINDOW, true) }
    val initialCenterTopBar = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, false) }

    var syncAllEdges by rememberSaveable { mutableStateOf(!initialCustom) }
    var physicalLeftDp by rememberSaveable { mutableIntStateOf(initialPhysical.left) }
    var physicalTopDp by rememberSaveable { mutableIntStateOf(initialPhysical.top) }
    var physicalRightDp by rememberSaveable { mutableIntStateOf(initialPhysical.right) }
    var physicalBottomDp by rememberSaveable { mutableIntStateOf(initialPhysical.bottom) }
    var disableInMultiwindow by rememberSaveable { mutableStateOf(initialDisableInMulti) }
    var centerTopBarWithinBounds by rememberSaveable { mutableStateOf(initialCenterTopBar) }

    val maxMarginDp = 80

    // Compute current window insets dynamically from physical chassis insets
    val currentPhysicalInsets = SafeAreaInsets(physicalLeftDp, physicalTopDp, physicalRightDp, physicalBottomDp)
    val currentWindowInsets = physicalToWindowInsets(currentPhysicalInsets, currentRotation)

    val leftMarginDp = currentWindowInsets.left
    val topMarginDp = currentWindowInsets.top
    val rightMarginDp = currentWindowInsets.right
    val bottomMarginDp = currentWindowInsets.bottom

    var showDiscardConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val hasUnsavedChanges = physicalLeftDp != initialPhysical.left ||
            physicalTopDp != initialPhysical.top ||
            physicalRightDp != initialPhysical.right ||
            physicalBottomDp != initialPhysical.bottom ||
            syncAllEdges != !initialCustom ||
            disableInMultiwindow != initialDisableInMulti ||
            centerTopBarWithinBounds != initialCenterTopBar

    fun handleBack() {
        if (hasUnsavedChanges) {
            showDiscardConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved safe area calibration changes. Are you sure you want to discard them?") },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = { showDiscardConfirmDialog = false }
                ) {
                    Text("Keep Editing")
                }
            }
        )
    }

    fun updateAllMargins(value: Int) {
        val clamped = value.coerceIn(0, maxMarginDp)
        physicalLeftDp = clamped
        physicalTopDp = clamped
        physicalRightDp = clamped
        physicalBottomDp = clamped
    }

    fun updateWindowEdge(edge: WindowEdge, value: Int) {
        if (syncAllEdges) {
            updateAllMargins(value)
        } else {
            val clamped = value.coerceIn(0, maxMarginDp)
            val newWindow = when (edge) {
                WindowEdge.LEFT -> currentWindowInsets.copy(left = clamped)
                WindowEdge.TOP -> currentWindowInsets.copy(top = clamped)
                WindowEdge.RIGHT -> currentWindowInsets.copy(right = clamped)
                WindowEdge.BOTTOM -> currentWindowInsets.copy(bottom = clamped)
            }
            val newPhysical = windowToPhysicalInsets(newWindow, currentRotation)
            physicalLeftDp = newPhysical.left.coerceIn(0, maxMarginDp)
            physicalTopDp = newPhysical.top.coerceIn(0, maxMarginDp)
            physicalRightDp = newPhysical.right.coerceIn(0, maxMarginDp)
            physicalBottomDp = newPhysical.bottom.coerceIn(0, maxMarginDp)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val scrimColor = Color.Black.copy(alpha = 0.55f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val screenWPx = screenW.value * density.density
        val screenHPx = screenH.value * density.density

        val halfWPx = (screenWPx / 2f).coerceAtLeast(1f)
        val halfHPx = (screenHPx / 2f).coerceAtLeast(1f)

        val leftPx = with(density) { leftMarginDp.dp.toPx() }
        val topPx = with(density) { topMarginDp.dp.toPx() }
        val rightPx = with(density) { rightMarginDp.dp.toPx() }
        val bottomPx = with(density) { bottomMarginDp.dp.toPx() }

        // 1:1 Canvas Full-Screen Scrim & Bounds Drawing
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val safeLeft = leftPx.coerceIn(0f, width / 2f)
            val safeTop = topPx.coerceIn(0f, height / 2f)
            val safeRight = (width - rightPx).coerceIn(width / 2f, width)
            val safeBottom = (height - bottomPx).coerceIn(height / 2f, height)
            val safeW = safeRight - safeLeft
            val safeH = safeBottom - safeTop

            // Draw outer darkened scrims
            if (safeTop > 0) {
                drawRect(color = scrimColor, topLeft = Offset.Zero, size = Size(width, safeTop))
            }
            if (height - safeBottom > 0) {
                drawRect(color = scrimColor, topLeft = Offset(0f, safeBottom), size = Size(width, height - safeBottom))
            }
            if (safeLeft > 0) {
                drawRect(color = scrimColor, topLeft = Offset(0f, safeTop), size = Size(safeLeft, safeH))
            }
            if (width - safeRight > 0) {
                drawRect(color = scrimColor, topLeft = Offset(safeRight, safeTop), size = Size(width - safeRight, safeH))
            }

            // Draw Safe Area Outline
            drawRect(
                color = primaryColor,
                topLeft = Offset(safeLeft, safeTop),
                size = Size(safeW, safeH),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )

            // Draw Corner HUD Brackets
            val cornerLen = 24.dp.toPx()
            val strokeWidth = 4.dp.toPx()

            // Top-Left
            drawLine(primaryColor, Offset(safeLeft, safeTop), Offset(safeLeft + cornerLen, safeTop), strokeWidth)
            drawLine(primaryColor, Offset(safeLeft, safeTop), Offset(safeLeft, safeTop + cornerLen), strokeWidth)

            // Top-Right
            drawLine(primaryColor, Offset(safeRight, safeTop), Offset(safeRight - cornerLen, safeTop), strokeWidth)
            drawLine(primaryColor, Offset(safeRight, safeTop), Offset(safeRight, safeTop + cornerLen), strokeWidth)

            // Bottom-Left
            drawLine(primaryColor, Offset(safeLeft, safeBottom), Offset(safeLeft + cornerLen, safeBottom), strokeWidth)
            drawLine(primaryColor, Offset(safeLeft, safeBottom), Offset(safeLeft, safeBottom - cornerLen), strokeWidth)

            // Bottom-Right
            drawLine(primaryColor, Offset(safeRight, safeBottom), Offset(safeRight - cornerLen, safeBottom), strokeWidth)
            drawLine(primaryColor, Offset(safeRight, safeBottom), Offset(safeRight, safeBottom - cornerLen), strokeWidth)
        }

        // ==========================================
        // Draggable Handlebars with Continuous Smooth Gesture Tracking
        // ==========================================

        var leftDragAccumulator by remember { mutableFloatStateOf(0f) }
        var leftDragStartMargin by remember { mutableIntStateOf(0) }

        // Left Handle
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = leftPx.roundToInt() - 28.dp.roundToPx(),
                        y = (screenH.toPx() / 2f - 40.dp.toPx()).roundToInt()
                    )
                }
                .size(56.dp, 80.dp)
                .pointerInput(syncAllEdges, halfWPx, currentRotation) {
                    detectDragGestures(
                        onDragStart = {
                            leftDragStartMargin = leftMarginDp
                            leftDragAccumulator = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            leftDragAccumulator += dragAmount.x
                            val deltaDp = (leftDragAccumulator / halfWPx) * maxMarginDp
                            val newInt = (leftDragStartMargin + deltaDp).roundToInt().coerceIn(0, maxMarginDp)
                            if (newInt != leftMarginDp) {
                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                updateWindowEdge(WindowEdge.LEFT, newInt)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier.size(24.dp, 40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag Left Handle",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        var rightDragAccumulator by remember { mutableFloatStateOf(0f) }
        var rightDragStartMargin by remember { mutableIntStateOf(0) }

        // Right Handle
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (screenW.toPx() - rightPx).roundToInt() - 28.dp.roundToPx(),
                        y = (screenH.toPx() / 2f - 40.dp.toPx()).roundToInt()
                    )
                }
                .size(56.dp, 80.dp)
                .pointerInput(syncAllEdges, halfWPx, currentRotation) {
                    detectDragGestures(
                        onDragStart = {
                            rightDragStartMargin = rightMarginDp
                            rightDragAccumulator = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            rightDragAccumulator += (-dragAmount.x)
                            val deltaDp = (rightDragAccumulator / halfWPx) * maxMarginDp
                            val newInt = (rightDragStartMargin + deltaDp).roundToInt().coerceIn(0, maxMarginDp)
                            if (newInt != rightMarginDp) {
                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                updateWindowEdge(WindowEdge.RIGHT, newInt)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier.size(24.dp, 40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag Right Handle",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        var topDragAccumulator by remember { mutableFloatStateOf(0f) }
        var topDragStartMargin by remember { mutableIntStateOf(0) }

        // Top Handle
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (screenW.toPx() / 2f - 40.dp.toPx()).roundToInt(),
                        y = topPx.roundToInt() - 28.dp.roundToPx()
                    )
                }
                .size(80.dp, 56.dp)
                .pointerInput(syncAllEdges, halfHPx, currentRotation) {
                    detectDragGestures(
                        onDragStart = {
                            topDragStartMargin = topMarginDp
                            topDragAccumulator = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            topDragAccumulator += dragAmount.y
                            val deltaDp = (topDragAccumulator / halfHPx) * maxMarginDp
                            val newInt = (topDragStartMargin + deltaDp).roundToInt().coerceIn(0, maxMarginDp)
                            if (newInt != topMarginDp) {
                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                updateWindowEdge(WindowEdge.TOP, newInt)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier.size(40.dp, 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag Top Handle",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        var bottomDragAccumulator by remember { mutableFloatStateOf(0f) }
        var bottomDragStartMargin by remember { mutableIntStateOf(0) }

        // Bottom Handle
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (screenW.toPx() / 2f - 40.dp.toPx()).roundToInt(),
                        y = (screenH.toPx() - bottomPx).roundToInt() - 28.dp.roundToPx()
                    )
                }
                .size(80.dp, 56.dp)
                .pointerInput(syncAllEdges, halfHPx, currentRotation) {
                    detectDragGestures(
                        onDragStart = {
                            bottomDragStartMargin = bottomMarginDp
                            bottomDragAccumulator = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            bottomDragAccumulator += (-dragAmount.y)
                            val deltaDp = (bottomDragAccumulator / halfHPx) * maxMarginDp
                            val newInt = (bottomDragStartMargin + deltaDp).roundToInt().coerceIn(0, maxMarginDp)
                            if (newInt != bottomMarginDp) {
                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                updateWindowEdge(WindowEdge.BOTTOM, newInt)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier.size(40.dp, 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag Bottom Handle",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // ==========================================
        // Responsive Center Floating Control HUD
        // Switches to 2-Column layout in compact vertical heights
        // ==========================================
        val isCompactHeight = screenH < 560.dp

        ElevatedCard(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(12.dp)
                .widthIn(max = if (isCompactHeight) 640.dp else 440.dp)
                .heightIn(max = screenH - 24.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            if (isCompactHeight) {
                // Compact Landscape 2-Column Layout
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Header & Diamond Controls (No Text Labels)
                    Column(
                        modifier = Modifier.weight(1.1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Safe Area Calibration",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Diamond Steppers (without text labels)
                        DiamondStepperControls(
                            windowInsets = currentWindowInsets,
                            syncAllEdges = syncAllEdges,
                            onToggleSync = {
                                syncAllEdges = !syncAllEdges
                                if (syncAllEdges) updateAllMargins(leftMarginDp)
                            },
                            onUpdateWindowEdge = { edge, newVal ->
                                updateWindowEdge(edge, newVal)
                            }
                        )
                    }

                    // Right Column: Presets, Toggles, and Action Buttons
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Presets
                        PresetsRow(
                            syncAllEdges = syncAllEdges,
                            currentMargin = leftMarginDp,
                            onSelectPreset = {
                                syncAllEdges = true
                                updateAllMargins(it)
                            }
                        )

                        // Checkboxes (Full Row / Text Clickable)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .toggleable(
                                    value = disableInMultiwindow,
                                    role = Role.Checkbox,
                                    onValueChange = { disableInMultiwindow = it }
                                )
                                .padding(vertical = 2.dp, horizontal = 2.dp)
                        ) {
                            Checkbox(
                                checked = disableInMultiwindow,
                                onCheckedChange = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Disable in split-screen/floating",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .toggleable(
                                    value = centerTopBarWithinBounds,
                                    role = Role.Checkbox,
                                    onValueChange = { centerTopBarWithinBounds = it }
                                )
                                .padding(vertical = 2.dp, horizontal = 2.dp)
                        ) {
                            Checkbox(
                                checked = centerTopBarWithinBounds,
                                onCheckedChange = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Center toolbar within safe bounds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Action Buttons Row
                        ActionButtonsRow(
                            onReset = { updateAllMargins(0) },
                            onCancel = { handleBack() },
                            onSave = {
                                val editor = x11Prefs.edit()
                                editor.putBoolean(X11Preferences.KEY_SAFE_AREA_CUSTOM_EDGES, !syncAllEdges)
                                editor.putInt(X11Preferences.KEY_SAFE_AREA_MARGIN_ALL, physicalLeftDp)
                                editor.putInt(X11Preferences.KEY_SAFE_AREA_LEFT, physicalLeftDp)
                                editor.putInt(X11Preferences.KEY_SAFE_AREA_TOP, physicalTopDp)
                                editor.putInt(X11Preferences.KEY_SAFE_AREA_RIGHT, physicalRightDp)
                                editor.putInt(X11Preferences.KEY_SAFE_AREA_BOTTOM, physicalBottomDp)
                                editor.putInt(X11Preferences.KEY_SAFE_AREA_REF_ROTATION, Surface.ROTATION_0)
                                editor.putBoolean(X11Preferences.KEY_SAFE_AREA_DISABLE_IN_MULTIWINDOW, disableInMultiwindow)
                                editor.putBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, centerTopBarWithinBounds)
                                editor.apply()

                                X11Preferences.notifyChanged(context, X11Preferences.KEY_SAFE_AREA_MARGIN_ALL)
                                onNavigateBack()
                            }
                        )
                    }
                }
            } else {
                // Ample Height Stacked Column Layout
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Safe Area Calibration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Drag handles or use diamond controls to inset X11 canvas away from rounded device corners.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Diamond Steppers (without text labels)
                    DiamondStepperControls(
                        windowInsets = currentWindowInsets,
                        syncAllEdges = syncAllEdges,
                        onToggleSync = {
                            syncAllEdges = !syncAllEdges
                            if (syncAllEdges) updateAllMargins(leftMarginDp)
                        },
                        onUpdateWindowEdge = { edge, newVal ->
                            updateWindowEdge(edge, newVal)
                        }
                    )

                    // Quick Presets Row
                    PresetsRow(
                        syncAllEdges = syncAllEdges,
                        currentMargin = leftMarginDp,
                        onSelectPreset = {
                            syncAllEdges = true
                            updateAllMargins(it)
                        }
                    )

                    // Multi-window / Split-screen Bypass Option (Full Row / Text Clickable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .toggleable(
                                value = disableInMultiwindow,
                                role = Role.Checkbox,
                                onValueChange = { disableInMultiwindow = it }
                            )
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = disableInMultiwindow,
                            onCheckedChange = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Disable safe area in split-screen & floating windows",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Center Top Bar Within Bounds Option (Full Row / Text Clickable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .toggleable(
                                value = centerTopBarWithinBounds,
                                role = Role.Checkbox,
                                onValueChange = { centerTopBarWithinBounds = it }
                            )
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = centerTopBarWithinBounds,
                            onCheckedChange = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Center toolbar within safe bounds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Action Buttons (Cancel, Reset, Save & Apply)
                    ActionButtonsRow(
                        onReset = { updateAllMargins(0) },
                        onCancel = { handleBack() },
                        onSave = {
                            val editor = x11Prefs.edit()
                            editor.putBoolean(X11Preferences.KEY_SAFE_AREA_CUSTOM_EDGES, !syncAllEdges)
                            editor.putInt(X11Preferences.KEY_SAFE_AREA_MARGIN_ALL, physicalLeftDp)
                            editor.putInt(X11Preferences.KEY_SAFE_AREA_LEFT, physicalLeftDp)
                            editor.putInt(X11Preferences.KEY_SAFE_AREA_TOP, physicalTopDp)
                            editor.putInt(X11Preferences.KEY_SAFE_AREA_RIGHT, physicalRightDp)
                            editor.putInt(X11Preferences.KEY_SAFE_AREA_BOTTOM, physicalBottomDp)
                            editor.putInt(X11Preferences.KEY_SAFE_AREA_REF_ROTATION, Surface.ROTATION_0)
                            editor.putBoolean(X11Preferences.KEY_SAFE_AREA_DISABLE_IN_MULTIWINDOW, disableInMultiwindow)
                            editor.putBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, centerTopBarWithinBounds)
                            editor.apply()

                            X11Preferences.notifyChanged(context, X11Preferences.KEY_SAFE_AREA_MARGIN_ALL)
                            onNavigateBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiamondStepperControls(
    windowInsets: SafeAreaInsets,
    syncAllEdges: Boolean,
    onToggleSync: () -> Unit,
    onUpdateWindowEdge: (WindowEdge, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // TOP
        Box(
            modifier = Modifier.widthIn(max = 135.dp),
            contentAlignment = Alignment.Center
        ) {
            NumericStepperField(
                value = windowInsets.top,
                onValueChange = { onUpdateWindowEdge(WindowEdge.TOP, it) }
            )
        }

        // MIDDLE ROW: LEFT - LINK - RIGHT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                NumericStepperField(
                    value = windowInsets.left,
                    onValueChange = { onUpdateWindowEdge(WindowEdge.LEFT, it) }
                )
            }

            IconButton(
                onClick = onToggleSync,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(34.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (syncAllEdges) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (syncAllEdges) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = if (syncAllEdges) "Linked" else "Individual",
                            modifier = Modifier.size(16.dp),
                            tint = if (syncAllEdges) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                NumericStepperField(
                    value = windowInsets.right,
                    onValueChange = { onUpdateWindowEdge(WindowEdge.RIGHT, it) }
                )
            }
        }

        // BOTTOM
        Box(
            modifier = Modifier.widthIn(max = 135.dp),
            contentAlignment = Alignment.Center
        ) {
            NumericStepperField(
                value = windowInsets.bottom,
                onValueChange = { onUpdateWindowEdge(WindowEdge.BOTTOM, it) }
            )
        }
    }
}

@Composable
private fun PresetsRow(
    syncAllEdges: Boolean,
    currentMargin: Int,
    onSelectPreset: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        val presets = listOf(
            0 to "0 dp",
            8 to "8 dp",
            16 to "16 dp",
            24 to "24 dp",
            32 to "32 dp"
        )
        presets.forEach { (presetVal, label) ->
            val isSelected = syncAllEdges && currentMargin == presetVal
            FilterChip(
                selected = isSelected,
                onClick = { onSelectPreset(presetVal) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onReset) {
            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }

            Button(onClick = onSave) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save & Apply")
            }
        }
    }
}

@Composable
private fun NumericStepperField(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onValueChange((value - 2).coerceAtLeast(0)) },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = "$value dp",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            IconButton(
                onClick = { onValueChange((value + 2).coerceAtMost(80)) },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
