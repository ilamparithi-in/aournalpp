package dev.ilamparithi.aournalpp.ui.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getGeneralPrefs(context) }
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    var autoShowIme by remember {
        mutableStateOf(prefs.getBoolean("pref_auto_show_ime_on_focus", true))
    }
    var enforceCharBasedInput by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT, false))
    }
    var tripleBackForceClose by remember {
        mutableStateOf(prefs.getBoolean("pref_triple_back_force_close", true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard & Navigation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Auto-toggle Keyboard on Focus",
                        supporting = "Automatically open the soft keyboard when tapping into text boxes or canvas annotations.",
                        checked = autoShowIme,
                        onCheckedChange = {
                            autoShowIme = it
                            prefs.edit().putBoolean("pref_auto_show_ime_on_focus", it).apply()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Enforce Character-Based Input",
                        supporting = "Directly dispatch committed Unicode characters rather than synthesized hardware key scancodes.",
                        checked = enforceCharBasedInput,
                        onCheckedChange = {
                            enforceCharBasedInput = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Triple-Back Emergency Force Close",
                        supporting = "Pressing Back 3 times rapidly inside Canvas brings up a force-close dialog if X11 becomes unresponsive.",
                        checked = tripleBackForceClose,
                        onCheckedChange = {
                            tripleBackForceClose = it
                            prefs.edit().putBoolean("pref_triple_back_force_close", it).apply()
                        }
                    )
                }
            }
        }
    }
}
