package dev.ilamparithi.aournalpp.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dev.ilamparithi.aournalpp.model.AutosaveInfo
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentRepository(private val context: Context) {

    private val env = LinuxEnvironment(context)

    fun getLinuxEnvironment(): LinuxEnvironment = env

    fun scanDocuments(query: String = "", showHidden: Boolean = false): List<NoteDocument> {
        env.ensureDirectoryTree()

        val primaryDir = env.getNotesDirectory()
        val homeNotesDir = File(env.homeDir, "Notes")
        val homeDir = env.homeDir
        val importedDir = File(primaryDir, "Imported")

        val scanDirs = listOfNotNull(
            primaryDir.takeIf { it.exists() },
            importedDir.takeIf { it.exists() && it.canonicalPath != primaryDir.canonicalPath },
            homeNotesDir.takeIf { it.exists() && it.canonicalPath != primaryDir.canonicalPath },
            homeDir.takeIf { it.exists() && it.canonicalPath != primaryDir.canonicalPath }
        )

        val seenMainPaths = mutableSetOf<String>()
        val resultNotes = mutableListOf<NoteDocument>()
        val matchedAutosavePaths = mutableSetOf<String>()

        val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

        for (dir in scanDirs) {
            val allFilesInDir = dir.listFiles() ?: continue

            // 1. Gather normal, non-hidden .xopp document files
            val mainFiles = allFilesInDir.filter { file ->
                file.isFile &&
                (file.extension.equals("xopp", ignoreCase = true) || file.extension.equals("pdf", ignoreCase = true)) &&
                !file.name.startsWith(".") &&
                !file.name.endsWith("~") &&
                !file.name.contains(".autosave.", ignoreCase = true)
            }

            for (file in mainFiles) {
                val canonical = try { file.canonicalPath } catch (e: Exception) { file.absolutePath }
                if (!seenMainPaths.add(canonical)) continue

                // Filter by search query if present
                if (query.isNotBlank() && !file.name.contains(query.trim(), ignoreCase = true)) {
                    continue
                }

                // Find matching autosave candidates in the same directory
                val autosaveCandidate = findMatchingAutosave(dir, file)
                val autosaveInfo = autosaveCandidate?.let { autoFile ->
                    matchedAutosavePaths.add(try { autoFile.canonicalPath } catch (e: Exception) { autoFile.absolutePath })
                    AutosaveInfo(
                        autosaveFile = autoFile,
                        mainFile = file,
                        mainLastModifiedMs = file.lastModified(),
                        autosaveLastModifiedMs = autoFile.lastModified(),
                        mainSizeBytes = file.length(),
                        autosaveSizeBytes = autoFile.length()
                    )
                }

                val sizeKb = (file.length() + 1023) / 1024
                resultNotes.add(
                    NoteDocument(
                        file = file,
                        title = file.nameWithoutExtension,
                        path = file.absolutePath,
                        lastModifiedMs = file.lastModified(),
                        sizeBytes = file.length(),
                        lastModifiedFormatted = dateFormat.format(Date(file.lastModified())),
                        sizeFormatted = "${sizeKb} KB",
                        autosaveInfo = autosaveInfo,
                        isHidden = false,
                        folder = dir.name
                    )
                )
            }

            // 2. If showHidden is true, include unmatched hidden notes / autosaves
            if (showHidden) {
                val hiddenOrBackupFiles = allFilesInDir.filter { file ->
                    file.isFile &&
                    (file.name.startsWith(".") || file.name.endsWith("~") || file.name.contains(".autosave.", ignoreCase = true)) &&
                    (file.name.contains(".xopp", ignoreCase = true) || file.name.endsWith("~"))
                }

                for (file in hiddenOrBackupFiles) {
                    val canonical = try { file.canonicalPath } catch (e: Exception) { file.absolutePath }
                    if (matchedAutosavePaths.contains(canonical) || seenMainPaths.contains(canonical)) continue

                    if (query.isNotBlank() && !file.name.contains(query.trim(), ignoreCase = true)) {
                        continue
                    }

                    val sizeKb = (file.length() + 1023) / 1024
                    resultNotes.add(
                        NoteDocument(
                            file = file,
                            title = file.name,
                            path = file.absolutePath,
                            lastModifiedMs = file.lastModified(),
                            sizeBytes = file.length(),
                            lastModifiedFormatted = dateFormat.format(Date(file.lastModified())),
                            sizeFormatted = "${sizeKb} KB",
                            autosaveInfo = null,
                            isHidden = true,
                            folder = dir.name
                        )
                    )
                }
            }
        }

        return resultNotes.sortedByDescending { it.lastModifiedMs }
    }

    private fun findMatchingAutosave(parentDir: File, mainFile: File): File? {
        val candidates = listOf(
            File(parentDir, ".${mainFile.name}.autosave.xopp"),
            File(parentDir, ".${mainFile.nameWithoutExtension}.autosave.xopp"),
            File(parentDir, ".${mainFile.name}~"),
            File(parentDir, "${mainFile.name}~"),
            File(parentDir, ".${mainFile.nameWithoutExtension}.xopp~")
        )

        return candidates.firstOrNull { it.exists() && it.isFile && it.length() > 0 }
    }

    suspend fun renameNote(doc: NoteDocument, newTitle: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = newTitle.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
            if (cleanTitle.isBlank()) error("Document name cannot be blank")

            val ext = doc.file.extension
            val targetName = if (cleanTitle.endsWith(".$ext", ignoreCase = true)) {
                cleanTitle
            } else {
                "$cleanTitle.$ext"
            }

            val parentDir = doc.file.parentFile ?: error("Parent directory not found")
            val targetFile = File(parentDir, targetName)

            if (targetFile.exists() && targetFile.canonicalPath != doc.file.canonicalPath) {
                error("A file named '$targetName' already exists")
            }

            // Rename primary file
            if (!doc.file.renameTo(targetFile)) {
                error("Failed to rename file to '$targetName'")
            }

            // Rename associated autosave if present
            doc.autosaveInfo?.autosaveFile?.let { autoFile ->
                if (autoFile.exists()) {
                    val autoExt = autoFile.extension
                    val targetAuto = File(parentDir, ".${targetFile.nameWithoutExtension}.autosave.$autoExt")
                    autoFile.renameTo(targetAuto)
                }
            }

            targetFile
        }
    }

    suspend fun duplicateNote(doc: NoteDocument): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val parentDir = doc.file.parentFile ?: error("Parent directory not found")
            val baseName = doc.file.nameWithoutExtension
            val ext = doc.file.extension

            var counter = 1
            var candidate = File(parentDir, "$baseName (Copy).$ext")
            while (candidate.exists()) {
                counter++
                candidate = File(parentDir, "$baseName (Copy $counter).$ext")
            }

            doc.file.copyTo(candidate, overwrite = false)
            candidate
        }
    }

    suspend fun deleteNote(doc: NoteDocument): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Delete main file
            if (doc.file.exists()) {
                doc.file.delete()
            }

            // Delete associated autosave
            doc.autosaveInfo?.autosaveFile?.let { autoFile ->
                if (autoFile.exists()) {
                    autoFile.delete()
                }
            }
            Unit
        }
    }

    fun shareNoteAsXopp(context: Context, doc: NoteDocument) {
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            doc.file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (doc.file.extension.equals("pdf", ignoreCase = true)) "application/pdf" else "application/x-xopp"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, doc.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Note"))
    }

    suspend fun shareNoteAsPdf(
        context: Context,
        doc: NoteDocument,
        pdfExportManager: PdfExportManager
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val pdfFile = if (doc.file.extension.equals("pdf", ignoreCase = true)) {
                doc.file
            } else {
                pdfExportManager.renderPdfForSharing(context, doc.file).getOrThrow()
            }

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "${doc.title}.pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(intent, "Share PDF"))
            }
            Unit
        }
    }

    fun replaceWithAutosave(note: NoteDocument): File {
        val autoInfo = note.autosaveInfo ?: return note.file
        try {
            autoInfo.autosaveFile.copyTo(note.file, overwrite = true)
            autoInfo.autosaveFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return note.file
    }

    fun keepBoth(note: NoteDocument): File {
        val autoInfo = note.autosaveInfo ?: return note.file
        try {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = sdf.format(Date(autoInfo.autosaveLastModifiedMs))
            val backupFile = File(note.file.parentFile, "${note.file.nameWithoutExtension}_autosave_$timestamp.xopp")
            autoInfo.autosaveFile.copyTo(backupFile, overwrite = true)
            autoInfo.autosaveFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return note.file
    }

    fun discardAutosave(note: NoteDocument): File {
        val autoInfo = note.autosaveInfo ?: return note.file
        try {
            if (autoInfo.autosaveFile.exists()) {
                autoInfo.autosaveFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return note.file
    }

    fun openEmergencyRecoverySession(recoveryFile: File): File {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val defaultName = "Recovered_Session_${sdf.format(Date(recoveryFile.lastModified()))}.xopp"
        return saveEmergencyRecoveryToNotes(recoveryFile, defaultName)
    }

    fun saveEmergencyRecoveryToNotes(recoveryFile: File, userSpecifiedName: String): File {
        val notesDir = env.getNotesDirectory()
        if (!notesDir.exists()) notesDir.mkdirs()

        val cleanName = if (userSpecifiedName.endsWith(".xopp", ignoreCase = true)) {
            userSpecifiedName
        } else {
            "$userSpecifiedName.xopp"
        }

        val target = File(notesDir, cleanName)
        recoveryFile.copyTo(target, overwrite = true)
        recoveryFile.delete()
        env.clearQuarantinedEmergencySave()
        return target
    }

    fun discardEmergencyRecovery() {
        env.clearQuarantinedEmergencySave()
    }
}
