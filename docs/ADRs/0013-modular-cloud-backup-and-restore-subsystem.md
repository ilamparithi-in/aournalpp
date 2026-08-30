# ADR 0013: Modular Cloud Backup and Restore Subsystem (Nextcloud, WebDAV, Google Drive, SFTP, SMB3, FTP)

## Status
Accepted

## Context
1. **Multi-Protocol Cloud Synchronization**: Users require flexible cloud backup and restoration across diverse remote storage endpoints:
   - Dedicated Cloud Services: Google Drive (REST API v3 / OAuth2 PKCE), Nextcloud (WebDAV + App Password QR Code Scanner)
   - Standard Protocols: FTP / FTPS (Explicit & Implicit TLS), SFTP (SSH File Transfer Protocol via `sshj`), SMB3 / Samba (Server Message Block v2/v3 via `smbj`), WebDAV
2. **Dual-Mode Backup Architecture**:
   - **Complete Backup**: Direct mirroring of local `$HOME` (`Notes/` including in-situ `Emergency Saves/` and `.config/xournalpp/`) under `<Remote Root>/Aournalpp/`.
   - **Custom Mappings**: Granular mapping of individual local directories (e.g. `~/Notes/Biology` or external folders) to arbitrary remote server paths, browsable via hierarchical local and remote directory pickers and accessible directly from folder 3-dot menus.
   - Multiple cloud services can run concurrently with their own complete backup toggles and custom folder mappings.
3. **Differential Sync & Performance**: Unchanged files must not waste network bandwidth or battery. SHA-256 content hashes must be tracked in a Room database (`sync_metadata`) to achieve zero-byte transfer skips on unmodified documents, paired with configurable concurrency (1 to 4 parallel transfer workers).
4. **Configurable Exclusion Filters**: The system must enforce default transient filtering (`.autosave.xopp`, `.sock`, `.tmp`, `.swp`, `.X0-lock`, `bootstrap_installed.ver`, binary caches) while allowing users to define custom filename regex patterns, extension inclusion/exclusion lists, and excluded folder paths, protected with long-press reset safeguards.
5. **Secure Credential Vault**: Server credentials, SSH private keys, and OAuth2 tokens are stored in hardware-backed `EncryptedSharedPreferences` via Android Jetpack Security `MasterKey` (AES-256 GCM) with account uniqueness validation.
6. **Automation & Synchronization**: Background backups orchestrated via `androidx.work.CoroutineWorker` (`BackupWorker`) with Android 14+ `FOREGROUND_SERVICE_TYPE_DATA_SYNC` notifications, daily scheduled sync, OneNote-style fast periodic foreground intervals (5m, 15m, 30m, 1h), on-app-exit hooks, and startup cloud change checks.
7. **Top-Level Navigation & Quick Sync**: Dedicated "Cloud" tab in `MainActivity` with Speed-Dial FAB, full-page Transfer Queue subpage, and Quick Sync indicator buttons in Home and Document Hub top app bars.

## Decision

1. **Provider Abstraction (`CloudStorageProvider.kt` & `StorageProviderFactory.kt`)**:
   - Created clean protocol interfaces decoupling transport layers from note domain semantics.
   - Built specialized providers: `WebDavStorageProvider`, `GoogleDriveProvider`, `SftpStorageProvider`, `SmbStorageProvider`, and `FtpStorageProvider`.
2. **Nextcloud QR Code Scanner (`NextcloudQrParser.kt` & `QrCodeScannerDialog.kt`)**:
   - Integrated offline CameraX + ZXing scanner supporting `nc://`, `server:...;user:...;password:...`, JSON payloads, and raw token strings.
3. **Google Drive OAuth2 Flow with PKCE (`GoogleOAuthManager.kt`)**:
   - Implemented RFC 7636 Authorization Code Flow with PKCE, Chrome Custom Tabs redirect (`dev.ilamparithi.aournalpp:/oauth2redirect`), and token auto-refresh.
4. **Room Differential Sync Database (`SyncDatabase.kt`)**:
   - Created `sync_metadata` table keyed by composite `(serviceId, relativePath)` with SHA-256 checksums and timestamps.
5. **Local & Remote Directory Browser (`FolderBrowserDialog.kt`)**:
   - Unified breadcrumb browser for local filesystem notes and live remote endpoint directories.
6. **Live Transfer Queue Subpage (`TransferQueueSubpage.kt` & `FileTransferQueueManager.kt`)**:
   - StateFlow-based real-time queue subpage tracking active, queued, completed, and failed transfers with per-item progress, transfer speed (KB/s or MB/s), cancellation controls, and speed gauges.
7. **Quick Sync & Background Automation (`QuickSyncButton.kt`, `BackupWorker.kt`, `BackupScheduler.kt`)**:
   - Quick sync buttons with spinning indicator badges in Home and Document Hub headers.
   - Configurable sync frequency, launch check for cloud changes, and automated on-exit sync.
8. **Material 3 Cloud Hub (`CloudScreen.kt`, `ServiceConfigDialog.kt`, `CustomMappingDialog.kt`, `ExclusionFilterDialog.kt`)**:
   - Speed-dial FAB with "Add Cloud Service" and "Add Custom Folder Mapping".
   - Account uniqueness verification and long-press confirmation for filter resets.

## Consequences
- Seamless backup and restore across all major personal cloud protocols (Nextcloud, Google Drive, SFTP, SMB3, FTP/FTPS).
- Zero-byte differential sync for unchanged notes.
- Quick sync button accessible from any major document view.
- Real-time full-page transfer queue and speed monitoring.
