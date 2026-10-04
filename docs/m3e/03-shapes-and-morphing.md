# 03: MaterialShapes Catalog & Morphing Architecture

## 1. Overview: The Built-In `MaterialShapes`

Material 3 Expressive provides a native catalog of **32 pre-built polygonal shapes** in `androidx.compose.material3.MaterialShapes`.

**Crucial Fact**:
There is **no need** to calculate polygons using math formulas or write custom `Shape` classes. All canonical shapes are pre-defined as `androidx.graphics.shapes.RoundedPolygon` objects and can be converted directly into a Compose `Shape` via `.toShape()`.

---

## 2. The 32 Canonical `MaterialShapes`

All accessible via `MaterialShapes.<ShapeName>`:

| Category | Available Shapes |
| :--- | :--- |
| **Floral & Organic** | `Sunny`, `VerySunny`, `Flower`, `Clover4Leaf`, `Clover8Leaf` |
| **Architectural & Curves** | `Arch`, `Fan`, `SemiCircle`, `Oval`, `Pill`, `Heart`, `Bun` |
| **Expressive Accents** | `Burst`, `SoftBurst`, `Boom`, `SoftBoom`, `Ghostish`, `Puffy`, `PuffyDiamond` |
| **Geometric & Cut** | `Square`, `Slanted`, `Triangle`, `Diamond`, `Pentagon`, `Gem`, `ClamShell` |
| **Cookies & Multi-Sided** | `Cookie4Sided`, `Cookie6Sided`, `Cookie7Sided`, `Cookie9Sided`, `Cookie12Sided` |
| **Digital & Pixel** | `PixelCircle`, `PixelTriangle`, `Circle` |

---

## 3. Usage: Converting to Compose `Shape`

To apply any `MaterialShapes` polygon as a clipping shape or border:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveBadge() {
    // Convert official MaterialShapes.Sunny directly to Compose Shape
    val sunnyShape = MaterialShapes.Sunny.toShape()

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(sunnyShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
    )
}
```

---

## 4. Geometric Morphing between Polygons (`Morph`)

To morph between two polygons during user interaction (e.g., hover, touch, selection):

```kotlin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingPolygonButton(
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    val isPressed by interactionSource.collectIsPressedAsState()

    // 1. Morph from Cookie4Sided (idle) to Sunny (pressed)
    val morph = remember {
        Morph(MaterialShapes.Cookie4Sided, MaterialShapes.Sunny)
    }

    // 2. Animate morph progress using the theme's spatial motion token
    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "morphProgress"
    )

    // 3. Render morphed path
    val color = MaterialTheme.colorScheme.primary
    val androidPath = remember { android.graphics.Path() }

    Canvas(modifier = modifier.size(56.dp)) {
        androidPath.rewind()
        morph.toPath(progress, androidPath)

        // Scale and center the path within canvas bounds
        val matrix = android.graphics.Matrix()
        matrix.setScale(size.width / 2f, size.height / 2f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        androidPath.transform(matrix)

        drawPath(path = androidPath.asComposePath(), color = color)
    }
}
```

---

## 5. Strict Anti-Pattern Rule

❌ **PROHIBITED IN THIS REPOSITORY**:
```kotlin
// NEVER WRITE THIS:
class SunnyShape(private val vertices: Int = 8) : Shape {
    override fun createOutline(...) {
        for (i in 0 until vertices) {
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            // ...
        }
    }
}
```
Always use `MaterialShapes.Sunny.toShape()` or `androidx.graphics.shapes`.
