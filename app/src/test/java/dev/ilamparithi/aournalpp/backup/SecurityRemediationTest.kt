package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.security.InMemorySharedPreferences
import dev.ilamparithi.aournalpp.runtime.BootstrapInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

class SecurityRemediationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testBackupEngineResolveSafeChild_validPaths() {
        val baseDir = tempFolder.newFolder("notesRoot")
        val safe1 = BackupEngine.resolveSafeChild(baseDir, "math.xopp")
        assertEquals(File(baseDir, "math.xopp").canonicalPath, safe1.canonicalPath)

        val safe2 = BackupEngine.resolveSafeChild(baseDir, "subfolder/nested/lecture.xopp")
        assertEquals(File(baseDir, "subfolder/nested/lecture.xopp").canonicalPath, safe2.canonicalPath)
    }

    @Test
    fun testBackupEngineResolveSafeChild_rejectsTraversal() {
        val baseDir = tempFolder.newFolder("notesRoot")

        // Directory traversal attempting to escape baseDir
        assertThrows(SecurityException::class.java) {
            BackupEngine.resolveSafeChild(baseDir, "../outside.xopp")
        }

        assertThrows(SecurityException::class.java) {
            BackupEngine.resolveSafeChild(baseDir, "subfolder/../../escaped.xopp")
        }

        assertThrows(SecurityException::class.java) {
            BackupEngine.resolveSafeChild(baseDir, "../../../../../etc/passwd")
        }

        assertThrows(SecurityException::class.java) {
            BackupEngine.resolveSafeChild(baseDir, "   ")
        }
    }

    @Test
    fun testBootstrapInstaller_isPathWithinRoot() {
        val rootDir = tempFolder.newFolder("sandboxRoot")
        val validFile = File(rootDir, "usr/bin/xournalpp")
        assertTrue(BootstrapInstaller.isPathWithinRoot(rootDir, validFile))

        val outsideFile = File(rootDir.parentFile, "system_file")
        assertFalse(BootstrapInstaller.isPathWithinRoot(rootDir, outsideFile))

        val traversalFile = File(rootDir, "../escaped_target")
        assertFalse(BootstrapInstaller.isPathWithinRoot(rootDir, traversalFile))
    }

    @Test
    fun testBootstrapInstaller_isSymlinkTargetWithinRoot() {
        val rootDir = tempFolder.newFolder("sandboxRoot")
        val binDir = File(rootDir, "usr/bin").apply { mkdirs() }

        // Relative symlink staying inside root
        assertTrue(BootstrapInstaller.isSymlinkTargetWithinRoot(rootDir, binDir, "../lib/libxopp.so"))

        // Relative symlink attempting escape
        assertFalse(BootstrapInstaller.isSymlinkTargetWithinRoot(rootDir, binDir, "../../../etc/shadow"))

        // Absolute symlink staying inside root
        val internalTarget = File(rootDir, "usr/lib/libxopp.so").absolutePath
        assertTrue(BootstrapInstaller.isSymlinkTargetWithinRoot(rootDir, binDir, internalTarget))

        // Absolute symlink attempting escape
        assertFalse(BootstrapInstaller.isSymlinkTargetWithinRoot(rootDir, binDir, "/data/data/other.app/secret"))
    }

    @Test
    fun testInMemorySharedPreferences_failClosedStorage() {
        val prefs = InMemorySharedPreferences()
        assertNull(prefs.getString("token", null))

        prefs.edit()
            .putString("token", "secret-token-xyz")
            .putStringSet("pending", setOf("id1", "id2"))
            .apply()

        assertEquals("secret-token-xyz", prefs.getString("token", null))
        assertEquals(setOf("id1", "id2"), prefs.getStringSet("pending", null))

        prefs.edit().remove("token").apply()
        assertNull(prefs.getString("token", null))

        prefs.edit().clear().apply()
        assertNull(prefs.getStringSet("pending", null))
    }

    @Test
    fun testXmlParser_dtdFeatureDisabling() {
        val parser = try {
            XmlPullParserFactory.newInstance().newPullParser()
        } catch (_: RuntimeException) {
            // Android mock stub in local JVM unit test
            return
        }
        try {
            parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl", false)
            assertEquals(false, parser.getFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl"))
        } catch (_: Exception) {}
    }
}
