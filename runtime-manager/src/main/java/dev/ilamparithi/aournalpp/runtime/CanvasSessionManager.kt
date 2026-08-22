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
                Log.i(TAG, "Ensuring runtime environment tree, storage symlinks, bookmarks, and settings...")
                env.ensureDirectoryTree()

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

                // 4. Start X11 Title Watcher to monitor document renames & saves
                supervisor.startTitleWatcher()

                // 5. Start GTK IME Focus Bridge server
                startImeBridgeServer(lorieView)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize canvas session", e)
            }
        }
    }

    private var imeServer: android.net.LocalServerSocket? = null
    private var imeServerRunning = false

    private fun startImeBridgeServer(lorieView: LorieView) {
        scope.launch(Dispatchers.IO) {
            try {
                imeServer?.close()
                val server = android.net.LocalServerSocket("aournal_ime_bridge")
                imeServer = server
                imeServerRunning = true
                Log.i(TAG, "IME focus bridge server listening on @aournal_ime_bridge")

                while (imeServerRunning && isSessionRunning) {
                    try {
                        val client = server.accept()
                        scope.launch(Dispatchers.IO) {
                            try {
                                client.inputStream.bufferedReader().use { reader ->
                                    var line: String? = reader.readLine()
                                    while (line != null && isSessionRunning) {
                                        val cmd = line.trim()
                                        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
                                        val autoShowEnabled = prefs.getBoolean("pref_auto_show_ime_on_focus", true)

                                        if (autoShowEnabled) {
                                            if (cmd == "FOCUS_IN") {
                                                withContext(Dispatchers.Main) {
                                                    lorieView.requestFocus()
                                                    lorieView.setKeyboardVisible(true)
                                                }
                                            } else if (cmd == "FOCUS_OUT") {
                                                withContext(Dispatchers.Main) {
                                                    lorieView.setKeyboardVisible(false)
                                                }
                                            }
                                        }
                                        line = reader.readLine()
                                    }
                                }
                            } catch (e: Exception) {
                                // Client socket closed
                            }
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start IME focus bridge server", e)
            }
        }
    }

    val documentTitle: kotlinx.coroutines.flow.StateFlow<String?>
        get() = supervisor.documentTitle

    fun stopSession() {
        if (!isSessionRunning) return
        Log.i(TAG, "Stopping canvas session...")
        imeServerRunning = false
        try {
            imeServer?.close()
            imeServer = null
        } catch (e: Exception) {
            // Ignore
        }
        supervisor.terminateAll()
        File(env.tmpDir, ".X0-lock").delete()
        File(env.tmpDir, ".X11-unix/X0").delete()
        isSessionRunning = false
    }
}
