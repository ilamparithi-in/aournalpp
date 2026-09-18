package dev.ilamparithi.aournalpp.backup.provider

/**
 * Common path resolution utilities for remote cloud storage providers.
 */
object CloudPathUtils {

    /**
     * Resolves a relative or absolute path against a base remote path,
     * ensuring proper Unix path separator normalization and avoiding path duplication.
     *
     * @param remoteBasePath The root directory configured on the remote server (e.g. "Notes" or "/Notes/").
     * @param path The relative or absolute file path to resolve (e.g. "Folder/note.xopp").
     * @return Normalized path without leading/trailing slashes (e.g. "Notes/Folder/note.xopp").
     */
    @JvmStatic
    fun resolveRemotePath(remoteBasePath: String, path: String): String {
        val base = remoteBasePath.trim().trim('/')
        val cleanPath = path.trim().trim('/').replace('\\', '/')
        if (base.isEmpty()) return cleanPath
        if (cleanPath.isEmpty()) return base
        if (cleanPath == base || cleanPath.startsWith("$base/")) {
            return cleanPath
        }
        return "$base/$cleanPath"
    }
}
