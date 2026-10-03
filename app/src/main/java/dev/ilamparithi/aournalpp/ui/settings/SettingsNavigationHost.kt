package dev.ilamparithi.aournalpp.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.ui.LicensesScreen
import dev.ilamparithi.aournalpp.ui.ScreenSafeAreaEditorScreen
import dev.ilamparithi.aournalpp.ui.ToolbarPositionEditorScreen
import dev.ilamparithi.aournalpp.ui.animation.LocalMotionPreferences
import dev.ilamparithi.aournalpp.ui.animation.SpringSlideTransition
import dev.ilamparithi.aournalpp.ui.settings.screens.AppearanceCanvasSettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.DisplaySettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.FilesStorageSettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.InputSettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.KeyboardSettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.LenovoPenSettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.SystemMaintenanceSettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.ToolbarSettingsScreen
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget

object SettingsTransitionHelper {
    fun isForwardCategoryTransition(
        initial: SettingsCategory,
        target: SettingsCategory
    ): Boolean = target.ordinal > initial.ordinal

    fun isForwardCompactTransition(
        initial: SettingsCategory?,
        target: SettingsCategory?
    ): Boolean = when {
        initial == null && target != null -> true
        initial != null && target == null -> false
        initial != null && target != null -> target.ordinal > initial.ordinal
        else -> true
    }
}

@Composable
fun SettingsScreen(onBack: (() -> Unit)? = null) {
    SettingsNavigationHost(onFinish = { onBack?.invoke() })
}

