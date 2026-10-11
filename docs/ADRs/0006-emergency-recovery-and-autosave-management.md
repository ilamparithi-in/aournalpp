# ADR 0006: Emergency Recovery Auto-Quarantine and On-Open Autosave Resolution

## Status
Accepted

## Context
1. **Intrusive X11 Emergency Save Prompt**: When Xournal++ terminates unexpectedly (e.g., system OOM, force-stop, or crash), it generates an `emergencysave.xopp` file in its configuration directory (`~/.config/xournalpp/emergencysave.xopp`). On subsequent launches, Xournal++'s native GTK code displays a blocking modal X11 dialog asking whether to restore the previous session. In our Android environment, this dialog disrupts the immersive mobile experience and blocks initialization.
2. **Hidden Autosave Files**: During active note-taking, Xournal++ periodically generates hidden autosave files in the document directory (e.g. `.{file-name}.autosave.xopp` or `.{file-name}.xopp~`). These files should not clutter the primary document list by default, but users must be informed when newer autosaved edits exist and given full control to review and resolve them.

## Decision

1. **Startup Emergency Save Auto-Quarantine**:
   - In [LinuxEnvironment.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/LinuxEnvironment.kt), `checkAndQuarantineEmergencySave()` checks for `File(xournalConfigDir, "emergencysave.xopp")`.
   - If present, the file is immediately moved to internal cache (`quarantined_emergencysave.xopp`) and deleted from the config directory.
   - **Result**: Xournal++ launches smoothly without displaying any legacy X11 modal dialogs.

2. **Native Material 3 Launch Recovery Prompt**:
   - On app startup in [DocumentHubScreen.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/ui/DocumentHubScreen.kt), if a quarantined emergency session is detected, a Material 3 dialog presents the user with three options:
     - **Open Now**: Automatically stages the recovered session directly into the user's active Notes folder as a timestamped note (e.g. `Recovered_Session_yyyyMMdd_HHmm.xopp`), clears the quarantine cache, and opens it in `CanvasActivity`. When the user closes the session, the note remains safely in their Notes folder, and the recovery prompt does not re-appear.
     - **Save to Notes**: Prompts for a custom filename and writes the file directly into the user's active Notes folder.
     - **Discard**: Purges the quarantined recovery file.

3. **Hidden Autosave File Pairing & On-Open Interception**:
   - [DocumentRepository.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/data/DocumentRepository.kt) matches hidden autosaves against their parent `.xopp` documents.
   - Hidden files starting with `.` or ending in `~` are excluded from the main document list by default (a "Show Hidden Files" toggle is available in the Top App Bar menu and Settings).
   - Document cards with matching autosaves display a standard **Material 3 Assist Badge** (`secondaryContainer`).
   - **On-Open Interception**: When tapping a note with an autosave, the app intercepts navigation and presents the **Autosave Resolution Dialog**:
     - Displays timestamp and file size comparisons along with relative freshness (*"Autosave is 5 min newer"* vs *"Autosave is 2 hr older"*).
     - **Replace with Autosave**: Highlighted filled button when autosave is newer; de-emphasized outlined button when older.
     - **Keep Both**: Backs up the autosave as a timestamped file (`{file}_autosave_{timestamp}.xopp`) and opens the note.
     - **Keep Existing**: Deletes the autosave and opens the original note.

4. **Unified On-Open Dialog Precedence Hierarchy**:
   To avoid conflicting or overlapping modal dialogs and prevent accidental data loss, every note/file opening pathway (Document Hub, Home screen, external file intents, and secondary window requests) must evaluate conditions in a strict 4-tier deterministic precedence order:
   - **Tier 1 — Emergency Recovery (`EmergencyRecoveryDialog`)**:
     - **Condition**: A genuine unhandled emergency recovery file (`quarantined_emergencysave.xopp`) exists in the cache quarantine directory from an unexpected crash or force-kill.
     - **Action**: Intercepts and defers opening the target note. The user must resolve the emergency session (*"Open Now"*, *"Save to Notes"*, or *"Discard"*) before opening any other document.
   - **Tier 2 — Active Note Conflict (`ActiveNoteOpenPromptDialog`)**:
     - **Condition**: The target document is currently open in an active X11 window tracked by [ActiveNotesTracker.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/ActiveNotesTracker.kt).
     - **Action**: Presents a prompt offering to bring the existing window to focus (*"View Note"*) or open a separate concurrent instance (*"Open in New Window"*).
   - **Tier 3 — Hidden Autosave Resolution (`AutosaveResolutionDialog`)**:
     - **Condition**: The note is not actively open in canvas, and a paired hidden autosave file (`.{name}.autosave.xopp`, `.{name}.xopp~`, etc.) is detected on disk.
     - **Action**: Halts navigation to present the comparative resolution dialog (*"Replace with Autosave"*, *"Keep Both"*, or *"Keep Existing"*). The note is never opened in Xournal++ until the autosave choice is explicitly made.
   - **Tier 4 — Action Choice (`NoteOpenActionDialog`)**:
     - **Condition**: None of Tiers 1–3 are triggered, and the user's default open action preference is set to `"ask"` (Ask every time).
     - **Action**: Prompts the user to select whether to open the note for editing in Xournal++ Canvas or render it as a view-only PDF.

## Consequences
- No legacy X11 modal recovery popups will ever block the canvas or startup.
- Emergency crash data is safely captured, presented natively in Material 3, and can be saved or resumed with 1 tap.
- Autosaves are seamlessly managed with zero clutter in the document list and complete protection against accidental overwrites.
- Deterministic dialog precedence prevents race conditions, ensuring that active session window focus, emergency crash recovery, autosave pairing, and user action preferences are consistently and predictably resolved across all UI screens.
