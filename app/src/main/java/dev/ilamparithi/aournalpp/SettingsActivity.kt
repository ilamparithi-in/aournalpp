package dev.ilamparithi.aournalpp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager
import dev.ilamparithi.aournalpp.ui.settings.SettingsNavigationHost
import dev.ilamparithi.aournalpp.ui.settings.SettingsScreen
import dev.ilamparithi.aournalpp.ui.settings.SettingsSubpage
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.ui.theme.AournalppTheme

typealias SettingsSubpage = dev.ilamparithi.aournalpp.ui.settings.SettingsSubpage

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        X11Preferences.initDefaults(this)

        setContent {
            AournalppTheme {
                SettingsNavigationHost(onFinish = { finish() })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val env = LinuxEnvironment(this)
        NotesHomeConfigManager.sync(this, env)
    }
}

@Composable
fun SettingsScreen(onBack: (() -> Unit)? = null) {
    dev.ilamparithi.aournalpp.ui.settings.SettingsScreen(onBack)
}

@Composable
fun SettingsNavigationHost(onFinish: () -> Unit) {
    dev.ilamparithi.aournalpp.ui.settings.SettingsNavigationHost(onFinish)
}

@Composable
fun SettingsSwitchListItem(
    headline: String,
    supporting: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem(
        headline = headline,
        supporting = supporting,
        checked = checked,
        enabled = enabled,
        leadingContent = leadingContent,
        onCheckedChange = onCheckedChange,
        modifier = modifier
    )
}
