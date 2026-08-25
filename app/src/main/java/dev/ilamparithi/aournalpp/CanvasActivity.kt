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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.termux.x11.LorieView
import com.termux.x11.input.LenovoPenButtonMapper
import com.termux.x11.input.TouchInputHandler
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.runtime.CanvasSessionManager
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.runtime.WallpaperHelper
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
        if (isFullscreen) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
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
                var isHeaderExpanded by remember { mutableStateOf(true) }
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

                val displayTitle = when {
                    openPreferences && (liveTitle == null || liveTitle?.removePrefix("*")?.trim() == "New Note" || liveTitle?.removePrefix("*")?.trim() == "Unsaved Document") -> "Preferences"
                    else -> liveTitle ?: initialTitle
                }

                BackHandler(enabled = true) {
                    handleSmartBackPress()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    val wallpaperBitmap = remember {
                        WallpaperHelper.resolveWallpaperBitmap(this@CanvasActivity)
                    }

                    Box(
                        modifier = if (isFullscreen) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .fillMaxSize()
                                .systemBarsPadding()
                        }
                    ) {
                        // Wallpaper Backdrop Layer
                        Image(
                            bitmap = wallpaperBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        X11Viewport(
                            modifier = Modifier.fillMaxSize(),
                            onLorieViewReady = { lorieView ->
                                activeLorieView = lorieView
                                sessionManager.startSession(lorieView, targetPath, openPreferences)
                            },
                            onInputHandlerReady = { handler ->
                                inputHandler = handler
                            },
                            onPenMapperReady = { mapper ->
                                penMapper = mapper
                            }
                        )

                        // Floating Top Header Bar with Material 3 Morphing
                        val M3MorphEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) }

                        Box(
                            modifier = if (isFullscreen) {
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .statusBarsPadding()
                                    .padding(top = 8.dp)
                            } else {
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                            }
                        ) {
                            Surface(
                                modifier = Modifier
                                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
                                    .clip(RoundedCornerShape(24.dp)),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                tonalElevation = 6.dp
                            ) {
                                AnimatedContent(
                                    targetState = isHeaderExpanded,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 60, easing = M3MorphEasing)) togetherWith
                                        fadeOut(animationSpec = tween(durationMillis = 100, easing = M3MorphEasing)) using
                                        SizeTransform(
                                            clip = true,
                                            sizeAnimationSpec = { _, _ -> tween(durationMillis = 350, easing = M3MorphEasing) }
                                        )
                                    },
                                    contentAlignment = Alignment.Center,
                                    label = "HeaderMorphTransition"
                                ) { expanded ->
                                    if (expanded) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { handleSmartBackPress() },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Exit Note",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.Description,
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

                                            // Stylus Click Override Mode Switcher (Animated Material 3 Mini Capsule)
                                            val showStylusClickOverride = remember {
                                                x11Prefs.getBoolean(dev.ilamparithi.aournalpp.data.X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, false)
                                            }
                                            var stylusClickMode by remember {
                                                mutableIntStateOf(com.termux.x11.input.TouchInputHandler.STYLUS_INPUT_HELPER_MODE)
                                            }

                                            if (showStylusClickOverride) {
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
                                                    animationSpec = tween(durationMillis = 320, easing = M3MorphEasing),
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
                                                                    animationSpec = tween(durationMillis = 200, easing = M3MorphEasing),
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
                                            val haptics = LocalHapticFeedback.current
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                                                ) {
                                                    // Cut (Ctrl+X)
                                                    IconButton(
                                                        onClick = {
                                                            try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                            injectKeyboardShortcut(KeyEvent.KEYCODE_X, "ctrl+x")
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

                                                    // Copy (Ctrl+C)
                                                    IconButton(
                                                        onClick = {
                                                            try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                            injectKeyboardShortcut(KeyEvent.KEYCODE_C, "ctrl+c")
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

                                                    // Paste (Ctrl+V)
                                                    IconButton(
                                                        onClick = {
                                                            try { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                                            injectKeyboardShortcut(KeyEvent.KEYCODE_V, "ctrl+v", isPaste = true)
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

                                            // Stateful Keyboard Toggle Action
                                            val isKeyboardOpen by remember { isKeyboardOpenState }
                                            IconButton(
                                                onClick = {
                                                    activeLorieView?.let { view ->
                                                        val insetsCtrl = WindowCompat.getInsetsController(window, window.decorView)
                                                        if (isKeyboardOpen) {
                                                            view.setKeyboardVisible(false)
                                                            insetsCtrl.hide(WindowInsetsCompat.Type.ime())
                                                        } else {
                                                            view.requestFocus()
                                                            view.setKeyboardVisible(true)
                                                            insetsCtrl.show(WindowInsetsCompat.Type.ime())
                                                        }
                                                    } ?: run {
                                                        val insetsCtrl = WindowCompat.getInsetsController(window, window.decorView)
                                                        if (isKeyboardOpen) {
                                                            insetsCtrl.hide(WindowInsetsCompat.Type.ime())
                                                        } else {
                                                            insetsCtrl.show(WindowInsetsCompat.Type.ime())
                                                        }
                                                    }
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

                                            // Collapse Pill Action
                                            IconButton(
                                                onClick = { isHeaderExpanded = false },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ExpandLess,
                                                    contentDescription = "Collapse Header",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .clickable { isHeaderExpanded = true }
                                                .padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = displayTitle,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ExpandMore,
                                                contentDescription = "Expand Header",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
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
            val dialogOpen = sessionManager.isModalOrDialogOpen()
            if (dialogOpen) {
                // If a dialog or prompt (e.g. Save confirmation, Preferences, Export) is open, notify user and keep it open
                Toast.makeText(
                    this@CanvasActivity,
                    "Please close any open dialogs or prompts in Xournal++ to exit",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // 1. Direct hardware-level X11 key injection through TouchInputHandler
                injectCtrlQDirect()

                // 2. Multi-strategy background X11 Ctrl+Q close
                sessionManager.requestCloseSession()

                // 3. If a save dialog/prompt opened in response to close request, notify the user
                kotlinx.coroutines.delay(1000)
                if (!isFinishing && supervisor.isXournalRunning() && !showEmergencyForceCloseDialogState.value) {
                    if (sessionManager.isModalOrDialogOpen()) {
                        Toast.makeText(
                            this@CanvasActivity,
                            "Please close any open dialogs or prompts in Xournal++ to exit",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }



    private fun injectKeyboardShortcut(keyCode: Int, shortcutStr: String, isPaste: Boolean = false) {
        activeLorieView?.let { view ->
            view.requestFocus()
            if (isPaste) {
                view.forceAnnounceClipboard()
            }
            view.post {
                view.sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, true)
                view.sendKeyEvent(0, keyCode, true)
                view.sendKeyEvent(0, keyCode, false)
                view.sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, false)
            }
        }

        inputHandler?.let { handler ->
            val now = android.os.SystemClock.uptimeMillis()
            val ctrlDown = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT, 0)
            val keyDown = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON)
            val keyUp = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON)
            val ctrlUp = KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT, 0)

            handler.sendKeyEvent(ctrlDown)
            handler.sendKeyEvent(keyDown)
            handler.sendKeyEvent(keyUp)
            handler.sendKeyEvent(ctrlUp)
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
        val handler = inputHandler
        if (handler != null && handler.sendKeyEvent(event)) {
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
