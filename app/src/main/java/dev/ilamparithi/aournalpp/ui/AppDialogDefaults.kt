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

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.compose.ui.window.DialogWindowProvider

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

    /**
     * Finds the host [Window] for a dialog composable from the given [view].
     */
    fun findDialogWindow(view: View): Window? {
        var current: View? = view
        while (current != null) {
            if (current is DialogWindowProvider) return current.window
            val parent = current.parent
            current = parent as? View
        }
        return null
    }

    /**
     * Configures a dialog window for true edge-to-edge rendering across display cutouts and system bars.
     */
    @Suppress("DEPRECATION")
    fun makeDialogFullscreenEdgeToEdge(view: View) {
        val window = findDialogWindow(view) ?: return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        )
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }
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
