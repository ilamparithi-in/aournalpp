package dev.ilamparithi.aournalpp.ui.settings.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsIconBadge
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.FloatingToolbarBar
import dev.ilamparithi.aournalpp.ui.STANDARD_TOOLBAR_PRESETS
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonGroup
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonItem
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.ui.settings.components.ValueEditPill
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import kotlin.math.roundToInt

/**
 * Section 3: Floating Toolbar settings screen.
 *
 * Implements Material 3 Expressive overhaul for:
 * 1. Placement & Anchors (Preset anchor ConnectedButtonGroup / Safe area confinement / Visual editor)
 * 2. Collapse & Pin Behavior (Start collapsed, Pin mode, standard Slider timeout + responsive input)
 * 3. Toolbar Items & Actions (Authentic FloatingToolbarLayout preview, functional filter chips, grouped cards)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToolbarSettingsScreen(
    showTopBar: Boolean = true,
    onNavigateToPositionEditor: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    fun saveBooleanPref(key: String, value: Boolean) {
        x11Prefs.edit().putBoolean(key, value).apply()
        X11Preferences.notifyChanged(context, key)
    }

    fun saveStringPref(key: String, value: String) {
        x11Prefs.edit().putString(key, value).apply()
        X11Preferences.notifyChanged(context, key)
    }

    fun saveIntPref(key: String, value: Int) {
        x11Prefs.edit().putInt(key, value).apply()
        X11Preferences.notifyChanged(context, key)
    }

    // 1. Placement & Anchors State
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

    // 2. Collapse & Pin Behavior State
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
    var stylusHoverExpands by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, true))
    }

    // 3. Toolbar Items & Actions State
    var showTitle by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, true))
    }
    var showBack by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_BACK, true))
    }
    var showClose by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, true))
    }
    var closeButtonBehavior by remember {
        mutableStateOf(
            x11Prefs.getString(X11Preferences.KEY_CLOSE_BUTTON_BEHAVIOR, X11Preferences.CLOSE_BEHAVIOR_FOREGROUND)
                ?: X11Preferences.CLOSE_BEHAVIOR_FOREGROUND
        )
    }
    var showWindowSwitcher by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER, true))
    }
    var showSnapLayouts by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS, true))
    }
    var showKeyboard by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, true))
    }
    var showDragHandle by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, true))
    }

    // Stylus items
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

    // Tools & Clipboard items
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

    fun applyPreset(id: String) {
        val preset = STANDARD_TOOLBAR_PRESETS.firstOrNull { it.id == id } ?: return
        presetId = preset.id
        normX = preset.normX
        normY = preset.normY
        x11Prefs.edit()
            .putString(X11Preferences.KEY_TOOLBAR_POSITION_PRESET, preset.id)
            .putFloat(X11Preferences.KEY_TOOLBAR_POS_X_RATIO, preset.normX)
            .putFloat(X11Preferences.KEY_TOOLBAR_POS_Y_RATIO, preset.normY)
            .apply()
        X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_POSITION_PRESET)
        X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_POS_X_RATIO)
        X11Preferences.notifyChanged(context, X11Preferences.KEY_TOOLBAR_POS_Y_RATIO)
    }

    val totalItems = 13
    val enabledItemsCount = listOf(
        showTitle, showBack, showClose, showWindowSwitcher, showSnapLayouts,
        showKeyboard, showDragHandle, showStylusMode, showTouchStylus,
        showCut, showCopy, showPaste, showImage
    ).count { it }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.pref_cat_toolbar),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // =================================================================
            // 1. Placement & Anchors Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_toolbar_placement_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val presetLabel = if (presetId == "custom") {
                        stringResource(
                            R.string.pref_toolbar_anchor_custom,
                            (normX * 100).roundToInt(),
                            (normY * 100).roundToInt()
                        )
                    } else {
                        when (presetId) {
                            "top_left" -> stringResource(R.string.pref_toolbar_anchor_top_left)
                            "top_right" -> stringResource(R.string.pref_toolbar_anchor_top_right)
                            "bottom_left" -> stringResource(R.string.pref_toolbar_anchor_bottom_left)
                            "bottom_center" -> stringResource(R.string.pref_toolbar_anchor_bottom_center)
                            "bottom_right" -> stringResource(R.string.pref_toolbar_anchor_bottom_right)
                            else -> stringResource(R.string.pref_toolbar_anchor_top_center)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_toolbar_preset_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.pref_toolbar_preset_desc, presetLabel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = onNavigateToPositionEditor,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.pref_toolbar_configure))
                        }
                    }
                }
            }

            // =================================================================
            // 2. Collapse & Pin Behavior Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_toolbar_collapse_title),
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
                    // Start Collapsed (first element)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_start_collapsed),
                        supporting = stringResource(R.string.pref_toolbar_start_collapsed_desc),
                        checked = startCollapsed,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        onCheckedChange = {
                            startCollapsed = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_START_COLLAPSED, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Always Show File Name
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_always_show_filename),
                        supporting = stringResource(R.string.pref_toolbar_always_show_filename_desc),
                        checked = alwaysShowFileName,
                        shape = RectangleShape,
                        onCheckedChange = {
                            alwaysShowFileName = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Pin Mode Switch
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_pin_mode),
                        supporting = stringResource(R.string.pref_toolbar_pin_mode_desc),
                        checked = pinButtonMode,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.PushPin,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onCheckedChange = {
                            pinButtonMode = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE, it)
                        }
                    )

                    // Auto-collapse Inactivity Timeout with reverted standard Slider & Input Field
                    AnimatedVisibility(
                        visible = pinButtonMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = stringResource(R.string.pref_toolbar_timeout_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.pref_toolbar_timeout_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isDefault = autoCollapseTimeoutMs == 5000
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
                                                            autoCollapseTimeoutMs = 5000
                                                            saveIntPref(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, 5000)
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
                                                    contentDescription = stringResource(R.string.pref_toolbar_timeout_reset_desc),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    ValueEditPill(
                                        value = autoCollapseTimeoutMs,
                                        onValueChange = { newMs ->
                                            autoCollapseTimeoutMs = newMs
                                            saveIntPref(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, newMs)
                                        },
                                        valueRange = 500..60000,
                                        unitSuffix = stringResource(R.string.unit_milliseconds),
                                        limitToastMessage = stringResource(R.string.toast_toolbar_timeout_limit)
                                    )
                                }
                            }

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
                                    saveIntPref(X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS, newMs)
                                },
                                valueRange = 1f..30f,
                                steps = 28,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Expand on Stylus Hover (last element)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_hover_expand),
                        supporting = stringResource(R.string.pref_toolbar_hover_expand_desc),
                        checked = stylusHoverExpands,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Edit,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.alpha(if (stylusHoverExpands) 1f else 0.4f)
                            )
                        },
                        onCheckedChange = {
                            stylusHoverExpands = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS, it)
                        }
                    )
                }
            }

            // =================================================================
            // 3. Toolbar Items & Actions Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_toolbar_items_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Card 3A: Live Interactive Preview & Quick Toggles
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = stringResource(R.string.pref_toolbar_preview_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.pref_toolbar_active_items_count,
                                    enabledItemsCount,
                                    totalItems
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    showTitle = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, true)
                                    showBack = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_BACK, true)
                                    showClose = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, true)
                                    showWindowSwitcher = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER, true)
                                    showSnapLayouts = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS, true)
                                    showKeyboard = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, true)
                                    showDragHandle = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, true)
                                    showStylusMode = false; saveBooleanPref(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, false)
                                    showTouchStylus = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, true)
                                    showCut = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_CUT, true)
                                    showCopy = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_COPY, true)
                                    showPaste = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, true)
                                    showImage = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_IMAGE, true)
                                }
                            ) {
                                Text(stringResource(R.string.pref_toolbar_reset_items), style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(
                                onClick = {
                                    showTitle = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, true)
                                    showBack = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_BACK, true)
                                    showClose = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, true)
                                    showWindowSwitcher = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER, true)
                                    showSnapLayouts = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS, true)
                                    showKeyboard = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, true)
                                    showDragHandle = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, true)
                                    showStylusMode = true; saveBooleanPref(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, true)
                                    showTouchStylus = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, true)
                                    showCut = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_CUT, true)
                                    showCopy = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_COPY, true)
                                    showPaste = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, true)
                                    showImage = true; saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_IMAGE, true)
                                }
                            ) {
                                Text(stringResource(R.string.pref_toolbar_enable_all), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Live Authentic Floating Toolbar Preview
                    FloatingToolbarPreview(
                        showBack = showBack,
                        showClose = showClose,
                        showWindowSwitcher = showWindowSwitcher,
                        showSnapLayouts = showSnapLayouts,
                        showTitle = showTitle,
                        showStylusMode = showStylusMode,
                        showTouchStylus = showTouchStylus,
                        showCut = showCut,
                        showCopy = showCopy,
                        showPaste = showPaste,
                        showImage = showImage,
                        showKeyboard = showKeyboard,
                        pinButtonMode = pinButtonMode,
                        showDragHandle = showDragHandle
                    )

                    // Interactive Filter Chips Row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = showTitle,
                            onClick = {
                                showTitle = !showTitle
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, showTitle)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_title)) },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showBack,
                            onClick = {
                                showBack = !showBack
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_BACK, showBack)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_back)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showClose,
                            onClick = {
                                showClose = !showClose
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, showClose)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_close)) },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showWindowSwitcher,
                            onClick = {
                                showWindowSwitcher = !showWindowSwitcher
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER, showWindowSwitcher)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_switcher)) },
                            leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showSnapLayouts,
                            onClick = {
                                showSnapLayouts = !showSnapLayouts
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS, showSnapLayouts)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_snap)) },
                            leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showKeyboard,
                            onClick = {
                                showKeyboard = !showKeyboard
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, showKeyboard)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_keyboard)) },
                            leadingIcon = { Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showDragHandle,
                            onClick = {
                                showDragHandle = !showDragHandle
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, showDragHandle)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_drag)) },
                            leadingIcon = { Icon(Icons.Default.DragIndicator, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showTouchStylus,
                            onClick = {
                                showTouchStylus = !showTouchStylus
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS, showTouchStylus)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_finger_stylus)) },
                            leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showStylusMode,
                            onClick = {
                                showStylusMode = !showStylusMode
                                saveBooleanPref(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, showStylusMode)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_stylus_click)) },
                            leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showCut,
                            onClick = {
                                showCut = !showCut
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_CUT, showCut)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_cut)) },
                            leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showCopy,
                            onClick = {
                                showCopy = !showCopy
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_COPY, showCopy)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_copy)) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showPaste,
                            onClick = {
                                showPaste = !showPaste
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, showPaste)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_paste)) },
                            leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showImage,
                            onClick = {
                                showImage = !showImage
                                saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_IMAGE, showImage)
                            },
                            label = { Text(stringResource(R.string.pref_toolbar_chip_image)) },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            // Card 3B: Navigation & Window System Controls
            Text(
                text = stringResource(R.string.pref_toolbar_group_window),
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
                    // Window Title & Document Icon (first element)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_title),
                        supporting = stringResource(R.string.pref_toolbar_item_title_desc),
                        checked = showTitle,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Description,
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                iconTint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onCheckedChange = {
                            showTitle = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_TITLE, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Back Button
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_back),
                        supporting = stringResource(R.string.pref_toolbar_item_back_desc),
                        checked = showBack,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onCheckedChange = {
                            showBack = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_BACK, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Close Button (Ctrl+Q)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_close),
                        supporting = stringResource(R.string.pref_toolbar_item_close_desc),
                        checked = showClose,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Close,
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                iconTint = MaterialTheme.colorScheme.error
                            )
                        },
                        onCheckedChange = {
                            showClose = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_CLOSE, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Close Button Action (Connected Button Group)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.pref_toolbar_close_action),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (closeButtonBehavior == X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL) {
                                stringResource(R.string.pref_toolbar_close_all_desc)
                            } else {
                                stringResource(R.string.pref_toolbar_close_foreground_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val closeActions = listOf(
                            ConnectedButtonItem(
                                X11Preferences.CLOSE_BEHAVIOR_FOREGROUND,
                                stringResource(R.string.pref_toolbar_close_foreground)
                            ),
                            ConnectedButtonItem(
                                X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL,
                                stringResource(R.string.pref_toolbar_close_all)
                            )
                        )
                        ConnectedButtonGroup(
                            items = closeActions,
                            selectedItem = closeButtonBehavior,
                            onItemSelected = { value ->
                                closeButtonBehavior = value
                                saveStringPref(X11Preferences.KEY_CLOSE_BUTTON_BEHAVIOR, value)
                            },
                            showCheckmark = true
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Window Switcher
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_switcher),
                        supporting = stringResource(R.string.pref_toolbar_item_switcher_desc),
                        checked = showWindowSwitcher,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Layers,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        },
                        onCheckedChange = {
                            showWindowSwitcher = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Snap Layouts
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_snap),
                        supporting = stringResource(R.string.pref_toolbar_item_snap_desc),
                        checked = showSnapLayouts,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.GridView,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        },
                        onCheckedChange = {
                            showSnapLayouts = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Soft Keyboard Toggle
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_keyboard),
                        supporting = stringResource(R.string.pref_toolbar_item_keyboard_desc),
                        checked = showKeyboard,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Keyboard,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onCheckedChange = {
                            showKeyboard = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Movable Drag Handle (last element)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_drag),
                        supporting = stringResource(R.string.pref_toolbar_item_drag_desc),
                        checked = showDragHandle,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.DragIndicator,
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                iconTint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onCheckedChange = {
                            showDragHandle = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE, it)
                        }
                    )
                }
            }

            // Card 3C: Stylus & Touch Navigation Controls
            Text(
                text = stringResource(R.string.pref_toolbar_group_stylus),
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
                    // Stylus Click Mode Switcher (L/M/R) (first element)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_stylus_click),
                        supporting = stringResource(R.string.pref_toolbar_item_stylus_click_desc),
                        checked = showStylusMode,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.alpha(if (showStylusMode) 1f else 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.pref_toolbar_stylus_btn_left),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.pref_toolbar_stylus_btn_middle),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.pref_toolbar_stylus_btn_right),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onCheckedChange = {
                            showStylusMode = it
                            saveBooleanPref(X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Finger as Stylus Toggle
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_touch_stylus),
                        supporting = stringResource(R.string.pref_toolbar_item_touch_stylus_desc),
                        checked = showTouchStylus,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Draw,
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                iconTint = MaterialTheme.colorScheme.primary
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

                    // Turn off Touch as Stylus on Hover
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_disable_touch_on_hover),
                        supporting = stringResource(R.string.pref_toolbar_item_disable_touch_on_hover_desc),
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

                    // Remember Last Toggled State (last element)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_remember_finger_state),
                        supporting = stringResource(R.string.pref_toolbar_item_remember_finger_state_desc),
                        checked = rememberFingerAsStylusState,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        onCheckedChange = {
                            rememberFingerAsStylusState = it
                            saveBooleanPref(X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE, it)
                        }
                    )
                }
            }

            // Card 3D: Clipboard & Quick Actions Controls
            Text(
                text = stringResource(R.string.pref_toolbar_group_tools),
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
                    // Cut Action (Ctrl+X) (first element)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_cut),
                        supporting = stringResource(R.string.pref_toolbar_item_cut_desc),
                        checked = showCut,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.ContentCut,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onCheckedChange = {
                            showCut = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_CUT, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Copy Action (Ctrl+C)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_copy),
                        supporting = stringResource(R.string.pref_toolbar_item_copy_desc),
                        checked = showCopy,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.ContentCopy,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onCheckedChange = {
                            showCopy = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_COPY, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Paste Action (Ctrl+V)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_paste),
                        supporting = stringResource(R.string.pref_toolbar_item_paste_desc),
                        checked = showPaste,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.ContentPaste,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onCheckedChange = {
                            showPaste = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_PASTE, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Insert Image Action (last element)
                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_toolbar_item_image),
                        supporting = stringResource(R.string.pref_toolbar_item_image_desc),
                        checked = showImage,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Image,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onCheckedChange = {
                            showImage = it
                            saveBooleanPref(X11Preferences.KEY_TOOLBAR_SHOW_IMAGE, it)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Authentic Floating Toolbar Preview.
 * Uses the canonical [FloatingToolbarBar] renderer in a centered canvas presentation container.
 */
@Composable
private fun FloatingToolbarPreview(
    showBack: Boolean,
    showClose: Boolean,
    showWindowSwitcher: Boolean,
    showSnapLayouts: Boolean,
    showTitle: Boolean,
    showStylusMode: Boolean,
    showTouchStylus: Boolean,
    showCut: Boolean,
    showCopy: Boolean,
    showPaste: Boolean,
    showImage: Boolean,
    showKeyboard: Boolean,
    pinButtonMode: Boolean,
    showDragHandle: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingToolbarBar(
            showBack = showBack,
            showClose = showClose,
            showWindowSwitcher = showWindowSwitcher,
            showSnapLayouts = showSnapLayouts,
            showTitle = showTitle,
            showStylusClickOverride = showStylusMode,
            showTouchStylus = showTouchStylus,
            showCut = showCut,
            showCopy = showCopy,
            showPaste = showPaste,
            showImage = showImage,
            showKeyboard = showKeyboard,
            pinButtonMode = pinButtonMode,
            showDragHandle = showDragHandle
        )
    }
}
