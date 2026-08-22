# ADR 0001: Architectural Foundation

## Status
Accepted

## Context
We need to run Xournal++ (a GTK3/Cairo/C++ desktop app) on Android seamlessly without root access. Android's native runtime environment does not support X11 or standard desktop Linux toolchains directly. However, porting the entire Cairo and GTK stack to pure Android is not feasible.

## Decision
We will use a hybrid approach:
1. **Embedded Linux Userland**: We will package standard Linux binaries built for aarch64 (using Termux build recipes) inside the app's internal `/data/data/<package_name>/files/usr/` sandbox.
2. **Display Server**: We will bundle `Termux-X11` as an embedded native library (C/NDK) starting an X11 server over UNIX domain sockets.
3. **Window Manager**: `matchbox-window-manager` will be used to enforce kiosk mode (fullscreen) with modal dialog support.
4. **Native Android Shell**: We will build the launcher, file manager (SAF/WebDAV integration), settings editor, and process supervisor using Jetpack Compose and Material 3.
5. **No Root/Chroot**: Execution will happen via straightforward `exec` directly from the app's internal storage, avoiding PRoot overhead and root dependencies.

## Consequences
- Requires a large bootstrap archive (`bootstrap.tar.xz`).
- The entire project must be GPL-3.0 compliant because of Termux-X11.
- No direct OpenGL acceleration (VirGL is excluded); drawing will rely on CPU Cairo rendering via MIT-SHM.
- Input events (stylus, pressure) must be manually translated from Android `MotionEvent` to X11 events.
