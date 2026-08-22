package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.system.Os
import android.util.Log
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
        const val CURRENT_BOOTSTRAP_VERSION = 1
    }

    fun isInstalled(): Boolean {
        val versionFile = File(env.rootDir, VERSION_FLAG)
        if (!versionFile.exists()) return false
        return try {
            val version = versionFile.readText().trim().toInt()
            version == CURRENT_BOOTSTRAP_VERSION
        } catch (e: Exception) {
            false
        }
    }

    fun clearInstallation() {
        try {
            val versionFile = File(env.rootDir, VERSION_FLAG)
            if (versionFile.exists()) {
                versionFile.delete()
            }
            // Clean up any accidental double nested usr/usr if it existed
            val nestedUsr = File(env.usrDir, "usr")
            if (nestedUsr.exists()) {
                nestedUsr.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed during clearInstallation", e)
        }
    }

    suspend fun installBootstrap(onProgress: (InstallProgress) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
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
                                    try {
                                        Os.symlink(entry.linkName, destFile.absolutePath)
                                    } catch (symlinkErr: Exception) {
                                        Log.w(TAG, "Could not create symlink ${destFile.name} -> ${entry.linkName}: ${symlinkErr.message}")
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
            
            val versionFile = File(env.rootDir, VERSION_FLAG)
            versionFile.writeText(CURRENT_BOOTSTRAP_VERSION.toString())
            Log.i(TAG, "Bootstrap extraction completed successfully.")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap installation failed", e)
            Result.failure(e)
        }
    }
}
