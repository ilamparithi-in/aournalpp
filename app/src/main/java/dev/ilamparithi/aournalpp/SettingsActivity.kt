package dev.ilamparithi.aournalpp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CancellationException
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
import dev.ilamparithi.aournalpp.ui.util.a11yHeading
import dev.ilamparithi.aournalpp.ui.util.minTouchTarget
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import dev.ilamparithi.aournalpp.utils.FileNameTemplateEngine
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.app.Activity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.res.stringResource
import dev.ilamparithi.aournalpp.runtime.LinuxLocaleManager
import dev.ilamparithi.aournalpp.runtime.LinuxLocaleInfo
import dev.ilamparithi.aournalpp.util.AppLocaleHelper
import dev.ilamparithi.aournalpp.util.AppLanguageInfo
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.runtime.ConfigFileType
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager
import dev.ilamparithi.aournalpp.runtime.WallpaperHelper
import dev.ilamparithi.aournalpp.runtime.XournalConfigManager
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.ui.ConfigViewerDialog
import dev.ilamparithi.aournalpp.ui.ScreenSafeAreaEditorScreen
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme
import dev.ilamparithi.aournalpp.utils.NoteOpenAction
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

import dev.ilamparithi.aournalpp.ui.ToolbarPositionEditorScreen
import dev.ilamparithi.aournalpp.ui.STANDARD_TOOLBAR_PRESETS

enum class SettingsSubpage {
    MAIN,
    TOOLBAR,
    TOOLBAR_POSITION_EDITOR,
    KEYBOARD,
    INPUT,
    LENOVO_PEN,
    DISPLAY,
    SAFE_AREA_EDITOR
}

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        X11Preferences.initDefaults(this)

        setContent {
            AournalTheme {
                SettingsNavigationHost(onFinish = { finish() })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val env = LinuxEnvironment(this)
        NotesHomeConfigManager.sync(this, env)
    }
}

