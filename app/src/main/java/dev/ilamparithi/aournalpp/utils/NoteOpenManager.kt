package dev.ilamparithi.aournalpp.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * Supported actions for opening a note or file in Aournal.
 */
enum class NoteOpenAction(val value: String, val displayName: String) {
    ASK("ask", "Ask every time"),
    EDIT("edit", "Edit in Xournal++"),
    VIEW("view", "View as PDF");

    companion object {
        fun fromValue(value: String?): NoteOpenAction {
            return entries.firstOrNull { it.value == value } ?: ASK
        }
    }
}

/**
 * Centralized manager for handling file/note opening workflows,
 * default open actions, and PDF/Canvas dispatching.
 */
object NoteOpenManager {

    const val PREF_KEY_DEFAULT_OPEN_ACTION = "pref_default_open_action"
    private const val PREFS_NAME = "aournal_prefs"

    fun getDefaultAction(context: Context): NoteOpenAction {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(PREF_KEY_DEFAULT_OPEN_ACTION, NoteOpenAction.ASK.value)
        return NoteOpenAction.fromValue(raw)
    }

    fun setDefaultAction(context: Context, action: NoteOpenAction) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_DEFAULT_OPEN_ACTION, action.value).apply()
    }

    /**
     * Directly launches the native Xournal++ canvas editor for the given note file.
     */
    fun openInCanvas(
        context: Context,
        file: File,
        repository: DocumentRepository? = null,
        localView: View? = null
    ) {
        val targetFile = if (file.absolutePath.contains("/cache/staged_imports/")) {
            val env = repository?.getLinuxEnvironment() ?: LinuxEnvironment(context)
            try {
                val importedDir = env.getImportedDirectory()
                if (!importedDir.exists()) importedDir.mkdirs()
                val dest = File(importedDir, file.name)
                file.copyTo(dest, overwrite = true)
                dest
            } catch (e: Exception) {
                file
            }
        } else {
            file
        }

        try {
            repository?.recordNoteOpened(targetFile.absolutePath) ?: run {
                DocumentRepository(context).recordNoteOpened(targetFile.absolutePath)
            }
        } catch (_: Exception) {}

        val intent = Intent(context, CanvasActivity::class.java).apply {
            putExtra(CanvasActivity.EXTRA_NOTE_PATH, targetFile.absolutePath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (localView != null && localView.width > 0 && localView.height > 0) {
            val options = ActivityOptionsCompat.makeClipRevealAnimation(
                localView,
                localView.width / 2,
                localView.height / 2,
                localView.width / 4,
                localView.height / 4
            ).toBundle()
            context.startActivity(intent, options)
        } else {
            context.startActivity(intent)
        }
    }

    /**
     * Converts the file to PDF (if .xopp/.xoj) and launches the system/external PDF viewer.
     */
    fun openAsPdf(
        context: Context,
        file: File,
        pdfExportManager: PdfExportManager,
        scope: CoroutineScope,
        repository: DocumentRepository? = null,
        onConvertingState: ((Boolean) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        try {
            if (!file.absolutePath.contains("staged_imports") && !file.absolutePath.contains("/cache/")) {
                repository?.recordNoteOpened(file.absolutePath) ?: run {
                    DocumentRepository(context).recordNoteOpened(file.absolutePath)
                }
            }
        } catch (_: Exception) {}

        scope.launch {
            try {
                val isPdf = file.extension.equals("pdf", ignoreCase = true)
                val pdfFile = if (isPdf) {
                    file
                } else {
                    onConvertingState?.invoke(true)
                    val result = pdfExportManager.renderPdfForSharing(context, file)
                    onConvertingState?.invoke(false)
                    result.getOrThrow()
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )

                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooser = Intent.createChooser(viewIntent, "View Note as PDF").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: ActivityNotFoundException) {
                onConvertingState?.invoke(false)
                Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_LONG).show()
                onError?.invoke("No PDF viewer app found on device")
            } catch (e: Exception) {
                onConvertingState?.invoke(false)
                val errorMsg = e.message ?: "Failed to convert or open PDF"
                Toast.makeText(context, "PDF Error: $errorMsg", Toast.LENGTH_SHORT).show()
                onError?.invoke(errorMsg)
            }
        }
    }

    /**
     * Core dispatcher for any file open event. Evaluates current default action setting:
     * - If [NoteOpenAction.EDIT], directly launches CanvasActivity.
     * - If [NoteOpenAction.VIEW], directly launches PDF viewer.
     * - If [NoteOpenAction.ASK], triggers the [onShowPrompt] callback with the target file.
     */
    fun handleFileOpen(
        context: Context,
        file: File,
        pdfExportManager: PdfExportManager,
        scope: CoroutineScope,
        repository: DocumentRepository? = null,
        localView: View? = null,
        onShowPrompt: (File) -> Unit,
        onConvertingState: ((Boolean) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        when (getDefaultAction(context)) {
            NoteOpenAction.EDIT -> openInCanvas(context, file, repository, localView)
            NoteOpenAction.VIEW -> openAsPdf(context, file, pdfExportManager, scope, repository, onConvertingState, onError)
            NoteOpenAction.ASK -> onShowPrompt(file)
        }
    }
}
