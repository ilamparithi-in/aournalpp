package dev.ilamparithi.aournalpp.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level settings categories for the 2-Pane Adaptive Settings Scaffold.
 */
enum class SettingsCategory(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    FILES_STORAGE(
        title = "Files & Storage",
        description = "Notes folder, templates, open actions & recovery",
        icon = Icons.Default.Folder
    ),
    APPEARANCE_CANVAS(
        title = "Appearance & Canvas",
        description = "App theme, GTK canvas theme, wallpaper & animations",
        icon = Icons.Default.Palette
    ),
    TOOLBAR(
        title = "Floating Toolbar",
        description = "Placement, auto-collapse & toolbar controls",
        icon = Icons.Default.Tune
    ),
    INPUT_STYLUS(
        title = "Stylus & Input",
        description = "Touch mode, finger-as-stylus & pen gestures",
        icon = Icons.Default.TouchApp
    ),
    DISPLAY_KEYBOARD(
        title = "Display & Keyboard",
        description = "Canvas UI scale, resolution, safe area & keyboard",
        icon = Icons.Default.DisplaySettings
    ),
    SYSTEM_MAINTENANCE(
        title = "System & Maintenance",
        description = "Language, config backups & diagnostics",
        icon = Icons.Default.Settings
    ),
    ABOUT(
        title = "About",
        description = "Versions, engine info & open source licenses",
        icon = Icons.Default.Info
    )
}
