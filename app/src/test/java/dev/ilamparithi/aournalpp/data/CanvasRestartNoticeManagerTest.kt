package dev.ilamparithi.aournalpp.data

import android.content.Context
import dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.LinuxLocaleManager
import dev.ilamparithi.aournalpp.testutils.TestSharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CanvasRestartNoticeManagerTest {

    private lateinit var mockContext: Context
    private lateinit var testPrefs: TestSharedPreferences

    @Before
    fun setUp() {
        mockContext = mockk()
        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "test_canvas_restart")
        tempDir.mkdirs()
        every { mockContext.filesDir } returns tempDir
        every { mockContext.cacheDir } returns tempDir
        testPrefs = TestSharedPreferences()
        every { mockContext.getSharedPreferences(AppPreferences.PREFS_GENERAL, Context.MODE_PRIVATE) } returns testPrefs
        mockkObject(ActiveSessionTracker)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test restart required keys includes scale, locale, theme, and notes dir`() {
        val expected = setOf(
            "pref_ui_scale",
            LinuxLocaleManager.PREF_KEY_LINUX_LOCALE,
            LinuxEnvironment.PREF_KEY_GTK_THEME,
            LinuxEnvironment.PREF_KEY_NOTES_DIR
        )
        assertEquals(expected, CanvasRestartNoticeManager.RESTART_REQUIRED_KEYS)
    }

    @Test
    fun `test onPreferenceChanged sets pending notice flag when session is active`() {
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns true

        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "pref_ui_scale")

        assertTrue(testPrefs.getBoolean(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE, false))
        assertTrue(CanvasRestartNoticeManager.hasPendingNotice(mockContext))
    }

    @Test
    fun `test onPreferenceChanged does not set notice when session is inactive`() {
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns false

        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "pref_ui_scale")

        assertFalse(testPrefs.getBoolean(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE, false))
        assertFalse(CanvasRestartNoticeManager.hasPendingNotice(mockContext))
    }

    @Test
    fun `test onPreferenceChanged ignores keys that do not require restart`() {
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns true

        // Keys that are live updated:
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "displayResolutionMode")
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "touchMode")
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "fullscreen")
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "pref_wallpaper_mode")

        assertFalse(testPrefs.getBoolean(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE, false))
        assertFalse(CanvasRestartNoticeManager.hasPendingNotice(mockContext))
    }

    @Test
    fun `test hasPendingNotice auto-clears and returns false if session terminates`() {
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns true
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, LinuxLocaleManager.PREF_KEY_LINUX_LOCALE)
        assertTrue(CanvasRestartNoticeManager.hasPendingNotice(mockContext))

        // Session terminates:
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns false

        val hasNotice = CanvasRestartNoticeManager.hasPendingNotice(mockContext)
        assertFalse(hasNotice)
        assertFalse(testPrefs.getBoolean(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE, false))
    }

    @Test
    fun `test clearPendingNotice removes flag`() {
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns true
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, LinuxEnvironment.PREF_KEY_GTK_THEME)
        assertTrue(CanvasRestartNoticeManager.hasPendingNotice(mockContext))

        CanvasRestartNoticeManager.clearPendingNotice(mockContext)
        assertFalse(CanvasRestartNoticeManager.hasPendingNotice(mockContext))
        assertFalse(testPrefs.contains(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE))
    }

    @Test
    fun `test baseline comparison sets notice when setting value differs from session baseline`() {
        val sessionInfo = dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo(
            isRunning = true,
            pid = 1234,
            activeNotePath = null,
            documentTitle = null,
            baseUiScale = "1.0",
            baseLocale = "system",
            baseGtkTheme = "system",
            baseNotesDir = "/test/notes"
        )
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns true
        every { ActiveSessionTracker.getActiveSession(any(), any()) } returns sessionInfo

        testPrefs.putString("pref_ui_scale", "1.25")
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "pref_ui_scale")

        assertTrue(testPrefs.getBoolean(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE, false))
        assertTrue(CanvasRestartNoticeManager.hasPendingNotice(mockContext))
    }

    @Test
    fun `test restoring setting to baseline clears pending notice flag and suppresses notice`() {
        val sessionInfo = dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo(
            isRunning = true,
            pid = 1234,
            activeNotePath = null,
            documentTitle = null,
            baseUiScale = "1.0",
            baseLocale = "system",
            baseGtkTheme = "system",
            baseNotesDir = "/test/notes"
        )
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns true
        every { ActiveSessionTracker.getActiveSession(any(), any()) } returns sessionInfo

        // Initialize baseline settings matching the session
        testPrefs.putString("pref_ui_scale", "1.0")
        testPrefs.putString(LinuxLocaleManager.PREF_KEY_LINUX_LOCALE, "system")
        testPrefs.putString(LinuxEnvironment.PREF_KEY_GTK_THEME, "system")
        testPrefs.putString(LinuxEnvironment.PREF_KEY_NOTES_DIR, "/test/notes")

        // 1. User changes UI scale to 1.25 -> notice is set
        testPrefs.putString("pref_ui_scale", "1.25")
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "pref_ui_scale")
        assertTrue(CanvasRestartNoticeManager.hasPendingNotice(mockContext))
        assertTrue(testPrefs.contains(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE))

        // 2. User restores UI scale back to baseline 1.0 -> notice is CLEARED!
        testPrefs.putString("pref_ui_scale", "1.0")
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "pref_ui_scale")
        assertFalse(CanvasRestartNoticeManager.hasPendingNotice(mockContext))
        assertFalse(testPrefs.contains(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE))
    }

    @Test
    fun `test hasPendingNotice auto-clears stale flag when all settings match session baseline`() {
        val sessionInfo = dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo(
            isRunning = true,
            pid = 1234,
            activeNotePath = null,
            documentTitle = null,
            baseUiScale = "1.0",
            baseLocale = "system",
            baseGtkTheme = "system",
            baseNotesDir = "/test/notes"
        )
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns true
        every { ActiveSessionTracker.getActiveSession(any(), any()) } returns sessionInfo

        // Settings currently match session baseline
        testPrefs.putString("pref_ui_scale", "1.0")
        testPrefs.putString(LinuxLocaleManager.PREF_KEY_LINUX_LOCALE, "system")
        testPrefs.putString(LinuxEnvironment.PREF_KEY_GTK_THEME, "system")
        testPrefs.putString(LinuxEnvironment.PREF_KEY_NOTES_DIR, "/test/notes")

        // Simulate a stale or false-positive flag previously recorded
        testPrefs.putBoolean(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE, true)

        // hasPendingNotice should detect that current settings match baseline and clear it!
        val hasNotice = CanvasRestartNoticeManager.hasPendingNotice(mockContext)
        assertFalse(hasNotice)
        assertFalse(testPrefs.contains(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE))
    }

    @Test
    fun `test changing non-restart setting such as fullscreen does not trigger notice`() {
        val sessionInfo = dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo(
            isRunning = true,
            pid = 1234,
            activeNotePath = null,
            documentTitle = null,
            baseUiScale = "1.0",
            baseLocale = "system",
            baseGtkTheme = "system",
            baseNotesDir = "/test/notes"
        )
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns true
        every { ActiveSessionTracker.getActiveSession(any(), any()) } returns sessionInfo

        testPrefs.putString("pref_ui_scale", "1.0")
        testPrefs.putString(LinuxLocaleManager.PREF_KEY_LINUX_LOCALE, "system")
        testPrefs.putString(LinuxEnvironment.PREF_KEY_GTK_THEME, "system")
        testPrefs.putString(LinuxEnvironment.PREF_KEY_NOTES_DIR, "/test/notes")

        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "fullscreen")

        assertFalse(CanvasRestartNoticeManager.hasPendingNotice(mockContext))
        assertFalse(testPrefs.contains(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE))
    }

    @Test
    fun `test multiple settings modified and restored progressively`() {
        val sessionInfo = dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo(
            isRunning = true,
            pid = 1234,
            activeNotePath = null,
            documentTitle = null,
            baseUiScale = "1.0",
            baseLocale = "system",
            baseGtkTheme = "system",
            baseNotesDir = "/test/notes"
        )
        every { ActiveSessionTracker.isSessionActive(any(), any()) } returns true
        every { ActiveSessionTracker.getActiveSession(any(), any()) } returns sessionInfo

        // Baseline setup
        testPrefs.putString("pref_ui_scale", "1.0")
        testPrefs.putString(LinuxLocaleManager.PREF_KEY_LINUX_LOCALE, "system")
        testPrefs.putString(LinuxEnvironment.PREF_KEY_GTK_THEME, "system")
        testPrefs.putString(LinuxEnvironment.PREF_KEY_NOTES_DIR, "/test/notes")

        // Change 2 settings: scale and theme
        testPrefs.putString("pref_ui_scale", "1.5")
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "pref_ui_scale")
        testPrefs.putString(LinuxEnvironment.PREF_KEY_GTK_THEME, "dark")
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, LinuxEnvironment.PREF_KEY_GTK_THEME)

        assertTrue(CanvasRestartNoticeManager.hasPendingNotice(mockContext))

        // Restore scale back to 1.0 (theme is still modified) -> notice remains
        testPrefs.putString("pref_ui_scale", "1.0")
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, "pref_ui_scale")
        assertTrue(CanvasRestartNoticeManager.hasPendingNotice(mockContext))

        // Restore theme back to "system" -> all match baseline -> notice clears!
        testPrefs.putString(LinuxEnvironment.PREF_KEY_GTK_THEME, "system")
        CanvasRestartNoticeManager.onPreferenceChanged(mockContext, LinuxEnvironment.PREF_KEY_GTK_THEME)
        assertFalse(CanvasRestartNoticeManager.hasPendingNotice(mockContext))
        assertFalse(testPrefs.contains(CanvasRestartNoticeManager.PREF_KEY_PENDING_RESTART_NOTICE))
    }

    @Test
    fun `test ui scale float equivalence formatted string does not trigger difference`() {
        val sessionInfo = dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo(
            isRunning = true,
            pid = 1234,
            activeNotePath = null,
            documentTitle = null,
            baseUiScale = "1.0"
        )
        testPrefs.putString("pref_ui_scale", "1.00")
        assertFalse(CanvasRestartNoticeManager.hasSettingsDifferences(mockContext, sessionInfo))
    }
}
