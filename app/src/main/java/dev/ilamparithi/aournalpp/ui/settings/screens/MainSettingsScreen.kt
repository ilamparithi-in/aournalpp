package dev.ilamparithi.aournalpp.ui.settings.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.LicensesActivity
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.runtime.ConfigFileType
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.LinuxLocaleManager
import dev.ilamparithi.aournalpp.runtime.WallpaperHelper
import dev.ilamparithi.aournalpp.runtime.XournalConfigManager
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.ConfigViewerDialog
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.ui.settings.SettingsSubpage
import dev.ilamparithi.aournalpp.ui.settings.components.FileNameTemplateSettingsCard
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.ui.settings.dialogs.AppLanguagePickerDialog
import dev.ilamparithi.aournalpp.ui.settings.dialogs.XournalppLanguagePickerDialog
import dev.ilamparithi.aournalpp.utils.AppLocaleHelper
import dev.ilamparithi.aournalpp.utils.NoteOpenAction
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    onNavigate: (SettingsSubpage) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = remember { AppPreferences.getGeneralPrefs(context) }
    val env = remember { LinuxEnvironment(context) }
    val configManager = remember { XournalConfigManager(env) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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

            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        currentLinuxLocaleTag = LinuxLocaleManager.getSavedLocale(context)
                        appLanguageDisplayName = AppLocaleHelper.getCurrentAppLanguageDisplayName()
                    }
                }
                val lifecycle = (context as? LifecycleOwner)?.lifecycle
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
                            val count = result.getOrNull() ?: 0
                            snackbarHostState.showSnackbar(
                                context.resources.getQuantityString(
                                    R.plurals.msg_backup_exported_configs,
                                    count,
                                    count
                                )
                            )
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
                                snackbarHostState.showSnackbar(
                                    context.resources.getQuantityString(
                                        R.plurals.msg_backup_restored_configs,
                                        count,
                                        count
                                    )
                                )
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
                            ConfigFileType.entries.forEach { type ->
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
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
