# ADR 0009: Material 3 Expressive Document Hub, Responsive Navigation Shell, Headless Thumbnails & Smart X11 Lifecycle

## Status
Accepted

## Context
1. **Material 3 Expressive (M3E) & Responsive Form Factors**: Tablet, foldable, and desktop-class display environments require adaptive multi-column layouts and responsive navigation rails to avoid wasted horizontal space while maintaining mobile bottom bar ergonomics.
2. **Document Hub Operations & File Hierarchy**: Users need deep subfolder navigation (`~/Notes/<Subject>/`), custom folder color metadata (`.folder.json`), batch actions (select all, invert, batch PDF/note share), and a non-destructive Trashcan (`.Trash/`) with restore support.
3. **Headless Page-0 Previews**: Users need visual thumbnail previews for `.xopp`, `.xoj`, and `.pdf` notes without starting an X11 display session.
4. **Strict Format Ingestion**: To prevent file manager clutter, only files openable by Xournal++ (`.xopp`, `.xoj`, `.pdf`) should be indexed, ignoring non-openable hidden system files.
5. **Smart X11 Back Gesture & Lifecycle**: When pressing Back in canvas mode, the system should trigger Xournal++'s native `Ctrl+Q` close sequence (which shows GTK's native Save/Discard/Cancel dialog if unsaved). In case of X11 freeze, 3 rapid back presses trigger an emergency force close dialog that is immune to accidental dismissal by subsequent back taps.

## Decision

1. **Responsive Material 3 Expressive Shell (`MainActivity.kt`)**:
   - Implemented `MainResponsiveAppShell` using `BoxWithConstraints`:
     - Width >= 600dp (Tablets / Landscape): Left `NavigationRail` with expressive icon containers and page titles.
     - Width < 600dp (Phones / Portrait): Bottom `NavigationBar`.
   - Embedded pages: **Notes**, **Settings**, and **About & Licenses**.

2. **Headless Page-0 Thumbnail Generation (`ThumbnailManager.kt`)**:
   - Utilizes Android's native `android.graphics.pdf.PdfRenderer`.
   - `.pdf` files are rendered directly from disk.
   - `.xopp` / `.xoj` notes are converted headlessly to temporary PDFs via `PdfExportManager` and rendered to disk-cached 400px PNGs in `cacheDir/thumbnails/`.

3. **Subfolders, `.folder.json` Metadata & Trashcan (`DocumentRepository.kt`)**:
   - Strictly scans `.xopp`, `.xoj`, and `.pdf` formats.
   - Folder theming supported via `.folder.json` (`{"color": "#..."}`).
   - Non-destructive `.Trash/` directory storing deleted notes and `.trash_manifest.json` mapping original absolute paths for 1-tap restore.

4. **Multi-Selection & Batch Operations (`DocumentHubScreen.kt`)**:
   - Long-press contextual action bar: Select All, Invert Selection, Batch Export to PDF, Batch Share as PDF, Batch Share as `.xopp`, and Batch Move to Trash.
   - Hero greeting, recent note jump-back card, and quick-action speed pills (*New Note*, *Import PDF*, *New Folder*).

5. **Smart Native X11 Back Button & Non-Dismissible Force Close (`CanvasActivity.kt` & `CanvasSessionManager.kt`)**:
   - Back button transmits `Ctrl+Q` via `xdotool` to trigger GTK's native save confirmation dialog.
   - `CanvasSessionManager.setOnProcessExitListener` automatically finishes the activity when Xournal++ terminates cleanly.
   - 3 back presses in 2s activates an emergency force close dialog configured with `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)` so that 4th and subsequent back presses cannot dismiss the prompt.

## Backwards Compatibility
- All Jetpack Compose Material 3 Expressive components, spring physics animations, and shape tokens are bundled directly in the APK and run across all supported Android versions down to `minSdk` (API 26 / Android 8.0 Oreo).
- Dynamic system wallpaper colors (`dynamicColor`) activate on Android 12+ (API 31+), gracefully falling back to curated Material 3 Expressive palettes on Android 8–11.
