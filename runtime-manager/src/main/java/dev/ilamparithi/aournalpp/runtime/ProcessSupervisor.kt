package dev.ilamparithi.aournalpp.runtime

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CopyOnWriteArrayList

class ProcessSupervisor(private val env: LinuxEnvironment) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeProcesses = CopyOnWriteArrayList<Process>()

    fun startKioskWindowManager(): Process? {
        val matchboxFile = File(env.binDir, "matchbox-window-manager")
        val openboxFile = File(env.binDir, "openbox")
        val wmFile = when {
            matchboxFile.exists() && matchboxFile.canExecute() -> matchboxFile
            openboxFile.exists() && openboxFile.canExecute() -> openboxFile
            else -> null
        }
        if (wmFile == null) {
            Log.w("ProcessSupervisor", "No window manager found in ${env.binDir.absolutePath}, skipping WM startup")
            return null
        }
        val command = if (wmFile.name == "openbox") {
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

    fun startXournal(targetFilePath: String? = null): Process? {
        val xournalFile = File(env.binDir, "xournalpp")
        if (!xournalFile.exists() || !xournalFile.canExecute()) {
            Log.e("ProcessSupervisor", "xournalpp not found at ${xournalFile.absolutePath}")
            return null
        }
        val command = mutableListOf(xournalFile.absolutePath)
        if (targetFilePath != null) {
            command.add(targetFilePath)
        }
        
        val process = ProcessBuilder(command)
            .directory(env.homeDir)
            .redirectErrorStream(true)
            .apply { environment().putAll(env.getEnvMap()) }
            .start()
            
        activeProcesses.add(process)
        monitorProcessOutput(process, "NativeProcess:Xournalpp")
        return process
    }

    fun startTitleWatcher(): Process? {
        val watcherFile = File(env.binDir, "xopp-title-watcher")
        if (!watcherFile.exists() || !watcherFile.canExecute()) {
            Log.w("ProcessSupervisor", "xopp-title-watcher not found or not executable at ${watcherFile.absolutePath}")
            return null
        }
        val command = listOf(watcherFile.absolutePath)
        Log.i("ProcessSupervisor", "Starting X11 title watcher: $command")
        val process = ProcessBuilder(command)
            .directory(env.homeDir)
            .redirectErrorStream(true)
            .apply { environment().putAll(env.getEnvMap()) }
            .start()

        activeProcesses.add(process)

        scope.launch {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
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
                Log.e("NativeProcess:TitleWatcher", "Error reading title watcher output", e)
            } finally {
                activeProcesses.remove(process)
            }
        }
        return process
    }

    private val ignoredWindowTitles = setOf(
        "Openbox",
        "com.github.xournalpp.xournalpp",
        "Xournal++",
        "X11",
        "Desktop",
        "Save As",
        "Open Document",
        "Export as PDF"
    )

    private fun sanitizeWindowTitle(raw: String): String {
        val cleaned = raw.replace(Regex("\\s*-\\s*Xournal\\+\\+.*$"), "")
            .replace(Regex("\\s*\\[autosaved\\]"), "")
            .trim()
            .removeSuffix("*")
            .trim()

        return if (ignoredWindowTitles.contains(cleaned) || ignoredWindowTitles.contains(raw)) {
            ""
        } else {
            cleaned
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
    }
}
