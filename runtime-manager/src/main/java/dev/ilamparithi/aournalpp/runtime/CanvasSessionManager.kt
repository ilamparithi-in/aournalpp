package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.os.ParcelFileDescriptor
import android.system.Os
import android.util.Log
import com.termux.x11.CmdEntryPoint
import com.termux.x11.LorieView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CanvasSessionManager(
    private val context: Context,
    private val env: LinuxEnvironment,
    private val supervisor: ProcessSupervisor,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "CanvasSessionManager"
    }

    private var isSessionRunning = false
    private var cmdEntryPoint: CmdEntryPoint? = null

    fun startSession(lorieView: LorieView, targetFilePath: String? = null) {
        if (isSessionRunning) return
        isSessionRunning = true

        scope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Initializing X11 socket environment...")
                File(env.tmpDir, ".X11-unix").mkdirs()
                File(env.tmpDir, ".X0-lock").delete()
                File(env.tmpDir, ".X11-unix/X0").delete()

                // Set environment for embedded X server
                try {
                    Os.setenv("TMPDIR", env.tmpDir.absolutePath, true)

                    val xkbDir = File(env.shareDir, "X11/xkb")
                    val xkbConfig2Dir = File(env.shareDir, "xkeyboard-config-2")
                    val effectiveXkbDir = if (xkbDir.exists() && xkbDir.isDirectory && xkbDir.canRead()) {
                        xkbDir
                    } else if (xkbConfig2Dir.exists() && xkbConfig2Dir.isDirectory) {
                        try {
                            if (xkbDir.exists() || !xkbDir.canRead()) {
                                xkbDir.delete()
                                Os.symlink(xkbConfig2Dir.absolutePath, xkbDir.absolutePath)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to repair X11/xkb symlink", e)
                        }
                        xkbConfig2Dir
                    } else {
                        xkbDir
                    }

                    Log.i(TAG, "Configuring XKB_CONFIG_ROOT: ${effectiveXkbDir.absolutePath}")
                    Os.setenv("XKB_CONFIG_ROOT", effectiveXkbDir.absolutePath, true)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set environment variables", e)
                }

                // 1. Initialize and start embedded Xlorie server on Main thread for AChoreographer/Looper
                withContext(Dispatchers.Main) {
                    try {
                        System.loadLibrary("Xlorie")
                    } catch (e: UnsatisfiedLinkError) {
                        Log.w(TAG, "libXlorie.so already loaded or loading failed", e)
                    }

                    Log.i(TAG, "Starting Xlorie server on :0 via CmdEntryPoint...")
                    val entryPoint = CmdEntryPoint(context, arrayOf(":0", "-listen", "tcp", "-ac"))
                    cmdEntryPoint = entryPoint

                    val pfd: ParcelFileDescriptor? = entryPoint.xConnection
                    if (pfd != null) {
                        val fd = pfd.detachFd()
                        Log.i(TAG, "Connecting LorieView to X server fd=$fd...")
                        lorieView.connect(fd)
                        lorieView.post {
                            lorieView.requestLayout()
                        }
                    } else {
                        Log.e(TAG, "Failed to obtain X connection file descriptor")
                    }
                }

                delay(300) // Allow X server startup and connection settle

                // 2. Start Kiosk Window Manager (Matchbox)
                Log.i(TAG, "Starting Matchbox Window Manager...")
                supervisor.startKioskWindowManager()
                delay(200) // Allow WM socket initialization

                // 3. Launch Xournal++ (passes targetFilePath if opening an existing file; launches blank if null)
                if (targetFilePath.isNullOrBlank()) {
                    Log.i(TAG, "Launching Xournal++ without arguments (blank canvas)...")
                    supervisor.startXournal()
                } else {
                    Log.i(TAG, "Launching Xournal++ with existing file: $targetFilePath")
                    supervisor.startXournal(targetFilePath)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize canvas session", e)
            }
        }
    }

    fun stopSession() {
        if (!isSessionRunning) return
        Log.i(TAG, "Stopping canvas session...")
        supervisor.terminateAll()
        File(env.tmpDir, ".X0-lock").delete()
        File(env.tmpDir, ".X11-unix/X0").delete()
        isSessionRunning = false
    }
}
