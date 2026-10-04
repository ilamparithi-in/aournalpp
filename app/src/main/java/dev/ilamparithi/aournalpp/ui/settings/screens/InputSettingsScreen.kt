package dev.ilamparithi.aournalpp.ui.settings.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.ilamparithi.aournalpp.ui.common.AppDropdownMenuItem
import dev.ilamparithi.aournalpp.ui.common.AppExposedDropdownMenu
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsIconBadge
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonGroup
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonItem
import dev.ilamparithi.aournalpp.ui.settings.components.ExpressiveSlider
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.ui.settings.components.ValueEditPill
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import kotlin.math.roundToInt

/**
 * Section 4: Stylus & Input Settings Screen.
 *
 * Implements Material 3 Expressive guidelines for:
 * 1. Touch Mode (ConnectedButtonGroup: Direct 1:1, Trackpad, Simulated)
 * 2. Finger-as-Stylus Behavior (Auto-disable on hover, state persistence)
 * 3. Hardware Stylus & Hover (Mouse mode, contact modifier, hover expand)
 * 4. External Pointer (Speed ExpressiveSlider with editable pill & reset, Rotation)
 * 5. OEM-Specific Pen Settings (Sub-pages for OEM pens: Lenovo Precision Pen / Pen Plus)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputSettingsScreen(
    showTopBar: Boolean = true,
    onNavigateToLenovoPen: () -> Unit,
    onNavigateToToolbar: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    fun saveBooleanPref(key: String, value: Boolean) {
        x11Prefs.edit { putBoolean(key, value) }
        X11Preferences.notifyChanged(context, key)
    }

    fun saveStringPref(key: String, value: String) {
        x11Prefs.edit { putString(key, value) }
        X11Preferences.notifyChanged(context, key)
    }

    fun saveIntPref(key: String, value: Int) {
        x11Prefs.edit { putInt(key, value) }
        X11Preferences.notifyChanged(context, key)
    }

    // 1. Touch Mode State
    var touchMode by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_TOUCH_MODE, "3") ?: "3")
    }

    // 2. Finger as Stylus State
    var showTouchStylus by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, true))
    }
    var disableTouchStylusOnStylusHover by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER, true))
    }
    var rememberFingerAsStylusState by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE, false))
    }

    // 3. Hardware Stylus & Hover State
    var showStylusClickOverride by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, false))
    }
    var stylusHoverExpands by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, true))
    }
    var stylusIsMouse by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_STYLUS_IS_MOUSE, false))
    }
    var stylusButtonContactModifier by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_STYLUS_BUTTON_CONTACT_MODIFIER, false))
    }
    var showMouseHelper by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_SHOW_MOUSE_HELPER, false))
    }
    var scaleTouchpad by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_SCALE_TOUCHPAD, true))
    }
    var tapToMove by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TAP_TO_MOVE, false))
    }
    var ignoreGamepad by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_IGNORE_GAMEPAD_EVENTS, false))
    }

    // 4. External Pointer State
    val initialSpeed = x11Prefs.getInt(X11Preferences.KEY_CAPTURED_POINTER_SPEED, 100)
    var capturedSpeed by remember {
        mutableFloatStateOf(initialSpeed.toFloat())
    }
    var transformCaptured by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_TRANSFORM_CAPTURED_POINTER, "no") ?: "no")
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.pref_cat_input_stylus),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.a11yHeading()
                        )
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack, modifier = Modifier.minTouchTarget()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
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
            // =================================================================
            // 1. Touch Mode Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_input_touch_mode_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(
                            imageVector = Icons.Default.TouchApp,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_input_touch_mode_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.pref_input_touch_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val touchOptions = listOf(
                        ConnectedButtonItem("3", stringResource(R.string.pref_input_touch_mode_direct)),
                        ConnectedButtonItem("1", stringResource(R.string.pref_input_touch_mode_trackpad)),
                        ConnectedButtonItem("2", stringResource(R.string.pref_input_touch_mode_simulated))
                    )
                    ConnectedButtonGroup(
                        items = touchOptions,
                        selectedItem = touchMode,
                        onItemSelected = { value ->
                            touchMode = value
                            saveStringPref(X11Preferences.KEY_TOUCH_MODE, value)
                        },
                        showCheckmark = true
                    )
                }
            }

            // =================================================================
            // 2. Finger-as-Stylus Behavior Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_input_finger_stylus_title),
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_input_show_finger_stylus),
                        supporting = stringResource(R.string.pref_input_show_finger_stylus_desc),
                        checked = showTouchStylus,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Draw,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        },
                        onCheckedChange = {
                            showTouchStylus = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_input_disable_touch_hover),
                        supporting = stringResource(R.string.pref_input_disable_touch_hover_desc),
                        checked = disableTouchStylusOnStylusHover,
                        shape = RectangleShape,
                        onCheckedChange = {
                            disableTouchStylusOnStylusHover = it
                            saveBooleanPref(X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_input_remember_finger_state),
                        supporting = stringResource(R.string.pref_input_remember_finger_state_desc),
                        checked = rememberFingerAsStylusState,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        onCheckedChange = {
                            rememberFingerAsStylusState = it
                            saveBooleanPref(X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE, it)
                        }
                    )
                }
            }

            // =================================================================
            // 3. Hardware Stylus & Hover Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_input_hardware_stylus_title),
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_input_show_stylus_click),
                        supporting = stringResource(R.string.pref_input_show_stylus_click_desc),
                        checked = showStylusClickOverride,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Edit,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        },
                        onCheckedChange = {
                            showStylusClickOverride = it
                            saveBooleanPref(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_input_hover_expand),
                        supporting = stringResource(R.string.pref_input_hover_expand_desc),
                        checked = stylusHoverExpands,
                        shape = RectangleShape,
                        onCheckedChange = {
                            stylusHoverExpands = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_input_stylus_is_mouse),
                        supporting = stringResource(R.string.pref_input_stylus_is_mouse_desc),
                        checked = stylusIsMouse,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Mouse,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onCheckedChange = {
                            stylusIsMouse = it
                            saveBooleanPref(X11Preferences.KEY_STYLUS_IS_MOUSE, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_input_contact_modifier),
                        supporting = stringResource(R.string.pref_input_contact_modifier_desc),
                        checked = stylusButtonContactModifier,
                        shape = if (touchMode == "1") RectangleShape else RectangleShape,
                        onCheckedChange = {
                            stylusButtonContactModifier = it
                            saveBooleanPref(X11Preferences.KEY_STYLUS_BUTTON_CONTACT_MODIFIER, it)
                        }
                    )

                    if (touchMode == "1") {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        SettingsSwitchListItem(
                            headline = stringResource(R.string.pref_input_mouse_helper),
                            supporting = stringResource(R.string.pref_input_mouse_helper_desc),
                            checked = showMouseHelper,
                            shape = RectangleShape,
                            onCheckedChange = {
                                showMouseHelper = it
                                saveBooleanPref(X11Preferences.KEY_SHOW_MOUSE_HELPER, it)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        SettingsSwitchListItem(
                            headline = stringResource(R.string.pref_input_scale_trackpad),
                            supporting = stringResource(R.string.pref_input_scale_trackpad_desc),
                            checked = scaleTouchpad,
                            shape = RectangleShape,
                            onCheckedChange = {
                                scaleTouchpad = it
                                saveBooleanPref(X11Preferences.KEY_SCALE_TOUCHPAD, it)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        SettingsSwitchListItem(
                            headline = stringResource(R.string.pref_input_tap_to_move),
                            supporting = stringResource(R.string.pref_input_tap_to_move_desc),
                            checked = tapToMove,
                            shape = RectangleShape,
                            onCheckedChange = {
                                tapToMove = it
                                saveBooleanPref(X11Preferences.KEY_TAP_TO_MOVE, it)
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_input_ignore_gamepad),
                        supporting = stringResource(R.string.pref_input_ignore_gamepad_desc),
                        checked = ignoreGamepad,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        onCheckedChange = {
                            ignoreGamepad = it
                            saveBooleanPref(X11Preferences.KEY_IGNORE_GAMEPAD_EVENTS, it)
                        }
                    )
                }
            }

            // =================================================================
            // 4. External Pointer Group (Speed WavySlider & Rotation)
            // =================================================================
            Text(
                text = stringResource(R.string.pref_input_external_pointer_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.pref_input_pointer_speed_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.pref_input_pointer_speed_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isDefault = capturedSpeed.roundToInt() == 100
                            val haptic = LocalHapticFeedback.current
                            val resetPrompt = stringResource(R.string.toast_long_press_to_reset)

                            AnimatedVisibility(
                                visible = !isDefault,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    Toast.makeText(
                                                        context,
                                                        resetPrompt,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                onLongPress = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    capturedSpeed = 100f
                                                    saveIntPref(X11Preferences.KEY_CAPTURED_POINTER_SPEED, 100)
                                                }
                                            )
                                        }
                                        .semantics {
                                            role = Role.Button
                                        },
                                    tonalElevation = 1.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.RestartAlt,
                                            contentDescription = stringResource(R.string.pref_input_pointer_speed_reset_desc),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            ValueEditPill(
                                value = capturedSpeed.roundToInt(),
                                onValueChange = { newSpeed ->
                                    capturedSpeed = newSpeed.toFloat()
                                    saveIntPref(X11Preferences.KEY_CAPTURED_POINTER_SPEED, newSpeed)
                                },
                                valueRange = 1..300,
                                unitSuffix = stringResource(R.string.unit_percent),
                                limitToastMessage = stringResource(R.string.toast_pointer_speed_limit)
                            )
                        }
                    }

                    // Material 3 Expressive Standard Slider for Pointer Speed
                    ExpressiveSlider(
                        value = capturedSpeed,
                        onValueChange = {
                            capturedSpeed = it
                        },
                        onValueChangeFinished = {
                            saveIntPref(X11Preferences.KEY_CAPTURED_POINTER_SPEED, capturedSpeed.roundToInt())
                        },
                        valueRange = 1f..300f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    val transformOptions = listOf(
                        "no" to stringResource(R.string.pref_input_rotation_none),
                        "c" to stringResource(R.string.pref_input_rotation_cw90),
                        "cc" to stringResource(R.string.pref_input_rotation_ccw90),
                        "ud" to stringResource(R.string.pref_input_rotation_ud180),
                        "at" to stringResource(R.string.pref_input_rotation_touchpad)
                    )
                    var transformExpanded by remember { mutableStateOf(false) }
                    val currentTransformLabel = transformOptions.firstOrNull { it.first == transformCaptured }?.second
                        ?: stringResource(R.string.pref_input_rotation_none)

                    Text(
                        text = stringResource(R.string.pref_input_pointer_rotation_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

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
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        AppExposedDropdownMenu(
                            expanded = transformExpanded,
                            onDismissRequest = { transformExpanded = false }
                        ) {
                            transformOptions.forEach { (value, label) ->
                                AppDropdownMenuItem(
                                    text = { Text(label) },
                                    selected = value == transformCaptured,
                                    onClick = {
                                        transformCaptured = value
                                        transformExpanded = false
                                        saveStringPref(X11Preferences.KEY_TRANSFORM_CAPTURED_POINTER, value)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 5. OEM-Specific Pen Settings Group (Sub-pages for OEM Pens)
            // =================================================================
            Text(
                text = stringResource(R.string.pref_input_oem_pens_title),
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
                    // Lenovo Precision Pen / Pen Plus Sub-page
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToLenovoPen)
                            .semantics { role = Role.Button },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Gesture,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.pref_input_lenovo_pen_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.pref_input_lenovo_pen_desc),
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
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
