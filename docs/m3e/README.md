# Material 3 Expressive (M3E) Technical Specification & Standards

> **MANDATE FOR ALL AGENTS AND ENGINEERS**:
> 1. **ZERO TOLERANCE FOR MIMICRY**: Never hand-roll custom replicas of Material components using basic Compose primitives (`Canvas`, `Path`, `Box`, `Row`) when official Compose Material 3 / M3 Expressive APIs exist.
> 2. **OFFICIAL APIS ONLY**: All "Expressive" components must consume genuine APIs from `androidx.compose.material3:1.5.0-alpha29` (under `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` when required) or `androidx.graphics.shapes`.
> 3. **NO SHADOW MATH OR MAGIC NUMBERS**: Never hand-code arbitrary spring constants (`stiffness = 460f`, `dampingRatio = 0.52f`) or trigonometry loops (`cos`, `sin`). All motion must derive from `MaterialTheme.motionScheme`, and all shape morphing must use `MaterialShapes` or `androidx.graphics.shapes`.
> 4. **NO FAKE ANNOTATIONS**: Never place `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` on functions that do not actually call an official API gated by that annotation.

---

## 1. Complete Documentation Index

| Document | Purpose & Covered Component Families |
| :--- | :--- |
| [**`01-official-apis-and-dependencies.md`**](01-official-apis-and-dependencies.md) | Verified Gradle dependencies, complete inventory of M3E APIs in `1.5.0-alpha29`, and strict anti-mimicry rules. |
| [**`02-motion-scheme-and-tokens.md`**](02-motion-scheme-and-tokens.md) | Canonical motion token architecture: `MotionScheme.expressive()`, `MaterialTheme.motionScheme`, spatial vs. effects specs, and accessible reduced motion. |
| [**`03-shapes-and-morphing.md`**](03-shapes-and-morphing.md) | All **32 pre-built `MaterialShapes`** (`Sunny`, `VerySunny`, `Arch`, `Flower`, `Burst`, `Clover`, `Boom`, etc.), `RoundedPolygon.toShape()` extension, and `Morph`. |
| [**`04-connected-and-split-buttons.md`**](04-connected-and-split-buttons.md) | `ButtonGroup`, `ButtonGroupScope` DSL (`toggleableItem`, `clickableItem`, `customItem`, `weight`), and `SplitButtonLayout` with all 4 variants. |
| [**`05-floating-toolbars.md`**](05-floating-toolbars.md) | `HorizontalFloatingToolbar` & `VerticalFloatingToolbar`, `rememberFloatingToolbarState`, and scroll collapse integration. |
| [**`06-progress-and-loading-indicators.md`**](06-progress-and-loading-indicators.md) | Polygon morphing `LoadingIndicator`, `ContainedLoadingIndicator`, and `LinearWavyProgressIndicator` / `CircularWavyProgressIndicator`. |
| [**`07-menus-lists-and-sliders.md`**](07-menus-lists-and-sliders.md) | `ListItem` selection overload (`ListItemDefaults.shapes()`), `SelectableDropdownMenuItem` with `MenuItemShapes`, and `SliderDefaults.Track` stop-dot indicators. |
| [**`08-adaptive-layout-and-theming.md`**](08-adaptive-layout-and-theming.md) | `MaterialExpressiveTheme`, `LocalUsingExpressiveTheme`, `NavigationSuiteScaffold`, typography scale, and Android 15 `enableEdgeToEdge()`. |
| [**`09-carousels-search-and-sheets.md`**](09-carousels-search-and-sheets.md) | `HorizontalMultiBrowseCarousel`, `SearchBar` / `DockedSearchBar`, `PullToRefreshBox`, `ModalBottomSheet`, and `TooltipBox`. |

---

## 2. Hard Invariants & Agent Verification Checklist

Before writing or modifying any UI code claimed to be "Material 3 Expressive", verify:

- [ ] **No Custom Primitives Replacing Official Components**:
  - ❌ **Forbidden**: Re-implementing button groups with `Row` + `Box.selectable` + `animateDpAsState`.
  - ✅ **Required**: Use `androidx.compose.material3.ButtonGroup` and `ButtonGroupScope.toggleableItem()`.
  - ❌ **Forbidden**: Re-implementing floating toolbars with custom `Surface` + `Row` + drag offset math.
  - ✅ **Required**: Use `androidx.compose.material3.HorizontalFloatingToolbar` / `VerticalFloatingToolbar`.
  - ❌ **Forbidden**: Re-implementing hero spinners with custom rotating `SunnyShape` and `rememberInfiniteTransition`.
  - ✅ **Required**: Use `androidx.compose.material3.LoadingIndicator` or `CircularWavyProgressIndicator`.
  - ❌ **Forbidden**: Hand-rolling polygon shapes using loops with `sin()` and `cos()`.
  - ✅ **Required**: Use `androidx.compose.material3.MaterialShapes.<Shape>.toShape()`.
  - ❌ **Forbidden**: Custom swipe-to-refresh or custom modal sheets with `Dialog`.
  - ✅ **Required**: Use `PullToRefreshBox` and `ModalBottomSheet`.
- [ ] **Motion Scheme Integration**:
  - ❌ **Forbidden**: Hardcoding ad-hoc spring values (`spring(0.55f, 460f)`) or cubic beziers in private objects.
  - ✅ **Required**: Query `MaterialTheme.motionScheme.defaultSpatialSpec()` or appropriate canonical tokens.
- [ ] **Navigation & Shell**:
  - ❌ **Forbidden**: Manual `BoxWithConstraints` with hardcoded 600dp breakpoint switching between rails and bars.
  - ✅ **Required**: Use `material3-adaptive-navigation-suite` (`NavigationSuiteScaffold`).
- [ ] **Theme Completeness**:
  - Wrap the app in `MaterialExpressiveTheme` (or `MaterialTheme` configured with `motionScheme = MotionScheme.expressive()`).
  - Ensure `enableEdgeToEdge()` is used in Activities; **never** tint windows using deprecated `window.statusBarColor`.
