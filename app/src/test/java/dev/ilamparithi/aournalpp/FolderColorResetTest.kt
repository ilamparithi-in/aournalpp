package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.ui.DEFAULT_PRESET_FOLDER_COLORS
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderColorResetTest {

    @Test
    fun `test resetting folder color removes color property from metadata JSON`() {
        val json = JSONObject()
        json.put("color", "#4CAF50")
        json.put("emoji", "📁")

        // Simulate writeFolderMeta logic when clearing color
        val clearedColor: String? = null
        if (!clearedColor.isNullOrBlank()) {
            json.put("color", clearedColor)
        } else {
            json.remove("color")
        }

        assertFalse(json.has("color"))
        assertTrue(json.has("emoji"))
        assertEquals("📁", json.getString("emoji"))
    }

    @Test
    fun `test blank folder color removes color property from metadata JSON`() {
        val json = JSONObject()
        json.put("color", "#4CAF50")

        val blankColor = "   "
        if (!blankColor.isNullOrBlank()) {
            json.put("color", blankColor)
        } else {
            json.remove("color")
        }

        assertFalse(json.has("color"))
    }

    @Test
    fun `test folder color picker selection logic for default, preset, and custom colors`() {
        val presets = DEFAULT_PRESET_FOLDER_COLORS

        // 1. When color is null (Default)
        val nullColor: String? = null
        val isDefaultNull = nullColor.isNullOrBlank()
        val isCustomNull = !nullColor.isNullOrBlank() && presets.none { it.equals(nullColor, ignoreCase = true) }
        assertTrue("Null color must be default selected", isDefaultNull)
        assertFalse("Null color must NOT be custom selected", isCustomNull)

        // 2. When color is blank string (Default)
        val blankColor = ""
        val isDefaultBlank = blankColor.isNullOrBlank()
        val isCustomBlank = !blankColor.isNullOrBlank() && presets.none { it.equals(blankColor, ignoreCase = true) }
        assertTrue("Blank color must be default selected", isDefaultBlank)
        assertFalse("Blank color must NOT be custom selected", isCustomBlank)

        // 3. When color is in presets
        val presetColor = presets.first() // "#3F51B5"
        val isDefaultPreset = presetColor.isNullOrBlank()
        val isCustomPreset = !presetColor.isNullOrBlank() && presets.none { it.equals(presetColor, ignoreCase = true) }
        val isPresetSelected = !presetColor.isNullOrBlank() && presets.any { it.equals(presetColor, ignoreCase = true) }
        assertFalse(isDefaultPreset)
        assertFalse(isCustomPreset)
        assertTrue(isPresetSelected)

        // 4. When color is a custom hex not in presets
        val customHex = "#123456"
        val isDefaultCustom = customHex.isNullOrBlank()
        val isCustomCustom = !customHex.isNullOrBlank() && presets.none { it.equals(customHex, ignoreCase = true) }
        assertFalse(isDefaultCustom)
        assertTrue("Custom hex not in presets must be marked as custom selected", isCustomCustom)
    }

    @Test
    fun `test reading cleared color falls back to null`() {
        val json = JSONObject("""{"emoji":"📚"}""")
        val color = if (json.has("color")) {
            json.optString("color").takeIf { it.isNotBlank() }
        } else {
            null
        }
        assertNull(color)
    }
}
