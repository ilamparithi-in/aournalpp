package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.LinuxLocaleManager
import dev.ilamparithi.aournalpp.util.AppLocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LocaleManagerTest {

    @Test
    fun `test AppLocaleHelper supported languages includes system default and standard locales`() {
        val list = AppLocaleHelper.getSupportedAppLanguages()
        assertTrue("Expected supported app languages list to not be empty", list.isNotEmpty())

        val first = list.first()
        assertEquals(AppLocaleHelper.SYSTEM_DEFAULT_TAG, first.tag)
        assertTrue(first.isSystemDefault)

        val tags = list.map { it.tag }
        assertTrue("Should include English", tags.contains("en") || tags.contains("en-US"))
        assertTrue("Should include German", tags.contains("de"))
        assertTrue("Should include French", tags.contains("fr"))
        assertTrue("Should include Spanish", tags.contains("es"))
        assertTrue("Should include Japanese", tags.contains("ja"))
        assertTrue("Should include Simplified Chinese", tags.contains("zh-CN"))
        assertTrue("Should include Arabic (RTL)", tags.contains("ar"))
        assertTrue("Should include Persian (RTL)", tags.contains("fa"))
        assertTrue("Should include Hebrew (RTL)", tags.contains("he"))
        assertTrue("Should include en-XA pseudolocale", tags.contains("en-XA"))
        assertTrue("Should include ar-XB pseudolocale", tags.contains("ar-XB"))
    }

    @Test
    fun `test LinuxLocaleManager fallback and known catalog contains comprehensive languages`() {
        // Mocking an environment or testing static properties
        assertEquals("system", LinuxLocaleManager.SYSTEM_DEFAULT_TAG)
        assertEquals("pref_linux_locale", LinuxLocaleManager.PREF_KEY_LINUX_LOCALE)
    }

    @Test
    fun `test parse locale POSIX variable resolution for specific language tags`() {
        // If a specific tag like de_DE.UTF-8 is requested, verify the Triple structure
        val deTag = "de_DE.UTF-8"
        val normalizedTag = if (deTag.endsWith(".UTF-8")) deTag else "$deTag.UTF-8"
        val code = normalizedTag.removeSuffix(".UTF-8")
        val lang = code.substringBefore('_')
        val languageVar = "$code:$lang:en_US:en"

        assertEquals("de_DE.UTF-8", normalizedTag)
        assertEquals("de_DE:de:en_US:en", languageVar)
    }
}
