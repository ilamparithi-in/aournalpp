package dev.ilamparithi.aournalpp.utils

import dev.ilamparithi.aournalpp.ui.collage.CollageLayoutMemoryCache
import dev.ilamparithi.aournalpp.ui.collage.CollageLayoutResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XoppNativeRendererTest {

    @Test
    fun `test parseXoppColor with 8-digit RGBA hex`() {
        // Xournal stores #RRGGBBAA -> #FF000080 is Red with 50% alpha (128 = 0x80)
        val argb = XoppNativeRenderer.parseXoppColor("#FF000080", 0)
        val alpha = (argb ushr 24) and 0xFF
        val red = (argb ushr 16) and 0xFF
        val green = (argb ushr 8) and 0xFF
        val blue = argb and 0xFF

        assertEquals(0x80, alpha)
        assertEquals(0xFF, red)
        assertEquals(0x00, green)
        assertEquals(0x00, blue)
    }

    @Test
    fun `test parseXoppColor with 6-digit RGB hex`() {
        val argb = XoppNativeRenderer.parseXoppColor("#00FF00", 0)
        val alpha = (argb ushr 24) and 0xFF
        val red = (argb ushr 16) and 0xFF
        val green = (argb ushr 8) and 0xFF
        val blue = argb and 0xFF

        assertEquals(0xFF, alpha)
        assertEquals(0x00, red)
        assertEquals(0xFF, green)
        assertEquals(0x00, blue)
    }

    @Test
    fun `test parseXoppColor with named colors`() {
        assertEquals(0xFF000000.toInt(), XoppNativeRenderer.parseXoppColor("black", 0))
        assertEquals(0xFFFFFFFF.toInt(), XoppNativeRenderer.parseXoppColor("white", 0))
        assertEquals(0xFFFF0000.toInt(), XoppNativeRenderer.parseXoppColor("red", 0))
        assertEquals(0xFF008000.toInt(), XoppNativeRenderer.parseXoppColor("green", 0))
        assertEquals(0xFF3333CC.toInt(), XoppNativeRenderer.parseXoppColor("blue", 0))
    }

    @Test
    fun `test version disable and permanent latching`() {
        assertTrue(XoppNativeRenderer.isNativeRenderingEnabled("xournalpp-v1.2.0"))

        // Disable for version 1.2.0
        XoppNativeRenderer.disableNativeRenderingForVersion("xournalpp-v1.2.0")
        assertFalse(XoppNativeRenderer.isNativeRenderingEnabled("xournalpp-v1.2.0"))

        // Other versions should still be enabled
        assertTrue(XoppNativeRenderer.isNativeRenderingEnabled("xournalpp-v1.3.0"))

        // Global disable disables all versions
        XoppNativeRenderer.disableNativeRenderingGlobally()
        assertFalse(XoppNativeRenderer.isNativeRenderingEnabled("xournalpp-v1.3.0"))
    }

    @Test
    fun `test collage layout memory cache`() {
        CollageLayoutMemoryCache.clear()
        assertNull(CollageLayoutMemoryCache.getLayout(800))

        val dummyResult = CollageLayoutResult(
            cards = emptyList(),
            totalWidth = 800f,
            totalHeight = 600f,
            isScrollable = false
        )
        CollageLayoutMemoryCache.putLayout(800, dummyResult)

        val retrieved = CollageLayoutMemoryCache.getLayout(800)
        assertNotNull(retrieved)
        assertEquals(800f, retrieved?.totalWidth)
        assertEquals(600f, retrieved?.totalHeight)

        CollageLayoutMemoryCache.clear()
        assertNull(CollageLayoutMemoryCache.getLayout(800))
    }

    @Test
    fun `test cache key uniqueness for same name in different folders`() {
        val fileInFolderA = java.io.File("/storage/emulated/0/Notes/Math/lecture.xopp")
        val fileInFolderB = java.io.File("/storage/emulated/0/Notes/Physics/lecture.xopp")

        val keyA = ThumbnailManager.cacheKeyFor(fileInFolderA)
        val keyB = ThumbnailManager.cacheKeyFor(fileInFolderB)

        org.junit.Assert.assertNotEquals("Files with the same name in different folders must not share cache key", keyA, keyB)
    }

    @Test
    fun `test cache key uniqueness for same name with different extensions in same folder`() {
        val xoppFile = java.io.File("/storage/emulated/0/Notes/lecture.xopp")
        val pdfFile = java.io.File("/storage/emulated/0/Notes/lecture.pdf")

        val keyXopp = ThumbnailManager.cacheKeyFor(xoppFile)
        val keyPdf = ThumbnailManager.cacheKeyFor(pdfFile)

        org.junit.Assert.assertNotEquals("Same name with different extensions must not share cache key", keyXopp, keyPdf)
    }

    @Test
    fun `test cache key sanitization for special characters and emojis`() {
        val fileWithSpecialChars = java.io.File("/storage/emulated/0/Notes/🔥 Note: 1 #test?.xopp")
        val key = ThumbnailManager.cacheKeyFor(fileWithSpecialChars)

        // Must not contain invalid filesystem characters like :, ?, *, /, #, space or emoji
        assertFalse(key.contains(":"))
        assertFalse(key.contains("?"))
        assertFalse(key.contains("#"))
        assertFalse(key.contains("🔥"))
        assertTrue(key.endsWith(".png"))
    }
}
