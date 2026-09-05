package dev.ilamparithi.aournalpp.ui.snap

import android.content.Context
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor

class SnapLayoutManager(private val context: Context? = null) {

    private var configs: List<SnapLayoutConfig> = context?.let {
        SnapLayoutConfigFile.loadConfigurations(it)
    } ?: SnapLayoutConfigFile.createDefaultConfigurations()

    var activeMode: SnapLayoutMode = SnapLayoutMode.SINGLE
        private set

    var isMirrored: Boolean = false
        private set

    val dividerRatios = mutableMapOf<String, Float>()
    val slotAssignments = mutableMapOf<Int, String>()

    fun setConfigs(newConfigs: List<SnapLayoutConfig>) {
        configs = newConfigs
    }

    fun setMode(mode: SnapLayoutMode, mirrored: Boolean = false) {
        activeMode = mode
        isMirrored = mirrored
    }

    fun toggleMirrored() {
        isMirrored = !isMirrored
    }

    fun getAvailableModes(openWindowCount: Int): List<SnapLayoutMode> {
        val list = mutableListOf(SnapLayoutMode.SINGLE, SnapLayoutMode.UNLOCKED)
        val configMap = configs.associateBy { it.id }
        if (openWindowCount >= 2 && configMap[SnapLayoutMode.SPLIT_TWO.id]?.enabled != false) {
            list.add(SnapLayoutMode.SPLIT_TWO)
        }
        if (openWindowCount >= 3 && configMap[SnapLayoutMode.SPLIT_THREE.id]?.enabled != false) {
            list.add(SnapLayoutMode.SPLIT_THREE)
        }
        if (openWindowCount >= 4 && configMap[SnapLayoutMode.GRID_FOUR.id]?.enabled != false) {
            list.add(SnapLayoutMode.GRID_FOUR)
        }
        return list
    }

    fun updateDividerRatio(dividerId: String, ratio: Float) {
        val config = configs.find { it.id == activeMode.id } ?: return
        val allDividers = config.landscape.dividers + config.portrait.dividers +
                (config.mirroredLandscape?.dividers ?: emptyList()) +
                (config.mirroredPortrait?.dividers ?: emptyList())
        val def = allDividers.find { it.id == dividerId }
        val minR = def?.minRatio ?: 0.15f
        val maxR = def?.maxRatio ?: 0.85f
        dividerRatios[dividerId] = ratio.coerceIn(minR, maxR)
    }

    fun resetDividerRatios() {
        dividerRatios.clear()
    }

    fun assignWindowToSlot(slotIndex: Int, windowId: String) {
        slotAssignments[slotIndex] = windowId
    }

    fun clearAssignments() {
        slotAssignments.clear()
    }

    fun getActiveVariant(viewportWidth: Int, viewportHeight: Int): LayoutVariant? {
        val config = configs.find { it.id == activeMode.id } ?: return null
        val isLandscape = viewportWidth >= viewportHeight
        return if (isLandscape) {
            if (isMirrored && config.mirroredLandscape != null) {
                config.mirroredLandscape
            } else {
                config.landscape
            }
        } else {
            if (isMirrored && config.mirroredPortrait != null) {
                config.mirroredPortrait
            } else {
                config.portrait
            }
        }
    }

    fun resolveAnchor(anchor: String, resolvedRatios: Map<String, Float>, defaultVal: Float): Float {
        val trimmed = anchor.trim()
        when (trimmed) {
            "0.0", "0" -> return 0.0f
            "1.0", "1" -> return 1.0f
        }
        resolvedRatios[trimmed]?.let { return it }
        trimmed.toFloatOrNull()?.let { return it }

        // Support basic expressions like "1.0 - v1", "1 - v1", "v1 + 0.1"
        if (trimmed.contains("-")) {
            val parts = trimmed.split("-", limit = 2).map { it.trim() }
            if (parts.size == 2) {
                val left = resolveAnchor(parts[0], resolvedRatios, 0f)
                val right = resolveAnchor(parts[1], resolvedRatios, 0f)
                return (left - right).coerceIn(0f, 1f)
            }
        } else if (trimmed.contains("+")) {
            val parts = trimmed.split("+", limit = 2).map { it.trim() }
            if (parts.size == 2) {
                val left = resolveAnchor(parts[0], resolvedRatios, 0f)
                val right = resolveAnchor(parts[1], resolvedRatios, 0f)
                return (left + right).coerceIn(0f, 1f)
            }
        }
        return defaultVal
    }