@Composable
fun SettingsSwitchListItem(
    headline: String,
    supporting: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        leadingContent = leadingContent,
        headlineContent = {
            Text(headline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = supporting?.let {
            { Text(it, style = MaterialTheme.typography.bodySmall) }
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null
            )
        },
        modifier = modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

fun getParentSettingsSubpage(page: SettingsSubpage): SettingsSubpage = when (page) {
    SettingsSubpage.TOOLBAR_POSITION_EDITOR -> SettingsSubpage.TOOLBAR
    SettingsSubpage.TOOLBAR -> SettingsSubpage.MAIN
    SettingsSubpage.KEYBOARD -> SettingsSubpage.MAIN
    SettingsSubpage.INPUT -> SettingsSubpage.MAIN
    SettingsSubpage.LENOVO_PEN -> SettingsSubpage.INPUT
    SettingsSubpage.DISPLAY -> SettingsSubpage.MAIN
    SettingsSubpage.SAFE_AREA_EDITOR -> SettingsSubpage.DISPLAY
    SettingsSubpage.MAIN -> SettingsSubpage.MAIN
}

@Composable
fun SettingsScreen(onBack: (() -> Unit)? = null) {
    SettingsNavigationHost(onFinish = { onBack?.invoke() })
}

@Composable
fun SettingsNavigationHost(onFinish: () -> Unit) {
    var currentSubpage by rememberSaveable { mutableStateOf(SettingsSubpage.MAIN) }
    val context = LocalContext.current
    val aournalPrefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    val reduceAnimations = remember { aournalPrefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false) }
    val parentSubpage = getParentSettingsSubpage(currentSubpage)

    @Composable
    fun RenderSettingsPageContent(
        targetPage: SettingsSubpage,
        onNavigate: (SettingsSubpage) -> Unit,
        onBackAction: () -> Unit,
        onSetSubpage: (SettingsSubpage) -> Unit
    ) {
        when (targetPage) {
            SettingsSubpage.MAIN -> MainSettingsScreen(
                onNavigate = onNavigate,
                onBack = onBackAction
            )
            SettingsSubpage.TOOLBAR -> ToolbarSettingsScreen(
                onNavigateToPositionEditor = { onSetSubpage(SettingsSubpage.TOOLBAR_POSITION_EDITOR) },
                onBack = { onSetSubpage(SettingsSubpage.MAIN) }
            )
            SettingsSubpage.TOOLBAR_POSITION_EDITOR -> {
                Dialog(
                    onDismissRequest = { onSetSubpage(SettingsSubpage.TOOLBAR) },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    ToolbarPositionEditorScreen(
                        onNavigateBack = { onSetSubpage(SettingsSubpage.TOOLBAR) }
                    )
                }
            }
            SettingsSubpage.KEYBOARD -> KeyboardSettingsScreen(
                onBack = { onSetSubpage(SettingsSubpage.MAIN) }
            )
            SettingsSubpage.INPUT -> InputSettingsScreen(
                onNavigateToLenovoPen = { onSetSubpage(SettingsSubpage.LENOVO_PEN) },
                onNavigateToToolbar = { onSetSubpage(SettingsSubpage.TOOLBAR) },
                onBack = { onSetSubpage(SettingsSubpage.MAIN) }
            )
            SettingsSubpage.LENOVO_PEN -> LenovoPenSettingsScreen(
                onBack = { onSetSubpage(SettingsSubpage.INPUT) }
            )
            SettingsSubpage.DISPLAY -> DisplaySettingsScreen(
                onNavigateToSafeAreaEditor = { onSetSubpage(SettingsSubpage.SAFE_AREA_EDITOR) },
                onBack = { onSetSubpage(SettingsSubpage.MAIN) }
            )
            SettingsSubpage.SAFE_AREA_EDITOR -> {
                Dialog(
                    onDismissRequest = { onSetSubpage(SettingsSubpage.DISPLAY) },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    ScreenSafeAreaEditorScreen(
                        onNavigateBack = { onSetSubpage(SettingsSubpage.DISPLAY) }
                    )
                }
            }
        }
    }

    dev.ilamparithi.aournalpp.ui.predictive.PredictiveBackLayout(
        enabled = currentSubpage != SettingsSubpage.MAIN,
        onBack = { currentSubpage = parentSubpage },
        reduceAnimations = reduceAnimations,
        backgroundContent = if (currentSubpage != SettingsSubpage.MAIN) {
            {
                RenderSettingsPageContent(
                    targetPage = parentSubpage,
                    onNavigate = { currentSubpage = it },
                    onBackAction = onFinish,
                    onSetSubpage = { currentSubpage = it }
                )
            }
        } else null
    ) { _, _ ->
        RenderSettingsPageContent(
            targetPage = currentSubpage,
            onNavigate = { currentSubpage = it },
            onBackAction = onFinish,
            onSetSubpage = { currentSubpage = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    onNavigate: (SettingsSubpage) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    val env = remember { LinuxEnvironment(context) }
    val configManager = remember { XournalConfigManager(env) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (env.checkAndOverrideAutoloadPreference() || env.hasPendingAutoloadOverrideNotification()) {
                    env.clearPendingAutoloadOverrideNotification()
                    scope.launch {
                        snackbarHostState.showSnackbar("Xournal++ startup autoload was cleared to preserve 'Continue where you left off'.")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.a11yHeading()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minTouchTarget()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Storage & Notes Directory (Direct Setting at Top)
            var currentNotesDir by remember { mutableStateOf(env.getNotesDirectory().absolutePath) }
            var showCustomPathDialog by remember { mutableStateOf(false) }
            var customPathInput by remember { mutableStateOf(currentNotesDir) }
            var pendingPresetChange by remember { mutableStateOf<Pair<String, String>?>(null) }

            val folderPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    val rawPath = uri.path ?: ""
                    val resolved = if (rawPath.contains("primary:")) {
                        val rel = rawPath.substringAfter("primary:").trim('/')
                        File(android.os.Environment.getExternalStorageDirectory(), rel).absolutePath
                    } else {
                        rawPath
                    }
                    env.setNotesDirectory(resolved)
                    currentNotesDir = resolved
                    Toast.makeText(context, "Notes folder set to: $resolved", Toast.LENGTH_SHORT).show()
                }
            }

            if (showCustomPathDialog) {
                AlertDialog(
                    onDismissRequest = { showCustomPathDialog = false },
                    properties = AppDialogDefaults.Properties,
                    modifier = Modifier.promptWidth(),
                    title = {
                        Text(
                            text = "Set Custom Notes Folder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Enter an absolute path on device storage where Xournal++ notes should save:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = customPathInput,
                                onValueChange = { customPathInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Directory Path") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (customPathInput.isNotBlank()) {
                                    val trimmed = customPathInput.trim()
                                    env.setNotesDirectory(trimmed)
                                    currentNotesDir = trimmed
                                    showCustomPathDialog = false
                                    Toast.makeText(context, "Notes folder set to: $trimmed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomPathDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            pendingPresetChange?.let { (label, path) ->
                AlertDialog(
                    onDismissRequest = { pendingPresetChange = null },
                    properties = AppDialogDefaults.Properties,
                    modifier = Modifier.promptWidth(),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = {
                        Text(
                            text = "Switch Notes Directory?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Do you want to switch your active notes storage location to:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = path,
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Xournal++ will save and organize new notes in this folder. Any notes in your previous folder will remain untouched.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                env.setNotesDirectory(path)
                                currentNotesDir = path
                                pendingPresetChange = null
                                Toast.makeText(context, "Notes folder set to: $path", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Switch Directory")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingPresetChange = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Text(
                text = "Notes & Storage Location",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Current Notes Directory:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = currentNotesDir,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Browse & Choose Folder...",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Preset storage locations:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val presets = listOf(
                        "Documents/Notes" to File(android.os.Environment.getExternalStorageDirectory(), "Documents/Notes").absolutePath,
                        "Documents/Xournal" to File(android.os.Environment.getExternalStorageDirectory(), "Documents/Xournal").absolutePath,
                        "Download" to File(android.os.Environment.getExternalStorageDirectory(), "Download").absolutePath
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (label, path) ->
                            val isActive = currentNotesDir == path
                            OutlinedButton(
                                onClick = {
                                    if (!isActive) {
                                        pendingPresetChange = label to path
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = label.substringAfter('/'),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            customPathInput = currentNotesDir
                            showCustomPathDialog = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Enter Custom Path Manually")
                    }

                    HorizontalDivider()

                    var showHiddenFilesPref by remember {
                        mutableStateOf(prefs.getBoolean("pref_show_hidden_files", false))
                    }

                    SettingsSwitchListItem(
                        headline = "Show Hidden & Backup Files",
                        supporting = "Display hidden files and backup copies in the Document Hub.",
                        checked = showHiddenFilesPref,
                        onCheckedChange = {
                            showHiddenFilesPref = it
                            prefs.edit().putBoolean("pref_show_hidden_files", it).apply()
                        }
                    )

                    var intelligentRecoveryPref by remember {
                        mutableStateOf(prefs.getBoolean("pref_intelligent_emergency_recovery", true))
                    }

                    SettingsSwitchListItem(
                        headline = "Intelligent Session Recovery",
                        supporting = "Detect crashed/unsaved note sessions and offer restoration.",
                        checked = intelligentRecoveryPref,
                        onCheckedChange = {
                            intelligentRecoveryPref = it
                            prefs.edit().putBoolean("pref_intelligent_emergency_recovery", it).apply()
                        }
                    )
                }
            }

            // 2. Default Note Action (View / Edit / Ask every time)
            var defaultActionPref by remember {
                mutableStateOf(
                    prefs.getString(NoteOpenManager.PREF_KEY_DEFAULT_OPEN_ACTION, NoteOpenAction.ASK.value)
                        ?: NoteOpenAction.ASK.value
                )
            }

            Text(
                text = "Default Note Action",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Action when opening any note or file:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val defaultActionOptions = listOf(
                        NoteOpenAction.ASK to "Ask every time",
                        NoteOpenAction.EDIT to "Edit (Canvas)",
                        NoteOpenAction.VIEW to "View (PDF)"
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        defaultActionOptions.forEachIndexed { index, (action, label) ->
                            SegmentedButton(
                                selected = defaultActionPref == action.value,
                                onClick = {
                                    defaultActionPref = action.value
                                    prefs.edit().putString(NoteOpenManager.PREF_KEY_DEFAULT_OPEN_ACTION, action.value).apply()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, defaultActionOptions.size),
                                label = { Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                            )
                        }
                    }

                    Text(
                        text = when (defaultActionPref) {
                            NoteOpenAction.EDIT.value -> "Notes will immediately open in the Xournal++ canvas editor for fast note-taking."
                            NoteOpenAction.VIEW.value -> "Notes will be converted and opened in your external/system PDF viewer."
                            else -> "A prompt will ask whether to View as PDF or Edit in Xournal++ every time you open a note."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2.5 File Name Templates (Comprehensive Customization with Live Preview & Guide)
            Text(
                text = "File Name Templates",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            FileNameTemplateSettingsCard(prefs = prefs, context = context, snackbarHostState = snackbarHostState)

            // 3. Appearance & Canvas Backdrop (Compact Material 3 Switchers)
            var appThemePref by remember {
                mutableStateOf(prefs.getString(LinuxEnvironment.PREF_KEY_APP_THEME, "system") ?: "system")
            }
            var gtkThemePref by remember {
                mutableStateOf(prefs.getString(LinuxEnvironment.PREF_KEY_GTK_THEME, "system") ?: "system")
            }

            Text(
                text = "Appearance & Theme",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Android App UI Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val appThemeOptions = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        appThemeOptions.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = appThemePref == value,
                                onClick = {
                                    appThemePref = value
                                    prefs.edit().putString(LinuxEnvironment.PREF_KEY_APP_THEME, value).apply()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, appThemeOptions.size),
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Xournal++ GTK Canvas Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val gtkThemeOptions = listOf("system" to "System", "light" to "Adwaita Light", "dark" to "Adwaita Dark")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        gtkThemeOptions.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = gtkThemePref == value,
                                onClick = {
                                    gtkThemePref = value
                                    prefs.edit().putString(LinuxEnvironment.PREF_KEY_GTK_THEME, value).apply()
                                    env.writeGtkSettings()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, gtkThemeOptions.size),
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    HorizontalDivider()

                    var wallpaperModePref by remember {
                        mutableStateOf(WallpaperHelper.getWallpaperMode(context))
                    }
                    var customWallpaperVersion by remember { mutableIntStateOf(0) }

                    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            val res = WallpaperHelper.saveCustomWallpaper(context, uri)
                            if (res.isSuccess) {
                                wallpaperModePref = WallpaperHelper.MODE_CUSTOM
                                customWallpaperVersion++
                                scope.launch {
                                    snackbarHostState.showSnackbar("Custom wallpaper applied for Canvas")
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to set wallpaper: ${res.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    }

                    Text(
                        text = "Canvas Backdrop & Wallpaper",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val wallpaperOptions = listOf(
                        WallpaperHelper.MODE_SYSTEM to "System",
                        WallpaperHelper.MODE_CUSTOM to "Custom",
                        WallpaperHelper.MODE_THEME to "Theme"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        wallpaperOptions.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = wallpaperModePref == value,
                                onClick = {
                                    if (value == WallpaperHelper.MODE_CUSTOM && !WallpaperHelper.getCustomWallpaperFile(context).exists()) {
                                        wallpaperPickerLauncher.launch("image/*")
                                    } else {
                                        wallpaperModePref = value
                                        WallpaperHelper.setWallpaperMode(context, value)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, wallpaperOptions.size),
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    if (wallpaperModePref == WallpaperHelper.MODE_CUSTOM) {
                        val customFile = remember(customWallpaperVersion) { WallpaperHelper.getCustomWallpaperFile(context) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { wallpaperPickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (customFile.exists()) "Change Image..." else "Select Image...",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            if (customFile.exists()) {
                                TextButton(
                                    onClick = {
                                        WallpaperHelper.clearCustomWallpaper(context)
                                        wallpaperModePref = WallpaperHelper.MODE_SYSTEM
                                        customWallpaperVersion++
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Reset to System Wallpaper")
                                        }
                                    }
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    var reduceAnimationsPref by remember {
                        mutableStateOf(prefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reduce Animations",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Disable expressive motion effects and sunburst spinners for improved performance on lower-end devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = reduceAnimationsPref,
                            onCheckedChange = {
                                reduceAnimationsPref = it
                                prefs.edit().putBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, it).apply()
                            }
                        )
                    }
                }
            }

            // 3.5 Language & Localization (App UI & Linux Environment)
            var showAppLanguageDialog by remember { mutableStateOf(false) }
            var showLinuxLanguageDialog by remember { mutableStateOf(false) }
            var currentLinuxLocaleTag by remember { mutableStateOf(LinuxLocaleManager.getSavedLocale(context)) }
            var appLanguageDisplayName by remember { mutableStateOf(AppLocaleHelper.getCurrentAppLanguageDisplayName()) }

            androidx.compose.runtime.DisposableEffect(Unit) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        currentLinuxLocaleTag = LinuxLocaleManager.getSavedLocale(context)
                        appLanguageDisplayName = AppLocaleHelper.getCurrentAppLanguageDisplayName()
                    }
                }
                val lifecycle = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycle
                lifecycle?.addObserver(observer)
                onDispose {
                    lifecycle?.removeObserver(observer)
                }
            }

            if (showAppLanguageDialog) {
                AppLanguagePickerDialog(
                    onDismissRequest = { showAppLanguageDialog = false },
                    onLanguageSelected = { tag ->
                        AppLocaleHelper.setAppLanguage(tag)
                        appLanguageDisplayName = AppLocaleHelper.getCurrentAppLanguageDisplayName()
                        showAppLanguageDialog = false
                    }
                )
            }

            if (showLinuxLanguageDialog) {
                XournalppLanguagePickerDialog(
                    context = context,
                    onDismissRequest = { showLinuxLanguageDialog = false },
                    onLanguageSelected = { info ->
                        LinuxLocaleManager.setSavedLocale(context, info.tag)
                        currentLinuxLocaleTag = info.tag
                        env.ensureXournalppSettings()
                        showLinuxLanguageDialog = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_linux_language_updated, info.displayName),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }

            Text(
                text = stringResource(R.string.pref_cat_language),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // App Language
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                (context as? Activity)?.let { act ->
                                    AppLocaleHelper.openAppLanguageSettings(act) {
                                        showAppLanguageDialog = true
                                    }
                                } ?: run { showAppLanguageDialog = true }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_app_language_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = appLanguageDisplayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.pref_app_language_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider()

                    // Xournal++ Linux Locale
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showLinuxLanguageDialog = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_linux_language_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = LinuxLocaleManager.getLocaleDisplayName(context, currentLinuxLocaleTag),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.pref_linux_language_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 4. Xournal++ Preferences & Configuration Backup
            var showConfigViewerDialog by remember { mutableStateOf(false) }
            var showAdvancedExportDialog by remember { mutableStateOf(false) }
            var exportTargetType by remember { mutableStateOf(ConfigFileType.SETTINGS_XML) }

            val exportFileLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                if (uri != null) {
                    val result = configManager.exportConfigFile(context, exportTargetType, uri)
                    scope.launch {
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar("Successfully exported ${exportTargetType.fileName}")
                        } else {
                            snackbarHostState.showSnackbar("Export failed: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            }

            val exportZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/zip")
            ) { uri ->
                if (uri != null) {
                    val result = configManager.exportFullBackupZip(context, uri)
                    scope.launch {
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar("Exported ${result.getOrNull()} configuration files to backup ZIP")
                        } else {
                            snackbarHostState.showSnackbar("ZIP export failed: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            }

            val importFileOrZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    val uriString = uri.toString().lowercase()
                    scope.launch {
                        if (uriString.endsWith(".zip") || context.contentResolver.getType(uri)?.contains("zip") == true) {
                            val result = configManager.importFullBackupZip(context, uri)
                            if (result.isSuccess) {
                                val count = result.getOrNull() ?: 0
                                snackbarHostState.showSnackbar("Successfully restored $count config files from ZIP archive")
                            } else {
                                snackbarHostState.showSnackbar("ZIP restore failed: ${result.exceptionOrNull()?.message}")
                            }
                        } else {
                            val result = configManager.importConfigFile(context, uri)
                            if (result.isSuccess) {
                                val type = result.getOrNull()
                                snackbarHostState.showSnackbar("Successfully imported ${type?.fileName ?: "configuration"}")
                            } else {
                                snackbarHostState.showSnackbar("Import failed: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    }
                }
            }

            if (showConfigViewerDialog) {
                ConfigViewerDialog(
                    configManager = configManager,
                    onDismiss = { showConfigViewerDialog = false }
                )
            }

            if (showAdvancedExportDialog) {
                AlertDialog(
                    onDismissRequest = { showAdvancedExportDialog = false },
                    properties = AppDialogDefaults.Properties,
                    modifier = Modifier.promptWidth(),
                    title = {
                        Text(
                            text = "Export Specific Config File",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Select a specific configuration component to export individually:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ConfigFileType.values().forEach { type ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            exportTargetType = type
                                            showAdvancedExportDialog = false
                                            exportFileLauncher.launch(type.fileName)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = type.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = type.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAdvancedExportDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            Text(
                text = "Xournal++ Preferences & Backup",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(context, CanvasActivity::class.java).apply {
                                putExtra(CanvasActivity.EXTRA_OPEN_PREFERENCES, true)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Native GTK Preferences Dialog", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { showConfigViewerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Inspect settings.xml File", fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Xournal++'s native \"Autoload most recent file on startup\" in Load/Save is automatically overridden to preserve Android \"Continue where you left off\" workspace control.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = "Backup & Restore:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(
                                onClick = { exportZipLauncher.launch("xournalpp_config_backup.zip") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Full Backup (.zip)", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { importFileOrZipLauncher.launch(arrayOf("*/*", "application/zip", "text/xml")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore from Backup (.zip or .xml)", fontWeight = FontWeight.SemiBold)
                            }

                            TextButton(
                                onClick = { showAdvancedExportDialog = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export Specific File...", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // 4. Nested Settings Sections Navigation Cards
            Text(
                text = "Engine & Input Configuration",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Floating Toolbar", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Position placement, pin/unpin auto-collapse, button visibility") },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        },
                        modifier = Modifier.clickable { onNavigate(SettingsSubpage.TOOLBAR) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Keyboard & Navigation", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Auto-keyboard toggle, character-based input, emergency gestures") },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        },
                        modifier = Modifier.clickable { onNavigate(SettingsSubpage.KEYBOARD) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Stylus & Touch Input", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Direct touch, stylus click modes, Lenovo pen gesture mappings") },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        },
                        modifier = Modifier.clickable { onNavigate(SettingsSubpage.INPUT) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Display & Resolution", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Resolution scaling, filtering, keyboard resizing, fullscreen mode") },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.DisplaySettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        },
                        modifier = Modifier.clickable { onNavigate(SettingsSubpage.DISPLAY) }
                    )
                }
            }

            // 5. About & Licenses (At the Bottom)
            Text(
                text = "About",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Xournal++ Version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1.3.7-custom", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("X11 Engine", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Termux-X11", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Window Manager", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Openbox (Auto-Maximized)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, LicensesActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Source Licenses", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarSettingsScreen(
    onNavigateToPositionEditor: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    var presetId by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_TOOLBAR_POSITION_PRESET, "top_center") ?: "top_center")
    }
    var normX by remember {
        mutableFloatStateOf(x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_X_RATIO, 0.5f))
    }
    var normY by remember {
        mutableFloatStateOf(x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_Y_RATIO, 0.0f))
    }
    var centerWithinSafeArea by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, false))
    }
    var startCollapsed by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_START_COLLAPSED, false))
    }
    var alwaysShowFileName by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME, false))
    }
    var pinButtonMode by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE, false))
    }
    var autoCollapseTimeoutMs by remember {
        mutableIntStateOf(x11Prefs.getInt(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, 5000))
    }
    var autoCollapseMsText by remember {
        mutableStateOf(autoCollapseTimeoutMs.toString())
    }
    var stylusHoverExpands by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, true))
    }

    var showStylusMode by remember {
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
    var showTitle by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, true))
    }
    var showBack by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_BACK, true))
    }
    var showClose by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, true))
    }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Floating Toolbar", fontWeight = FontWeight.Bold, modifier = Modifier.a11yHeading()) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minTouchTarget()) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Toolbar Placement & Positioning
            Text("Placement & Calibration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val presetLabel = if (presetId == "custom") {
                        "Custom (X:${(normX * 100).roundToInt()}%, Y:${(normY * 100).roundToInt()}%)"
                    } else {
                        STANDARD_TOOLBAR_PRESETS.firstOrNull { it.id == presetId }?.label ?: "Top Center"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Toolbar Position", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Position anchor: $presetLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = onNavigateToPositionEditor,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Configure")
                        }
                    }

                    HorizontalDivider()

                    SettingsSwitchListItem(
                        headline = "Confine to Screen Safe Area",
                        supporting = "Align and keep the floating toolbar within calibrated display corner margins.",
                        checked = centerWithinSafeArea,
                        onCheckedChange = {
                            centerWithinSafeArea = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS)
                        }
                    )
                }
            }

            // 2. Startup & Collapse Behavior
            Text("Startup & Collapse Behavior", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Start Collapsed",
                        supporting = "Automatically launch the canvas with the toolbar minimized into a compact pill.",
                        checked = startCollapsed,
                        onCheckedChange = {
                            startCollapsed = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_START_COLLAPSED, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_START_COLLAPSED)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Always Show File Name",
                        supporting = "Keep the active note file name in the toolbar at all times instead of switching to dialog names.",
                        checked = alwaysShowFileName,
                        onCheckedChange = {
                            alwaysShowFileName = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Replace Collapse with Pin / Unpin",
                        supporting = "Tap collapsed toolbar to expand. Unpinned toolbar auto-collapses after inactivity; pin button holds it open.",
                        checked = pinButtonMode,
                        onCheckedChange = {
                            pinButtonMode = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE)
                        }
                    )

                    if (pinButtonMode) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Collapse Inactivity Timeout", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Duration before unpinned toolbar collapses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${autoCollapseTimeoutMs / 1000f} s",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Slider: 1 to 30 seconds in 1-second steps
                            var sliderSeconds by remember(autoCollapseTimeoutMs) {
                                mutableFloatStateOf((autoCollapseTimeoutMs / 1000f).coerceIn(1f, 30f))
                            }
                            Slider(
                                value = sliderSeconds,
                                onValueChange = { newVal ->
                                    val roundedSec = newVal.roundToInt()
                                    sliderSeconds = roundedSec.toFloat()
                                    val newMs = roundedSec * 1000
                                    autoCollapseTimeoutMs = newMs
                                    autoCollapseMsText = newMs.toString()
                                    x11Prefs.edit().putInt(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, newMs).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS)
                                },
                                valueRange = 1f..30f,
                                steps = 28 // 1s increments from 1s to 30s
                            )

                            // Exact timing in milliseconds text field
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Exact Timeout (ms)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "1s = 1000ms (e.g. 3500ms)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = autoCollapseMsText,
                                    onValueChange = { input ->
                                        autoCollapseMsText = input
                                        val parsed = input.toIntOrNull()
                                        if (parsed != null && parsed in 500..60000) {
                                            autoCollapseTimeoutMs = parsed
                                            x11Prefs.edit().putInt(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, parsed).apply()
                                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS)
                                        }
                                    },
                                    modifier = Modifier.width(110.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    placeholder = { Text("5000") }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Expand on Stylus Hover",
                        supporting = "Automatically expand the collapsed toolbar when hovering over it with a stylus pen.",
                        checked = stylusHoverExpands,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (stylusHoverExpands) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            stylusHoverExpands = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS)
                        }
                    )
                }
            }

            // 3. Visible Elements & Action Buttons
            Text("Visible Elements & Shortcuts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Stylus Click Mode Switcher (L/M/R)",
                        supporting = "Displays Left / Middle / Right click toggle buttons directly in the floating toolbar.",
                        checked = showStylusMode,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.alpha(if (showStylusMode) 1f else 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box(modifier = Modifier.size(18.dp, 18.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "L",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Box(modifier = Modifier.size(18.dp, 18.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "M",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(modifier = Modifier.size(18.dp, 18.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "R",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        onCheckedChange = {
                            showStylusMode = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Finger as Stylus Toggle",
                        supporting = "Displays a toolbar button to quickly switch between drawing with your finger as a stylus and standard touch/gesture navigation.",
                        checked = showTouchStylus,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.alpha(if (showTouchStylus) 1f else 0.4f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Draw,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showTouchStylus = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Turn off Touch as Stylus on Stylus Hover",
                        supporting = "Automatically disables finger drawing and restores touch navigation as soon as a physical stylus pen hovers over or touches the screen.",
                        checked = disableTouchStylusOnStylusHover,
                        onCheckedChange = {
                            disableTouchStylusOnStylusHover = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Remember Last Toggled State",
                        supporting = "Preserves whether Finger as Stylus was active across app launches. When disabled, Finger as Stylus resets to off on startup.",
                        checked = rememberFingerAsStylusState,
                        onCheckedChange = {
                            rememberFingerAsStylusState = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Window Title & Document Icon",
                        supporting = "Displays note title and dynamic window type icon.",
                        checked = showTitle,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.alpha(if (showTitle) 1f else 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Notes.xopp",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showTitle = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_TITLE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Back Button",
                        supporting = "Displays back arrow button to gracefully save and exit canvas.",
                        checked = showBack,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showBack) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showBack = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_BACK, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_BACK)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Close Button (Ctrl+Q)",
                        supporting = "Displays a red Material 3 close button on the floating toolbar to trigger Xournal++ quit / save prompt.",
                        checked = showClose,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showClose) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showClose = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_CLOSE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Soft Keyboard Toggle",
                        supporting = "Displays soft keyboard show/hide action button.",
                        checked = showKeyboard,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showKeyboard) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Keyboard,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showKeyboard = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Movable Drag Handle",
                        supporting = "Displays handle to long-press and drag toolbar anywhere.",
                        checked = showDragHandle,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showDragHandle) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showDragHandle = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Cut Action (Ctrl+X)",
                        supporting = "Displays Cut clipboard action button.",
                        checked = showCut,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showCut) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCut,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showCut = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CUT, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_CUT)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Copy Action (Ctrl+C)",
                        supporting = "Displays Copy clipboard action button.",
                        checked = showCopy,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showCopy) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showCopy = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_COPY, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_COPY)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Paste Action (Ctrl+V)",
                        supporting = "Displays Paste clipboard action button.",
                        checked = showPaste,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showPaste) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showPaste = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_PASTE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Insert Image Action",
                        supporting = "Displays Image action button on the toolbar to insert pictures from Camera, Photos/Gallery, or Files.",
                        checked = showImage,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showImage = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_IMAGE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_IMAGE)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    var autoShowIme by remember {
        mutableStateOf(prefs.getBoolean("pref_auto_show_ime_on_focus", true))
    }
    var enforceCharBasedInput by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT, false))
    }
    var tripleBackForceClose by remember {
        mutableStateOf(prefs.getBoolean("pref_triple_back_force_close", true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard & Navigation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Auto-toggle Keyboard on Focus",
                        supporting = "Automatically open the soft keyboard when tapping into text boxes or canvas annotations.",
                        checked = autoShowIme,
                        onCheckedChange = {
                            autoShowIme = it
                            prefs.edit().putBoolean("pref_auto_show_ime_on_focus", it).apply()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Enforce Character-Based Input",
                        supporting = "Directly dispatch committed Unicode characters rather than synthesized hardware key scancodes.",
                        checked = enforceCharBasedInput,
                        onCheckedChange = {
                            enforceCharBasedInput = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Triple-Back Emergency Force Close",
                        supporting = "Pressing Back 3 times rapidly inside Canvas brings up a force-close dialog if X11 becomes unresponsive.",
                        checked = tripleBackForceClose,
                        onCheckedChange = {
                            tripleBackForceClose = it
                            prefs.edit().putBoolean("pref_triple_back_force_close", it).apply()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputSettingsScreen(
    onNavigateToLenovoPen: () -> Unit,
    onNavigateToToolbar: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    var touchMode by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_TOUCH_MODE, "3") ?: "3")
    }
    var scaleTouchpad by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_SCALE_TOUCHPAD, true))
    }
    var stylusIsMouse by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_STYLUS_IS_MOUSE, false))
    }
    var stylusButtonContactModifier by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_STYLUS_BUTTON_CONTACT_MODIFIER, false))
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
    var stylusHoverExpands by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, true))
    }
    var showMouseHelper by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_SHOW_MOUSE_HELPER, false))
    }
    var tapToMove by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TAP_TO_MOVE, false))
    }
    var ignoreGamepad by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_IGNORE_GAMEPAD_EVENTS, false))
    }
    var capturedSpeed by remember {
        mutableFloatStateOf(x11Prefs.getInt(X11Preferences.KEY_CAPTURED_POINTER_SPEED, 100).toFloat())
    }
    var transformCaptured by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_TRANSFORM_CAPTURED_POINTER, "no") ?: "no")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stylus & Input", fontWeight = FontWeight.Bold, modifier = Modifier.a11yHeading()) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minTouchTarget()) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Touch Mode
            Text("Touch Input Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val touchOptions = listOf(
                "3" to "Direct Touch (1:1)",
                "1" to "Trackpad",
                "2" to "Simulated"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                touchOptions.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = touchMode == value,
                        onClick = {
                            touchMode = value
                            x11Prefs.edit().putString(X11Preferences.KEY_TOUCH_MODE, value).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOUCH_MODE)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, touchOptions.size),
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            // Finger as Stylus Section
            Text("Finger as Stylus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Show Finger as Stylus Toggle in Toolbar",
                        supporting = "Displays a toggle button in the floating toolbar to quickly switch between drawing with your finger as a stylus and standard touch navigation.",
                        checked = showTouchStylus,
                        onCheckedChange = {
                            showTouchStylus = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Turn off Touch as Stylus on Stylus Hover",
                        supporting = "Automatically disables finger drawing and restores touch navigation as soon as a physical stylus pen hovers over or touches the screen.",
                        checked = disableTouchStylusOnStylusHover,
                        onCheckedChange = {
                            disableTouchStylusOnStylusHover = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Remember Last Toggled State",
                        supporting = "Preserves whether Finger as Stylus was active across app launches. When disabled, Finger as Stylus resets to off on startup.",
                        checked = rememberFingerAsStylusState,
                        onCheckedChange = {
                            rememberFingerAsStylusState = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE)
                        }
                    )
                }
            }

            // Stylus Controls
            Text("Stylus & Pointer Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Show Stylus Click Mode in Toolbar",
                        supporting = "Displays Left / Middle / Right click toggle capsule directly in the floating toolbar (also configurable in Floating Toolbar settings).",
                        checked = showStylusClickOverride,
                        onCheckedChange = {
                            showStylusClickOverride = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Expand Toolbar on Stylus Hover",
                        supporting = "Automatically expand the collapsed floating toolbar when hovering over it with a stylus pen.",
                        checked = stylusHoverExpands,
                        onCheckedChange = {
                            stylusHoverExpands = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Enable Stylus Mouse Mode",
                        supporting = "Treat hardware stylus touch events as desktop mouse pointer clicks.",
                        checked = stylusIsMouse,
                        onCheckedChange = {
                            stylusIsMouse = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_STYLUS_IS_MOUSE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_STYLUS_IS_MOUSE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Stylus Button Contact Modifier Mode",
                        supporting = "Modify contact properties when the stylus side barrel button is depressed.",
                        checked = stylusButtonContactModifier,
                        onCheckedChange = {
                            stylusButtonContactModifier = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_STYLUS_BUTTON_CONTACT_MODIFIER, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_STYLUS_BUTTON_CONTACT_MODIFIER)
                        }
                    )

                    if (touchMode == "1") {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchListItem(
                            headline = "Show Mouse Click Helper Overlay",
                            supporting = "On-screen Left / Middle / Right floating mouse buttons for trackpad mode.",
                            checked = showMouseHelper,
                            onCheckedChange = {
                                showMouseHelper = it
                                x11Prefs.edit().putBoolean(X11Preferences.KEY_SHOW_MOUSE_HELPER, it).apply()
                                X11Preferences.notifyChanged(context, X11Preferences.KEY_SHOW_MOUSE_HELPER)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchListItem(
                            headline = "Scale Trackpad to Display Factor",
                            supporting = "Scale cursor movement speed according to display resolution.",
                            checked = scaleTouchpad,
                            onCheckedChange = {
                                scaleTouchpad = it
                                x11Prefs.edit().putBoolean(X11Preferences.KEY_SCALE_TOUCHPAD, it).apply()
                                X11Preferences.notifyChanged(context, X11Preferences.KEY_SCALE_TOUCHPAD)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchListItem(
                            headline = "Enable Tap-to-Move",
                            supporting = "Tap and drag to move pointer without holding physical clicks.",
                            checked = tapToMove,
                            onCheckedChange = {
                                tapToMove = it
                                x11Prefs.edit().putBoolean(X11Preferences.KEY_TAP_TO_MOVE, it).apply()
                                X11Preferences.notifyChanged(context, X11Preferences.KEY_TAP_TO_MOVE)
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchListItem(
                        headline = "Ignore Gamepad Events",
                        supporting = "Suppress controller joystick and button input events from driving pointer.",
                        checked = ignoreGamepad,
                        onCheckedChange = {
                            ignoreGamepad = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_IGNORE_GAMEPAD_EVENTS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_IGNORE_GAMEPAD_EVENTS)
                        }
                    )
                }
            }

            // External Pointer Speed & Rotation
            Text("External Captured Pointer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pointer Speed Factor", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${capturedSpeed.roundToInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = capturedSpeed,
                        onValueChange = { capturedSpeed = it },
                        onValueChangeFinished = {
                            x11Prefs.edit().putInt(X11Preferences.KEY_CAPTURED_POINTER_SPEED, capturedSpeed.roundToInt()).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_CAPTURED_POINTER_SPEED)
                        },
                        valueRange = 1f..300f
                    )

                    HorizontalDivider()

                    val transformOptions = listOf(
                        "no" to "No Rotation",
                        "c" to "Clockwise 90°",
                        "cc" to "Counter-Clockwise 90°",
                        "ud" to "Upside Down 180°",
                        "at" to "Automatic for Touchpad"
                    )
                    var transformExpanded by remember { mutableStateOf(false) }
                    val currentTransformLabel = transformOptions.firstOrNull { it.first == transformCaptured }?.second ?: "No Rotation"

                    Text("Pointer Movement Rotation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    ExposedDropdownMenuBox(
                        expanded = transformExpanded,
                        onExpandedChange = { transformExpanded = !transformExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentTransformLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transformExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = transformExpanded,
                            onDismissRequest = { transformExpanded = false }
                        ) {
                            transformOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        transformCaptured = value
                                        transformExpanded = false
                                        x11Prefs.edit().putString(X11Preferences.KEY_TRANSFORM_CAPTURED_POINTER, value).apply()
                                        X11Preferences.notifyChanged(context, X11Preferences.KEY_TRANSFORM_CAPTURED_POINTER)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Lenovo Pen Mapping Sub-page Tile
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Lenovo Pen Button Gesture Mapping", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Configure single/double/triple/long click shortcuts for Lenovo stylus barrel buttons.") },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Gesture, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                    modifier = Modifier.clickable { onNavigateToLenovoPen() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LenovoPenSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    var showDetections by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_LENOVO_PEN_SHOW_DETECTIONS, false))
    }
    var showToggleDebug by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS, false))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lenovo Pen Mapping", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Show Detection Toasts",
                        supporting = "Display transient toast messages when barrel button gestures are detected.",
                        checked = showDetections,
                        onCheckedChange = {
                            showDetections = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_LENOVO_PEN_SHOW_DETECTIONS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_LENOVO_PEN_SHOW_DETECTIONS)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchListItem(
                        headline = "Show Toggle Debug Toasts",
                        supporting = "Display state toasts when toggle mode button states change.",
                        checked = showToggleDebug,
                        onCheckedChange = {
                            showToggleDebug = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS)
                        }
                    )
                }
            }

            val gestures = listOf(
                "Single Press (Keycode 600)" to (X11Preferences.KEY_LENOVO_PEN_SINGLE_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_SINGLE_TOGGLE, X11Preferences.KEY_LENOVO_PEN_SINGLE_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_SINGLE_DURATION)),
                "Double Press (Keycode 601)" to (X11Preferences.KEY_LENOVO_PEN_DOUBLE_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_DOUBLE_TOGGLE, X11Preferences.KEY_LENOVO_PEN_DOUBLE_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_DOUBLE_DURATION)),
                "Triple Press (Keycode 602)" to (X11Preferences.KEY_LENOVO_PEN_TRIPLE_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_TRIPLE_TOGGLE, X11Preferences.KEY_LENOVO_PEN_TRIPLE_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_TRIPLE_DURATION)),
                "Long Press (Keycode 603)" to (X11Preferences.KEY_LENOVO_PEN_LONG_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_LONG_TOGGLE, X11Preferences.KEY_LENOVO_PEN_LONG_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_LONG_DURATION)),
                "Long Press and Click (Keycode 604)" to (X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_TOGGLE, X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_DURATION))
            )

            gestures.forEach { (title, keys) ->
                val (actionKey, extraKeys) = keys
                val (toggleKey, offOnLiftKey, durationKey) = extraKeys

                var actionVal by remember {
                    mutableStateOf(x11Prefs.getString(actionKey, "disabled") ?: "disabled")
                }
                var toggleVal by remember {
                    mutableStateOf(x11Prefs.getBoolean(toggleKey, false))
                }
                var offOnLiftVal by remember {
                    mutableStateOf(x11Prefs.getBoolean(offOnLiftKey, false))
                }
                var durationVal by remember {
                    mutableStateOf(x11Prefs.getString(durationKey, "150") ?: "150")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Map To Action:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val actionOptions = listOf(
                            "disabled" to "Disabled",
                            "2" to "Primary (Button 2)",
                            "3" to "Secondary (Button 3)"
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            actionOptions.forEachIndexed { index, (value, label) ->
                                SegmentedButton(
                                    selected = actionVal == value,
                                    onClick = {
                                        actionVal = value
                                        x11Prefs.edit().putString(actionKey, value).apply()
                                        X11Preferences.notifyChanged(context, actionKey)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index, actionOptions.size),
                                    label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }

                        val isActionEnabled = actionVal != "disabled"
                        val isHoldDurationEnabled = isActionEnabled && !toggleVal
                        val isOffOnLiftEnabled = isActionEnabled && toggleVal

                        HorizontalDivider()

                        SettingsSwitchListItem(
                            headline = "Toggle Target Button",
                            supporting = if (toggleVal)
                                "On: toggle mode — gesture once for down and again for up."
                            else
                                "Off: momentary mode — press and hold target for specified duration.",
                            checked = toggleVal,
                            enabled = isActionEnabled,
                            onCheckedChange = {
                                toggleVal = it
                                x11Prefs.edit().putBoolean(toggleKey, it).apply()
                                X11Preferences.notifyChanged(context, toggleKey)
                            },
                            modifier = Modifier.alpha(if (isActionEnabled) 1f else 0.38f)
                        )

                        HorizontalDivider()

                        SettingsSwitchListItem(
                            headline = "Toggle OFF on Lift",
                            supporting = "When toggled ON and pen lifts after contact, release the mapped button automatically.",
                            checked = offOnLiftVal,
                            enabled = isOffOnLiftEnabled,
                            onCheckedChange = {
                                offOnLiftVal = it
                                x11Prefs.edit().putBoolean(offOnLiftKey, it).apply()
                                X11Preferences.notifyChanged(context, offOnLiftKey)
                            },
                            modifier = Modifier.alpha(if (isOffOnLiftEnabled) 1f else 0.38f)
                        )

                        HorizontalDivider()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isHoldDurationEnabled) 1f else 0.38f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Hold Duration (ms)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Applied when Toggle mode is OFF (10 - 8192 ms).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = durationVal,
                                    enabled = isHoldDurationEnabled,
                                    onValueChange = { input ->
                                        durationVal = input
                                        val num = input.toLongOrNull()
                                        if (num != null && num in 10..8192) {
                                            x11Prefs.edit().putString(durationKey, input).apply()
                                            X11Preferences.notifyChanged(context, durationKey)
                                        }
                                    },
                                    modifier = Modifier.width(110.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            val durationPresets = listOf("50", "150", "300", "500", "1000")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                durationPresets.forEach { preset ->
                                    val isSelected = durationVal == preset
                                    OutlinedButton(
                                        onClick = {
                                            durationVal = preset
                                            x11Prefs.edit().putString(durationKey, preset).apply()
                                            X11Preferences.notifyChanged(context, durationKey)
                                        },
                                        enabled = isHoldDurationEnabled,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${preset}ms",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected && isHoldDurationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    onNavigateToSafeAreaEditor: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }
    val aournalPrefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }

    var selectedUiScale by remember {
        mutableStateOf(aournalPrefs.getString("pref_ui_scale", "1.0") ?: "1.0")
    }
    var resMode by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_DISPLAY_RES_MODE, "native") ?: "native")
    }
    var displayScale by remember {
        mutableFloatStateOf(x11Prefs.getInt(X11Preferences.KEY_DISPLAY_SCALE, 100).toFloat())
    }
    var exactRes by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_DISPLAY_RES_EXACT, "1280x1024") ?: "1280x1024")
    }
    var customRes by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_DISPLAY_RES_CUSTOM, "1280x1024") ?: "1280x1024")
    }
    var filteringMode by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_DISPLAY_FILTERING, "nearest") ?: "nearest")
    }
    var adjustRes by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_ADJUST_RESOLUTION, false))
    }
    var displayStretch by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_DISPLAY_STRETCH, false))
    }
    var reseedIme by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_RESEED, false))
    }
    var fullscreenCanvas by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_FULLSCREEN, false))
    }
    var idleTimeout by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, "system") ?: "system")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display & Resolution", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Canvas UI Scale (Discrete Slider + Custom Fractional Field)
            var uiScaleFloat by remember {
                mutableFloatStateOf((selectedUiScale.toFloatOrNull() ?: 1.0f).coerceIn(0.5f, 3.0f))
            }
            var customScaleText by remember {
                mutableStateOf(selectedUiScale)
            }

            Text("Canvas UI Scale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GTK Interface Scale", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Scales toolbars, menus, and canvas controls.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.2f", uiScaleFloat)}x",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Slider(
                        value = uiScaleFloat,
                        onValueChange = { newVal ->
                            val rounded = (newVal * 20).roundToInt() / 20f // Discrete step of 0.05
                            uiScaleFloat = rounded
                            val str = String.format(java.util.Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
                            val formatted = if (str.contains('.')) str else "$str.0"
                            selectedUiScale = formatted
                            customScaleText = formatted
                            aournalPrefs.edit().putString("pref_ui_scale", formatted).apply()
                        },
                        valueRange = 0.5f..3.0f,
                        steps = 49 // 0.05 increments between 0.5 and 3.0
                    )

                    // Preset Chips
                    val scalePresets = listOf("1.0", "1.25", "1.5", "1.75", "2.0", "2.5")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        scalePresets.forEach { preset ->
                            val presetFloat = preset.toFloat()
                            val isSelected = Math.abs(uiScaleFloat - presetFloat) < 0.01f
                            OutlinedButton(
                                onClick = {
                                    uiScaleFloat = presetFloat
                                    selectedUiScale = preset
                                    customScaleText = preset
                                    aournalPrefs.edit().putString("pref_ui_scale", preset).apply()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${preset}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Custom Fractional Scale", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Enter exact decimal scale (0.50 – 4.00)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = customScaleText,
                            onValueChange = { input ->
                                customScaleText = input
                                val parsed = input.toFloatOrNull()
                                if (parsed != null && parsed in 0.5f..4.0f) {
                                    selectedUiScale = input
                                    uiScaleFloat = parsed.coerceIn(0.5f, 3.0f)
                                    aournalPrefs.edit().putString("pref_ui_scale", input).apply()
                                }
                            },
                            modifier = Modifier.width(110.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("1.33") }
                        )
                    }
                }
            }

            // Display Resolution Mode
            Text("Resolution Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val resOptions = listOf(
                "native" to "Native",
                "scaled" to "Scaled",
                "exact" to "Exact",
                "custom" to "Custom"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                resOptions.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = resMode == value,
                        onClick = {
                            resMode = value
                            x11Prefs.edit().putString(X11Preferences.KEY_DISPLAY_RES_MODE, value).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_RES_MODE)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, resOptions.size),
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (resMode) {
                        "scaled" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Display Scale Factor", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${displayScale.roundToInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = displayScale,
                                onValueChange = { displayScale = it },
                                onValueChangeFinished = {
                                    val rounded = (displayScale / 10).roundToInt() * 10
                                    displayScale = rounded.toFloat()
                                    x11Prefs.edit().putInt(X11Preferences.KEY_DISPLAY_SCALE, rounded).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_SCALE)
                                },
                                valueRange = 30f..300f,
                                steps = 26
                            )
                        }
                        "exact" -> {
                            val exactOptions = listOf(
                                "1280x720", "1280x800", "1280x1024", "1366x768",
                                "1600x900", "1600x1200", "1920x1080", "1920x1200",
                                "2048x1536", "2560x1440", "2560x1600", "3840x2160"
                            )
                            var exactExpanded by remember { mutableStateOf(false) }

                            Text("Preset Resolution", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            ExposedDropdownMenuBox(
                                expanded = exactExpanded,
                                onExpandedChange = { exactExpanded = !exactExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = exactRes,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exactExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = exactExpanded,
                                    onDismissRequest = { exactExpanded = false }
                                ) {
                                    exactOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                exactRes = option
                                                exactExpanded = false
                                                x11Prefs.edit().putString(X11Preferences.KEY_DISPLAY_RES_EXACT, option).apply()
                                                X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_RES_EXACT)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        "custom" -> {
                            Text("Custom Resolution (WxH)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = customRes,
                                onValueChange = {
                                    customRes = it
                                    if (it.matches(Regex("^[0-9]+x[0-9]+$"))) {
                                        x11Prefs.edit().putString(X11Preferences.KEY_DISPLAY_RES_CUSTOM, it).apply()
                                        X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_RES_CUSTOM)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("e.g. 1920x1080") }
                            )
                        }
                        else -> {
                            Text("Using device's full 1:1 physical pixel resolution.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider()

                    Text("Display Filtering Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val filterOptions = listOf(
                        "nearest" to "Nearest (Sharp)",
                        "bilinear" to "Bilinear (Smooth)"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        filterOptions.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = filteringMode == value,
                                onClick = {
                                    filteringMode = value
                                    x11Prefs.edit().putString(X11Preferences.KEY_DISPLAY_FILTERING, value).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_FILTERING)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, filterOptions.size),
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }
            }

            // Canvas Layout & Insets Behavior
            Text("Canvas Layout & System Insets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Adjust Resolution for Orientation",
                        supporting = "Automatically swap width and height when device orientation rotates.",
                        checked = adjustRes,
                        onCheckedChange = {
                            adjustRes = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_ADJUST_RESOLUTION, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_ADJUST_RESOLUTION)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Stretch to Fit Display",
                        supporting = "Scale canvas image non-proportionally to eliminate black letterbox bars.",
                        checked = displayStretch,
                        onCheckedChange = {
                            displayStretch = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_DISPLAY_STRETCH, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_STRETCH)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Reseed Screen with Soft Keyboard",
                        supporting = "Dynamically adjust X11 screen dimensions when on-screen keyboard appears.",
                        checked = reseedIme,
                        onCheckedChange = {
                            reseedIme = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_RESEED, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_RESEED)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Fullscreen Canvas",
                        supporting = "Hide status and navigation bars. Disabling allows Android's floating rotation button to appear.",
                        checked = fullscreenCanvas,
                        onCheckedChange = {
                            fullscreenCanvas = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_FULLSCREEN, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_FULLSCREEN)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    val safeCustom = x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_CUSTOM_EDGES, false)
                    val safeAll = x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_MARGIN_ALL, 0)
                    val safeLeft = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_LEFT, 0) else safeAll
                    val safeTop = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_TOP, 0) else safeAll
                    val safeRight = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_RIGHT, 0) else safeAll
                    val safeBottom = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_BOTTOM, 0) else safeAll

                    val summaryText = if (safeCustom) {
                        "Custom: L:${safeLeft}dp T:${safeTop}dp R:${safeRight}dp B:${safeBottom}dp"
                    } else if (safeAll > 0) {
                        "Uniform: ${safeAll} dp on all edges"
                    } else {
                        "Full Screen (0 dp)"
                    }

                    ListItem(
                        headlineContent = { Text("Screen Safe Area Calibration", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Set margin insets to prevent UI clipping from rounded corners ($summaryText).") },
                        trailingContent = {
                            FilledTonalButton(
                                onClick = onNavigateToSafeAreaEditor,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Calibrate")
                            }
                        }
                    )
                }
            }

            // Screen Idle Timeout
            Text("Screen Idle Timeout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val timeoutOptions = listOf(
                        "never" to "Never (Keep screen on)",
                        "1" to "1 minute",
                        "5" to "5 minutes",
                        "10" to "10 minutes",
                        "20" to "20 minutes",
                        "60" to "1 hour",
                        "system" to "System default"
                    )
                    var timeoutExpanded by remember { mutableStateOf(false) }
                    val currentTimeoutLabel = timeoutOptions.firstOrNull { it.first == idleTimeout }?.second ?: "System default"

                    ExposedDropdownMenuBox(
                        expanded = timeoutExpanded,
                        onExpandedChange = { timeoutExpanded = !timeoutExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentTimeoutLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeoutExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = timeoutExpanded,
                            onDismissRequest = { timeoutExpanded = false }
                        ) {
                            timeoutOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        idleTimeout = value
                                        timeoutExpanded = false
                                        x11Prefs.edit().putString(X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, value).apply()
                                        X11Preferences.notifyChanged(context, X11Preferences.KEY_SCREEN_IDLE_TIMEOUT)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class FileNameTemplateTarget(
    val key: String,
    val label: String,
    val defaultTemplate: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    NEW_FILE(
        FileNameTemplateEngine.PREF_KEY_TEMPLATE_NEW_FILE,
        "New File",
        FileNameTemplateEngine.DEFAULT_TEMPLATE_NEW_FILE,
        "Default name pattern when creating a blank note in Home or Files Hub.",
        Icons.Default.Add
    ),
    SAVE_AS(
        FileNameTemplateEngine.PREF_KEY_TEMPLATE_SAVE_AS,
        "Save As",
        FileNameTemplateEngine.DEFAULT_TEMPLATE_SAVE_AS,
        "Default name pattern when duplicating or saving a note under a new copy.",
        Icons.Default.ContentCopy
    ),
    EXPORT_PDF(
        FileNameTemplateEngine.PREF_KEY_TEMPLATE_EXPORT_PDF,
        "Export PDF",
        FileNameTemplateEngine.DEFAULT_TEMPLATE_EXPORT_PDF,
        "Default file name pattern when exporting notes to Exports/ folder.",
        Icons.Default.FileDownload
    ),
    SHARE_PDF(
        FileNameTemplateEngine.PREF_KEY_TEMPLATE_SHARE_PDF,
        "Share PDF",
        FileNameTemplateEngine.DEFAULT_TEMPLATE_SHARE_PDF,
        "Default file name pattern when rendering and sharing PDF with external apps.",
        Icons.Default.PictureAsPdf
    ),
    SHARE_XOPP(
        FileNameTemplateEngine.PREF_KEY_TEMPLATE_SHARE_XOPP,
        "Share XOPP",
        FileNameTemplateEngine.DEFAULT_TEMPLATE_SHARE_XOPP,
        "Default file name pattern when sharing the notebook (.xopp) file.",
        Icons.Default.Share
    )
}

private data class TemplatePlaceholderGuide(
    val token: String,
    val label: String,
    val description: String
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FileNameTemplateSettingsCard(
    prefs: android.content.SharedPreferences,
    context: Context,
    snackbarHostState: androidx.compose.material3.SnackbarHostState
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var selectedTarget by remember { mutableStateOf(FileNameTemplateTarget.NEW_FILE) }

    // Map of active template values from preferences
    var currentTemplateText by remember(selectedTarget) {
        mutableStateOf(prefs.getString(selectedTarget.key, selectedTarget.defaultTemplate) ?: selectedTarget.defaultTemplate)
    }

    var selectedGuideCategory by remember { mutableStateOf(0) }
    var selectedPlaceholderHelp by remember { mutableStateOf<TemplatePlaceholderGuide?>(null) }

    // Sample dummy file for live preview demonstration
    val dummySampleFile = remember {
        java.io.File(
            java.io.File(
                java.io.File(context.filesDir, "sample_root"),
                "Physics_101"
            ),
            "Mechanics_Lecture.xopp"
        )
    }

    val livePreviewResult = remember(currentTemplateText, selectedTarget) {
        try {
            FileNameTemplateEngine.evaluate(currentTemplateText, context, dummySampleFile)
        } catch (e: Exception) {
            "Invalid template syntax"
        }
    }

    val datePlaceholders = remember {
        listOf(
            TemplatePlaceholderGuide("{date}", "Date (yyyy-MM-dd)", "Current date formatted as 2026-08-30"),
            TemplatePlaceholderGuide("{time}", "Time (HH-mm-ss)", "Current time formatted as 14-30-00"),
            TemplatePlaceholderGuide("{datetime}", "DateTime (ISO)", "Combined timestamp: 2026-08-30_14-30-00"),
            TemplatePlaceholderGuide("{datetime:yyyy_MM_dd}", "Custom Format", "Custom SimpleDateFormat pattern inside {datetime:PATTERN}"),
            TemplatePlaceholderGuide("{year}", "Year (YYYY)", "4-digit current year e.g. 2026"),
            TemplatePlaceholderGuide("{month}", "Month (MM)", "2-digit month e.g. 08"),
            TemplatePlaceholderGuide("{day}", "Day (dd)", "2-digit day of month e.g. 30"),
            TemplatePlaceholderGuide("{hour}", "Hour (HH)", "2-digit 24-hour hour e.g. 14"),
            TemplatePlaceholderGuide("{minute}", "Minute (mm)", "2-digit minute e.g. 30"),
            TemplatePlaceholderGuide("{second}", "Second (ss)", "2-digit second e.g. 45")
        )
    }

    val contextPlaceholders = remember {
        listOf(
            TemplatePlaceholderGuide("{name}", "Note Name", "Base name of current note without extension (e.g. Mechanics_Lecture)"),
            TemplatePlaceholderGuide("{filename}", "Full Filename", "Original file name with extension"),
            TemplatePlaceholderGuide("{ext}", "Extension", "File extension without dot (e.g. xopp, pdf)"),
            TemplatePlaceholderGuide("{folder}", "Folder Name", "Direct parent folder name (e.g. Physics_101)"),
            TemplatePlaceholderGuide("{folder:1}", "Folder (Level N)", "Folder name N levels up in folder path hierarchy"),
            TemplatePlaceholderGuide("{folders}", "Folder Hierarchy", "Full relative folder path separated by underscores")
        )
    }

    val metadataPlaceholders = remember {
        listOf(
            TemplatePlaceholderGuide("{created}", "Created Date", "File creation timestamp (yyyy-MM-dd)"),
            TemplatePlaceholderGuide("{created:yyyy_MM_dd}", "Custom Created", "Custom SimpleDateFormat pattern for file creation time"),
            TemplatePlaceholderGuide("{modified}", "Modified Date", "File last modified timestamp (yyyy-MM-dd)"),
            TemplatePlaceholderGuide("{modified:HH-mm}", "Custom Modified", "Custom SimpleDateFormat pattern for modification time")
        )
    }

    val randomPlaceholders = remember {
        listOf(
            TemplatePlaceholderGuide("{random}", "Random (4 chars)", "4 random alphanumeric characters for unique filenames"),
            TemplatePlaceholderGuide("{random:6}", "Random (N chars)", "Custom N random alphanumeric characters e.g. {random:6}")
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Configure customizable file name templates using interactive placeholders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Scrollable Category Tabs for 5 Targets
            ScrollableTabRow(
                selectedTabIndex = selectedTarget.ordinal,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                FileNameTemplateTarget.values().forEach { target ->
                    Tab(
                        selected = selectedTarget == target,
                        onClick = { selectedTarget = target },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = target.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = target.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTarget == target) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            // Description of active target
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedTarget.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Template Editor TextField with Long-Press Reset
            OutlinedTextField(
                value = currentTemplateText,
                onValueChange = {
                    currentTemplateText = it
                    prefs.edit().putString(selectedTarget.key, it).apply()
                },
                label = { Text("${selectedTarget.label} Pattern") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .pointerInput(selectedTarget) {
                                detectTapGestures(
                                    onTap = {
                                        Toast.makeText(context, "Long press to reset", Toast.LENGTH_SHORT).show()
                                    },
                                    onLongPress = {
                                        currentTemplateText = selectedTarget.defaultTemplate
                                        prefs.edit().putString(selectedTarget.key, selectedTarget.defaultTemplate).apply()
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                                                vm?.defaultVibrator?.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                            } else {
                                                @Suppress("DEPRECATION")
                                                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                                @Suppress("DEPRECATION")
                                                v?.vibrate(50)
                                            }
                                        } catch (_: Exception) {}
                                        Toast.makeText(context, "Reset ${selectedTarget.label} pattern to default", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Long press to reset pattern",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            // Live Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Live Evaluation Preview",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val extensionSuffix = when (selectedTarget) {
                        FileNameTemplateTarget.EXPORT_PDF, FileNameTemplateTarget.SHARE_PDF -> ".pdf"
                        else -> ".xopp"
                    }
                    Text(
                        text = "$livePreviewResult$extensionSuffix",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider()

            // Interactive Placeholder Guide & Reference Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Placeholder Guide & Quick Insert",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tab ribbon matching the target template tab ribbon above
            val guideCategories = listOf("Date/Time", "Context", "Metadata", "Random")
            ScrollableTabRow(
                selectedTabIndex = selectedGuideCategory,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                guideCategories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedGuideCategory == index,
                        onClick = { selectedGuideCategory = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedGuideCategory == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            val activePlaceholders = when (selectedGuideCategory) {
                0 -> datePlaceholders
                1 -> contextPlaceholders
                2 -> metadataPlaceholders
                else -> randomPlaceholders
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activePlaceholders.forEach { guide ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            selectedPlaceholderHelp = guide
                            val separator = if (currentTemplateText.isNotEmpty() && !currentTemplateText.endsWith("-") && !currentTemplateText.endsWith("_")) "-" else ""
                            currentTemplateText = currentTemplateText + separator + guide.token
                            prefs.edit().putString(selectedTarget.key, currentTemplateText).apply()
                        },
                        label = {
                            Text(
                                text = guide.token,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            selectedPlaceholderHelp?.let { help ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = help.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = help.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLanguagePickerDialog(
    onDismissRequest: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = remember { AppLocaleHelper.getSupportedAppLanguages() }
    var searchQuery by remember { mutableStateOf("") }
    val currentTag = remember { AppLocaleHelper.getCurrentLanguageTag() }

    val filtered = remember(searchQuery, languages) {
        if (searchQuery.isBlank()) languages
        else {
            val q = searchQuery.trim().lowercase()
            languages.filter {
                it.displayName.lowercase().contains(q) ||
                it.nativeName.lowercase().contains(q) ||
                it.englishName.lowercase().contains(q) ||
                it.tag.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = { Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(
                text = stringResource(R.string.pref_app_language_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.pref_language_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.tag }) { item ->
                        val isSelected = currentTag.equals(item.tag, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onLanguageSelected(item.tag) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.nativeName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!item.isSystemDefault && !item.nativeName.equals(item.englishName, ignoreCase = true)) {
                                        Text(
                                            text = item.englishName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun XournalppLanguagePickerDialog(
    context: Context,
    onDismissRequest: () -> Unit,
    onLanguageSelected: (LinuxLocaleInfo) -> Unit
) {
    val supportedLocales = remember { LinuxLocaleManager.getSupportedLocales(context) }
    var searchQuery by remember { mutableStateOf("") }
    val currentSavedTag = remember { LinuxLocaleManager.getSavedLocale(context) }

    val filtered = remember(searchQuery, supportedLocales) {
        if (searchQuery.isBlank()) supportedLocales
        else {
            val q = searchQuery.trim().lowercase()
            supportedLocales.filter {
                it.displayName.lowercase().contains(q) ||
                it.nativeName.lowercase().contains(q) ||
                it.englishName.lowercase().contains(q) ||
                it.tag.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = { Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(
                text = stringResource(R.string.pref_linux_language_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.pref_language_footnote),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.pref_language_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.tag }) { item ->
                        val isSelected = currentSavedTag.equals(item.tag, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onLanguageSelected(item) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.nativeName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!item.isSystemDefault && !item.nativeName.equals(item.englishName, ignoreCase = true)) {
                                        Text(
                                            text = "${item.englishName} (${item.tag})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
