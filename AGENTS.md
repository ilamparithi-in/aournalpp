# AGENTS.md: Developer & Agent Engineering Guide for Aournal++

Welcome to **Aournal++** (`xopp-android`)! This document serves as the primary technical specification, architectural reference, and operational playbook for autonomous AI coding agents and human engineers working on this repository.

Always read and adhere to the guidelines, architectural invariants, and security mandates established here and in [`docs/ADRs/`](docs/ADRs/).

---

## 1. Project Overview & Mission

**Aournal++** is an independent, community-driven Android companion and wrapper for [Xournal++](https://xournalpp.github.io/), engineered to bring desktop-class vector handwriting, note-taking, and PDF annotation to Android tablets without requiring root or external chroot apps.

### Core Capabilities
* **Embedded Termux-X11 Display Server**: Bundles a native Xorg display server (`libXlorie.so`) and X11 userland rootfs inside the APK.
* **Low-Latency Stylus Pipeline**: Direct hardware routing for active pens (Lenovo Precision Pen, Samsung S-Pen, etc.) with configurable button actions (erase, switch tools, undo) and finger-as-stylus toggle.
* **Material 3 Expressive Companion**: Jetpack Compose UI featuring a visual Document Hub, collage/grid note views, instant resume, and safe area inset calibration.
* **Multi-Provider Cloud Sync**: Bi-directional automated synchronization supporting Nextcloud, Google Drive, generic WebDAV, FTP, SMB, and local directories.
* **Hermetic Sandbox & Recovery**: Isolated execution environment, zero-data-loss autosave, emergency backup recovery, and cross-process coordination.

---

## 2. Repository & Module Architecture

The project is structured into modular Gradle projects and native components:

```
xopp-android/
├── app/                  # Main Android Application (UI, ViewModels, Compose, Room DB, Cloud Sync)
│   └── src/
│       ├── main/         # UI screens, navigation, activities, receivers, cloud sync providers
│       ├── test/         # Fast Host JVM unit tests (ViewModels, parsers, sync policies)
│       └── androidTest/  # Instrumented tests (Room SQLite DAOs, Compose UI components)
├── runtime-manager/      # Core Linux environment orchestration library
│   └── src/
│       ├── main/         # Bootstrap installer, LinuxEnvironment, config generators, ProcessSupervisor
│       └── test/         # Pure Host JVM tests for configs, process locks, and path validators
├── x11-core/             # Android library compiling Termux-X11 & Xorg native C++ code via CMake
├── submodules/
│   └── termux-x11/       # Git submodule tracking customized Termux-X11 display server
├── patches/              # Standalone reproducible patch archives for submodules
├── scripts/              # Bootstrap rootfs packaging and native build utilities
├── docs/ADRs/            # Architectural Decision Records (ADR 0001 through ADR 0019+)
├── libs/                 # Local prebuilt binaries (e.g., x11-core-release.aar)
└── gradle/               # Version catalog (libs.versions.toml) and Gradle wrapper
```

### Module Responsibilities

| Module / Directory | Role & Key Components |
| :--- | :--- |
| **`:app`** | Houses the user-facing Jetpack Compose application.<br>• `MainActivity.kt`: Gatekeeper for intent routing, Document Hub, note browser, search, and settings.<br>• `CanvasActivity.kt`: Hosts the native X11 rendering surface, floating toolbars, stylus input gestures, and IME integration.<br>• `backup/`: Modular cloud sync engines (`WebDavStorageProvider`, `GoogleDriveStorageProvider`, `BackupEngine`, `CredentialsVault`).<br>• `data/`: Room database, note metadata entities, DAOs, and document caches. |
| **`:runtime-manager`** | Completely decoupled runtime orchestration layer.<br>• `LinuxEnvironment.kt`: Bootstraps userland paths, environment variables (`DISPLAY=:0`, `LD_PRELOAD`, `PULSE_SERVER`), and socket binding.<br>• `ProcessSupervisor.kt`: Spawns, tracks, and supervises Openbox, Xournal++, and companion background daemons.<br>• `BootstrapInstaller.kt`: Extracts compressed userland rootfs with Tar Slip validation and symlink confinement.<br>• `DesktopConfigGenerator.kt` & `XournalConfigManager.kt`: Generates compliant XDG, Openbox, and Xournal++ configuration files. |
| **`:x11-core`** | Wraps the native C/C++ Termux-X11 server. Builds `libXlorie.so` with 16 KB page-alignment (`-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON`). Can be substituted by `libs/x11-core-release.aar` to accelerate builds. |
| **`scripts/`** | Python and shell automation tools (`build_bootstrap.py`, `build_x11_core_aar.sh`, `bootstrap.lock.json`). |

---

## 3. Strict Architectural Invariants & ADR Directives

All modifications must adhere to the Architectural Decision Records located in [`docs/ADRs/`](docs/ADRs/). Pay critical attention to the following mandates:

### 3.1. Testing Architecture & Testability (ADR 0018)

* **Zero Tolerance for Shadow Implementations**:
  * **Strict Rule**: Tests must **never** duplicate or re-implement production business logic, parser algorithms, or state rules inside test files or mock helpers.
  * **Pure Extraction Pattern**: If production logic is difficult to test because it touches Android platform APIs (`Context`, `SharedPreferences`, `Activity`), extract the algorithmic core into a pure, testable function in the production codebase (typically in a companion object or pure domain class).
* **ViewModel Constructor Injection**:
  * ViewModels must **never** call hardcoded singletons (e.g. `getInstance()`) or dispatchers (`Dispatchers.IO`) inside properties or methods.
  * Always use `@JvmOverloads constructor(...)` with production defaults so the Android framework can instantiate them without boilerplate while unit tests can inject test dispatchers and mocks.
* **Deterministic Flow Testing**:
  * Test `StateFlow` and channels using `kotlinx-coroutines-test` (`StandardTestDispatcher`, `runTest`, `advanceUntilIdle()`) and CashApp `Turbine` (`flow.test { ... }`).
* **Test Separation (JVM vs. Instrumented)**:
  * **Host JVM Tests (`src/test`)**: Must encompass 95%+ of tests. Covers ViewModels, domain logic, configuration generators, and `:runtime-manager` components. Uses `io.mockk:mockk`.
  * **Instrumented Device Tests (`src/androidTest`)**: Reserved strictly for Room SQLite database migrations/DAOs, Compose UI semantic interactions, and hardware stylus/event routing.
  * **Do NOT Enable `isReturnDefaultValues = true`**: Gradle configuration must not enable default return values on host mocks, as it hides silent platform failures (e.g., `Os.kill` error handling).
* **Module Decoupling**: `:runtime-manager` must **never** depend on `:app` UI classes.

### 3.2. Security Hardening & Sandboxing (ADR 0019)

* **Component Sandboxing (SEC-01)**:
  * `CanvasActivity` and `SettingsActivity` are strictly internal (`android:exported="false"`).
  * `MainActivity` is the sole entry point for `ACTION_VIEW` and `ACTION_EDIT` intents. Incoming URIs must be validated and canonicalized before internal routing.
* **Cryptographic Vault & Fail-Closed Semantics (SEC-02)**:
  * Zero plaintext credentials on flash storage.
  * If `AndroidKeyStore` corruption occurs, `CredentialsVault` self-heals by purging stale keys and falls back to volatile `InMemorySharedPreferences` (RAM-only). Never write unencrypted secrets to disk.
* **Canonical Path Traversal Defense (SEC-03)**:
  * All cloud sync, file import, and preview paths must be resolved via canonical bounds checks (`resolveSafeChild`).
  * Any path escaping the designated base directory (`../`, absolute paths) must throw a `SecurityException` immediately.
* **Tar Slip & Symlink Confinement (SEC-04)**:
  * Archive unpackers (`BootstrapInstaller`) must verify that every destination path resides strictly within `rootDir`.
  * Symlink targets must be validated to ensure they remain inside the application sandbox and do not escape to `/system` or arbitrary storage.
* **Least-Privilege FileProvider (SEC-05)**:
  * Never declare root wildcards (`path="."`) in `res/xml/file_paths.xml`. Only expose dedicated functional subdirectories (`home/Notes/`, `shared_pdfs/`, etc.).
* **XML Processing Hardening (SEC-08)**:
  * All XML pull parsers must disable DTD processing and external entity expansion (`features.html#process-docdecl = false`).
* **Unix Domain Sockets**:
  * X11 and PulseAudio domain sockets (`/tmp/.X11-unix/X0`) must be created with strict permissions (`0700`) within private internal app storage.

---

## 4. Build System & Toolchain Setup

> [!IMPORTANT]
> Detailed toolchain setup, SDK/NDK installation, signing key configuration, multi-architecture compilation, and upstream submodule synchronization are documented in [**`BUILDING.md`**](BUILDING.md).

### Prerequisites Summary

| Tool | Version Requirement | Reference |
| :--- | :--- | :--- |
| **JDK** | JDK 17 (Temurin / OpenJDK 17) | [BUILDING.md § 1](BUILDING.md#host-system-tools-linux--macos--wsl) |
| **Android SDK** | Compile SDK `36`, Target SDK `36`, Min SDK `26` | [BUILDING.md § 1](BUILDING.md#android-sdk--ndk) |
| **Android NDK** | `27.3.13750724` (`r27d` LTS) | [BUILDING.md § 1](BUILDING.md#android-sdk--ndk) |
| **CMake** | `3.22.1+` | [BUILDING.md § 1](BUILDING.md#android-sdk--ndk) |
| **Host Tools** | `python3`, `bison`, `patch`, `make`, `gcc` | [BUILDING.md § 1](BUILDING.md#host-system-tools-linux--macos--wsl) |

### Git Submodules Requirement

Aournal++ relies on recursive submodules for Termux-X11 and native C/C++ libraries (`pixman`, `libepoxy`, etc.):

```bash
# Clone with recursive submodules:
git clone --recurse-submodules https://github.com/ilamparithi-in/aournalpp.git

# Or initialize if already cloned:
git submodule update --init --recursive
```
For architecture details on native patches and 16 KB page-size alignment, see [BUILDING.md § 2 & § 3](BUILDING.md#3-submodule--patch-architecture).

---

## 5. Development Workflows & Useful Commands

For full building, APK signing, and variant information, consult [**`BUILDING.md`**](BUILDING.md). Below are the essential commands for routine agent development, testing, and verification:

### 5.1. Fast Builds Using Prebuilt `:x11-core` AAR
Compiling `:x11-core` from source builds the entire Xorg display server (several minutes). When working primarily on Kotlin, Compose UI, or runtime management, utilize the prebuilt AAR optimization (see [BUILDING.md § 3.4](BUILDING.md#3-submodule--patch-architecture)):

```bash
# Build the native AAR once into libs/:
./scripts/build_x11_core_aar.sh

# settings.gradle.kts automatically detects libs/x11-core-release.aar
# and skips recompiling :x11-core!
```

To force building `:x11-core` from source, pass `-PusePrebuiltX11=false`:
```bash
./gradlew assembleArm64Debug -PusePrebuiltX11=false
```

### 5.2. Compiling Debug & Beta APKs
See [BUILDING.md § 4](BUILDING.md#4-building-the-project-multi-architecture) for full multi-architecture and signing instructions.

```bash
# ARM64 (standard for Android physical tablets & phones):
./gradlew assembleArm64Debug

# Direct install to connected device via ADB:
./gradlew :app:installArm64Debug

# x86_64 (for Android emulators):
./gradlew assembleX86_64Debug

# Beta build (installs alongside release with .beta suffix):
./gradlew assembleArm64Beta
```

### 5.3. Running Automated Tests
See [BUILDING.md § 5](BUILDING.md#5-running-tests--static-analysis) and [ADR 0018](docs/ADRs/0018-testing-strategy-testability-architecture-and-code-guidelines.md) for testing guidelines.

```bash
# Run all host unit tests across :app and :runtime-manager:
./gradlew testArm64DebugUnitTest :runtime-manager:testDebugUnitTest

# Run specific unit test class:
./gradlew :app:testArm64DebugUnitTest --tests "dev.ilamparithi.aournalpp.ui.viewmodel.DocumentHubViewModelTest"
./gradlew :runtime-manager:testDebugUnitTest --tests "dev.ilamparithi.aournalpp.runtime.DesktopConfigGeneratorTest"

# Run instrumented tests on an active device or emulator:
./gradlew :app:connectedArm64DebugAndroidTest
```

### 5.4. Static Analysis & Lint
See [BUILDING.md § 5](BUILDING.md#static-analysis--android-lint) for details:

```bash
# Run Android lint on the primary ARM64 debug variant:
./gradlew lintArm64Debug
```

### 5.5. Bootstrap Rootfs Management
See [BUILDING.md § 6](BUILDING.md#6-standalone-bootstrap-packaging) for userland rootfs archive packaging:

```bash
# Package bootstrap via Gradle helper task:
./gradlew :app:generateBootstrapArm64

# Package bootstrap standalone via Python:
python3 scripts/build_bootstrap.py --arch aarch64 --output app/src/arm64/assets/bootstrap.tar.xz

# Update package lockfile from upstream repositories:
python3 scripts/build_bootstrap.py --update-lock
```

---

## 6. Coding & Design Standards

### 6.1. Kotlin & Jetpack Compose
* **Material 3 Expressive**: Utilize Material You dynamic theming and components (`androidx.compose.material3`).
* **Unidirectional Data Flow**: Screen composables should consume immutable `StateFlow<UiState>` and emit user events upwards via lambdas (`onAction: (UiAction) -> Unit`).
* **Accessibility (a11y)**: Every interactive element (`IconButton`, `Card`, clickable row) must have an explicit `contentDescription` or provide `Modifier.semantics` for TalkBack navigation.
* **Coroutines**: Never launch unconfined coroutines on `GlobalScope`. Always scope background work to `viewModelScope`, `lifecycleScope`, or an injected `CoroutineScope`.

### 6.2. IPC & Process Management
* Aournal++ runs two primary processes:
  * `:main`: Hosts `MainActivity`, Document Hub, settings, and database access.
  * `:canvas`: Dedicated isolated process hosting `CanvasActivity` and the native X11 engine.
* Inter-process synchronization and commands must pass through `CanvasCommandReceiver` or verified Android IPC mechanisms with UID checks (`Binder.getCallingUid() == Process.myUid()`).
* Cross-process file locks (`CrossProcessLock`) must be used when coordinating file access between the UI and background workers.

---

## 7. Agent Verification Checklist

Before submitting code, creating a pull request, or concluding an engineering session, agents must execute and verify the following steps:

- [ ] **Submodule Integrity**: Verify no untracked or dirty submodules exist (`git status`).
- [ ] **No Shadow Implementations**: Confirm new tests exercise production code paths without duplicated algorithms.
- [ ] **Dependency Injection**: Ensure any new or modified ViewModels use `@JvmOverloads constructor(...)` with injectable dispatchers and repositories.
- [ ] **Security Checks**: Ensure no unvalidated file paths or unexported activity vulnerabilities were introduced.
- [ ] **Host Unit Tests Pass**:
  ```bash
  ./gradlew testArm64DebugUnitTest :runtime-manager:testDebugUnitTest
  ```
- [ ] **Lint Cleanliness**:
  ```bash
  ./gradlew lintArm64Debug
  ```
- [ ] **Compilation Validation**: Ensure both flavors compile cleanly:
  ```bash
  ./gradlew assembleArm64Debug
  ```
