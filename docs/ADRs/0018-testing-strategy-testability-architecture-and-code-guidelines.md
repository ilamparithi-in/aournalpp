# ADR 0018: Testing Strategy, Testability Architecture, and Code Guidelines for Future Development

## Status
Accepted

## Context
During the comprehensive testing audit of Aournal++ (Testing Audit: Part 5), several systemic architectural blind spots and testing anti-patterns were identified:
1. **0% ViewModel Test Coverage**: Core ViewModels (`DocumentHubViewModel`, `BootstrapViewModel`, `CanvasViewModel`) were tightly coupled to hardcoded singletons (`LinuxEnvironment.getInstance()`, `DocumentRepository.getInstance()`) and hardcoded coroutine dispatchers (`Dispatchers.IO`), making unit testing impossible on host JVMs.
2. **Untested Runtime Infrastructure**: The `:runtime-manager` module—responsible for bootstrap payload verification, Xournal++ INI configuration generation, Openbox window manager rules, and cross-process file locks—had 0 automated tests.
3. **"Shadow Implementation" Anti-Pattern**: Test suites (e.g., `AutoloadPreferenceOverrideTest`, `OnboardingAndBootstrapTest`) copied and re-implemented production XML parsing and version comparison logic directly inside test files rather than exercising the actual production code, creating false confidence while allowing real regressions to pass undetected.
4. **Host JVM vs. Android Runtime Nuances**: Running Android code on the host JVM lacks the Android OS runtime. Prior work showed that enabling `isReturnDefaultValues = true` in Gradle masked critical failure modes (e.g., `Os.kill(pid, 0)` returning dummy `0` instead of throwing, breaking process tracking tests). Conversely, Room SQLite and Compose UI testing require the real Android OS engine or an instrumented environment.

To maintain engineering velocity, prevent regressions, and ensure long-term stability, this architecture decision record establishes standard testing strategies and code design guidelines for all future development.

---

## Decisions

### 1. Zero Tolerance for "Shadow Implementations"
- **Strict Rule**: Tests must **never** duplicate or re-implement production business logic, parser algorithms, or state rules inside test files or mock helpers.
- **The Pure Extraction Pattern**: If production logic is difficult to test because it is tangled with Android platform APIs (`Context`, `SharedPreferences`, `Activity`, `File` system side-effects), the algorithmic core must be extracted into a pure, testable function in the production codebase (typically in a companion object or pure domain class):
  ```kotlin
  // In Production Code (e.g., LinuxEnvironment.kt):
  companion object {
      @VisibleForTesting
      fun overrideAutoloadInXml(xmlContent: String): String { ... }
  }

  // In Test Code (AutoloadPreferenceOverrideTest.kt):
  @Test
  fun testAutoloadOverride() {
      val result = LinuxEnvironment.overrideAutoloadInXml(sampleXml)
      assertEquals("false", extractAutoloadValue(result))
  }
  ```

---

### 2. ViewModel Testability Architecture & Constructor Injection
All ViewModels must be designed for testability by following the **Default Constructor Dependency Injection** pattern:
- **No Hardcoded Singletons**: ViewModels must never directly call `getInstance()` inside property initializers or methods.
- **No Hardcoded Dispatchers**: ViewModels must never call `Dispatchers.IO` or `Dispatchers.Default` directly; they must accept `CoroutineDispatcher` via constructor.
- **`@JvmOverloads` for Framework & Test Compatibility**: Use `@JvmOverloads constructor(...)` with production defaults. This allows the Android framework (`viewModel()`, `ViewModelProvider.Factory`) to construct the ViewModel without boilerplate DI while allowing unit tests to supply mocks, fakes, and test dispatchers:
  ```kotlin
  class DocumentHubViewModel @JvmOverloads constructor(
      application: Application,
      private val repository: DocumentRepository = DocumentRepository.getInstance(application),
      private val historyTracker: NoteHistoryTracker = NoteHistoryTracker.getInstance(application),
      private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
  ) : AndroidViewModel(application) { ... }
  ```
- **Testing StateFlow with Turbine**: State flows must be tested deterministically using `kotlinx-coroutines-test` (`StandardTestDispatcher`, `runTest`, `advanceUntilIdle()`) and CashApp `Turbine` (`turbineScope`, `flow.test { ... }`):
  ```kotlin
  @Test
  fun testSearchQueryUpdatesUiState() = runTest(testDispatcher) {
      val viewModel = DocumentHubViewModel(mockApp, mockRepo, mockTracker, testDispatcher)
      viewModel.uiState.test {
          assertEquals("", awaitItem().searchQuery)
          viewModel.setSearchQuery("lecture")
          advanceUntilIdle()
          assertEquals("lecture", awaitItem().searchQuery)
      }
  }
  ```

