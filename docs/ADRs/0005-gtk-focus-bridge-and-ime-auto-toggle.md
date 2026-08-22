# ADR 0005: GTK Text Focus Bridge and Automatic Soft Keyboard (IME) Toggle

## Status
Accepted

## Context
In an embedded X11 environment running on Android tablets, users expect the on-screen software keyboard (IME) to automatically appear when tapping into editable text fields (such as the **File $\rightarrow$ Save As** filename input, search boxes, page number inputs, and on-canvas text annotations). Conversely, when returning to stylus drawing on the canvas, the keyboard should dismiss automatically.

### Technical Constraint:
In X11, the X server only tracks **toplevel window surfaces** (`XWindow` IDs). GTK3 uses **client-side rendering**: all internal widgets (buttons, canvas, text entries) live within a single X11 surface. Focus changes between a canvas and a text entry happen entirely within GTK's client memory, making raw X11 focus events insufficient to detect text input.

## Decision

1. **Lightweight GTK Focus Bridge Module (`libgtk-android-ime.so`)**:
   - Implemented a custom GTK3 module [scripts/gtk-android-ime.c](file:///home/ilam_common/DevHome/GitHub/xopp-android/scripts/gtk-android-ime.c) compiled via NDK and loaded via `GTK_MODULES`.
   - Intercepts GTK's `set-focus` signal across all top-level windows and dialogs.
   - Evaluates whether the active widget is an editable text input:
     `GTK_IS_ENTRY(widget) || GTK_IS_TEXT_VIEW(widget) || GTK_IS_EDITABLE(widget)`
   - Emits `FOCUS_IN\n` upon focusing an editable text widget and `FOCUS_OUT\n` when focus shifts to non-editable widgets or the drawing canvas.

2. **Abstract Unix Domain Socket IPC (`@aournal_ime_bridge`)**:
   - The GTK module communicates with the host Android runtime over an abstract Linux domain socket (`@aournal_ime_bridge`).
   - Abstract namespace sockets require no filesystem paths, avoid Android sandboxing permissions, and clean up automatically on process exit.

3. **Android Runtime IME Controller**:
   - [CanvasSessionManager.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/CanvasSessionManager.kt) runs an asynchronous `LocalServerSocket` listener.
   - Upon receiving `FOCUS_IN`, it invokes `lorieView.setKeyboardVisible(true)` on the main thread.
   - Upon receiving `FOCUS_OUT`, it invokes `lorieView.setKeyboardVisible(false)`.

4. **User Preference Toggle in Settings**:
   - Added a user-configurable preference in [SettingsActivity.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/SettingsActivity.kt):
     - **Key**: `pref_auto_show_ime_on_focus` (Default: `true`).
     - Allows stylus-heavy users to disable automatic keyboard popups if they prefer manual control via the floating top header toggle.

## Consequences
- Tapping any text box or dialog input in Xournal++ automatically presents the Android software keyboard without requiring manual button taps.
- Returning to stylus writing dismisses the keyboard automatically.
- Stylus users retain full control to toggle the auto-popup behavior on or off in App Settings.
