package dev.ilamparithi.aournalpp.logging

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Global UncaughtExceptionHandler and crash reporter for Aournal++.
 *
 * Intercepts JVM exceptions across all app threads and processes (:main, :canvas),
 * generates a comprehensive diagnostic report, persists it to private storage,
 * and delegates to the default Android handler.
 */
class CrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    private val isHandlingCrash = AtomicBoolean(false)
    private val storage = CrashLogStorage(context)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (isHandlingCrash.compareAndSet(false, true)) {
            try {
                val report = buildJvmCrashReport(thread, throwable)
                storage.saveCrashReport(report)
                Log.e(TAG, "Uncaught exception safely captured to crash log: ${report.id}", throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture crash report", e)
            }
        }

        // Delegate to original system handler so Android can manage process termination
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun buildJvmCrashReport(thread: Thread, throwable: Throwable): CrashReport {
        val now = System.currentTimeMillis()
        val processName = getProcessName(context)
        val processSuffix = processName.substringAfterLast(":", "main").replace("[^a-zA-Z0-9]".toRegex(), "")
        val timeFormatted = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
        val reportId = "crash_${timeFormatted}_${processSuffix}"

        val deviceMetadata = extractDeviceMetadata(context)
        val stackTrace = getStackTraceString(throwable)
        val recentLogs = try {
            LogcatReader.readLogcat(120).map { it.raw }
        } catch (_: Exception) {
            emptyList()
        }

        return CrashReport(
            id = reportId,
            timestamp = now,
            type = CrashType.JVM_EXCEPTION,
            processName = processName,
            threadName = thread.name,
            errorClass = throwable.javaClass.name,
            errorMessage = throwable.message ?: "No error message provided",
            stackTrace = stackTrace,
            deviceMetadata = deviceMetadata,
            recentLogs = recentLogs,
            exitCode = null
        )
    }

    companion object {
        private const val TAG = "CrashHandler"

        @Volatile
        private var isInstalled = false

        /**
         * Installs the crash handler globally across all threads in the current process.
         */
        @JvmStatic
        fun install(context: Context) {
            if (isInstalled) return
            synchronized(this) {
                if (isInstalled) return
                val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
                val handler = CrashHandler(context.applicationContext, existingHandler)
                Thread.setDefaultUncaughtExceptionHandler(handler)
                isInstalled = true
                Log.i(TAG, "CrashHandler installed for process ${getProcessName(context)}")
            }
        }

        /**
         * Explicitly records a crash or abnormal termination of a native binary (e.g. Xournal++ or X11).
         */
        @JvmStatic
        fun recordNativeCrash(
            context: Context,
            binaryName: String,
            exitCode: Int,
            stdoutStderrTail: List<String>
        ): CrashReport? {
            return try {
                val now = System.currentTimeMillis()
                val timeFormatted = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
                val reportId = "crash_native_${timeFormatted}_${binaryName.replace("[^a-zA-Z0-9]".toRegex(), "")}"
                val deviceMetadata = extractDeviceMetadata(context)

                val recentLogcat = try {
                    LogcatReader.readLogcat(100).map { it.raw }
                } catch (_: Exception) {
                    emptyList()
                }

                val combinedLogs = if (stdoutStderrTail.isNotEmpty()) {
                    listOf("--- Native Process ($binaryName) Output ---") +
                            stdoutStderrTail +
                            listOf("--- System Logcat Tail ---") +
                            recentLogcat
                } else {
                    recentLogcat
                }

                val report = CrashReport(
                    id = reportId,
                    timestamp = now,
                    type = CrashType.NATIVE_PROCESS,
                    processName = getProcessName(context),
                    threadName = null,
                    errorClass = "NativeProcessCrash ($binaryName)",
                    errorMessage = "Native binary '$binaryName' terminated abnormally with exit code $exitCode",
                    stackTrace = stdoutStderrTail.joinToString("\n").ifBlank { "Exit code $exitCode" },
                    deviceMetadata = deviceMetadata,
                    recentLogs = combinedLogs,
                    exitCode = exitCode
                )

                val storage = CrashLogStorage(context)
                storage.saveCrashReport(report)
                Log.w(TAG, "Recorded native crash report for $binaryName (exitCode=$exitCode): $reportId")
                report
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record native crash report for $binaryName", e)
                null
            }
        }

        fun getProcessName(context: Context): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return Application.getProcessName()
            }
            return try {
                File("/proc/self/cmdline").readText(Charsets.UTF_8).trim().replace("\u0000", "")
            } catch (_: Exception) {
                context.packageName
            }
        }

        fun extractDeviceMetadata(context: Context): DeviceMetadata {
            val (versionName, versionCode) = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toLong()
                }
                Pair(pInfo.versionName ?: "Unknown", vCode)
            } catch (_: Exception) {
                Pair("Unknown", 0L)
            }

            var availRam = -1L
            var totalRam = -1L
            try {
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                if (actManager != null) {
                    val memInfo = ActivityManager.MemoryInfo()
                    actManager.getMemoryInfo(memInfo)
                    availRam = memInfo.availMem / (1024 * 1024)
                    totalRam = memInfo.totalMem / (1024 * 1024)
                }
            } catch (_: Exception) {}

            return DeviceMetadata(
                manufacturer = Build.MANUFACTURER ?: "Unknown",
                model = Build.MODEL ?: "Unknown",
                device = Build.DEVICE ?: "Unknown",
                androidVersion = Build.VERSION.RELEASE ?: "Unknown",
                sdkInt = Build.VERSION.SDK_INT,
                supportedAbis = Build.SUPPORTED_ABIS?.toList() ?: emptyList(),
                appVersionName = versionName,
                appVersionCode = versionCode,
                availableRamMb = availRam,
                totalRamMb = totalRam
            )
        }

        private fun getStackTraceString(throwable: Throwable): String {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            pw.flush()
            return sw.toString()
        }
    }
}