---

### 3. Separation of Host JVM Tests vs. Instrumented Device Tests

The testing pyramid for Aournal++ is split into two complementary layers:

#### A. Host JVM Tests (`src/test`): Fast, Deterministic (< 3s total)
- **Target**: 95%+ of all codebase unit tests.
- **Scope**:
  - ViewModels (state machines, filtering, user event handling).
  - Domain repositories and business logic algorithms.
  - `:runtime-manager` modules: INI/XML generators, desktop configs, version extractors, lock synchronizers.
  - Data parsers, formatters, and conflict resolution policies.
- **Mocking Standard**: Use `io.mockk:mockk` for mock objects. When Android `Log` or framework statics must be stubbed, use `mockkStatic(Log::class)`.
- **Warning on Gradle Config**: Do **NOT** enable `isReturnDefaultValues = true` in `app/build.gradle.kts`. It introduces silent default returns for native and OS calls (such as `Os.kill`), which invalidates real crash and error handling tests.

#### B. Instrumented Device Tests (`src/androidTest`): Real Android Runtime
- **Target**: Tests that strictly require the Android Linux Kernel, SQLite native engine, or Compose rendering pipeline.
- **Scope**:
  - **Room Database & DAOs**: Use `Room.inMemoryDatabaseBuilder(context, ...)` to verify real SQLite schema constraints, foreign keys, migrations, and transactional queries.
  - **Compose UI Component Tests**: Use `createComposeRule()` to test individual reusable components (`StandardNoteCard`, `FloatingToolbarLayout`, dialogs). Verify semantic trees, TalkBack accessibility descriptions, and click handlers.
  - **Hardware / Stylus / IME Event Routing**: Tests verifying stylus pressure, button overrides, or X11 surface canvas interactions.

---

### 4. `:runtime-manager` Module Isolation
- The `:runtime-manager` module is responsible for the chrooted Linux runtime orchestration (`xournalpp`, Openbox, bootstrap).
- **Rule**: `:runtime-manager` must remain completely decoupled from the `:app` UI layer.
- All configuration generators (`DesktopConfigGenerator`, `XournalConfigManager`), bootstrap extractors (`BootstrapInstaller`), and cross-process coordination primitives (`CrossProcessLock`) must have dedicated, hermetic host unit tests in `:runtime-manager/src/test`.
- Temporary directories created during testing must use JUnit `@TempDir` / `createTempDirectory()` and be cleaned up automatically.

---

### 5. General Code Guidelines for Writing App Code

When writing or refactoring production code in Aournal++, adhere to the following testability principles:

1. **Immutable UI State**:
   - Expose UI state strictly as `StateFlow<UiState>` where `UiState` is an immutable data class with descriptive properties and sealed sub-interfaces for transient events (e.g., `NavigationEvent`, `SnackbarMessage`).
2. **Encapsulate Platform Storage Behind Testable Abstractions**:
   - For `SharedPreferences`, use testable wrappers or implement in-memory equivalents (e.g., `TestSharedPreferences`) rather than relying on Android Robolectric or unmocked XML stores.
3. **Compose Testability**:
   - Ensure interactive UI components (`IconButton`, `Card`, `ListItem`) provide appropriate `contentDescription` for accessibility and testing, or explicit `Modifier.testTag(...)` when semantic descriptions are dynamic.
4. **Coroutine Hygiene**:
   - Never launch coroutines on `GlobalScope`. Always launch on ViewModel `viewModelScope` or pass an explicit CoroutineScope.
   - Prefer suspending functions that return values over callbacks or internal async job scheduling.
5. **Fail-Fast Pure Logic**:
   - Keep file path transformations, regular expressions, and protocol payload builders in pure static or companion functions without side effects.

---

## Consequences

- **Quality & Velocity**: Continuous integration runs host unit tests across both modules in under 4 seconds, providing instant regression feedback.
- **Maintainability**: Future refactorings of ViewModels or runtime configs are guarded by unit tests rather than requiring slow manual builds.
- **Standardization**: All new ViewModels and domain components follow the `@JvmOverloads` constructor injection pattern by default.
