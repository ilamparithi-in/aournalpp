# 06: Progress & Loading Indicators Architecture

## 1. Overview: The Expressive Loading Paradigm

Material 3 Expressive replaces traditional spinning circles with:
1. **`LoadingIndicator`**: An organic indicator that continuously morphs between a list of polygonal shapes.
2. **`ContainedLoadingIndicator`**: A shape-morphing indicator housed within a rounded container badge.
3. **`LinearWavyProgressIndicator` & `CircularWavyProgressIndicator`**: Progress indicators featuring dynamic sinusoidal wave motion.

In `Aournal++`, these components replace custom spinners (such as the legacy `ExpressiveHeroSpinner.kt`).

---

## 2. Polygon-Morphing `LoadingIndicator`

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLoadingSpinner(
    modifier: Modifier = Modifier
) {
    // Uses Google's canonical polygon morphing sequence
    LoadingIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        polygons = LoadingIndicatorDefaults.indeterminateIndicatorPolygons
    )
}
```

---

## 3. `ContainedLoadingIndicator` (Hero Loading Badge)

For prominent loading cards, document importing screens, or splash states:

```kotlin
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeroImportingIndicator(
    modifier: Modifier = Modifier
) {
    ContainedLoadingIndicator(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialShapes.Cookie4Sided.toShape(),
        polygons = LoadingIndicatorDefaults.indeterminateIndicatorPolygons
    )
}
```

---

## 4. Wavy Progress Indicators

### 4.1. `LinearWavyProgressIndicator`
For file downloads, sync progress, and export operations:

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SyncProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    LinearWavyProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth()
    )
}
```

### 4.2. `CircularWavyProgressIndicator`
For compact circular progress:

```kotlin
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CompactWavyProgress(
    modifier: Modifier = Modifier
) {
    // Indeterminate wavy circular progress
    CircularWavyProgressIndicator(
        modifier = modifier
    )
}
```

---

## 5. Strict Anti-Mimicry Directives

❌ **PROHIBITED IN THIS REPOSITORY**:
1. Hand-rolling custom loading spinners with `rememberInfiniteTransition` and rotating a custom `SunnyShape` with `LinearEasing` (e.g. legacy `ExpressiveHeroSpinner.kt`).
2. Drawing wavy lines manually on a `Canvas` with sine wave calculations.
3. Use `LoadingIndicator` or `WavyProgressIndicator` exclusively.
