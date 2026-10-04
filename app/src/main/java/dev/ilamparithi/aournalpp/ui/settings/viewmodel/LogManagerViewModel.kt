package dev.ilamparithi.aournalpp.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ilamparithi.aournalpp.logging.CrashLogStorage
import dev.ilamparithi.aournalpp.logging.CrashReport
import dev.ilamparithi.aournalpp.logging.LogEntry
import dev.ilamparithi.aournalpp.logging.LogLevel
import dev.ilamparithi.aournalpp.logging.LogcatReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LogTab {
    CRASHES,
    LIVE_LOGS
}

val DEFAULT_LOG_LEVELS: Set<LogLevel> = setOf(
    LogLevel.DEBUG,
    LogLevel.INFO,
    LogLevel.WARN,
    LogLevel.ERROR
)

data class LogManagerUiState(
    val activeTab: LogTab = LogTab.CRASHES,
    val crashReports: List<CrashReport> = emptyList(),
    val selectedCrashReport: CrashReport? = null,
    val liveLogs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val selectedLogLevels: Set<LogLevel> = DEFAULT_LOG_LEVELS,
    val selectedLogLevel: LogLevel? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAutoRefreshEnabled: Boolean = true,
    val feedbackMessage: String? = null
)

class LogManagerViewModel @JvmOverloads constructor(
    application: Application,
    private val storage: CrashLogStorage = CrashLogStorage(application),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logReader: (maxLines: Int) -> List<LogEntry> = { LogcatReader.readLogcat(it) },
    private val logClearer: () -> Unit = { LogcatReader.clearLogcat() }
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LogManagerUiState(isLoading = true))
    val uiState: StateFlow<LogManagerUiState> = _uiState.asStateFlow()

    private var lastKnownLogTail: String? = null
    private var isFirstLoad = true

    init {
        loadData(silent = false)
    }

    fun loadData(silent: Boolean = false) {
        viewModelScope.launch(ioDispatcher) {
            val crashes = storage.getAllCrashReports()
            val rawLogs = logReader(1000)

            val markedLogs = if (isFirstLoad) {
                isFirstLoad = false
                lastKnownLogTail = rawLogs.lastOrNull()?.raw
                rawLogs
            } else {
                val previousTail = lastKnownLogTail
                val lastIdx = if (previousTail != null) {
                    rawLogs.indexOfLast { it.raw == previousTail }
                } else -1

                lastKnownLogTail = rawLogs.lastOrNull()?.raw

                if (lastIdx in 0 until rawLogs.lastIndex) {
                    rawLogs.mapIndexed { index, entry ->
                        if (index > lastIdx) entry.copy(isNew = true) else entry
                    }
                } else {
                    rawLogs
                }
            }

            val current = _uiState.value
            val filtered = LogcatReader.filterLogsByLevels(
                entries = markedLogs,
                levels = current.selectedLogLevels,
                query = current.searchQuery
            )

            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    state.copy(
                        crashReports = crashes,
                        liveLogs = markedLogs,
                        filteredLogs = filtered,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun refresh(silent: Boolean = false) {
        if (!silent) {
            _uiState.update { it.copy(isRefreshing = true) }
        }
        loadData(silent)
    }

    fun switchTab(tab: LogTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == LogTab.LIVE_LOGS && _uiState.value.liveLogs.isEmpty()) {
            refresh()
        }
    }

    fun toggleLogLevel(level: LogLevel) {
        _uiState.update { current ->
            val newLevels = if (level in current.selectedLogLevels) {
                current.selectedLogLevels - level
            } else {
                current.selectedLogLevels + level
            }
            val filtered = LogcatReader.filterLogsByLevels(
                entries = current.liveLogs,
                levels = newLevels,
                query = current.searchQuery
            )
            current.copy(
                selectedLogLevels = newLevels,
                filteredLogs = filtered
            )
        }
    }

    fun setLogLevel(level: LogLevel?) {
        _uiState.update { current ->
            val newLevel = if (current.selectedLogLevel == level) null else level
            val newLevels = if (newLevel != null) setOf(newLevel) else DEFAULT_LOG_LEVELS
            val filtered = LogcatReader.filterLogsByLevels(
                entries = current.liveLogs,
                levels = newLevels,
                query = current.searchQuery
            )
            current.copy(
                selectedLogLevel = newLevel,
                selectedLogLevels = newLevels,
                filteredLogs = filtered
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val filtered = LogcatReader.filterLogsByLevels(
                entries = current.liveLogs,
                levels = current.selectedLogLevels,
                query = query
            )
            current.copy(searchQuery = query, filteredLogs = filtered)
        }
    }

    fun toggleAutoRefresh() {
        _uiState.update { it.copy(isAutoRefreshEnabled = !it.isAutoRefreshEnabled) }
    }

    fun setAutoRefreshEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAutoRefreshEnabled = enabled) }
    }

    fun clearLiveLogs() {
        viewModelScope.launch(ioDispatcher) {
            logClearer()
            lastKnownLogTail = null
            withContext(Dispatchers.Main) {
                _uiState.update { current ->
                    current.copy(
                        liveLogs = emptyList(),
                        filteredLogs = emptyList(),
                        feedbackMessage = "Logs cleared"
                    )
                }
            }
        }
    }

    fun selectCrashReport(report: CrashReport?) {
        _uiState.update { it.copy(selectedCrashReport = report) }
    }

    fun deleteCrashReport(reportId: String) {
        viewModelScope.launch(ioDispatcher) {
            storage.deleteCrashReport(reportId)
            val updated = storage.getAllCrashReports()
            withContext(Dispatchers.Main) {
                _uiState.update { current ->
                    current.copy(
                        crashReports = updated,
                        selectedCrashReport = if (current.selectedCrashReport?.id == reportId) null else current.selectedCrashReport,
                        feedbackMessage = "Crash report deleted"
                    )
                }
            }
        }
    }

    fun clearAllCrashReports() {
        viewModelScope.launch(ioDispatcher) {
            val count = storage.clearAllCrashReports()
            withContext(Dispatchers.Main) {
                _uiState.update { current ->
                    current.copy(
                        crashReports = emptyList(),
                        selectedCrashReport = null,
                        feedbackMessage = "Cleared $count crash report(s)"
                    )
                }
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
