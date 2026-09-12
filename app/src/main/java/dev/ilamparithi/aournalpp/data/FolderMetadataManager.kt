package dev.ilamparithi.aournalpp.data

import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Metadata associated with a notes folder stored in `.folder.json`.
 */
data class FolderMetaData(
    val colorHex: String? = null,
    val iconEmoji: String? = null,
    val iconType: String? = null,
    val role: String? = null,
    val excludeFromRecents: Boolean = false
)

/**
 * Manages `.folder.json` parsing, caching, and persistence.
 */
class FolderMetadataManager(
    private val folderMetaCache: ConcurrentHashMap<String, Pair<Long, FolderMetaData>>,
    private val isEmergencySavesFolder: (File) -> Boolean,
    private val getImportedCanonical: () -> String,
    private val getAudioCanonical: () -> String,
    private val onInvalidateCaches: () -> Unit
) {
    companion object {
        const val FOLDER_META_FILE = ".folder.json"
        const val EMERGENCY_SAVES_DEFAULT_COLOR = "#E06C75"
        const val EMERGENCY_SAVES_DEFAULT_ICON = "emergency"
    }

    private fun canonicalOf(file: File): String =
        try { file.canonicalPath } catch (e: Exception) { file.absolutePath }

    fun getFolderMeta(folderDir: File): FolderMetaData {
        return readFolderMeta(folderDir)
    }

    fun readFolderMeta(folderDir: File): FolderMetaData {
        val metaFile = File(folderDir, FOLDER_META_FILE)
        val metaLastModified = if (metaFile.exists()) metaFile.lastModified() else -1L
        val cacheKey = folderDir.absolutePath
        val cached = folderMetaCache[cacheKey]
        if (cached != null && cached.first == metaLastModified) {
            return cached.second
        }

        val dirName = folderDir.name
        var detectedRole: String? = null
        var defaultColor: String? = null
        var defaultIcon: String? = null

        when {
            dirName.equals("Emergency Saves", ignoreCase = true) || isEmergencySavesFolder(folderDir) -> {
                detectedRole = "emergency"
                defaultColor = EMERGENCY_SAVES_DEFAULT_COLOR
                defaultIcon = EMERGENCY_SAVES_DEFAULT_ICON
            }
            dirName.equals("Imported", ignoreCase = true) ||
                (try { canonicalOf(folderDir) == getImportedCanonical() } catch (_: Exception) { false }) -> {
                detectedRole = "import"
                defaultIcon = "import"
            }
            dirName.equals("Audio", ignoreCase = true) ||
                (try { canonicalOf(folderDir) == getAudioCanonical() } catch (_: Exception) { false }) -> {
                detectedRole = "audio"
                defaultIcon = "audio"
            }
        }

        if (!metaFile.exists()) {
            val meta = FolderMetaData(
                colorHex = defaultColor,
                iconEmoji = null,
                iconType = defaultIcon,
                role = detectedRole,
                excludeFromRecents = false
            )
            folderMetaCache[cacheKey] = Pair(metaLastModified, meta)
            return meta
        }

        val resultMeta = try {
            val json = JSONObject(metaFile.readText())
            val role = if (json.has("role")) {
                json.optString("role").takeIf { it.isNotBlank() } ?: detectedRole
            } else {
                detectedRole
            }

            val color = if (json.has("color")) {
                json.optString("color").takeIf { it.isNotBlank() }
            } else {
                defaultColor
            }

            val emoji = if (json.has("emoji")) {
                json.optString("emoji").takeIf { it.isNotBlank() }
            } else {
                null
            }

            val icon = if (json.has("icon")) {
                json.optString("icon").takeIf { it.isNotBlank() }
            } else if (emoji == null) {
                defaultIcon
            } else {
                null
            }

            val excludeFromRecents = json.optBoolean("excludeFromRecents", false) || json.optBoolean("exclude_from_recents", false)

            FolderMetaData(color, emoji, icon, role, excludeFromRecents)
        } catch (e: Exception) {
            FolderMetaData(
                colorHex = defaultColor,
                iconEmoji = null,
                iconType = defaultIcon,
                role = detectedRole,
                excludeFromRecents = false
            )
        }

        folderMetaCache[cacheKey] = Pair(metaLastModified, resultMeta)
        return resultMeta
    }

    fun writeFolderMeta(
        folderDir: File,
        colorHex: String?,
        iconEmoji: String?,
        iconType: String? = null,
        role: String? = null,
        excludeFromRecents: Boolean? = null
    ): Result<Unit> = runCatching {
        val metaFile = File(folderDir, FOLDER_META_FILE)
        val json = if (metaFile.exists()) {
            try { JSONObject(metaFile.readText()) } catch (e: Exception) { JSONObject() }
        } else {
            JSONObject()
        }

        if (!colorHex.isNullOrBlank()) {
            json.put("color", colorHex)
        } else {
            json.remove("color")
        }

        if (!iconEmoji.isNullOrBlank()) {
            json.put("emoji", iconEmoji.trim())
            json.remove("icon")
        } else {
            json.remove("emoji")
            if (!iconType.isNullOrBlank()) {
                json.put("icon", iconType.trim())
            } else {
                json.remove("icon")
            }
        }

        if (!role.isNullOrBlank()) {
            json.put("role", role.trim())
        }

        if (excludeFromRecents != null) {
            if (excludeFromRecents) {
                json.put("excludeFromRecents", true)
            } else {
                json.remove("excludeFromRecents")
                json.remove("exclude_from_recents")
            }
        }

        metaFile.writeText(json.toString(2))
        folderMetaCache.remove(folderDir.absolutePath)
        onInvalidateCaches()
    }

    fun setFolderColor(folderDir: File, colorHex: String?): Result<Unit> {
        val meta = readFolderMeta(folderDir)
        return writeFolderMeta(folderDir, colorHex, meta.iconEmoji, meta.iconType, meta.role, meta.excludeFromRecents)
    }

    fun setFolderEmoji(folderDir: File, emoji: String?): Result<Unit> {
        val meta = readFolderMeta(folderDir)
        return writeFolderMeta(folderDir, meta.colorHex, emoji, if (emoji == null) (meta.iconType ?: "folder") else null, meta.role, meta.excludeFromRecents)
    }

    fun setFolderIcon(folderDir: File, iconType: String?): Result<Unit> {
        val meta = readFolderMeta(folderDir)
        return writeFolderMeta(folderDir, meta.colorHex, null, iconType, meta.role, meta.excludeFromRecents)
    }

    fun setFolderExcludeFromRecents(folderDir: File, exclude: Boolean): Result<Unit> {
        val meta = readFolderMeta(folderDir)
        return writeFolderMeta(folderDir, meta.colorHex, meta.iconEmoji, meta.iconType, meta.role, exclude)
    }

    fun updateFolderMeta(
        folderDir: File,
        colorHex: String?,
        iconEmoji: String?,
        iconType: String? = null,
        role: String? = null,
        excludeFromRecents: Boolean? = null
    ): Result<Unit> {
        val existing = readFolderMeta(folderDir)
        val existingRole = role ?: existing.role
        val existingExclude = excludeFromRecents ?: existing.excludeFromRecents
        return writeFolderMeta(folderDir, colorHex, iconEmoji, iconType, existingRole, existingExclude)
    }
}
