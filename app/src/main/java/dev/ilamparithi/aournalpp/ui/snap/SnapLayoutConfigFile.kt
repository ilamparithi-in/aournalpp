package dev.ilamparithi.aournalpp.ui.snap

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SnapLayoutConfigFile {
    private const val TAG = "SnapLayoutConfigFile"
    const val CONFIG_FILENAME = "snap_layouts.json"

    fun getConfigFile(context: Context): File {
        val configDir = File(context.filesDir, "config")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        return File(configDir, CONFIG_FILENAME)
    }

    fun loadConfigurations(context: Context): List<SnapLayoutConfig> {
        val file = getConfigFile(context)
        if (!file.exists()) {
            val defaults = createDefaultConfigurations()
            saveConfigurations(context, defaults)
            return defaults
        }

        return try {
            val content = file.readText()
            val root = JSONObject(content)
            val version = root.optInt("version", 1)
            val layoutsArr = root.optJSONArray("layouts") ?: JSONArray()
            val result = mutableListOf<SnapLayoutConfig>()
            for (i in 0 until layoutsArr.length()) {
                val item = layoutsArr.getJSONObject(i)
                result.add(SnapLayoutConfig.fromJson(item))
            }
            val gridConfig = result.find { it.id == SnapLayoutMode.GRID_FOUR.id }
            if (result.isEmpty() || version < 2 || (gridConfig != null && gridConfig.landscape.dividers.size < 4)) {
                val defaults = createDefaultConfigurations()
                saveConfigurations(context, defaults)
                defaults
            } else {
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse $CONFIG_FILENAME, falling back to defaults", e)
            createDefaultConfigurations()
        }
    }

    fun saveConfigurations(context: Context, configs: List<SnapLayoutConfig>) {
        try {
            val file = getConfigFile(context)
            val root = JSONObject()
            root.put("version", 2)
            val layoutsArr = JSONArray()
            configs.forEach { layoutsArr.put(it.toJson()) }
            root.put("layouts", layoutsArr)
            file.writeText(root.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save $CONFIG_FILENAME", e)
        }
    }

    fun createDefaultConfigurations(): List<SnapLayoutConfig> {
        return listOf(
            createSplitTwoConfig(),
            createSplitThreeConfig(),
            createGridFourConfig()
        )
    }

    private fun createSplitTwoConfig(): SnapLayoutConfig {
        // Landscape: Left | Right (vertical divider)
        val landDividers = listOf(
            DividerDef(id = "v1", orientation = DividerOrientation.VERTICAL, defaultRatio = 0.5f, minRatio = 0.15f, maxRatio = 0.85f)
        )
        val landSlots = listOf(
            SlotDef(slotIndex = 0, left = "0.0", top = "0.0", right = "v1", bottom = "1.0"),
            SlotDef(slotIndex = 1, left = "v1", top = "0.0", right = "1.0", bottom = "1.0")
        )

        // Portrait: Top / Bottom (horizontal divider)
        val portDividers = listOf(
            DividerDef(id = "h1", orientation = DividerOrientation.HORIZONTAL, defaultRatio = 0.5f, minRatio = 0.15f, maxRatio = 0.85f)
        )
        val portSlots = listOf(
            SlotDef(slotIndex = 0, left = "0.0", top = "0.0", right = "1.0", bottom = "h1"),
            SlotDef(slotIndex = 1, left = "0.0", top = "h1", right = "1.0", bottom = "1.0")
        )

        return SnapLayoutConfig(
            id = SnapLayoutMode.SPLIT_TWO.id,
            name = SnapLayoutMode.SPLIT_TWO.title,
            minWindows = 2,
            landscape = LayoutVariant(dividers = landDividers, slots = landSlots),
            portrait = LayoutVariant(dividers = portDividers, slots = portSlots)
        )
    }

    private fun createSplitThreeConfig(): SnapLayoutConfig {
        // Landscape Standard: Left (slot 0) | Right Top (slot 1) / Right Bottom (slot 2)
        val landDividers = listOf(
            DividerDef(id = "v1", orientation = DividerOrientation.VERTICAL, defaultRatio = 0.5f, minRatio = 0.15f, maxRatio = 0.85f),
            DividerDef(id = "h1", orientation = DividerOrientation.HORIZONTAL, defaultRatio = 0.5f, minRatio = 0.15f, maxRatio = 0.85f, start = "v1", end = "1.0")
        )
        val landSlots = listOf(
            SlotDef(slotIndex = 0, left = "0.0", top = "0.0", right = "v1", bottom = "1.0"),
            SlotDef(slotIndex = 1, left = "v1", top = "0.0", right = "1.0", bottom = "h1"),
            SlotDef(slotIndex = 2, left = "v1", top = "h1", right = "1.0", bottom = "1.0")
        )

        // Landscape Mirrored: Left Top (slot 1) / Left Bottom (slot 2) | Right (slot 0)
        val landMirroredDividers = listOf(
            DividerDef(id = "v1", orientation = DividerOrientation.VERTICAL, defaultRatio = 0.5f, minRatio = 0.15f, maxRatio = 0.85f),
            DividerDef(id = "h1", orientation = DividerOrientation.HORIZONTAL, defaultRatio = 0.5f, minRatio = 0.15f, maxRatio = 0.85f, start = "0.0", end = "v1")
        )
        val landMirroredSlots = listOf(
            SlotDef(slotIndex = 1, left = "0.0", top = "0.0", right = "v1", bottom = "h1"),
            SlotDef(slotIndex = 2, left = "0.0", top = "h1", right = "v1", bottom = "1.0"),
            SlotDef(slotIndex = 0, left = "v1", top = "0.0", right = "1.0", bottom = "1.0")
        )

        // Portrait Standard: Top (slot 0) / Bottom Left (slot 1) | Bottom Right (slot 2)
        val portDividers = listOf(
            DividerDef(id = "h1", orientation = DividerOrientation.HORIZONTAL, defaultRatio = 0.5f, minRatio = 0.15f, maxRatio = 0.85f),
            DividerDef(id = "v1", orientation = DividerOrientation.VERTICAL, defaultRatio = 0.5f, minRatio = 0.15f, maxRatio = 0.85f, start = "h1", end = "1.0")
        )
        val portSlots = listOf(
            SlotDef(slotIndex = 0, left = "0.0", top = "0.0", right = "1.0", bottom = "h1"),
            SlotDef(slotIndex = 1, left = "0.0", top = "h1", right = "v1", bottom = "1.0"),
            SlotDef(slotIndex = 2, left = "v1", top = "h1", right = "1.0", bottom = "1.0")
        )

        // Portrait Mirrored: Top Left (slot 1) | Top Right (slot 2) / Bottom (slot 0)
        val portMirroredDividers = listOf(
            DividerDef(id = "h1", orientation = DividerOrientation.HORIZONTAL, defaultRatio = 0.5f, minRatio = 0.15f, maxRatio = 0.85f),
            DividerDef(id = "v1", orientation = DividerOrientation.VERTICAL, defaultRatio = 0.5f, minRatio = 0.15f, maxRatio = 0.85f, start = "0.0", end = "h1")
        )
        val portMirroredSlots = listOf(
            SlotDef(slotIndex = 1, left = "0.0", top = "0.0", right = "v1", bottom = "h1"),
            SlotDef(slotIndex = 2, left = "v1", top = "0.0", right = "1.0", bottom = "h1"),
            SlotDef(slotIndex = 0, left = "0.0", top = "h1", right = "1.0", bottom = "1.0")
        )

        return SnapLayoutConfig(
            id = SnapLayoutMode.SPLIT_THREE.id,
            name = SnapLayoutMode.SPLIT_THREE.title,
            minWindows = 3,
            landscape = LayoutVariant(dividers = landDividers, slots = landSlots),
            portrait = LayoutVariant(dividers = portDividers, slots = portSlots),
            mirroredLandscape = LayoutVariant(dividers = landMirroredDividers, slots = landMirroredSlots),
            mirroredPortrait = LayoutVariant(dividers = portMirroredDividers, slots = portMirroredSlots)
        )
    }

    private fun createGridFourConfig(): SnapLayoutConfig {
        // 4 separate handlebars: 2 vertical, 2 horizontal for full independent window control
        val dividers = listOf(
            DividerDef(
                id = "v_top",
                orientation = DividerOrientation.VERTICAL,
                defaultRatio = 0.5f,
                minRatio = 0.15f,
                maxRatio = 0.85f,
                start = "0.0",
                end = "min(h_left, h_right)"
            ),
            DividerDef(
                id = "v_bottom",
                orientation = DividerOrientation.VERTICAL,
                defaultRatio = 0.5f,
                minRatio = 0.15f,
                maxRatio = 0.85f,
                start = "max(h_left, h_right)",
                end = "1.0"
            ),
            DividerDef(
                id = "h_left",
                orientation = DividerOrientation.HORIZONTAL,
                defaultRatio = 0.5f,
                minRatio = 0.15f,
                maxRatio = 0.85f,
                start = "0.0",
                end = "min(v_top, v_bottom)"
            ),
            DividerDef(
                id = "h_right",
                orientation = DividerOrientation.HORIZONTAL,
                defaultRatio = 0.5f,
                minRatio = 0.15f,
                maxRatio = 0.85f,
                start = "max(v_top, v_bottom)",
                end = "1.0"
            )
        )
        val slots = listOf(
            SlotDef(slotIndex = 0, left = "0.0", top = "0.0", right = "v_top", bottom = "h_left"),
            SlotDef(slotIndex = 1, left = "v_top", top = "0.0", right = "1.0", bottom = "h_right"),
            SlotDef(slotIndex = 2, left = "0.0", top = "h_left", right = "v_bottom", bottom = "1.0"),
            SlotDef(slotIndex = 3, left = "v_bottom", top = "h_right", right = "1.0", bottom = "1.0")
        )

        return SnapLayoutConfig(
            id = SnapLayoutMode.GRID_FOUR.id,
            name = SnapLayoutMode.GRID_FOUR.title,
            minWindows = 4,
            landscape = LayoutVariant(dividers = dividers, slots = slots),
            portrait = LayoutVariant(dividers = dividers, slots = slots)
        )
    }
}
