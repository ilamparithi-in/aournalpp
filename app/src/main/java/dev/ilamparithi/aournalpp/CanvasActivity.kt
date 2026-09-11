package dev.ilamparithi.aournalpp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.DragEvent
import android.view.KeyEvent
import android.view.PixelCopy
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.termux.x11.LorieView
import com.termux.x11.input.InputEventSender
import com.termux.x11.input.LenovoPenButtonMapper
import com.termux.x11.input.TouchInputHandler
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.runtime.CanvasSessionManager
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.ui.canvas.CanvasScreen
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class CanvasActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NOTE_PATH = "dev.ilamparithi.aournalpp.extra.NOTE_PATH"
        const val EXTRA_OPEN_PREFERENCES = "dev.ilamparithi.aournalpp.extra.OPEN_PREFERENCES"
        const val EXTRA_OPEN_PREFS_ALIAS = "EXTRA_OPEN_PREFERENCES"
        const val EXTRA_TRIGGER_APP_EXIT = "dev.ilamparithi.aournalpp.extra.TRIGGER_APP_EXIT"

        @Volatile
        private var instance: CanvasActivity? = null

        fun handleBackgroundCloseRequest() {
            instance?.requestBackgroundClose()
        }

        fun notifyPreferenceChanged(key: String) {
            instance?.onPreferenceChanged(key)
        }
    }

    internal val preferenceUpdateVersionState = mutableIntStateOf(0)

    fun onPreferenceChanged(key: String) {
        preferenceUpdateVersionState.intValue++
    }

    override fun onResume() {
        super.onResume()
        preferenceUpdateVersionState.intValue++
    }

    internal lateinit var env: LinuxEnvironment
    internal lateinit var supervisor: ProcessSupervisor
    fun isSupervisorInitialized(): Boolean = ::supervisor.isInitialized
    internal lateinit var sessionManager: CanvasSessionManager
    internal var inputHandler: TouchInputHandler? = null
    internal var inputSender: InputEventSender? = null
    internal var penMapper: LenovoPenButtonMapper? = null
    internal var activeLorieView: LorieView? = null

    internal val showEmergencyForceCloseDialogState = mutableStateOf(false)
    internal val isKeyboardOpenState = mutableStateOf(false)
    private val backPressTimestamps = mutableListOf<Long>()

    private var cameraTempFile: File? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraTempFile
        if (success && file != null && file.exists() && file.length() > 0L) {
            processAndPasteCameraImage(file)
        } else if (file != null && file.exists() && file.length() == 0L) {
            try { file.delete() } catch (_: Exception) {}
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            processAndPasteImageUri(uri)
        }
    }

    private val fileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            processAndPasteImageUri(uri)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        bindMainProcessBridge()
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleSmartBackPress()
            }
        })

        // Initialize X11 preferences with defaults (e.g. Direct Touch)
        dev.ilamparithi.aournalpp.data.X11Preferences.initDefaults(this)
        val x11Prefs = dev.ilamparithi.aournalpp.data.X11Preferences.getPrefs(this)

        val isFullscreen = x11Prefs.getBoolean(dev.ilamparithi.aournalpp.data.X11Preferences.KEY_FULLSCREEN, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (isFullscreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        // Ensure window manager does not pan or push the activity when keyboard appears
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        // Screen idle timeout configuration
        val idleTimeoutMode = x11Prefs.getString(dev.ilamparithi.aournalpp.data.X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, "system") ?: "system"
        when (idleTimeoutMode) {
            "never" -> window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            "system" -> window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else -> window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // High-performance debounce-aware soft keyboard Reseed and state listener
        val reseedEnabled = x11Prefs.getBoolean(dev.ilamparithi.aournalpp.data.X11Preferences.KEY_RESEED, false)
        var lastImeHeight = -1
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeHeight = imeInsets.bottom
            val isImeOpen = imeHeight > 0 || insets.isVisible(WindowInsetsCompat.Type.ime())
            isKeyboardOpenState.value = isImeOpen

            if (imeHeight != lastImeHeight) {
                lastImeHeight = imeHeight
                activeLorieView?.let { view ->
                    if (reseedEnabled) {
                        view.setContentInsets(0, 0, 0, imeHeight)
                        view.setObscuredBottom(0)
                    } else {
                        view.setContentInsets(0, 0, 0, 0)
                        view.setObscuredBottom(imeHeight)
                    }
                    if (!isImeOpen) {
                        view.setKeyboardVisible(false)
                    }
                }
            }
            insets
        }

        com.termux.x11.MainActivity.setPrefs(com.termux.x11.Prefs(this))

        env = LinuxEnvironment(this)
        supervisor = ProcessSupervisor(env)
        sessionManager = CanvasSessionManager(
            context = this,
            env = env,
            supervisor = supervisor,
            scope = lifecycleScope
        )

        // Automatically finish session and return to MainActivity when Xournal++ terminates
        sessionManager.setOnProcessExitListener {
            runOnUiThread {
                if (!isFinishing) {
                    Log.i("CanvasActivity", "X11 / Xournal++ session terminated. isAppExitInProgress=$isAppExitInProgress")
                    sessionManager.stopSession()
                    try {
                        sendBroadcast(Intent("dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED").setPackage(packageName))
                    } catch (_: Exception) {}
                    if (!isAppExitInProgress) {
                        navigateBackToHome()
                    }
                    finish()
                }
            }
        }

        val openPreferences = intent.getBooleanExtra(EXTRA_OPEN_PREFERENCES, false)
            || intent.getBooleanExtra(EXTRA_OPEN_PREFS_ALIAS, false)
            || intent.getBooleanExtra("EXTRA_OPEN_PREFS", false)

        val targetPath = intent.getStringExtra(EXTRA_NOTE_PATH)
        val prefs = dev.ilamparithi.aournalpp.data.AppPreferences.getGeneral(this)
        if (targetPath != null) {
            prefs.edit().putString("pref_last_opened_note_path", targetPath).apply()
            lifecycleScope.launch(Dispatchers.IO) {
                dev.ilamparithi.aournalpp.data.DocumentRepository.getInstance(this@CanvasActivity).recordNoteOpened(targetPath)
            }
        } else {
            prefs.edit().remove("pref_last_opened_note_path").apply()
        }

        val initialTitle = when {
            targetPath != null -> File(targetPath).name
            openPreferences -> "Preferences"
            else -> "New Note"
        }

        if (intent.getBooleanExtra(EXTRA_TRIGGER_APP_EXIT, false)) {
            handleExitRequest()
        }

        setContent {
            AournalTheme {
                dev.ilamparithi.aournalpp.ui.canvas.CanvasScreen(
                    activity = this,
                    targetPath = targetPath,
                    initialTitle = initialTitle,
                    openPreferences = openPreferences
                )
            }
        }
    }

    internal fun navigateBackToHome() {
        val homeIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        startActivity(homeIntent)
    }

    private var isAppExitInProgress = false

    private fun handleExitRequest() {
        requestBackgroundClose()
    }

    fun requestBackgroundClose() {
        isAppExitInProgress = true
        Log.i("CanvasActivity", "Executing focus-aware sequential close for app exit...")
        sessionManager.initiateFocusAwareSequentialClose(
            onAllClosed = {
                runOnUiThread {
                    Log.i("CanvasActivity", "All Xournal++ windows closed. Finishing CanvasActivity cleanly.")
                    sessionManager.stopSession()
                    try {
                        sendBroadcast(Intent("dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED").setPackage(packageName))
                    } catch (_: Exception) {}
                    finish()
                }
            },
            onAborted = {
                runOnUiThread {
                    isAppExitInProgress = false
                    Log.i("CanvasActivity", "Sequential exit aborted by user.")
                    Toast.makeText(this@CanvasActivity, "Exit aborted", Toast.LENGTH_SHORT).show()
                }
            },
            onPromptBlocking = {
                runOnUiThread {
                    Log.i("CanvasActivity", "Prompt blocking exit detected! Bringing CanvasActivity to foreground...")
                    val bringToFrontIntent = Intent(this@CanvasActivity, CanvasActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    startActivity(bringToFrontIntent)
                    Toast.makeText(this@CanvasActivity, "Save or discard changes to exit", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    internal fun handleSmartBackPress() {
        // If force close dialog is already active, ignore back press to avoid accidental dismissal
        if (showEmergencyForceCloseDialogState.value) {
            return
        }

        val now = System.currentTimeMillis()
        backPressTimestamps.add(now)
        backPressTimestamps.removeAll { now - it > 2000 }

        val prefs = dev.ilamparithi.aournalpp.data.AppPreferences.getGeneral(this)
        val tripleBackEnabled = prefs.getBoolean("pref_triple_back_force_close", true)

        if (tripleBackEnabled && backPressTimestamps.size >= 3) {
            backPressTimestamps.clear()
            showEmergencyForceCloseDialogState.value = true
            return
        }

        lifecycleScope.launch {
            if (sessionManager.isModalOrDialogOpen()) {
                sessionManager.dismissTopDialogOrModal()
            } else {
                navigateBackToHome()
            }
        }
    }



    internal fun handleCloseWindow() {
        lifecycleScope.launch {
            if (sessionManager.isModalOrDialogOpen()) {
                sessionManager.dismissTopDialogOrModal()
                Toast.makeText(this@CanvasActivity, "Close the open prompt to exit", Toast.LENGTH_SHORT).show()
            } else {
                val prefs = X11Preferences.getPrefs(this@CanvasActivity)
                val behavior = prefs.getString(
                    X11Preferences.KEY_CLOSE_BUTTON_BEHAVIOR,
                    X11Preferences.CLOSE_BEHAVIOR_FOREGROUND
                )
                if (behavior == X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL) {
                    sessionManager.initiateFocusAwareSequentialClose(
                        onAllClosed = {
                            runOnUiThread {
                                sessionManager.stopSession()
                                try {
                                    sendBroadcast(Intent("dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED").setPackage(packageName))
                                } catch (_: Exception) {}
                                navigateBackToHome()
                                finish()
                            }
                        },
                        onAborted = {
                            Toast.makeText(this@CanvasActivity, "Exit cancelled", Toast.LENGTH_SHORT).show()
                        },
                        onPromptBlocking = {
                            Toast.makeText(this@CanvasActivity, "Save or discard changes to exit", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    injectCtrlQDirect()
                    delay(350)
                    if (sessionManager.isModalOrDialogOpen()) {
                        Toast.makeText(this@CanvasActivity, "Save or discard changes to exit", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    internal fun injectKeyboardShortcut(keyCode: Int, shortcutStr: String) {
        val lorieView = activeLorieView
        if (lorieView != null) {
            lorieView.requestFocus()
            lorieView.post {
                lorieView.sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, true)
                lorieView.postDelayed({
                    lorieView.sendKeyEvent(0, keyCode, true)
                    lorieView.sendKeyEvent(0, keyCode, false)
                    lorieView.sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, false)
                }, 30)
            }
        } else {
            sessionManager.injectShortcut(shortcutStr)
        }
    }

    internal fun injectCtrlQDirect() {
        injectKeyboardShortcut(KeyEvent.KEYCODE_Q, "ctrl+q")
    }

    internal fun setupDragAndDropListener(view: LorieView) {
        view.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    val desc = event.clipDescription
                    desc != null && (
                        desc.hasMimeType("image/*") ||
                        desc.hasMimeType("image/png") ||
                        desc.hasMimeType("image/jpeg") ||
                        desc.hasMimeType("image/webp") ||
                        desc.hasMimeType("image/bmp") ||
                        desc.hasMimeType("text/plain") ||
                        desc.hasMimeType("text/html")
                    )
                }
                DragEvent.ACTION_DRAG_ENTERED -> true
                DragEvent.ACTION_DRAG_LOCATION -> true
                DragEvent.ACTION_DROP -> {
                    try {
                        requestDragAndDropPermissions(event)
                    } catch (e: Exception) {
                        Log.w("CanvasActivity", "Could not request drag and drop permissions", e)
                    }
                    val clipData = event.clipData
                    if (clipData != null && clipData.itemCount > 0) {
                        val item = clipData.getItemAt(0)
                        val uri = item.uri
                        val text = item.text ?: item.htmlText
                        if (uri != null) {
                            processAndPasteImageUri(uri)
                        } else if (text != null) {
                            activeLorieView?.stageClipboardText(text.toString())
                            injectKeyboardShortcut(KeyEvent.KEYCODE_V, "ctrl+v")
                            Toast.makeText(this@CanvasActivity, "Text pasted", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> true
                else -> false
            }
        }
    }

    internal fun launchCameraCapture() {
        try {
            val cameraDir = File(LinuxEnvironment(this).getNotesDirectory(), ".temp/Camera").apply {
                if (!exists()) mkdirs()
            }
            val tempFile = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
            cameraTempFile = tempFile
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", tempFile)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e("CanvasActivity", "Error launching camera", e)
            Toast.makeText(this, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    internal fun launchGalleryPicker() {
        try {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: Exception) {
            Log.w("CanvasActivity", "PhotoPicker unavailable, falling back to file picker", e)
            launchFilePicker()
        }
    }

    internal fun launchFilePicker() {
        try {
            fileLauncher.launch(arrayOf("image/*"))
        } catch (e: Exception) {
            Log.e("CanvasActivity", "Error launching file picker", e)
            Toast.makeText(this, "Could not open file picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processAndPasteImageUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (sessionManager.isModalOrDialogOpen()) {
                    withContext(Dispatchers.Main.immediate) {
                        Toast.makeText(this@CanvasActivity, "Close open dialogs before inserting image", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                var rotationDegrees = 0f
                try {
                    contentResolver.openInputStream(uri)?.use { exifStream ->
                        val exif = ExifInterface(exifStream)
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        rotationDegrees = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                    }
                } catch (e: Exception) {
                    Log.w("CanvasActivity", "Could not parse EXIF from URI", e)
                }

                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { isStream ->
                    BitmapFactory.decodeStream(isStream, null, boundsOptions)
                }

                val maxDim = 2560
                var sampleSize = 1
                while (boundsOptions.outWidth / sampleSize > maxDim || boundsOptions.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }

                val isPng = boundsOptions.outMimeType?.equals("image/png", ignoreCase = true) == true

                val pngBytes: ByteArray = if (isPng && sampleSize == 1 && rotationDegrees == 0f) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                } else {
                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val originalBitmap = contentResolver.openInputStream(uri)?.use { isStream ->
                        BitmapFactory.decodeStream(isStream, null, decodeOptions)
                    }

                    if (originalBitmap != null) {
                        val finalBitmap = if (rotationDegrees != 0f) {
                            val matrix = Matrix().apply { postRotate(rotationDegrees) }
                            Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true).also {
                                if (it != originalBitmap) originalBitmap.recycle()
                            }
                        } else {
                            originalBitmap
                        }

                        val baos = ByteArrayOutputStream()
                        finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        val bytes = baos.toByteArray()
                        finalBitmap.recycle()
                        bytes
                    } else {
                        contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    }
                }

                if (pngBytes.isNotEmpty()) {
                    withContext(Dispatchers.Main.immediate) {
                        // Isolated in-memory X11 clipboard push: does NOT touch host Android clipboard!
                        activeLorieView?.stageClipboardImage(pngBytes)
                        injectKeyboardShortcut(KeyEvent.KEYCODE_V, "ctrl+v")
                        Toast.makeText(this@CanvasActivity, "Image inserted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main.immediate) {
                        Toast.makeText(this@CanvasActivity, "Failed to read image data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CanvasActivity", "Failed to process image URI", e)
                withContext(Dispatchers.Main.immediate) {
                    Toast.makeText(this@CanvasActivity, "Failed to insert image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    internal fun processAndPasteCameraImage(file: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (sessionManager.isModalOrDialogOpen()) {
                    withContext(Dispatchers.Main.immediate) {
                        Toast.makeText(this@CanvasActivity, "Close open dialogs before inserting image", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val exif = ExifInterface(file.absolutePath)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }

                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

                val maxDim = 2560
                var sampleSize = 1
                while (boundsOptions.outWidth / sampleSize > maxDim || boundsOptions.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val originalBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)

                val finalBitmap = if (rotationDegrees != 0f && originalBitmap != null) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees) }
                    Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true).also {
                        if (it != originalBitmap) originalBitmap.recycle()
                    }
                } else {
                    originalBitmap
                }

                if (finalBitmap != null) {
                    val baos = ByteArrayOutputStream()
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val pngBytes = baos.toByteArray()
                    finalBitmap.recycle()

                    withContext(Dispatchers.Main.immediate) {
                        // Isolated in-memory X11 clipboard push: does NOT touch host Android clipboard!
                        activeLorieView?.stageClipboardImage(pngBytes)
                        injectKeyboardShortcut(KeyEvent.KEYCODE_V, "ctrl+v")
                        Toast.makeText(this@CanvasActivity, "Photo inserted", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CanvasActivity", "Failed to process captured camera photo", e)
                withContext(Dispatchers.Main.immediate) {
                    Toast.makeText(this@CanvasActivity, "Failed to insert photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            return super.dispatchKeyEvent(event)
        }
        val mapper = penMapper
        if (mapper != null && mapper.onKeyEvent(event)) {
            return true
        }
        val sender = inputSender
        if (sender != null && sender.sendKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val triggerExit = intent.getBooleanExtra(EXTRA_TRIGGER_APP_EXIT, false)
        if (triggerExit) {
            handleExitRequest()
            return
        }

        val openPrefs = intent.getBooleanExtra(EXTRA_OPEN_PREFERENCES, false) ||
                intent.getBooleanExtra(EXTRA_OPEN_PREFS_ALIAS, false)
        if (openPrefs) {
            injectKeyboardShortcut(KeyEvent.KEYCODE_COMMA, "ctrl+comma")
            return
        }

        val targetPath = intent.getStringExtra(EXTRA_NOTE_PATH)
        if (!targetPath.isNullOrBlank()) {
            val prefs = dev.ilamparithi.aournalpp.data.AppPreferences.getGeneral(this)
            prefs.edit().putString("pref_last_opened_note_path", targetPath).apply()
            lifecycleScope.launch(Dispatchers.IO) {
                dev.ilamparithi.aournalpp.data.DocumentRepository.getInstance(this@CanvasActivity).recordNoteOpened(targetPath)
            }
            sessionManager.openNoteInNewWindow(targetPath)
        }
    }

    private var bridgeServiceConnection: android.content.ServiceConnection? = null

    private fun bindMainProcessBridge() {
        if (bridgeServiceConnection != null) return
        val connection = object : android.content.ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                Log.d("CanvasActivity", "Connected to MainProcessBridgeService (main process protected)")
            }

            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                Log.d("CanvasActivity", "Disconnected from MainProcessBridgeService")
            }
        }
        val intent = Intent(this, dev.ilamparithi.aournalpp.runtime.MainProcessBridgeService::class.java)
        try {
            if (bindService(intent, connection, Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT)) {
                bridgeServiceConnection = connection
            }
        } catch (e: Exception) {
            Log.w("CanvasActivity", "Failed to bind to MainProcessBridgeService", e)
        }
    }

    private fun unbindMainProcessBridge() {
        bridgeServiceConnection?.let {
            try {
                unbindService(it)
            } catch (e: Exception) {
                Log.w("CanvasActivity", "Failed to unbind MainProcessBridgeService", e)
            }
            bridgeServiceConnection = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindMainProcessBridge()
        if (instance == this) {
            instance = null
        }
        if (isFinishing) {
            sessionManager.stopSession()
            // Terminate isolated :canvas process so the next launch initializes a fresh native X11 instance
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
