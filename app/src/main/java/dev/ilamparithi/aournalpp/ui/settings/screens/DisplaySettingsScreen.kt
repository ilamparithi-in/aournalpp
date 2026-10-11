package dev.ilamparithi.aournalpp.ui.settings.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.common.AppDropdownMenuItem
import dev.ilamparithi.aournalpp.ui.common.AppExposedDropdownMenu
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonGroup
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonItem
import dev.ilamparithi.aournalpp.ui.settings.components.ExpressiveSlider
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsIconBadge
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.ui.settings.components.ValueEditPill
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Section 5: Display & Keyboard Settings Screen.
 *
 * Implements Material 3 Expressive guidelines for:
 * 1. Canvas UI Scale (GTK Interface Scale with ExpressiveSlider, ValueEditPill & reset)
 * 2. Resolution Mode & Filtering (ConnectedButtonGroup with dynamic explanation box above)
 * 3. Safe Area Insets & Calibration
 * 4. Screen Idle Timeout & Fullscreen Mode
 * 5. Soft Keyboard & IME (Auto-show on focus, character-based input, reseed)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    showTopBar: Boolean = true,
    onNavigateToSafeAreaEditor: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }
    val aournalPrefs = remember { AppPreferences.getGeneralPrefs(context) }

    fun saveX11Boolean(key: String, value: Boolean) {
        x11Prefs.edit { putBoolean(key, value) }
        X11Preferences.notifyChanged(context, key)
    }

    fun saveX11String(key: String, value: String) {
        x11Prefs.edit { putString(key, value) }
        X11Preferences.notifyChanged(context, key)
    }

    fun saveX11Int(key: String, value: Int) {
        x11Prefs.edit { putInt(key, value) }
        X11Preferences.notifyChanged(context, key)
    }

    fun saveAournalString(key: String, value: String) {
        aournalPrefs.edit { putString(key, value) }
        AppPreferences.notifyChanged(context, key)
    }

    fun saveAournalBoolean(key: String, value: Boolean) {
        aournalPrefs.edit { putBoolean(key, value) }
        AppPreferences.notifyChanged(context, key)
    }

    // 1. Canvas UI Scale State
    var selectedUiScale by remember {
        mutableStateOf(aournalPrefs.getString("pref_ui_scale", "1.0") ?: "1.0")
    }
    var uiScaleFloat by remember {
        mutableFloatStateOf((selectedUiScale.toFloatOrNull() ?: 1.0f).coerceIn(0.5f, 3.0f))
    }

    // 2. Resolution Mode & Filtering State
    var resMode by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_DISPLAY_RES_MODE, "native") ?: "native")
    }
    var displayScale by remember {
        mutableFloatStateOf(x11Prefs.getInt(X11Preferences.KEY_DISPLAY_SCALE, 100).toFloat())
    }
    var exactRes by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_DISPLAY_RES_EXACT, "1280x1024") ?: "1280x1024")
    }
    var customRes by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_DISPLAY_RES_CUSTOM, "1280x1024") ?: "1280x1024")
    }
    var filteringMode by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_DISPLAY_FILTERING, "nearest") ?: "nearest")
    }

    // 3. Screen Layout & Timeout State
    var adjustRes by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_ADJUST_RESOLUTION, false))
    }
    var displayStretch by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_DISPLAY_STRETCH, false))
    }
    var fullscreenCanvas by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_FULLSCREEN, false))
    }
    var idleTimeout by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, "system") ?: "system")
    }

    // 4. Soft Keyboard & IME State
    var autoShowIme by remember {
        mutableStateOf(aournalPrefs.getBoolean("pref_auto_show_ime_on_focus", true))
    }
    var enforceCharBasedInput by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT, false))
    }
    var reseedIme by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_RESEED, false))
    }
    var tripleBackForceClose by remember {
        mutableStateOf(aournalPrefs.getBoolean("pref_triple_back_force_close", true))
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.pref_cat_display_keyboard),
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
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
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
            // 1. Canvas UI Scale Group (GTK Interface Scale)
            // =================================================================
            Text(
                text = stringResource(R.string.pref_display_ui_scale_section),
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
                            modifier = Modifier.weight(1f)
                        ) {
                            SettingsIconBadge(
                                imageVector = Icons.Default.FormatSize,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.pref_display_gtk_scale_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.pref_display_gtk_scale_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isDefault = abs(uiScaleFloat - 1.0f) < 0.01f
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
                                                    Toast.makeText(context, resetPrompt, Toast.LENGTH_SHORT).show()
                                                },
                                                onLongPress = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    uiScaleFloat = 1.0f
                                                    selectedUiScale = "1.0"
                                                    saveAournalString("pref_ui_scale", "1.0")
                                                }
                                            )
                                        }
                                        .semantics { role = Role.Button },
                                    tonalElevation = 1.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.RestartAlt,
                                            contentDescription = stringResource(R.string.pref_ui_scale_reset_desc),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            ValueEditPill(
                                value = uiScaleFloat,
                                onValueChange = { newScale ->
                                    uiScaleFloat = newScale
                                    val str = String.format(Locale.US, "%.2f", newScale).trimEnd('0').trimEnd('.')
                                    val formatted = if (str.contains('.')) str else "$str.0"
                                    selectedUiScale = formatted
                                    saveAournalString("pref_ui_scale", formatted)
                                },
                                valueRange = 0.5f..3.0f,
                                unitSuffix = "x",
                                limitToastMessage = stringResource(R.string.toast_ui_scale_limit)
                            )
                        }
                    }

                    // Material 3 Expressive Standard Slider for UI Scale
                    ExpressiveSlider(
                        value = uiScaleFloat,
                        onValueChange = { newVal ->
                            val rounded = (newVal * 20).roundToInt() / 20f // 0.05 increments
                            uiScaleFloat = rounded
                            val str = String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
                            val formatted = if (str.contains('.')) str else "$str.0"
                            selectedUiScale = formatted
                            saveAournalString("pref_ui_scale", formatted)
                        },
                        valueRange = 0.5f..3.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Scale Presets Connected Button Group (Synced bidirectionally with the slider)
                    val scalePresets: List<ConnectedButtonItem<String?>> = listOf(
                        ConnectedButtonItem("1.0", "1.0x"),
                        ConnectedButtonItem("1.25", "1.25x"),
                        ConnectedButtonItem("1.5", "1.5x"),
                        ConnectedButtonItem("1.75", "1.75x"),
                        ConnectedButtonItem("2.0", "2.0x"),
                        ConnectedButtonItem("2.5", "2.5x")
                    )
                    val matchedPreset = scalePresets.firstOrNull { presetItem ->
                        presetItem.value?.toFloatOrNull()?.let { abs(uiScaleFloat - it) < 0.01f } == true
                    }?.value

                    ConnectedButtonGroup(
                        items = scalePresets,
                        selectedItem = matchedPreset,
                        onItemSelected = { preset ->
                            if (preset != null) {
                                val presetFloat = preset.toFloat()
                                uiScaleFloat = presetFloat
                                selectedUiScale = preset
                                saveAournalString("pref_ui_scale", preset)
                            }
                        },
                        showCheckmark = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // =================================================================
            // 2. Resolution Mode & Filtering Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_display_res_mode_section),
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(
                            imageVector = Icons.Default.AspectRatio,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.pref_display_res_mode_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.pref_display_res_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Explanation Box directly ABOVE the button group
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = resMode,
                            transitionSpec = {
                                (fadeIn() + slideInVertically { it / 3 }).togetherWith(fadeOut() + slideOutVertically { -it / 3 })
                            },
                            label = "ResModeExplanation"
                        ) { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        "native" -> Icons.Default.HighQuality
                                        "scaled" -> Icons.Default.Speed
                                        "exact" -> Icons.Default.AspectRatio
                                        "custom" -> Icons.Default.Tune
                                        else -> Icons.Default.Info
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = when (mode) {
                                        "native" -> stringResource(R.string.pref_display_res_mode_native_desc)
                                        "scaled" -> stringResource(R.string.pref_display_res_mode_scaled_desc)
                                        "exact" -> stringResource(R.string.pref_display_res_mode_exact_desc)
                                        "custom" -> stringResource(R.string.pref_display_res_mode_custom_desc)
                                        else -> stringResource(R.string.pref_display_res_mode_default_desc)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Button Group right below the explanation
                    val resOptions = listOf(
                        ConnectedButtonItem("native", stringResource(R.string.pref_display_res_native)),
                        ConnectedButtonItem("scaled", stringResource(R.string.pref_display_res_scaled)),
                        ConnectedButtonItem("exact", stringResource(R.string.pref_display_res_exact)),
                        ConnectedButtonItem("custom", stringResource(R.string.pref_display_res_custom))
                    )
                    ConnectedButtonGroup(
                        items = resOptions,
                        selectedItem = resMode,
                        onItemSelected = { value ->
                            resMode = value
                            saveX11String(X11Preferences.KEY_DISPLAY_RES_MODE, value)
                        },
                        showCheckmark = false
                    )

                    // Mode-specific configuration controls
                    AnimatedVisibility(
                        visible = resMode == "scaled",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.pref_display_scale_factor_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.pref_display_scale_factor_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isDefault = displayScale.roundToInt() == 100
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
                                                            Toast.makeText(context, resetPrompt, Toast.LENGTH_SHORT).show()
                                                        },
                                                        onLongPress = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            displayScale = 100f
                                                            saveX11Int(X11Preferences.KEY_DISPLAY_SCALE, 100)
                                                        }
                                                    )
                                                }
                                                .semantics { role = Role.Button },
                                            tonalElevation = 1.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.RestartAlt,
                                                    contentDescription = stringResource(R.string.pref_display_scale_reset_desc),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    ValueEditPill(
                                        value = displayScale.roundToInt(),
                                        onValueChange = { newScale ->
                                            displayScale = newScale.toFloat()
                                            saveX11Int(X11Preferences.KEY_DISPLAY_SCALE, newScale)
                                        },
                                        valueRange = 30..300,
                                        unitSuffix = stringResource(R.string.unit_percent),
                                        limitToastMessage = stringResource(R.string.toast_display_scale_limit)
                                    )
                                }
                            }

                            ExpressiveSlider(
                                value = displayScale,
                                onValueChange = { displayScale = it },
                                onValueChangeFinished = {
                                    val rounded = (displayScale / 10).roundToInt() * 10
                                    displayScale = rounded.toFloat()
                                    saveX11Int(X11Preferences.KEY_DISPLAY_SCALE, rounded)
                                },
                                valueRange = 30f..300f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = resMode == "exact",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val exactOptions = listOf(
                            "1280x720", "1280x800", "1280x1024", "1366x768",
                            "1600x900", "1600x1200", "1920x1080", "1920x1200",
                            "2048x1536", "2560x1440", "2560x1600", "3840x2160"
                        )
                        var exactExpanded by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.pref_display_exact_preset_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            ExposedDropdownMenuBox(
                                expanded = exactExpanded,
                                onExpandedChange = { exactExpanded = !exactExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = exactRes,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exactExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                AppExposedDropdownMenu(
                                    expanded = exactExpanded,
                                    onDismissRequest = { exactExpanded = false }
                                ) {
                                    exactOptions.forEach { option ->
                                        AppDropdownMenuItem(
                                            text = { Text(option) },
                                            selected = option == exactRes,
                                            onClick = {
                                                exactRes = option
                                                exactExpanded = false
                                                saveX11String(X11Preferences.KEY_DISPLAY_RES_EXACT, option)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = resMode == "custom",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.pref_display_custom_res_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = customRes,
                                onValueChange = {
                                    customRes = it
                                    if (it.matches(Regex("^[0-9]+x[0-9]+$"))) {
                                        saveX11String(X11Preferences.KEY_DISPLAY_RES_CUSTOM, it)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Ascii,
                                    autoCorrectEnabled = false,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(12.dp),
                                placeholder = { Text(stringResource(R.string.pref_display_custom_res_placeholder)) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Display Filtering Mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.pref_display_filtering_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.pref_display_filtering_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val filterOptions = listOf(
                            ConnectedButtonItem("nearest", stringResource(R.string.pref_display_filtering_nearest)),
                            ConnectedButtonItem("bilinear", stringResource(R.string.pref_display_filtering_bilinear))
                        )
                        ConnectedButtonGroup(
                            items = filterOptions,
                            selectedItem = filteringMode,
                            onItemSelected = { value ->
                                filteringMode = value
                                saveX11String(X11Preferences.KEY_DISPLAY_FILTERING, value)
                            }
                        )
                    }
                }
            }

            // =================================================================
            // 3. Safe Area Insets & Calibration
            // =================================================================
            Text(
                text = stringResource(R.string.pref_display_safe_area_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            val safeCustom = x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_CUSTOM_EDGES, false)
            val safeAll = x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_MARGIN_ALL, 0)
            val safeLeft = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_LEFT, 0) else safeAll
            val safeTop = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_TOP, 0) else safeAll
            val safeRight = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_RIGHT, 0) else safeAll
            val safeBottom = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_BOTTOM, 0) else safeAll

            val summaryText = if (safeCustom) {
                stringResource(R.string.pref_display_safe_area_custom_summary, safeLeft, safeTop, safeRight, safeBottom)
            } else if (safeAll > 0) {
                stringResource(R.string.pref_display_safe_area_uniform_summary, safeAll)
            } else {
                stringResource(R.string.pref_display_safe_area_fullscreen_summary)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                ListItem(
                    leadingContent = {
                        SettingsIconBadge(
                            imageVector = Icons.Default.AspectRatio,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.pref_display_safe_area_desc, summaryText),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        FilledTonalButton(
                            onClick = onNavigateToSafeAreaEditor,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.pref_display_safe_area_calibrate_btn))
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(
                        text = stringResource(R.string.pref_display_safe_area_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // =================================================================
            // 4. Screen Idle Timeout & Fullscreen Mode
            // =================================================================
            Text(
                text = stringResource(R.string.pref_display_idle_fullscreen_section),
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val timeoutOptions = listOf(
                            "never" to stringResource(R.string.pref_display_timeout_never),
                            "1" to stringResource(R.string.pref_display_timeout_1m),
                            "5" to stringResource(R.string.pref_display_timeout_5m),
                            "10" to stringResource(R.string.pref_display_timeout_10m),
                            "20" to stringResource(R.string.pref_display_timeout_20m),
                            "60" to stringResource(R.string.pref_display_timeout_1h),
                            "system" to stringResource(R.string.pref_display_timeout_system)
                        )
                        var timeoutExpanded by remember { mutableStateOf(false) }
                        val currentTimeoutLabel = timeoutOptions.firstOrNull { it.first == idleTimeout }?.second
                            ?: stringResource(R.string.pref_display_timeout_system)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Timer,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.pref_display_idle_timeout_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.pref_display_idle_timeout_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = timeoutExpanded,
                            onExpandedChange = { timeoutExpanded = !timeoutExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentTimeoutLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeoutExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            AppExposedDropdownMenu(
                                expanded = timeoutExpanded,
                                onDismissRequest = { timeoutExpanded = false }
                            ) {
                                timeoutOptions.forEach { (value, label) ->
                                    AppDropdownMenuItem(
                                        text = { Text(label) },
                                        selected = value == idleTimeout,
                                        onClick = {
                                            idleTimeout = value
                                            timeoutExpanded = false
                                            saveX11String(X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, value)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_display_fullscreen_title),
                        supporting = stringResource(R.string.pref_display_fullscreen_desc),
                        checked = fullscreenCanvas,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Fullscreen,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        },
                        onCheckedChange = {
                            fullscreenCanvas = it
                            saveX11Boolean(X11Preferences.KEY_FULLSCREEN, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_display_adjust_res_title),
                        supporting = stringResource(R.string.pref_display_adjust_res_desc),
                        checked = adjustRes,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.ScreenRotation,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onCheckedChange = {
                            adjustRes = it
                            saveX11Boolean(X11Preferences.KEY_ADJUST_RESOLUTION, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_display_stretch_title),
                        supporting = stringResource(R.string.pref_display_stretch_desc),
                        checked = displayStretch,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.FitScreen,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onCheckedChange = {
                            displayStretch = it
                            saveX11Boolean(X11Preferences.KEY_DISPLAY_STRETCH, it)
                        }
                    )
                }
            }

            // =================================================================
            // 5. Soft Keyboard & IME Group
            // =================================================================
            Text(
                text = stringResource(R.string.pref_display_keyboard_ime_section),
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
                        headline = stringResource(R.string.pref_keyboard_auto_show_title),
                        supporting = stringResource(R.string.pref_keyboard_auto_show_desc),
                        checked = autoShowIme,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Keyboard,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        },
                        onCheckedChange = {
                            autoShowIme = it
                            saveAournalBoolean("pref_auto_show_ime_on_focus", it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_keyboard_char_input_title),
                        supporting = stringResource(R.string.pref_keyboard_char_input_desc),
                        checked = enforceCharBasedInput,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.Spellcheck,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        },
                        onCheckedChange = {
                            enforceCharBasedInput = it
                            saveX11Boolean(X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_keyboard_reseed_title),
                        supporting = stringResource(R.string.pref_keyboard_reseed_desc),
                        checked = reseedIme,
                        shape = RectangleShape,
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.ViewCompact,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onCheckedChange = {
                            reseedIme = it
                            saveX11Boolean(X11Preferences.KEY_RESEED, it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    SettingsSwitchListItem(
                        headline = stringResource(R.string.pref_keyboard_triple_close_title),
                        supporting = stringResource(R.string.pref_keyboard_triple_close_desc),
                        checked = tripleBackForceClose,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        leadingContent = {
                            SettingsIconBadge(
                                imageVector = Icons.Default.PowerSettingsNew,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                iconTint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        },
                        onCheckedChange = {
                            tripleBackForceClose = it
                            saveAournalBoolean("pref_triple_back_force_close", it)
                        }
                    )
                }
            }
        }
    }
}
