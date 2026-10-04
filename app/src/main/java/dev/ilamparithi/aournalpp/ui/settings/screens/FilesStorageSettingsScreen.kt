package dev.ilamparithi.aournalpp.ui.settings.screens

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsIconBadge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonGroup
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.ui.settings.components.FileNameTemplateTarget
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.utils.FileNameTemplateEngine
import dev.ilamparithi.aournalpp.utils.NoteOpenAction
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import kotlinx.coroutines.launch
import java.io.File

/**
 * Redesigned Section 1: Files & Storage settings screen.
 * Implements M3 Expressive Inset Group containers, clear Documents folder hierarchy,
 * segmented note actions, and an intuitive file name template builder.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilesStorageSettingsScreen(
    showTopBar: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = remember { AppPreferences.getGeneralPrefs(context) }
    val env = remember { LinuxEnvironment(context) }

    data class PendingFolderChange(val label: String, val path: String)

    var currentNotesDir by remember { mutableStateOf(env.getNotesDirectory().absolutePath) }
    var showCustomPathDialog by remember { mutableStateOf(false) }
    var customPathInput by remember { mutableStateOf(currentNotesDir) }
    var pendingFolderChange by remember { mutableStateOf<PendingFolderChange?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val rawPath = uri.path ?: ""
            val resolved = if (rawPath.contains("primary:")) {
                val rel = rawPath.substringAfter("primary:").trim('/')
                File(Environment.getExternalStorageDirectory(), rel).absolutePath
            } else {
                rawPath
            }
            if (resolved != currentNotesDir) {
                val label = if (resolved.contains("Documents/")) "Documents / " + resolved.substringAfter("Documents/") else resolved.substringAfterLast('/')
                pendingFolderChange = PendingFolderChange(label, resolved)
            }
        }
    }

    if (showCustomPathDialog) {
        AlertDialog(
            onDismissRequest = { showCustomPathDialog = false },
            properties = AppDialogDefaults.Properties,
            modifier = Modifier.promptWidth(),
            title = {
                Text(
                    text = stringResource(R.string.dialog_custom_folder_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.dialog_custom_folder_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customPathInput,
                        onValueChange = { customPathInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.pref_directory_path)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (customPathInput.isNotBlank()) {
                                val trimmed = customPathInput.trim()
                                if (trimmed != currentNotesDir) {
                                    val label = if (trimmed.contains("Documents/")) "Documents / " + trimmed.substringAfter("Documents/") else trimmed.substringAfterLast('/')
                                    pendingFolderChange = PendingFolderChange(label, trimmed)
                                }
                                showCustomPathDialog = false
                            }
                        })
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customPathInput.isNotBlank()) {
                            val trimmed = customPathInput.trim()
                            if (trimmed != currentNotesDir) {
                                val label = if (trimmed.contains("Documents/")) "Documents / " + trimmed.substringAfter("Documents/") else trimmed.substringAfterLast('/')
                                pendingFolderChange = PendingFolderChange(label, trimmed)
                            }
                            showCustomPathDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPathDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    pendingFolderChange?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingFolderChange = null },
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
                    text = stringResource(R.string.dialog_switch_dir_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.dialog_switch_dir_question),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = pending.path,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(R.string.dialog_switch_dir_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val path = pending.path
                        env.setNotesDirectory(path)
                        currentNotesDir = path
                        val label = pending.label
                        pendingFolderChange = null
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_notes_folder_set, label))
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_switch_directory))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFolderChange = null }) {
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
                            text = stringResource(R.string.pref_cat_files_storage),
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
            // 1. Storage Location & Recommended Presets Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_storage_location_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Active Folder Display Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(
                            imageVector = Icons.Default.Folder,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_notes_storage_folder),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentNotesDir,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Documents Folder Presets using Connected Button Group
                    Text(
                        text = stringResource(R.string.pref_folder_presets_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val documentBase = Environment.getExternalStorageDirectory()
                    val notesPreset = File(documentBase, "Documents/Notes").absolutePath
                    val xoppPreset = File(documentBase, "Documents/Xournal++").absolutePath
                    val notebooksPreset = File(documentBase, "Documents/Notebooks").absolutePath

                    val presetButtons = listOf(
                        ConnectedButtonItem(notesPreset, stringResource(R.string.pref_preset_notes)),
                        ConnectedButtonItem(xoppPreset, stringResource(R.string.pref_preset_xournal)),
                        ConnectedButtonItem(notebooksPreset, stringResource(R.string.pref_preset_notebooks))
                    )

                    ConnectedButtonGroup(
                        items = presetButtons,
                        selectedItem = currentNotesDir,
                        onItemSelected = { path ->
                            if (path != currentNotesDir) {
                                val label = when (path) {
                                    notesPreset -> "Documents / Notes"
                                    xoppPreset -> "Documents / Xournal++"
                                    notebooksPreset -> "Documents / Notebooks"
                                    else -> path.substringAfterLast('/')
                                }
                                pendingFolderChange = PendingFolderChange(label, path)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_browse_system),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                customPathInput = currentNotesDir
                                showCustomPathDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_custom_path),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 2. Default Open Action Group (XOPP vs PDF)
            // =================================================================
            // =================================================================
            // 2. Default Open Action Group (XOPP vs PDF)
            // =================================================================
            Text(
                text = stringResource(R.string.pref_default_note_action_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            var xoppActionPref by remember {
                mutableStateOf(NoteOpenManager.getDefaultAction(context, isPdf = false).value)
            }
            var pdfActionPref by remember {
                mutableStateOf(NoteOpenManager.getDefaultAction(context, isPdf = true).value)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Notebook Files
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.pref_notebook_files_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val notebookButtons = listOf(
                            ConnectedButtonItem(NoteOpenAction.ASK.value, stringResource(R.string.pref_action_ask)),
                            ConnectedButtonItem(NoteOpenAction.EDIT.value, stringResource(R.string.pref_action_edit_canvas)),
                            ConnectedButtonItem(NoteOpenAction.VIEW.value, stringResource(R.string.pref_action_view_pdf))
                        )
                        ConnectedButtonGroup(
                            items = notebookButtons,
                            selectedItem = xoppActionPref,
                            onItemSelected = { selected ->
                                xoppActionPref = selected
                                val action = NoteOpenAction.entries.firstOrNull { it.value == selected } ?: NoteOpenAction.ASK
                                NoteOpenManager.setDefaultAction(context, isPdf = false, action)
                            }
                        )

                        Text(
                            text = when (xoppActionPref) {
                                NoteOpenAction.EDIT.value -> stringResource(R.string.pref_notebook_action_edit_desc)
                                NoteOpenAction.VIEW.value -> stringResource(R.string.pref_notebook_action_view_desc)
                                else -> stringResource(R.string.pref_notebook_action_ask_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // 2. PDF Documents
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.pref_pdf_documents_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val pdfButtons = listOf(
                            ConnectedButtonItem(NoteOpenAction.ASK.value, stringResource(R.string.pref_action_ask)),
                            ConnectedButtonItem(NoteOpenAction.EDIT.value, stringResource(R.string.pref_action_annotate)),
                            ConnectedButtonItem(NoteOpenAction.VIEW.value, stringResource(R.string.pref_action_view_system))
                        )
                        ConnectedButtonGroup(
                            items = pdfButtons,
                            selectedItem = pdfActionPref,
                            onItemSelected = { selected ->
                                pdfActionPref = selected
                                val action = NoteOpenAction.entries.firstOrNull { it.value == selected } ?: NoteOpenAction.ASK
                                NoteOpenManager.setDefaultAction(context, isPdf = true, action)
                            }
                        )

                        Text(
                            text = when (pdfActionPref) {
                                NoteOpenAction.EDIT.value -> stringResource(R.string.pref_pdf_action_annotate_desc)
                                NoteOpenAction.VIEW.value -> stringResource(R.string.pref_pdf_action_view_desc)
                                else -> stringResource(R.string.pref_pdf_action_ask_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // =================================================================
            // 3. File Name Templates Group (Clean, No-Russian-Doll UI)
            // =================================================================
            Text(
                text = stringResource(R.string.pref_filename_templates_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            FileNameTemplateSection(
                prefs = prefs,
                context = context,
                snackbarHostState = snackbarHostState
            )

            // =================================================================
            // 4. File Management & Recovery Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_file_management_recovery_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    var showHiddenFilesPref by remember {
                        mutableStateOf(prefs.getBoolean("pref_show_hidden_files", false))
                    }
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_show_hidden_files_title),
                        supporting = stringResource(R.string.pref_show_hidden_files_desc),
                        checked = showHiddenFilesPref,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        onCheckedChange = {
                            showHiddenFilesPref = it
                            prefs.edit().putBoolean("pref_show_hidden_files", it).apply()
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    var intelligentRecoveryPref by remember {
                        mutableStateOf(prefs.getBoolean("pref_intelligent_emergency_recovery", true))
                    }
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_intelligent_recovery_title),
                        supporting = stringResource(R.string.pref_intelligent_recovery_desc),
                        checked = intelligentRecoveryPref,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        onCheckedChange = {
                            intelligentRecoveryPref = it
                            prefs.edit().putBoolean("pref_intelligent_emergency_recovery", it).apply()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun getTemplateTargetLabel(target: FileNameTemplateTarget): String {
    return when (target) {
        FileNameTemplateTarget.NEW_FILE -> stringResource(R.string.template_target_new_note)
        FileNameTemplateTarget.SAVE_AS -> stringResource(R.string.template_target_save_as)
        FileNameTemplateTarget.EXPORT_PDF -> stringResource(R.string.template_target_export_pdf)
        FileNameTemplateTarget.SHARE_PDF -> stringResource(R.string.template_target_share_pdf)
        FileNameTemplateTarget.SHARE_XOPP -> stringResource(R.string.template_target_share_xopp)
    }
}

/**
 * File Name Template interactive builder section featuring full-width connected target selector,
 * dynamic live preview card with sample context, high-contrast editable template field,
 * cursor-aware token insertion palette, and an explicit reset button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FileNameTemplateSection(
    prefs: android.content.SharedPreferences,
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    var selectedTarget by remember { mutableStateOf(FileNameTemplateTarget.NEW_FILE) }

    val initialTemplate = remember(selectedTarget) {
        prefs.getString(selectedTarget.key, selectedTarget.defaultTemplate) ?: selectedTarget.defaultTemplate
    }

    var textFieldValue by remember(selectedTarget) {
        mutableStateOf(TextFieldValue(initialTemplate, selection = TextRange(initialTemplate.length)))
    }

    val dummySampleFile = remember {
        File(
            File(File(context.filesDir, "sample_root"), "Physics_101"),
            "Mechanics_Lecture.xopp"
        )
    }

    val invalidSyntaxMessage = stringResource(R.string.template_invalid_syntax)
    val livePreviewResult = remember(textFieldValue.text, selectedTarget, invalidSyntaxMessage) {
        try {
            FileNameTemplateEngine.evaluate(textFieldValue.text, context, dummySampleFile)
        } catch (_: Exception) {
            invalidSyntaxMessage
        }
    }

    val extensionSuffix = when (selectedTarget) {
        FileNameTemplateTarget.EXPORT_PDF, FileNameTemplateTarget.SHARE_PDF -> ".pdf"
        else -> ".xopp"
    }

    val resetPatternDesc = stringResource(R.string.action_reset_pattern_desc)
    val msgPatternResetDefault = stringResource(R.string.msg_pattern_reset_default)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.pref_filename_templates_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val targetButtons = listOf(
                ConnectedButtonItem(FileNameTemplateTarget.NEW_FILE, getTemplateTargetLabel(FileNameTemplateTarget.NEW_FILE)),
                ConnectedButtonItem(FileNameTemplateTarget.SAVE_AS, getTemplateTargetLabel(FileNameTemplateTarget.SAVE_AS)),
                ConnectedButtonItem(FileNameTemplateTarget.EXPORT_PDF, getTemplateTargetLabel(FileNameTemplateTarget.EXPORT_PDF)),
                ConnectedButtonItem(FileNameTemplateTarget.SHARE_PDF, getTemplateTargetLabel(FileNameTemplateTarget.SHARE_PDF)),
                ConnectedButtonItem(FileNameTemplateTarget.SHARE_XOPP, getTemplateTargetLabel(FileNameTemplateTarget.SHARE_XOPP))
            )
            ConnectedButtonGroup(
                items = targetButtons,
                selectedItem = selectedTarget,
                modifier = Modifier.fillMaxWidth(),
                isScrollable = false,
                showCheckmark = false,
                onItemSelected = { target ->
                    selectedTarget = target
                    val t = prefs.getString(target.key, target.defaultTemplate) ?: target.defaultTemplate
                    textFieldValue = TextFieldValue(t, selection = TextRange(t.length))
                }
            )

            // Live Preview Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.template_live_preview_badge),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = getTemplateTargetLabel(selectedTarget),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "$livePreviewResult$extensionSuffix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.template_live_preview_sample_context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Template Editor TextField with explicit Reset Button
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    prefs.edit().putString(selectedTarget.key, it.text).apply()
                },
                label = { Text(stringResource(R.string.template_pattern_field_label, getTemplateTargetLabel(selectedTarget))) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val def = selectedTarget.defaultTemplate
                            textFieldValue = TextFieldValue(def, selection = TextRange(def.length))
                            prefs.edit().putString(selectedTarget.key, def).apply()
                            Toast.makeText(context, msgPatternResetDefault, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = resetPatternDesc,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Categorized Placeholder Tokens Palette
            Text(
                text = stringResource(R.string.template_insert_tokens_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val tokenGroups = listOf(
                stringResource(R.string.template_token_group_datetime) to listOf("{date}", "{time}", "{datetime}", "{year}", "{month}", "{day}"),
                stringResource(R.string.template_token_group_context) to listOf("{name}", "{folder}", "{ext}"),
                stringResource(R.string.template_token_group_metadata) to listOf("{created}", "{modified}", "{random}")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tokenGroups.forEach { (groupTitle, tokens) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = groupTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tokens.forEach { token ->
                                SuggestionChip(
                                    onClick = {
                                        val currentText = textFieldValue.text
                                        val selStart = textFieldValue.selection.start.coerceIn(0, currentText.length)
                                        val selEnd = textFieldValue.selection.end.coerceIn(0, currentText.length)
                                        val newText = currentText.substring(0, selStart) + token + currentText.substring(selEnd)
                                        val newCursor = selStart + token.length
                                        textFieldValue = TextFieldValue(newText, selection = TextRange(newCursor))
                                        prefs.edit().putString(selectedTarget.key, newText).apply()
                                    },
                                    label = {
                                        Text(
                                            text = token,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
