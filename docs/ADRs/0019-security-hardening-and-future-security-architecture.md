# ADR 0019: Security Hardening, Cryptographic Credential Vault Resilience, Sandbox Confinement, and Future Security Architecture

## Status
Accepted

## Context
During the comprehensive security audit of Aournal++ (Security Audit: Part 3), eight significant security vulnerabilities and architectural concerns were identified across component security, credential storage, file I/O, archive extraction, network security, and XML processing:

1. **Unintended Component Exposure (SEC-01)**: `CanvasActivity` and `SettingsActivity` were declared with `android:exported="true"` but defined no intent filters or permissions, allowing any third-party app to arbitrarily invoke the native X11 canvas engine or manipulate preferences.
2. **Insecure Plaintext Storage Downgrade (SEC-02)**: In `CredentialsVault`, hardware KeyStore failures silently fell back to storing high-value cloud credentials (WebDAV, Nextcloud, SMB, OAuth tokens) in unencrypted plaintext `SharedPreferences` on disk (`cloud_credentials_vault_fallback.xml`).
3. **Path Traversal / Arbitrary File Overwrite in Cloud Sync (SEC-03)**: In `BackupEngine`, remote file paths returned by untrusted or compromised cloud storage endpoints were joined directly to local directories without canonical bounds validation, creating critical Zip Slip / directory traversal vulnerabilities.
4. **Tar Slip & Unrestricted Symlink Extraction in Bootstrap (SEC-04)**: In `BootstrapInstaller`, archive entries were unpacked without verifying that entry destinations stayed within `rootDir`. Additionally, archive symlinks were created without verifying that link targets remained within the app sandbox.
5. **Overly Permissive FileProvider Configuration (SEC-05)**: In `file_paths.xml`, `<files-path path="." />`, `<cache-path path="." />`, and `<external-path path="." />` exposed root directory hierarchies, risking internal database and keystore leakage through `content://` URI grants.
6. **Unconfigured Backup & Data Extraction Rules (SEC-06)**: Android Auto Backup and device-to-device transfers were unconstrained, risking plaintext credential leakage or post-restore crashes (`AEADBadTagException`) caused by non-migratable KeyStore keys.
7. **Cleartext HTTP & Missing Network Security Config (SEC-07)**: Absence of `network_security_config.xml` left cleartext HTTP unmanaged. Users on local networks faced crashes or risks of unencrypted credential transmission over public Wi-Fi.
8. **XML Parser External DTD & Entity Processing (SEC-08)**: XML parsing engines lacked explicit feature disabling for DTD and entity expansion, creating exposure to Billion Laughs DoS attacks and XXE.

This ADR records the immediate architectural remediations implemented to address these findings and establishes architectural guidelines for future security engineering in Aournal++.

---

## Decisions: Current Architectural Actions

### 1. Component Sandboxing & External Intent Routing (SEC-01)
- **Private Activities**: `CanvasActivity` and `SettingsActivity` are strictly non-exported (`android:exported="false"`).
- **Single External Entry Point**: `MainActivity` serves as the sole external gatekeeper for `ACTION_VIEW` and `ACTION_EDIT` intents. Incoming URIs are validated, resolved to safe local documents, and routed to `CanvasActivity` internally.

### 2. Cryptographic Credential Vault Resilience & Fail-Closed Semantics (SEC-02)
- **Zero Plaintext on Disk**: Silent fallback to unencrypted disk storage is eliminated.
- **Self-Healing KeyStore Recovery**: When `EncryptedSharedPreferences.create` fails (e.g. following OS upgrades or KeyStore desynchronization), `CredentialsVault` automatically deletes the corrupted preferences file and purges the stale alias from `AndroidKeyStore`, attempting clean re-initialization.
- **Fail-Closed Transient Storage**: If KeyStore recovery fails catastrophically, `CredentialsVault` falls back to `InMemorySharedPreferences`. Secrets are retained solely in volatile RAM for the process lifecycle and are never persisted unencrypted to flash storage.

### 3. Canonical Path Traversal Defense (SEC-03)
- **Strict Canonical Bounds Verification**: All cloud sync restore and preview discovery operations must resolve local destinations via a centralized validator:
  ```kotlin
  fun resolveSafeChild(baseDir: File, subPath: String): File {
      val cleanSubPath = subPath.trim().trim('/')
      if (cleanSubPath.isEmpty()) throw SecurityException("Empty subpath provided")
      val canonicalBase = baseDir.canonicalFile
      val canonicalTarget = File(baseDir, cleanSubPath).canonicalFile
      if (!canonicalTarget.toPath().startsWith(canonicalBase.toPath())) {
          throw SecurityException("Path traversal detected: '$subPath' escapes '${baseDir.path}'")
      }
      return canonicalTarget
  }
  ```
- Any remote path attempting directory escape (`../`, absolute roots) is rejected immediately, recorded in sync error logs, and omitted from the download queue.

### 4. Archive Extraction Bounds & Symlink Confinement (SEC-04)
- **Tar Slip Guard**: Every archive entry is verified before directory creation or extraction:
  ```kotlin
  if (!isPathWithinRoot(env.rootDir, destFile)) {
      throw SecurityException("Tar slip detected for entry '${entry.name}'")
  }
  ```
