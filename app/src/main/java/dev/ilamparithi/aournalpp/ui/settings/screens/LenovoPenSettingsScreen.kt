package dev.ilamparithi.aournalpp.ui.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonGroup
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonItem
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget

/**
 * OEM-Specific Sub-page: Lenovo Precision Pen / Pen Plus Gesture Mapping.
 *
 * Implements Material 3 Expressive guidelines for:
 * - Diagnostic toast toggles in 24.dp curved containers with corner morphing.
 * - Hardware keycode mapping actions (Single, Double, Triple, Long, Long+Click) via ConnectedButtonGroup.
 * - Momentary hold duration vs Toggle mode bindings with spring recoil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LenovoPenSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    fun saveBooleanPref(key: String, value: Boolean) {
        x11Prefs.edit { putBoolean(key, value) }
        X11Preferences.notifyChanged(context, key)
    }

    fun saveStringPref(key: String, value: String) {
        x11Prefs.edit { putString(key, value) }
        X11Preferences.notifyChanged(context, key)
    }

    var showDetections by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_LENOVO_PEN_SHOW_DETECTIONS, false))
    }
    var showToggleDebug by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS, false))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.pref_input_lenovo_pen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.a11yHeading()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minTouchTarget()) {
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Diagnostics & Toasts Group
            Text(
                text = stringResource(R.string.pref_lenovo_pen_diagnostics_title),
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
                        headline = stringResource(R.string.pref_lenovo_pen_show_detections_title),
                        supporting = stringResource(R.string.pref_lenovo_pen_show_detections_desc),
                        checked = showDetections,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        onCheckedChange = {
                            showDetections = it
                            saveBooleanPref(X11Preferences.KEY_LENOVO_PEN_SHOW_DETECTIONS, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_lenovo_pen_debug_toggle_title),
                        supporting = stringResource(R.string.pref_lenovo_pen_debug_toggle_desc),
                        checked = showToggleDebug,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        onCheckedChange = {
                            showToggleDebug = it
                            saveBooleanPref(X11Preferences.KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS, it)
                        }
                    )
                }
            }

            // Gesture Mappings Group
            Text(
                text = stringResource(R.string.pref_lenovo_pen_gestures_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            val gestures = listOf(
                stringResource(R.string.pref_lenovo_pen_gesture_single) to (X11Preferences.KEY_LENOVO_PEN_SINGLE_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_SINGLE_TOGGLE, X11Preferences.KEY_LENOVO_PEN_SINGLE_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_SINGLE_DURATION)),
                stringResource(R.string.pref_lenovo_pen_gesture_double) to (X11Preferences.KEY_LENOVO_PEN_DOUBLE_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_DOUBLE_TOGGLE, X11Preferences.KEY_LENOVO_PEN_DOUBLE_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_DOUBLE_DURATION)),
                stringResource(R.string.pref_lenovo_pen_gesture_triple) to (X11Preferences.KEY_LENOVO_PEN_TRIPLE_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_TRIPLE_TOGGLE, X11Preferences.KEY_LENOVO_PEN_TRIPLE_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_TRIPLE_DURATION)),
                stringResource(R.string.pref_lenovo_pen_gesture_long) to (X11Preferences.KEY_LENOVO_PEN_LONG_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_LONG_TOGGLE, X11Preferences.KEY_LENOVO_PEN_LONG_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_LONG_DURATION)),
                stringResource(R.string.pref_lenovo_pen_gesture_long_click) to (X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_ACTION to Triple(X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_TOGGLE, X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_OFF_ON_LIFT, X11Preferences.KEY_LENOVO_PEN_LONG_CLICK_DURATION))
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

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = stringResource(R.string.pref_lenovo_pen_map_action),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val actionOptions = listOf(
                            ConnectedButtonItem("disabled", stringResource(R.string.pref_lenovo_pen_action_disabled)),
                            ConnectedButtonItem("2", stringResource(R.string.pref_lenovo_pen_action_btn2)),
                            ConnectedButtonItem("3", stringResource(R.string.pref_lenovo_pen_action_btn3))
                        )
                        ConnectedButtonGroup(
                            items = actionOptions,
                            selectedItem = actionVal,
                            onItemSelected = { value ->
                                actionVal = value
                                saveStringPref(actionKey, value)
                            },
                            showCheckmark = true
                        )

                        val isActionEnabled = actionVal != "disabled"
                        val isHoldDurationEnabled = isActionEnabled && !toggleVal
                        val isOffOnLiftEnabled = isActionEnabled && toggleVal

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        SettingsSwitchListItem(
                            headline = stringResource(R.string.pref_lenovo_pen_toggle_title),
                            supporting = if (toggleVal)
                                stringResource(R.string.pref_lenovo_pen_toggle_on_desc)
                            else
                                stringResource(R.string.pref_lenovo_pen_toggle_off_desc),
                            checked = toggleVal,
                            enabled = isActionEnabled,
                            shape = RoundedCornerShape(12.dp),
                            onCheckedChange = {
                                toggleVal = it
                                saveBooleanPref(toggleKey, it)
                            },
                            modifier = Modifier.alpha(if (isActionEnabled) 1f else 0.38f)
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        SettingsSwitchListItem(
                            headline = stringResource(R.string.pref_lenovo_pen_off_on_lift_title),
                            supporting = stringResource(R.string.pref_lenovo_pen_off_on_lift_desc),
                            checked = offOnLiftVal,
                            enabled = isOffOnLiftEnabled,
                            shape = RoundedCornerShape(12.dp),
                            onCheckedChange = {
                                offOnLiftVal = it
                                saveBooleanPref(offOnLiftKey, it)
                            },
                            modifier = Modifier.alpha(if (isOffOnLiftEnabled) 1f else 0.38f)
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

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
                                    Text(
                                        text = stringResource(R.string.pref_lenovo_pen_duration_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.pref_lenovo_pen_duration_desc),
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
                                            saveStringPref(durationKey, input)
                                        }
                                    },
                                    modifier = Modifier.width(110.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
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
                                            saveStringPref(durationKey, preset)
                                        },
                                        enabled = isHoldDurationEnabled,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.pref_lenovo_pen_duration_preset, preset),
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
