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
