package dev.ilamparithi.aournalpp.ui.home

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.AutosaveInfo
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.ActiveNotesTracker
import dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.utils.FileNameTemplateEngine
import dev.ilamparithi.aournalpp.utils.ThumbnailManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * Dialog states for the Home screen to avoid wide-scope recompositions.
 */
sealed interface HomeDialogState {
    data object None : HomeDialogState
    data object CreateFolder : HomeDialogState
    data class NewNote(val defaultName: String, val folders: List<FolderItem>) : HomeDialogState
    data object AutoloadOverride : HomeDialogState
    data class AutosavePrompt(val note: NoteDocument) : HomeDialogState
    data class SaveAutosavePrompt(val note: NoteDocument) : HomeDialogState
    data class NoteAction(val file: File) : HomeDialogState
    data class ActiveSessionPrompt(
        val file: File,
        val match: ActiveNotesTracker.ActiveNoteMatch,
        val pendingNote: NoteDocument? = null
    ) : HomeDialogState
    data class RenameNote(val note: NoteDocument) : HomeDialogState
    data class DeleteNote(val note: NoteDocument) : HomeDialogState
    data class ShareExport(val note: NoteDocument) : HomeDialogState
    data class PdfConverting(val message: String) : HomeDialogState
}

/**
 * Consolidated UI State for HomeScreen.
 */
