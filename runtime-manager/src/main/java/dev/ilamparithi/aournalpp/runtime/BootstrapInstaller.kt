package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.system.Os
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import android.os.StatFs
import android.os.SystemClock
import org.json.JSONObject

data class InstallProgress(
    val currentFile: String,
    val extractedBytes: Long,
    val percentage: Float
)

data class BootstrapPackageInfo(
    val name: String,
    val version: String,
    val installedSize: Long,
    val debSize: Long
)

data class BootstrapManifest(
    val manifestVersion: Int,
    val arch: String,
    val abi: String,
    val bootstrapSeries: String,
    val archiveCompressedBytes: Long,
    val archiveUncompressedBytes: Long,
    val archiveSha256: String,
    val generatedAt: String,
    val packages: Map<String, BootstrapPackageInfo>
) {
    companion object {
        fun parse(jsonString: String): BootstrapManifest {
            val root = JSONObject(jsonString)
            val pkgsObj = root.optJSONObject("packages") ?: JSONObject()
            val pkgMap = mutableMapOf<String, BootstrapPackageInfo>()
            val keys = pkgsObj.keys()
            while (keys.hasNext()) {
                val pkgName = keys.next()
                val p = pkgsObj.getJSONObject(pkgName)
                pkgMap[pkgName] = BootstrapPackageInfo(
                    name = pkgName,
                    version = p.optString("version", ""),
                    installedSize = p.optLong("installed_size", 0L),
                    debSize = p.optLong("deb_size", 0L)
                )
            }
            return BootstrapManifest(
                manifestVersion = root.optInt("manifest_version", 1),
                arch = root.optString("arch", ""),
                abi = root.optString("abi", ""),
                bootstrapSeries = root.optString("bootstrap_series", "bootstrap-v1"),
                archiveCompressedBytes = root.optLong("archive_compressed_bytes", 0L),
                archiveUncompressedBytes = root.optLong("archive_uncompressed_bytes", 0L),
                archiveSha256 = root.optString("archive_sha256", ""),
                generatedAt = root.optString("generated_at", ""),
                packages = pkgMap
            )
        }
    }
}

data class PackageChange(
    val name: String,
    val oldVersion: String?,
    val newVersion: String?
)

data class BootstrapDiff(
    val added: List<PackageChange>,
    val updated: List<PackageChange>,
    val removed: List<PackageChange>,
    val requiredSpaceBytes: Long,
    val availableSpaceBytes: Long,
    val hasSufficientSpace: Boolean
) {
    val totalChanges: Int get() = added.size + updated.size + removed.size
}

class InsufficientStorageException(
    val requiredBytes: Long,
    val availableBytes: Long
) : IOException(
    "Insufficient device storage: requires ${requiredBytes / (1024 * 1024)} MB, but only ${availableBytes / (1024 * 1024)} MB is available."
)

data class StorageCheckResult(
    val requiredBytes: Long,
    val availableBytes: Long,
    val totalBytes: Long,
    val isInsufficient: Boolean,
    val isLowStorageWarning: Boolean,
    val missingBytes: Long
)

class BootstrapInstaller(private val context: Context, private val env: LinuxEnvironment) {
    companion object {
        private const val TAG = "BootstrapInstaller"
        const val ASSET_NAME = "bootstrap.tar.xz"
        const val MANIFEST_NAME = "bootstrap_manifest.json"
        const val VERSION_FLAG = "bootstrap_installed.ver"
        const val SAFETY_BUFFER_BYTES = 50L * 1024L * 1024L // 50MB buffer for runtime operations
        private val installMutex = Mutex()
    }

