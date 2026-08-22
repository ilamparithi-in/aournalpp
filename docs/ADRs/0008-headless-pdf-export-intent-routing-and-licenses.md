# ADR 0008: Headless CLI PDF Conversion, Document Hub Management, External Intent Ingestion & Licenses Compliance

## Status
Accepted

## Context
1. **Headless PDF Generation**: In a mobile productivity environment, users frequently need to export or share `.xopp` notes as standard PDF documents without launching the graphical X11 environment or switching away from the Document Hub.
2. **Document Management (CRUD & Search)**: Users need real-time search filtering, renaming, duplicating, and deleting documents with immediate storage synchronization and autosave pairing.
3. **Third-Party App Interoperability (External Intent Routing)**: When users open `.xopp` or `.pdf` files from file managers (e.g., Google Files), cloud storage, or chat apps, the system passes incoming `content://` URIs with transient read permissions. The runtime must stage these files into persistent userland storage (`~/Notes/Imported/`) and route them to `CanvasActivity`.
4. **Open-Source Attribution & License Compliance**: Aournal bundles GPL-3.0, GPL-2.0, LGPL-2.1, and Apache-2.0 components (Xournal++, Termux-X11, Matchbox, GTK3, AndroidX). Strict license compliance requires accessible in-app license texts and upstream repository references.

## Decision

1. **Headless Background PDF Conversion via CLI (`PdfExportManager.kt`)**:
   - Implemented [PdfExportManager.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/PdfExportManager.kt) in `:runtime-manager`.
   - Utilizes Xournal++'s native headless CLI flag: `usr/bin/xournalpp -p <output_pdf_path> <input_xopp_path>`.
   - Executes asynchronously on `Dispatchers.IO` via `ProcessSupervisor.runBinary`, bypassing X11 viewport rendering.
   - Streams directly to user-selected SAF destinations via `contentResolver.openOutputStream` or caches into `cacheDir/shared_pdfs/` for instant Android share sheet actions.

2. **Full-Featured Document Hub & Note CRUD (`DocumentRepository.kt` & `DocumentHubScreen.kt`)**:
   - [DocumentRepository.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/data/DocumentRepository.kt):
     - Real-time search query filtering across primary notes storage, imported notes, and home directories.
     - Safe rename, duplicate, and delete operations with automatic synchronization of associated hidden autosave files (`.{name}.autosave.xopp`).
     - Sharing integrations via `FileProvider` (`ACTION_SEND` with `application/x-xopp` and `application/pdf`).
   - [DocumentHubScreen.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/ui/DocumentHubScreen.kt):
     - Integrated search bar with clear button.
     - Card action overflow menu (Export to PDF, Share as PDF, Share as .xopp, Rename, Duplicate, Delete).
     - Non-blocking background PDF generation progress indicators.

3. **External Intent Staging Resolver (`ExternalFileHandler.kt` & `MainActivity.kt`)**:
   - [ExternalFileHandler.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/utils/ExternalFileHandler.kt):
     - Resolves `content://` and `file://` URIs by querying `OpenableColumns.DISPLAY_NAME`.
     - Sanitizes filenames and stages payloads into `~/Notes/Imported/<timestamp>_<filename>`.
   - Configured `FileProvider` and intent filters in `AndroidManifest.xml` for `android.intent.action.VIEW` and `EDIT`.
   - [MainActivity.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/MainActivity.kt) processes external intents upon launch and via `onNewIntent`, routing staged files directly to `CanvasActivity`.

4. **Material 3 Open-Source Licenses Hub (`LicensesScreen.kt` & `LicensesActivity.kt`)**:
   - [LicensesScreen.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/ui/LicensesScreen.kt) presents bundled open-source packages:
     - Aournal Port: GPL-3.0
     - Xournal++: GPL-2.0-or-later
     - Termux-X11: GPL-3.0
     - Matchbox & Openbox: GPL-2.0
     - GTK3 Stack: LGPL-2.1
     - AndroidX / Jetpack: Apache-2.0
   - Provides full license text inspection modals and upstream source code links.
   - Accessible from both the Document Hub top menu and Settings screen.

## Consequences
- Fast, background PDF generation without visual disruption or activity switching.
- Complete document organization (search, rename, duplicate, delete) directly from the Document Hub.
- Seamless opening of `.xopp` and `.pdf` files from third-party Android apps.
- Full GPL-3.0 and GPL-2.0 compliance with attribution in the user interface.
