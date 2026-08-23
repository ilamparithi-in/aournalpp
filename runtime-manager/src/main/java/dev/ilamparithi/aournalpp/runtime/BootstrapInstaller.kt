package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.system.Os
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class InstallProgress(
    val currentFile: String,
    val extractedBytes: Long,
    val percentage: Float
)

class BootstrapInstaller(private val context: Context, private val env: LinuxEnvironment) {
    companion object {
        private const val TAG = "BootstrapInstaller"
        const val ASSET_NAME = "bootstrap.tar.xz"
        const val VERSION_FLAG = "bootstrap_installed.ver"
    }

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

    fun isInstalled(): Boolean = !needsBootstrap()

    fun clearInstallation() {
        try {
            val versionFile = File(env.rootDir, VERSION_FLAG)
            if (versionFile.exists()) {
                versionFile.delete()
            }
            if (env.usrDir.exists()) {
                env.usrDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed during clearInstallation", e)
        }
    }

    suspend fun installOrUpgrade(onProgress: (InstallProgress) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Safely purge system-only userland (/files/usr) to eliminate stale/mismatched binaries
            // User data (/files/home and /files/home/.config) remains untouched
            if (env.usrDir.exists()) {
                Log.i(TAG, "Purging ${env.usrDir.absolutePath} for isolated upgrade...")
                env.usrDir.deleteRecursively()
            }

            env.ensureDirectoryTree()
            val assetManager = context.assets
            
            try {
                val totalCompressedBytes = try {
                    context.assets.openFd(ASSET_NAME).length
                } catch (e: Exception) {
                    // Fallback to available if openFd fails
                    context.assets.open(ASSET_NAME).use { it.available().toLong() }
                }

                var compressedBytesRead = 0L
                var extractedBytes = 0L

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
                                        val copied = tarIn.copyTo(out)
                                        extractedBytes += copied
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
                                
                                val progressPercent = if (totalCompressedBytes > 0) {
                                    (compressedBytesRead.toFloat() / totalCompressedBytes) * 100f
                                } else {
                                    0f
                                }
                                
                                onProgress(InstallProgress(
                                    currentFile = entry.name,
                                    extractedBytes = extractedBytes,
                                    percentage = progressPercent.coerceIn(0f, 100f)
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
            Log.i(TAG, "Bootstrap installation/upgrade completed successfully. Stamped version: $currentVersion")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap installation/upgrade failed", e)
            Result.failure(e)
        }
    }

    suspend fun installBootstrap(onProgress: (InstallProgress) -> Unit): Result<Unit> = installOrUpgrade(onProgress)
}
