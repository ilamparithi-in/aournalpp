package dev.ilamparithi.aournalpp.ui.shell

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import dev.ilamparithi.aournalpp.R

/**
 * Top-level application navigation tabs.
 */
enum class AppTab(
    @param:StringRes val titleRes: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    WORKSPACE(R.string.tab_workspace, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    HOME(R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    FILES(R.string.tab_files, Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy),
    CLOUD(R.string.tab_cloud, Icons.Filled.Cloud, Icons.Outlined.Cloud),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
    ABOUT(R.string.tab_about, Icons.Filled.Info, Icons.Outlined.Info);

    val id: Int get() = ordinal
}
