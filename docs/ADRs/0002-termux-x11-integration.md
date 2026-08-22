# ADR 0002: Termux-X11 Integration, Lenovo Pen Plus Patch, and Canvas Session Orchestration

## Status
Accepted

## Context
Xournal++ requires an X11 display server to render its GTK3 UI and receive input events (touch, mouse, keyboard, and pressure-sensitive stylus). The upstream Termux-X11 engine provides a battle-tested standalone X11 server and Android surface integration (`LorieView` + native `liblorie.so`). Furthermore, users with hardware like the Lenovo Pen Plus require barrel button event mapping (keycodes 600-604) to primary/secondary/tertiary clicks and toggle modes. Finally, users must retain full control over X11 configurations (resolutions, scaling, input modes, key mappings, and stylus behaviors) without the underlying engine being hidden.

## Decision
1. **Upstream Termux-X11 Submodule & Clean Patching**:
   - Integrated upstream `https://github.com/termux/termux-x11` as a recursive Git submodule at `submodules/termux-x11`.
   - Applied hardware extension patch supporting Lenovo Pen Plus barrel button gesture mapping (`LenovoPenButtonMapper`, `LenovoPenButtonListener`, `StylusState`, and associated settings).
2. **`:x11-core` Architecture & Build System**:
   - Configured `:x11-core` to compile Termux-X11 sources, AIDL interfaces, and native CMake C/C++ libraries.
   - Implemented automated Gradle tasks `generatePrefs` and `generateShortcuts` during pre-build to generate `Prefs.java` and shortcut definitions.
   - Wrapped `LorieView` inside Jetpack Compose `X11Viewport`.
3. **Session Lifecycle Orchestration (`CanvasSessionManager`)**:
   - Supervised runtime execution flow:
     1. Ensure `.X11-unix` socket directory exists in `TMPDIR`.
     2. Connect `LorieView` to display `:0`.
     3. Start `matchbox-window-manager` in kiosk mode.
     4. Launch `xournalpp` with zero arguments for new blank notes, or with a target `.xopp` file path for existing notes.
4. **Immersive Full-Screen Note-Taking (`CanvasActivity`)**:
   - Set sticky immersive mode hiding system navigation/status bars to prevent gesture conflicts while drawing.
   - Enabled `FLAG_KEEP_SCREEN_ON` during active sessions.
   - Ensured atomic teardown on back press and activity destruction to terminate child processes (`ProcessSupervisor.terminateAll()`).
5. **Open Settings Architecture**:
   - Termux-X11 preference screens (`LoriePreferences`) are directly accessible from the Document Hub top app bar, ensuring users retain full freedom over display scaling, touch modes, and pen mappings.

## Consequences
- Requires NDK toolchain to compile Termux-X11's embedded X server native components.
- GPL-3.0 compliance is strictly maintained across `:x11-core`.
- High responsiveness and pressure sensitivity are achieved directly through `LorieView`'s unbuffered touch dispatch and MIT-SHM frame delivery.
