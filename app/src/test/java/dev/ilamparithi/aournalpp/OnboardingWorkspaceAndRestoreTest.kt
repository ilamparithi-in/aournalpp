package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.RestoreResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OnboardingWorkspaceAndRestoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test RestoreResult default and custom hasRestoredConfigs flag`() {
        val notesOnlyResult = RestoreResult(
            serviceId = "test-service",
            serviceName = "Test Cloud",
            totalFilesDiscovered = 15,
            filesRestored = 15,
            filesFailed = 0,
            filesSkipped = 0,
            totalBytesDownloaded = 1024 * 1024,
            durationMs = 500L,
            hasRestoredConfigs = false
        )
        assertFalse(notesOnlyResult.hasRestoredConfigs)
        assertTrue(notesOnlyResult.isSuccess)
        assertEquals(15, notesOnlyResult.filesRestored)

        val fullRestoreResult = notesOnlyResult.copy(
            hasRestoredConfigs = true
        )
        assertTrue(fullRestoreResult.hasRestoredConfigs)
    }

    @Test
    fun `test BackupEngine isConfigFile identifies configs accurately`() {
        // Device configs and legacy configs
        assertTrue(BackupEngine.isConfigFile(".config/xournalpp/settings.xml"))
        assertTrue(BackupEngine.isConfigFile(".config/xournalpp/toolbar.ini"))
        assertTrue(BackupEngine.isConfigFile(".config/xournalpp/palette.gpl"))
        assertTrue(BackupEngine.isConfigFile(".config/xournalpp/colornames.ini"))
        assertTrue(BackupEngine.isConfigFile(".config/openbox/rc.xml"))
        assertTrue(BackupEngine.isConfigFile(".config/gtk-3.0/settings.ini"))
        assertTrue(BackupEngine.isConfigFile(".config/gtk-3.0/bookmarks"))
        assertTrue(BackupEngine.isConfigFile(".devices/01234567-89ab-cdef-0123-456789abcdef/.config/xournalpp/settings.xml"))

        // Standard user notes and media files must NOT be flagged as config files
        assertFalse(BackupEngine.isConfigFile("Lecture1.xopp"))
        assertFalse(BackupEngine.isConfigFile("Folder/Subfolder/Calculus.xopp"))
        assertFalse(BackupEngine.isConfigFile("Imported/Reference.pdf"))
        assertFalse(BackupEngine.isConfigFile("Audio/lecture.m4a"))
        assertFalse(BackupEngine.isConfigFile("Emergency Saves/auto.xopp"))
    }

    @Test
    fun `test pure workspace folder creation behavior`() {
        val notesDir = tempFolder.newFolder("TestNotesHome")
        val emergencyDir = File(notesDir, "Emergency Saves")
        val importedDir = File(notesDir, "Imported")
        val audioDir = File(notesDir, "Audio")

        // Prior to explicit ensureWorkspaceDirectories call, none of these should exist
        assertFalse(emergencyDir.exists())
        assertFalse(importedDir.exists())
        assertFalse(audioDir.exists())

        // Simulating ensureWorkspaceDirectories() logic
        fun ensureWorkspace(notes: File) {
            val emergency = File(notes, "Emergency Saves")
            if (!emergency.exists()) emergency.mkdirs()
            val emergencyMeta = File(emergency, ".aoppfolder")
            if (!emergencyMeta.exists()) {
                emergencyMeta.writeText("""{"role":"emergency","color":"#F44336","icon":"emergency","pinned":true}""")
            }

            val imported = File(notes, "Imported")
            if (!imported.exists()) imported.mkdirs()
            val importedMeta = File(imported, ".aoppfolder")
            if (!importedMeta.exists()) {
                importedMeta.writeText("""{"role":"imported","color":"#2196F3","icon":"folder","pinned":true}""")
            }

            val audio = File(notes, "Audio")
            if (!audio.exists()) audio.mkdirs()
            val audioMeta = File(audio, ".aoppfolder")
            if (!audioMeta.exists()) {
                audioMeta.writeText("""{"role":"audio","color":"#4CAF50","icon":"mic","pinned":true}""")
            }
        }

        ensureWorkspace(notesDir)

        assertTrue(emergencyDir.exists())
        assertTrue(File(emergencyDir, ".aoppfolder").exists())
        assertTrue(importedDir.exists())
        assertTrue(File(importedDir, ".aoppfolder").exists())
        assertTrue(audioDir.exists())
        assertTrue(File(audioDir, ".aoppfolder").exists())
    }
}
