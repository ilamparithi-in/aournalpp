package dev.ilamparithi.aournalpp.ui.settings.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.runtime.ConfigFileType
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.LinuxLocaleManager
import dev.ilamparithi.aournalpp.runtime.XournalConfigManager
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.ConfigViewerDialog
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.ui.settings.dialogs.AppLanguagePickerDialog
import dev.ilamparithi.aournalpp.ui.settings.dialogs.XournalppLanguagePickerDialog
import dev.ilamparithi.aournalpp.utils.AppLocaleHelper
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import kotlinx.coroutines.launch

/**
 * Section 6: System & Maintenance Settings Screen.
 *
 * Implements Material 3 Expressive guidelines with unified card-row hierarchy,
 * WCAG-compliant accessible contrast for all icon badges, and consistent typography.
 *
 * 1. Language & Localization (Android app language & Linux editor locale)
 * 2. Configuration Backup & Restore (Full ZIP backup, XML import, component export)
 * 3. Advanced Diagnostic Tools (settings.xml inspector, Native GTK Preferences, session safeguard)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMaintenanceSettingsScreen(
    showTopBar: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val env = remember { LinuxEnvironment(context) }
    val configManager = remember { XournalConfigManager(env) }

    // 1. Language & Localization State
    var showAppLanguageDialog by remember { mutableStateOf(false) }
    var showLinuxLanguageDialog by remember { mutableStateOf(false) }
    var currentLinuxLocaleTag by remember { mutableStateOf(LinuxLocaleManager.getSavedLocale(context)) }
    var appLanguageDisplayName by remember { mutableStateOf(AppLocaleHelper.getCurrentAppLanguageDisplayName()) }

    // 2. Configuration Backup & Diagnostics State
    var showConfigViewerDialog by remember { mutableStateOf(false) }
    var showAdvancedExportDialog by remember { mutableStateOf(false) }
    var exportTargetType by remember { mutableStateOf(ConfigFileType.SETTINGS_XML) }

    // Lifecycle observers: refresh locales & check autoload override
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentLinuxLocaleTag = LinuxLocaleManager.getSavedLocale(context)
                appLanguageDisplayName = AppLocaleHelper.getCurrentAppLanguageDisplayName()
                if (env.checkAndOverrideAutoloadPreference() || env.hasPendingAutoloadOverrideNotification()) {
                    env.clearPendingAutoloadOverrideNotification()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.msg_autoload_override_cleared)
                        )
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Launchers for Backup & Export
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

    // Dialog: App Language Picker
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

    // Dialog: Xournal++ Linux Locale Picker
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

    // Dialog: Live Configuration Inspector
    if (showConfigViewerDialog) {
        ConfigViewerDialog(
            configManager = configManager,
            onDismiss = { showConfigViewerDialog = false }
        )
    }

    // Dialog: Granular Export of Specific Config File
    if (showAdvancedExportDialog) {
        AlertDialog(
            onDismissRequest = { showAdvancedExportDialog = false },
            properties = AppDialogDefaults.Properties,
            modifier = Modifier.promptWidth(),
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.pref_dialog_export_specific_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.pref_dialog_export_specific_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ConfigFileType.entries.forEach { type ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    exportTargetType = type
                                    showAdvancedExportDialog = false
                                    exportFileLauncher.launch(type.fileName)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
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
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.pref_cat_system_maintenance),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.a11yHeading()
                        )
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack, modifier = Modifier.minTouchTarget()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // =================================================================
            // 1. Language & Localization Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_cat_language),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Item 1A: Android App Interface Language
                    MaintenanceActionRow(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.pref_app_language_title),
                        description = stringResource(R.string.pref_app_language_desc),
                        badgeText = appLanguageDisplayName,
                        onClick = {
                            (context as? Activity)?.let { act ->
                                AppLocaleHelper.openAppLanguageSettings(act) {
                                    showAppLanguageDialog = true
                                }
                            } ?: run { showAppLanguageDialog = true }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Item 1B: Xournal++ Linux Locale
                    MaintenanceActionRow(
                        icon = Icons.Default.Translate,
                        title = stringResource(R.string.pref_linux_language_title),
                        description = stringResource(R.string.pref_linux_language_desc),
                        badgeText = LinuxLocaleManager.getLocaleDisplayName(context, currentLinuxLocaleTag),
                        onClick = { showLinuxLanguageDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Informational Footnote
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.pref_language_footnote),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // =================================================================
            // 2. Configuration Backup & Restore Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_cat_config_backup),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Action 2A: Full Backup ZIP
                    MaintenanceActionRow(
                        icon = Icons.Default.Archive,
                        title = stringResource(R.string.pref_btn_create_full_backup),
                        description = stringResource(R.string.pref_btn_create_full_backup_desc),
                        onClick = { exportZipLauncher.launch("xournalpp_config_backup.zip") }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Action 2B: Restore from Archive or File
                    MaintenanceActionRow(
                        icon = Icons.Default.FileUpload,
                        title = stringResource(R.string.pref_btn_restore_backup),
                        description = stringResource(R.string.pref_btn_restore_backup_desc),
                        onClick = {
                            importFileOrZipLauncher.launch(
                                arrayOf(
                                    "*/*",
                                    "application/zip",
                                    "text/xml",
                                    "application/x-zip-compressed"
                                )
                            )
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Action 2C: Granular Component Export
                    MaintenanceActionRow(
                        icon = Icons.Default.FileDownload,
                        title = stringResource(R.string.pref_btn_export_specific),
                        description = stringResource(R.string.pref_btn_export_specific_desc),
                        onClick = { showAdvancedExportDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Footnote of included files
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Backups include settings.xml, toolbar.ini, palette.gpl, colornames.ini, and print-settings.ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // =================================================================
            // 3. Advanced Diagnostic Tools Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_cat_advanced_diagnostics),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Tool 3A: Live Configuration Inspector
                    MaintenanceActionRow(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.pref_inspect_settings_title),
                        description = stringResource(R.string.pref_inspect_settings_desc),
                        onClick = { showConfigViewerDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Tool 3B: Native GTK Preferences Launch
                    MaintenanceActionRow(
                        icon = Icons.Default.Tune,
                        title = stringResource(R.string.pref_native_gtk_prefs_title),
                        description = stringResource(R.string.pref_native_gtk_prefs_desc),
                        onClick = {
                            val intent = Intent(context, CanvasActivity::class.java).apply {
                                putExtra(CanvasActivity.EXTRA_OPEN_PREFERENCES, true)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Workspace Autoload Safeguard Footnote
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.pref_autoload_safeguard_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Reusable, high-contrast, perfectly aligned setting row for System & Maintenance.
 */
@Composable
private fun MaintenanceActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (badgeText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}
