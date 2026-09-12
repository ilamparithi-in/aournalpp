package dev.ilamparithi.aournalpp.ui.settings.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsSwitchListItem
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    onNavigateToSafeAreaEditor: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val x11Prefs = remember { X11Preferences.getPrefs(context) }
    val aournalPrefs = remember { AppPreferences.getGeneralPrefs(context) }

    var selectedUiScale by remember {
        mutableStateOf(aournalPrefs.getString("pref_ui_scale", "1.0") ?: "1.0")
    }
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
    var adjustRes by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_ADJUST_RESOLUTION, false))
    }
    var displayStretch by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_DISPLAY_STRETCH, false))
    }
    var reseedIme by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_RESEED, false))
    }
    var fullscreenCanvas by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_FULLSCREEN, false))
    }
    var idleTimeout by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, "system") ?: "system")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display & Resolution", fontWeight = FontWeight.Bold) },
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
            // Canvas UI Scale (Discrete Slider + Custom Fractional Field)
            var uiScaleFloat by remember {
                mutableFloatStateOf((selectedUiScale.toFloatOrNull() ?: 1.0f).coerceIn(0.5f, 3.0f))
            }
            var customScaleText by remember {
                mutableStateOf(selectedUiScale)
            }

            Text("Canvas UI Scale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GTK Interface Scale", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Scales toolbars, menus, and canvas controls.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.2f", uiScaleFloat)}x",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Slider(
                        value = uiScaleFloat,
                        onValueChange = { newVal ->
                            val rounded = (newVal * 20).roundToInt() / 20f // Discrete step of 0.05
                            uiScaleFloat = rounded
                            val str = String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
                            val formatted = if (str.contains('.')) str else "$str.0"
                            selectedUiScale = formatted
                            customScaleText = formatted
                            aournalPrefs.edit().putString("pref_ui_scale", formatted).apply()
                        },
                        valueRange = 0.5f..3.0f,
                        steps = 49 // 0.05 increments between 0.5 and 3.0
                    )

                    // Preset Chips
                    val scalePresets = listOf("1.0", "1.25", "1.5", "1.75", "2.0", "2.5")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        scalePresets.forEach { preset ->
                            val presetFloat = preset.toFloat()
                            val isSelected = Math.abs(uiScaleFloat - presetFloat) < 0.01f
                            OutlinedButton(
                                onClick = {
                                    uiScaleFloat = presetFloat
                                    selectedUiScale = preset
                                    customScaleText = preset
                                    aournalPrefs.edit().putString("pref_ui_scale", preset).apply()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${preset}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Custom Fractional Scale", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Enter exact decimal scale (0.50 – 4.00)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = customScaleText,
                            onValueChange = { input ->
                                customScaleText = input
                                val parsed = input.toFloatOrNull()
                                if (parsed != null && parsed in 0.5f..4.0f) {
                                    selectedUiScale = input
                                    uiScaleFloat = parsed.coerceIn(0.5f, 3.0f)
                                    aournalPrefs.edit().putString("pref_ui_scale", input).apply()
                                }
                            },
                            modifier = Modifier.width(110.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("1.33") }
                        )
                    }
                }
            }

            // Display Resolution Mode
            Text("Resolution Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val resOptions = listOf(
                "native" to "Native",
                "scaled" to "Scaled",
                "exact" to "Exact",
                "custom" to "Custom"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                resOptions.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = resMode == value,
                        onClick = {
                            resMode = value
                            x11Prefs.edit().putString(X11Preferences.KEY_DISPLAY_RES_MODE, value).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_RES_MODE)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, resOptions.size),
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (resMode) {
                        "scaled" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Display Scale Factor", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${displayScale.roundToInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = displayScale,
                                onValueChange = { displayScale = it },
                                onValueChangeFinished = {
                                    val rounded = (displayScale / 10).roundToInt() * 10
                                    displayScale = rounded.toFloat()
                                    x11Prefs.edit().putInt(X11Preferences.KEY_DISPLAY_SCALE, rounded).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_SCALE)
                                },
                                valueRange = 30f..300f,
                                steps = 26
                            )
                        }
                        "exact" -> {
                            val exactOptions = listOf(
                                "1280x720", "1280x800", "1280x1024", "1366x768",
                                "1600x900", "1600x1200", "1920x1080", "1920x1200",
                                "2048x1536", "2560x1440", "2560x1600", "3840x2160"
                            )
                            var exactExpanded by remember { mutableStateOf(false) }

                            Text("Preset Resolution", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
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
                                                x11Prefs.edit().putString(X11Preferences.KEY_DISPLAY_RES_EXACT, option).apply()
                                                X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_RES_EXACT)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        "custom" -> {
                            Text("Custom Resolution (WxH)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = customRes,
                                onValueChange = {
                                    customRes = it
                                    if (it.matches(Regex("^[0-9]+x[0-9]+$"))) {
                                        x11Prefs.edit().putString(X11Preferences.KEY_DISPLAY_RES_CUSTOM, it).apply()
                                        X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_RES_CUSTOM)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("e.g. 1920x1080") }
                            )
                        }
                        else -> {
                            Text("Using device's full 1:1 physical pixel resolution.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider()

                    Text("Display Filtering Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val filterOptions = listOf(
                        "nearest" to "Nearest (Sharp)",
                        "bilinear" to "Bilinear (Smooth)"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        filterOptions.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = filteringMode == value,
                                onClick = {
                                    filteringMode = value
                                    x11Prefs.edit().putString(X11Preferences.KEY_DISPLAY_FILTERING, value).apply()
                                    X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_FILTERING)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, filterOptions.size),
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }
            }

            // Canvas Layout & Insets Behavior
            Text("Canvas Layout & System Insets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsSwitchListItem(
                        headline = "Adjust Resolution for Orientation",
                        supporting = "Automatically swap width and height when device orientation rotates.",
                        checked = adjustRes,
                        onCheckedChange = {
                            adjustRes = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_ADJUST_RESOLUTION, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_ADJUST_RESOLUTION)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Stretch to Fit Display",
                        supporting = "Scale canvas image non-proportionally to eliminate black letterbox bars.",
                        checked = displayStretch,
                        onCheckedChange = {
                            displayStretch = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_DISPLAY_STRETCH, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_DISPLAY_STRETCH)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Reseed Screen with Soft Keyboard",
                        supporting = "Dynamically adjust X11 screen dimensions when on-screen keyboard appears.",
                        checked = reseedIme,
                        onCheckedChange = {
                            reseedIme = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_RESEED, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_RESEED)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchListItem(
                        headline = "Fullscreen Canvas",
                        supporting = "Hide status and navigation bars. Disabling allows Android's floating rotation button to appear.",
                        checked = fullscreenCanvas,
                        onCheckedChange = {
                            fullscreenCanvas = it
                            x11Prefs.edit().putBoolean(X11Preferences.KEY_FULLSCREEN, it).apply()
                            X11Preferences.notifyChanged(context, X11Preferences.KEY_FULLSCREEN)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    val safeCustom = x11Prefs.getBoolean(X11Preferences.KEY_SAFE_AREA_CUSTOM_EDGES, false)
                    val safeAll = x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_MARGIN_ALL, 0)
                    val safeLeft = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_LEFT, 0) else safeAll
                    val safeTop = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_TOP, 0) else safeAll
                    val safeRight = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_RIGHT, 0) else safeAll
                    val safeBottom = if (safeCustom) x11Prefs.getInt(X11Preferences.KEY_SAFE_AREA_BOTTOM, 0) else safeAll

                    val summaryText = if (safeCustom) {
                        "Custom: L:${safeLeft}dp T:${safeTop}dp R:${safeRight}dp B:${safeBottom}dp"
                    } else if (safeAll > 0) {
                        "Uniform: ${safeAll} dp on all edges"
                    } else {
                        "Full Screen (0 dp)"
                    }

                    ListItem(
                        headlineContent = { Text("Screen Safe Area Calibration", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Set margin insets to prevent UI clipping from rounded corners ($summaryText).") },
                        trailingContent = {
                            FilledTonalButton(
                                onClick = onNavigateToSafeAreaEditor,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Calibrate")
                            }
                        }
                    )
                }
            }

            // Screen Idle Timeout
            Text("Screen Idle Timeout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
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
                                        x11Prefs.edit().putString(X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, value).apply()
                                        X11Preferences.notifyChanged(context, X11Preferences.KEY_SCREEN_IDLE_TIMEOUT)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