    private fun purgeStaleDirectoriesAsync() {
        try {
            val staleDirs = env.rootDir.listFiles { file ->
                file.isDirectory && file.name.startsWith("usr_stale_")
            } ?: emptyArray()

            for (staleDir in staleDirs) {
                try {
                    staleDir.deleteRecursively()
                    Log.i(TAG, "Successfully purged stale directory: ${staleDir.name}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed purging stale directory: ${staleDir.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning for stale directories", e)
        }
    }

    private fun retireUsrDirForAsyncPurge() {
        if (env.usrDir.exists()) {
            val staleDir = File(env.rootDir, "usr_stale_${System.currentTimeMillis()}")
            if (env.usrDir.renameTo(staleDir)) {
                Log.i(TAG, "Atomically moved ${env.usrDir.name} to ${staleDir.name} for background purge")
            } else {
                Log.w(TAG, "Atomic rename failed, falling back to deleteRecursively()")
                env.usrDir.deleteRecursively()
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            purgeStaleDirectoriesAsync()
        }
    }

    fun isExtractionInProgress(): Boolean = installMutex.isLocked

    fun getCurrentAppVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(packageInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve app version code, fallback to 1", e)
            1L
        }
    }

    fun needsBootstrap(): Boolean {
        val versionFile = File(env.rootDir, VERSION_FLAG)
        if (!versionFile.exists()) return true
        return try {
            val installedVersion = versionFile.readText().trim().toLong()
            val currentVersion = getCurrentAppVersionCode()
            installedVersion < currentVersion
        } catch (e: Exception) {
            true
        }
    }

    fun getInstalledVersion(): Long? {
        val versionFile = File(env.rootDir, VERSION_FLAG)
        if (!versionFile.exists()) return null
        return try {
            versionFile.readText().trim().toLong()
        } catch (e: Exception) {
            null
        }
    }

    fun hasValidInstallation(): Boolean {
        val versionFile = File(env.rootDir, VERSION_FLAG)
        val xournalBin = env.resolveExecutable("xournalpp")
        return versionFile.exists() && xournalBin.exists() && xournalBin.canExecute()
    }

    fun isUpgradeAvailable(): Boolean {
        return hasValidInstallation() && needsBootstrap()
    }

    fun getInstalledManifest(): BootstrapManifest? {
        val file = File(env.rootDir, MANIFEST_NAME)
        if (!file.exists()) return null
        return try {
            BootstrapManifest.parse(file.readText())
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading installed manifest", e)
            null
        }
    }

    fun getIncomingManifest(): BootstrapManifest? {
        return try {
            context.assets.open(MANIFEST_NAME).bufferedReader().use {
                BootstrapManifest.parse(it.readText())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading incoming asset manifest", e)
            null
        }
    }

    fun getAvailableStorageBytes(): Long {
        return try {
            StatFs(env.rootDir.absolutePath).availableBytes
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving available storage bytes", e)
            Long.MAX_VALUE
        }
    }

    fun getTotalStorageBytes(): Long {
        return try {
            StatFs(env.rootDir.absolutePath).totalBytes
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving total storage bytes", e)
            0L
        }
    }

    fun checkStorageThresholds(isDynamicDownload: Boolean = false): StorageCheckResult {
        val manifest = getIncomingManifest()
        val requiredBytes = if (manifest != null) {
            computeRequiredStorageBytes(manifest, isDynamicDownload)
        } else {
            // 200MB uncompressed + 50MB safety fallback
            250L * 1024L * 1024L
        }
        val availableBytes = getAvailableStorageBytes()
        val totalBytes = getTotalStorageBytes()

        val isInsufficient = availableBytes < requiredBytes
        val missingBytes = if (isInsufficient) requiredBytes - availableBytes else 0L

        val projectedRemaining = availableBytes - requiredBytes
        val tenPercentTotal = if (totalBytes > 0L) (totalBytes * 0.10).toLong() else 512L * 1024L * 1024L
        val fiveTwelveMb = 512L * 1024L * 1024L
        val lowStorageThreshold = minOf(tenPercentTotal, fiveTwelveMb)

        val isLowStorageWarning = !isInsufficient && projectedRemaining < lowStorageThreshold

        return StorageCheckResult(
            requiredBytes = requiredBytes,
            availableBytes = availableBytes,
            totalBytes = totalBytes,
            isInsufficient = isInsufficient,
            isLowStorageWarning = isLowStorageWarning,
            missingBytes = missingBytes
        )
    }

    fun computeRequiredStorageBytes(manifest: BootstrapManifest, isDynamicDownload: Boolean): Long {
        val uncompressed = if (manifest.archiveUncompressedBytes > 0) {
            manifest.archiveUncompressedBytes
        } else {
            200L * 1024L * 1024L // 200MB fallback estimate
        }
        return if (isDynamicDownload) {
            manifest.archiveCompressedBytes + uncompressed + SAFETY_BUFFER_BYTES
        } else {
            uncompressed + SAFETY_BUFFER_BYTES
        }
    }

    fun computeDiff(isDynamicDownload: Boolean = false): BootstrapDiff? {
        val incoming = getIncomingManifest() ?: return null
        val installed = getInstalledManifest()

        val added = mutableListOf<PackageChange>()
        val updated = mutableListOf<PackageChange>()
        val removed = mutableListOf<PackageChange>()

        if (installed == null) {
            incoming.packages.values.forEach {
                added.add(PackageChange(it.name, null, it.version))
            }
        } else {
            for ((name, newPkg) in incoming.packages) {
                val oldPkg = installed.packages[name]
                if (oldPkg == null) {
                    added.add(PackageChange(name, null, newPkg.version))
                } else if (oldPkg.version != newPkg.version) {
                    updated.add(PackageChange(name, oldPkg.version, newPkg.version))
                }
            }
            for ((name, oldPkg) in installed.packages) {
                if (!incoming.packages.containsKey(name)) {
                    removed.add(PackageChange(name, oldPkg.version, null))
                }
            }
        }

        val available = getAvailableStorageBytes()
        val required = computeRequiredStorageBytes(incoming, isDynamicDownload)

        return BootstrapDiff(
            added = added.sortedBy { it.name },
            updated = updated.sortedBy { it.name },
            removed = removed.sortedBy { it.name },
            requiredSpaceBytes = required,
            availableSpaceBytes = available,
            hasSufficientSpace = available >= required
        )
    }

    fun isInstalled(): Boolean = !needsBootstrap()

    fun clearInstallation() {
        try {
            val versionFile = File(env.rootDir, VERSION_FLAG)
            if (versionFile.exists()) {
                versionFile.delete()
            }
            val manifestFile = File(env.rootDir, MANIFEST_NAME)
            if (manifestFile.exists()) {
                manifestFile.delete()
            }
            retireUsrDirForAsyncPurge()
        } catch (e: Exception) {
            Log.w(TAG, "Failed during clearInstallation", e)
        }
    }

    suspend fun installOrUpgrade(onProgress: (InstallProgress) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        if (!installMutex.tryLock()) {
            Log.w(TAG, "installOrUpgrade is already running in background. Waiting for active extraction to complete...")
            installMutex.withLock {
                return@withContext if (hasValidInstallation()) {
                    Log.i(TAG, "Active extraction completed successfully.")
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Active extraction failed or was incomplete"))
                }
            }
        }
        try {
            // 1. Storage Preflight Check: verify enough space exists before deleting or extracting
            val incomingManifest = getIncomingManifest()
            if (incomingManifest != null) {
                val requiredBytes = computeRequiredStorageBytes(incomingManifest, isDynamicDownload = false)
                val availableBytes = getAvailableStorageBytes()
                if (availableBytes < requiredBytes) {
                    Log.e(TAG, "Storage preflight check failed: required=$requiredBytes, available=$availableBytes")
                    return@withContext Result.failure(InsufficientStorageException(requiredBytes, availableBytes))
                }
                Log.i(TAG, "Storage preflight check passed: required=$requiredBytes, available=$availableBytes")
            }

            // Safely purge system-only userland (/files/usr) to eliminate stale/mismatched binaries
            // User data (/files/home and /files/home/.config) remains untouched
            retireUsrDirForAsyncPurge()

            env.ensureDirectoryTree()
            val assetManager = context.assets
            
            try {
                val totalCompressedBytes = try {
                    context.assets.openFd(ASSET_NAME).length
                } catch (e: Exception) {
                    context.assets.open(ASSET_NAME).use { it.available().toLong() }
                }

                val totalUncompressedTargetBytes = incomingManifest?.archiveUncompressedBytes ?: 0L
                var compressedBytesRead = 0L
                var extractedBytes = 0L
                var lastProgressEmit = 0L
                val chunkBuffer = ByteArray(64 * 1024)

                val rawInputStream = assetManager.open(ASSET_NAME)
                val countingInputStream = object : java.io.FilterInputStream(rawInputStream) {
                    override fun read(): Int {
                        val b = super.read()
                        if (b != -1) compressedBytesRead++
                        return b
                    }
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        val n = super.read(b, off, len)
                        if (n > 0) compressedBytesRead += n
                        return n
                    }
                }

                countingInputStream.use { inputStream ->
                    XZInputStream(inputStream).use { xzIn ->
                        TarArchiveInputStream(xzIn).use { tarIn ->
                            var entry = tarIn.nextTarEntry
                            while (entry != null) {
                                // Archive root entries start with "usr/" or might be relative
                                val destFile = if (entry.name.startsWith("usr/")) {
                                    File(env.rootDir, entry.name)
                                } else if (entry.name == "usr") {
                                    env.usrDir
                                } else {
                                    File(env.usrDir, entry.name)
                                }

                                if (entry.isDirectory) {
                                    destFile.mkdirs()
                                } else if (entry.isSymbolicLink) {
                                    destFile.parentFile?.mkdirs()
                                    if (destFile.exists()) {
                                        destFile.delete()
                                    }
                                    val rawTarget = entry.linkName
                                    val linkTarget = if (rawTarget.startsWith("/data/data/com.termux/files/")) {
                                        rawTarget.replace("/data/data/com.termux/files", env.rootDir.absolutePath)
                                    } else if (rawTarget.startsWith("/data/user/0/com.termux/files/")) {
                                        rawTarget.replace("/data/user/0/com.termux/files", env.rootDir.absolutePath)
                                    } else {
                                        rawTarget
                                    }
                                    try {
                                        Os.symlink(linkTarget, destFile.absolutePath)
                                    } catch (symlinkErr: Exception) {
                                        Log.w(TAG, "Could not create symlink ${destFile.name} -> $linkTarget: ${symlinkErr.message}")
                                    }
                                } else {
                                    destFile.parentFile?.mkdirs()
                                    FileOutputStream(destFile).use { out ->
                                        var nRead = tarIn.read(chunkBuffer)
                                        while (nRead != -1) {
                                            out.write(chunkBuffer, 0, nRead)
                                            extractedBytes += nRead
                                            val now = SystemClock.uptimeMillis()
                                            // Throttle progress emissions to ~60 FPS (every 16ms)
                                            if (now - lastProgressEmit >= 16) {
                                                val pct = if (totalUncompressedTargetBytes > 0) {
                                                    (extractedBytes.toFloat() / totalUncompressedTargetBytes) * 100f
                                                } else if (totalCompressedBytes > 0) {
                                                    (compressedBytesRead.toFloat() / totalCompressedBytes) * 100f
                                                } else 0f

                                                onProgress(InstallProgress(
                                                    currentFile = entry.name,
                                                    extractedBytes = extractedBytes,
                                                    percentage = pct.coerceIn(0f, 100f)
                                                ))
                                                lastProgressEmit = now
                                            }
                                            nRead = tarIn.read(chunkBuffer)
                                        }
                                    }
                                    
                                    val isInBinDir = destFile.absolutePath.startsWith(env.binDir.absolutePath)
                                    val isLibrary = destFile.name.endsWith(".so") || destFile.name.contains(".so.")
                                    // 73 is octal 0111 (execute bits for user, group, other)
                                    val isExecutable = (entry.mode and 73) != 0
                                    
                                    if (isInBinDir || isLibrary || isExecutable) {
                                        destFile.setExecutable(true, false)
                                        destFile.setReadable(true, false)
                                    }
                                }
                                
                                val finalPct = if (totalUncompressedTargetBytes > 0) {
                                    (extractedBytes.toFloat() / totalUncompressedTargetBytes) * 100f
                                } else if (totalCompressedBytes > 0) {
                                    (compressedBytesRead.toFloat() / totalCompressedBytes) * 100f
                                } else 0f
                                
                                onProgress(InstallProgress(
                                    currentFile = entry.name,
                                    extractedBytes = extractedBytes,
                                    percentage = finalPct.coerceIn(0f, 100f)
                                ))
                                
                                entry = tarIn.nextTarEntry
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Extraction failed due to IO error", e)
                return@withContext Result.failure(e)
            }
            
            // Ensure GSettings schemas are compiled for GTK file chooser and desktop settings
            val schemasDir = File(env.shareDir, "glib-2.0/schemas")
            val compileSchemasBin = env.resolveExecutable("glib-compile-schemas")
            if (compileSchemasBin.exists() && schemasDir.exists()) {
                try {
                    val pb = ProcessBuilder(compileSchemasBin.absolutePath, schemasDir.absolutePath)
                        .redirectErrorStream(true)
                    pb.environment().putAll(env.getEnvMap())
                    val p = pb.start()
                    p.waitFor()
                    Log.i(TAG, "Compiled glib schemas successfully in ${schemasDir.absolutePath}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to compile glib schemas", e)
                }
            }

            // Ensure gdk-pixbuf loaders.cache is generated for icon rendering
            val gdkDir = File(env.libDir, "gdk-pixbuf-2.0/2.10.0")
            val loadersCache = File(gdkDir, "loaders.cache")
            val loadersDir = File(gdkDir, "loaders")
            val queryLoadersBin = env.resolveExecutable("gdk-pixbuf-query-loaders")
            if (queryLoadersBin.exists() && loadersDir.exists()) {
                try {
                    val loaderFiles = loadersDir.listFiles { _, name -> name.endsWith(".so") }
                    if (!loaderFiles.isNullOrEmpty()) {
                        val cmd = mutableListOf(queryLoadersBin.absolutePath)
                        loaderFiles.forEach { cmd.add(it.absolutePath) }
                        val pb = ProcessBuilder(cmd)
                            .redirectOutput(loadersCache)
                        pb.environment().putAll(env.getEnvMap())
                        val p = pb.start()
                        p.waitFor()
                        Log.i(TAG, "Generated gdk-pixbuf loaders.cache successfully")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to run gdk-pixbuf-query-loaders", e)
                }
            }

            val currentVersion = getCurrentAppVersionCode()
            val versionFile = File(env.rootDir, VERSION_FLAG)
            versionFile.writeText(currentVersion.toString())
            try {
                context.assets.open(MANIFEST_NAME).use { input ->
                    File(env.rootDir, MANIFEST_NAME).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to copy $MANIFEST_NAME to rootDir", e)
            }
            Log.i(TAG, "Bootstrap installation/upgrade completed successfully. Stamped version: $currentVersion")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap installation/upgrade failed", e)
            Result.failure(e)
        } finally {
            installMutex.unlock()
        }
    }

    suspend fun installBootstrap(onProgress: (InstallProgress) -> Unit): Result<Unit> = installOrUpgrade(onProgress)
}
