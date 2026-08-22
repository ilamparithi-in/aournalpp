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
        val openboxFile = File(env.binDir, "openbox")
        val matchboxFile = File(env.binDir, "matchbox-window-manager")
        val wmFile = when {
            openboxFile.exists() && openboxFile.canExecute() -> openboxFile
            matchboxFile.exists() && matchboxFile.canExecute() -> matchboxFile
            else -> null
        }
        if (wmFile == null) {
            Log.w("ProcessSupervisor", "No window manager found in ${env.binDir.absolutePath}, skipping WM startup")
            return null
        }
        val command = if (wmFile.name == "openbox") {
            listOf(wmFile.absolutePath, "--sm-disable")
        } else {
            listOf(wmFile.absolutePath, "-use_titlebar", "no")
        }
        val process = ProcessBuilder(command)
            .directory(env.homeDir)
            .redirectErrorStream(true)
            .apply { environment().putAll(env.getEnvMap()) }
            .start()
            
        activeProcesses.add(process)
        monitorProcessOutput(process, "NativeProcess:WindowManager")
        return process
    }

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