    fun calculateGeometries(viewportWidth: Int, viewportHeight: Int): List<WindowSlotGeometry> {
        if (activeMode == SnapLayoutMode.SINGLE || activeMode == SnapLayoutMode.UNLOCKED) {
            return listOf(
                WindowSlotGeometry(
                    slotIndex = 0,
                    x = 0,
                    y = 0,
                    width = viewportWidth,
                    height = viewportHeight
                )
            )
        }

        val variant = getActiveVariant(viewportWidth, viewportHeight) ?: return emptyList()

        val resolvedRatios = mutableMapOf<String, Float>()
        for (div in variant.dividers) {
            val r = (dividerRatios[div.id] ?: div.defaultRatio).coerceIn(div.minRatio, div.maxRatio)
            resolvedRatios[div.id] = r
        }

        return variant.slots.map { slot ->
            val left = resolveAnchor(slot.left, resolvedRatios, 0.0f)
            val right = resolveAnchor(slot.right, resolvedRatios, 1.0f)
            val top = resolveAnchor(slot.top, resolvedRatios, 0.0f)
            val bottom = resolveAnchor(slot.bottom, resolvedRatios, 1.0f)

            val x = Math.round(left * viewportWidth).toInt()
            val y = Math.round(top * viewportHeight).toInt()
            val rightPx = Math.round(right * viewportWidth).toInt()
            val bottomPx = Math.round(bottom * viewportHeight).toInt()

            val w = (rightPx - x).coerceAtLeast(1)
            val h = (bottomPx - y).coerceAtLeast(1)

            WindowSlotGeometry(
                slotIndex = slot.slotIndex,
                x = x,
                y = y,
                width = w,
                height = h
            )
        }
    }

    fun calculateDividerGeometries(viewportWidth: Int, viewportHeight: Int): List<DividerGeometry> {
        if (activeMode == SnapLayoutMode.SINGLE || activeMode == SnapLayoutMode.UNLOCKED) {
            return emptyList()
        }

        val variant = getActiveVariant(viewportWidth, viewportHeight) ?: return emptyList()

        val resolvedRatios = mutableMapOf<String, Float>()
        for (div in variant.dividers) {
            val r = (dividerRatios[div.id] ?: div.defaultRatio).coerceIn(div.minRatio, div.maxRatio)
            resolvedRatios[div.id] = r
        }

        return variant.dividers.map { div ->
            val ratio = resolvedRatios[div.id] ?: div.defaultRatio
            if (div.orientation == DividerOrientation.VERTICAL) {
                val x = Math.round(ratio * viewportWidth).toInt()
                val startY = Math.round(resolveAnchor(div.start, resolvedRatios, 0.0f) * viewportHeight).toInt()
                val endY = Math.round(resolveAnchor(div.end, resolvedRatios, 1.0f) * viewportHeight).toInt()
                DividerGeometry(
                    id = div.id,
                    orientation = DividerOrientation.VERTICAL,
                    x = x,
                    y = startY,
                    length = (endY - startY).coerceAtLeast(1),
                    currentRatio = ratio,
                    minRatio = div.minRatio,
                    maxRatio = div.maxRatio,
                    defaultRatio = div.defaultRatio
                )
            } else {
                val y = Math.round(ratio * viewportHeight).toInt()
                val startX = Math.round(resolveAnchor(div.start, resolvedRatios, 0.0f) * viewportWidth).toInt()
                val endX = Math.round(resolveAnchor(div.end, resolvedRatios, 1.0f) * viewportWidth).toInt()
                DividerGeometry(
                    id = div.id,
                    orientation = DividerOrientation.HORIZONTAL,
                    x = startX,
                    y = y,
                    length = (endX - startX).coerceAtLeast(1),
                    currentRatio = ratio,
                    minRatio = div.minRatio,
                    maxRatio = div.maxRatio,
                    defaultRatio = div.defaultRatio
                )
            }
        }
    }

    fun buildSnapAssignments(
        viewportWidth: Int,
        viewportHeight: Int,
        openWindows: List<ProcessSupervisor.X11WindowInfo>
    ): List<ProcessSupervisor.WindowSnapAssignment> {
        if (openWindows.isEmpty()) return emptyList()

        if (activeMode == SnapLayoutMode.SINGLE) {
            val activeWin = openWindows.find { it.isActive } ?: openWindows.first()
            return listOf(
                ProcessSupervisor.WindowSnapAssignment(
                    windowId = activeWin.id,
                    x = 0,
                    y = 0,
                    width = viewportWidth,
                    height = viewportHeight
                )
            )
        }

        val geometries = calculateGeometries(viewportWidth, viewportHeight)
        val result = mutableListOf<ProcessSupervisor.WindowSnapAssignment>()

        val remainingWindows = openWindows.toMutableList()
        val activeWin = openWindows.find { it.isActive }

        for (geo in geometries) {
            val assignedWinId = slotAssignments[geo.slotIndex]
            val win = if (assignedWinId != null) {
                remainingWindows.find { it.id == assignedWinId } ?: remainingWindows.firstOrNull()
            } else if (geo.slotIndex == 0 && activeWin != null && remainingWindows.contains(activeWin)) {
                activeWin
            } else {
                remainingWindows.firstOrNull()
            }

            if (win != null) {
                remainingWindows.remove(win)
                slotAssignments[geo.slotIndex] = win.id
                result.add(
                    ProcessSupervisor.WindowSnapAssignment(
                        windowId = win.id,
                        x = geo.x,
                        y = geo.y,
                        width = geo.width,
                        height = geo.height
                    )
                )
            }
        }

        return result
    }
}
