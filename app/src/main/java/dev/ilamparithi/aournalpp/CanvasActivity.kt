package dev.ilamparithi.aournalpp

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.termux.x11.LorieView
import com.termux.x11.input.InputEventSender
import com.termux.x11.input.LenovoPenButtonMapper
import com.termux.x11.input.TouchInputHandler
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.runtime.CanvasSessionManager
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.runtime.WallpaperHelper
import dev.ilamparithi.aournalpp.ui.theme.ExpressiveSprings
import dev.ilamparithi.aournalpp.utils.WindowTitleHelper
import kotlin.math.abs
import kotlin.math.roundToInt
import android.os.Build
import android.view.Surface
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import dev.ilamparithi.aournalpp.ui.SafeAreaInsets
import dev.ilamparithi.aournalpp.ui.getRotatedSafeAreaInsets
import dev.ilamparithi.aournalpp.ui.rememberCutoutPlacement
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme
import dev.ilamparithi.aournalpp.x11.X11Viewport
import java.io.File

class CanvasActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NOTE_PATH = "dev.ilamparithi.aournalpp.extra.NOTE_PATH"
        const val EXTRA_OPEN_PREFERENCES = "dev.ilamparithi.aournalpp.extra.OPEN_PREFERENCES"
        const val EXTRA_OPEN_PREFS_ALIAS = "EXTRA_OPEN_PREFERENCES"
    }

    private lateinit var env: LinuxEnvironment
    private lateinit var supervisor: ProcessSupervisor
    private lateinit var sessionManager: CanvasSessionManager
    private var inputHandler: TouchInputHandler? = null
    private var inputSender: InputEventSender? = null
    private var penMapper: LenovoPenButtonMapper? = null
    private var activeLorieView: LorieView? = null

    private val showEmergencyForceCloseDialogState = mutableStateOf(false)
    private val isKeyboardOpenState = mutableStateOf(false)
    private val backPressTimestamps = mutableListOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleSmartBackPress()
            }
        })

        // Initialize X11 preferences with defaults (e.g. Direct Touch)
        dev.ilamparithi.aournalpp.data.X11Preferences.initDefaults(this)
        val x11Prefs = dev.ilamparithi.aournalpp.data.X11Preferences.getPrefs(this)

        val isFullscreen = x11Prefs.getBoolean(dev.ilamparithi.aournalpp.data.X11Preferences.KEY_FULLSCREEN, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (isFullscreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        // Ensure window manager does not pan or push the activity when keyboard appears
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        // Screen idle timeout configuration
        val idleTimeoutMode = x11Prefs.getString(dev.ilamparithi.aournalpp.data.X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, "system") ?: "system"
        when (idleTimeoutMode) {
            "never" -> window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            "system" -> window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else -> window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // High-performance debounce-aware soft keyboard Reseed and state listener
        val reseedEnabled = x11Prefs.getBoolean(dev.ilamparithi.aournalpp.data.X11Preferences.KEY_RESEED, false)
        var lastImeHeight = -1
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeHeight = imeInsets.bottom
            val isImeOpen = imeHeight > 0 || insets.isVisible(WindowInsetsCompat.Type.ime())
            isKeyboardOpenState.value = isImeOpen

            if (imeHeight != lastImeHeight) {
                lastImeHeight = imeHeight
                activeLorieView?.let { view ->
                    if (reseedEnabled) {
                        view.setContentInsets(0, 0, 0, imeHeight)
                        view.setObscuredBottom(0)
                    } else {
                        view.setContentInsets(0, 0, 0, 0)
                        view.setObscuredBottom(imeHeight)
                    }
                    if (!isImeOpen) {
                        view.setKeyboardVisible(false)
                    }
                }
            }
            insets
        }

        com.termux.x11.MainActivity.setPrefs(com.termux.x11.Prefs(this))

        env = LinuxEnvironment(this)
        supervisor = ProcessSupervisor(env)
        sessionManager = CanvasSessionManager(
            context = this,
            env = env,
            supervisor = supervisor,
            scope = lifecycleScope
        )

        // Automatically finish activity when Xournal++ terminates
        sessionManager.setOnProcessExitListener {
            runOnUiThread {
                if (!isFinishing) {
                    finish()
                }
            }
        }

        val openPreferences = intent.getBooleanExtra(EXTRA_OPEN_PREFERENCES, false)
            || intent.getBooleanExtra(EXTRA_OPEN_PREFS_ALIAS, false)
            || intent.getBooleanExtra("EXTRA_OPEN_PREFS", false)

        val targetPath = intent.getStringExtra(EXTRA_NOTE_PATH)
        val prefs = getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        if (targetPath != null) {
            prefs.edit().putString("pref_last_opened_note_path", targetPath).apply()
            DocumentRepository(this).recordNoteOpened(targetPath)
        } else {
            prefs.edit().remove("pref_last_opened_note_path").apply()
        }

        val initialTitle = when {
            targetPath != null -> File(targetPath).name
            openPreferences -> "Preferences"
            else -> "New Note"
        }

        setContent {
            AournalTheme {
                val showEmergencyForceCloseDialog by remember { showEmergencyForceCloseDialogState }
                val alwaysShowFileName = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME, false)
                }
                val startCollapsed = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_START_COLLAPSED, false)
                }
                val pinButtonMode = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE, false)
                }
                val autoCollapseTimeoutMs = remember {
                    x11Prefs.getInt(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, 5000)
                }

                val liveTitle by sessionManager.documentTitle.collectAsState()

                androidx.compose.runtime.LaunchedEffect(liveTitle) {
                    val title = liveTitle?.removePrefix("*")?.removeSuffix("*")?.trim()
                    if (!title.isNullOrBlank() && title != "New Note" && title != "Unsaved Document" && title != "Preferences") {
                        withContext(Dispatchers.IO) {
                            val repo = DocumentRepository(this@CanvasActivity)
                            val root = repo.getRootNotesDirectory()
                            val file = root.walkTopDown().filter {
                                it.isFile && (it.name.equals(title, ignoreCase = true) || it.nameWithoutExtension.equals(title, ignoreCase = true))
                            }.firstOrNull()
                            if (file != null) {
                                repo.recordNoteOpened(file.absolutePath)
                            }
                        }
                    }
                }

                val baseDocumentName = remember(targetPath, initialTitle) {
                    targetPath?.let { File(it).nameWithoutExtension } ?: (initialTitle ?: "New Note")
                }
                val displayTitle = when {
                    alwaysShowFileName -> baseDocumentName
                    openPreferences && (liveTitle == null || liveTitle?.removePrefix("*")?.trim() == "New Note" || liveTitle?.removePrefix("*")?.trim() == "Unsaved Document") -> "Preferences"
                    else -> liveTitle ?: initialTitle ?: "New Note"
                }

                val windowIcon = remember(displayTitle) {
                    WindowTitleHelper.resolveWindowIcon(displayTitle)
                }

                val defaultNormX = remember {
                    x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_X_RATIO, 0.5f)
                }
                val defaultNormY = remember {
                    x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_Y_RATIO, 0.0f)
                }
                val showStylusClickOverride = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, false)
                }
                val showTitle = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, true)
                }
                val showBack = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_BACK, true)
                }
                val showKeyboard = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, true)
                }
                val showDragHandle = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, true)
                }
                val showCut = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CUT, true)
                }
                val showCopy = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_COPY, true)
                }
                val showPaste = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, true)
                }
                val stylusHoverExpands = remember {
                    x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, true)
                }

                BackHandler(enabled = true) {
                    handleSmartBackPress()
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
                        // Wallpaper Backdrop Layer (covers edge-to-edge)
                        Image(
                            bitmap = WallpaperHelper.resolveWallpaperBitmap(this@CanvasActivity).asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        val currentRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            this@CanvasActivity.display?.rotation ?: Surface.ROTATION_0
                        } else {
                            @Suppress("DEPRECATION")
                            windowManager.defaultDisplay.rotation
                        }

                        val safeCustom = remember { x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_CUSTOM_EDGES, false) }
                        val safeAll = remember { x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_MARGIN_ALL, 0) }
                        val rawLeft = remember(safeCustom, safeAll) { if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_LEFT, 0) else safeAll }
                        val rawTop = remember(safeCustom, safeAll) { if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_TOP, 0) else safeAll }
                        val rawRight = remember(safeCustom, safeAll) { if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_RIGHT, 0) else safeAll }
                        val rawBottom = remember(safeCustom, safeAll) { if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_BOTTOM, 0) else safeAll }
                        val refRotation = remember { x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_REF_ROTATION, Surface.ROTATION_0) }
                        val disableInMulti = remember { x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_DISABLE_IN_MULTIWINDOW, true) }
                        val centerTopBarWithinBounds = remember { x11Prefs.getBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, false) }

                        val isAndroidMultiWindow = isInMultiWindowMode
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

                        X11Viewport(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = effectiveInsets.left.dp + systemBarPadding.calculateStartPadding(LayoutDirection.Ltr),
                                    top = effectiveInsets.top.dp + systemBarPadding.calculateTopPadding(),
                                    end = effectiveInsets.right.dp + systemBarPadding.calculateEndPadding(LayoutDirection.Ltr),
                                    bottom = effectiveInsets.bottom.dp + systemBarPadding.calculateBottomPadding()
                                ),
                            onLorieViewReady = { lorieView ->
                                activeLorieView = lorieView
                                sessionManager.startSession(lorieView, targetPath, openPreferences)
                            },
                            onInputHandlerReady = { handler ->
                                inputHandler = handler
                            },
                            onInputSenderReady = { sender ->
                                inputSender = sender
                            },
                            onPenMapperReady = { mapper ->
                                penMapper = mapper
                            }
                        )

                        // Floating Toolbar Overlay with Isolated Recomposition Scope
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
                            startCollapsed = startCollapsed,
                            pinButtonMode = pinButtonMode,
                            autoCollapseTimeoutMs = autoCollapseTimeoutMs,
                            showStylusClickOverride = showStylusClickOverride,
                            showTitle = showTitle,
                            showBack = showBack,
                            showKeyboard = showKeyboard,
                            showDragHandle = showDragHandle,
                            showCut = showCut,
                            showCopy = showCopy,
                            showPaste = showPaste,
                            stylusHoverExpands = stylusHoverExpands,
                            onSmartBackPress = { handleSmartBackPress() },
                            onToggleKeyboard = {
                                activeLorieView?.let { view ->
                                    val insetsCtrl = WindowCompat.getInsetsController(window, window.decorView)
                                    if (isKeyboardOpenState.value) {
                                        view.setKeyboardVisible(false)
                                        insetsCtrl.hide(WindowInsetsCompat.Type.ime())
                                    } else {
                                        view.requestFocus()
                                        view.setKeyboardVisible(true)
                                        insetsCtrl.show(WindowInsetsCompat.Type.ime())
                                    }
                                } ?: run {
                                    val insetsCtrl = WindowCompat.getInsetsController(window, window.decorView)
                                    if (isKeyboardOpenState.value) {
                                        insetsCtrl.hide(WindowInsetsCompat.Type.ime())
                                    } else {
                                        insetsCtrl.show(WindowInsetsCompat.Type.ime())
                                    }
                                }
                            },
                            onInjectShortcut = { keyCode, shortcutStr ->
                                injectKeyboardShortcut(keyCode, shortcutStr)
                            },
                            isKeyboardOpen = isKeyboardOpenState.value
                        )

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
                                            activeLorieView?.let { v ->
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
                                            activeLorieView?.let { v ->
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
                                            activeLorieView?.let { v ->
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
                            AlertDialog(
                                onDismissRequest = { /* Non-dismissible on tap outside or back press */ },
                                properties = DialogProperties(
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false
                                ),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                title = {
                                    Text(
                                        text = "Force Close Session?",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Xournal++ is not responding to normal close requests. Force terminating will immediately exit the canvas.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        onClick = {
                                            showEmergencyForceCloseDialogState.value = false
                                            sessionManager.stopSession()
                                            finish()
                                        }
                                    ) {
                                        Text("Force Close")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            showEmergencyForceCloseDialogState.value = false
                                        }
                                    ) {
                                        Text("Wait / Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleSmartBackPress() {
        // If force close dialog is already active, ignore back press to avoid accidental dismissal
        if (showEmergencyForceCloseDialogState.value) {
            return
        }

        val now = System.currentTimeMillis()
        backPressTimestamps.add(now)
        backPressTimestamps.removeAll { now - it > 2000 }

        val prefs = getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val tripleBackEnabled = prefs.getBoolean("pref_triple_back_force_close", true)

        if (tripleBackEnabled && backPressTimestamps.size >= 3) {
            backPressTimestamps.clear()
            showEmergencyForceCloseDialogState.value = true
            return
        }

        lifecycleScope.launch {
            // 1. Direct hardware-level X11 key injection
            injectCtrlQDirect()

            // 2. Multi-strategy background X11 Ctrl+Q close
            sessionManager.requestCloseSession()

            // 3. If Xournal++ remains open (e.g. Save prompt or modal dialog waiting for user interaction)
            kotlinx.coroutines.delay(1000)
            if (!isFinishing && supervisor.isXournalRunning() && !showEmergencyForceCloseDialogState.value) {
                Toast.makeText(
                    this@CanvasActivity,
                    "Please close any open dialogs or prompts in Xournal++ to exit",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }



    private fun injectKeyboardShortcut(keyCode: Int, shortcutStr: String) {
        activeLorieView?.let { view ->
            view.requestFocus()
            view.post {
                view.sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, true)
                view.sendKeyEvent(0, keyCode, true)
                view.sendKeyEvent(0, keyCode, false)
                view.sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, false)
            }
        }

        inputSender?.let { sender ->
            val now = android.os.SystemClock.uptimeMillis()
            val ctrlDown = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT, 0)
            val keyDown = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON)
            val keyUp = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON)
            val ctrlUp = KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT, 0)

            sender.sendKeyEvent(ctrlDown)
            sender.sendKeyEvent(keyDown)
            sender.sendKeyEvent(keyUp)
            sender.sendKeyEvent(ctrlUp)
        }

        // xdotool window-activated shortcut injection (matching edit settings & back button quit)
        sessionManager.injectShortcut(shortcutStr)
    }

    private fun injectCtrlQDirect() {
        injectKeyboardShortcut(KeyEvent.KEYCODE_Q, "ctrl+q")
    }



    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            return super.dispatchKeyEvent(event)
        }
        val mapper = penMapper
        if (mapper != null && mapper.onKeyEvent(event)) {
            return true
        }
        val sender = inputSender
        if (sender != null && sender.sendKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            sessionManager.stopSession()
            // Terminate isolated :canvas process cleanly so reopening starts fresh
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}

@Composable
private fun FloatingToolbarOverlay(
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    defaultNormX: Float,
    defaultNormY: Float,
    centerTopBarWithinBounds: Boolean,
    effectiveInsets: SafeAreaInsets,
    systemBarPadding: PaddingValues,
    isFullscreen: Boolean,
    displayTitle: String,
    windowIcon: ImageVector,
    startCollapsed: Boolean,
    pinButtonMode: Boolean,
    autoCollapseTimeoutMs: Int,
    showStylusClickOverride: Boolean,
    showTitle: Boolean,
    showBack: Boolean,
    showKeyboard: Boolean,
    showDragHandle: Boolean,
    showCut: Boolean,
    showCopy: Boolean,
    showPaste: Boolean,
    stylusHoverExpands: Boolean,
    onSmartBackPress: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onInjectShortcut: (Int, String) -> Unit,
    isKeyboardOpen: Boolean
) {
    val M3MorphEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) }
    val cutoutPlacement = rememberCutoutPlacement()
    val density = LocalDensity.current
    val cutoutTopOffsetDp = with(density) { cutoutPlacement.topOffsetPx.toDp() }

    var isHeaderExpanded by rememberSaveable { mutableStateOf(!startCollapsed) }
    var isPinned by rememberSaveable { mutableStateOf(false) }

    val interactionSignal = remember {
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    LaunchedEffect(isHeaderExpanded, isPinned, pinButtonMode, autoCollapseTimeoutMs) {
        if (pinButtonMode && isHeaderExpanded && !isPinned) {
            while (isActive) {
                val triggered = withTimeoutOrNull(autoCollapseTimeoutMs.toLong()) {
                    interactionSignal.first()
                }
                if (triggered == null) {
                    if (isHeaderExpanded && !isPinned) {
                        isHeaderExpanded = false
                    }
                    break
                }
            }
        }
    }

    var dragNormX by remember { mutableFloatStateOf(defaultNormX) }
    var dragNormY by remember { mutableFloatStateOf(defaultNormY) }
    val animPixelOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isMovedFromDefault by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var toolbarSizePx by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .offset {
                val totalWidthPx = canvasWidthPx
                val totalHeightPx = canvasHeightPx
                val tWidthPx = if (toolbarSizePx.width > 0) toolbarSizePx.width.toFloat() else 320.dp.toPx()
                val tHeightPx = if (toolbarSizePx.height > 0) toolbarSizePx.height.toFloat() else 48.dp.toPx()

                val minX: Float
                val maxX: Float
                val minY: Float
                val maxY: Float

                if (centerTopBarWithinBounds) {
                    minX = effectiveInsets.left.dp.toPx() + systemBarPadding.calculateStartPadding(LayoutDirection.Ltr).toPx() + 8.dp.toPx()
                    maxX = maxOf(minX, totalWidthPx - tWidthPx - effectiveInsets.right.dp.toPx() - systemBarPadding.calculateEndPadding(LayoutDirection.Ltr).toPx() - 8.dp.toPx())
                    minY = effectiveInsets.top.dp.toPx() + systemBarPadding.calculateTopPadding().toPx() + 8.dp.toPx()
                    maxY = maxOf(minY, totalHeightPx - tHeightPx - effectiveInsets.bottom.dp.toPx() - systemBarPadding.calculateBottomPadding().toPx() - 8.dp.toPx())
                } else {
                    minX = 8.dp.toPx()
                    maxX = maxOf(minX, totalWidthPx - tWidthPx - 8.dp.toPx())
                    minY = if (isFullscreen) {
                        if (cutoutPlacement.hasCenterCutout) cutoutTopOffsetDp.toPx() + 8.dp.toPx() else 8.dp.toPx()
                    } else {
                        systemBarPadding.calculateTopPadding().toPx() + 8.dp.toPx()
                    }
                    maxY = maxOf(minY, totalHeightPx - tHeightPx - 8.dp.toPx())
                }

                val basePosX = minX + (maxX - minX) * dragNormX
                val basePosY = minY + (maxY - minY) * dragNormY
                // Allow spring overshoot past bounds during reset animation without clamping
                val posX = (basePosX + animPixelOffset.value.x).roundToInt()
                val posY = (basePosY + animPixelOffset.value.y).roundToInt()
                IntOffset(posX, posY)
            }
            .onSizeChanged { size ->
                if (toolbarSizePx != size) {
                    toolbarSizePx = size
                }
            }
    ) {
        Surface(
            modifier = Modifier
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .pointerInput(stylusHoverExpands) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val isStylus = event.changes.any {
                                it.type == PointerType.Stylus || it.type == PointerType.Eraser
                            }
                            if (isStylus && (event.type == PointerEventType.Move || event.type == PointerEventType.Enter)) {
                                interactionSignal.tryEmit(Unit)
                                if (stylusHoverExpands && !isHeaderExpanded) {
                                    isHeaderExpanded = true
                                }
                            }
                        }
                    }
                },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 6.dp
        ) {
            AnimatedContent(
                targetState = isHeaderExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 180, easing = M3MorphEasing)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 120, easing = M3MorphEasing)) using
                    SizeTransform(
                        clip = true,
                        sizeAnimationSpec = { _, _ -> tween(durationMillis = 300, easing = M3MorphEasing) }
                    )
                },
                contentAlignment = Alignment.Center,
                label = "HeaderMorphTransition"
            ) { expanded ->
                if (expanded) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { interactionSignal.tryEmit(Unit) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Navigation & Document Info
                        if (showBack) {
                            IconButton(
                                onClick = {
                                    interactionSignal.tryEmit(Unit)
                                    onSmartBackPress()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Exit Note",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (showTitle) {
                            Icon(
                                imageVector = windowIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(2.dp))

                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }

                        // Stylus Click Override Mode Switcher
                        if (showStylusClickOverride) {
                            var stylusClickMode by remember {
                                mutableIntStateOf(com.termux.x11.input.TouchInputHandler.STYLUS_INPUT_HELPER_MODE)
                            }
                            val modes = listOf(
                                1 to "L",
                                2 to "M",
                                4 to "R"
                            )
                            val selectedIndex = when (stylusClickMode) {
                                2 -> 1
                                4 -> 2
                                else -> 0
                            }

                            val itemWidth = 26.dp
                            val itemHeight = 24.dp
                            val spacing = 2.dp
                            val padding = 2.dp

                            val indicatorOffset by animateDpAsState(
                                targetValue = (itemWidth + spacing) * selectedIndex,
                                animationSpec = tween(durationMillis = 240, easing = M3MorphEasing),
                                label = "StylusIndicatorOffset"
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(padding)
                                ) {
                                    // Animated Active Indicator Pill
                                    Surface(
                                        modifier = Modifier
                                            .offset(x = indicatorOffset)
                                            .size(itemWidth, itemHeight),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        shadowElevation = 1.dp
                                    ) {}

                                    // Interactive Mode Buttons
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(spacing),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        modes.forEach { (modeValue, label) ->
                                            val isSelected = stylusClickMode == modeValue
                                            val textColor by animateColorAsState(
                                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                animationSpec = tween(durationMillis = 240, easing = M3MorphEasing),
                                                label = "StylusTextColor"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .size(itemWidth, itemHeight)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        interactionSignal.tryEmit(Unit)
                                                        com.termux.x11.input.TouchInputHandler.STYLUS_INPUT_HELPER_MODE = modeValue
                                                        stylusClickMode = modeValue
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = textColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Shared Clipboard Actions Capsule (Cut Ctrl+X, Copy Ctrl+C, Paste Ctrl+V)
                        if (showCut || showCopy || showPaste) {
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
                                        IconButton(
                                            onClick = {
                                                interactionSignal.tryEmit(Unit)
                                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                onInjectShortcut(KeyEvent.KEYCODE_X, "ctrl+x")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCut,
                                                contentDescription = "Cut (Ctrl+X)",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (showCopy) {
                                        IconButton(
                                            onClick = {
                                                interactionSignal.tryEmit(Unit)
                                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                onInjectShortcut(KeyEvent.KEYCODE_C, "ctrl+c")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy (Ctrl+C)",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (showPaste) {
                                        IconButton(
                                            onClick = {
                                                interactionSignal.tryEmit(Unit)
                                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                onInjectShortcut(KeyEvent.KEYCODE_V, "ctrl+v")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste (Ctrl+V)",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Stateful Keyboard Toggle Action
                        if (showKeyboard) {
                            IconButton(
                                onClick = {
                                    interactionSignal.tryEmit(Unit)
                                    onToggleKeyboard()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "Toggle Keyboard",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isKeyboardOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Position Reset Action (shown when dragged away from configured default)
                        if (isMovedFromDefault) {
                            IconButton(
                                onClick = {
                                    interactionSignal.tryEmit(Unit)
                                    try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    val (startDeltaX, startDeltaY) = with(density) {
                                        val totalW = canvasWidthPx
                                        val totalH = canvasHeightPx
                                        val tW = if (toolbarSizePx.width > 0) toolbarSizePx.width.toFloat() else 320.dp.toPx()
                                        val tH = if (toolbarSizePx.height > 0) toolbarSizePx.height.toFloat() else 48.dp.toPx()

                                        val minX: Float
                                        val maxX: Float
                                        val minY: Float
                                        val maxY: Float

                                        if (centerTopBarWithinBounds) {
                                            minX = effectiveInsets.left.dp.toPx() + systemBarPadding.calculateStartPadding(LayoutDirection.Ltr).toPx() + 8.dp.toPx()
                                            maxX = maxOf(minX, totalW - tW - effectiveInsets.right.dp.toPx() - systemBarPadding.calculateEndPadding(LayoutDirection.Ltr).toPx() - 8.dp.toPx())
                                            minY = effectiveInsets.top.dp.toPx() + systemBarPadding.calculateTopPadding().toPx() + 8.dp.toPx()
                                            maxY = maxOf(minY, totalH - tH - effectiveInsets.bottom.dp.toPx() - systemBarPadding.calculateBottomPadding().toPx() - 8.dp.toPx())
                                        } else {
                                            minX = 8.dp.toPx()
                                            maxX = maxOf(minX, totalW - tW - 8.dp.toPx())
                                            minY = if (isFullscreen) {
                                                if (cutoutPlacement.hasCenterCutout) cutoutTopOffsetDp.toPx() + 8.dp.toPx() else 8.dp.toPx()
                                            } else {
                                                systemBarPadding.calculateTopPadding().toPx() + 8.dp.toPx()
                                            }
                                            maxY = maxOf(minY, totalH - tH - 8.dp.toPx())
                                        }

                                        val currentBaseX = minX + (maxX - minX) * dragNormX
                                        val currentBaseY = minY + (maxY - minY) * dragNormY
                                        val targetBaseX = minX + (maxX - minX) * defaultNormX
                                        val targetBaseY = minY + (maxY - minY) * defaultNormY

                                        (currentBaseX - targetBaseX) to (currentBaseY - targetBaseY)
                                    }

                                    dragNormX = defaultNormX
                                    dragNormY = defaultNormY
                                    isMovedFromDefault = false

                                    coroutineScope.launch {
                                        animPixelOffset.snapTo(Offset(startDeltaX, startDeltaY))
                                        animPixelOffset.animateTo(
                                            targetValue = Offset.Zero,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset Toolbar Position",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Pin/Unpin Mode or Collapse Action
                        if (pinButtonMode) {
                            IconButton(
                                onClick = {
                                    interactionSignal.tryEmit(Unit)
                                    isPinned = !isPinned
                                    try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (isPinned) "Unpin Toolbar" else "Pin Toolbar",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    interactionSignal.tryEmit(Unit)
                                    isHeaderExpanded = false
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandLess,
                                    contentDescription = "Collapse Toolbar",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Movable Drag Handle
                        if (showDragHandle) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                coroutineScope.launch { animPixelOffset.snapTo(Offset.Zero) }
                                                interactionSignal.tryEmit(Unit)
                                                try { haptics.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                interactionSignal.tryEmit(Unit)
                                                val totalW = canvasWidthPx
                                                val totalH = canvasHeightPx
                                                val tW = if (toolbarSizePx.width > 0) toolbarSizePx.width.toFloat() else 320f
                                                val tH = if (toolbarSizePx.height > 0) toolbarSizePx.height.toFloat() else 48f
                                                val spanX = maxOf(1f, totalW - tW)
                                                val spanY = maxOf(1f, totalH - tH)
                                                dragNormX = (dragNormX + dragAmount.x / spanX).coerceIn(0f, 1f)
                                                dragNormY = (dragNormY + dragAmount.y / spanY).coerceIn(0f, 1f)
                                                isMovedFromDefault = abs(dragNormX - defaultNormX) > 0.03f || abs(dragNormY - defaultNormY) > 0.03f
                                            },
                                            onDragEnd = {
                                                interactionSignal.tryEmit(Unit)
                                                try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                isMovedFromDefault = abs(dragNormX - defaultNormX) > 0.03f || abs(dragNormY - defaultNormY) > 0.03f
                                            },
                                            onDragCancel = {}
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = "Drag to Move Toolbar",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Collapsed Pill View: Entire pill is draggable on long-press & expandable on single-tap (no handle icon)
                    Row(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        coroutineScope.launch { animPixelOffset.snapTo(Offset.Zero) }
                                        interactionSignal.tryEmit(Unit)
                                        try { haptics.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        interactionSignal.tryEmit(Unit)
                                        val totalW = canvasWidthPx
                                        val totalH = canvasHeightPx
                                        val tW = if (toolbarSizePx.width > 0) toolbarSizePx.width.toFloat() else 140f
                                        val tH = if (toolbarSizePx.height > 0) toolbarSizePx.height.toFloat() else 36f
                                        val spanX = maxOf(1f, totalW - tW)
                                        val spanY = maxOf(1f, totalH - tH)
                                        dragNormX = (dragNormX + dragAmount.x / spanX).coerceIn(0f, 1f)
                                        dragNormY = (dragNormY + dragAmount.y / spanY).coerceIn(0f, 1f)
                                        isMovedFromDefault = abs(dragNormX - defaultNormX) > 0.03f || abs(dragNormY - defaultNormY) > 0.03f
                                    },
                                    onDragEnd = {
                                        interactionSignal.tryEmit(Unit)
                                        try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                        isMovedFromDefault = abs(dragNormX - defaultNormX) > 0.03f || abs(dragNormY - defaultNormY) > 0.03f
                                    },
                                    onDragCancel = {}
                                )
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                interactionSignal.tryEmit(Unit)
                                isHeaderExpanded = true
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showTitle) {
                            Icon(
                                imageVector = windowIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand Toolbar",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
