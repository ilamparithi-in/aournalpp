package dev.ilamparithi.aournalpp.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ilamparithi.aournalpp.ui.ScreenSafeAreaEditorScreen
import dev.ilamparithi.aournalpp.ui.ToolbarPositionEditorScreen
import dev.ilamparithi.aournalpp.ui.animation.LocalMotionPreferences
import dev.ilamparithi.aournalpp.ui.settings.screens.DisplaySettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.InputSettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.KeyboardSettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.LenovoPenSettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.MainSettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.screens.ToolbarSettingsScreen

@Composable
fun SettingsScreen(onBack: (() -> Unit)? = null) {
    SettingsNavigationHost(onFinish = { onBack?.invoke() })
}

@Composable
fun SettingsNavigationHost(onFinish: () -> Unit) {
    var currentSubpage by rememberSaveable { mutableStateOf(SettingsSubpage.MAIN) }

    BackHandler(enabled = true) {
        when (currentSubpage) {
            SettingsSubpage.MAIN -> onFinish()
            SettingsSubpage.TOOLBAR -> currentSubpage = SettingsSubpage.MAIN
            SettingsSubpage.TOOLBAR_POSITION_EDITOR -> currentSubpage = SettingsSubpage.TOOLBAR
            SettingsSubpage.LENOVO_PEN -> currentSubpage = SettingsSubpage.INPUT
            SettingsSubpage.SAFE_AREA_EDITOR -> currentSubpage = SettingsSubpage.DISPLAY
            else -> currentSubpage = SettingsSubpage.MAIN
        }
    }

    val reduceAnimations = LocalMotionPreferences.current.reduceAnimations
    AnimatedContent(
        targetState = currentSubpage,
        transitionSpec = {
            if (reduceAnimations) {
                fadeIn(tween(120)) togetherWith fadeOut(tween(100))
            } else if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
            } else {
                slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
            }
        },
        label = "SettingsSubpageTransition"
    ) { targetPage ->
        when (targetPage) {
            SettingsSubpage.MAIN -> MainSettingsScreen(
                onNavigate = { currentSubpage = it },
                onBack = onFinish
            )
            SettingsSubpage.TOOLBAR -> ToolbarSettingsScreen(
                onNavigateToPositionEditor = { currentSubpage = SettingsSubpage.TOOLBAR_POSITION_EDITOR },
                onBack = { currentSubpage = SettingsSubpage.MAIN }
            )
            SettingsSubpage.TOOLBAR_POSITION_EDITOR -> {
                Dialog(
                    onDismissRequest = { currentSubpage = SettingsSubpage.TOOLBAR },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    ToolbarPositionEditorScreen(
                        onNavigateBack = { currentSubpage = SettingsSubpage.TOOLBAR }
                    )
                }
            }
            SettingsSubpage.KEYBOARD -> KeyboardSettingsScreen(
                onBack = { currentSubpage = SettingsSubpage.MAIN }
            )
            SettingsSubpage.INPUT -> InputSettingsScreen(
                onNavigateToLenovoPen = { currentSubpage = SettingsSubpage.LENOVO_PEN },
                onNavigateToToolbar = { currentSubpage = SettingsSubpage.TOOLBAR },
                onBack = { currentSubpage = SettingsSubpage.MAIN }
            )
            SettingsSubpage.LENOVO_PEN -> LenovoPenSettingsScreen(
                onBack = { currentSubpage = SettingsSubpage.INPUT }
            )
            SettingsSubpage.DISPLAY -> DisplaySettingsScreen(
                onNavigateToSafeAreaEditor = { currentSubpage = SettingsSubpage.SAFE_AREA_EDITOR },
                onBack = { currentSubpage = SettingsSubpage.MAIN }
            )
            SettingsSubpage.SAFE_AREA_EDITOR -> {
                Dialog(
                    onDismissRequest = { currentSubpage = SettingsSubpage.DISPLAY },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    ScreenSafeAreaEditorScreen(
                        onNavigateBack = { currentSubpage = SettingsSubpage.DISPLAY }
                    )
                }
            }
        }
    }
}
