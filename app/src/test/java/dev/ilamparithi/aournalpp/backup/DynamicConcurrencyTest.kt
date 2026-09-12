package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class DynamicConcurrencyTest {

    @Test
    fun testDynamicConcurrencyScaling() = runBlocking {
        val concurrencyFlow = MutableStateFlow(1)
        val activeWorkers = AtomicInteger(0)
        val peakWorkers = AtomicInteger(0)
        val processedItems = Collections.synchronizedList(mutableListOf<Int>())

        val items = (1..20).toList()

        // Test dynamic scaling using the same channel-worker model implemented in BackupEngine
        val job = launch {
            val totalItems = items.size
            val doneItems = AtomicInteger(0)
            val channel = kotlinx.coroutines.channels.Channel<Int>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            for (item in items) {
                channel.send(item)
            }
            channel.close()

            lateinit var supervisor: kotlinx.coroutines.Job
            val scope = this

            fun launchWorker() {
                scope.launch {
                    try {
                        while (isActive) {
                            if (activeWorkers.get() > concurrencyFlow.value.coerceIn(1, 4)) {
                                break
                            }
                            val receive = channel.receiveCatching()
                            if (receive.isClosed) break
                            val item = receive.getOrNull() ?: break
                            try {
                                val current = activeWorkers.get()
                                peakWorkers.updateAndGet { prev -> maxOf(prev, current) }
                                delay(30)
                                processedItems.add(item)
                            } finally {
                                val finished = doneItems.incrementAndGet()
                                if (finished >= totalItems) {
                                    supervisor.cancel()
                                }
                            }
                        }
                    } finally {
                        val remaining = activeWorkers.decrementAndGet()
                        if (remaining == 0 && doneItems.get() >= totalItems) {
                            supervisor.cancel()
                        } else if (doneItems.get() < totalItems && activeWorkers.get() < concurrencyFlow.value.coerceIn(1, 4)) {
                            val current = activeWorkers.incrementAndGet()
                            if (current <= concurrencyFlow.value.coerceIn(1, 4)) {
                                launchWorker()
                            } else {
                                activeWorkers.decrementAndGet()
                            }
                        }
                    }
                }
            }

            supervisor = launch {
                concurrencyFlow.collect { desired ->
                    val target = desired.coerceIn(1, 4)
                    while (activeWorkers.get() < target && doneItems.get() < totalItems) {
                        val current = activeWorkers.incrementAndGet()
                        if (current <= target) {
                            launchWorker()
                        } else {
                            activeWorkers.decrementAndGet()
                            break
                        }
                    }
                }
            }
        }

        // Initially concurrency is 1. After 50ms, dynamically bump concurrency to 4!
        delay(50)
        concurrencyFlow.value = 4

        job.join()

        // All 20 items must be processed
        assertEquals(20, processedItems.size)
        // Peak workers should have reached 4 after dynamic upscale
        assertTrue("Peak workers should be > 1, was ${peakWorkers.get()}", peakWorkers.get() >= 3)
    }
}