data class HomeUiState(
    val recentNotes: List<NoteDocument> = emptyList(),
    val continueNote: NoteDocument? = null,
    val totalNotesCount: Int = 0,
    val totalFoldersCount: Int = 0,
    val isRefreshing: Boolean = false,
    val refreshSeed: Long = 0L,
    val viewMode: String = "EXPRESSIVE",
    val isFabExpanded: Boolean = false,
    val dialogState: HomeDialogState = HomeDialogState.None,
    val quarantinedEmergencyFile: File? = null
)

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    val repository: DocumentRepository = DocumentRepository.getInstance(application),
    val env: LinuxEnvironment = repository.getLinuxEnvironment(),
    val supervisor: ProcessSupervisor = ProcessSupervisor(env),
    val pdfExportManager: PdfExportManager = PdfExportManager(env, supervisor),
    val prefs: SharedPreferences = AppPreferences.getGeneral(application),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            recentNotes = repository.getCachedHomeNotes(16) ?: emptyList(),
            continueNote = repository.getCachedContinueNote(),
            totalNotesCount = repository.getCachedTotalNotesCount() ?: 0,
            totalFoldersCount = repository.getCachedTotalFoldersCount() ?: 0,
            viewMode = prefs.getString("pref_home_view_mode", "EXPRESSIVE") ?: "EXPRESSIVE"
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastLoadedTimestamp: Long = 0L
    private var loadJob: Job? = null
    private var initialChecksDone: Boolean = false

    private val sessionClosedReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            ActiveSessionTracker.notifySessionChanged()
            loadJob?.cancel()
            loadJob = viewModelScope.launch {
                delay(500.milliseconds)
                loadHomeDataNow(checkEmergencyAndAutoload = true)
            }
        }
    }

    init {
        val filter = IntentFilter("dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED")
        ContextCompat.registerReceiver(
            getApplication(),
            sessionClosedReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        loadHomeData(force = true, checkEmergencyAndAutoload = true)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(sessionClosedReceiver)
        } catch (_: Exception) {}
    }

    /**
     * Debounced lifecycle resume handler with a 2-second cooldown to eliminate redundant disk scans.
     */
    fun onAppResume(force: Boolean = false) {
        ActiveSessionTracker.notifySessionChanged()
        val now = System.currentTimeMillis()
        if (!force && (now - lastLoadedTimestamp < 2000L)) {
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            delay(200.milliseconds)
            loadHomeDataNow()
        }
    }

    suspend fun loadHomeDataNow(checkEmergencyAndAutoload: Boolean = false) {
        val payload = withContext(ioDispatcher) {
            repository.getHomeData(16)
        }
        lastLoadedTimestamp = System.currentTimeMillis()

        _uiState.update { current ->
            current.copy(
                recentNotes = payload.notes,
                continueNote = payload.continueNote,
                totalNotesCount = payload.totalNotesCount,
                totalFoldersCount = payload.totalFoldersCount,
                isRefreshing = false
            )
        }

        // Prefetch thumbnails on background dispatcher
        ThumbnailManager.prefetchThumbnails(getApplication(), payload.notes, pdfExportManager, viewModelScope)

        // Perform emergency save & autoload checks once or on session closed trigger
        if (checkEmergencyAndAutoload || !initialChecksDone) {
            initialChecksDone = true
            val emergencyFile = withContext(ioDispatcher) { env.checkAndQuarantineEmergencySave() }
            if (emergencyFile != null && emergencyFile.exists() && emergencyFile.length() > 0) {
                _uiState.update { it.copy(quarantinedEmergencyFile = emergencyFile) }
            }

            val autoloadOverridden = withContext(ioDispatcher) { env.checkAndOverrideAutoloadPreference() }
            if (autoloadOverridden || env.hasPendingAutoloadOverrideNotification()) {
                _uiState.update { it.copy(dialogState = HomeDialogState.AutoloadOverride) }
            }
        }
    }

    fun loadHomeData(force: Boolean = false, checkEmergencyAndAutoload: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadHomeDataNow(checkEmergencyAndAutoload = checkEmergencyAndAutoload)
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, refreshSeed = System.currentTimeMillis()) }
        loadHomeData(force = true)
    }

    fun setViewMode(mode: String) {
        prefs.edit().putString("pref_home_view_mode", mode).apply()
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setFabExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isFabExpanded = expanded) }
    }

    fun setDialogState(state: HomeDialogState) {
        _uiState.update { it.copy(dialogState = state) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = HomeDialogState.None) }
    }

    fun dismissAutoloadOverride() {
        env.clearPendingAutoloadOverrideNotification()
        dismissDialog()
    }

    fun clearQuarantinedEmergencyFile() {
        _uiState.update { it.copy(quarantinedEmergencyFile = null) }
    }

    fun promptNewNote(context: Context) {
        viewModelScope.launch {
            val template = FileNameTemplateEngine.getNewFileTemplate(context)
            val defaultName = FileNameTemplateEngine.evaluate(template, context)
            val folders = withContext(ioDispatcher) { repository.getAllFolders() }
            setDialogState(HomeDialogState.NewNote(defaultName, folders))
        }
    }

    fun onNoteClick(note: NoteDocument, context: Context, onDirectOpen: (File) -> Unit) {
        // Priority 1: Check active running sessions FIRST
        val activeMatch = ActiveNotesTracker.findActiveNote(context, note.file)
        if (activeMatch != null) {
            setDialogState(HomeDialogState.ActiveSessionPrompt(note.file, activeMatch, pendingNote = note))
            return
        }

        // Priority 2: Check orphaned autosave file from an earlier crash/dirty exit
        if (note.autosaveInfo != null) {
            setDialogState(HomeDialogState.AutosavePrompt(note))
            return
        }

        // Priority 3: Dispatched to regular open handler
        onDirectOpen(note.file)
    }

    fun togglePin(note: NoteDocument) {
        viewModelScope.launch {
            repository.togglePinNote(note.path)
            loadHomeData(force = true)
        }
    }

    fun duplicateNote(note: NoteDocument, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.duplicateNote(note)
            if (result.isSuccess) {
                loadHomeData(force = true)
                onResult(Result.success(Unit))
            } else {
                onResult(Result.failure(result.exceptionOrNull() ?: Exception("Unknown error")))
            }
        }
    }

    fun createFolder(
        name: String,
        colorHex: String?,
        iconEmoji: String?,
        iconType: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createFolder(
                parentDir = repository.getRootNotesDirectory(),
                name = name,
                colorHex = colorHex,
                iconEmoji = iconEmoji,
                iconType = iconType
            )
            if (res.isSuccess) {
                loadHomeData(force = true)
                onResult(Result.success(Unit))
            } else {
                onResult(Result.failure(res.exceptionOrNull() ?: Exception("Unknown error")))
            }
        }
    }

    fun createBlankNote(
        name: String,
        targetFolder: File,
        onResult: (Result<File>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createBlankNote(name, targetFolder)
            if (result.isSuccess) {
                loadHomeData(force = true)
                onResult(Result.success(result.getOrThrow()))
            } else {
                onResult(Result.failure(result.exceptionOrNull() ?: Exception("Unknown error")))
            }
        }
    }

    fun renameNote(note: NoteDocument, newName: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.renameNote(note, newName)
            if (result.isSuccess) {
                loadHomeData(force = true)
                onResult(Result.success(Unit))
            } else {
                onResult(Result.failure(result.exceptionOrNull() ?: Exception("Unknown error")))
            }
        }
    }

    fun deleteNote(note: NoteDocument, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.moveToTrash(listOf(note))
            if (result.isSuccess) {
                loadHomeData(force = true)
                onResult(Result.success(Unit))
            } else {
                onResult(Result.failure(result.exceptionOrNull() ?: Exception("Unknown error")))
            }
        }
    }

    fun restoreFromTrash(note: NoteDocument) {
        viewModelScope.launch {
            val trashed = repository.scanTrash().find { it.title == note.title }
            if (trashed != null) {
                repository.restoreFromTrash(trashed)
                loadHomeData(force = true)
            }
        }
    }

    fun replaceWithAutosave(note: NoteDocument): File {
        val target = repository.replaceWithAutosave(note)
        loadHomeData(force = true)
        return target
    }

    fun discardAutosave(note: NoteDocument): File {
        val target = repository.discardAutosave(note)
        loadHomeData(force = true)
        return target
    }

    fun saveAutosaveAsNote(autosaveInfo: AutosaveInfo, name: String, targetFolder: File): File {
        val saved = repository.saveAutosaveAsNote(autosaveInfo, name, targetFolder)
        loadHomeData(force = true)
        return saved
    }
}
