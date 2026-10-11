package dev.ilamparithi.aournalpp.runtime

import kotlin.math.ceil

/**
 * Pure domain advisor that evaluates window layout requisitions against viewport
 * and display geometries to determine window placement policies and orientation advisories.
 */
object WindowOrientationAdvisor {

    // Base widget requisition in GTK logical pixels from ui/settings.glade
    const val PREFERENCES_MIN_WIDTH = 1024
    const val PREFERENCES_MIN_HEIGHT = 740

    // Standard dialogs (e.g. Export, Print, File Choosers) base minimum dimensions
    const val STANDARD_DIALOG_MIN_WIDTH = 640
    const val STANDARD_DIALOG_MIN_HEIGHT = 480

    /**
     * Determines whether a window with base requisitions [baseWidth] × [baseHeight] can fit
     * comfortably in fullscreen within [viewportWidth] × [viewportHeight] under [uiScale].
     */
    fun canFitFullscreen(
        viewportWidth: Int,
        viewportHeight: Int,
        baseWidth: Int,
        baseHeight: Int,
        uiScale: Float
    ): Boolean {
        if (viewportWidth <= 0 || viewportHeight <= 0) return true
        val effectiveScale = uiScale.coerceAtLeast(0.5f)
        val requiredWidth = ceil(baseWidth * effectiveScale).toInt()
        val requiredHeight = ceil(baseHeight * effectiveScale).toInt()
        return viewportWidth >= requiredWidth && viewportHeight >= requiredHeight
    }

    /**
     * Determines whether Preferences can fit in fullscreen under current viewport and UI scale.
     */
    fun canFitPreferencesFullscreen(
        viewportWidth: Int,
        viewportHeight: Int,
        uiScale: Float
    ): Boolean = canFitFullscreen(
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        baseWidth = PREFERENCES_MIN_WIDTH,
        baseHeight = PREFERENCES_MIN_HEIGHT,
        uiScale = uiScale
    )

    /**
     * Evaluates whether a window/dialog is best viewed in landscape mode.
     *
     * Invariants:
     * 1. Returns `true` ONLY IF:
     *    a) The window cannot fit in fullscreen in the current orientation ([currentWidth] × [currentHeight]).
     *    b) The window CAN fit in fullscreen in landscape orientation ([displayWidth] × [displayHeight]).
     * 2. Returns `false` if landscape mode ALSO cannot fit the window in fullscreen (e.g. small screen
     *    or excessively high UI scale), ensuring users are never misled.
     */
    fun shouldRecommendLandscape(
        windowTitle: String?,
        currentWidth: Int,
        currentHeight: Int,
        displayWidth: Int,
        displayHeight: Int,
        uiScale: Float,
        matchedTitles: Set<String> = emptySet()
    ): Boolean {
        if (windowTitle.isNullOrBlank()) return false
        val (baseW, baseH) = getWindowMinimumRequisition(windowTitle, matchedTitles) ?: return false

        // 1. If it already fits in current viewport, no advisory needed
        val currentFits = canFitFullscreen(currentWidth, currentHeight, baseW, baseH, uiScale)
        if (currentFits) return false

        // 2. Compute landscape screen dimensions
        val landscapeWidth = maxOf(displayWidth, displayHeight)
        val landscapeHeight = minOf(displayWidth, displayHeight)

        // 3. Only recommend landscape if landscape can actually fit it in fullscreen!
        val landscapeFits = canFitFullscreen(landscapeWidth, landscapeHeight, baseW, baseH, uiScale)
        return landscapeFits
    }

    /**
     * Determines whether the given [title] matches a Preferences/Settings window across supported locales.
     */
    fun isPreferencesDialog(
        title: String,
        matchedTitles: Set<String> = emptySet()
    ): Boolean {
        val lower = title.lowercase()
        if (lower.contains("preference") || lower.contains("setting")) return true
        if (matchedTitles.any { lower.contains(it.lowercase()) }) return true
        return DesktopConfigGenerator.FALLBACK_PREFERENCES_TITLES.any { lower.contains(it.lowercase()) }
    }

    /**
     * Resolves the base logical requisition (width × height) for a given window [title].
     */
    fun getWindowMinimumRequisition(
        title: String,
        matchedTitles: Set<String> = emptySet()
    ): Pair<Int, Int>? {
        // Preferences dialogs (e.g. "Xournal++ Preferences", "Preferences", "Einstellungen", "Préférences")
        if (isPreferencesDialog(title, matchedTitles)) {
            return Pair(PREFERENCES_MIN_WIDTH, PREFERENCES_MIN_HEIGHT)
        }

        val lower = title.lowercase()
        // Standard dialogs: Export, Print, Save, Open
        if (lower.contains("export") || lower.contains("print") || lower.contains("save") || lower.contains("open")) {
            return Pair(STANDARD_DIALOG_MIN_WIDTH, STANDARD_DIALOG_MIN_HEIGHT)
        }

        return null
    }
}
