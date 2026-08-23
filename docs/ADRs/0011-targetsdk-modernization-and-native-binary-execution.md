# ADR 0011: Target SDK 36 Modernization, W^X SELinux Compliance & Dynamic Path Translation Shim

## Status
Accepted

## Context
1. **Target SDK & Google Play Policy**: Google Play Store mandates that applications target recent Android SDK versions (currently SDK 34/35/36). Running under legacy `targetSdk = 28` subjects apps to potential deprecation, security warnings, and rejection on app distribution platforms.
2. **Android 10+ W^X SELinux Security Policy**: Starting with Android 10 (API 29) and strictly enforced on modern Android platforms (API 34/35/36), Android's SELinux policy enforces W^X (Write XOR Execute) memory protection. Unprivileged apps cannot execute binaries (`execve`) from writable app data storage (such as `/data/data/<pkg>/files/usr/bin/`). Any attempt triggers `java.io.IOException: error=13, Permission denied` (`avc: denied { execute }`).
3. **Executable Relocation & Xournal++ Path Discovery**: When native executables are extracted to Android's read-only native library directory (`context.applicationInfo.nativeLibraryDir` $\rightarrow$ `/data/app/.../lib/<abi>/lib<name>.so`), they execute legally under SELinux. However, Xournal++ discovers its UI assets (`about.glade`, `main.glade`, settings, XML menus, icons) by querying `readlink("/proc/self/exe")` and navigating to parent directories. When executed from `nativeLibraryDir`, path lookup fails because the Android APK extraction directory contains no `share/xournalpp/` assets.
4. **App Update Safety & Extensibility**: Upgrades to the application and bootstrap assets must install seamlessly over existing installations without destroying user notes, configurations, or custom styles, while providing a clean pattern for future binary additions (audio recorders, helper daemons, interpreters).

---

## Decision

### 1. Target SDK Bump (`gradle/libs.versions.toml`)
- Bumped `targetSdk = "36"` (Android 16).

### 2. Native Library Packaging Architecture (`build_bootstrap.py` & `app/build.gradle.kts`)
- Configured `useLegacyPackaging = true` in Gradle packaging options and `android:extractNativeLibs="true"` in [`AndroidManifest.xml`](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/AndroidManifest.xml).
- Enhanced `scripts/build_bootstrap.py` with `--jnilibs-dir` export logic to stage all standalone native executables using the Android `lib<name>.so` naming convention:
  - `xournalpp` $\rightarrow$ `libxournalpp.so`
  - `openbox` $\rightarrow$ `libopenbox.so`
  - `xopp-title-watcher` $\rightarrow$ `libxopp_title_watcher.so`
  - `xopp-wallpaper` $\rightarrow$ `libxopp_wallpaper.so`
  - `gdk-pixbuf-query-loaders` $\rightarrow$ `libgdk_pixbuf_query_loaders.so`
  - `glib-compile-schemas` $\rightarrow$ `libglib_compile_schemas.so`
  - `xdotool` $\rightarrow$ `libxdotool.so`
  - `libxopp-shim.so` $\rightarrow$ `libxopp_shim.so`
  - `libgtk-android-ime.so` $\rightarrow$ `libgtk-android-ime.so`
- Wired `merge<Flavor>DebugJniLibFolders` task dependencies in Gradle so native binaries are automatically bundled into APK `lib/<abi>/` and extracted by Android's package installer into `nativeLibraryDir` with `-rwxr-xr-x` executable permissions.

### 3. Dynamic Executable Resolution (`LinuxEnvironment.kt`)
- Added `resolveExecutable(name: String): File` to `LinuxEnvironment`:
  1. Searches `nativeLibDir/lib<name_with_underscores>.so`
  2. Searches `nativeLibDir/lib<name>.so`
  3. Searches `nativeLibDir/<name>`
  4. Falls back to `binDir/<name>`
- Updated all runtime supervisors ([`ProcessSupervisor.kt`](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/ProcessSupervisor.kt), [`PdfExportManager.kt`](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/PdfExportManager.kt), [`CanvasSessionManager.kt`](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/CanvasSessionManager.kt), [`BootstrapInstaller.kt`](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/BootstrapInstaller.kt), and [`BootstrapViewModel.kt`](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/ui/BootstrapViewModel.kt)) to resolve processes via `resolveExecutable(...)`.

### 4. Executable Path Translation Shim (`scripts/xopp-shim.c`)
- Created a standalone C interception library (`scripts/xopp-shim.c`) compiled with 16KB page alignment into `libxopp_shim.so`.
- Intercepts `readlink()` and `readlinkat()` calls targeting `/proc/self/exe` and `/proc/thread-self/exe`.
- When invoked by Xournal++, returns `${PREFIX}/bin/xournalpp` (or `${HOME}/../usr/bin/xournalpp`), restoring standard relative lookup for `share/xournalpp/ui/about.glade`, XML menus, and icons without requiring binary patching or modifying upstream Xournal++ source code.
- Preloaded cleanly via `LD_PRELOAD = libxopp_shim.so` in `LinuxEnvironment.getEnvMap()`.

---

## Consequences

### Positive
- **Full Modern Android & Play Store Compliance**: Targets Android 16 (`targetSdk = 36`) with 100% compliance with Android SELinux W^X security rules.
- **Zero Loss of Functionality**: Full GTK3 UI, Lucide vector icons, custom palettes, Openbox window manager, text IME focus bridge, and document renderers operate seamlessly.
- **Atomic App Updates**: Android's native package manager manages the lifecycle of all native executables in `nativeLibraryDir`. App upgrades replace native binaries atomically while user data in `home/` and external storage remains untouched.
- **Clean Extensibility**: Future native CLI binaries (e.g. `ffmpeg`, `sox`, `arecord`, plugin interpreters) can be added simply by registering their names in `scripts/build_bootstrap.py` and invoking them via `env.resolveExecutable(...)`.

### Considerations
- Direct execution via raw shell paths like `sh -c /data/data/.../files/usr/bin/foo` is prohibited on modern Android; all standalone native executables must be packaged as `lib*.so` and resolved through `env.resolveExecutable(name)`.
