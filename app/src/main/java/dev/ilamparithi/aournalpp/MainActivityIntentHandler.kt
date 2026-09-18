package dev.ilamparithi.aournalpp

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.backup.security.GoogleOAuthManager
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.runtime.ActiveNotesTracker
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.ui.cloud.CloudSubpage
import dev.ilamparithi.aournalpp.ui.shell.AppTab
import dev.ilamparithi.aournalpp.utils.ExternalFileHandler
import dev.ilamparithi.aournalpp.utils.FileTypeDetector
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles inbound Intent routing for MainActivity, including Google OAuth redirects,
 * push notification deep links, and external note/PDF file opening.
 */
class MainActivityIntentHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val emergencyManager: MainActivityEmergencyManager,
    private val onNavigateTab: (tabId: Int, subpage: CloudSubpage?) -> Unit,
    private val onShowPrompt: (file: File, isImport: Boolean) -> Unit,
    private val onShowActiveNotePrompt: (targetFile: File, match: ActiveNotesTracker.ActiveNoteMatch) -> Unit
) {
    companion object {
        private const val TAG = "MainActivityIntentHandler"
    }

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action

        // 1. Notification / Tab Navigation (e.g. Action Open Transfer Queue)
        if (action == "dev.ilamparithi.aournalpp.ACTION_OPEN_TRANSFER_QUEUE" ||
            intent.hasExtra("EXTRA_OPEN_TAB") ||
            intent.hasExtra("EXTRA_CLOUD_SUBPAGE")
        ) {
            val tabId = intent.getIntExtra("EXTRA_OPEN_TAB", AppTab.CLOUD.id)
            val subpageStr = intent.getStringExtra("EXTRA_CLOUD_SUBPAGE")
            val subpage = if (subpageStr == "TRANSFER_QUEUE" || action == "dev.ilamparithi.aournalpp.ACTION_OPEN_TRANSFER_QUEUE") {
                CloudSubpage.TRANSFER_QUEUE
            } else {
                null
            }
            onNavigateTab(tabId, subpage)
            return
        }

        val uri = intent.data ?: return

        // 2. Google OAuth2 Redirect Handler
        val isOAuthRedirect = (uri.scheme == "dev.ilamparithi.aournalpp" ||
                uri.scheme?.startsWith("com.googleusercontent.apps.") == true) &&
                (uri.host == "oauth2redirect" || uri.path?.contains("oauth2redirect") == true)
        if (isOAuthRedirect) {
            scope.launch {
                try {
                    val result = GoogleOAuthManager.handleRedirectUri(uri)
                    if (result.isSuccess) {
                        val tokenResponse = result.getOrThrow()
                        val vault = CredentialsVault.getInstance(context)
                        val existingGdrive = vault.getAllServices().firstOrNull { it.providerType == StorageProviderType.GOOGLE_DRIVE }
                        if (existingGdrive != null) {
                            val updated = existingGdrive.copy(
                                authToken = tokenResponse.accessToken,
                                refreshToken = tokenResponse.refreshToken ?: existingGdrive.refreshToken,
                                accountIdentifier = tokenResponse.userEmail ?: existingGdrive.accountIdentifier,
                                tokenExpiryEpochMs = System.currentTimeMillis() + (tokenResponse.expiresInSeconds * 1000L),
                                lastSyncStatus = "Connected"
                            )
                            vault.saveService(updated)
                        }
                        Log.i(TAG, "Successfully authenticated Google Drive for ${tokenResponse.userEmail}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Google Drive connected: ${tokenResponse.userEmail ?: "Authorized"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Log.e(TAG, "Google OAuth token exchange failed: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling Google OAuth redirect", e)
                }
            }
            return
        }

        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_EDIT) return

        if (emergencyManager.quarantinedEmergencySave.value != null) {
            emergencyManager.deferExternalIntentForEmergencySave(intent)
            return
        }

        scope.launch {
            try {
                Log.i(TAG, "Handling external file intent: $uri (action=$action)")
                val env = LinuxEnvironment(context)
                val repo = DocumentRepository.getInstance(context)
                val rootNotesDir = repo.getRootNotesDirectory()

                // 1. Origin check: If file is already inside notes home, open in-place!
                val existingNote = ExternalFileHandler.resolveIfInNotesDirectory(context, uri, rootNotesDir)
                if (existingNote != null) {
                    Log.i(TAG, "URI belongs to existing note in notes directory: ${existingNote.absolutePath}. Opening in-place.")
                    val supervisor = ProcessSupervisor(env)
                    val pdfExportManager = PdfExportManager(env, supervisor)
                    NoteOpenManager.handleFileOpen(
                        context = context,
                        file = existingNote,
                        pdfExportManager = pdfExportManager,
                        scope = scope,
                        repository = repo,
                        onShowPrompt = { onShowPrompt(it, false) },
                        onShowActiveNotePrompt = { targetFile, match ->
                            onShowActiveNotePrompt(targetFile, match)
                        }
                    )
                    return@launch
                }

                // 2. External file: stage to temporary cache
                val result = ExternalFileHandler.stageExternalUri(context, uri, env)
                if (result.isSuccess) {
                    val stagedFile = result.getOrThrow()

                    // 3. Inspect file type via magic bytes
                    val detection = FileTypeDetector.detect(stagedFile)
                    if (!detection.isSupportedNote) {
                        Log.i(TAG, "Unsupported file type '${detection.detectedMimeType}' for $uri. Redirecting to external app.")
                        stagedFile.delete()

                        Toast.makeText(
                            context,
                            context.getString(R.string.unsupported_file_redirecting, detection.detectedMimeType),
                            Toast.LENGTH_SHORT
                        ).show()

                        val redirected = FileTypeDetector.redirectIntent(
                            context = context,
                            uri = uri,
                            detectedMimeType = detection.detectedMimeType
                        )
                        if (!redirected) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.unsupported_file_no_app, detection.detectedMimeType),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }

                    // 4. Supported note format: proceed with normal open flow
                    val supervisor = ProcessSupervisor(env)
                    val pdfExportManager = PdfExportManager(env, supervisor)

                    NoteOpenManager.handleFileOpen(
                        context = context,
                        file = stagedFile,
                        pdfExportManager = pdfExportManager,
                        scope = scope,
                        repository = repo,
                        onShowPrompt = { onShowPrompt(it, true) },
                        onShowActiveNotePrompt = { targetFile, match ->
                            onShowActiveNotePrompt(targetFile, match)
                        }
                    )
                } else {
                    Log.e(TAG, "Failed to stage external file URI: $uri", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception handling external intent", e)
            }
        }
    }
}
