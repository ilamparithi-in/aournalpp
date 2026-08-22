package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class BootstrapInstaller(private val context: Context, private val env: LinuxEnvironment) {
    companion object {
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

    suspend fun installBootstrap(onProgress: (String) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            env.ensureDirectoryTree()
            val assetManager = context.assets
            
            try {
                assetManager.open(ASSET_NAME).use { inputStream ->
                    XZInputStream(inputStream).use { xzIn ->
                        TarArchiveInputStream(xzIn).use { tarIn ->
                            var entry = tarIn.nextTarEntry
                            while (entry != null) {
                                val destFile = File(env.usrDir, entry.name)
                                if (entry.isDirectory) {
                                    destFile.mkdirs()
                                } else {
                                    destFile.parentFile?.mkdirs()
                                    FileOutputStream(destFile).use { out ->
                                        tarIn.copyTo(out)
                                    }
                                    
                                    val isBinDir = destFile.parentFile?.absolutePath == env.binDir.absolutePath
                                    // 73 is octal 0111 (execute bits for user, group, other)
                                    val isExecutable = (entry.mode and 73) != 0
                                    
                                    if (isBinDir || isExecutable) {
                                        destFile.setExecutable(true, false)
                                        destFile.setReadable(true, false)
                                    }
                                }
                                onProgress("Unpacking: ${entry.name}")
                                entry = tarIn.nextTarEntry
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                // Return failure if asset is missing before packaging stage
                return@withContext Result.failure(e)
            }
            
            val versionFile = File(env.rootDir, VERSION_FLAG)
            versionFile.writeText(CURRENT_BOOTSTRAP_VERSION.toString())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
