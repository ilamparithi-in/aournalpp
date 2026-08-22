package dev.ilamparithi.aournalpp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import dev.ilamparithi.aournalpp.runtime.ConfigFileType
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.XournalConfigManager
import dev.ilamparithi.aournalpp.ui.ConfigViewerDialog
import kotlinx.coroutines.launch
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AournalTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    val env = remember { LinuxEnvironment(context) }
    val configManager = remember { XournalConfigManager(env) }

    var selectedScale by remember {
        mutableStateOf(prefs.getString("pref_ui_scale", "1.0") ?: "1.0")
    }

    val scaleOptions = listOf(
        "1.0" to ("1.0x (100% Native 1:1)" to "Exact physical pixels. Highest sharpness, compact UI."),
        "1.25" to ("1.25x (125% Fractional)" to "Slightly enlarged icons and toolbars."),
        "1.5" to ("1.5x (150% Balanced Scale)" to "Optimal size for high-resolution tablet screens."),
        "1.75" to ("1.75x (175% Large Scale)" to "Generous touch targets and large toolbar icons."),
        "2.0" to ("2.0x (200% Integer HiDPI)" to "Full 2x raster scale for ultra-dense displays.")
    )

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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Category: UI & Display Scaling
            Text(
                text = "Display & Canvas Scaling",
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
                    scaleOptions.forEachIndexed { index, (value, meta) ->
                        val (title, description) = meta
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedScale == value),
                                    onClick = {
                                        selectedScale = value
                                        prefs.edit().putString("pref_ui_scale", value).apply()
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedScale == value),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index < scaleOptions.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(start = 40.dp))
                        }
                    }
                }
            }

            // Category: Storage & Notes Directory
            var currentNotesDir by remember { mutableStateOf(env.getNotesDirectory().absolutePath) }
            var showCustomPathDialog by remember { mutableStateOf(false) }
            var customPathInput by remember { mutableStateOf(currentNotesDir) }

            val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    val rawPath = uri.path ?: ""
                    val resolved = if (rawPath.contains("primary:")) {
                        val rel = rawPath.substringAfter("primary:").trim('/')
                        java.io.File(android.os.Environment.getExternalStorageDirectory(), rel).absolutePath
                    } else {
                        rawPath
                    }
                    env.setNotesDirectory(resolved)
                    currentNotesDir = resolved
                    android.widget.Toast.makeText(context, "Notes folder set to: $resolved", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            if (showCustomPathDialog) {
                androidx.compose.material3.AlertDialog(
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
                                text = "Enter an absolute path on device storage where Xournal++ notes should default and save:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = customPathInput,
                                onValueChange = { customPathInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Directory Path") }
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                if (customPathInput.isNotBlank()) {
                                    val trimmed = customPathInput.trim()
                                    env.setNotesDirectory(trimmed)
                                    currentNotesDir = trimmed
                                    showCustomPathDialog = false
                                    android.widget.Toast.makeText(context, "Notes folder set to: $trimmed", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { showCustomPathDialog = false }
                        ) {
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

                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Browse & Choose Folder...",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Or choose a preset storage directory:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val presets = listOf(
                        "Documents/Notes (Default)" to java.io.File(android.os.Environment.getExternalStorageDirectory(), "Documents/Notes").absolutePath,
                        "Documents/Xournal" to java.io.File(android.os.Environment.getExternalStorageDirectory(), "Documents/Xournal").absolutePath,
                        "Download" to java.io.File(android.os.Environment.getExternalStorageDirectory(), "Download").absolutePath
                    )

                    presets.forEach { (label, path) ->
                        OutlinedButton(
                            onClick = {
                                env.setNotesDirectory(path)
                                currentNotesDir = path
                                android.widget.Toast.makeText(context, "Notes folder set to: $path", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            if (currentNotesDir == path) {
                                Text(text = "Active", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    androidx.compose.material3.TextButton(
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Show Hidden & Backup Files",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Display hidden files and backup copies directly in the Document Hub list.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = showHiddenFilesPref,
                            onCheckedChange = {
                                showHiddenFilesPref = it
                                prefs.edit().putBoolean("pref_show_hidden_files", it).apply()
                            }
                        )
                    }

                    HorizontalDivider()

                    var intelligentRecoveryPref by remember {
                        mutableStateOf(prefs.getBoolean("pref_intelligent_emergency_recovery", true))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Intelligent Session Recovery",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Detect crashed or unsaved note sessions, filter out duplicate saves, and offer native Material 3 restoration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = intelligentRecoveryPref,
                            onCheckedChange = {
                                intelligentRecoveryPref = it
                                prefs.edit().putBoolean("pref_intelligent_emergency_recovery", it).apply()
                            }
                        )
                    }
                }
            }

            // Category: Keyboard & Input Behavior
            var autoShowIme by remember {
                mutableStateOf(prefs.getBoolean("pref_auto_show_ime_on_focus", true))
            }

            Text(
                text = "Keyboard & Input Behavior",
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Auto-toggle Keyboard on Text Focus",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automatically open soft keyboard when tapping into a text box, file name field, or canvas text annotation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = autoShowIme,
                            onCheckedChange = {
                                autoShowIme = it
                                prefs.edit().putBoolean("pref_auto_show_ime_on_focus", it).apply()
                            }
                        )
                    }
                }
            }

            // Category: Xournal++ Preferences & Configuration
            Text(
                text = "Xournal++ Preferences & Configuration",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Card 1: Preferences Editor
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Open Xournal++ Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Launch directly into the native GTK settings dialog to configure toolbars, defaults, and canvas properties.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            val intent = Intent(context, CanvasActivity::class.java).apply {
                                putExtra(CanvasActivity.EXTRA_OPEN_PREFERENCES, true)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Settings",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Card 2: Configuration Backup & Portability
            var showConfigViewerDialog by remember { mutableStateOf(false) }
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

            val importFileLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    val result = configManager.importConfigFile(context, uri)
                    scope.launch {
                        if (result.isSuccess) {
                            val type = result.getOrNull()
                            snackbarHostState.showSnackbar("Successfully imported ${type?.fileName ?: "configuration"}")
                        } else {
                            snackbarHostState.showSnackbar("Import failed: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            }

            val importZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    val result = configManager.importFullBackupZip(context, uri)
                    scope.launch {
                        if (result.isSuccess) {
                            val count = result.getOrNull() ?: 0
                            snackbarHostState.showSnackbar("Successfully restored $count config files from ZIP")
                        } else {
                            snackbarHostState.showSnackbar("ZIP restore failed: ${result.exceptionOrNull()?.message}")
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Backup & Portability",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Inspect, backup, and restore Xournal++ configuration (settings.xml), custom toolbars (toolbar.ini), palettes, or full ZIP archives.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Disclaimer Note
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Config values are loaded directly by Xournal++. Semantic checking is not performed by Android.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // View XML / Toolbars Modal Button
                    OutlinedButton(
                        onClick = { showConfigViewerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View settings.xml",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = "Export & Backup:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                exportTargetType = ConfigFileType.SETTINGS_XML
                                exportFileLauncher.launch("settings.xml")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Configuration", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                exportTargetType = ConfigFileType.TOOLBAR_INI
                                exportFileLauncher.launch("toolbar.ini")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("toolbar.ini", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            exportZipLauncher.launch("xournalpp_config_backup.zip")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Full Backup (.zip)")
                    }

                    HorizontalDivider()

                    Text(
                        text = "Import & Restore:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                importFileLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Configuration", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                importZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore ZIP", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Category: Termux-X11 Engine Preferences
            Text(
                text = "X11 Display Server & Stylus Preferences",
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
                        text = "Configure native display modes, pointer capture, and hardware stylus button gesture mappings directly in the Termux-X11 backend engine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, com.termux.x11.LoriePreferences::class.java).apply {
                                action = Intent.ACTION_MAIN
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Open Termux-X11 Preferences",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Category: About & Version
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
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Source Licenses", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
