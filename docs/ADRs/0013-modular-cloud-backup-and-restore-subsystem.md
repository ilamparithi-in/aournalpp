# ADR 0013: Modular Cloud Backup and Restore Subsystem (Nextcloud, WebDAV, Google Drive, SFTP, SMB3, FTP)

## Status
Accepted

## Context
1. **Multi-Protocol Cloud Synchronization**: Users require flexible cloud backup and restoration across diverse remote storage endpoints:
   - Nextcloud & Generic WebDAV (HTTP WebDAV methods via OkHttp)
   - Google Drive (REST API v3 over OkHttp without Google Play Services dependencies)
   - Generic SFTP (SSH File Transfer Protocol via `sshj`)
   - Generic SMB3 / Samba (Server Message Block v2/v3 via `smbj`)
   - Generic FTP / FTPS (Explicit & Implicit TLS via Apache Commons Net)
2. **Dual-Mode Backup Architecture**:
   - **Complete Backup**: Direct mirroring of local `$HOME` (`Notes/` including in-situ `Emergency Saves/` and `.config/xournalpp/`) under `<Remote Root>/Aournalpp/`.
   - **Custom Mappings**: Granular mapping of individual local directories (e.g. `~/Notes/Biology` or external folders) to arbitrary remote server paths.
   - Multiple cloud services can run concurrently with their own complete backup toggles and custom folder mappings.
3. **Differential Sync & Performance**: Unchanged files must not waste network bandwidth or battery. SHA-256 content hashes must be tracked in a Room database (`sync_metadata`) to achieve zero-byte transfer skips on unmodified documents, paired with configurable concurrency (1 to 4 parallel transfer workers).
4. **Configurable Exclusion Filters**: The system must enforce default transient filtering (`.autosave.xopp`, `.sock`, `.tmp`, `.swp`, `.X0-lock`, `bootstrap_installed.ver`, binary caches) while allowing users to define custom filename regex patterns, extension inclusion/exclusion lists, and excluded folder paths.
5. **Secure Credential Vault**: Server credentials, SSH private keys, and OAuth2 tokens must be stored in hardware-backed `EncryptedSharedPreferences` via Android Jetpack Security `MasterKey` (AES-256 GCM).
6. **Foreground Service & Automation**: Background backups orchestrated via `androidx.work.CoroutineWorker` (`BackupWorker`) with Android 14+ `FOREGROUND_SERVICE_TYPE_DATA_SYNC` notifications, daily scheduled sync, and on-app-exit hooks.
7. **First-Class Mobile & Tablet Navigation**: Dedicated "Cloud" tab in `MainActivity` with live `FileQueueSheet` progress tracking, transfer speed indicators, and conflict resolution policy controls (`Keep Newer`, `Overwrite Local`, `Skip Conflicts`).

## Decision

1. **Provider Abstraction (`CloudStorageProvider.kt` & `StorageProviderFactory.kt`)**:
   - Created clean protocol interfaces decoupling transport layers from note domain semantics.
   - Built specialized providers: `WebDavStorageProvider`, `GoogleDriveProvider`, `SftpStorageProvider`, `SmbStorageProvider`, and `FtpStorageProvider`.
2. **Room Differential Sync Database (`SyncDatabase.kt`)**:
   - Created `sync_metadata` table keyed by composite `(serviceId, relativePath)` with SHA-256 checksums and timestamps.
3. **Local Scanner with Configurable Filters (`BackupScanner.kt`)**:
   - Scans `Notes/` (in-situ emergency recovery notes) and `.config/xournalpp/`.
   - Evaluates default transient exclusions and user-configured regex, extension, and path filters.
4. **Live Transfer Queue & Concurrency Controller (`FileTransferQueueManager.kt`)**:
   - StateFlow-based real-time queue tracking active, queued, completed, and failed transfers with per-item progress, transfer speed (KB/s or MB/s), and cancellation controls.
   - Semaphore-based concurrency pipeline for parallel uploads and downloads.
5. **Background Automation & WorkManager (`BackupWorker.kt` & `BackupScheduler.kt`)**:
   - Implemented `CoroutineWorker` with foreground notification and Wi-Fi / battery constraints.
   - Added automated on-exit sync triggering in `MainActivity.onStop()`.
6. **Material 3 Cloud Hub (`CloudScreen.kt`, `ServiceConfigDialog.kt`, `CustomMappingDialog.kt`, `FileQueueSheet.kt`)**:
   - Integrated as the 3rd top-level navigation tab in `MainActivity` (Bottom Bar & Navigation Rail).
   - Dynamic provider forms with inline connection verification.
   - Conflict-aware restoration engine with automatic Xournal++ configuration sanitation (`ensureXournalppSettings()`, `checkAndOverrideAutoloadPreference()`).

## Consequences
- Seamless backup and restore across all major personal cloud protocols (Nextcloud, Google Drive, SFTP, SMB3, FTP/FTPS).
- Zero-byte differential sync for unchanged notes.
- Users can map individual folders to different servers simultaneously.
- Clean separation of UI, background worker, database, and protocol providers.
