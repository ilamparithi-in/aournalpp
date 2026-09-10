package dev.ilamparithi.aournalpp.ui.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.STANDARD_TOOLBAR_PRESETS
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarSettingsScreen(
    onNavigateToPositionEditor: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    var presetId by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_TOOLBAR_POSITION_PRESET, "top_center") ?: "top_center")
    }
    var normX by remember {
        mutableFloatStateOf(x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_X_RATIO, 0.5f))
    }
    var normY by remember {
        mutableFloatStateOf(x11Prefs.getFloat(X11Preferences.KEY_TOOLBAR_POS_Y_RATIO, 0.0f))
    }
    var centerWithinSafeArea by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, false))
    }
    var startCollapsed by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_START_COLLAPSED, false))
    }
    var alwaysShowFileName by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME, false))
    }
    var pinButtonMode by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE, true))
    }
    var autoCollapseTimeoutMs by remember {
        mutableIntStateOf(x11Prefs.getInt(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, 5000))
    }
    var autoCollapseMsText by remember {
        mutableStateOf(autoCollapseTimeoutMs.toString())
    }
    var stylusHoverExpands by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, true))
    }

    var showStylusMode by remember {
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
    var showTitle by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, true))
    }
    var showBack by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_BACK, true))
    }
    var showClose by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, true))
    }
    var showWindowSwitcher by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER, true))
    }
    var showSnapLayouts by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS, true))
    }
    var closeButtonBehavior by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_CLOSE_BUTTON_BEHAVIOR, X11Preferences.CLOSE_BEHAVIOR_FOREGROUND) ?: X11Preferences.CLOSE_BEHAVIOR_FOREGROUND)
    }
    var showKeyboard by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, true))
    }
    var showDragHandle by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, true))
    }
    var showCut by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CUT, true))
    }
    var showCopy by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_COPY, true))
    }
    var showPaste by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, true))
    }
    var showImage by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_IMAGE, true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Floating Toolbar", fontWeight = FontWeight.Bold, modifier = Modifier.a11yHeading()) },
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
            // 1. Toolbar Placement & Positioning
            Text("Placement & Calibration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val presetLabel = if (presetId == "custom") {
                        "Custom (X:${(normX * 100).roundToInt()}%, Y:${(normY * 100).roundToInt()}%)"
                    } else {
                        STANDARD_TOOLBAR_PRESETS.firstOrNull { it.id == presetId }?.label ?: "Top Center"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Toolbar Position", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Position anchor: $presetLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = onNavigateToPositionEditor,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Configure")
                        }
                    }

                    HorizontalDivider()

                    SettingsSwitchListItem(
                        headline = "Confine to Screen Safe Area",
                        supporting = "Align and keep the floating toolbar within calibrated display corner margins.",
                        checked = centerWithinSafeArea,
                        onCheckedChange = {
                            centerWithinSafeArea = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS)
                        }
                    )
                }
            }

            // 2. Startup & Collapse Behavior
            Text("Startup & Collapse Behavior", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Start Collapsed",
                        supporting = "Automatically launch the canvas with the toolbar minimized into a compact pill.",
                        checked = startCollapsed,
                        onCheckedChange = {
                            startCollapsed = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_START_COLLAPSED, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_START_COLLAPSED)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Always Show File Name",
                        supporting = "Keep the active note file name in the toolbar at all times instead of switching to dialog names.",
                        checked = alwaysShowFileName,
                        onCheckedChange = {
                            alwaysShowFileName = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Replace Collapse with Pin / Unpin",
                        supporting = "Tap collapsed toolbar to expand. Unpinned toolbar auto-collapses after inactivity; pin button holds it open.",
                        checked = pinButtonMode,
                        onCheckedChange = {
                            pinButtonMode = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE)
                        }
                    )

                    if (pinButtonMode) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Collapse Inactivity Timeout", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Duration before unpinned toolbar collapses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${autoCollapseTimeoutMs / 1000f} s",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Slider: 1 to 30 seconds in 1-second steps
                            var sliderSeconds by remember(autoCollapseTimeoutMs) {
                                mutableFloatStateOf((autoCollapseTimeoutMs / 1000f).coerceIn(1f, 30f))
                            }
                            Slider(
                                value = sliderSeconds,
                                onValueChange = { newVal ->
                                    val roundedSec = newVal.roundToInt()
                                    sliderSeconds = roundedSec.toFloat()
                                    val newMs = roundedSec * 1000
                                    autoCollapseTimeoutMs = newMs
                                    autoCollapseMsText = newMs.toString()
                                    x11Prefs.edit().putInt(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, newMs).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS)
                                },
                                valueRange = 1f..30f,
                                steps = 28 // 1s increments from 1s to 30s
                            )

                            // Exact timing in milliseconds text field
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Exact Timeout (ms)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "1s = 1000ms (e.g. 3500ms)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = autoCollapseMsText,
                                    onValueChange = { input ->
                                        autoCollapseMsText = input
                                        val parsed = input.toIntOrNull()
                                        if (parsed != null && parsed in 500..60000) {
                                            autoCollapseTimeoutMs = parsed
                                            x11Prefs.edit().putInt(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, parsed).apply()
                                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS)
                                        }
                                    },
                                    modifier = Modifier.width(110.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    placeholder = { Text("5000") }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Expand on Stylus Hover",
                        supporting = "Automatically expand the collapsed toolbar when hovering over it with a stylus pen.",
                        checked = stylusHoverExpands,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (stylusHoverExpands) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            stylusHoverExpands = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS)
                        }
                    )
                }
            }

            // 3. Visible Elements & Action Buttons
            Text("Visible Elements & Shortcuts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Stylus Click Mode Switcher (L/M/R)",
                        supporting = "Displays Left / Middle / Right click toggle buttons directly in the floating toolbar.",
                        checked = showStylusMode,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.alpha(if (showStylusMode) 1f else 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box(modifier = Modifier.size(18.dp, 18.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "L",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Box(modifier = Modifier.size(18.dp, 18.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "M",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(modifier = Modifier.size(18.dp, 18.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "R",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        onCheckedChange = {
                            showStylusMode = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Finger as Stylus Toggle",
                        supporting = "Displays a toolbar button to quickly switch between drawing with your finger as a stylus and standard touch/gesture navigation.",
                        checked = showTouchStylus,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.alpha(if (showTouchStylus) 1f else 0.4f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Draw,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
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

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Window Title & Document Icon",
                        supporting = "Displays note title and dynamic window type icon.",
                        checked = showTitle,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.alpha(if (showTitle) 1f else 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Notes.xopp",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showTitle = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_TITLE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Back Button",
                        supporting = "Displays back arrow button to gracefully save and exit canvas.",
                        checked = showBack,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showBack) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showBack = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_BACK, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_BACK)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Close Button (Ctrl+Q)",
                        supporting = "Displays a red Material 3 close button on the floating toolbar to trigger Xournal++ quit / save prompt.",
                        checked = showClose,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showClose) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showClose = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_CLOSE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Close Button Action Behavior
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Close Button Action", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (closeButtonBehavior == X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL)
                                    "Sequentially closes all open windows"
                                else
                                    "Closes active/foreground window only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = closeButtonBehavior == X11Preferences.CLOSE_BEHAVIOR_FOREGROUND,
                                onClick = {
                                    closeButtonBehavior = X11Preferences.CLOSE_BEHAVIOR_FOREGROUND
                                    x11Prefs.edit().putString(X11Preferences.KEY_CLOSE_BUTTON_BEHAVIOR, X11Preferences.CLOSE_BEHAVIOR_FOREGROUND).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_CLOSE_BUTTON_BEHAVIOR)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Foreground", style = MaterialTheme.typography.labelSmall)
                            }
                            SegmentedButton(
                                selected = closeButtonBehavior == X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL,
                                onClick = {
                                    closeButtonBehavior = X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL
                                    x11Prefs.edit().putString(X11Preferences.KEY_CLOSE_BUTTON_BEHAVIOR, X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_CLOSE_BUTTON_BEHAVIOR)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("All", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Window Switcher",
                        supporting = "Displays Alt+Tab window switcher button. Long press to open the Window Gallery.",
                        checked = showWindowSwitcher,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showWindowSwitcher) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showWindowSwitcher = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Snap Layouts",
                        supporting = "Displays the snap layouts dropdown button on the floating toolbar.",
                        checked = showSnapLayouts,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showSnapLayouts) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showSnapLayouts = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Soft Keyboard Toggle",
                        supporting = "Displays soft keyboard show/hide action button.",
                        checked = showKeyboard,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showKeyboard) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Keyboard,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showKeyboard = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Movable Drag Handle",
                        supporting = "Displays handle to long-press and drag toolbar anywhere.",
                        checked = showDragHandle,
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showDragHandle) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showDragHandle = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Cut Action (Ctrl+X)",
                        supporting = "Displays Cut clipboard action button.",
                        checked = showCut,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showCut) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCut,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showCut = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CUT, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_CUT)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Copy Action (Ctrl+C)",
                        supporting = "Displays Copy clipboard action button.",
                        checked = showCopy,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showCopy) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showCopy = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_COPY, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_COPY)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Paste Action (Ctrl+V)",
                        supporting = "Displays Paste clipboard action button.",
                        checked = showPaste,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(if (showPaste) 1f else 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showPaste = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_PASTE)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Insert Image Action",
                        supporting = "Displays Image action button on the toolbar to insert pictures from Camera, Photos/Gallery, or Files.",
                        checked = showImage,
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showImage = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_TOOLBAR_SHOW_IMAGE, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_SHOW_IMAGE)
                        }
                    )
                }
            }
        }
    }
}