- **Symlink Confinement**: Archive symlinks are normalized and checked to guarantee that link targets (whether absolute or relative) stay confined within `env.rootDir`. Symlinks pointing to `/system` or arbitrary locations outside the sandbox root are rejected.

### 5. Least-Privilege FileProvider Configuration (SEC-05)
- **Elimination of Wildcards**: All root `path="."` mappings in `file_paths.xml` are deleted.
- **Explicit Functional Whitelist**: Only specific functional subdirectories required for note sharing, PDF export, camera capture, and clipboard transfers are exposed:
  - `<files-path name="internal_notes" path="home/Notes/" />`
  - `<cache-path name="shared_pdfs" path="shared_pdfs/" />`
  - `<cache-path name="shared_notes" path="shared_notes/" />`
  - `<cache-path name="clipboard" path="clipboard/" />`
  - `<external-path name="external_documents" path="Documents/Notes/" />`

### 6. Cloud Backup & Device Transfer Exclusions (SEC-06)
- **Exclusion of Non-Transferable Keystore Data**: In `data_extraction_rules.xml` (Android 12+) and `backup_rules.xml` (Android 11 and below), `secure_cloud_credentials.xml` is excluded from cloud backup and device transfers.
- **Exclusion of Native Rootfs and Databases**: Private app directories (`root`, `file`, `database`) housing Termux bootstrap files and SQLite caches are excluded from backup to prevent restore corruption.

### 7. Explicit Network Security Configuration (SEC-07)
- **Cleartext Disabled by Default**: `res/xml/network_security_config.xml` enforces `<base-config cleartextTrafficPermitted="false">`.
- **Restricted Loopback Cleartext**: Cleartext HTTP is permitted solely for local development and test environments (`localhost`, `127.0.0.1`, `10.0.2.2`).
- **Support for Private & User CAs**: `<certificates src="user" />` is trusted alongside `<certificates src="system" />` to enable self-hosted Nextcloud and WebDAV instances utilizing private organizational CAs.
- **Transit Security Warning**: `WebDavStorageProvider` logs explicit security warnings if unencrypted HTTP is configured for a remote host.

### 8. Hardened XML Parsing (SEC-08)
- External DTD and entity expansion processing is disabled across all XML parsers:
  ```kotlin
  try {
      parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl", false)
  } catch (_: Exception) {}
  ```
- Applied uniformly across `XoppParser` (document preview and page rendering), `WebDavStorageProvider` (PROPFIND response parsing), and `XournalConfigManager` (configuration validation).

---

## Future Security Considerations & Architectural Guidelines

To preserve security as Aournal++ scales, developers and contributors must adhere to the following guidelines:

### 1. Remote Bootstrap & Dynamic Package Verification
- **Cryptographic Manifest Verification**: When remote mirror downloads or dynamic package updates are introduced, bootstrap archives must be accompanied by an Ed25519 or SHA-256 digital signature verified against a hardcoded public key prior to unpacking.
- **Streaming Integrity Verification**: Verify archive hashes during decompression rather than relying on unverified post-extraction checks.

### 2. Inter-Process Communication (IPC) Hardening
- **Signature Permissions on Services**: Background bridge services (`MainProcessBridgeService`, `CanvasCommandReceiver`) operating across the main process and `:canvas` process must enforce signature-level permissions (`android:protectionLevel="signature"`) or verify caller UID via `Binder.getCallingUid() == Process.myUid()`.
- **Command Parameter Validation**: IPC intents passing file paths or command payloads must validate arguments against internal directory bounds before execution.

### 3. Biometric & Master Authentication Vault Gating
- **Biometric-Guarded Master Key**: Provide user-configurable option to gate `CredentialsVault` behind `BiometricPrompt` with hardware-backed Keystore auth-gated keys (`setUserAuthenticationRequired(true)`).
- **Sensitive Memory Scrubbing**: Sensitive strings (passwords, private key passphrases) should be represented as `CharArray` or `ByteArray` where feasible and zeroed out after network request dispatch.

### 4. Scoped Storage Transition & Storage Access Framework (SAF)
- **Migration Away from Legacy Storage**: The application currently utilizes `requestLegacyExternalStorage="true"` and `MANAGE_EXTERNAL_STORAGE` for direct Linux POSIX filesystem compatibility.
- **Future Roadmap**: Implement a virtual FUSE or SAF document provider bridge to allow X11 native processes to access user-selected directories without requiring broad device storage permissions.

### 5. Native Code Hardening (X11 & Xournal++ Runtime)
- **Binary Hardening Flags**: Native binaries bundled in the bootstrap (X11 server, Openbox, Xournal++) must be built with standard security compiler flags:
  - Position Independent Executable (`-fPIE -pie`)
  - Stack Smashing Protection (`-fstack-protector-strong`)
  - Fortify Source (`-D_FORTIFY_SOURCE=2`)
  - Immediate Binding (`-Wl,-z,relro -Wl,-z,now`)
- **Strict Socket Permissions**: UNIX domain sockets used for X11 communication (`/tmp/.X11-unix/X0`) must remain strictly confined to the app's private internal storage (`filesDir`) with mode `0700`.

---

## Verification & Compliance
- All PRs touching file extraction, cloud sync, or IPC must include negative test cases asserting that path traversal and unauthorized invocations are rejected.
- Regular security regression suites (`SecurityRemediationTest`) must be maintained in CI covering `resolveSafeChild`, archive extraction guards, and XML parser configuration.
