# 01: Official Material 3 Expressive APIs & Dependencies

## 1. Verified Gradle Dependencies

All components described in this suite are verified against the bytecode of `androidx.compose.material3:1.5.0-alpha29` and `androidx.graphics.shapes:1.0.1`:

```toml
# gradle/libs.versions.toml
[versions]
material3Version = "1.5.0-alpha29"
graphicsShapes = "1.0.1"

[libraries]
androidx-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "material3Version" }
androidx-material3-adaptive-navigation-suite = { group = "androidx.compose.material3", name = "material3-adaptive-navigation-suite", version.ref = "material3Version" }
androidx-graphics-shapes = { group = "androidx.graphics", name = "graphics-shapes", version.ref = "graphicsShapes" }
```

In `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.graphics.shapes)
}
```

---

## 2. Complete Inventory of Material 3 Expressive APIs

Verified directly from `classes.jar` in `material3-android-1.5.0-alpha29.aar`:

### 2.1. Theming & Motion
* **`MaterialExpressiveTheme`**: Official top-level composable enabling the expressive design system.
* **`MaterialTheme.motionScheme`**: Centralized motion token provider.
* **`MotionScheme.Companion.expressive()`** & **`MotionScheme.Companion.standard()`**: Canonical motion factories.
* **`LocalUsingExpressiveTheme`**: Composition local indicating active expressive theming.

### 2.2. Button Groups & Split Buttons
* **`androidx.compose.material3.ButtonGroup`**: Container for connected actions.
* **`androidx.compose.material3.ButtonGroupScope`**: DSL providing `toggleableItem`, `clickableItem`, `customItem`, `weight`, `animateWidth`.
* **`androidx.compose.material3.ButtonGroupDefaults`**: Corner shape presets (`connectedLeadingButtonShapes`, `connectedMiddleButtonShapes`, `connectedTrailingButtonShapes`, `connectedButtonCheckedShape`) and `expandedRatio`.
* **`androidx.compose.material3.SplitButtonLayout`**: Split action button container.
* **`androidx.compose.material3.SplitButtonDefaults`**: `LeadingButton`, `TrailingButton`, `TonalLeadingButton`, `TonalTrailingButton`, `OutlinedLeadingButton`, `ElevatedLeadingButton`.

### 2.3. Floating Toolbars
* **`androidx.compose.material3.HorizontalFloatingToolbar`**: Floating action strip for horizontal layouts.
* **`androidx.compose.material3.VerticalFloatingToolbar`**: Floating action strip for vertical/rail layouts.
* **`androidx.compose.material3.FloatingToolbarDefaults`**: Dimensions, colors, and nested scroll expansion modifiers.
* **`rememberFloatingToolbarState`** & **`FloatingToolbarScrollBehavior`**: Automatic expand/collapse on content scroll.

### 2.4. Shapes & Geometry
* **`androidx.compose.material3.MaterialShapes`**: 32 built-in canonical `RoundedPolygon` definitions (`Sunny`, `VerySunny`, `Arch`, `Flower`, `Clover4Leaf`, `Burst`, `Pill`, `Boom`, etc.).
* **`androidx.compose.material3.MaterialShapesKt.toShape()`**: Extension function converting any `RoundedPolygon` directly into a Compose `Shape`.
* **`androidx.graphics.shapes.Morph`**: Real-time geometric morphing between polygons.

### 2.5. Progress & Loading Indicators
* **`androidx.compose.material3.LoadingIndicator`**: Polygon-morphing indeterminate loading indicator.
* **`androidx.compose.material3.ContainedLoadingIndicator`**: Contained polygon-morphing spinner.
* **`LoadingIndicatorDefaults`**: Pre-configured indicator polygons (`indeterminateIndicatorPolygons`, `determinateIndicatorPolygons`).
* **`LinearWavyProgressIndicator`** & **`CircularWavyProgressIndicator`**: Wavy animated progress bars.

### 2.6. Menus & List Items
* **`ListItem` with selection semantics**: Gated under `@ExperimentalMaterial3ExpressiveApi`. Supports `checked`, `onCheckedChange`, and `shapes = ListItemDefaults.shapes()`.
* **`SelectableDropdownMenuItem`** & **`DropdownMenuItemChecked`**: Selection-aware menu items.
* **`MenuItemShapes`** & **`MenuGroupShapes`**: Expressive corner rounding for menu items.

### 2.7. Sliders
* **`Slider` with `SliderDefaults.Track`**: Supports native `trackStopIndicatorSize`, active/inactive flat vs rounded corner morphing, and centered tracks.
* **`WavySlider`**: Expressive wavy slider component.

### 2.8. Adaptive Shell
* **`NavigationSuiteScaffold`**: Automatic size-class-aware navigation bar / rail / drawer transitions.
