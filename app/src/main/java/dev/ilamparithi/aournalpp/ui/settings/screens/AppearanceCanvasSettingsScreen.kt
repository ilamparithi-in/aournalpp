package dev.ilamparithi.aournalpp.ui.settings.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsIconBadge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.WallpaperHelper
import dev.ilamparithi.aournalpp.runtime.XournalConfigManager
import dev.ilamparithi.aournalpp.ui.ConfigViewerDialog
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonGroup
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonItem
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import kotlinx.coroutines.launch

/**
 * Section 2: Appearance & Canvas settings screen.
 * Provides localized Material 3 Expressive controls for:
 * 1. Android App UI Theme (System / Light / Dark)
 * 2. Xournal++ GTK Canvas Theme (System / Adwaita Light / Adwaita Dark)
 * 3. Canvas Backdrop & Wallpaper (System / Custom / Theme)
 * 4. Motion & Animations (Reduce Animations switch)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceCanvasSettingsScreen(
    showTopBar: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val generalPrefs = remember { AppPreferences.getGeneralPrefs(context) }
    val legacyPrefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    val env = remember { LinuxEnvironment(context) }
    val configManager = remember { XournalConfigManager(env) }
    var showConfigViewerDialog by remember { mutableStateOf(false) }

    fun saveStringPref(key: String, value: String) {
        generalPrefs.edit().putString(key, value).apply()
        legacyPrefs.edit().putString(key, value).apply()
    }

    fun saveBooleanPref(key: String, value: Boolean) {
        generalPrefs.edit().putBoolean(key, value).apply()
        legacyPrefs.edit().putBoolean(key, value).apply()
    }

    // 1. Android App Theme State
    var appThemePref by remember {
        mutableStateOf(generalPrefs.getString(LinuxEnvironment.PREF_KEY_APP_THEME, "system") ?: "system")
    }

    // 2. GTK Canvas Theme State
    var gtkThemePref by remember {
        mutableStateOf(generalPrefs.getString(LinuxEnvironment.PREF_KEY_GTK_THEME, "system") ?: "system")
    }

    // 3. Canvas Wallpaper Mode State
    var wallpaperModePref by remember {
        mutableStateOf(WallpaperHelper.getWallpaperMode(context))
    }
    var customWallpaperVersion by remember { mutableIntStateOf(0) }

    // 4. Reduce Animations State
    var reduceAnimationsPref by remember {
        mutableStateOf(generalPrefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false))
    }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val res = WallpaperHelper.saveCustomWallpaper(context, uri)
            if (res.isSuccess) {
                wallpaperModePref = WallpaperHelper.MODE_CUSTOM
                saveStringPref(WallpaperHelper.PREF_KEY_WALLPAPER_MODE, WallpaperHelper.MODE_CUSTOM)
                customWallpaperVersion++
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_custom_wallpaper_applied))
                }
            } else {
                val errorMsg = res.exceptionOrNull()?.message ?: ""
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_wallpaper_error, errorMsg))
                }
            }
        }
    }

    if (showConfigViewerDialog) {
        ConfigViewerDialog(
            configManager = configManager,
            onDismiss = { showConfigViewerDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.pref_cat_appearance_canvas),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.a11yHeading()
                        )
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack, modifier = Modifier.minTouchTarget()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // =================================================================
            // 1. Android App Theme Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_app_theme_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(
                            imageVector = Icons.Default.Palette,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_app_theme_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.pref_app_theme_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val appThemeButtons = listOf(
                        ConnectedButtonItem("system", stringResource(R.string.pref_theme_system)),
                        ConnectedButtonItem("light", stringResource(R.string.pref_theme_light)),
                        ConnectedButtonItem("dark", stringResource(R.string.pref_theme_dark))
                    )
                    ConnectedButtonGroup(
                        items = appThemeButtons,
                        selectedItem = appThemePref,
                        onItemSelected = { value ->
                            appThemePref = value
                            saveStringPref(LinuxEnvironment.PREF_KEY_APP_THEME, value)
                        },
                        showCheckmark = true
                    )
                }
            }

            // =================================================================
            // 2. Xournal++ GTK Canvas Theme Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_gtk_theme_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(
                            imageVector = Icons.Default.Brush,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_gtk_theme_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.pref_gtk_theme_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val gtkThemeButtons = listOf(
                        ConnectedButtonItem("system", stringResource(R.string.pref_theme_system)),
                        ConnectedButtonItem("light", stringResource(R.string.pref_gtk_theme_adwaita_light)),
                        ConnectedButtonItem("dark", stringResource(R.string.pref_gtk_theme_adwaita_dark))
                    )
                    ConnectedButtonGroup(
                        items = gtkThemeButtons,
                        selectedItem = gtkThemePref,
                        onItemSelected = { value ->
                            gtkThemePref = value
                            saveStringPref(LinuxEnvironment.PREF_KEY_GTK_THEME, value)
                            env.writeGtkSettings()
                            val displayLabel = when (value) {
                                "light" -> context.getString(R.string.pref_gtk_theme_adwaita_light)
                                "dark" -> context.getString(R.string.pref_gtk_theme_adwaita_dark)
                                else -> context.getString(R.string.pref_theme_system)
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.msg_gtk_theme_updated, displayLabel))
                            }
                        },
                        showCheckmark = true
                    )
                }
            }

            // =================================================================
            // 3. Canvas Backdrop & Wallpaper Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_wallpaper_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(
                            imageVector = Icons.Default.Wallpaper,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconTint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_wallpaper_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.pref_wallpaper_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val wallpaperButtons = listOf(
                        ConnectedButtonItem(WallpaperHelper.MODE_SYSTEM, stringResource(R.string.pref_theme_system)),
                        ConnectedButtonItem(WallpaperHelper.MODE_CUSTOM, stringResource(R.string.pref_wallpaper_custom)),
                        ConnectedButtonItem(WallpaperHelper.MODE_THEME, stringResource(R.string.pref_wallpaper_theme))
                    )
                    ConnectedButtonGroup(
                        items = wallpaperButtons,
                        selectedItem = wallpaperModePref,
                        onItemSelected = { value ->
                            if (value == WallpaperHelper.MODE_CUSTOM && !WallpaperHelper.getCustomWallpaperFile(context).exists()) {
                                wallpaperPickerLauncher.launch("image/*")
                            } else {
                                wallpaperModePref = value
                                WallpaperHelper.setWallpaperMode(context, value)
                                saveStringPref(WallpaperHelper.PREF_KEY_WALLPAPER_MODE, value)
                                val label = when (value) {
                                    WallpaperHelper.MODE_CUSTOM -> context.getString(R.string.pref_wallpaper_custom_label)
                                    WallpaperHelper.MODE_THEME -> context.getString(R.string.pref_wallpaper_theme_label)
                                    else -> context.getString(R.string.pref_wallpaper_system_label)
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.msg_wallpaper_set, label))
                                }
                            }
                        },
                        showCheckmark = true
                    )

                    if (wallpaperModePref == WallpaperHelper.MODE_CUSTOM) {
                        val customFile = remember(customWallpaperVersion) { WallpaperHelper.getCustomWallpaperFile(context) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { wallpaperPickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(if (customFile.exists()) R.string.pref_wallpaper_change_image else R.string.pref_wallpaper_select_image),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            if (customFile.exists()) {
                                TextButton(
                                    onClick = {
                                        WallpaperHelper.clearCustomWallpaper(context)
                                        wallpaperModePref = WallpaperHelper.MODE_SYSTEM
                                        saveStringPref(WallpaperHelper.PREF_KEY_WALLPAPER_MODE, WallpaperHelper.MODE_SYSTEM)
                                        customWallpaperVersion++
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.msg_wallpaper_reset))
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.pref_wallpaper_reset))
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 4. Motion & Animations Group (Reverted to Reduce Animations switch)
            // =================================================================
            Text(
                text = stringResource(R.string.pref_motion_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_reduce_animations_title),
                        supporting = stringResource(R.string.pref_reduce_animations_desc),
                        checked = reduceAnimationsPref,
                        shape = RoundedCornerShape(24.dp),
                        onCheckedChange = { isChecked ->
                            reduceAnimationsPref = isChecked
                            saveBooleanPref(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, isChecked)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(
                                        if (isChecked) R.string.msg_reduce_animations_enabled
                                        else R.string.msg_reduce_animations_disabled
                                    )
                                )
                            }
                        }
                    )
                }
            }

            // =================================================================
            // 5. Xournal++ Preferences Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_cat_xournal_prefs),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Item 1: Configuration File Inspector
                    AppearanceActionRow(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.pref_inspect_settings_title),
                        description = stringResource(R.string.pref_inspect_settings_desc),
                        onClick = { showConfigViewerDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Item 2: Open Xournal++ Preferences Page
                    AppearanceActionRow(
                        icon = Icons.Default.Tune,
                        title = stringResource(R.string.pref_native_gtk_prefs_title),
                        description = stringResource(R.string.pref_native_gtk_prefs_desc),
                        onClick = {
                            val intent = Intent(context, CanvasActivity::class.java).apply {
                                putExtra(CanvasActivity.EXTRA_OPEN_PREFERENCES, true)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Footnote: Workspace Autoload Safeguard Message
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.pref_autoload_safeguard_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Reusable setting row for Appearance & Canvas subpage actions.
 */
@Composable
private fun AppearanceActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBadge(icon = icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

