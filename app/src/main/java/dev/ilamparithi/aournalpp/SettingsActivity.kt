package dev.ilamparithi.aournalpp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DisplaySettings
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
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
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
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.runtime.ConfigFileType
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager
import dev.ilamparithi.aournalpp.runtime.WallpaperHelper
import dev.ilamparithi.aournalpp.runtime.XournalConfigManager
import dev.ilamparithi.aournalpp.ui.ConfigViewerDialog
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme
import dev.ilamparithi.aournalpp.utils.NoteOpenAction
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

enum class SettingsSubpage {
    MAIN,
    KEYBOARD,
    INPUT,
    LENOVO_PEN,
    DISPLAY
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
fun SettingsScreen(onBack: (() -> Unit)? = null) {
    SettingsNavigationHost(onFinish = { onBack?.invoke() })
}

@Composable
fun SettingsNavigationHost(onFinish: () -> Unit) {
    var currentSubpage by rememberSaveable { mutableStateOf(SettingsSubpage.MAIN) }

    BackHandler(enabled = true) {
        when (currentSubpage) {
            SettingsSubpage.MAIN -> onFinish()
            SettingsSubpage.LENOVO_PEN -> currentSubpage = SettingsSubpage.INPUT
            else -> currentSubpage = SettingsSubpage.MAIN
        }
    }

    AnimatedContent(
        targetState = currentSubpage,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
            } else {
                slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
            }
        },
        label = "SettingsSubpageTransition"
    ) { targetPage ->
        when (targetPage) {
            SettingsSubpage.MAIN -> MainSettingsScreen(
                onNavigate = { currentSubpage = it },
                onBack = onFinish
            )
            SettingsSubpage.KEYBOARD -> KeyboardSettingsScreen(
                onBack = { currentSubpage = SettingsSubpage.MAIN }
            )
            SettingsSubpage.INPUT -> InputSettingsScreen(
                onNavigateToLenovoPen = { currentSubpage = SettingsSubpage.LENOVO_PEN },
                onBack = { currentSubpage = SettingsSubpage.MAIN }
            )
            SettingsSubpage.LENOVO_PEN -> LenovoPenSettingsScreen(
                onBack = { currentSubpage = SettingsSubpage.INPUT }
            )
            SettingsSubpage.DISPLAY -> DisplaySettingsScreen(
                onBack = { currentSubpage = SettingsSubpage.MAIN }
            )
        }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
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

                    ListItem(
                        headlineContent = {
                            Text("Show Hidden & Backup Files", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            Text("Display hidden files and backup copies in the Document Hub.", style = MaterialTheme.typography.bodySmall)
                        },
                        trailingContent = {
                            Switch(
                                checked = showHiddenFilesPref,
                                onCheckedChange = {
                                    showHiddenFilesPref = it
                                    prefs.edit().putBoolean("pref_show_hidden_files", it).apply()
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    var intelligentRecoveryPref by remember {
                        mutableStateOf(prefs.getBoolean("pref_intelligent_emergency_recovery", true))
                    }

                    ListItem(
                        headlineContent = {
                            Text("Intelligent Session Recovery", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            Text("Detect crashed/unsaved note sessions and offer restoration.", style = MaterialTheme.typography.bodySmall)
                        },
                        trailingContent = {
                            Switch(
                                checked = intelligentRecoveryPref,
                                onCheckedChange = {
                                    intelligentRecoveryPref = it
                                    prefs.edit().putBoolean("pref_intelligent_emergency_recovery", it).apply()
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                }
            }

            // 3. Xournal++ Preferences & Configuration Backup
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
                    ListItem(
                        headlineContent = { Text("Auto-toggle Keyboard on Focus", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Automatically open the soft keyboard when tapping into text boxes or canvas annotations.") },
                        trailingContent = {
                            Switch(
                                checked = autoShowIme,
                                onCheckedChange = {
                                    autoShowIme = it
                                    prefs.edit().putBoolean("pref_auto_show_ime_on_focus", it).apply()
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Enforce Character-Based Input", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Directly dispatch committed Unicode characters rather than synthesized hardware key scancodes.") },
                        trailingContent = {
                            Switch(
                                checked = enforceCharBasedInput,
                                onCheckedChange = {
                                    enforceCharBasedInput = it
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT, it).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT)
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Triple-Back Emergency Force Close", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Pressing Back 3 times rapidly inside Canvas brings up a force-close dialog if X11 becomes unresponsive.") },
                        trailingContent = {
                            Switch(
                                checked = tripleBackForceClose,
                                onCheckedChange = {
                                    tripleBackForceClose = it
                                    prefs.edit().putBoolean("pref_triple_back_force_close", it).apply()
                                }
                            )
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
                title = { Text("Stylus & Touch Input", fontWeight = FontWeight.Bold) },
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

            // Stylus Controls
            Text("Stylus & Pointer Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ListItem(
                        headlineContent = { Text("Show Stylus Click Mode in Top Bar", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Displays Left / Middle / Right click toggle chip directly in the Canvas floating header.") },
                        trailingContent = {
                            Switch(
                                checked = showStylusClickOverride,
                                onCheckedChange = {
                                    showStylusClickOverride = it
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, it).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE)
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Enable Stylus Mouse Mode", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Treat hardware stylus touch events as desktop mouse pointer clicks.") },
                        trailingContent = {
                            Switch(
                                checked = stylusIsMouse,
                                onCheckedChange = {
                                    stylusIsMouse = it
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_STYLUS_IS_MOUSE, it).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_STYLUS_IS_MOUSE)
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Stylus Button Contact Modifier Mode", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Modify contact properties when the stylus side barrel button is depressed.") },
                        trailingContent = {
                            Switch(
                                checked = stylusButtonContactModifier,
                                onCheckedChange = {
                                    stylusButtonContactModifier = it
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_STYLUS_BUTTON_CONTACT_MODIFIER, it).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_STYLUS_BUTTON_CONTACT_MODIFIER)
                                }
                            )
                        }
                    )

                    if (touchMode == "1") {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Show Mouse Click Helper Overlay", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("On-screen Left / Middle / Right floating mouse buttons for trackpad mode.") },
                            trailingContent = {
                                Switch(
                                    checked = showMouseHelper,
                                    onCheckedChange = {
                                        showMouseHelper = it
                                        x11Prefs.edit().putBoolean(X11Preferences.KEY_SHOW_MOUSE_HELPER, it).apply()
                                        X11Preferences.notifyChanged(context, X11Preferences.KEY_SHOW_MOUSE_HELPER)
                                    }
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Scale Trackpad to Display Factor", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Scale cursor movement speed according to display resolution.") },
                            trailingContent = {
                                Switch(
                                    checked = scaleTouchpad,
                                    onCheckedChange = {
                                        scaleTouchpad = it
                                        x11Prefs.edit().putBoolean(X11Preferences.KEY_SCALE_TOUCHPAD, it).apply()
                                        X11Preferences.notifyChanged(context, X11Preferences.KEY_SCALE_TOUCHPAD)
                                    }
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Enable Tap-to-Move", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Tap and drag to move pointer without holding physical clicks.") },
                            trailingContent = {
                                Switch(
                                    checked = tapToMove,
                                    onCheckedChange = {
                                        tapToMove = it
                                        x11Prefs.edit().putBoolean(X11Preferences.KEY_TAP_TO_MOVE, it).apply()
                                        X11Preferences.notifyChanged(context, X11Preferences.KEY_TAP_TO_MOVE)
                                    }
                                )
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Ignore Gamepad Events", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Suppress controller joystick and button input events from driving pointer.") },
                        trailingContent = {
                            Switch(
                                checked = ignoreGamepad,
                                onCheckedChange = {
                                    ignoreGamepad = it
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_IGNORE_GAMEPAD_EVENTS, it).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_IGNORE_GAMEPAD_EVENTS)
                                }
                            )
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
                    ListItem(
                        headlineContent = { Text("Show Detection Toasts", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Display transient toast messages when barrel button gestures are detected.") },
                        trailingContent = {
                            Switch(
                                checked = showDetections,
                                onCheckedChange = {
                                    showDetections = it
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_LENOVO_PEN_SHOW_DETECTIONS, it).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_LENOVO_PEN_SHOW_DETECTIONS)
                                }
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Show Toggle Debug Toasts", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Display state toasts when toggle mode button states change.") },
                        trailingContent = {
                            Switch(
                                checked = showToggleDebug,
                                onCheckedChange = {
                                    showToggleDebug = it
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS, it).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS)
                                }
                            )
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

                        ListItem(
                            headlineContent = { Text("Toggle Target Button", fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Text(
                                    if (toggleVal)
                                        "On: toggle mode — gesture once for down and again for up."
                                    else
                                        "Off: momentary mode — press and hold target for specified duration."
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = toggleVal,
                                    enabled = isActionEnabled,
                                    onCheckedChange = {
                                        toggleVal = it
                                        x11Prefs.edit().putBoolean(toggleKey, it).apply()
                                        X11Preferences.notifyChanged(context, toggleKey)
                                    }
                                )
                            },
                            modifier = Modifier.alpha(if (isActionEnabled) 1f else 0.38f),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text("Toggle OFF on Lift", fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Text("When toggled ON and pen lifts after contact, release the mapped button automatically.")
                            },
                            trailingContent = {
                                Switch(
                                    checked = offOnLiftVal,
                                    enabled = isOffOnLiftEnabled,
                                    onCheckedChange = {
                                        offOnLiftVal = it
                                        x11Prefs.edit().putBoolean(offOnLiftKey, it).apply()
                                        X11Preferences.notifyChanged(context, offOnLiftKey)
                                    }
                                )
                            },
                            modifier = Modifier.alpha(if (isOffOnLiftEnabled) 1f else 0.38f),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
fun DisplaySettingsScreen(onBack: () -> Unit) {
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
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${preset}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                    ListItem(
                        headlineContent = { Text("Adjust Resolution for Orientation", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Automatically swap width and height when device orientation rotates.") },
                        trailingContent = {
                            Switch(
                                checked = adjustRes,
                                onCheckedChange = {
                                    adjustRes = it
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_ADJUST_RESOLUTION, it).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_ADJUST_RESOLUTION)
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Stretch to Fit Display", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Scale canvas image non-proportionally to eliminate black letterbox bars.") },
                        trailingContent = {
                            Switch(
                                checked = displayStretch,
                                onCheckedChange = {
                                    displayStretch = it
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_DISPLAY_STRETCH, it).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_STRETCH)
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Reseed Screen with Soft Keyboard", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Dynamically adjust X11 screen dimensions when on-screen keyboard appears.") },
                        trailingContent = {
                            Switch(
                                checked = reseedIme,
                                onCheckedChange = {
                                    reseedIme = it
                                    x11Prefs.edit().putBoolean(X11Preferences.KEY_RESEED, it).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_RESEED)
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Fullscreen Canvas", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Hide status and navigation bars. Disabling allows Android's floating rotation button to appear.") },
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
