package dev.ilamparithi.aournalpp.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Standardized dialog properties for prompts across the application.
 */
object AppDialogDefaults {
    /**
     * DialogProperties disabling platform default width so [Modifier.promptWidth] can size the prompt.
     */
    val Properties: DialogProperties
        get() = DialogProperties(usePlatformDefaultWidth = false)

    /**
     * Helper to create custom DialogProperties while ensuring [usePlatformDefaultWidth] is false.
     */
    fun properties(
        dismissOnBackPress: Boolean = true,
        dismissOnClickOutside: Boolean = true,
        decorFitsSystemWindows: Boolean = true
    ): DialogProperties = DialogProperties(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = decorFitsSystemWindows
    )
}

/**
 * Standard prompt width modifier:
 * - Portrait: fillMaxWidth with 8dp horizontal padding.
 * - Landscape: widened to 80% width (min 440dp, max 920dp) with 16dp horizontal padding.
 */
@Composable
fun Modifier.promptWidth(): Modifier {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    return this.then(
        if (isLandscape) {
            Modifier
                .widthIn(min = 440.dp, max = 920.dp)
                .fillMaxWidth(0.80f)
                .padding(horizontal = 16.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        }
    )
}
