package dev.ilamparithi.aournalpp.runtime

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CopyOnWriteArrayList

class ProcessSupervisor(private val env: LinuxEnvironment) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeProcesses = CopyOnWriteArrayList<Process>()

    fun startKioskWindowManager(): Process? {
        val openboxFile = env.resolveExecutable("openbox")
        val matchboxFile = env.resolveExecutable("matchbox-window-manager")
        val wmFile = when {
            openboxFile.exists() && openboxFile.canExecute() -> openboxFile
            matchboxFile.exists() && matchboxFile.canExecute() -> matchboxFile
            else -> null
        }
        if (wmFile == null) {
            Log.w("ProcessSupervisor", "No window manager found in ${env.nativeLibDir.absolutePath} or ${env.binDir.absolutePath}, skipping WM startup")
            return null
        }
        val command = if (wmFile.name.contains("openbox")) {
            listOf(wmFile.absolutePath, "--sm-disable")
        } else {
            listOf(wmFile.absolutePath, "-use_titlebar", "no", "-use_cursor", "no")
        }
        val tag = if (wmFile.name.contains("matchbox")) "NativeProcess:MatchboxWM" else "NativeProcess:WindowManager"
        Log.i("ProcessSupervisor", "Starting WM with command: $command (tag=$tag)")
        val process = ProcessBuilder(command)
            .directory(env.homeDir)
            .redirectErrorStream(true)
            .apply { environment().putAll(env.getEnvMap()) }
            .start()
            
        activeProcesses.add(process)
        monitorProcessOutput(process, tag)
        return process
    }

    private val _documentTitle = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val documentTitle: kotlinx.coroutines.flow.StateFlow<String?> = _documentTitle

    private var xournalProcess: Process? = null
    private var onXournalExitListener: (() -> Unit)? = null

    fun setOnXournalExitListener(listener: (() -> Unit)?) {
        onXournalExitListener = listener
    }

    fun isXournalRunning(): Boolean {
        return xournalProcess?.isAlive == true
    }

    fun startXournal(targetFilePath: String? = null): Process? {
        val xournalFile = env.resolveExecutable("xournalpp")
        if (!xournalFile.exists() || !xournalFile.canExecute()) {
            Log.e("ProcessSupervisor", "xournalpp not found at ${xournalFile.absolutePath}")
            return null
        }
        val command = mutableListOf(xournalFile.absolutePath)
        if (targetFilePath != null) {
            command.add(targetFilePath)
        }
        
        val notesDir = env.getNotesDirectory()
        val xoppWorkingDir = if (notesDir.exists()) notesDir else env.homeDir
        val process = ProcessBuilder(command)
            .directory(xoppWorkingDir)
            .redirectErrorStream(true)
            .apply { environment().putAll(env.getEnvMap()) }
            .start()
            
        xournalProcess = process
        activeProcesses.add(process)
        monitorProcessOutput(process, "NativeProcess:Xournalpp")

        scope.launch {
            try {
                process.waitFor()
                Log.i("ProcessSupervisor", "Xournal++ process terminated cleanly.")
                onXournalExitListener?.invoke()
            } catch (e: Exception) {
                // Ignore
            }
        }

        return process
    }

    fun startTitleWatcher() {
        val watcherFile = env.resolveExecutable("xopp-title-watcher")
        if (!watcherFile.exists() || !watcherFile.canExecute()) {
            Log.w("ProcessSupervisor", "xopp-title-watcher not found or not executable at ${watcherFile.absolutePath}")
            return
        }

        scope.launch {
            while (isActive) {
                var process: Process? = null
                try {
                    val command = listOf(watcherFile.absolutePath)
                    Log.i("ProcessSupervisor", "Starting X11 title watcher: $command")
                    val proc = ProcessBuilder(command)
                        .directory(env.homeDir)
                        .redirectErrorStream(true)
                        .apply { environment().putAll(env.getEnvMap()) }
                        .start()
                    process = proc
                    activeProcesses.add(proc)

                    BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null && isActive) {
                            Log.d("NativeProcess:TitleWatcher", line)
                            if (line.startsWith("TITLE:")) {
                                val raw = line.removePrefix("TITLE:").trim()
                                val clean = sanitizeWindowTitle(raw)
                                if (clean.isNotBlank() && clean != "Xournal++") {
                                    _documentTitle.value = clean
                                }
                            }
                            line = reader.readLine()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NativeProcess:TitleWatcher", "Error in title watcher loop", e)
                } finally {
                    process?.let { activeProcesses.remove(it) }
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun setX11Wallpaper(ppmPath: String): Boolean {
        val wallpaperBin = env.resolveExecutable("xopp-wallpaper")
        if (!wallpaperBin.exists() || !wallpaperBin.canExecute()) {
            Log.w("ProcessSupervisor", "xopp-wallpaper not found or not executable at ${wallpaperBin.absolutePath}")
            return false
        }
        return try {
            val command = listOf(wallpaperBin.absolutePath, ppmPath)
            Log.i("ProcessSupervisor", "Setting X11 root wallpaper via $command")
            val process = ProcessBuilder(command)
                .directory(env.homeDir)
                .redirectErrorStream(true)
                .apply { environment().putAll(env.getEnvMap()) }
                .start()
            val code = process.waitFor()
            Log.i("ProcessSupervisor", "xopp-wallpaper finished with exit code $code")
            code == 0
        } catch (e: Exception) {
            Log.w("ProcessSupervisor", "Failed to set X11 wallpaper", e)
            false
        }
    }

    fun resetDocumentTitle(title: String? = null) {
        _documentTitle.value = title
    }

    fun triggerXournalExit() {
        onXournalExitListener?.invoke()
    }

    companion object {
        val ignoredWindowTitles = setOf(
            "Openbox",
            "com.github.xournalpp.xournalpp",
            "Xournal++",
            "X11",
            "Desktop",
            "Save File",
            "Save As",
            "Save Document",
            "Save",
            "Open File",
            "Open Document",
            "Open",
            "Export as PDF",
            "Export PDF",
            "Export As...",
            "Export As",
            "Export",
            "Print",
            "Page Setup",
            "Preferences",
            "Xournal++ Preferences",
            "About Xournal++",
            "About",
            "Plugin Manager",
            "Manage Plugins",
            "Page Background",
            "Set Page Background",
            "Paper Format",
            "Select Font",
            "Font Selection",
            "Choose Font",
            "Font",
            "Select Color",
            "Color Selection",
            "Choose Color",
            "Color",
            "Custom Color",
            "Select Folder",
            "Choose Folder",
            "Select Destination Folder",
            "Question",
            "Warning",
            "Error",
            "Information",
            "Confirm",
            "LaTeX",
            "Insert Text",
            "Edit Text"
        )

        fun isIgnoredTitle(title: String): Boolean {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) return true

            // Exact match ignoring case
            if (ignoredWindowTitles.any { it.equals(trimmed, ignoreCase = true) }) {
                return true
            }

            val lower = trimmed.lowercase()

            // Substring checks for dialogs (unless it's an actual note with that name ending in .xopp)
            if (lower.contains("preferences") && !lower.contains(".xopp")) return true
            if (lower.contains("about xournal++")) return true
            if (lower.contains("plugin manager")) return true
            if (lower.contains("font selection")) return true
            if (lower.contains("color selection")) return true
            if (lower.contains("page background")) return true
            if (lower.contains("save changes")) return true
            if (lower.contains("error saving")) return true
            if (lower.contains("error loading")) return true

            // Prefix checks for common dialog types
            if (lower.startsWith("select font") ||
                lower.startsWith("select color") ||
                lower.startsWith("export as") ||
                lower.startsWith("save file") ||
                lower.startsWith("open file") ||
                lower.startsWith("choose folder") ||
                lower.startsWith("select folder")
            ) {
                return true
            }

            // Dialog titles starting with "Xournal++ " (e.g. "Xournal++ Preferences", "Xournal++ Warning")
            // Note: Document windows in Xournal++ format title as "<filename> - Xournal++" (ends with "- Xournal++")
            if (lower.startsWith("xournal++ ") && !lower.contains("- xournal++")) {
                return true
            }

            return false
        }

        fun sanitizeWindowTitle(raw: String): String {
            if (raw.isBlank() || isIgnoredTitle(raw)) {
                return ""
            }

            val withoutAppSuffix = raw.replace(Regex("\\s*-\\s*Xournal\\+\\+.*$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\[autosaved\\]", RegexOption.IGNORE_CASE), "")
                .trim()

            val isDirty = raw.trim().startsWith("*") ||
                    raw.trim().endsWith("*") ||
                    withoutAppSuffix.startsWith("*") ||
                    withoutAppSuffix.endsWith("*")

            val cleaned = withoutAppSuffix
                .removePrefix("*")
                .removeSuffix("*")
                .trim()

            if (cleaned.isBlank() || isIgnoredTitle(cleaned)) {
                return ""
            }

            val baseName = if (cleaned.equals("Unsaved Document", ignoreCase = true) ||
                cleaned.equals("Untitled", ignoreCase = true) ||
                cleaned.equals("Untitled Document", ignoreCase = true)
            ) {
                "New Note"
            } else {
                cleaned
            }

            return if (isDirty) "*$baseName" else baseName
        }
    }

    private fun monitorProcessOutput(process: Process, tag: String) {
        scope.launch {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        Log.d(tag, line)
                        line = reader.readLine()
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error reading process output", e)
            } finally {
                activeProcesses.remove(process)
            }
        }
    }

    fun launchBinary(name: String, command: List<String>): Process? {
        val rawPath = command.first()
        val binFile = if (File(rawPath).isAbsolute) {
            val f = File(rawPath)
            if (f.exists() && f.canExecute()) f else env.resolveExecutable(f.name)
        } else {
            env.resolveExecutable(rawPath)
        }
        if (!binFile.exists() || !binFile.canExecute()) {
            Log.w("ProcessSupervisor", "$name binary not found or not executable at ${binFile.absolutePath}")
            return null
        }
        val resolvedCommand = listOf(binFile.absolutePath) + command.drop(1)
        return try {
            Log.i("ProcessSupervisor", "Launching binary $name: $resolvedCommand")
            val process = ProcessBuilder(resolvedCommand)
                .directory(env.homeDir)
                .redirectErrorStream(true)
                .apply { environment().putAll(env.getEnvMap()) }
                .start()
            activeProcesses.add(process)
            monitorProcessOutput(process, "NativeProcess:$name")
            process
        } catch (e: Exception) {
            Log.e("ProcessSupervisor", "Failed to launch binary $name", e)
            null
        }
    }

    fun runBinary(command: List<String>): Pair<Int, String> {
        val rawPath = command.first()
        val binFile = if (File(rawPath).isAbsolute) {
            val f = File(rawPath)
            if (f.exists() && f.canExecute()) f else env.resolveExecutable(f.name)
        } else {
            env.resolveExecutable(rawPath)
        }
        if (!binFile.exists() || !binFile.canExecute()) {
            Log.w("ProcessSupervisor", "Binary not found or not executable at ${binFile.absolutePath}")
            return Pair(-1, "Binary not found: ${binFile.absolutePath}")
        }
        val resolvedCommand = listOf(binFile.absolutePath) + command.drop(1)
        val workingDir = if (binFile.name.contains("xournalpp")) {
            File(env.shareDir, "xournalpp").takeIf { it.exists() } ?: env.homeDir
        } else {
            env.homeDir
        }
        return try {
            val process = ProcessBuilder(resolvedCommand)
                .directory(workingDir)
                .redirectErrorStream(true)
                .apply { environment().putAll(env.getEnvMap()) }
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            Pair(exitCode, output)
        } catch (e: Exception) {
            Log.e("ProcessSupervisor", "Error running binary: $resolvedCommand", e)
            Pair(-1, e.message ?: "Execution failed")
        }
    }

    fun terminateAll() {
        for (process in activeProcesses) {
            try {
                process.destroy()
                if (process.isAlive) {
                    Thread.sleep(100)
                    if (process.isAlive) {
                        process.destroyForcibly()
                    }
                }
            } catch (e: Exception) {
                // Ignore exceptions during teardown
            }
        }
        activeProcesses.clear()
        _documentTitle.value = null
    }
}

