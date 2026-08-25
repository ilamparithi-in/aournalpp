package dev.ilamparithi.aournalpp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoloadPreferenceOverrideTest {

    private fun sanitizeXml(xmlContent: String): Pair<String, Boolean> {
        var content = xmlContent
        var overridden = false

        val propertyNames = listOf("autoloadMostRecent", "autoloadLastFile")
        for (prop in propertyNames) {
            val propRegex = Regex("""<property\b(?=[^>]*\bname\s*=\s*["']$prop["'])(?=[^>]*\bvalue\s*=\s*["']([^"']*)["'])[^>]*/>""")
            val match = propRegex.find(content)
            if (match != null) {
                val currentValue = match.groupValues[1].trim()
                val isEnabled = currentValue.equals("true", ignoreCase = true) ||
                        currentValue == "1" ||
                        currentValue.equals("yes", ignoreCase = true) ||
                        currentValue.equals("on", ignoreCase = true)

                if (isEnabled) {
                    content = content.replace(match.value, """<property name="$prop" value="false"/>""")
                    overridden = true
                }
            }
        }

        if (!content.contains("autoloadMostRecent") && content.contains("</settings>")) {
            content = content.replace("</settings>", "  <property name=\"autoloadMostRecent\" value=\"false\"/>\n</settings>")
        }

        return Pair(content, overridden)
    }

    @Test
    fun `test autoloadMostRecent true is overridden to false`() {
        val input = """
            <?xml version="1.0" encoding="UTF-8"?>
            <settings>
              <property name="defaultSaveDir" value="/storage/emulated/0/Documents/Notes"/>
              <property name="autoloadMostRecent" value="true"/>
              <property name="autosaveEnabled" value="true"/>
            </settings>
        """.trimIndent()

        val (result, overridden) = sanitizeXml(input)

        assertTrue(overridden)
        assertTrue(result.contains("""<property name="autoloadMostRecent" value="false"/>"""))
        assertFalse(result.contains("""name="autoloadMostRecent" value="true""""))
    }

    @Test
    fun `test autoloadMostRecent numeric 1 is overridden to false`() {
        val input = """
            <?xml version="1.0" encoding="UTF-8"?>
            <settings>
              <property name="autoloadMostRecent" value="1"/>
            </settings>
        """.trimIndent()

        val (result, overridden) = sanitizeXml(input)

        assertTrue(overridden)
        assertTrue(result.contains("""<property name="autoloadMostRecent" value="false"/>"""))
        assertFalse(result.contains("""value="1""""))
    }

    @Test
    fun `test autoloadMostRecent already false is not modified`() {
        val input = """
            <?xml version="1.0" encoding="UTF-8"?>
            <settings>
              <property name="autoloadMostRecent" value="false"/>
            </settings>
        """.trimIndent()

        val (result, overridden) = sanitizeXml(input)

        assertFalse(overridden)
        assertEquals(input, result)
    }

    @Test
    fun `test missing autoloadMostRecent is injected as false`() {
        val input = """
            <?xml version="1.0" encoding="UTF-8"?>
            <settings>
              <property name="defaultSaveDir" value="/storage/emulated/0/Documents/Notes"/>
            </settings>
        """.trimIndent()

        val (result, overridden) = sanitizeXml(input)

        assertFalse(overridden)
        assertTrue(result.contains("""<property name="autoloadMostRecent" value="false"/>"""))
    }

    @Test
    fun `test reversed attribute order in XML property is detected and overridden`() {
        val input = """
            <?xml version="1.0" encoding="UTF-8"?>
            <settings>
              <property value="true" name="autoloadMostRecent"/>
            </settings>
        """.trimIndent()

        val (result, overridden) = sanitizeXml(input)

        assertTrue(overridden)
        assertTrue(result.contains("""<property name="autoloadMostRecent" value="false"/>"""))
    }

    @Test
    fun `test legacy autoloadLastFile is also overridden to false`() {
        val input = """
            <?xml version="1.0" encoding="UTF-8"?>
            <settings>
              <property name="autoloadLastFile" value="true"/>
            </settings>
        """.trimIndent()

        val (result, overridden) = sanitizeXml(input)

        assertTrue(overridden)
        assertTrue(result.contains("""<property name="autoloadLastFile" value="false"/>"""))
    }
}
