package dev.ilamparithi.aournalpp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PageTemplateParserTest {

    @Test
    fun `test default fallback when xml is null or blank`() {
        val template = PageTemplate.parseFromSettingsXml(null)
        assertEquals(PageTemplate.DEFAULT_WIDTH, template.width, 0.0001)
        assertEquals(PageTemplate.DEFAULT_HEIGHT, template.height, 0.0001)
        assertEquals("lined", template.style)
        assertEquals("#ffffffff", template.color)
        assertNull(template.config)
    }

    @Test
    fun `test parsing standard xournalpp pageTemplate string with 6-digit hex color`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <settings>
              <property name="pageTemplate" value="xoj/template&#10;copyLastPageSettings=true&#10;size=595.275591x841.889764&#10;backgroundType=lined&#10;backgroundColor=#ffffff&#10;"/>
            </settings>
        """.trimIndent()

        val template = PageTemplate.parseFromSettingsXml(xml)
        assertEquals(595.275591, template.width, 0.0001)
        assertEquals(841.889764, template.height, 0.0001)
        assertEquals("lined", template.style)
        // Verify 6-digit #ffffff is normalized to 8-digit #ffffffff to fix the cyan/blue background bug
        assertEquals("#ffffffff", template.color)
        assertNull(template.config)
    }

    @Test
    fun `test parsing graph background with custom config and 8-digit color`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <settings>
              <property name="pageTemplate" value="xoj/template&#10;copyLastPageSettings=false&#10;size=612.0x792.0&#10;backgroundType=graph&#10;backgroundColor=#fdf6e3ff&#10;backgroundTypeConfig=m1=40,rm=1&#10;"/>
            </settings>
        """.trimIndent()

        val template = PageTemplate.parseFromSettingsXml(xml)
        assertEquals(612.0, template.width, 0.0001)
        assertEquals(792.0, template.height, 0.0001)
        assertEquals("graph", template.style)
        assertEquals("#fdf6e3ff", template.color)
        assertEquals("m1=40,rm=1", template.config)
    }

    @Test
    fun `test normalizeColor formats various color representations`() {
        // 6-digit hex -> 8-digit hex with ff alpha
        assertEquals("#ffffffff", PageTemplate.normalizeColor("#ffffff"))
        assertEquals("#1e1e1eff", PageTemplate.normalizeColor("#1e1e1e"))

        // 8-digit hex preserved
        assertEquals("#ffffffff", PageTemplate.normalizeColor("#ffffffff"))
        assertEquals("#fdf6e380", PageTemplate.normalizeColor("#fdf6e380"))

        // 3-digit shorthand
        assertEquals("#ffffffff", PageTemplate.normalizeColor("#fff"))

        // Named colors
        assertEquals("#ffffffff", PageTemplate.normalizeColor("white"))
        assertEquals("#000000ff", PageTemplate.normalizeColor("black"))
    }

    @Test
    fun `test buildXml generates valid xopp document XML`() {
        val template = PageTemplate(
            width = 595.27559100,
            height = 841.88976400,
            style = "lined",
            color = "#ffffffff",
            config = null
        )

        val xml = template.buildXml()
        assertTrue(xml.contains("""<xournal creator="Xournal++ 1.2.x" fileversion="4">"""))
        assertTrue(xml.contains("""<page width="595.27559100" height="841.88976400">"""))
        assertTrue(xml.contains("""<background type="solid" color="#ffffffff" style="lined"/>"""))
        assertTrue(xml.contains("<layer/>"))
    }

    @Test
    fun `test buildXml with config attribute`() {
        val template = PageTemplate(
            width = 612.0,
            height = 792.0,
            style = "graph",
            color = "#fdf6e3ff",
            config = "m1=40,rm=1"
        )

        val xml = template.buildXml()
        assertTrue(xml.contains("""<background type="solid" color="#fdf6e3ff" style="graph" config="m1=40,rm=1"/>"""))
    }
}
