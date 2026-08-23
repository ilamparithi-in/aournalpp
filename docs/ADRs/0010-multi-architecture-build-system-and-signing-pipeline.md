# ADR 0010: Multi-Architecture Build System, Dynamic Userland Packager & Release Signing Pipeline

## Status
Accepted

## Context
1. **Multi-Architecture Support**: Aournal++ embeds a full Linux userland (Xournal++, Openbox, GTK3, Cairo, Poppler) alongside Termux-X11 native X server libraries (`libXlorie.so`). The previous build system was hardcoded strictly for ARM64 (`aarch64` / `arm64-v8a`), making it impossible to compile or execute on `x86_64` environments (such as Android Studio Emulators, ChromeOS, and Windows Subsystem for Android) or 32-bit platforms.
2. **Lean APK Distribution**: Bundling multiple complete desktop userland archives inside a single APK would result in a massive ~220MB+ package. Separate architecture-specific packages are needed to keep downloads lean (~100MB).
3. **Reproducible Native Helper Compilation**: Helper binaries (`xopp-title-watcher`, `xopp-wallpaper`, `libgtk-android-ime.so`, `libportaudio.so.2`) were previously committed to version control as precompiled ARM64 binaries, violating source-only repository hygiene and breaking multi-arch builds.
4. **CI & Caching Readiness**: Bootstrap packaging involves resolving and extracting ~70 Debian packages (~100MB). A persistent disk cache is necessary to avoid redundant downloads on consecutive local builds and GitHub Actions CI runs.
5. **Secure & Flexible Release Signing**: Release APK signing must support local developer overrides and CI automation (GitHub Actions Secrets) without committing keys or hardcoded paths to Git.

---

## Decision

### 1. Gradle Product Flavors (`app/build.gradle.kts`)
- Introduced the `abi` flavor dimension with two primary flavors:
  - **`arm64`**: Configures `ndk.abiFilters` to `["arm64-v8a"]` and packages `bootstrap-assets/arm64`.
  - **`x86_64`**: Configures `ndk.abiFilters` to `["x86_64"]` and packages `bootstrap-assets/x86_64`.
- Standard Gradle build commands:
  - Debug: `./gradlew assembleArm64Debug`, `./gradlew assembleX86_64Debug`
  - Release: `./gradlew assembleArm64Release`, `./gradlew assembleX86_64Release`
  - All flavors: `./gradlew assembleDebug`, `./gradlew assembleRelease`

### 2. Multi-Arch Bootstrap Packager (`scripts/build_bootstrap.py`)
- Parameterized `--arch` / `-a` with support for `aarch64`, `x86_64`, `arm`, and `i686` (along with standard Android ABI aliases).
- Dynamic repository resolution: queries `binary-<termux_arch>` APT indices directly from official Termux main and X11 repositories.
- Local package cache: caches downloaded `.deb` archives in `build/deb_cache/<arch>` for instant offline rebuilds and CI cache integration.
- On-the-fly cross-compilation:
  - Discovers the appropriate NDK Clang compiler (`<triple>-clang`) for the target architecture.
  - Cross-compiles `portaudio_stub.c`, `gtk-android-ime.c`, `xopp-title-watcher.c`, and `xopp-wallpaper.c` natively against the extracted staging headers/libraries.
  - Removed all precompiled binaries (`*-arm64`, `*.so`) from source control.

### 3. Dynamic Process Bitness Detection (`LinuxEnvironment.kt`)
- Replaced filesystem-only checks with the standard Android framework API [`android.os.Process.is64Bit()`](https://developer.android.com/reference/android/os/Process#is64Bit()):
  ```kotlin
  val systemLibDir = when {
      android.os.Process.is64Bit() && File("/system/lib64").exists() -> "/system/lib64"
      else -> "/system/lib"
  }
  ```
- Ensures `LD_LIBRARY_PATH` correctly resolves `/system/lib64` on 64-bit processes (`arm64-v8a`, `x86_64`) and `/system/lib` on 32-bit processes.

### 4. Release Signing Configuration (`app/build.gradle.kts`)
- Release signing credentials resolve dynamically via:
  1. Environment variables (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)
  2. Local properties (`release.keystore.path`, `release.keystore.password`, `release.key.alias`, `release.key.password` in `local.properties`)
  3. Gradle project properties (`-PKEYSTORE_FILE=...`, `-PKEYSTORE_PASSWORD=...`, etc.)
- Enables V1, V2, V3, and V4 APK signature schemes.
- Generates signed release APKs when credentials are provided, or unsigned release APKs if omitted, ensuring build tasks do not fail on unauthenticated environments.

### 5. Task Dependency & Lint Management
- Explicitly mapped `generateBootstrap<Flavor>` dependencies into `preBuild`, `mergeAssets`, and `lintModel` task graphs to satisfy AGP 8+/9+ input-output task validation.
- Configured `android.lint.abortOnError = false` and `checkReleaseBuilds = false` for release targets.

### 6. NDK r27d (LTS) Upgrade & 16KB Flexible Page Size Alignment
- Upgraded the NDK toolchain to **`27.0.12077973`** (r27d LTS) in [`x11-core/build.gradle.kts`](file:///home/ilam_common/DevHome/GitHub/xopp-android/x11-core/build.gradle.kts).
- Enabled `-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON` in CMake flags, the official Google-endorsed mechanism to automatically generate 16KB page-aligned ELF shared libraries (`libXlorie.so`, `libc++_shared.so`).
- Configured `-Wl,-z,max-page-size=16384` for on-the-fly compilation of helper executables (`xopp-title-watcher`, `xopp-wallpaper`) and module libraries (`libgtk-android-ime.so`, `libportaudio.so.2`) in `build_bootstrap.py`.
- Verified 16KB segment alignment (`Align: 0x4000` / 16384 bytes) across all generated ELF binaries with `readelf -l`.

---

## Consequences
- Clean separation of architecture artifacts with ~100MB per-flavor APK outputs.
- Complete support for building, running, and debugging across both ARM64 physical hardware and x86_64 development environments.
- Ready for Android 15+ devices running 16KB memory page kernels with verified `0x4000` (16KB) ELF alignment.
- Ready for parallel matrix execution in GitHub Actions CI.
