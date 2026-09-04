package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.model.TransferDirection
import dev.ilamparithi.aournalpp.backup.model.TransferItem
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileTransferQueueManagerTest {

    @Before
    fun setup() {
        FileTransferQueueManager.clearAll()
    }

    @Test
    fun testEnqueueAndProgressUpdates() {
        val item = TransferItem(
            id = "test_item_1",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/note.xopp",
            remotePath = "Aournalpp/Notes/note.xopp",
            fileName = "note.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 1000L,
            status = TransferStatus.QUEUED
        )

        FileTransferQueueManager.enqueue(item)
        assertEquals(1, FileTransferQueueManager.items.value.size)
        assertEquals(TransferStatus.QUEUED, FileTransferQueueManager.items.value[0].status)

        FileTransferQueueManager.markStarted("test_item_1")
        assertEquals(TransferStatus.IN_PROGRESS, FileTransferQueueManager.items.value[0].status)

        FileTransferQueueManager.updateProgress("test_item_1", 500L, 1000L)
        val updated = FileTransferQueueManager.items.value[0]
        assertEquals(500L, updated.transferredBytes)
        assertEquals(0.5f, updated.progress, 0.01f)

        FileTransferQueueManager.markCompleted("test_item_1")
        val completed = FileTransferQueueManager.items.value[0]
        assertEquals(TransferStatus.COMPLETED, completed.status)
        assertEquals(1f, completed.progress, 0.01f)
    }

    @Test
    fun testCancellation() {
        val item = TransferItem(
            id = "test_item_cancel",
            serviceId = "srv_1",
            serviceName = "SFTP",
            localFilePath = "/test/drawing.xopp",
            remotePath = "Aournalpp/Notes/drawing.xopp",
            fileName = "drawing.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 5000L,
            status = TransferStatus.QUEUED
        )

        FileTransferQueueManager.enqueue(item)
        assertFalse(FileTransferQueueManager.isCancelled("test_item_cancel"))

        FileTransferQueueManager.requestCancel("test_item_cancel")
        assertTrue(FileTransferQueueManager.isCancelled("test_item_cancel"))
        assertEquals(TransferStatus.CANCELLED, FileTransferQueueManager.items.value[0].status)
    }

    @Test
    fun testClearCompleted() {
        val item1 = TransferItem(
            id = "item_1",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/1.xopp",
            remotePath = "1.xopp",
            fileName = "1.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.COMPLETED
        )
        val item2 = TransferItem(
            id = "item_2",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/2.xopp",
            remotePath = "2.xopp",
            fileName = "2.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.IN_PROGRESS
        )

        FileTransferQueueManager.enqueue(item1)
        FileTransferQueueManager.enqueue(item2)
        assertEquals(2, FileTransferQueueManager.items.value.size)

        FileTransferQueueManager.clearCompleted()
        assertEquals(1, FileTransferQueueManager.items.value.size)
        assertEquals("item_2", FileTransferQueueManager.items.value[0].id)
    }

    @Test
    fun testCancelDoesNotAffectOtherQueuedItems() {
        val item1 = TransferItem(
            id = "item_1",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/1.xopp",
            remotePath = "1.xopp",
            fileName = "1.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.QUEUED
        )
        val item2 = TransferItem(
            id = "item_2",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/2.xopp",
            remotePath = "2.xopp",
            fileName = "2.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.QUEUED
        )
        val item3 = TransferItem(
            id = "item_3",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/3.xopp",
            remotePath = "3.xopp",
            fileName = "3.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.QUEUED
        )

        FileTransferQueueManager.enqueueAll(listOf(item1, item2, item3))
        FileTransferQueueManager.markStarted("item_3")

        // Cancel item_3 (in-progress)
        FileTransferQueueManager.requestCancel("item_3")
        assertTrue(FileTransferQueueManager.isCancelled("item_3"))
        assertFalse(FileTransferQueueManager.isCancelled("item_1"))
        assertFalse(FileTransferQueueManager.isCancelled("item_2"))

        val itemsAfterCancel3 = FileTransferQueueManager.items.value.associateBy { it.id }
        assertEquals(TransferStatus.CANCELLED, itemsAfterCancel3["item_3"]?.status)
        assertEquals(TransferStatus.QUEUED, itemsAfterCancel3["item_1"]?.status)
        assertEquals(TransferStatus.QUEUED, itemsAfterCancel3["item_2"]?.status)

        // Cancel item_1 (queued)
        FileTransferQueueManager.requestCancel("item_1")
        assertTrue(FileTransferQueueManager.isCancelled("item_1"))
        assertFalse(FileTransferQueueManager.isCancelled("item_2"))

        val itemsAfterCancel1 = FileTransferQueueManager.items.value.associateBy { it.id }
        assertEquals(TransferStatus.CANCELLED, itemsAfterCancel1["item_1"]?.status)
        assertEquals(TransferStatus.QUEUED, itemsAfterCancel1["item_2"]?.status)
    }

    @Test
    fun testPauseAndResume() {
        val item = TransferItem(
            id = "item_pause",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/pause.xopp",
            remotePath = "pause.xopp",
            fileName = "pause.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 500L,
            status = TransferStatus.QUEUED
        )

        FileTransferQueueManager.enqueue(item)
        FileTransferQueueManager.markStarted("item_pause")
        assertEquals(TransferStatus.IN_PROGRESS, FileTransferQueueManager.items.value[0].status)

        FileTransferQueueManager.requestPause("item_pause")
        assertTrue(FileTransferQueueManager.isPaused("item_pause"))
        assertEquals(TransferStatus.PAUSED, FileTransferQueueManager.items.value[0].status)
        assertEquals(0L, FileTransferQueueManager.items.value[0].speedBytesPerSec)

        FileTransferQueueManager.requestResume("item_pause")
        assertFalse(FileTransferQueueManager.isPaused("item_pause"))
        assertEquals(TransferStatus.QUEUED, FileTransferQueueManager.items.value[0].status)
    }

    @Test
    fun testPauseAllAndResumeAll() {
        val item1 = TransferItem(
            id = "item_1",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/1.xopp",
            remotePath = "1.xopp",
            fileName = "1.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.QUEUED
        )
        val item2 = TransferItem(
            id = "item_2",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/2.xopp",
            remotePath = "2.xopp",
            fileName = "2.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.QUEUED
        )

        FileTransferQueueManager.enqueueAll(listOf(item1, item2))
        FileTransferQueueManager.markStarted("item_1")

        FileTransferQueueManager.pauseAll()
        assertTrue(FileTransferQueueManager.isPaused("item_1"))
        assertTrue(FileTransferQueueManager.isPaused("item_2"))
        FileTransferQueueManager.items.value.forEach {
            assertEquals(TransferStatus.PAUSED, it.status)
        }

        FileTransferQueueManager.resumeAll()
        assertFalse(FileTransferQueueManager.isPaused("item_1"))
        assertFalse(FileTransferQueueManager.isPaused("item_2"))
        FileTransferQueueManager.items.value.forEach {
            assertEquals(TransferStatus.QUEUED, it.status)
        }
    }

    @Test
    fun testDeterministicIdDeduplication() {
        val itemOriginal = TransferItem(
            id = "srv_1_UPLOAD_test.xopp",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/test.xopp",
            remotePath = "test.xopp",
            fileName = "test.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 1000L,
            status = TransferStatus.QUEUED
        )

        FileTransferQueueManager.enqueue(itemOriginal)
        FileTransferQueueManager.markStarted("srv_1_UPLOAD_test.xopp")
        FileTransferQueueManager.markFailed("srv_1_UPLOAD_test.xopp", "Network timeout")
        assertEquals(1, FileTransferQueueManager.items.value.size)
        assertEquals(TransferStatus.FAILED, FileTransferQueueManager.items.value[0].status)

        // Re-enqueuing the same file transfer replaces the failed entry instead of appending
        val itemRetry = TransferItem(
            id = "srv_1_UPLOAD_test.xopp",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/test.xopp",
            remotePath = "test.xopp",
            fileName = "test.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 1000L,
            status = TransferStatus.QUEUED
        )
        FileTransferQueueManager.enqueue(itemRetry)
        assertEquals(1, FileTransferQueueManager.items.value.size)
        assertEquals(TransferStatus.QUEUED, FileTransferQueueManager.items.value[0].status)
    }

    @Test
    fun testDismissItem() {
        val item = TransferItem(
            id = "item_dismiss",
            serviceId = "srv_1",
            serviceName = "Nextcloud",
            localFilePath = "/test/dismiss.xopp",
            remotePath = "dismiss.xopp",
            fileName = "dismiss.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.FAILED,
            errorMessage = "Server error"
        )

        FileTransferQueueManager.enqueue(item)
        assertEquals(1, FileTransferQueueManager.items.value.size)

        FileTransferQueueManager.dismissItem("item_dismiss")
        assertEquals(0, FileTransferQueueManager.items.value.size)
    }

    @Test
    fun testPauseItemsAndResumeItemsSubset() {
        val srv1Item = TransferItem(
            id = "srv1_item",
            serviceId = "srv_1",
            serviceName = "Google Drive",
            localFilePath = "/test/1.xopp",
            remotePath = "1.xopp",
            fileName = "1.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.QUEUED
        )
        val srv2Item = TransferItem(
            id = "srv2_item",
            serviceId = "srv_2",
            serviceName = "OneDrive",
            localFilePath = "/test/2.xopp",
            remotePath = "2.xopp",
            fileName = "2.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 200L,
            status = TransferStatus.QUEUED
        )

        FileTransferQueueManager.enqueueAll(listOf(srv1Item, srv2Item))

        // Pause only srv1Item
        FileTransferQueueManager.pauseItems(setOf("srv1_item"))
        assertTrue(FileTransferQueueManager.isPaused("srv1_item"))
        assertFalse(FileTransferQueueManager.isPaused("srv2_item"))

        val itemsAfterPause = FileTransferQueueManager.items.value.associateBy { it.id }
        assertEquals(TransferStatus.PAUSED, itemsAfterPause["srv1_item"]?.status)
        assertEquals(TransferStatus.QUEUED, itemsAfterPause["srv2_item"]?.status)

        // Resume only srv1Item
        FileTransferQueueManager.resumeItems(setOf("srv1_item"))
        assertFalse(FileTransferQueueManager.isPaused("srv1_item"))
        val itemsAfterResume = FileTransferQueueManager.items.value.associateBy { it.id }
        assertEquals(TransferStatus.QUEUED, itemsAfterResume["srv1_item"]?.status)
        assertEquals(TransferStatus.QUEUED, itemsAfterResume["srv2_item"]?.status)
    }

    @Test
    fun testClearCompletedScopedToServiceId() {
        val srv1Completed = TransferItem(
            id = "srv1_done",
            serviceId = "srv_1",
            serviceName = "Google Drive",
            localFilePath = "/test/1.xopp",
            remotePath = "1.xopp",
            fileName = "1.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.COMPLETED
        )
        val srv2Completed = TransferItem(
            id = "srv2_done",
            serviceId = "srv_2",
            serviceName = "OneDrive",
            localFilePath = "/test/2.xopp",
            remotePath = "2.xopp",
            fileName = "2.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 200L,
            status = TransferStatus.COMPLETED
        )
        val srv1Queued = TransferItem(
            id = "srv1_queued",
            serviceId = "srv_1",
            serviceName = "Google Drive",
            localFilePath = "/test/3.xopp",
            remotePath = "3.xopp",
            fileName = "3.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 300L,
            status = TransferStatus.QUEUED
        )

        FileTransferQueueManager.enqueueAll(listOf(srv1Completed, srv2Completed, srv1Queued))
        assertEquals(3, FileTransferQueueManager.items.value.size)

        // Clear completed only for srv_1
        FileTransferQueueManager.clearCompleted(serviceId = "srv_1")
        val remaining = FileTransferQueueManager.items.value
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.id == "srv2_done" })
        assertTrue(remaining.any { it.id == "srv1_queued" })
        assertFalse(remaining.any { it.id == "srv1_done" })

        // Clear completed for all services (serviceId = null)
        FileTransferQueueManager.clearCompleted(serviceId = null)
        val finalRemaining = FileTransferQueueManager.items.value
        assertEquals(1, finalRemaining.size)
        assertEquals("srv1_queued", finalRemaining[0].id)
    }

    @Test
    fun testMultiCloudServicesInQueue() {
        val srv1Item = TransferItem(
            id = "srv1_item",
            serviceId = "srv_1",
            serviceName = "Google Drive",
            localFilePath = "/test/1.xopp",
            remotePath = "1.xopp",
            fileName = "1.xopp",
            direction = TransferDirection.UPLOAD,
            totalBytes = 100L,
            status = TransferStatus.QUEUED
        )
        val srv2Item = TransferItem(
            id = "srv2_item",
            serviceId = "srv_2",
            serviceName = "OneDrive",
            localFilePath = "/test/2.xopp",
            remotePath = "2.xopp",
            fileName = "2.xopp",
            direction = TransferDirection.DOWNLOAD,
            totalBytes = 200L,
            status = TransferStatus.IN_PROGRESS
        )

        FileTransferQueueManager.enqueueAll(listOf(srv1Item, srv2Item))

        val distinctServices = FileTransferQueueManager.items.value.map { it.serviceId to it.serviceName }.distinct()
        assertEquals(2, distinctServices.size)
        assertTrue(distinctServices.contains("srv_1" to "Google Drive"))
        assertTrue(distinctServices.contains("srv_2" to "OneDrive"))

        // Filtering by service srv_1
        val srv1Filtered = FileTransferQueueManager.items.value.filter { it.serviceId == "srv_1" }
        assertEquals(1, srv1Filtered.size)
        assertEquals("Google Drive", srv1Filtered[0].serviceName)

        // Filtering by service srv_2
        val srv2Filtered = FileTransferQueueManager.items.value.filter { it.serviceId == "srv_2" }
        assertEquals(1, srv2Filtered.size)
        assertEquals("OneDrive", srv2Filtered[0].serviceName)
    }
}
