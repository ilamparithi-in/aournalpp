# 05: Floating Toolbars Architecture

## 1. Overview: Expressive Floating Toolbars

Material 3 Expressive introduces **`HorizontalFloatingToolbar`** and **`VerticalFloatingToolbar`** specifically designed for creative canvas applications, document editors, and tablet productivity suites.

In `Aournal++`, these native components replace custom canvas toolbars (such as the legacy `FloatingCanvasToolbar.kt`), providing built-in elevation, drag gestures, shape morphing, and scroll collapse behaviors.

---

## 2. `HorizontalFloatingToolbar` Implementation

```kotlin
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CanvasFloatingToolbar(
    expanded: Boolean = true,
    onUndo: () -> Unit,
    onPen: () -> Unit,
    onEraser: () -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalFloatingToolbar(
        expanded = expanded,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        content = {
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = onPen) {
                Icon(Icons.Default.Create, contentDescription = "Pen")
            }
            IconButton(onClick = onEraser) {
                Icon(Icons.Default.Delete, contentDescription = "Eraser")
            }
        }
    )
}
```

---

## 3. `VerticalFloatingToolbar` for Tablet Canvas & Rails

For tablet sidebars and vertical canvas edges:

```kotlin
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CanvasVerticalSidebarToolbar(
    expanded: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    VerticalFloatingToolbar(
        expanded = expanded,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        content = content
    )
}
```

---

## 4. Scroll-Aware Expansion & Collapse

To automatically collapse the floating toolbar when scrolling canvas or notes:

```kotlin
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.rememberFloatingToolbarState
import androidx.compose.runtime.Composable

@Composable
fun ScrollableCanvasScreen() {
    val toolbarState = rememberFloatingToolbarState()
    val scrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(toolbarState)

    // Pass scrollBehavior to HorizontalFloatingToolbar
}
```

---

## 5. Strict Anti-Mimicry Directives

❌ **PROHIBITED IN THIS REPOSITORY**:
1. Hand-rolling floating toolbars using custom `Surface` + `Row` + manual drag offset listeners.
2. Writing custom bezier morphing logic (`M3MorphEasing = CubicBezierEasing(...)`) to animate toolbar size.
3. Managing expanded/collapsed width manually with `animateDpAsState` when `HorizontalFloatingToolbar(expanded = ...)` handles it natively.
