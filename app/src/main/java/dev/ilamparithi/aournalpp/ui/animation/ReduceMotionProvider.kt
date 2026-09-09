package dev.ilamparithi.aournalpp.ui.animation

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment

@Stable
data class MotionPreferences(
    val reduceAnimations: Boolean
)

val LocalMotionPreferences = compositionLocalOf { MotionPreferences(reduceAnimations = false) }

@Composable
fun ProvideMotionPreferences(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    var reduce by remember {
        mutableStateOf(prefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false))
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS) {
                reduce = sp.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val motion = remember(reduce) { MotionPreferences(reduceAnimations = reduce) }
    CompositionLocalProvider(LocalMotionPreferences provides motion) {
        content()
    }
}
