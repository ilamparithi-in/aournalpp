package dev.ilamparithi.aournalpp.backup.provider

import java.io.FilterInputStream
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream

const val DEFAULT_PROGRESS_STEP_BYTES = 32 * 1024L
const val DEFAULT_PROGRESS_INTERVAL_MS = 100L

/**
 * FilterInputStream that tracks bytes read and invokes a throttled progress callback.
 */
class CountingInputStream(
    input: InputStream,
    private val stepBytes: Long = DEFAULT_PROGRESS_STEP_BYTES,
    private val intervalMs: Long = DEFAULT_PROGRESS_INTERVAL_MS,
    private val onProgress: (Long) -> Unit
) : FilterInputStream(input) {
    private var bytesReadTotal = 0L
    private var lastReportedBytes = 0L
    private var lastReportedTime = 0L

    private fun notifyProgressIfNeeded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || (bytesReadTotal - lastReportedBytes >= stepBytes) || (now - lastReportedTime >= intervalMs)) {
            if (bytesReadTotal != lastReportedBytes || force) {
                lastReportedBytes = bytesReadTotal
                lastReportedTime = now
                onProgress(bytesReadTotal)
            }
        }
    }

    override fun read(): Int {
        val b = super.read()
        if (b != -1) {
            bytesReadTotal++
            notifyProgressIfNeeded()
        } else {
            notifyProgressIfNeeded(force = true)
        }
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val count = super.read(b, off, len)
        if (count != -1) {
            bytesReadTotal += count
            notifyProgressIfNeeded()
        } else {
            notifyProgressIfNeeded(force = true)
        }
        return count
    }

    override fun close() {
        try {
            notifyProgressIfNeeded(force = true)
        } finally {
            super.close()
        }
    }
}

/**
 * FilterOutputStream that tracks bytes written and invokes a throttled progress callback.
 */
class CountingOutputStream(
    output: OutputStream,
    private val stepBytes: Long = DEFAULT_PROGRESS_STEP_BYTES,
    private val intervalMs: Long = DEFAULT_PROGRESS_INTERVAL_MS,
    private val onProgress: (Long) -> Unit
) : FilterOutputStream(output) {
    private var bytesWrittenTotal = 0L
    private var lastReportedBytes = 0L
    private var lastReportedTime = 0L

    private fun notifyProgressIfNeeded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || (bytesWrittenTotal - lastReportedBytes >= stepBytes) || (now - lastReportedTime >= intervalMs)) {
            if (bytesWrittenTotal != lastReportedBytes || force) {
                lastReportedBytes = bytesWrittenTotal
                lastReportedTime = now
                onProgress(bytesWrittenTotal)
            }
        }
    }

    override fun write(b: Int) {
        super.write(b)
        bytesWrittenTotal++
        notifyProgressIfNeeded()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        bytesWrittenTotal += len
        notifyProgressIfNeeded()
    }

    override fun close() {
        try {
            notifyProgressIfNeeded(force = true)
        } finally {
            super.close()
        }
    }
}
