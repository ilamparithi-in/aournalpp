# ADR 0012: Autoload Preference Override and Workspace Control

## Status
Accepted

## Context
1. **Xournal++ Native Autoload Setting**: In Xournal++ (under `Preferences > Load/Save`), there is a native configuration option: *"Enable autoloading of most recent file on application startup"* (`autoloadMostRecent="true"` in `~/.config/xournalpp/settings.xml`).
2. **Conflict with Aournal++ Workspace & Document Lifecycle**: In Aournal++ on Android, document lifecycle and recent notes are managed natively through modern Material 3 interfaces:
   - The Home Screen provides a dedicated, hero-sized *"Continue where you left off"* card showing the active note, autosave status, metadata, and quick PDF export/editing actions.
   - Starting a new canvas session (e.g. from the Speed Dial FAB or New Note button) expects a clean, empty canvas unless a specific document URI/file is supplied.
   - If Xournal++'s native `autoloadMostRecent` is set to `true`, Xournal++ automatically intercepts application startup and forces open the last note from its internal history, bypassing user intent and giving up startup control to Xournal++.
3. **Requirement**: Aournal++ must automatically override and clear `autoloadMostRecent` to `false` across all configuration channels (runtime initialization, GTK preferences dialog dismissal, session termination, external storage synchronization, and backup ZIP import), while clearly informing the user that this preference was cleared to preserve the native *"Continue where you left off"* workspace experience.

## Decision

1. **Active Override and Sanitization Pipeline**:
   - In [LinuxEnvironment.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/LinuxEnvironment.kt):
     - `ensureXournalppSettings()` guarantees that `autoloadMostRecent="false"` is provisioned in initial XML and actively enforced on existing `settings.xml` configurations.
     - `checkAndOverrideAutoloadPreference()` inspects `settings.xml`. If `autoloadMostRecent` (or legacy `autoloadLastFile`) is found to be enabled (`true`, `1`, `yes`, `on`), it replaces the property with `value="false"`, saves the file, and sets the persistent pending notification flag (`pref_pending_autoload_conflict_notification = true`).
   - In [CanvasSessionManager.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/CanvasSessionManager.kt):
     - Triggered immediately when the native X11 GTK Preferences window is dismissed (`!isPreferencesWindowOpen()`) and when terminating a canvas session in `stopSession()`.
   - In [NotesHomeConfigManager.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/NotesHomeConfigManager.kt) & [XournalConfigManager.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/runtime-manager/src/main/java/dev/ilamparithi/aournalpp/runtime/XournalConfigManager.kt):
     - Incoming `settings.xml` files imported from SAF or synced from external `.config` are sanitized immediately upon import.

2. **User Notification & Transparency**:
   - On [HomeScreen.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/ui/HomeScreen.kt) and [DocumentHubScreen.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/ui/DocumentHubScreen.kt), when `hasPendingAutoloadOverrideNotification()` is true, a Material 3 `AlertDialog` is presented:
     - **Title**: *"Startup Preference Overridden"*
     - **Message**: Explains that *"Enable autoloading of most recent file on application startup"* in Xournal++ Preferences conflicts with *"Continue where you left off"*, and confirms that the preference was cleared to `"false"`.
     - **Action**: *"Understood"* button dismisses the dialog and clears the notification flag.
   - In [SettingsActivity.kt](file:///home/ilam_common/DevHome/GitHub/xopp-android/app/src/main/java/dev/ilamparithi/aournalpp/SettingsActivity.kt):
     - An informative caption note is displayed beneath the *Native GTK Preferences Dialog* launcher card.
     - On activity resume, a Snackbar notification is shown if an override occurred.

## Consequences
- Xournal++ will never unexpectedly auto-open previous notes on blank note creation or launch.
- Aournal++'s *"Continue where you left off"* card retains full control over document resuming and workspace state.
- Users are fully informed why their preference in the native GTK settings was cleared, ensuring transparency and preventing confusion.
