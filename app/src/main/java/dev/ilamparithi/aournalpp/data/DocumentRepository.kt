package dev.ilamparithi.aournalpp.data

import android.content.Context
import dev.ilamparithi.aournalpp.model.AutosaveInfo
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentRepository(private val context: Context) {

    private val env = LinuxEnvironment(context)

    fun getLinuxEnvironment(): LinuxEnvironment = env

    fun scanDocuments(showHidden: Boolean = false): List<NoteDocument> {
        env.ensureDirectoryTree()

        val primaryDir = env.getNotesDirectory()
        val homeNotesDir = File(env.homeDir, "Notes")
        val homeDir = env.homeDir

        val scanDirs = listOfNotNull(
            primaryDir.takeIf { it.exists() },
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
                file.extension.equals("xopp", ignoreCase = true) &&
                !file.name.startsWith(".") &&
                !file.name.endsWith("~") &&
                !file.name.contains(".autosave.", ignoreCase = true)
            }

            for (file in mainFiles) {
                val canonical = try { file.canonicalPath } catch (e: Exception) { file.absolutePath }
                if (!seenMainPaths.add(canonical)) continue

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
