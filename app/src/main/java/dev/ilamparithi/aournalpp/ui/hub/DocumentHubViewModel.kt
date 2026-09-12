package dev.ilamparithi.aournalpp.ui.hub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.utils.ThumbnailManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DocumentHubViewModel(application: Application) : AndroidViewModel(application) {

    val repository: DocumentRepository = DocumentRepository.getInstance(application)
    val env = repository.getLinuxEnvironment()
    val supervisor = ProcessSupervisor(env)
    val pdfExportManager = PdfExportManager(env, supervisor)
    val prefs = AppPreferences.getGeneral(application)

    private val _currentDirectory = MutableStateFlow(repository.getRootNotesDirectory())
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderItem>>(
        repository.getCachedDirectory(repository.getRootNotesDirectory(), "", false)?.first ?: emptyList()
    )
    val folders: StateFlow<List<FolderItem>> = _folders.asStateFlow()

    private val _notes = MutableStateFlow<List<NoteDocument>>(
        repository.getCachedDirectory(repository.getRootNotesDirectory(), "", false)?.second ?: emptyList()
    )
    val notes: StateFlow<List<NoteDocument>> = _notes.asStateFlow()

    private val _trashedNotes = MutableStateFlow<List<NoteDocument>>(emptyList())
    val trashedNotes: StateFlow<List<NoteDocument>> = _trashedNotes.asStateFlow()

    private val _recentNotes = MutableStateFlow<List<NoteDocument>>(
        repository.getCachedRecentNotes(10) ?: emptyList()
    )
    val recentNotes: StateFlow<List<NoteDocument>> = _recentNotes.asStateFlow()

    private val _isViewingTrash = MutableStateFlow(false)
    val isViewingTrash: StateFlow<Boolean> = _isViewingTrash.asStateFlow()

    private val _isGridView = MutableStateFlow(prefs.getBoolean("pref_is_grid_view", true))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(prefs.getBoolean("pref_show_hidden_files", false))
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedNotePaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedNotePaths: StateFlow<Set<String>> = _selectedNotePaths.asStateFlow()

    private val _lastSelectedNotePath = MutableStateFlow<String?>(null)
    val lastSelectedNotePath: StateFlow<String?> = _lastSelectedNotePath.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isPdfConverting = MutableStateFlow(false)
    val isPdfConverting: StateFlow<Boolean> = _isPdfConverting.asStateFlow()

    private val _convertingMessage = MutableStateFlow("")
    val convertingMessage: StateFlow<String> = _convertingMessage.asStateFlow()

    private val _quarantinedEmergencySave = MutableStateFlow<File?>(null)
    val quarantinedEmergencySave: StateFlow<File?> = _quarantinedEmergencySave.asStateFlow()

    private val _showEmergencyDialog = MutableStateFlow(false)
    val showEmergencyDialog: StateFlow<Boolean> = _showEmergencyDialog.asStateFlow()

    private val _showAutoloadOverrideDialog = MutableStateFlow(false)
    val showAutoloadOverrideDialog: StateFlow<Boolean> = _showAutoloadOverrideDialog.asStateFlow()

    fun setCurrentDirectory(directory: File) {
        _currentDirectory.value = directory
        val cached = repository.getCachedDirectory(directory, _searchQuery.value, _showHiddenFiles.value)
        _folders.value = cached?.first ?: emptyList()
        _notes.value = cached?.second ?: emptyList()
    }

    fun setViewingTrash(viewing: Boolean) {
        _isViewingTrash.value = viewing
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
    }

    fun setSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedNotePaths.value = emptySet()
            _lastSelectedNotePath.value = null
        }
    }

    fun setSelectedNotePaths(paths: Set<String>) {
        _selectedNotePaths.value = paths
    }

    fun setLastSelectedNotePath(path: String?) {
        _lastSelectedNotePath.value = path
    }

    fun toggleGridView() {
        val updated = !_isGridView.value
        _isGridView.value = updated
        prefs.edit().putBoolean("pref_is_grid_view", updated).apply()
    }

    fun toggleShowHiddenFiles() {
        val updated = !_showHiddenFiles.value
        _showHiddenFiles.value = updated
        prefs.edit().putBoolean("pref_show_hidden_files", updated).apply()
        loadContent()
    }

    fun setPdfConverting(converting: Boolean, message: String = "") {
        _isPdfConverting.value = converting
        _convertingMessage.value = message
    }

    fun dismissEmergencyDialog() {
        _showEmergencyDialog.value = false
    }

    fun clearQuarantinedEmergencySave() {
        _quarantinedEmergencySave.value = null
    }

    fun dismissAutoloadOverrideDialog() {
        _showAutoloadOverrideDialog.value = false
        env.clearPendingAutoloadOverrideNotification()
    }

    suspend fun loadContentNow() {
        if (_isViewingTrash.value) {
            _trashedNotes.value = repository.scanTrash()
        } else {
            val (fList, nList) = repository.scanDirectory(
                targetDir = _currentDirectory.value,
                query = _searchQuery.value,
                showHidden = _showHiddenFiles.value
            )
            _folders.value = fList
            _notes.value = nList

            if (repository.isRootNotesDirectory(_currentDirectory.value)) {
                _recentNotes.value = repository.getAllRecentNotes(10)
            }

            ThumbnailManager.prefetchThumbnails(getApplication(), nList, pdfExportManager, viewModelScope)

            val emergencyFile = withContext(Dispatchers.IO) {
                val f = env.checkAndQuarantineEmergencySave()
                if (f != null && f.exists() && f.length() > 0) f else null
            }
            if (emergencyFile != null) {
                if (_quarantinedEmergencySave.value == null) {
                    _quarantinedEmergencySave.value = emergencyFile
                    _showEmergencyDialog.value = true
                }
            }
        }

        val autoloadOverridden = withContext(Dispatchers.IO) { env.checkAndOverrideAutoloadPreference() }
        if (autoloadOverridden || env.hasPendingAutoloadOverrideNotification()) {
            _showAutoloadOverrideDialog.value = true
        }
    }

    fun loadContent() {
        viewModelScope.launch { loadContentNow() }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadContentNow()
            _isRefreshing.value = false
        }
    }
}
