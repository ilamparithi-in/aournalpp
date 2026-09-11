package dev.ilamparithi.aournalpp.ui.canvas

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.PixelCopy
import android.view.Surface
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.termux.x11.LorieView
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.runtime.CanvasSessionManager
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.runtime.WallpaperHelper
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.FloatingToolbarLayout
import dev.ilamparithi.aournalpp.ui.InteractiveMarqueeText
import dev.ilamparithi.aournalpp.ui.SafeAreaInsets
import dev.ilamparithi.aournalpp.ui.animation.SpringSlideTransition
import dev.ilamparithi.aournalpp.ui.getRotatedSafeAreaInsets
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.ui.rememberCutoutPlacement
import dev.ilamparithi.aournalpp.ui.snap.*
import dev.ilamparithi.aournalpp.ui.theme.ExpressiveSprings
import dev.ilamparithi.aournalpp.ui.window.WindowSwitchTransitionOverlay
import dev.ilamparithi.aournalpp.ui.window.WindowSwitcherGallery
import dev.ilamparithi.aournalpp.utils.FormatUtils
import dev.ilamparithi.aournalpp.utils.WindowPreviewUtils
import dev.ilamparithi.aournalpp.utils.WindowTitleHelper
import dev.ilamparithi.aournalpp.x11.X11Viewport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CanvasScreen(
    activity: CanvasActivity,
    targetPath: String?,
    initialTitle: String?,
    openPreferences: Boolean,
    viewModel: CanvasViewModel = viewModel()
) {
    val x11Prefs = remember(activity) { X11Preferences.getPrefs(activity) }
    val isFullscreen = x11Prefs.getBoolean(X11Preferences.KEY_FULLSCREEN, false)
                val showEmergencyForceCloseDialog by remember { activity.showEmergencyForceCloseDialogState }
                val preferenceVersion by remember { activity.preferenceUpdateVersionState }

                var alwaysShowFileName by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME, false))
                }
                var startCollapsed by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_START_COLLAPSED, false))
                }
                var pinButtonMode by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE, true))
                }
                var autoCollapseTimeoutMs by remember {
                    mutableIntStateOf(x11Prefs.getInt(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, 5000))
                }

                val liveTitle by activity.sessionManager.documentTitle.collectAsState()
                val activePromptTitle by activity.sessionManager.activePromptTitle.collectAsState()
                val openWindows by activity.sessionManager.openWindows.collectAsState(initial = emptyList())
                val windowPreviewCache = viewModel.windowPreviewCache
                var transitionOutgoingBitmap by viewModel.transitionOutgoingBitmap
                var transitionIncomingBitmap by viewModel.transitionIncomingBitmap
                var transitionTargetWindow by viewModel.transitionTargetWindow
                var isTransitionForward by viewModel.isTransitionForward
                var isSwitchTransitionActive by viewModel.isSwitchTransitionActive
                var transitionSequence by viewModel.transitionSequence

                DisposableEffect(Unit) {
                    onDispose {
                        for (bmp in windowPreviewCache.values) {
                            if (!bmp.isRecycled) {
                                bmp.recycle()
                            }
                        }
                        windowPreviewCache.clear()
                        transitionOutgoingBitmap = null
                        transitionIncomingBitmap = null
                    }
                }

                val activeWindow by remember {
                    derivedStateOf { openWindows.find { it.isActive } }
                }
                val currentDisplayWindow by remember {
                    derivedStateOf {
                        if (isSwitchTransitionActive && transitionTargetWindow != null) {
                            transitionTargetWindow
                        } else {
                            activeWindow
                        }
                    }
                }
                val activeWindowIndex by remember {
                    derivedStateOf {
                        val current = currentDisplayWindow
                        if (current != null) openWindows.indexOfFirst { it.id == current.id }.coerceAtLeast(0) else 0
                    }
                }

                var hadWindowsOpen by remember { mutableStateOf(false) }
                LaunchedEffect(openWindows) {
                    if (openWindows.isNotEmpty()) {
                        hadWindowsOpen = true
                    } else if (hadWindowsOpen) {
                        delay(250)
                        if (openWindows.isEmpty() && !activity.sessionManager.isModalOrDialogOpen() && !activity.isFinishing) {
                            Log.i("CanvasActivity", "All Xournal++ windows closed. Finishing session cleanly.")
                            activity.sessionManager.stopSession()
                            try {
                                activity.sendBroadcast(Intent("dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED").setPackage(activity.packageName))
                            } catch (_: Exception) {}
                            activity.navigateBackToHome()
                            activity.finish()
                        }
                    }
                }

                androidx.compose.runtime.LaunchedEffect(liveTitle) {
                    val title = liveTitle?.removePrefix("*")?.removeSuffix("*")?.trim()
                    if (!title.isNullOrBlank() && title != "New Note" && title != "Unsaved Document" && title != "Preferences") {
                        withContext(Dispatchers.IO) {
                            val currentTarget = targetPath
                            if (currentTarget != null) {
                                val currentFile = File(currentTarget)
                                if (currentFile.name.equals(title, ignoreCase = true) || currentFile.nameWithoutExtension.equals(title, ignoreCase = true)) {
                                    dev.ilamparithi.aournalpp.data.DocumentRepository.getInstance(activity).recordNoteOpened(currentFile.absolutePath)
                                    return@withContext
                                }
                            }
                            val repo = dev.ilamparithi.aournalpp.data.DocumentRepository.getInstance(activity)
                            val root = repo.getRootNotesDirectory()
                            val directFile = File(root, if (title.endsWith(".xopp", ignoreCase = true) || title.endsWith(".pdf", ignoreCase = true) || title.endsWith(".xoj", ignoreCase = true)) title else "$title.xopp")
                            if (directFile.exists() && directFile.isFile) {
                                repo.recordNoteOpened(directFile.absolutePath)
                            }
                        }
                    }
                }

                val baseDocumentName = remember(targetPath, initialTitle) {
                    targetPath?.let { File(it).nameWithoutExtension } ?: (initialTitle ?: "New Note")
                }
                val displayTitle = remember(openWindows, currentDisplayWindow, liveTitle, activePromptTitle, alwaysShowFileName, openPreferences, baseDocumentName, initialTitle) {
                    val currentWin = currentDisplayWindow
                    val raw = when {
                        currentWin != null && currentWin.title.isNotBlank() && currentWin.title != "Xournal++" -> currentWin.title
                        !liveTitle.isNullOrBlank() && liveTitle != "Xournal++" -> liveTitle!!
                        openPreferences && (liveTitle == null || liveTitle?.removePrefix("*")?.trim() == "New Note" || liveTitle?.removePrefix("*")?.trim() == "Unsaved Document") -> "Preferences"
                        else -> initialTitle ?: "New Note"
                    }

                    val fileName = if (alwaysShowFileName) {
                        val isDirty = raw.startsWith("*") || raw.endsWith("*")
                        val clean = raw.removePrefix("*").trim()
                        val base = if (clean.equals("New Note", ignoreCase = true) || clean.equals("Unsaved Document", ignoreCase = true) || clean.equals("Preferences", ignoreCase = true)) {
                            clean
                        } else {
                            val nameWithoutExt = File(clean).nameWithoutExtension
                            if (nameWithoutExt.isNotBlank()) nameWithoutExt else clean
                        }
                        if (isDirty) "*$base" else base
                    } else {
                        raw
                    }

                    val prompt = activePromptTitle?.trim()
                    if (!prompt.isNullOrBlank()) {
                        "$fileName - $prompt"
                    } else {
                        fileName
                    }
                }

                val windowIcon = remember(displayTitle) {
                    WindowTitleHelper.resolveWindowIcon(displayTitle)
                }

                var defaultNormX by remember {
                    mutableFloatStateOf(x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_X_RATIO, 0.5f))
                }
                var defaultNormY by remember {
                    mutableFloatStateOf(x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_Y_RATIO, 0.0f))
                }
                var showStylusClickOverride by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, false))
                }
                var showTouchStylus by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, true))
                }
                var disableTouchStylusOnStylusHover by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER, true))
                }
                var rememberFingerAsStylusState by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE, false))
                }
                var isFingerAsStylus by remember {
                    mutableStateOf(
                        if (rememberFingerAsStylusState) {
                            x11Prefs.getBoolean(X11Preferences.KEY_FINGER_AS_STYLUS_ENABLED, false)
                        } else {
                            false
                        }
                    )
                }
                var stylusClickMode by remember {
                    mutableIntStateOf(1)
                }
                var showTitle by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, true))
                }
                var showBack by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_BACK, true))
                }
                var showClose by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, true))
                }
                var showWindowSwitcher by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER, true))
                }
                var showSnapLayouts by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS, true))
                }
                val snapLayoutManager = remember { SnapLayoutManager(activity) }
                var activeSnapMode by viewModel.activeSnapMode
                var isSnapMirrored by viewModel.isSnapMirrored
                var showSnapAssistHost by viewModel.showSnapAssistHost
                var activeConfiguringSlot by viewModel.activeConfiguringSlot
                var snapSlotAssignments by viewModel.snapSlotAssignments
                var snapDividers by viewModel.snapDividers
                var snapGeometries by viewModel.snapGeometries
                var showKeyboard by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, true))
                }
                var showDragHandle by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, true))
                }
                var showCut by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CUT, true))
                }
                var showCopy by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_COPY, true))
                }
                var showPaste by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, true))
                }
                var showImage by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_IMAGE, true))
                }
                var showImageSourceDialog by viewModel.showImageSourceDialog
                var showWindowSwitcherGallery by viewModel.showWindowSwitcherGallery
                var stylusHoverExpands by remember {
                    mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, true))
                }

                DisposableEffect(x11Prefs) {
                    val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                        when (key) {
                            X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE ->
                                pinButtonMode = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS ->
                                autoCollapseTimeoutMs = prefs.getInt(key, 5000)
                            X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME ->
                                alwaysShowFileName = prefs.getBoolean(key, false)
                            X11Preferences.KEY_TOOLBAR_START_COLLAPSED ->
                                startCollapsed = prefs.getBoolean(key, false)
                            X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS ->
                                stylusHoverExpands = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_TITLE ->
                                showTitle = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_BACK ->
                                showBack = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_CLOSE ->
                                showClose = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER ->
                                showWindowSwitcher = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS ->
                                showSnapLayouts = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD ->
                                showKeyboard = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE ->
                                showDragHandle = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_CUT ->
                                showCut = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_COPY ->
                                showCopy = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_PASTE ->
                                showPaste = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_SHOW_IMAGE ->
                                showImage = prefs.getBoolean(key, true)
                            X11Preferences.KEY_TOOLBAR_POS_X_RATIO ->
                                defaultNormX = prefs.getFloat(key, 0.5f)
                            X11Preferences.KEY_TOOLBAR_POS_Y_RATIO ->
                                defaultNormY = prefs.getFloat(key, 0.0f)
                            X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE ->
                                showStylusClickOverride = prefs.getBoolean(key, false)
                            X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS ->
                                showTouchStylus = prefs.getBoolean(key, true)
                            X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER ->
                                disableTouchStylusOnStylusHover = prefs.getBoolean(key, true)
                            X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE ->
                                rememberFingerAsStylusState = prefs.getBoolean(key, false)
                        }
                    }
                    x11Prefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose {
                        x11Prefs.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                LaunchedEffect(preferenceVersion) {
                    if (preferenceVersion > 0) {
                        pinButtonMode = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE, true)
                        autoCollapseTimeoutMs = x11Prefs.getInt(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, 5000)
                        alwaysShowFileName = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME, false)
                        startCollapsed = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_START_COLLAPSED, false)
                        stylusHoverExpands = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, true)
                        showTitle = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, true)
                        showBack = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_BACK, true)
                        showClose = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, true)
                        showWindowSwitcher = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER, true)
                        showSnapLayouts = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS, true)
                        showKeyboard = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, true)
                        showDragHandle = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, true)
                        showCut = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CUT, true)
                        showCopy = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_COPY, true)
                        showPaste = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, true)
                        showImage = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_IMAGE, true)
                        defaultNormX = x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_X_RATIO, 0.5f)
                        defaultNormY = x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_Y_RATIO, 0.0f)
                        showStylusClickOverride = x11Prefs.getBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, false)
                        showTouchStylus = x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, true)
                        disableTouchStylusOnStylusHover = x11Prefs.getBoolean(X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER, true)
                        rememberFingerAsStylusState = x11Prefs.getBoolean(X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE, false)
                    }
                }

                fun captureCurrentWindowPreview(onCaptured: ((Bitmap) -> Unit)? = null) {
                    val view = activity.activeLorieView ?: return
                    if (view.width <= 0 || view.height <= 0) return
                    try {
                        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                        PixelCopy.request(view, bitmap, { result ->
                            if (result == PixelCopy.SUCCESS) {
                                onCaptured?.invoke(bitmap)
                            }
                        }, Handler(Looper.getMainLooper()))
                    } catch (e: Exception) {
                        Log.w("CanvasActivity", "PixelCopy capture failed", e)
                    }
                }

                fun updateSlotPreviewsFromScreen(bmp: Bitmap) {
                    if (activeSnapMode == SnapLayoutMode.SINGLE || activeSnapMode == SnapLayoutMode.UNLOCKED || snapGeometries.isEmpty()) {
                        val activeWin = openWindows.find { it.isActive }
                        if (activeWin != null) {
                            windowPreviewCache[activeWin.id] = bmp
                        }
                    } else {
                        for (geo in snapGeometries) {
                            val winId = snapLayoutManager.slotAssignments[geo.slotIndex]
                            if (winId != null) {
                                val cropX = geo.x.coerceIn(0, bmp.width - 1)
                                val cropY = geo.y.coerceIn(0, bmp.height - 1)
                                val cropW = geo.width.coerceAtMost(bmp.width - cropX)
                                val cropH = geo.height.coerceAtMost(bmp.height - cropY)
                                if (cropW > 20 && cropH > 20) {
                                    try {
                                        val cropped = Bitmap.createBitmap(bmp, cropX, cropY, cropW, cropH)
                                        windowPreviewCache[winId] = cropped
                                    } catch (e: Exception) {
                                        Log.w("CanvasActivity", "Failed to crop slot thumbnail", e)
                                    }
                                }
                            }
                        }
                    }
                }

                fun applySnapLayout(
                    mode: SnapLayoutMode,
                    mirrored: Boolean = snapLayoutManager.isMirrored,
                    configureSlotsIfMultiWindow: Boolean = false
                ) {
                    val currentSupervisor = if (activity.isSupervisorInitialized()) activity.supervisor else return
                    val lorie = activity.activeLorieView
                    val vpW = lorie?.width?.takeIf { it > 0 } ?: activity.resources.displayMetrics.widthPixels
                    val vpH = lorie?.height?.takeIf { it > 0 } ?: activity.resources.displayMetrics.heightPixels

                    activeSnapMode = mode
                    isSnapMirrored = mirrored
                    snapLayoutManager.setMode(mode, mirrored)
                    x11Prefs.edit().putString(X11Preferences.KEY_ACTIVE_SNAP_LAYOUT, mode.id).apply()

                    val isSnapActive = (mode != SnapLayoutMode.SINGLE)
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        currentSupervisor.updateOpenboxSnapMode(isSnapActive)
                    }

                    when (mode) {
                        SnapLayoutMode.UNLOCKED -> {
                            showSnapAssistHost = false
                            snapSlotAssignments = emptyMap()
                            snapLayoutManager.clearAssignments()
                            snapDividers = emptyList()
                            snapGeometries = emptyList()
                            openWindows.forEach {
                                currentSupervisor.setWindowMaximized(it.id, false)
                                currentSupervisor.setWindowDecorations(it.id, decorated = true)
                            }
                        }
                        SnapLayoutMode.SINGLE -> {
                            showSnapAssistHost = false
                            snapSlotAssignments = emptyMap()
                            snapLayoutManager.clearAssignments()
                            snapDividers = emptyList()
                            snapGeometries = emptyList()
                            val wins = openWindows.ifEmpty { currentSupervisor.queryOpenWindows() }
                            wins.forEach { win ->
                                currentSupervisor.setWindowDecorations(win.id, decorated = false)
                                currentSupervisor.setWindowMaximized(win.id, true)
                            }
                            val activeWin = wins.find { it.isActive } ?: wins.firstOrNull()
                            if (activeWin != null) {
                                currentSupervisor.activateWindow(activeWin.id)
                            }
                        }
                        SnapLayoutMode.SPLIT_TWO, SnapLayoutMode.SPLIT_THREE, SnapLayoutMode.GRID_FOUR -> {
                            openWindows.forEach { currentSupervisor.setWindowDecorations(it.id, decorated = false) }
                            if (vpW > 0 && vpH > 0) {
                                val geometries = snapLayoutManager.calculateGeometries(vpW, vpH)
                                val dividers = snapLayoutManager.calculateDividerGeometries(vpW, vpH)
                                snapGeometries = geometries
                                snapDividers = dividers
                            }

                            if (configureSlotsIfMultiWindow && openWindows.size >= mode.minWindows) {
                                snapLayoutManager.clearAssignments()
                                snapSlotAssignments = emptyMap()
                                showSnapAssistHost = true
                                activeConfiguringSlot = null
                            } else {
                                if (vpW > 0 && vpH > 0) {
                                    val assignments = snapLayoutManager.buildSnapAssignments(vpW, vpH, openWindows)
                                    snapSlotAssignments = snapLayoutManager.slotAssignments.toMap()
                                    if (assignments.isNotEmpty()) {
                                        currentSupervisor.snapWindowsBatch(assignments)
                                        val activeId = openWindows.find { it.isActive }?.id
                                        val targetToActivate = if (activeId != null && assignments.any { it.windowId == activeId }) {
                                            activeId
                                        } else {
                                            assignments.firstOrNull()?.windowId
                                        }
                                        if (targetToActivate != null) {
                                            currentSupervisor.activateWindow(targetToActivate)
                                        }
                                    }
                                }
                                showSnapAssistHost = false
                            }
                        }
                    }
                }

                fun performWindowSwitch(targetWindow: dev.ilamparithi.aournalpp.runtime.ProcessSupervisor.X11WindowInfo, isForward: Boolean) {
                    val activeWin = openWindows.find { it.isActive }
                    val view = activity.activeLorieView
                    val currentW = view?.width ?: 0
                    val currentH = view?.height ?: 0

                    if (activeSnapMode != SnapLayoutMode.SINGLE && activeSnapMode != SnapLayoutMode.UNLOCKED) {
                        activity.lifecycleScope.launch(Dispatchers.IO) {
                            activity.sessionManager.switchToWindow(targetWindow.id)
                        }
                        return
                    }

                    if (isSwitchTransitionActive) {
                        // Rapid switching / spamming: advance immediately to the next target
                        val previousTarget = transitionTargetWindow
                        transitionOutgoingBitmap = transitionIncomingBitmap
                            ?: previousTarget?.let { windowPreviewCache[it.id] }
                            ?: activeWin?.let { windowPreviewCache[it.id] }
                        transitionIncomingBitmap = windowPreviewCache[targetWindow.id]
                        transitionTargetWindow = targetWindow
                        isTransitionForward = isForward
                        transitionSequence++
                        activity.lifecycleScope.launch(Dispatchers.IO) {
                            activity.sessionManager.switchToWindow(targetWindow.id)
                        }
                        return
                    }

                    val cachedCurrent = activeWin?.let { windowPreviewCache[it.id] }

                    fun startSwitchWithBitmap(currentBmp: Bitmap?) {
                        if (activeWin != null && currentBmp != null) {
                            windowPreviewCache[activeWin.id] = currentBmp
                        }
                        transitionOutgoingBitmap = currentBmp ?: activeWin?.let { windowPreviewCache[it.id] }
                        transitionIncomingBitmap = windowPreviewCache[targetWindow.id]
                        transitionTargetWindow = targetWindow
                        isTransitionForward = isForward
                        transitionSequence++
                        isSwitchTransitionActive = true
                    }

                    if (WindowPreviewUtils.isFreezeFrameDimensionMatching(cachedCurrent, currentW, currentH)) {
                        startSwitchWithBitmap(cachedCurrent)
                        captureCurrentWindowPreview { freshBmp ->
                            if (activeWin != null) {
                                windowPreviewCache[activeWin.id] = freshBmp
                            }
                        }
                    } else {
                        // Dimension mismatch (e.g. window was resized) or cache miss:
                        // capture fresh freeze frame at current size so the transition never stretches
                        captureCurrentWindowPreview { currentBmp ->
                            startSwitchWithBitmap(currentBmp)
                        }
                    }
                }

                fun performQuickSwitch() {
                    val wins = openWindows.ifEmpty { activity.sessionManager.queryOpenWindows() }
                    if (wins.isEmpty()) return
                    val currentIdx = if (isSwitchTransitionActive && transitionTargetWindow != null) {
                        wins.indexOfFirst { it.id == transitionTargetWindow?.id }
                    } else {
                        wins.indexOfFirst { it.isActive }
                    }
                    val nextIdx = if (currentIdx >= 0) (currentIdx + 1) % wins.size else 0
                    val target = wins[nextIdx]
                    performWindowSwitch(target, isForward = true)
                }

                BackHandler(enabled = true) {
                    if (showSnapAssistHost) {
                        showSnapAssistHost = false
                    } else if (showWindowSwitcherGallery) {
                        showWindowSwitcherGallery = false
                    } else {
                        activity.handleSmartBackPress()
                    }
                }

                // Track previous window IDs and MRU order for immediate background replacement
                var previousWindowIds by remember { mutableStateOf(openWindows.map { it.id }.toSet()) }
                val windowMruList = remember { mutableStateListOf<String>() }

                LaunchedEffect(activeWindow?.id) {
                    val id = activeWindow?.id ?: return@LaunchedEffect
                    windowMruList.remove(id)
                    windowMruList.add(0, id)
                }

                // Dynamic window replacement & step-down degradation when a window is closed in snap layouts
                LaunchedEffect(openWindows) {
                    val currentIds = openWindows.map { it.id }.toSet()
                    val closedIds = previousWindowIds.minus(currentIds)
                    previousWindowIds = currentIds
                    windowMruList.removeAll { it !in currentIds }
                    for (win in openWindows) {
                        if (win.id !in windowMruList) {
                            windowMruList.add(win.id)
                        }
                    }

                    // Evict and recycle previews for closed windows
                    for (closedId in closedIds) {
                        val bmp = windowPreviewCache.remove(closedId)
                        if (bmp != null && !bmp.isRecycled) {
                            bmp.recycle()
                        }
                    }

                    // Cap preview cache to 8 entries
                    while (windowPreviewCache.size > 8) {
                        val oldest = windowMruList.lastOrNull { it in windowPreviewCache.keys }
                            ?: windowPreviewCache.keys.firstOrNull()
                        if (oldest != null) {
                            val bmp = windowPreviewCache.remove(oldest)
                            if (bmp != null && !bmp.isRecycled) {
                                bmp.recycle()
                            }
                        } else {
                            break
                        }
                    }

                    val needsDegradation = openWindows.isNotEmpty() &&
                        activeSnapMode != SnapLayoutMode.SINGLE &&
                        activeSnapMode != SnapLayoutMode.UNLOCKED &&
                        openWindows.size < activeSnapMode.minWindows

                    if ((closedIds.isNotEmpty() || needsDegradation) && activeSnapMode != SnapLayoutMode.UNLOCKED) {
                        val resolution = snapLayoutManager.handleWindowClosed(
                            currentOpenWindows = openWindows,
                            mruOrder = windowMruList.toList()
                        )

                        if (resolution.modeChanged) {
                            // Window count dropped below n: drop to n-1 snap layout
                            applySnapLayout(
                                mode = resolution.newMode,
                                mirrored = snapLayoutManager.isMirrored,
                                configureSlotsIfMultiWindow = false
                            )
                        } else if (resolution.replacedSlotIndex != null) {
                            // Window count >= n: replace closed window with immediate background window
                            snapSlotAssignments = snapLayoutManager.slotAssignments.toMap()
                            val lorie = activity.activeLorieView
                            val vpW = lorie?.width?.takeIf { it > 0 } ?: activity.resources.displayMetrics.widthPixels
                            val vpH = lorie?.height?.takeIf { it > 0 } ?: activity.resources.displayMetrics.heightPixels
                            if (vpW > 0 && vpH > 0 && activity.isSupervisorInitialized()) {
                                val assignments = snapLayoutManager.buildSnapAssignments(vpW, vpH, openWindows)
                                if (assignments.isNotEmpty()) {
                                    activity.supervisor.snapWindowsBatch(assignments)
                                    val activeId = openWindows.find { it.isActive }?.id
                                    val targetToActivate = if (activeId != null && assignments.any { it.windowId == activeId }) {
                                        activeId
                                    } else {
                                        assignments.firstOrNull()?.windowId
                                    }
                                    if (targetToActivate != null) {
                                        activity.supervisor.activateWindow(targetToActivate)
                                    }
                                }
                            }
                        } else {
                            snapSlotAssignments = snapLayoutManager.slotAssignments.toMap()
                        }
                    }
                }

                val wallpaperBitmap = remember {
                    WallpaperHelper.resolveWallpaperBitmap(activity).asImageBitmap()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val canvasWidthPx = constraints.maxWidth.toFloat()
                        val canvasHeightPx = constraints.maxHeight.toFloat()

                        // Automatically re-adapt active snap layout on orientation/viewport dimension changes
                        LaunchedEffect(canvasWidthPx, canvasHeightPx, activeSnapMode, isSnapMirrored, showSnapAssistHost) {
                            if (activeSnapMode != SnapLayoutMode.SINGLE && activeSnapMode != SnapLayoutMode.UNLOCKED) {
                                val vpW = canvasWidthPx.toInt()
                                val vpH = canvasHeightPx.toInt()
                                if (vpW > 0 && vpH > 0) {
                                    snapGeometries = snapLayoutManager.calculateGeometries(vpW, vpH)
                                    snapDividers = snapLayoutManager.calculateDividerGeometries(vpW, vpH)
                                    // Never pre-assign windows or snap batch while user is configuring slots in SnapAssistHost!
                                    if (!showSnapAssistHost && snapLayoutManager.slotAssignments.isNotEmpty()) {
                                        val assignments = snapLayoutManager.buildSnapAssignments(vpW, vpH, openWindows)
                                        snapSlotAssignments = snapLayoutManager.slotAssignments.toMap()
                                        if (assignments.isNotEmpty() && activity.isSupervisorInitialized()) {
                                            activity.supervisor.snapWindowsBatch(assignments)
                                            val activeId = openWindows.find { it.isActive }?.id
                                            val targetToActivate = if (activeId != null && assignments.any { it.windowId == activeId }) {
                                                activeId
                                            } else {
                                                assignments.firstOrNull()?.windowId
                                            }
                                            if (targetToActivate != null) {
                                                activity.supervisor.activateWindow(targetToActivate)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Debounced window resize & preview synchronization
                        // Ensures active and background window previews match new window size without thrashing during active divider drag
                        LaunchedEffect(canvasWidthPx, canvasHeightPx, activeWindow?.id, isSwitchTransitionActive) {
                            val curActive = activeWindow
                            if (curActive != null && !isSwitchTransitionActive && !showSnapAssistHost && !showWindowSwitcherGallery && canvasWidthPx > 0 && canvasHeightPx > 0) {
                                delay(300)
                                val currentId = curActive.id
                                val view = activity.activeLorieView
                                if (view != null && view.width > 0 && view.height > 0) {
                                    captureCurrentWindowPreview { freshBmp ->
                                        if (activeSnapMode == SnapLayoutMode.SINGLE || activeSnapMode == SnapLayoutMode.UNLOCKED || snapGeometries.isEmpty()) {
                                            windowPreviewCache[currentId] = freshBmp
                                        } else {
                                            updateSlotPreviewsFromScreen(freshBmp)
                                        }
                                    }
                                }
                            }
                        }

                        // Wallpaper Backdrop Layer (covers edge-to-edge)
                        Image(
                            bitmap = wallpaperBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        val currentRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            activity.display?.rotation ?: Surface.ROTATION_0
                        } else {
                            @Suppress("DEPRECATION")
                            activity.windowManager.defaultDisplay.rotation
                        }

                        val safeCustom = remember { x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_CUSTOM_EDGES, false) }
                        val safeAll = remember { x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_MARGIN_ALL, 0) }
                        val rawLeft = remember(safeCustom, safeAll) { if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_LEFT, 0) else safeAll }
                        val rawTop = remember(safeCustom, safeAll) { if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_TOP, 0) else safeAll }
                        val rawRight = remember(safeCustom, safeAll) { if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_RIGHT, 0) else safeAll }
                        val rawBottom = remember(safeCustom, safeAll) { if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_BOTTOM, 0) else safeAll }
                        val refRotation = remember { x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_REF_ROTATION, Surface.ROTATION_0) }
                        val disableInMulti = remember { x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_DISABLE_IN_MULTIWINDOW, true) }
                        var centerTopBarWithinBounds by remember {
                            mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, false))
                        }

                        LaunchedEffect(preferenceVersion) {
                            if (preferenceVersion > 0) {
                                centerTopBarWithinBounds = x11Prefs.getBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, false)
                            }
                        }

                        val isAndroidMultiWindow = activity.isInMultiWindowMode
                        val effectiveInsets = remember(disableInMulti, isAndroidMultiWindow, rawLeft, rawTop, rawRight, rawBottom, refRotation, currentRotation) {
                            if (disableInMulti && isAndroidMultiWindow) {
                                SafeAreaInsets(0, 0, 0, 0)
                            } else {
                                getRotatedSafeAreaInsets(
                                    calibrated = SafeAreaInsets(rawLeft, rawTop, rawRight, rawBottom),
                                    refRotation = refRotation,
                                    currentRotation = currentRotation
                                )
                            }
                        }

                        val systemBarPadding = if (isFullscreen) {
                            PaddingValues(0.dp)
                        } else {
                            WindowInsets.systemBars.asPaddingValues()
                        }

                        val viewportModifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = effectiveInsets.left.dp + systemBarPadding.calculateStartPadding(LayoutDirection.Ltr),
                                top = effectiveInsets.top.dp + systemBarPadding.calculateTopPadding(),
                                end = effectiveInsets.right.dp + systemBarPadding.calculateEndPadding(LayoutDirection.Ltr),
                                bottom = effectiveInsets.bottom.dp + systemBarPadding.calculateBottomPadding()
                            )

                        X11Viewport(
                            modifier = viewportModifier,
                            onLorieViewReady = { lorieView ->
                                activity.activeLorieView = lorieView
                                activity.setupDragAndDropListener(lorieView)
                                activity.sessionManager.startSession(lorieView, targetPath, openPreferences)
                            },
                            onInputHandlerReady = { handler ->
                                activity.inputHandler = handler
                                handler.setDisableTouchStylusOnStylusHover(disableTouchStylusOnStylusHover)
                                handler.setTouchStylusStateListener {
                                    activity.runOnUiThread {
                                        isFingerAsStylus = false
                                        if (rememberFingerAsStylusState) {
                                            x11Prefs.edit().putBoolean(X11Preferences.KEY_FINGER_AS_STYLUS_ENABLED, false).apply()
                                        }
                                    }
                                }
                                handler.setFingerAsStylusEnabled(isFingerAsStylus)
                                handler.setStylusInputHelperMode(stylusClickMode)
                            },
                            onInputSenderReady = { sender ->
                                activity.inputSender = sender
                            },
                            onPenMapperReady = { mapper ->
                                activity.penMapper = mapper
                            }
                        )

                        // Experimental Spring Slide Window Switch Transition Overlay (directly over viewport)
                        if (isSwitchTransitionActive && transitionTargetWindow != null) {
                            val targetWin = transitionTargetWindow!!
                            val targetTitle = targetWin.title.ifBlank { "Note" }
                            val targetIcon = remember(targetTitle) { WindowTitleHelper.resolveWindowIcon(targetTitle) }
                            val isTargetActive = remember(targetWin.id, openWindows) {
                                openWindows.any { it.id == targetWin.id && it.isActive }
                            }

                            Box(
                                modifier = viewportModifier
                                    .zIndex(10f)
                            ) {
                                androidx.compose.runtime.key(transitionSequence) {
                                    WindowSwitchTransitionOverlay(
                                        outgoingBitmap = transitionOutgoingBitmap,
                                        incomingBitmap = transitionIncomingBitmap,
                                        targetTitle = targetTitle,
                                        targetIcon = targetIcon,
                                        wallpaperBitmap = wallpaperBitmap,
                                        isForward = isTransitionForward,
                                        onStarted = {
                                            activity.lifecycleScope.launch(Dispatchers.IO) {
                                                activity.sessionManager.switchToWindow(targetWin.id)
                                            }
                                        },
                                        onTransitionFinished = {
                                            isSwitchTransitionActive = false
                                            transitionTargetWindow = null
                                            transitionOutgoingBitmap = null
                                            transitionIncomingBitmap = null
                                            activity.lifecycleScope.launch {
                                                kotlinx.coroutines.delay(150)
                                                captureCurrentWindowPreview { bmp ->
                                                    windowPreviewCache[targetWin.id] = bmp
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Snap Divider Overlay (Real-time draggable resize handlebars along snap borders)
                        if (activeSnapMode != SnapLayoutMode.SINGLE && activeSnapMode != SnapLayoutMode.UNLOCKED && !showSnapAssistHost) {
                            val vpW = activity.activeLorieView?.width?.takeIf { it > 0 } ?: canvasWidthPx.toInt()
                            val vpH = activity.activeLorieView?.height?.takeIf { it > 0 } ?: canvasHeightPx.toInt()
                            Box(
                                modifier = viewportModifier
                                    .zIndex(15f)
                            ) {
                                SnapDividerOverlay(
                                    viewportWidth = vpW,
                                    viewportHeight = vpH,
                                    dividers = snapDividers,
                                    onUpdateRatio = { id, ratio ->
                                        snapLayoutManager.updateDividerRatio(id, ratio)
                                        val w = activity.activeLorieView?.width?.takeIf { it > 0 } ?: canvasWidthPx.toInt()
                                        val h = activity.activeLorieView?.height?.takeIf { it > 0 } ?: canvasHeightPx.toInt()
                                        snapDividers = snapLayoutManager.calculateDividerGeometries(w, h)
                                        snapGeometries = snapLayoutManager.calculateGeometries(w, h)
                                        val assignments = snapLayoutManager.buildSnapAssignments(w, h, openWindows)
                                        if (assignments.isNotEmpty() && activity.isSupervisorInitialized()) {
                                            activity.supervisor.snapWindowsBatch(assignments)
                                        }
                                    },
                                    onResetRatio = { id ->
                                        snapLayoutManager.resetDividerRatios()
                                        val w = activity.activeLorieView?.width?.takeIf { it > 0 } ?: canvasWidthPx.toInt()
                                        val h = activity.activeLorieView?.height?.takeIf { it > 0 } ?: canvasHeightPx.toInt()
                                        snapDividers = snapLayoutManager.calculateDividerGeometries(w, h)
                                        snapGeometries = snapLayoutManager.calculateGeometries(w, h)
                                        val assignments = snapLayoutManager.buildSnapAssignments(w, h, openWindows)
                                        if (assignments.isNotEmpty() && activity.isSupervisorInitialized()) {
                                            activity.supervisor.snapWindowsBatch(assignments)
                                        }
                                    },
                                    onDragEnd = {
                                        activity.lifecycleScope.launch {
                                            delay(150)
                                            captureCurrentWindowPreview { freshBmp ->
                                                if (freshBmp != null) {
                                                    updateSlotPreviewsFromScreen(freshBmp)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Snap Assist Segment Host (In-segment Note Selection Gallery)
                        if (showSnapAssistHost && snapGeometries.isNotEmpty()) {
                            Box(
                                modifier = viewportModifier
                                    .zIndex(20f)
                            ) {
                                SnapLayoutSegmentHost(
                                    geometries = snapGeometries,
                                    openWindows = openWindows,
                                    previewCache = windowPreviewCache,
                                    assignedSlotMap = snapSlotAssignments,
                                    activeConfiguringSlot = activeConfiguringSlot,
                                    onSelectWindowForSlot = { slotIdx, selectedWin ->
                                        val totalSlots = snapGeometries.size
                                        val allSlotsAssigned = snapLayoutManager.assignSlotAndAutoFillNthIfExact(
                                            slotIndex = slotIdx,
                                            windowId = selectedWin.id,
                                            totalSlots = totalSlots,
                                            allOpenWindows = openWindows
                                        )
                                        snapSlotAssignments = snapLayoutManager.slotAssignments.toMap()

                                        val vpW = activity.activeLorieView?.width?.takeIf { it > 0 } ?: canvasWidthPx.toInt()
                                        val vpH = activity.activeLorieView?.height?.takeIf { it > 0 } ?: canvasHeightPx.toInt()

                                        if (allSlotsAssigned) {
                                            val assignments = snapLayoutManager.buildSnapAssignments(vpW, vpH, openWindows)
                                            if (assignments.isNotEmpty() && activity.isSupervisorInitialized()) {
                                                activity.supervisor.snapWindowsBatch(assignments)
                                                val activeId = openWindows.find { it.isActive }?.id
                                                val targetToActivate = if (activeId != null && assignments.any { it.windowId == activeId }) {
                                                    activeId
                                                } else {
                                                    assignments.firstOrNull()?.windowId
                                                }
                                                if (targetToActivate != null) {
                                                    activity.supervisor.activateWindow(targetToActivate)
                                                }
                                            }
                                            showSnapAssistHost = false
                                            activeConfiguringSlot = null
                                            activity.lifecycleScope.launch {
                                                delay(200)
                                                captureCurrentWindowPreview { freshBmp ->
                                                    if (freshBmp != null) {
                                                        updateSlotPreviewsFromScreen(freshBmp)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onSlotClicked = { slotIdx ->
                                        activeConfiguringSlot = slotIdx
                                    },
                                    onDismiss = {
                                        showSnapAssistHost = false
                                        activeConfiguringSlot = null
                                        snapLayoutManager.clearAssignments()
                                        snapSlotAssignments = emptyMap()
                                        applySnapLayout(SnapLayoutMode.SINGLE, configureSlotsIfMultiWindow = false)
                                    }
                                )
                            }
                        }

                        // Floating Toolbar Overlay with Isolated Recomposition Scope (stays on top of window animations)
                        FloatingToolbarOverlay(
                            canvasWidthPx = canvasWidthPx,
                            canvasHeightPx = canvasHeightPx,
                            defaultNormX = defaultNormX,
                            defaultNormY = defaultNormY,
                            centerTopBarWithinBounds = centerTopBarWithinBounds,
                            effectiveInsets = effectiveInsets,
                            systemBarPadding = systemBarPadding,
                            isFullscreen = isFullscreen,
                            displayTitle = displayTitle,
                            windowIcon = windowIcon,
                            windowIndex = activeWindowIndex,
                            startCollapsed = startCollapsed,
                            pinButtonMode = pinButtonMode,
                            autoCollapseTimeoutMs = autoCollapseTimeoutMs,
                            showStylusClickOverride = showStylusClickOverride,
                            showTouchStylus = showTouchStylus,
                            isFingerAsStylus = isFingerAsStylus,
                            onToggleFingerAsStylus = {
                                val next = !isFingerAsStylus
                                isFingerAsStylus = next
                                activity.inputHandler?.setFingerAsStylusEnabled(next)
                                if (rememberFingerAsStylusState) {
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_FINGER_AS_STYLUS_ENABLED, next).apply()
                                }
                            },
                            stylusClickMode = stylusClickMode,
                            onStylusClickModeChange = { mode ->
                                stylusClickMode = mode
                                activity.inputHandler?.setStylusInputHelperMode(mode)
                            },
                            showTitle = showTitle,
                            showBack = showBack,
                            showClose = showClose,
                            showWindowSwitcher = showWindowSwitcher && (openWindows.size > 1),
                            showSnapLayouts = showSnapLayouts && (openWindows.size > 1),
                            activeSnapMode = activeSnapMode,
                            isSnapMirrored = isSnapMirrored,
                            onSelectSnapMode = { mode, mirrored ->
                                applySnapLayout(mode, mirrored, configureSlotsIfMultiWindow = true)
                            },
                            onToggleSnapMirror = {
                                snapLayoutManager.toggleMirrored()
                                applySnapLayout(activeSnapMode, snapLayoutManager.isMirrored, configureSlotsIfMultiWindow = false)
                            },
                            openWindowCount = openWindows.size.coerceAtLeast(1),
                            onQuickSwitchWindow = { performQuickSwitch() },
                            onOpenWindowGallery = {
                                val currentActiveId = openWindows.find { it.isActive }?.id
                                captureCurrentWindowPreview { bmp ->
                                    if (currentActiveId != null && bmp != null) {
                                        windowPreviewCache[currentActiveId] = bmp
                                    }
                                    showWindowSwitcherGallery = true
                                }
                            },
                            showKeyboard = showKeyboard,
                            showDragHandle = showDragHandle,
                            showCut = showCut,
                            showCopy = showCopy,
                            showPaste = showPaste,
                            showImage = showImage,
                            onOpenImageSelector = {
                                activity.lifecycleScope.launch {
                                    if (activity.sessionManager.isModalOrDialogOpen()) {
                                        Toast.makeText(activity, "Close open dialogs before inserting image", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showImageSourceDialog = true
                                    }
                                }
                            },
                            stylusHoverExpands = stylusHoverExpands,
                            onSmartBackPress = { activity.handleSmartBackPress() },
                            onCloseWindow = { activity.handleCloseWindow() },
                            onToggleKeyboard = {
                                activity.activeLorieView?.let { view ->
                                    val insetsCtrl = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                                    if (activity.isKeyboardOpenState.value) {
                                        view.setKeyboardVisible(false)
                                        insetsCtrl.hide(WindowInsetsCompat.Type.ime())
                                    } else {
                                        view.requestFocus()
                                        view.setKeyboardVisible(true)
                                        insetsCtrl.show(WindowInsetsCompat.Type.ime())
                                    }
                                } ?: run {
                                    val insetsCtrl = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                                    if (activity.isKeyboardOpenState.value) {
                                        insetsCtrl.hide(WindowInsetsCompat.Type.ime())
                                    } else {
                                        insetsCtrl.show(WindowInsetsCompat.Type.ime())
                                    }
                                }
                            },
                            onInjectShortcut = { keyCode, shortcutStr ->
                                activity.injectKeyboardShortcut(keyCode, shortcutStr)
                            },
                            isKeyboardOpen = activity.isKeyboardOpenState.value
                        )

                        // Window Switcher Gallery Overlay (Long-press on Window Switcher)
                        if (showWindowSwitcherGallery) {
                            val displayWindows = openWindows.ifEmpty {
                                activity.sessionManager.queryOpenWindows()
                            }
                            WindowSwitcherGallery(
                                windows = displayWindows,
                                previewCache = windowPreviewCache,
                                onSelectWindow = { selectedWin ->
                                    showWindowSwitcherGallery = false
                                    val currentWin = openWindows.find { it.isActive }
                                    if (selectedWin.id == currentWin?.id || selectedWin.isActive) {
                                        // Already focused window, do not play slide animation
                                        return@WindowSwitcherGallery
                                    }
                                    val currentIdx = openWindows.indexOfFirst { it.isActive }
                                    val targetIdx = openWindows.indexOfFirst { it.id == selectedWin.id }
                                    performWindowSwitch(selectedWin, isForward = targetIdx >= currentIdx)
                                },
                                onCloseWindow = { winToClose ->
                                    activity.lifecycleScope.launch(Dispatchers.IO) {
                                        activity.sessionManager.closeSpecificWindow(winToClose.id)
                                    }
                                },
                                onDismiss = {
                                    showWindowSwitcherGallery = false
                                }
                            )
                        }

                        // Onscreen Mouse Helper Overlay for Trackpad mode
                        val showMouseHelper = remember {
                            x11Prefs.getBoolean(dev.ilamparithi.aournalpp.data.X11Preferences.KEY_SHOW_MOUSE_HELPER, false)
                                && x11Prefs.getString(dev.ilamparithi.aournalpp.data.X11Preferences.KEY_TOUCH_MODE, "3") == "1"
                        }
                        if (showMouseHelper) {
                            Surface(
                                modifier = if (isFullscreen) {
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .navigationBarsPadding()
                                        .padding(end = 16.dp, bottom = 24.dp)
                                } else {
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 16.dp)
                                }
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                tonalElevation = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            activity.activeLorieView?.let { v ->
                                                v.sendMouseEvent(0f, 0f, 1, true, true)
                                                v.sendMouseEvent(0f, 0f, 1, false, true)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Left")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            activity.activeLorieView?.let { v ->
                                                v.sendMouseEvent(0f, 0f, 2, true, true)
                                                v.sendMouseEvent(0f, 0f, 2, false, true)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Middle")
                                    }
                                    Button(
                                        onClick = {
                                            activity.activeLorieView?.let { v ->
                                                v.sendMouseEvent(0f, 0f, 3, true, true)
                                                v.sendMouseEvent(0f, 0f, 3, false, true)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Right")
                                    }
                                }
                            }
                        }

                        // Emergency Force Close Dialog (Non-dismissible by 4th+ back presses)
                        if (showEmergencyForceCloseDialog) {
                            CanvasEmergencyForceCloseDialog(
                                onConfirmForceClose = {
                                    activity.showEmergencyForceCloseDialogState.value = false
                                    activity.sessionManager.stopSession()
                                    activity.finish()
                                },
                                onCancel = {
                                    activity.showEmergencyForceCloseDialogState.value = false
                                }
                            )
                        }

                        // Modern Image Source Selection Bottom Sheet
                        if (showImageSourceDialog) {
                            var tempFilesList by remember(showImageSourceDialog) {
                                val cameraDir = File(LinuxEnvironment(activity).getNotesDirectory(), ".temp/Camera")
                                val list = if (cameraDir.exists() && cameraDir.isDirectory) {
                                    cameraDir.listFiles { file ->
                                        file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
                                    }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
                                } else {
                                    emptyList()
                                }
                                mutableStateOf(list)
                            }

                            ModalBottomSheet(
                                onDismissRequest = { showImageSourceDialog = false },
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 8.dp)
                                        .navigationBarsPadding(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Insert Image",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    // Option 1: Take Photo (Camera)
                                    ListItem(
                                        headlineContent = { Text("Take Photo", fontWeight = FontWeight.Medium) },
                                        supportingContent = { Text("Capture directly using device camera") },
                                        leadingContent = {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.CameraAlt,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                showImageSourceDialog = false
                                                activity.launchCameraCapture()
                                            }
                                    )

                                    // Option 2: Choose from Photos / Gallery
                                    ListItem(
                                        headlineContent = { Text("Photo Gallery", fontWeight = FontWeight.Medium) },
                                        supportingContent = { Text("Select pictures from your photo library") },
                                        leadingContent = {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Image,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                showImageSourceDialog = false
                                                activity.launchGalleryPicker()
                                            }
                                    )

                                    // Option 3: Browse Files
                                    ListItem(
                                        headlineContent = { Text("Browse Files", fontWeight = FontWeight.Medium) },
                                        supportingContent = { Text("Pick image files from your storage or downloads") },
                                        leadingContent = {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.FolderOpen,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                showImageSourceDialog = false
                                                activity.launchFilePicker()
                                            }
                                    )

                                    if (tempFilesList.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Temporary Saves",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${tempFilesList.size}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(tempFilesList, key = { it.absolutePath }) { file ->
                                                TemporarySaveCard(
                                                    file = file,
                                                    onSelect = {
                                                        showImageSourceDialog = false
                                                        activity.processAndPasteCameraImage(file)
                                                    },
                                                    onDelete = {
                                                        try {
                                                            file.delete()
                                                        } catch (_: Exception) {}
                                                        val cameraDir = File(LinuxEnvironment(activity).getNotesDirectory(), ".temp/Camera")
                                                        tempFilesList = cameraDir.listFiles { f ->
                                                            f.isFile && f.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
                                                        }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
