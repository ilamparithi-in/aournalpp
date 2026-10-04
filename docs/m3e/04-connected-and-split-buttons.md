# 04: Button Groups & Split Buttons Architecture

## 1. `ButtonGroup`: Connected Segmented Actions

Material 3 Expressive replaces legacy segmented buttons with **`ButtonGroup`**.

### 1.1. Native Behavior
`ButtonGroup` coordinates:
* **Corner morphing**: Leading button has rounded left corners and flat right corners; trailing has rounded right corners; middle has flat inner corners.
* **Selection morphing**: Selected buttons morph to fully rounded pills via `ButtonGroupDefaults.connectedButtonCheckedShape()`.
* **Dynamic Weight Expansion**: Active buttons expand with physical spring motion via `ButtonGroupDefaults.expandedRatio`.

### 1.2. Canonical Implementation

```kotlin
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StoragePresetSelector(
    selectedPreset: String,
    presets: List<String>,
    onPresetSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ButtonGroup(modifier = modifier) {
        presets.forEach { preset ->
            val isSelected = (preset == selectedPreset)
            
            toggleableItem(
                checked = isSelected,
                label = preset,
                onCheckedChange = { onPresetSelected(preset) },
                weight = if (isSelected) ButtonGroupDefaults.expandedRatio else 1f
            ) {
                if (isSelected) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                }
                Text(text = preset)
            }
        }
    }
}
```

### 1.3. Clickable Actions in ButtonGroup
For non-toggleable action strips (e.g., action toolbars):
```kotlin
ButtonGroup {
    clickableItem(
        onClick = { onEdit() },
        label = "Edit"
    ) {
        Text("Edit")
    }
    clickableItem(
        onClick = { onShare() },
        label = "Share"
    ) {
        Text("Share")
    }
}
```

---

## 2. `SplitButton`: Dual-Action Expressive Buttons

A `SplitButton` combines a primary action and a secondary dropdown trigger within a unified visual container.

### 2.1. Canonical Implementation

```kotlin
import androidx.compose.material3.Icon
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SaveNoteSplitButton(
    onSavePrimary: () -> Unit,
    onOpenSaveMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = onSavePrimary
            ) {
                Text("Save")
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                onClick = onOpenSaveMenu
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Save Options"
                )
            }
        },
        modifier = modifier
    )
}
```

### 2.2. Visual Variants
`SplitButtonDefaults` provides standard variants:
* `LeadingButton` / `TrailingButton` (Filled)
* `TonalLeadingButton` / `TonalTrailingButton` (Tonal)
* `OutlinedLeadingButton` / `OutlinedTrailingButton` (Outlined)
* `ElevatedLeadingButton` / `ElevatedTrailingButton` (Elevated)

---

## 3. Strict Anti-Mimicry Directives

❌ **PROHIBITED IN THIS REPOSITORY**:
1. Hand-rolling a `Row` of `Box(Modifier.selectable)` with manual `animateDpAsState` corners.
2. Re-implementing dynamic weight expansion with custom `animateFloatAsState` squish ratios.
3. Placing two independent buttons side-by-side with zero margin to simulate a split button.
