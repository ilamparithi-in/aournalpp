# ADR 0007: Material 3 Configuration Hub and Direct Preferences Launch Engine

## Status
Accepted

## Context
1. **Xournal++ Configuration Access**: Xournal++ stores core application preferences in `~/.config/xournalpp/settings.xml`, custom toolbar button definitions in `toolbar.ini`, and custom palettes in `palette.gpl`. On Android, users require intuitive ways to inspect, backup, and restore these configuration files across devices via the Storage Access Framework (SAF).
2. **Configuration Semantics & Checking**: Configuration files contain diverse internal GTK and Xournal++ schema properties. Semantic verification is intentionally delegated to Xournal++ itself rather than the Android host, while Android provides basic structural validation (valid XML root tags, non-empty INI sections, valid GPL headers) and clear user notifications.
3. **Preferences Launching in Embedded X11**: Xournal++ does not provide a `--preferences` command-line flag. Users navigating from Android Settings expect to open the native GTK settings modal directly without having to manually locate the menu bar or press keyboard shortcuts. Additionally, if an unclean shutdown occurred and intelligent recovery is disabled, modal emergency recovery prompts must be handled gracefully without swallowing the shortcut.

## Decision

1. **Backend Configuration Manager (`XournalConfigManager.kt`)**:
   - Implemented in [:runtime-manager](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/XournalConfigManager.kt).
   - Supports individual file operations (`settings.xml`, `toolbar.ini`, `palette.gpl`, `colornames.ini`) and full ZIP backup/restoration.
   - Performs structural validation upon import:
     - `validateXmlStructure`: Uses Android's `XmlPullParser` to ensure well-formed XML and root tag presence.
     - `validateIniStructure`: Ensures non-empty INI key-value and section definitions.
     - `validateGplStructure`: Verifies standard `GIMP Palette` header.
   - Clarifies to users that semantic value validation is handled directly by Xournal++.

2. **Material 3 Configuration Hub (`ConfigViewerDialog.kt` & `SettingsActivity.kt`)**:
   - [ConfigViewerDialog.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/ui/ConfigViewerDialog.kt): Tabbed modal displaying formatted monospace code with line numbers, copy-to-clipboard button, and clear schema disclaimers.
   - [SettingsActivity.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/SettingsActivity.kt):
     - **Preferences Editor Card**: Direct launch into GTK preferences via `CanvasActivity(EXTRA_OPEN_PREFERENCES=true)`.
     - **Backup & Portability Card**: SAF `CreateDocument` and `OpenDocument` integrations with real-time `Snackbar` notifications.

3. **Direct Preferences Launch Engine via Window Monitoring & Shortcut Injection**:
   - Added `xdotool` to `ROOT_PACKAGES` in [scripts/build_bootstrap.py](file:///home/ilam_common/DevHome/GitHub/xopp-android/scripts/build_bootstrap.py).
   - In [CanvasSessionManager.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/CanvasSessionManager.kt):
     - Session startup runs `env.ensureDirectoryTree()`, ensuring emergency crash sessions are auto-quarantined when intelligent recovery is enabled.
     - When `openPreferencesOnLaunch = true`, `monitorAndInjectPreferencesShortcut()` initiates a coroutine that monitors X11 window state using `xdotool search --name "Preferences"`.
     - Injects `Ctrl+,` (`ctrl+comma`) once the Xournal++ main window is visible and focused. If a modal dialog (e.g. legacy recovery) is open, subsequent attempts trigger as soon as the user resolves the dialog, terminating immediately once the Preferences window is mapped.

## Consequences
- Users can view, backup, and restore individual configuration files (`settings.xml`, `toolbar.ini`, etc.) and complete ZIP archives through modern Android SAF dialogs.
- Tapping "Edit Settings" in Android Settings smoothly launches the canvas in kiosk mode and automatically displays the native Xournal++ Preferences dialog.
- Recovery dialogs and modal states do not break shortcut injection.
- Users are clearly informed that configuration values are interpreted directly by Xournal++.
