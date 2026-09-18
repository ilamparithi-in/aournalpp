package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.provider.CloudPathUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudPathUtilsTest {

    @Test
    fun testEmptyBaseReturnsCleanPath() {
        assertEquals("sub/file.xopp", CloudPathUtils.resolveRemotePath("", "sub/file.xopp"))
        assertEquals("sub/file.xopp", CloudPathUtils.resolveRemotePath("/", "/sub/file.xopp/"))
    }

    @Test
    fun testEmptyPathReturnsBase() {
        assertEquals("notes", CloudPathUtils.resolveRemotePath("notes", ""))
        assertEquals("notes", CloudPathUtils.resolveRemotePath("/notes/", "/"))
    }

    @Test
    fun testAlreadyPrefixedPathReturnsWithoutDuplication() {
        assertEquals("notes/sub/file.xopp", CloudPathUtils.resolveRemotePath("notes", "notes/sub/file.xopp"))
        assertEquals("notes/sub/file.xopp", CloudPathUtils.resolveRemotePath("/notes/", "/notes/sub/file.xopp"))
        assertEquals("notes", CloudPathUtils.resolveRemotePath("notes", "notes"))
    }

    @Test
    fun testRelativePathPrefixedWithBase() {
        assertEquals("notes/sub/file.xopp", CloudPathUtils.resolveRemotePath("notes", "sub/file.xopp"))
        assertEquals("notes/sub/file.xopp", CloudPathUtils.resolveRemotePath("/notes/", "/sub/file.xopp/"))
    }

    @Test
    fun testWindowsBackslashesNormalized() {
        assertEquals("notes/sub/file.xopp", CloudPathUtils.resolveRemotePath("notes", "sub\\file.xopp"))
    }
}
