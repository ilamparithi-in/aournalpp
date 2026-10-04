# 08: Adaptive Navigation & Theme Architecture

## 1. `MaterialExpressiveTheme` Configuration

The application theme must be configured via `MaterialExpressiveTheme` (or `MaterialTheme` with explicit `motionScheme`):

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = rememberAppColorScheme(useDarkTheme, dynamicColor)

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
```

---

## 2. Checking Expressive Theme State

To check if expressive theming is active across the tree:
```kotlin
import androidx.compose.material3.LocalUsingExpressiveTheme
import androidx.compose.runtime.Composable

@Composable
fun ThemedComponent() {
    val isExpressive = LocalUsingExpressiveTheme.current
    // Adapt styling or spacing if needed
}
```

---

## 3. Adaptive Navigation: `NavigationSuiteScaffold`

For tablet, foldable, and desktop adaptive layouts, consume `androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold`:

```kotlin
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun MainResponsiveAppShell(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    content: @Composable () -> Unit
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                item(
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = (destination == currentDestination),
                    onClick = { onNavigate(destination) }
                )
            }
        }
    ) {
        content()
    }
}
```

---

## 4. Modern Android 15 Edge-to-Edge Compliance

Targeting modern Android requires true edge-to-edge display without legacy window tinting:

### ❌ Strictly Prohibited:
```kotlin
// PROHIBITED: Deprecated window tinting
@Suppress("DEPRECATION")
window.statusBarColor = colorScheme.surface.toArgb()
```

### ✅ The Canonical Pattern:
1. In `MainActivity.onCreate()`:
   ```kotlin
   import androidx.activity.enableEdgeToEdge

   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       enableEdgeToEdge()
       setContent {
           AppTheme {
               MainResponsiveAppShell(...)
           }
       }
   }
   ```
2. In Compose layouts:
   Consume safe insets using `Modifier.systemBarsPadding()` or `WindowInsets.safeDrawing`.
