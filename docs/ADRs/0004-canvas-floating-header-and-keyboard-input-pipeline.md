# ADR 0004: Canvas Floating Header, Live X11 Window Title Monitoring, and Keyboard Pipeline

## Status
Accepted

## Context
1. **Document Visibility & Canvas Space**: When the legacy X11 window titlebar was removed for fullscreen note-taking, users lost visibility of the currently open note file name. Furthermore, when a new note was saved or renamed in Xournal++, the UI needed to reflect the new document title in real time.
2. **Keyboard Access without Bottom Toolbar Clutter**: Termux-X11's default bottom toolbar consumed vertical screen real estate and interfered with palm rejection during stylus writing. A streamlined way to toggle the software keyboard on-demand without permanent bottom bars was required.
3. **Hardware & Software Keyboard Mapping**: Only standard alphanumeric letter keys were functional in Xournal++. Crucial keys—such as `Control`, `Alt`, `Shift`, `Backspace` (`KEYCODE_DEL`), `Tab`, `Escape`, and `Arrow` keys (`KEYCODE_DPAD_*`)—failed to function.

## Root Cause Analysis
1. **Activity-Level Key Interception**: Android's `ComponentActivity` intercepts navigation keys (`DPAD`, `TAB`, `DEL`, `ESCAPE`) for focus traversal and consumes modifier keys before embedded `View` listeners receive them. Without overriding `Activity.dispatchKeyEvent()`, these events were swallowed before reaching `LorieView`.
2. **Scancode Calculation in `InputEventSender`**: When modifiers were active (`!no_modifiers`), `InputEventSender` took raw `e.getScanCode()`. On many Android devices, hardware scan codes do not match Linux evdev codes or return 0, causing the native X11 layer to discard the keypress.
3. **Dynamic Title Propagation**: Xournal++ updates its document title via standard EWMH (`_NET_WM_NAME`) and ICCCM (`WM_NAME`) window properties. Without a dedicated X11 property listener, changes made inside Xournal++ remained invisible to Android Jetpack Compose.

## Decision

1. **Floating Material 3 Document Header & Keyboard Toggle**:
   - Implemented a floating, collapsible pill header in [CanvasActivity.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/CanvasActivity.kt):
     - Displays the live document title (e.g. `Calculus_Lecture.xopp` or `New Note`).
     - Includes a 1-tap **Keyboard Toggle** action button to show/hide the soft keyboard on demand (`lorieView.toggleKeyboardVisible()`).
     - Supports expanding and collapsing into a minimalist pill so users have 100% borderless canvas when writing with a stylus.
     - Includes a safe exit confirmation action.

2. **Real-time X11 Title Watcher (`xopp-title-watcher`) with Modal Dialog Filtering**:
   - Created a lightweight C companion binary [xopp-title-watcher.c](file:///home/ilam_common/DevHome/GitHub/xopp-android/scripts/xopp-title-watcher.c) compiled with NDK and linked with `libX11.so`.
   - Listens for X11 `PropertyNotify` events (`_NET_WM_NAME` / `WM_NAME`) and emits title updates to stdout.
   - **Dialog & Transient Filtering**: Checks ICCCM `WM_TRANSIENT_FOR` hints and EWMH `_NET_WM_WINDOW_TYPE_DIALOG` to ignore temporary modal popups (such as "Save File", "Open Document", "Preferences"), ensuring the floating header exclusively displays the main document name.
   - [ProcessSupervisor.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/ProcessSupervisor.kt) reads title updates from the process stream, sanitizes the name, filters out system/window manager strings, and exposes a reactive `documentTitle: StateFlow<String?>`.
   - `CanvasActivity` observes `sessionManager.documentTitle.collectAsState()`, updating the floating top header instantly when a document is saved or renamed.

3. **Full Key Event Forwarding in Activity Dispatch**:
   - Overrode `CanvasActivity.dispatchKeyEvent(event: KeyEvent)` to forward all hardware and software key events directly to `TouchInputHandler.sendKeyEvent(event)`.

4. **Standardized Keycode Resolution in `InputEventSender`**:
   - Updated `InputEventSender.java` so that default input modes rely on `android_to_linux_keycode`, guaranteeing that `Ctrl`, `Alt`, `Shift`, `Backspace`, `Tab`, `Esc`, `Arrows`, `Home`, `End`, and all alphanumeric keys map directly to standard Linux X11 keysyms.

5. **Enhanced View Focusability in `X11Viewport`**:
   - Configured `FrameLayout` and `LorieView` with `isFocusable = true`, `isFocusableInTouchMode = true`, and `lorieView.requestFocus()`.

## Consequences
- The open note title updates instantly whenever a note is saved or renamed inside Xournal++ without polling.
- Soft keyboard is easily accessible with a single tap from the floating header without any intrusive bottom bars.
- Full keyboard functionality (physical keyboard shortcuts like `Ctrl+Z`, `Ctrl+S`, `Ctrl+Y`, arrow navigation, Backspace, Tab, and soft keyboard input) works seamlessly.
