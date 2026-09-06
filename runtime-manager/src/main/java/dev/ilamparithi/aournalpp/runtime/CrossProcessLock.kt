package dev.ilamparithi.aournalpp.runtime

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex

/**
 * Robust cross-process and cross-thread lock backed by an OS-level FileLock
 * and an in-memory Mutex. Ensures that only ONE process/window/thread can hold
 * critical operations (such as bootstrap extraction or cloud synchronization).
 */
class CrossProcessLock private constructor(
    private val lockFile: File,
    private val raf: RandomAccessFile,
    private val channel: FileChannel,
    private val fileLock: FileLock,
    private val mutex: Mutex
) : AutoCloseable {

    companion object {
        private const val TAG = "CrossProcessLock"
        private val inMemoryMutexes = ConcurrentHashMap<String, Mutex>()

        /**
         * Attempts to acquire both the in-process Mutex and the OS-level FileLock.
         * Returns null immediately if already held by another process or coroutine.
         */
        fun tryAcquire(lockFile: File): CrossProcessLock? {
            val path = lockFile.absolutePath
            val mutex = inMemoryMutexes.getOrPut(path) { Mutex() }
            if (!mutex.tryLock()) {
                Log.d(TAG, "CrossProcessLock busy (in-process): $path")
                return null
            }

            var raf: RandomAccessFile? = null
            var channel: FileChannel? = null
            var fileLock: FileLock? = null
            try {
                lockFile.parentFile?.mkdirs()
                raf = RandomAccessFile(lockFile, "rw")
                channel = raf.channel
                fileLock = channel.tryLock()
                if (fileLock == null) {
                    Log.d(TAG, "CrossProcessLock busy (cross-process): $path")
                    channel.close()
                    raf.close()
                    mutex.unlock()
                    return null
                }
                return CrossProcessLock(lockFile, raf, channel, fileLock, mutex)
            } catch (e: Exception) {
                Log.w(TAG, "Failed acquiring CrossProcessLock: $path", e)
                try { fileLock?.release() } catch (_: Exception) {}
                try { channel?.close() } catch (_: Exception) {}
                try { raf?.close() } catch (_: Exception) {}
                mutex.unlock()
                return null
            }
        }

        /**
         * Checks if the lock is currently held (either in-process or by another process).
         */
        fun isLocked(lockFile: File): Boolean {
            val path = lockFile.absolutePath
            val mutex = inMemoryMutexes[path]
            if (mutex != null && mutex.isLocked) return true
            if (!lockFile.exists()) return false

            return try {
                RandomAccessFile(lockFile, "rw").use { raf ->
                    raf.channel.use { channel ->
                        val lock = channel.tryLock()
                        if (lock == null) {
                            true
                        } else {
                            lock.release()
                            false
                        }
                    }
                }
            } catch (e: Exception) {
                true
            }
        }
    }

    override fun close() {
        release()
    }

    fun release() {
        try {
            if (fileLock.isValid) {
                fileLock.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing FileLock for ${lockFile.path}", e)
        }
        try {
            channel.close()
        } catch (_: Exception) {}
        try {
            raf.close()
        } catch (_: Exception) {}
        try {
            mutex.unlock()
        } catch (_: Exception) {}
    }
}
