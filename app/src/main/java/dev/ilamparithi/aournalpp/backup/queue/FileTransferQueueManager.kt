package dev.ilamparithi.aournalpp.backup.queue

import dev.ilamparithi.aournalpp.backup.model.TransferDirection
import dev.ilamparithi.aournalpp.backup.model.TransferItem
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe singleton managing real-time file transfer queues, live progress flows,
 * speeds, and cancellation states for the UI and background workers.
 */
object FileTransferQueueManager {

    private val _items = MutableStateFlow<List<TransferItem>>(emptyList())
    val items: StateFlow<List<TransferItem>> = _items.asStateFlow()

    private val cancellationFlags = ConcurrentHashMap<String, Boolean>()
    private val pauseFlags = ConcurrentHashMap<String, Boolean>()
    private val speedTrackers = ConcurrentHashMap<String, Pair<Long, Long>>() // id -> (lastBytes, lastTimestampMs)

    fun enqueue(item: TransferItem) {
        cancellationFlags[item.id] = false
        pauseFlags[item.id] = false
        _items.update { current ->
            current.filterNot { it.id == item.id } + item
        }
    }

    fun enqueueAll(newItems: List<TransferItem>) {
        newItems.forEach {
            cancellationFlags[it.id] = false
            pauseFlags[it.id] = false
        }
        val newIds = newItems.map { it.id }.toSet()
        _items.update { current ->
            current.filterNot { it.id in newIds } + newItems
        }
    }

    fun markStarted(id: String) {
        cancellationFlags[id] = false
        pauseFlags[id] = false
        speedTrackers[id] = 0L to System.currentTimeMillis()
        _items.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = TransferStatus.IN_PROGRESS,
                        startedAtEpochMs = System.currentTimeMillis()
                    )
                } else it
            }
        }
    }

    fun updateProgress(id: String, transferred: Long, total: Long) {
        val now = System.currentTimeMillis()
        val prev = speedTrackers[id]
        var speed = 0L
        if (prev != null) {
            val deltaBytes = transferred - prev.first
            val deltaTimeMs = now - prev.second
            if (deltaTimeMs >= 500) {
                speed = if (deltaTimeMs > 0) (deltaBytes * 1000L) / deltaTimeMs else 0L
                speedTrackers[id] = transferred to now
            }
        } else {
            speedTrackers[id] = transferred to now
        }

        val progressFraction = if (total > 0) (transferred.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

        _items.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        transferredBytes = transferred,
                        totalBytes = total,
                        progress = progressFraction,
                        speedBytesPerSec = if (speed > 0) speed else it.speedBytesPerSec
                    )
                } else it
            }
        }
    }

    fun markCompleted(id: String) {
        speedTrackers.remove(id)
        _items.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = TransferStatus.COMPLETED,
                        progress = 1f,
                        transferredBytes = it.totalBytes,
                        speedBytesPerSec = 0L,
                        completedAtEpochMs = System.currentTimeMillis()
                    )
                } else it
            }
        }
    }

    fun markFailed(id: String, error: String) {
        speedTrackers.remove(id)
        _items.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = TransferStatus.FAILED,
                        errorMessage = error,
                        speedBytesPerSec = 0L,
                        completedAtEpochMs = System.currentTimeMillis()
                    )
                } else it
            }
        }
    }

    fun markSkipped(id: String) {
        speedTrackers.remove(id)
        _items.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = TransferStatus.SKIPPED,
                        progress = 1f,
                        speedBytesPerSec = 0L,
                        completedAtEpochMs = System.currentTimeMillis()
                    )
                } else it
            }
        }
    }

    fun markRetrying(id: String) {
        cancellationFlags[id] = false
        pauseFlags[id] = false
        speedTrackers.remove(id)
        _items.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = TransferStatus.QUEUED,
                        errorMessage = null,
                        speedBytesPerSec = 0L,
                        progress = 0f,
                        transferredBytes = 0L
                    )
                } else it
            }
        }
    }

    fun requestCancel(id: String) {
        cancellationFlags[id] = true
        pauseFlags[id] = false
        speedTrackers.remove(id)
        _items.update { list ->
            list.map {
                if (it.id == id && (it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED || it.status == TransferStatus.PAUSED)) {
                    it.copy(status = TransferStatus.CANCELLED, speedBytesPerSec = 0L)
                } else it
            }
        }
    }

    fun isCancelled(id: String): Boolean {
        return cancellationFlags[id] == true
    }

    fun requestPause(id: String) {
        pauseFlags[id] = true
        speedTrackers.remove(id)
        _items.update { list ->
            list.map {
                if (it.id == id && (it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED)) {
                    it.copy(status = TransferStatus.PAUSED, speedBytesPerSec = 0L)
                } else it
            }
        }
    }

    fun requestResume(id: String) {
        pauseFlags[id] = false
        cancellationFlags[id] = false
        _items.update { list ->
            list.map {
                if (it.id == id && it.status == TransferStatus.PAUSED) {
                    it.copy(status = TransferStatus.QUEUED, speedBytesPerSec = 0L)
                } else it
            }
        }
    }

    fun isPaused(id: String): Boolean {
        return pauseFlags[id] == true
    }

    fun pauseAll() {
        _items.value.forEach { item ->
            if (item.status == TransferStatus.IN_PROGRESS || item.status == TransferStatus.QUEUED) {
                pauseFlags[item.id] = true
            }
        }
        speedTrackers.clear()
        _items.update { list ->
            list.map {
                if (it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED) {
                    it.copy(status = TransferStatus.PAUSED, speedBytesPerSec = 0L)
                } else it
            }
        }
    }

    fun resumeAll() {
        _items.value.forEach { item ->
            if (item.status == TransferStatus.PAUSED) {
                pauseFlags[item.id] = false
                cancellationFlags[item.id] = false
            }
        }
        _items.update { list ->
            list.map {
                if (it.status == TransferStatus.PAUSED) {
                    it.copy(status = TransferStatus.QUEUED)
                } else it
            }
        }
    }

    fun pauseItems(ids: Set<String>) {
        ids.forEach { id -> pauseFlags[id] = true }
        _items.update { list ->
            list.map {
                if (ids.contains(it.id) && (it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED)) {
                    speedTrackers.remove(it.id)
                    it.copy(status = TransferStatus.PAUSED, speedBytesPerSec = 0L)
                } else it
            }
        }
    }

    fun resumeItems(ids: Set<String>) {
        ids.forEach { id ->
            pauseFlags[id] = false
            cancellationFlags[id] = false
        }
        _items.update { list ->
            list.map {
                if (ids.contains(it.id) && it.status == TransferStatus.PAUSED) {
                    it.copy(status = TransferStatus.QUEUED)
                } else it
            }
        }
    }

    fun dismissItem(id: String) {
        cancellationFlags.remove(id)
        pauseFlags.remove(id)
        speedTrackers.remove(id)
        _items.update { list ->
            list.filterNot { it.id == id }
        }
    }

    fun clearCompleted(serviceId: String? = null) {
        _items.update { list ->
            list.filter {
                if (serviceId != null && it.serviceId != serviceId) {
                    true
                } else {
                    it.status == TransferStatus.IN_PROGRESS ||
                    it.status == TransferStatus.QUEUED ||
                    it.status == TransferStatus.PAUSED ||
                    it.status == TransferStatus.FAILED
                }
            }
        }
    }

    fun clearAll() {
        cancellationFlags.clear()
        pauseFlags.clear()
        speedTrackers.clear()
        _items.value = emptyList()
    }
}
