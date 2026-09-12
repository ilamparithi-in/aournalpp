package dev.ilamparithi.aournalpp.ui.snap

import android.content.Context
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import kotlin.math.roundToInt

class SnapLayoutManager(context: Context? = null) {

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
        val allDividers = buildList {
            addAll(config.landscape.dividers)
            addAll(config.portrait.dividers)
            config.mirroredLandscape?.dividers?.let { addAll(it) }
            config.mirroredPortrait?.dividers?.let { addAll(it) }
        }
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

    /**
     * Assigns [windowId] to [slotIndex].
     * If total open windows equals [totalSlots], automatically assigns the nth (last remaining)
     * slot with the single remaining unassigned window when exactly 1 slot remains unassigned.
     *
     * Returns true if all slots (0 until [totalSlots]) are now assigned, false otherwise.
     */
    fun assignSlotAndAutoFillNthIfExact(
        slotIndex: Int,
        windowId: String,
        totalSlots: Int,
        allOpenWindows: List<ProcessSupervisor.X11WindowInfo>
    ): Boolean {
        // Remove windowId from any other slot to avoid duplicates
        slotAssignments.entries.removeAll { it.value == windowId && it.key != slotIndex }
        slotAssignments[slotIndex] = windowId

        // If open windows == total slots, auto-fill the nth slot if exactly 1 slot remains unassigned
        if (allOpenWindows.size == totalSlots) {
            val unassignedSlots = (0 until totalSlots).filter { slotAssignments[it].isNullOrBlank() }
            if (unassignedSlots.size == 1) {
                val lastSlot = unassignedSlots.first()
                val assignedIds = slotAssignments.values.toSet()
                val remainingWin = allOpenWindows.firstOrNull { it.id !in assignedIds }
                if (remainingWin != null) {
                    slotAssignments[lastSlot] = remainingWin.id
                }
            }
        }

        return (0 until totalSlots).all { !slotAssignments[it].isNullOrBlank() }
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

        if (trimmed.startsWith("min(") && trimmed.endsWith(")")) {
            val inside = trimmed.substring(4, trimmed.length - 1)
            val parts = inside.split(",", limit = 2).map { it.trim() }
            if (parts.size == 2) {
                val left = resolveAnchor(parts[0], resolvedRatios, 0f)
                val right = resolveAnchor(parts[1], resolvedRatios, 0f)
                return minOf(left, right)
            }
        } else if (trimmed.startsWith("max(") && trimmed.endsWith(")")) {
            val inside = trimmed.substring(4, trimmed.length - 1)
            val parts = inside.split(",", limit = 2).map { it.trim() }
            if (parts.size == 2) {
                val left = resolveAnchor(parts[0], resolvedRatios, 0f)
                val right = resolveAnchor(parts[1], resolvedRatios, 0f)
                return maxOf(left, right)
            }
        }

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
        for ((id, _, defaultRatio, minRatio, maxRatio) in variant.dividers) {
            val r = (dividerRatios[id] ?: defaultRatio).coerceIn(minRatio, maxRatio)
            resolvedRatios[id] = r
        }

        return variant.slots.map { slot ->
            val left = resolveAnchor(slot.left, resolvedRatios, 0.0f)
            val right = resolveAnchor(slot.right, resolvedRatios, 1.0f)
            val top = resolveAnchor(slot.top, resolvedRatios, 0.0f)
            val bottom = resolveAnchor(slot.bottom, resolvedRatios, 1.0f)

            val x = (left * viewportWidth).roundToInt()
            val y = (top * viewportHeight).roundToInt()
            val rightPx = (right * viewportWidth).roundToInt()
            val bottomPx = (bottom * viewportHeight).roundToInt()

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
        for ((id, _, defaultRatio, minRatio, maxRatio) in variant.dividers) {
            val r = (dividerRatios[id] ?: defaultRatio).coerceIn(minRatio, maxRatio)
            resolvedRatios[id] = r
        }

        return variant.dividers.map { div ->
            val ratio = resolvedRatios[div.id] ?: div.defaultRatio
            if (div.orientation == DividerOrientation.VERTICAL) {
                val x = (ratio * viewportWidth).roundToInt()
                val startY = (resolveAnchor(div.start, resolvedRatios, 0.0f) * viewportHeight).roundToInt()
                val endY = (resolveAnchor(div.end, resolvedRatios, 1.0f) * viewportHeight).roundToInt()
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
                val y = (ratio * viewportHeight).roundToInt()
                val startX = (resolveAnchor(div.start, resolvedRatios, 0.0f) * viewportWidth).roundToInt()
                val endX = (resolveAnchor(div.end, resolvedRatios, 1.0f) * viewportWidth).roundToInt()
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

        val openWindowMap = openWindows.associateBy { it.id }
        // Clean up any stale slot indices beyond current geometry count or closed windows
        slotAssignments.keys.filter { it >= geometries.size }.toList().forEach { slotAssignments.remove(it) }
        slotAssignments.entries.removeAll { it.value !in openWindowMap }
        val remainingWindows = openWindows.toMutableList()
        val assignedWindowIds = mutableSetOf<String>()

        // Pass 1: Retain existing valid slot assignments
        for ((slotIndex) in geometries) {
            val assignedWinId = slotAssignments[slotIndex]
            if (assignedWinId != null && openWindowMap.containsKey(assignedWinId)) {
                assignedWindowIds.add(assignedWinId)
                remainingWindows.removeAll { it.id == assignedWinId }
            }
        }

        // Pass 2: For slots without a valid assigned window, fill from remaining unassigned windows
        val activeWin = openWindows.find { it.isActive }
        for ((slotIndex, x, y, width, height) in geometries) {
            var winId = slotAssignments[slotIndex]
            if (winId == null || !openWindowMap.containsKey(winId)) {
                val candidate = if (slotIndex == 0 && activeWin != null && remainingWindows.contains(activeWin)) {
                    activeWin
                } else {
                    remainingWindows.firstOrNull()
                }
                if (candidate != null) {
                    winId = candidate.id
                    slotAssignments[slotIndex] = winId
                    remainingWindows.remove(candidate)
                    assignedWindowIds.add(winId)
                }
            }

            if (winId != null && openWindowMap.containsKey(winId)) {
                result.add(
                    ProcessSupervisor.WindowSnapAssignment(
                        windowId = winId,
                        x = x,
                        y = y,
                        width = width,
                        height = height
                    )
                )
            }
        }

        return result
    }

    data class WindowCloseResolution(
        val newMode: SnapLayoutMode,
        val modeChanged: Boolean,
        val replacedSlotIndex: Int?,
        val replacementWindowId: String?,
        val updatedAssignments: Map<Int, String>
    )

    fun handleWindowClosed(
        currentOpenWindows: List<ProcessSupervisor.X11WindowInfo>,
        mruOrder: List<String> = emptyList()
    ): WindowCloseResolution {
        val currentCount = currentOpenWindows.size
        val currentOpenWindowIds = currentOpenWindows.map { it.id }.toSet()

        if (activeMode == SnapLayoutMode.UNLOCKED) {
            return WindowCloseResolution(
                newMode = SnapLayoutMode.UNLOCKED,
                modeChanged = false,
                replacedSlotIndex = null,
                replacementWindowId = null,
                updatedAssignments = emptyMap()
            )
        }

        val n = activeMode.minWindows
        val closedSlots = slotAssignments.filter { it.value !in currentOpenWindowIds }
        val survivingSlots = slotAssignments.filter { it.value in currentOpenWindowIds }

        // Case 1: When window count drops below n -> drop to n-1 snap layout
        if (currentCount < n) {
            val newMode = when {
                currentCount >= 4 -> SnapLayoutMode.GRID_FOUR
                currentCount == 3 -> SnapLayoutMode.SPLIT_THREE
                currentCount == 2 -> SnapLayoutMode.SPLIT_TWO
                else -> SnapLayoutMode.SINGLE
            }
            activeMode = newMode

            slotAssignments.clear()
            if (newMode == SnapLayoutMode.SINGLE) {
                val survivingWin = currentOpenWindows.find { it.isActive } ?: currentOpenWindows.firstOrNull()
                if (survivingWin != null) {
                    slotAssignments[0] = survivingWin.id
                }
            } else {
                val targetSlotCount = newMode.minWindows
                // Preserve surviving windows in their relative order
                val orderedWindows = survivingSlots.entries.sortedBy { it.key }.map { it.value }.toMutableList()
                for ((id) in currentOpenWindows) {
                    if (id !in orderedWindows) {
                        orderedWindows.add(id)
                    }
                }
                for (i in 0 until minOf(targetSlotCount, orderedWindows.size)) {
                    slotAssignments[i] = orderedWindows[i]
                }
            }

            return WindowCloseResolution(
                newMode = newMode,
                modeChanged = true,
                replacedSlotIndex = null,
                replacementWindowId = null,
                updatedAssignments = slotAssignments.toMap()
            )
        }

        // Case 2: Window count is >= n (till window count is n)
        // If an assigned slot had its window closed, replace it with the window in the immediate background
        var replacedSlot: Int? = null
        var replacementId: String? = null

        if (closedSlots.isNotEmpty()) {
            val assignedIds = survivingSlots.values.toSet()
            val candidateBackground = currentOpenWindows.filter { it.id !in assignedIds }
            val sortedBackground = if (mruOrder.isNotEmpty()) {
                candidateBackground.sortedBy { win ->
                    val idx = mruOrder.indexOf(win.id)
                    if (idx >= 0) idx else Int.MAX_VALUE
                }
            } else {
                candidateBackground
            }

            var bgIdx = 0
            for ((slotIdx, _) in closedSlots) {
                if (bgIdx < sortedBackground.size) {
                    val bgWin = sortedBackground[bgIdx++]
                    slotAssignments[slotIdx] = bgWin.id
                    replacedSlot = slotIdx
                    replacementId = bgWin.id
                } else {
                    slotAssignments.remove(slotIdx)
                }
            }
        }

        // Ensure surviving slots are intact
        for ((slotIdx, winId) in survivingSlots) {
            slotAssignments[slotIdx] = winId
        }

        // Clean up any stale slot assignments outside activeMode's slot bounds or closed windows
        val maxSlots = activeMode.minWindows
        slotAssignments.keys.filter { it >= maxSlots }.toList().forEach { slotAssignments.remove(it) }
        slotAssignments.entries.removeAll { it.value !in currentOpenWindowIds }

        return WindowCloseResolution(
            newMode = activeMode,
            modeChanged = false,
            replacedSlotIndex = replacedSlot,
            replacementWindowId = replacementId,
            updatedAssignments = slotAssignments.toMap()
        )
    }
}
