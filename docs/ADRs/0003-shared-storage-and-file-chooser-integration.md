# ADR 0003: GTK File Chooser Runtime Environment, Configurable Storage, and Openbox Dialog Integration

## Status
Accepted

## Context
When triggering file operations in Xournal++ (such as **File -> Save**, **File -> Save As**, and **File -> Export as PDF**), the embedded X11 canvas experienced complete blackouts, UI freezes, or native process crashes.

### Root Cause Analysis:
1. **D-Bus Portal Deadlock**: Modern GTK3 builds attempt to delegate file chooser requests to desktop portals (`org.freedesktop.portal.FileChooser`) over D-Bus by default. In an embedded Android runtime without an active system/session D-Bus daemon, synchronous D-Bus IPC calls time out or deadlock the GTK main event loop, causing the X11 drawing surface to freeze and render black frames.
2. **Missing Compiled GSettings Schemas (`GLib-GIO-ERROR`)**: When the GTK3 file chooser opens, it queries GSettings schemas (such as `org.gtk.Settings.FileChooser`) for dialog state and sorting preferences. If `gschemas.compiled` is missing from `GSETTINGS_SCHEMA_DIR`, GLib triggers a fatal `GLib-GIO-ERROR **: No GSettings schemas are installed on the system` and calls `abort()`, crashing Xournal++ with `SIGABRT` (signal 6) and destroying the canvas.
3. **GDK Pixbuf Loader Failures**: File dialogs load mimetype and folder icons; without a generated `loaders.cache` and correct `GDK_PIXBUF_MODULE_FILE` environment flag, pixbuf loaders fail or crash.
4. **Android Scoped Storage Constraints**: Linux binaries executed via native `exec` reside within the app's internal sandbox (`/data/data/<package>/files/home`), isolated from the user-accessible Android storage without symlinks and `MANAGE_EXTERNAL_STORAGE` permissions.
5. **Modal Stacking in Window Manager**: Kiosk-mode window managers (Openbox / Matchbox) must cleanly stack transient modal dialogs with focus and centering over the fullscreen canvas window.

## Decision

1. **Disable Desktop Portals & Force Local VFS**:
   - Injected `GTK_USE_PORTAL="0"` and `GIO_USE_VFS="local"` into `LinuxEnvironment.getEnvMap()`. This forces GTK3 to render its internal native file chooser dialog immediately without attempting D-Bus portal lookups.

2. **Self-Healing GSettings Schema & Loader Compilation**:
   - Implemented self-healing compilation routines in `LinuxEnvironment.ensureDirectoryTree()`. On every startup and session launch, `LinuxEnvironment` verifies if `gschemas.compiled` and `loaders.cache` exist. If missing, it immediately executes `glib-compile-schemas` and `gdk-pixbuf-query-loaders` with full `LD_LIBRARY_PATH` resolution.

3. **Configurable Notes Storage & Symlink Bridging**:
   - Defaulted note storage to `/sdcard/Documents/Notes`, with a configuration option in `SettingsActivity` allowing users to select any directory via system folder picker (SAF), common presets, or custom path.
   - Provisioned an atomic symbolic link from `$HOME/Notes` to the configured storage directory in `LinuxEnvironment.setupStorageSymlinks()`.

4. **Android Storage Management Permissions (`MANAGE_EXTERNAL_STORAGE`)**:
   - Declared `MANAGE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`, and `WRITE_EXTERNAL_STORAGE` in `AndroidManifest.xml`.
   - Added an automatic Material 3 `AlertDialog` and persistent banner in `DocumentHubScreen` prompting users to grant "All Files Access" via `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`.

5. **GTK Bookmarks & Xournal++ Default Directory Provisioning**:
   - Automatically generated `~/.config/gtk-3.0/bookmarks` pointing to the configured Notes folder ("Notes") and Downloads ("Downloads") so these locations appear directly in the GTK file chooser sidebar.
   - Initialized and synchronized `~/.config/xournalpp/settings.xml` (`defaultSaveDir` and `defaultOpenDir`) to match the configured Notes directory.

6. **Openbox Window Placement & Focus Configuration**:
   - Provisioned `~/.config/openbox/rc.xml` with `<placement>` center policy and `<focus>` rules so that modal dialogs (`type="dialog"`) appear centered and focused over the fullscreen Xournal++ window.

## Consequences
- GTK file choosers (Save, Save As, Export PDF) open instantaneously without D-Bus timeouts or schema crashes.
- Users have full freedom to pick their notes storage folder via Settings.
- Notes are saved directly to public storage, accessible to external Android file managers, backup tools, and sync services.
