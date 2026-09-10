package dev.ilamparithi.aournalpp.ui.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LenovoPenSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    var showDetections by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_LENOVO_PEN_SHOW_DETECTIONS, false))
    }
    var showToggleDebug by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS, false))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lenovo Pen Mapping", fontWeight = FontWeight.Bold) },
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
                        headline = "Show Detection Toasts",
                        supporting = "Display transient toast messages when barrel button gestures are detected.",
                        checked = showDetections,
                        onCheckedChange = {
                            showDetections = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_LENOVO_PEN_SHOW_DETECTIONS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_LENOVO_PEN_SHOW_DETECTIONS)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchListItem(
                        headline = "Show Toggle Debug Toasts",
                        supporting = "Display state toasts when toggle mode button states change.",
                        checked = showToggleDebug,
                        onCheckedChange = {
                            showToggleDebug = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS)
                        }
                    )
                }
            }

            val gestures = listOf(
                "Single Press (Keycode 600)" to (X11Preferences.KEY_LENOVO_PEN_SINGLE_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_SINGLE_TOGGLE, X11Preferences.KEY_LENOVO_PEN_SINGLE_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_SINGLE_DURATION)),
                "Double Press (Keycode 601)" to (X11Preferences.KEY_LENOVO_PEN_DOUBLE_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_DOUBLE_TOGGLE, X11Preferences.KEY_LENOVO_PEN_DOUBLE_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_DOUBLE_DURATION)),
                "Triple Press (Keycode 602)" to (X11Preferences.KEY_LENOVO_PEN_TRIPLE_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_TRIPLE_TOGGLE, X11Preferences.KEY_LENOVO_PEN_TRIPLE_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_TRIPLE_DURATION)),
                "Long Press (Keycode 603)" to (X11Preferences.KEY_LENOVO_PEN_LONG_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_LONG_TOGGLE, X11Preferences.KEY_LENOVO_PEN_LONG_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_LONG_DURATION)),
                "Long Press and Click (Keycode 604)" to (X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_TOGGLE, X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_DURATION))
            )

            gestures.forEach { (title, keys) ->
                val (actionKey, extraKeys) = keys
                val (toggleKey, offOnLiftKey, durationKey) = extraKeys

                var actionVal by remember {
                    mutableStateOf(x11Prefs.getString(actionKey, "disabled") ?: "disabled")
                }
                var toggleVal by remember {
                    mutableStateOf(x11Prefs.getBoolean(toggleKey, false))
                }
                var offOnLiftVal by remember {
                    mutableStateOf(x11Prefs.getBoolean(offOnLiftKey, false))
                }
                var durationVal by remember {
                    mutableStateOf(x11Prefs.getString(durationKey, "150") ?: "150")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Map To Action:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val actionOptions = listOf(
                            "disabled" to "Disabled",
                            "2" to "Primary (Button 2)",
                            "3" to "Secondary (Button 3)"
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            actionOptions.forEachIndexed { index, (value, label) ->
                                SegmentedButton(
                                    selected = actionVal == value,
                                    onClick = {
                                        actionVal = value
                                        x11Prefs.edit().putString(actionKey, value).apply()
                                        X11Preferences.notifyChanged(context, actionKey)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index, actionOptions.size),
                                    label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }

                        val isActionEnabled = actionVal != "disabled"
                        val isHoldDurationEnabled = isActionEnabled && !toggleVal
                        val isOffOnLiftEnabled = isActionEnabled && toggleVal

                        HorizontalDivider()

                        SettingsSwitchListItem(
                            headline = "Toggle Target Button",
                            supporting = if (toggleVal)
                                "On: toggle mode — gesture once for down and again for up."
                            else
                                "Off: momentary mode — press and hold target for specified duration.",
                            checked = toggleVal,
                            enabled = isActionEnabled,
                            onCheckedChange = {
                                toggleVal = it
                                x11Prefs.edit().putBoolean(toggleKey, it).apply()
                                X11Preferences.notifyChanged(context, toggleKey)
                            },
                            modifier = Modifier.alpha(if (isActionEnabled) 1f else 0.38f)
                        )

                        HorizontalDivider()

                        SettingsSwitchListItem(
                            headline = "Toggle OFF on Lift",
                            supporting = "When toggled ON and pen lifts after contact, release the mapped button automatically.",
                            checked = offOnLiftVal,
                            enabled = isOffOnLiftEnabled,
                            onCheckedChange = {
                                offOnLiftVal = it
                                x11Prefs.edit().putBoolean(offOnLiftKey, it).apply()
                                X11Preferences.notifyChanged(context, offOnLiftKey)
                            },
                            modifier = Modifier.alpha(if (isOffOnLiftEnabled) 1f else 0.38f)
                        )

                        HorizontalDivider()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isHoldDurationEnabled) 1f else 0.38f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Hold Duration (ms)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Applied when Toggle mode is OFF (10 - 8192 ms).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = durationVal,
                                    enabled = isHoldDurationEnabled,
                                    onValueChange = { input ->
                                        durationVal = input
                                        val num = input.toLongOrNull()
                                        if (num != null && num in 10..8192) {
                                            x11Prefs.edit().putString(durationKey, input).apply()
                                            X11Preferences.notifyChanged(context, durationKey)
                                        }
                                    },
                                    modifier = Modifier.width(110.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            val durationPresets = listOf("50", "150", "300", "500", "1000")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                durationPresets.forEach { preset ->
                                    val isSelected = durationVal == preset
                                    OutlinedButton(
                                        onClick = {
                                            durationVal = preset
                                            x11Prefs.edit().putString(durationKey, preset).apply()
                                            X11Preferences.notifyChanged(context, durationKey)
                                        },
                                        enabled = isHoldDurationEnabled,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${preset}ms",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected && isHoldDurationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
