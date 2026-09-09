package dev.ilamparithi.aournalpp.ui.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.ui.SaveAsNoteDialog
import java.io.File

/**
 * Shared dialog for prompting the user to name and select destination folder for a recovered emergency save note.
 */
@Composable
fun EmergencySaveNameDialog(
    file: File,
    initialName: String,
    initialFolder: File,
    repository: DocumentRepository,
    onDismiss: () -> Unit,
    onSaveSuccess: (File) -> Unit,
    onFolderCreated: () -> Unit
) {
    val allAvailableFolders by produceState<List<FolderItem>>(emptyList()) {
        value = repository.getAllFolders()
    }

    SaveAsNoteDialog(
        title = stringResource(R.string.dialog_save_recovered_title),
        subtitle = stringResource(R.string.dialog_save_recovered_subtitle),
        icon = Icons.Default.Emergency,
        initialName = initialName,
        initialFolder = initialFolder,
        availableFolders = allAvailableFolders,
        rootFolder = repository.getRootNotesDirectory(),
        onDismiss = onDismiss,
        onSave = { name, targetFolder ->
            val savedFile = repository.saveEmergencyRecoveryToNotes(
                file,
                name,
                targetFolder
            )
            onSaveSuccess(savedFile)
        },
        onCreateFolder = { name, colorHex, iconEmoji, iconType ->
            val result = repository.createFolder(
                parentDir = repository.getRootNotesDirectory(),
                name = name,
                colorHex = colorHex,
                iconEmoji = iconEmoji,
                iconType = iconType
            )
            if (result.isSuccess) {
                onFolderCreated()
            }
            result
        }
    )
}
