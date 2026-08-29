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
}
