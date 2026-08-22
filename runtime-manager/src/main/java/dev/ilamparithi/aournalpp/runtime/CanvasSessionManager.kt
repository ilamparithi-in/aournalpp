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

                // Set environment for embedded X server
                try {
                    Os.setenv("TMPDIR", env.tmpDir.absolutePath, true)
                    Os.setenv("XKB_CONFIG_ROOT", File(env.shareDir, "X11/xkb").absolutePath, true)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set environment variables", e)
                }

                // 1. Initialize and start embedded Xlorie server
                try {
                    System.loadLibrary("Xlorie")
                } catch (e: UnsatisfiedLinkError) {
                    Log.w(TAG, "libXlorie.so already loaded or loading failed", e)
                }

                Log.i(TAG, "Starting Xlorie server on :0...")
                CmdEntryPoint.start(arrayOf(":0"))
                delay(300) // Allow X server thread socket setup

                // 2. Connect LorieView to X connection
                try {
                    val entryPoint = CmdEntryPoint(arrayOf(":0"))
                    cmdEntryPoint = entryPoint
                    val pfd: ParcelFileDescriptor? = entryPoint.xConnection
                    if (pfd != null) {
                        val fd = pfd.detachFd()
                        lorieView.post {
                            lorieView.connect(fd)
                        }
                    } else {
                        Log.e(TAG, "Failed to obtain X connection file descriptor")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting LorieView", e)
                }

                // 3. Start Kiosk Window Manager (Matchbox)
                Log.i(TAG, "Starting Matchbox Window Manager...")
                supervisor.startKioskWindowManager()
                delay(200) // Allow WM socket initialization

                // 4. Launch Xournal++ (passes targetFilePath if opening an existing file; launches blank if null)
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
        isSessionRunning = false
    }
}
