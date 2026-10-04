package dev.ilamparithi.aournalpp.ui.settings

enum class SettingsSubpage {
    MAIN,
    TOOLBAR,
    TOOLBAR_POSITION_EDITOR,
    KEYBOARD,
    INPUT,
    LENOVO_PEN,
    DISPLAY,
    SAFE_AREA_EDITOR,
    LOG_MANAGER;

    val isFullPage: Boolean
        get() = when (this) {
            LOG_MANAGER, LENOVO_PEN -> true
            else -> false
        }
}
