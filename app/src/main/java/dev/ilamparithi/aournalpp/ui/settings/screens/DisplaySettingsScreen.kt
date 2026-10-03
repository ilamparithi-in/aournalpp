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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonGroup
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonItem
import dev.ilamparithi.aournalpp.ui.settings.components.ExpressiveSlider
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import dev.ilamparithi.aournalpp.ui.settings.components.ValueEditPill
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
    }

    fun saveAournalBoolean(key: String, value: Boolean) {
        aournalPrefs.edit { putBoolean(key, value) }
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
                    title = { Text("Display & Keyboard", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =================================================================
            // 1. Canvas UI Scale Group (GTK Interface Scale)
            // =================================================================
            Text(
                text = "Canvas UI Scale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
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
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "GTK Interface Scale",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Scales toolbars, menus, and canvas controls.",
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
                text = "Resolution Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Display Resolution Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Virtual canvas resolution and rendering scale.",
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
                                        "native" -> "Native: Uses the device's full 1:1 physical pixel resolution for maximum clarity and detail."
                                        "scaled" -> "Scaled: Adjusts resolution by a custom percentage factor relative to display size, optimizing performance."
                                        "exact" -> "Exact: Renders at an industry-standard fixed resolution preset (e.g. 1080p, 1440p) with aspect fitting."
                                        "custom" -> "Custom: Enter exact arbitrary pixel dimensions (Width × Height) for custom or virtual display setups."
                                        else -> "Select how canvas pixel resolution is rendered."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Button Group right below the explanation
                    val resOptions = listOf(
                        ConnectedButtonItem("native", "Native"),
                        ConnectedButtonItem("scaled", "Scaled"),
                        ConnectedButtonItem("exact", "Exact"),
                        ConnectedButtonItem("custom", "Custom")
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
                                        text = "Display Scale Factor",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Scale percentage relative to physical resolution",
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
                                text = "Preset Resolution",
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
                                ExposedDropdownMenu(
                                    expanded = exactExpanded,
                                    onDismissRequest = { exactExpanded = false }
                                ) {
                                    exactOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
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
                                text = "Custom Resolution (WxH)",
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
                                shape = RoundedCornerShape(12.dp),
                                placeholder = { Text("e.g. 1920x1080") }
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
                            text = "Display Filtering Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Texture interpolation filter applied during canvas scaling.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val filterOptions = listOf(
                            ConnectedButtonItem("nearest", "Nearest (Sharp)"),
                            ConnectedButtonItem("bilinear", "Bilinear (Smooth)")
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
                text = "Safe Area Insets & Calibration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val safeCustom = x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_CUSTOM_EDGES, false)
            val safeAll = x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_MARGIN_ALL, 0)
            val safeLeft = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_LEFT, 0) else safeAll
            val safeTop = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_TOP, 0) else safeAll
            val safeRight = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_RIGHT, 0) else safeAll
            val safeBottom = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_BOTTOM, 0) else safeAll

            val summaryText = if (safeCustom) {
                "Custom: L:${safeLeft}dp T:${safeTop}dp R:${safeRight}dp B:${safeBottom}dp"
            } else if (safeAll > 0) {
                "Uniform: $safeAll dp on all edges"
            } else {
                "Full Screen (0 dp)"
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                ListItem(
                    supportingContent = {
                        Text(
                            text = "Adjust margin insets to prevent UI clipping from rounded display corners and camera cutouts ($summaryText).",
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
                            Text("Calibrate")
                        }
                    },
                    colors = androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(
                        text = "Screen Safe Area Calibration",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // =================================================================
            // 4. Screen Idle Timeout & Fullscreen Mode
            // =================================================================
            Text(
                text = "Screen Idle Timeout & Fullscreen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val timeoutOptions = listOf(
                            "never" to "Never (Keep screen on)",
                            "1" to "1 minute",
                            "5" to "5 minutes",
                            "10" to "10 minutes",
                            "20" to "20 minutes",
                            "60" to "1 hour",
                            "system" to "System default"
                        )
                        var timeoutExpanded by remember { mutableStateOf(false) }
                        val currentTimeoutLabel = timeoutOptions.firstOrNull { it.first == idleTimeout }?.second ?: "System default"

                        Text(
                            text = "Screen Idle Timeout",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Duration of screen inactivity before the display goes to sleep while taking notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

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
                            ExposedDropdownMenu(
                                expanded = timeoutExpanded,
                                onDismissRequest = { timeoutExpanded = false }
                            ) {
                                timeoutOptions.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
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

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Fullscreen Canvas",
                        supporting = "Hide status and navigation bars. Disabling allows Android's floating rotation button to appear.",
                        checked = fullscreenCanvas,
                        shape = RectangleShape,
                        onCheckedChange = {
                            fullscreenCanvas = it
                            saveX11Boolean(X11Preferences.KEY_FULLSCREEN, it)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Adjust Resolution for Orientation",
                        supporting = "Automatically swap width and height when device orientation rotates.",
                        checked = adjustRes,
                        shape = RectangleShape,
                        onCheckedChange = {
                            adjustRes = it
                            saveX11Boolean(X11Preferences.KEY_ADJUST_RESOLUTION, it)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Stretch to Fit Display",
                        supporting = "Scale canvas image non-proportionally to eliminate black letterbox bars.",
                        checked = displayStretch,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
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
                text = "Soft Keyboard & IME",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSwitchListItem(
                        headline = "Auto-toggle Keyboard on Focus",
                        supporting = "Automatically open the soft keyboard when tapping into text boxes or canvas annotations.",
                        checked = autoShowIme,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        onCheckedChange = {
                            autoShowIme = it
                            saveAournalBoolean("pref_auto_show_ime_on_focus", it)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Enforce Character-Based Input",
                        supporting = "Directly dispatch committed Unicode characters rather than synthesized hardware key scancodes.",
                        checked = enforceCharBasedInput,
                        shape = RectangleShape,
                        onCheckedChange = {
                            enforceCharBasedInput = it
                            saveX11Boolean(X11Preferences.KEY_ENFORCE_CHAR_BASED_INPUT, it)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Reseed Screen with Soft Keyboard",
                        supporting = "Dynamically adjust X11 screen dimensions when on-screen keyboard appears.",
                        checked = reseedIme,
                        shape = RectangleShape,
                        onCheckedChange = {
                            reseedIme = it
                            saveX11Boolean(X11Preferences.KEY_RESEED, it)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Triple-Tap Close Force Close",
                        supporting = "Tapping the Close button on the floating toolbar or Save All & Close 3 times rapidly brings up a force-close dialog if X11 becomes unresponsive.",
                        checked = tripleBackForceClose,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
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
