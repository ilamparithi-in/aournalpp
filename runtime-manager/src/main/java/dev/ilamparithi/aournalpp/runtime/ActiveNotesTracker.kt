package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import java.io.File

/**
 * Multi-process safe tracker for open note documents across the active canvas session.
 * Inspects [ActiveSessionTracker] and [ActiveWorkspaceTracker] to determine whether
 * a specific note file is currently open in any window of the active session.
 */
object ActiveNotesTracker {

    data class ActiveNoteMatch(
        val windowId: String?,
        val title: String,
        val filePath: String?
    )

    private val GENERIC_TITLES = setOf(
        "New Note",
        "Unsaved Document",
        "Untitled",
        "Untitled Note",
        "Xournal++"
    )

    fun isGenericTitle(title: String): Boolean {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return true
        return GENERIC_TITLES.any { it.equals(trimmed, ignoreCase = true) }
    }

    fun cleanDisplayTitle(raw: String): String {
        return ProcessSupervisor.cleanNoteTitle(raw)
            .removeSuffix(".xopp")
            .removeSuffix(".pdf")
            .removeSuffix(".xoj")
            .trim()
    }

    /**
     * Checks whether the given note file is already open in the currently active canvas session.
     * Returns an [ActiveNoteMatch] containing the window ID (if known) and title if open, or null.
     */
    fun findActiveNote(context: Context, file: File): ActiveNoteMatch? {
        val env = LinuxEnvironment(context)
        return findActiveNote(env.tmpDir, file)
    }

    /**
     * Checks whether the given note file is already open in the currently active canvas session
     * using the specified base directory (typically `env.tmpDir`).
     */
    fun findActiveNote(baseDir: File, file: File): ActiveNoteMatch? {
        if (!ActiveSessionTracker.isSessionActive(baseDir)) {
            return null
        }

        val targetCanonical = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
        val targetAbsolute = file.absolutePath
        val targetName = file.name
        val targetNameNoExt = file.nameWithoutExtension

        // 1. Check workspace state for detailed multi-window records
        val workspaceState = ActiveWorkspaceTracker.getWorkspaceState(baseDir)
        if (workspaceState != null && workspaceState.windows.isNotEmpty()) {
            for (win in workspaceState.windows) {
                // Match by resolved filePath if available
                if (!win.filePath.isNullOrBlank()) {
                    val winCanonical = try { File(win.filePath).canonicalPath } catch (_: Exception) { win.filePath }
                    if (winCanonical.equals(targetCanonical, ignoreCase = true) ||
                        win.filePath.equals(targetAbsolute, ignoreCase = true)) {
                        return ActiveNoteMatch(
                            windowId = win.id,
                            title = win.cleanTitle.ifBlank { win.title },
                            filePath = win.filePath
                        )
                    }
                }

                // Match by clean title against file name (with or without extension)
                val clean = cleanDisplayTitle(win.cleanTitle)
                if (clean.isNotBlank() && !isGenericTitle(clean)) {
                    if (clean.equals(targetNameNoExt, ignoreCase = true) ||
                        clean.equals(targetName, ignoreCase = true)) {
                        return ActiveNoteMatch(
                            windowId = win.id,
                            title = clean,
                            filePath = win.filePath ?: targetAbsolute
                        )
                    }
                }

                // Match sanitized raw title
                val sanitized = ProcessSupervisor.sanitizeWindowTitle(win.title).trim()
                if (sanitized.isNotBlank() && !isGenericTitle(sanitized)) {
                    val cleanSanitized = cleanDisplayTitle(sanitized)
                    if (cleanSanitized.equals(targetNameNoExt, ignoreCase = true) ||
                        cleanSanitized.equals(targetName, ignoreCase = true) ||
                        sanitized.equals(targetName, ignoreCase = true)) {
                        return ActiveNoteMatch(
                            windowId = win.id,
                            title = cleanSanitized.ifBlank { sanitized },
                            filePath = win.filePath ?: targetAbsolute
                        )
                    }
                }
            }
        }

        // 2. Fallback check active session info (e.g. single-window session before workspace state synced)
        val sessionInfo = ActiveSessionTracker.getActiveSession(baseDir)
        if (sessionInfo != null && sessionInfo.isRunning) {
            if (!sessionInfo.activeNotePath.isNullOrBlank()) {
                val sessionCanonical = try { File(sessionInfo.activeNotePath).canonicalPath } catch (_: Exception) { sessionInfo.activeNotePath }
                if (sessionCanonical.equals(targetCanonical, ignoreCase = true) ||
                    sessionInfo.activeNotePath.equals(targetAbsolute, ignoreCase = true)) {
                    return ActiveNoteMatch(
                        windowId = null,
                        title = sessionInfo.documentTitle ?: targetNameNoExt,
                        filePath = sessionInfo.activeNotePath
                    )
                }
            }

            if (!sessionInfo.documentTitle.isNullOrBlank()) {
                val docTitle = sessionInfo.documentTitle.trim()
                val cleanDocTitle = cleanDisplayTitle(docTitle)
                if (cleanDocTitle.isNotBlank() && !isGenericTitle(cleanDocTitle)) {
                    if (cleanDocTitle.equals(targetNameNoExt, ignoreCase = true) ||
                        cleanDocTitle.equals(targetName, ignoreCase = true) ||
                        docTitle.equals(targetName, ignoreCase = true)) {
                        return ActiveNoteMatch(
                            windowId = null,
                            title = cleanDocTitle,
                            filePath = targetAbsolute
                        )
                    }
                }
            }
        }

        return null
    }

    /**
     * Checks if a note file is open in the active session.
     */
    fun isNoteOpen(context: Context, file: File): Boolean {
        return findActiveNote(context, file) != null
    }

    /**
     * Checks if a note file is open in the active session given the base directory.
     */
    fun isNoteOpen(baseDir: File, file: File): Boolean {
        return findActiveNote(baseDir, file) != null
    }
}
