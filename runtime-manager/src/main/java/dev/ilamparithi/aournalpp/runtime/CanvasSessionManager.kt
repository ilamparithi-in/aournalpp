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

    fun startSession(
        lorieView: LorieView,
        targetFilePath: String? = null,
        openPreferencesOnLaunch: Boolean = false
    ) {
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

                // 2. Start Kiosk Window Manager (Openbox / Matchbox)
                Log.i(TAG, "Starting Kiosk Window Manager...")
                supervisor.startKioskWindowManager()
                delay(200) // Allow WM socket initialization

                // 3. Paint X11 Root Window Wallpaper (System, Custom, or Theme Backdrop)
                try {
                    val ppmFile = File(env.tmpDir, "wallpaper.ppm")
                    val wallpaperBitmap = WallpaperHelper.resolveWallpaperBitmap(context)
                    WallpaperHelper.exportBitmapToPpm(wallpaperBitmap, ppmFile)
                    if (ppmFile.exists()) {
                        supervisor.setX11Wallpaper(ppmFile.absolutePath)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to apply X11 root wallpaper", e)
                }

                // 4. Launch Xournal++ (passes targetFilePath if opening an existing file; launches blank if null)
                if (targetFilePath.isNullOrBlank()) {
                    Log.i(TAG, "Launching Xournal++ without arguments (blank canvas)...")
                    supervisor.startXournal()
                } else {
                    Log.i(TAG, "Launching Xournal++ with existing file: $targetFilePath")
                    supervisor.startXournal(targetFilePath)
                }

                // 5. Start X11 Title Watcher to monitor document renames & saves
                supervisor.startTitleWatcher()

                // 6. Start GTK IME Focus Bridge server
                startImeBridgeServer(lorieView)

                // 6. Direct Preferences Launcher injection if requested
                if (openPreferencesOnLaunch) {
                    monitorAndInjectPreferencesShortcut()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize canvas session", e)
            }
        }
    }

    private fun monitorAndInjectPreferencesShortcut() {
        scope.launch(Dispatchers.IO) {
            val xdotoolBin = File(env.binDir, "xdotool")
            if (!xdotoolBin.exists() || !xdotoolBin.canExecute()) {
                Log.w(TAG, "xdotool not found at ${xdotoolBin.absolutePath}, cannot inject preferences shortcut")
                return@launch
            }

            // Initial settling delay
            delay(500)

            var attempts = 0
            val maxAttempts = 30

            while (isSessionRunning && attempts < maxAttempts) {
                attempts++
                try {
                    // Check if Preferences window is already open
                    val (searchCode, searchOut) = supervisor.runBinary(
                        listOf(xdotoolBin.absolutePath, "search", "--name", "Preferences")
                    )

                    if (searchCode == 0 && searchOut.trim().isNotEmpty()) {
                        Log.i(TAG, "Preferences dialog detected on display (attempt $attempts). Injection complete.")
                        break
                    }

                    // Check if main Xournal++ window is visible
                    val (winCode, winOut) = supervisor.runBinary(
                        listOf(xdotoolBin.absolutePath, "search", "--onlyvisible", "--class", "xournalpp")
                    )

                    if (winCode == 0 && winOut.trim().isNotEmpty()) {
                        Log.i(TAG, "Xournal++ window visible (attempt $attempts). Injecting Ctrl+Comma shortcut...")
                        supervisor.runBinary(
                            listOf(xdotoolBin.absolutePath, "key", "--clearmodifiers", "ctrl+comma")
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception during preferences shortcut check (attempt $attempts)", e)
                }
                delay(500)
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

    fun setOnProcessExitListener(listener: () -> Unit) {
        supervisor.setOnXournalExitListener(listener)
    }

    fun requestCloseSession() {
        if (!isSessionRunning) return
        scope.launch(Dispatchers.IO) {
            val xdotoolBin = File(env.binDir, "xdotool")
            if (!xdotoolBin.exists() || !xdotoolBin.canExecute()) {
                Log.w(TAG, "xdotool not available at ${xdotoolBin.absolutePath}, cannot send graceful close shortcut")
                return@launch
            }

            Log.i(TAG, "Initiating graceful close for active Xournal++ session...")

            val searchQueries = listOf(
                listOf("search", "--onlyvisible", "--class", "xournalpp"),
                listOf("search", "--onlyvisible", "--class", "xournal"),
                listOf("search", "--onlyvisible", "--name", "Xournal"),
                listOf("search", "--onlyvisible", "--classname", "xournalpp"),
                listOf("search", "--onlyvisible", "")
            )

            val windowIds = mutableListOf<String>()

            for (query in searchQueries) {
                val cmd = mutableListOf(xdotoolBin.absolutePath).apply { addAll(query) }
                val (code, out) = supervisor.runBinary(cmd)
                if (code == 0 && out.isNotBlank()) {
                    val ids = out.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }
                    if (ids.isNotEmpty()) {
                        windowIds.addAll(ids)
                        break
                    }
                }
            }

            if (windowIds.isNotEmpty()) {
                // Focus and close the topmost / active X11 window
                val topWid = windowIds.last()
                Log.i(TAG, "Found visible windows $windowIds. Sending WM_DELETE_WINDOW & Ctrl+Q to top window $topWid...")

                // 1. Activate window so GTK receives focus
                supervisor.runBinary(listOf(xdotoolBin.absolutePath, "windowactivate", "--sync", topWid))

                // 2. Send WM_DELETE_WINDOW (native X11 window close message)
                supervisor.runBinary(listOf(xdotoolBin.absolutePath, "windowclose", topWid))

                // 3. Inject Ctrl+Q directly to the window structure
                supervisor.runBinary(
                    listOf(xdotoolBin.absolutePath, "key", "--window", topWid, "--clearmodifiers", "ctrl+q")
                )
                supervisor.runBinary(
                    listOf(xdotoolBin.absolutePath, "key", "--window", topWid, "--clearmodifiers", "Control_L+q")
                )
            } else {
                Log.w(TAG, "No visible X11 window found via search, falling back to global keystrokes...")
                supervisor.runBinary(
                    listOf(xdotoolBin.absolutePath, "key", "--clearmodifiers", "ctrl+q")
                )
                supervisor.runBinary(
                    listOf(xdotoolBin.absolutePath, "key", "--clearmodifiers", "Control_L+q")
                )
            }
        }
    }

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

