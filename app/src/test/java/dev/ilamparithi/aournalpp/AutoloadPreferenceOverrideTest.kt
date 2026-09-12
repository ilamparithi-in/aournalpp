package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoloadPreferenceOverrideTest {

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

        val (result, overridden) = LinuxEnvironment.overrideAutoloadInXml(input)

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

        val (result, overridden) = LinuxEnvironment.overrideAutoloadInXml(input)

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

        val (result, overridden) = LinuxEnvironment.overrideAutoloadInXml(input)

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

        val (result, overridden) = LinuxEnvironment.overrideAutoloadInXml(input)

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

        val (result, overridden) = LinuxEnvironment.overrideAutoloadInXml(input)

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

        val (result, overridden) = LinuxEnvironment.overrideAutoloadInXml(input)

        assertTrue(overridden)
        assertTrue(result.contains("""<property name="autoloadLastFile" value="false"/>"""))
    }
}
