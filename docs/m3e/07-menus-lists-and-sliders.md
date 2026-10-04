# 07: Menus, Lists & Sliders Architecture

## 1. Expressive `ListItem` with Selection Semantics

The selection-aware `ListItem` overload is gated under `@ExperimentalMaterial3ExpressiveApi`. It coordinates accessible toggle targets, state announcements, and corner morphing.

### Canonical Implementation

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSwitchListItem(
    headline: String,
    supporting: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = ListItemDefaults.shapes(),
        headlineContent = { Text(text = headline) },
        supportingContent = supporting?.let { { Text(text = it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null, // Interaction is managed by the ListItem row
                thumbContent = if (checked) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        },
        modifier = modifier
    )
}
```

---

## 2. Expressive Menus & Selection

Material 3 Expressive introduces selection-aware menu items with rounded corner shapes:

```kotlin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun NoteSortingDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedSort: String,
    onSelectSort: (String) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        listOf("Date Modified", "Title", "File Size").forEach { option ->
            SelectableDropdownMenuItem(
                selected = (option == selectedSort),
                onClick = {
                    onSelectSort(option)
                    onDismissRequest()
                },
                text = { Text(option) }
            )
        }
    }
}
```

---

## 3. Sliders: Official Track with Stop Indicators & Wavy Sliders

### 3.1. Official `Slider` with `SliderDefaults.Track`
The official `Slider` supports stop-dot indicators and flat-to-rounded corner transitions:

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSettingsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 4,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                trackStopIndicatorSize = 4.dp
            )
        },
        modifier = modifier
    )
}
```

### 3.2. `WavySlider`
For expressive volume, brush size, or zoom adjustments:
```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.WavySlider
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrushSizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    WavySlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 1f..50f
    )
}
```

---

## 4. Strict Anti-Mimicry Directives

❌ **PROHIBITED IN THIS REPOSITORY**:
1. Hand-drawing tracks, stop dots, and pill thumbs using imperative `Canvas` and `detectDragGestures` (e.g. legacy `ExpressiveSlider.kt`).
2. Custom click-state tracking on rows when `ListItem(checked = ..., onCheckedChange = ...)` handles it natively.
