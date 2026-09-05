# ADR 0016: Multi-Window Management, Floating Toolbar Switcher, and Gallery Architecture

## Status
Accepted

## Context
1. **Multi-Window X11 Workflow**: Aournal++ supports opening multiple concurrent Xournal++ document windows under the embedded Openbox window manager. Users require an intuitive, native-feeling mechanism to switch between open notes directly from the canvas without having to exit to the Document Hub.
2. **Window Stacking & Title Desynchronization**: Previously, the floating toolbar displayed static document titles derived from the launch intent or initial file path, failing to track the actively focused window when multiple documents were open simultaneously.
3. **Alt+Tab and Gallery Ergonomics**: Users needed two complementary switching paradigms:
   - **Quick Switching**: Immediate sequential cycling across open windows with a single tap (analogous to Alt+Tab).
   - **Visual Gallery**: An expressive, multi-card overview on long-press displaying live previews, window titles, and individual window closing capabilities.
4. **Conditional Disclosure**: The window switcher control should only be presented when multiple windows exist (`openWindowCount > 1`), reducing visual clutter during single-document sessions.

---

## Decision

### 1. Multi-Window IPC Daemon (`xopp-title-watcher.c`)
An optimized native C watcher process monitors the X11 root window and window hierarchy:
- Subscribes to `_NET_CLIENT_LIST`, `_NET_ACTIVE_WINDOW`, and `_NET_WM_NAME` PropertyNotify events.
- Emits structured window updates to standard output:
  ```text
  WINDOWS:<active_window_id>|<window_id>:<window_title>|<window_id>:<window_title>...
  ```
- Ignores transient tooltips, popups, and dropdown menus (`override_redirect = true`).

---

### 2. Supervisor State Management (`ProcessSupervisor.kt` & `CanvasSessionManager.kt`)
The native watcher output is ingested and exposed as reactive state:
- `openWindows: StateFlow<List<X11WindowInfo>>`: Tracks all viewable document windows and identifies which window currently holds focus (`isActive = true`).
- `documentTitle: StateFlow<String?>`: Automatically synchronized with the active window's title, stripping asterisks (`*`) and X11 app suffixes (` - Xournal++`).
- Window activation is managed via `activateWindow(windowId: String)`, which interfaces with `xdotool windowactivate --sync`.

---

### 3. Floating Toolbar Window Switcher Lifecycle
Integrated directly into `FloatingToolbarOverlay`:
- **Conditional Visibility**: Only rendered when `showWindowSwitcher && openWindowCount > 1`.
- **Badging**: Displays a Material 3 badge indicating the total count of open windows.
- **Single-Tap Action (`onClick`)**: Executes `performQuickSwitch()`, cycling forward to `(currentIdx + 1) % openWindows.size`.
- **Long-Press Action (`onLongClick`)**: Captures the current window preview and opens the `WindowSwitcherGallery` overlay with haptic feedback.
- **Title Animation**: Window titles in both expanded and collapsed toolbar states animate using `AnimatedContent` backed by `SpringSlideTransition`, dynamically inferring forward or backward slide direction based on window indices.

---

### 4. Window Switcher Gallery Overlay (`WindowSwitcherGallery.kt`)
A full-screen modal overlay presenting a carousel of all active windows:
- **Visual Backdrop**: Uses `DreamyStarsBackground` with reduced strength (`0.65f`) and a 60% black scrim, matching the aesthetic of the Document Hub preview overlay.
- **Preview Cards**: Displays cached window previews, document title, and active badge indicator.
- **Close Button with Hover Highlight**: Each card features a close button with a distinctive red hover/pressed tint (`MaterialTheme.colorScheme.errorContainer`), executing `sessionManager.closeSpecificWindow(id)`.
- **Active Window Selection Suppression**: If the user selects the window that is already focused, the transition animation is suppressed to avoid redundant visual motion.

---

### 5. Automated Verification Standards
All window switching routing, visibility thresholds, and close behaviors are strictly verified in `FloatingToolbarWindowSwitcherTest.kt`:
- Forward and wrap-around window cycling calculations.
- Visibility suppression when `openWindowCount <= 1`.
- Close button behavioral routing (`foreground` vs `all_sequential`).
- Selection animation suppression for already-focused windows.

---

## Consequences

- **Seamless Multi-Tasking**: Enables desktop-class document switching directly from the canvas interface.
- **Accurate Toolbar State**: Guarantees the toolbar header and title always reflect the currently active X11 window.
- **Predictable UX**: Prevents clutter by hiding the switcher button when only one note is open.
- **Maintainability**: Clear separation between low-level X11 window tracking in `xopp-title-watcher.c` and high-level Compose UI in `CanvasActivity.kt`.
