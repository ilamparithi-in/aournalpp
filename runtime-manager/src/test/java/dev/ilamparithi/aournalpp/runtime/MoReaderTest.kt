package dev.ilamparithi.aournalpp.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MoReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSampleMoBytes(
        entries: Map<String, String>,
        byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): ByteArray {
        val sortedKeys = entries.keys.sorted()
        val count = sortedKeys.size

        val idBytesList = sortedKeys.map { it.toByteArray(Charsets.UTF_8) }
        val strBytesList = sortedKeys.map { (entries[it] ?: "").toByteArray(Charsets.UTF_8) }

        val idOffsets = mutableListOf<Pair<Int, Int>>()
        val strOffsets = mutableListOf<Pair<Int, Int>>()

        val idsBlock = ByteArrayOutputStream()
        val strsBlock = ByteArrayOutputStream()

        val headerSize = 28
        val tableSize = count * 8 * 2
        val stringsStart = headerSize + tableSize

        var currentOffset = stringsStart
        for (b in idBytesList) {
            idOffsets.add(b.size to currentOffset)
            idsBlock.write(b)
            idsBlock.write(0) // null-terminated
            currentOffset += b.size + 1
        }

        for (b in strBytesList) {
            strOffsets.add(b.size to currentOffset)
            strsBlock.write(b)
            strsBlock.write(0)
            currentOffset += b.size + 1
        }

        val origTabOffset = headerSize
        val transTabOffset = headerSize + count * 8

        val out = ByteArrayOutputStream()
        val buf = ByteBuffer.allocate(stringsStart).order(byteOrder)

        // Header: magic, rev, count, origTab, transTab, hashSize, hashOff
        buf.putInt(0x950412de.toInt())
        buf.putInt(0) // revision
        buf.putInt(count)
        buf.putInt(origTabOffset)
        buf.putInt(transTabOffset)
        buf.putInt(0) // hash table size
        buf.putInt(0) // hash table offset

        // Original table
        for ((len, off) in idOffsets) {
            buf.putInt(len)
            buf.putInt(off)
        }

        // Translation table
        for ((len, off) in strOffsets) {
            buf.putInt(len)
            buf.putInt(off)
        }

        out.write(buf.array())
        out.write(idsBlock.toByteArray())
        out.write(strsBlock.toByteArray())

        return out.toByteArray()
    }

    @Test
    fun `test parseMoBytes with little endian catalog parses translations correctly`() {
        val entries = mapOf(
            "Customize Toolbars" to "Werkzeugleisten anpassen",
            "Xournal++ Preferences" to "Xournal++ Einstellungen",
            "Preferences" to "Einstellungen",
            "Export" to "Exportieren"
        )
        val bytes = createSampleMoBytes(entries, ByteOrder.LITTLE_ENDIAN)

        val result = MoReader.parseMoBytes(
            bytes,
            listOf("Customize Toolbars", "Xournal++ Preferences", "Unknown Key")
        )

        assertEquals("Werkzeugleisten anpassen", result["Customize Toolbars"])
        assertEquals("Xournal++ Einstellungen", result["Xournal++ Preferences"])
        assertTrue(!result.containsKey("Unknown Key"))
    }

    @Test
    fun `test parseMoBytes with big endian catalog parses translations correctly`() {
        val entries = mapOf(
            "Customize Toolbars" to "Personnaliser les barres d'outils",
            "Preferences" to "Préférences"
        )
        val bytes = createSampleMoBytes(entries, ByteOrder.BIG_ENDIAN)

        val result = MoReader.parseMoBytes(bytes, listOf("Customize Toolbars", "Preferences"))

        assertEquals("Personnaliser les barres d'outils", result["Customize Toolbars"])
        assertEquals("Préférences", result["Preferences"])
    }

    @Test
    fun `test parseMoBytes with invalid magic or corrupt bytes returns empty map`() {
        val invalidBytes = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
        val result = MoReader.parseMoBytes(invalidBytes, listOf("Any Key"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test findTranslations discovers localized titles from locale directory`() {
        val localeDir = tempFolder.newFolder("locale")
        val deDir = File(localeDir, "de/LC_MESSAGES").apply { mkdirs() }
        val frDir = File(localeDir, "fr/LC_MESSAGES").apply { mkdirs() }

        val deMo = File(deDir, "xournalpp.mo")
        deMo.writeBytes(
            createSampleMoBytes(
                mapOf(
                    "Customize Toolbars" to "Werkzeugleisten anpassen",
                    "Xournal++ Preferences" to "Xournal++ Einstellungen"
                )
            )
        )

        val frMo = File(frDir, "xournalpp.mo")
        frMo.writeBytes(
            createSampleMoBytes(
                mapOf(
                    "Customize Toolbars" to "Personnaliser les barres d'outils",
                    "Xournal++ Preferences" to "Préférences de Xournal++"
                )
            )
        )

        val translations = MoReader.findTranslations(
            localeDir = localeDir,
            keys = listOf("Customize Toolbars", "Xournal++ Preferences"),
            activeLocale = "de_DE.UTF-8"
        )

        val toolbarTranslations = translations["Customize Toolbars"]
        assertNotNull(toolbarTranslations)
        assertTrue(toolbarTranslations!!.contains("Werkzeugleisten anpassen"))
        assertTrue(toolbarTranslations.contains("Personnaliser les barres d'outils"))

        val prefTranslations = translations["Xournal++ Preferences"]
        assertNotNull(prefTranslations)
        assertTrue(prefTranslations!!.contains("Xournal++ Einstellungen"))
        assertTrue(prefTranslations.contains("Préférences de Xournal++"))
    }
}
