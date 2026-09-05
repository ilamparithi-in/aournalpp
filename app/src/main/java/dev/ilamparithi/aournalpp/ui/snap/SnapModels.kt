package dev.ilamparithi.aournalpp.ui.snap

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

enum class DividerOrientation {
    VERTICAL,
    HORIZONTAL
}

enum class SnapLayoutMode(val id: String, val title: String, val minWindows: Int) {
    SINGLE("single", "Single Window", 1),
    SPLIT_TWO("split_two", "Two Windows (Split)", 2),
    SPLIT_THREE("split_three", "Three Windows", 3),
    GRID_FOUR("grid_four", "Four Windows (2x2)", 4),
    UNLOCKED("unlocked", "Unlock (Desktop Mode)", 1);

    companion object {
        fun fromId(id: String): SnapLayoutMode {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: SINGLE
        }
    }
}

data class DividerDef(
    val id: String,
    val orientation: DividerOrientation,
    val defaultRatio: Float = 0.5f,
    val minRatio: Float = 0.15f,
    val maxRatio: Float = 0.85f,
    val start: String = "0.0",
    val end: String = "1.0"
) : Serializable {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("orientation", orientation.name)
        put("defaultRatio", defaultRatio.toDouble())
        put("minRatio", minRatio.toDouble())
        put("maxRatio", maxRatio.toDouble())
        put("start", start)
        put("end", end)
    }

    companion object {
        fun fromJson(json: JSONObject): DividerDef {
            return DividerDef(
                id = json.optString("id", "d1"),
                orientation = DividerOrientation.valueOf(json.optString("orientation", "VERTICAL")),
                defaultRatio = json.optDouble("defaultRatio", 0.5).toFloat(),
                minRatio = json.optDouble("minRatio", 0.15).toFloat(),
                maxRatio = json.optDouble("maxRatio", 0.85).toFloat(),
                start = json.optString("start", "0.0"),
                end = json.optString("end", "1.0")
            )
        }
    }
}

data class SlotDef(
    val slotIndex: Int,
    val left: String,
    val top: String,
    val right: String,
    val bottom: String
) : Serializable {
    fun toJson(): JSONObject = JSONObject().apply {
        put("slotIndex", slotIndex)
        put("left", left)
        put("top", top)
        put("right", right)
        put("bottom", bottom)
    }

    companion object {
        fun fromJson(json: JSONObject): SlotDef {
            return SlotDef(
                slotIndex = json.optInt("slotIndex", 0),
                left = json.optString("left", "0.0"),
                top = json.optString("top", "0.0"),
                right = json.optString("right", "1.0"),
                bottom = json.optString("bottom", "1.0")
            )
        }
    }
}

data class LayoutVariant(
    val dividers: List<DividerDef>,
    val slots: List<SlotDef>
) : Serializable {
    fun toJson(): JSONObject = JSONObject().apply {
        val divArr = JSONArray()
        dividers.forEach { divArr.put(it.toJson()) }
        put("dividers", divArr)

        val slotArr = JSONArray()
        slots.forEach { slotArr.put(it.toJson()) }
        put("slots", slotArr)
    }

    companion object {
        fun fromJson(json: JSONObject): LayoutVariant {
            val divList = mutableListOf<DividerDef>()
            json.optJSONArray("dividers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    divList.add(DividerDef.fromJson(arr.getJSONObject(i)))
                }
            }

            val slotList = mutableListOf<SlotDef>()
            json.optJSONArray("slots")?.let { arr ->
                for (i in 0 until arr.length()) {
                    slotList.add(SlotDef.fromJson(arr.getJSONObject(i)))
                }
            }

            return LayoutVariant(dividers = divList, slots = slotList)
        }
    }
}

data class SnapLayoutConfig(
    val id: String,
    val name: String,
    val minWindows: Int,
    val enabled: Boolean = true,
    val landscape: LayoutVariant,
    val portrait: LayoutVariant,
    val mirroredLandscape: LayoutVariant? = null,
    val mirroredPortrait: LayoutVariant? = null
) : Serializable {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("minWindows", minWindows)
        put("enabled", enabled)
        put("landscape", landscape.toJson())
        put("portrait", portrait.toJson())
        mirroredLandscape?.let { put("mirroredLandscape", it.toJson()) }
        mirroredPortrait?.let { put("mirroredPortrait", it.toJson()) }
    }

    companion object {
        fun fromJson(json: JSONObject): SnapLayoutConfig {
            return SnapLayoutConfig(
                id = json.optString("id", "layout"),
                name = json.optString("name", "Snap Layout"),
                minWindows = json.optInt("minWindows", 2),
                enabled = json.optBoolean("enabled", true),
                landscape = LayoutVariant.fromJson(json.getJSONObject("landscape")),
                portrait = LayoutVariant.fromJson(json.getJSONObject("portrait")),
                mirroredLandscape = json.optJSONObject("mirroredLandscape")?.let { LayoutVariant.fromJson(it) },
                mirroredPortrait = json.optJSONObject("mirroredPortrait")?.let { LayoutVariant.fromJson(it) }
            )
        }
    }
}

data class WindowSlotGeometry(
    val slotIndex: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) : Serializable

data class DividerGeometry(
    val id: String,
    val orientation: DividerOrientation,
    val x: Int,
    val y: Int,
    val length: Int,
    val currentRatio: Float,
    val minRatio: Float,
    val maxRatio: Float
) : Serializable
