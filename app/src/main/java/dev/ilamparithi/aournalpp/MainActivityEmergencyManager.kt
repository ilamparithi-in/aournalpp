package dev.ilamparithi.aournalpp

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.utils.NoteOpenAction
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import kotlinx.coroutines.CoroutineScope
import java.io.File

/**
 * Manages detection, quarantine, recovery dialogs, and deferred note open queue
 * for unsaved Xournal++ emergency saves following unexpected app closures.
 */
class MainActivityEmergencyManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPromptFileOpen: (file: File, isImport: Boolean) -> Unit,
    private val onReplayExternalIntent: (intent: Intent) -> Unit
) {
    sealed class DeferredNoteOpen {
        data class NoteFile(val file: File, val isImport: Boolean = false) : DeferredNoteOpen()
        data class ExternalIntent(val intent: Intent) : DeferredNoteOpen()
    }

    val quarantinedEmergencySave = mutableStateOf<File?>(null)
    val showEmergencyRecoveryDialog = mutableStateOf(false)
    val showEmergencySaveNameDialog = mutableStateOf(false)
    var emergencySaveNameInput by mutableStateOf("")
    var emergencySaveTargetFolder by mutableStateOf<File?>(null)
    private val pendingDeferredNoteOpen = mutableStateOf<DeferredNoteOpen?>(null)
    var isRecoveredSessionRunning = false

    fun checkEmergencySave() {
        val env = LinuxEnvironment(context)
        val file = env.checkAndQuarantineEmergencySave()
        if (file != null && file.exists() && file.length() > 0) {
            quarantinedEmergencySave.value = file
        } else {
            quarantinedEmergencySave.value = null
            showEmergencyRecoveryDialog.value = false
            showEmergencySaveNameDialog.value = false
        }
    }

    fun hasPendingEmergencySave(): Boolean {
        checkEmergencySave()
        return quarantinedEmergencySave.value != null
    }

    fun deferNoteOpenForEmergencySave(file: File, isImport: Boolean = false) {
        pendingDeferredNoteOpen.value = DeferredNoteOpen.NoteFile(file, isImport)
        showEmergencyRecoveryDialog.value = true
    }

    fun deferExternalIntentForEmergencySave(intent: Intent) {
        pendingDeferredNoteOpen.value = DeferredNoteOpen.ExternalIntent(intent)
        showEmergencyRecoveryDialog.value = true
    }

    fun onEmergencySaveResolved() {
        quarantinedEmergencySave.value = null
        showEmergencyRecoveryDialog.value = false
        showEmergencySaveNameDialog.value = false
        replayDeferredNoteOpen()
    }

    fun onResume() {
        checkEmergencySave()
        if (isRecoveredSessionRunning) {
            if (!ActiveSessionTracker.isSessionActive(context)) {
                isRecoveredSessionRunning = false
                replayDeferredNoteOpen()
            }
        }
    }

    fun replayDeferredNoteOpen() {
        val pending = pendingDeferredNoteOpen.value ?: return
        pendingDeferredNoteOpen.value = null
        when (pending) {
            is DeferredNoteOpen.NoteFile -> {
                val action = NoteOpenManager.getDefaultAction(context, pending.file)
                if (action == NoteOpenAction.ASK || pending.isImport) {
                    onPromptFileOpen(pending.file, pending.isImport)
                } else if (action == NoteOpenAction.VIEW) {
                    val env = LinuxEnvironment(context)
                    val supervisor = ProcessSupervisor(env)
                    val pdfExportManager = PdfExportManager(env, supervisor)
                    NoteOpenManager.openAsPdf(
                        context = context,
                        file = pending.file,
                        pdfExportManager = pdfExportManager,
                        scope = scope,
                        repository = DocumentRepository.getInstance(context)
                    )
                } else {
                    NoteOpenManager.openInCanvas(
                        context = context,
                        file = pending.file,
                        repository = DocumentRepository.getInstance(context)
                    )
                }
            }
            is DeferredNoteOpen.ExternalIntent -> {
                onReplayExternalIntent(pending.intent)
            }
        }
    }
}