/**
 * Material 3 Adaptive Settings Navigation Host.
 * Supports:
 * - Wide screens (>= 720dp): Adaptive Two-Pane Layout (Master Category Pane + Detail Pane)
 * - Compact screens (< 720dp): Push-pop single pane with predictive back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNavigationHost(onFinish: () -> Unit) {
    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.FILES_STORAGE) }
    var activeCompactCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
    var activeDialogSubpage by rememberSaveable { mutableStateOf<SettingsSubpage?>(null) }

    val reduceAnimations = LocalMotionPreferences.current.reduceAnimations

    var lastCategorySwitchTime by remember { mutableLongStateOf(0L) }
    var isRapidCategorySwitch by remember { mutableStateOf(false) }

    val onCategorySelect: (SettingsCategory) -> Unit = { category ->
        val currentTime = System.currentTimeMillis()
        if (selectedCategory != category) {
            val delta = currentTime - lastCategorySwitchTime
            isRapidCategorySwitch = lastCategorySwitchTime > 0L && delta < 320L
            lastCategorySwitchTime = currentTime
            selectedCategory = category
        }
    }

    var lastCompactCategorySwitchTime by remember { mutableLongStateOf(0L) }
    var isRapidCompactSwitch by remember { mutableStateOf(false) }

    val onCompactCategorySelect: (SettingsCategory?) -> Unit = { category ->
        val currentTime = System.currentTimeMillis()
        if (activeCompactCategory != category) {
            val delta = currentTime - lastCompactCategorySwitchTime
            isRapidCompactSwitch = lastCompactCategorySwitchTime > 0L && delta < 320L
            lastCompactCategorySwitchTime = currentTime
            activeCompactCategory = category
        }
    }

    // Back handling
    BackHandler(enabled = true) {
        when {
            activeDialogSubpage != null -> activeDialogSubpage = null
            activeCompactCategory != null -> onCompactCategorySelect(null)
            else -> onFinish()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= 720.dp

        if (isTwoPane) {
            // =================================================================
            // Wide Screen: Two-Pane Adaptive Scaffold
            // =================================================================
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Master Pane: Categories List
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp)
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.settings_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.a11yHeading()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onFinish, modifier = Modifier.minTouchTarget()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SettingsCategory.entries.forEach { category ->
                            val isSelected = selectedCategory == category
                            CategoryNavTile(
                                category = category,
                                isSelected = isSelected,
                                onClick = { onCategorySelect(category) }
                            )
                        }
                    }
                }

                VerticalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxHeight()
                )

                // Right Detail Pane
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val detailTransitionSpec: androidx.compose.animation.AnimatedContentTransitionScope<SettingsCategory>.() -> androidx.compose.animation.ContentTransform = {
                        val isForward = SettingsTransitionHelper.isForwardCategoryTransition(initialState, targetState)
                        SpringSlideTransition.createSpec<SettingsCategory>(
                            isForward = isForward,
                            reduceAnimations = reduceAnimations,
                            isRapid = isRapidCategorySwitch
                        )(this)
                    }

                    AnimatedContent(
                        targetState = selectedCategory,
                        transitionSpec = detailTransitionSpec,
                        label = "SettingsDetailTransition"
                    ) { targetCategory ->
                        DetailPaneContent(
                            category = targetCategory,
                            showTopBar = true,
                            onBack = null,
                            onNavigateDialog = { activeDialogSubpage = it },
                            onSelectCategory = onCategorySelect
                        )
                    }
                }
            }
        } else {
            // =================================================================
            // Compact Screen: Single-Pane Push-Pop Navigation
            // =================================================================
            val compactTransitionSpec: androidx.compose.animation.AnimatedContentTransitionScope<SettingsCategory?>.() -> androidx.compose.animation.ContentTransform = {
                val isForward = SettingsTransitionHelper.isForwardCompactTransition(initialState, targetState)
                SpringSlideTransition.createSpec<SettingsCategory?>(
                    isForward = isForward,
                    reduceAnimations = reduceAnimations,
                    isRapid = isRapidCompactSwitch
                )(this)
            }

            AnimatedContent(
                targetState = activeCompactCategory,
                transitionSpec = compactTransitionSpec,
                label = "CompactSettingsTransition"
            ) { currentCategory ->
                if (currentCategory == null) {
                    // Category Selection List
                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(R.string.settings_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.a11yHeading()
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = onFinish, modifier = Modifier.minTouchTarget()) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.action_back)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    ) { padding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SettingsCategory.entries.forEach { category ->
                                CompactCategoryRow(
                                    category = category,
                                    onClick = { onCompactCategorySelect(category) }
                                )
                            }
                        }
                    }
                } else {
                    // Category Detail Screen
                    DetailPaneContent(
                        category = currentCategory,
                        showTopBar = true,
                        onBack = { onCompactCategorySelect(null) },
                        onNavigateDialog = { activeDialogSubpage = it },
                        onSelectCategory = { onCompactCategorySelect(it) }
                    )
                }
            }
        }
    }

    // Modal Fullscreen Editors
    when (activeDialogSubpage) {
        SettingsSubpage.TOOLBAR_POSITION_EDITOR -> {
            Dialog(
                onDismissRequest = { activeDialogSubpage = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                ToolbarPositionEditorScreen(
                    onNavigateBack = { activeDialogSubpage = null }
                )
            }
        }
        SettingsSubpage.SAFE_AREA_EDITOR -> {
            Dialog(
                onDismissRequest = { activeDialogSubpage = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                ScreenSafeAreaEditorScreen(
                    onNavigateBack = { activeDialogSubpage = null }
                )
            }
        }
        SettingsSubpage.LENOVO_PEN -> {
            LenovoPenSettingsScreen(
                onBack = { activeDialogSubpage = null }
            )
        }
        else -> {}
    }
}

@Composable
private fun CategoryNavTile(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CompactCategoryRow(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DetailPaneContent(
    category: SettingsCategory,
    showTopBar: Boolean,
    onBack: (() -> Unit)?,
    onNavigateDialog: (SettingsSubpage) -> Unit,
    onSelectCategory: (SettingsCategory) -> Unit
) {
    when (category) {
        SettingsCategory.FILES_STORAGE -> {
            FilesStorageSettingsScreen(
                showTopBar = showTopBar,
                onBack = onBack
            )
        }
        SettingsCategory.TOOLBAR -> {
            ToolbarSettingsScreen(
                showTopBar = showTopBar,
                onNavigateToPositionEditor = { onNavigateDialog(SettingsSubpage.TOOLBAR_POSITION_EDITOR) },
                onBack = onBack
            )
        }
        SettingsCategory.INPUT_STYLUS -> {
            InputSettingsScreen(
                showTopBar = showTopBar,
                onNavigateToLenovoPen = { onNavigateDialog(SettingsSubpage.LENOVO_PEN) },
                onNavigateToToolbar = { onSelectCategory(SettingsCategory.TOOLBAR) },
                onBack = onBack
            )
        }
        SettingsCategory.DISPLAY_KEYBOARD -> {
            DisplaySettingsScreen(
                showTopBar = showTopBar,
                onNavigateToSafeAreaEditor = { onNavigateDialog(SettingsSubpage.SAFE_AREA_EDITOR) },
                onBack = onBack
            )
        }
        SettingsCategory.APPEARANCE_CANVAS -> {
            AppearanceCanvasSettingsScreen(
                showTopBar = showTopBar,
                onBack = onBack
            )
        }
        SettingsCategory.SYSTEM_MAINTENANCE -> {
            SystemMaintenanceSettingsScreen(
                showTopBar = showTopBar,
                onBack = onBack
            )
        }
        SettingsCategory.ABOUT -> {
            LicensesScreen(
                onBack = onBack
            )
        }
    }
}
