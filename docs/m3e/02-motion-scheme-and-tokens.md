# 02: Canonical Motion Scheme & Token Architecture

## 1. Overview: The M3 Expressive Motion Philosophy

In Material 3 Expressive, motion is not a collection of arbitrary numbers scattered across individual UI files. Motion is a **centralized design token system** governed by `MotionScheme`.

Google categorizes motion into two distinct physical behaviors:
1. **Spatial Motion**: Dimensional changes, position movement, size expansion, and scale transformations. These utilize spring physics with natural mass and bounce.
2. **Effects Motion**: Non-spatial visual state changes, such as alpha fading, color transitions, and surface elevation shifts. These utilize damped or duration-based transitions.

---

## 2. The `MotionScheme` Architecture

Compose Material 3 `1.5.0-alpha+` introduces `MotionScheme`:

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveMotionExample() {
    // 1. Spatial specs for position, size, scale, translation:
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fastSpatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val slowSpatialSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()

    // 2. Effects specs for alpha, color, fade:
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fastEffectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val slowEffectsSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
}
```

### 2.1. Standard Token Roles

| Token | Usage | Canonical Physical Behavior |
| :--- | :--- | :--- |
| `defaultSpatialSpec` | Container expansion, button weight changes, sheet movement | Balanced expressive spring with tangible tactile recoil |
| `fastSpatialSpec` | Small micro-interactions, icon shifts, press indications | High-frequency snappy response, minimizing tap lag |
| `slowSpatialSpec` | Hero view transitions, large canvas transformations | Extended fluid motion for spatial comprehension |
| `defaultEffectsSpec` | Crossfades, backdrop scrims, color harmonizations | Smooth easing without spatial overshoot |
| `fastEffectsSpec` | Quick highlights, instantaneous color switches | Rapid effect transition without visual lag |
| `slowEffectsSpec` | Atmospheric transitions, gradual fades | Extended easing for ambient changes |

### 2.2. Underlying Canonical Values (`ExpressiveMotionTokens`)

Verified directly from the bytecode of `androidx.compose.material3.tokens.ExpressiveMotionTokens` in `material3-android-1.5.0-alpha29`:

| Internal Token Constant | Damping Ratio ($\zeta$) | Stiffness ($k$) | Role |
| :--- | :---: | :---: | :--- |
| `SpringDefaultSpatial` | **0.80f** | **380.0f** | Standard expressive spatial expansion & layout shifts |
| `SpringFastSpatial` | **0.60f** | **800.0f** | Snappy micro-interactions, press feedback |
| `SpringSlowSpatial` | **0.80f** | **200.0f** | Hero transitions & large surface movements |
| `SpringDefaultEffects` | **1.00f** (Critically Damped) | **1600.0f** | Smooth crossfades & alpha effects |
| `SpringFastEffects` | **1.00f** (Critically Damped) | **3800.0f** | High-velocity color/fade transitions |
| `SpringSlowEffects` | **1.00f** (Critically Damped) | **800.0f** | Extended opacity changes |

> **Key Takeaway**: Notice that Google's canonical `SpringDefaultSpatial` is `(0.8f, 380.0f)`. The legacy codebase's hand-rolled `(0.82f, 380f)` in `AppAnimationSpecs.kt` was simply an arbitrary, unnecessary re-implementation of the exact token already compiled into `material3:1.5.0-alpha29`.

---

## 3. Strict Rules: Eliminating Magic Numbers

### ❌ What Was Done Previously (Strictly Forbidden):
```kotlin
// PROHIBITED: Hardcoding arbitrary numbers in private objects
object ExpressiveSprings {
    val Bouncy = spring<Float>(dampingRatio = 0.6f, stiffness = 380f)
    val Snappy = spring<Float>(dampingRatio = 0.52f, stiffness = 460f)
}

// PROHIBITED: Hardcoding cubic beziers
val M3MorphEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
```

### ✅ The Standard M3E Pattern:
```kotlin
// Obtain animation specs directly from the active theme's MotionScheme:
val sizeAnimation = animateDpAsState(
    targetValue = targetWidth,
    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
)
```

---

## 4. Accessibility & Reduced Motion

Material 3 Expressive motion tokens automatically respect system accessibility settings. When the user enables "Remove animations" or "Reduce motion" at the system level:

1. `MotionScheme` transitions to non-spatial or instant specifications (`snap()` or immediate transitions).
2. Code consuming `MaterialTheme.motionScheme` inherits this accessibility compliance automatically without requiring manual `if (reduceAnimations) snap() else spring(...)` checks in every composable.
