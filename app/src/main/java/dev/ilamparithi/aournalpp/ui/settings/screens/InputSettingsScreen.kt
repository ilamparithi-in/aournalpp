package dev.ilamparithi.aournalpp.ui.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputSettingsScreen(
    onNavigateToLenovoPen: () -> Unit,
    onNavigateToToolbar: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    var touchMode by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_TOUCH_MODE, "3") ?: "3")
    }
    var scaleTouchpad by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_SCALE_TOUCHPAD, true))
    }
    var stylusIsMouse by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_STYLUS_IS_MOUSE, false))
    }
    var stylusButtonContactModifier by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_STYLUS_BUTTON_CONTACT_MODIFIER, false))
    }
    var showStylusClickOverride by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, false))
    }
    var showTouchStylus by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, true))
    }
    var disableTouchStylusOnStylusHover by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER, true))
    }
    var rememberFingerAsStylusState by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE, false))
    }
    var stylusHoverExpands by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, true))
    }
    var showMouseHelper by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_SHOW_MOUSE_HELPER, false))
    }
    var tapToMove by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TAP_TO_MOVE, false))
    }
    var ignoreGamepad by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_IGNORE_GAMEPAD_EVENTS, false))
    }
    var capturedSpeed by remember {
        mutableFloatStateOf(x11Prefs.getInt(X11Preferences.KEY_CAPTURED_POINTER_SPEED, 100).toFloat())
    }
    var transformCaptured by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_TRANSFORM_CAPTURED_POINTER, "no") ?: "no")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stylus & Input", fontWeight = FontWeight.Bold, modifier = Modifier.a11yHeading()) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minTouchTarget()) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            // Touch Mode
            Text("Touch Input Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val touchOptions = listOf(
                "3" to "Direct Touch (1:1)",
                "1" to "Trackpad",
                "2" to "Simulated"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                touchOptions.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = touchMode == value,
                        onClick = {
                            touchMode = value
                            x11Prefs.edit().putString(X11Preferences.KEY_TOUCH_MODE, value).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOUCH_MODE)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, touchOptions.size),
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            // Finger as Stylus Section
            Text("Finger as Stylus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Show Finger as Stylus Toggle in Toolbar",
                        supporting = "Displays a toggle button in the floating toolbar to quickly switch between drawing with your finger as a stylus and standard touch navigation.",
                        checked = showTouchStylus,
                        onCheckedChange = {
                            showTouchStylus = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Turn off Touch as Stylus on Stylus Hover",
                        supporting = "Automatically disables finger drawing and restores touch navigation as soon as a physical stylus pen hovers over or touches the screen.",
                        checked = disableTouchStylusOnStylusHover,
                        onCheckedChange = {
                            disableTouchStylusOnStylusHover = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Remember Last Toggled State",
                        supporting = "Preserves whether Finger as Stylus was active across app launches. When disabled, Finger as Stylus resets to off on startup.",
                        checked = rememberFingerAsStylusState,
                        onCheckedChange = {
                            rememberFingerAsStylusState = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE)
                        }
                    )
                }
            }

            // Stylus Controls
            Text("Stylus & Pointer Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Show Stylus Click Mode in Toolbar",
                        supporting = "Displays Left / Middle / Right click toggle capsule directly in the floating toolbar (also configurable in Floating Toolbar settings).",
                        checked = showStylusClickOverride,
                        onCheckedChange = {
                            showStylusClickOverride = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Expand Toolbar on Stylus Hover",
                        supporting = "Automatically expand the collapsed floating toolbar when hovering over it with a stylus pen.",
                        checked = stylusHoverExpands,
                        onCheckedChange = {
                            stylusHoverExpands = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Enable Stylus Mouse Mode",
                        supporting = "Treat hardware stylus touch events as desktop mouse pointer clicks.",
                        checked = stylusIsMouse,
                        onCheckedChange = {
                            stylusIsMouse = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_STYLUS_IS_MOUSE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_STYLUS_IS_MOUSE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Stylus Button Contact Modifier Mode",
                        supporting = "Modify contact properties when the stylus side barrel button is depressed.",
                        checked = stylusButtonContactModifier,
                        onCheckedChange = {
                            stylusButtonContactModifier = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_STYLUS_BUTTON_CONTACT_MODIFIER, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_STYLUS_BUTTON_CONTACT_MODIFIER)
                        }
                    )

                    if (touchMode == "1") {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchListItem(
                            headline = "Show Mouse Click Helper Overlay",
                            supporting = "On-screen Left / Middle / Right floating mouse buttons for trackpad mode.",
                            checked = showMouseHelper,
                            onCheckedChange = {
                                showMouseHelper = it
                                x11Prefs.edit().putBoolean(X11Preferences.KEY_SHOW_MOUSE_HELPER, it).apply()
                                X11Preferences.notifyChanged(context, X11Preferences.KEY_SHOW_MOUSE_HELPER)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchListItem(
                            headline = "Scale Trackpad to Display Factor",
                            supporting = "Scale cursor movement speed according to display resolution.",
                            checked = scaleTouchpad,
                            onCheckedChange = {
                                scaleTouchpad = it
                                x11Prefs.edit().putBoolean(X11Preferences.KEY_SCALE_TOUCHPAD, it).apply()
                                X11Preferences.notifyChanged(context, X11Preferences.KEY_SCALE_TOUCHPAD)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchListItem(
                            headline = "Enable Tap-to-Move",
                            supporting = "Tap and drag to move pointer without holding physical clicks.",
                            checked = tapToMove,
                            onCheckedChange = {
                                tapToMove = it
                                x11Prefs.edit().putBoolean(X11Preferences.KEY_TAP_TO_MOVE, it).apply()
                                X11Preferences.notifyChanged(context, X11Preferences.KEY_TAP_TO_MOVE)
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchListItem(
                        headline = "Ignore Gamepad Events",
                        supporting = "Suppress controller joystick and button input events from driving pointer.",
                        checked = ignoreGamepad,
                        onCheckedChange = {
                            ignoreGamepad = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_IGNORE_GAMEPAD_EVENTS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_IGNORE_GAMEPAD_EVENTS)
                        }
                    )
                }
            }

            // External Pointer Speed & Rotation
            Text("External Captured Pointer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pointer Speed Factor", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${capturedSpeed.roundToInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = capturedSpeed,
                        onValueChange = { capturedSpeed = it },
                        onValueChangeFinished = {
                            x11Prefs.edit().putInt(X11Preferences.KEY_CAPTURED_POINTER_SPEED, capturedSpeed.roundToInt()).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_CAPTURED_POINTER_SPEED)
                        },
                        valueRange = 1f..300f
                    )

                    HorizontalDivider()

                    val transformOptions = listOf(
                        "no" to "No Rotation",
                        "c" to "Clockwise 90°",
                        "cc" to "Counter-Clockwise 90°",
                        "ud" to "Upside Down 180°",
                        "at" to "Automatic for Touchpad"
                    )
                    var transformExpanded by remember { mutableStateOf(false) }
                    val currentTransformLabel = transformOptions.firstOrNull { it.first == transformCaptured }?.second ?: "No Rotation"

                    Text("Pointer Movement Rotation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    ExposedDropdownMenuBox(
                        expanded = transformExpanded,
                        onExpandedChange = { transformExpanded = !transformExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentTransformLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transformExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = transformExpanded,
                            onDismissRequest = { transformExpanded = false }
                        ) {
                            transformOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        transformCaptured = value
                                        transformExpanded = false
                                        x11Prefs.edit().putString(X11Preferences.KEY_TRANSFORM_CAPTURED_POINTER, value).apply()
                                        X11Preferences.notifyChanged(context, X11Preferences.KEY_TRANSFORM_CAPTURED_POINTER)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Lenovo Pen Mapping Sub-page Tile
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Lenovo Pen Button Gesture Mapping", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Configure single/double/triple/long click shortcuts for Lenovo stylus barrel buttons.") },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Gesture, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                    modifier = Modifier.clickable { onNavigateToLenovoPen() }
                )
            }
        }
    }
}
