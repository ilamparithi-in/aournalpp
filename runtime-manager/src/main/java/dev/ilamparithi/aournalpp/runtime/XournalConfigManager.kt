package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class ConfigFileType(
    val fileName: String,
    val displayName: String,
    val mimeType: String,
    val description: String
) {
    SETTINGS_XML(
        fileName = "settings.xml",
        displayName = "Core Settings (settings.xml)",
        mimeType = "text/xml",
        description = "Application preferences, autosave, directories, pen & touch behavior"
    ),
    TOOLBAR_INI(
        fileName = "toolbar.ini",
        displayName = "Custom Toolbars (toolbar.ini)",
        mimeType = "text/plain",
        description = "Customized toolbar button arrangements and layouts"
    ),
    PALETTE_GPL(
        fileName = "palette.gpl",
        displayName = "Color Palette (palette.gpl)",
        mimeType = "text/plain",
        description = "GIMP color palette definitions for custom stroke colors"
    ),
    COLORNAMES_INI(
        fileName = "colornames.ini",
        displayName = "Color Names (colornames.ini)",
        mimeType = "text/plain",
        description = "Human-readable names mapped to custom palette colors"
    ),
    PRINT_SETTINGS_INI(
        fileName = "print-settings.ini",
        displayName = "Print & Export Setup (print-settings.ini)",
        mimeType = "text/plain",
        description = "Saved printer preferences, page setup, orientation, and PDF export defaults"
    )
}

class XournalConfigManager(private val env: LinuxEnvironment) {

    companion object {
        private const val TAG = "XournalConfigManager"
    }

    val configFile: File
        get() = File(env.xournalConfigDir, ConfigFileType.SETTINGS_XML.fileName)

    val toolbarFile: File
        get() = File(env.xournalConfigDir, ConfigFileType.TOOLBAR_INI.fileName)

    val paletteFile: File
        get() = File(env.xournalConfigDir, ConfigFileType.PALETTE_GPL.fileName)

    fun getConfigFile(type: ConfigFileType): File {
        return File(env.xournalConfigDir, type.fileName)
    }

    fun listAvailableConfigs(): List<ConfigFileType> {
        return ConfigFileType.values().toList()
    }

    fun readConfigText(type: ConfigFileType = ConfigFileType.SETTINGS_XML): Result<String> = runCatching {
        val target = getConfigFile(type)
        if (!target.exists() || target.length() == 0L) {
            when (type) {
                ConfigFileType.SETTINGS_XML ->
                    "<!-- settings.xml has not been generated yet. Launch Xournal++ once to create defaults. -->"
                ConfigFileType.TOOLBAR_INI ->
                    "# toolbar.ini has not been created yet.\n# Custom toolbars configured in Xournal++ (View -> Toolbars -> Customize) will appear here."
                ConfigFileType.PALETTE_GPL ->
                    "# palette.gpl has not been created yet.\n# Custom color palettes imported or saved in Xournal++ will appear here."
                ConfigFileType.COLORNAMES_INI ->
                    "# colornames.ini has not been created yet.\n# Custom color names will appear here."
                ConfigFileType.PRINT_SETTINGS_INI ->
                    "# print-settings.ini has not been created yet.\n# Saved print and PDF export options configured in Xournal++ will appear here."
            }
        } else {
            target.readText(Charsets.UTF_8)
        }
    }

    fun exportConfigFile(context: Context, type: ConfigFileType, destUri: Uri): Result<Unit> = runCatching {
        val target = getConfigFile(type)
        if (!target.exists()) {
            error("${type.fileName} does not exist yet")
        }
        context.contentResolver.openOutputStream(destUri)?.use { out ->
            target.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: error("Failed to open destination stream for export")
    }

    fun exportConfig(context: Context, destUri: Uri): Result<Unit> {
        return exportConfigFile(context, ConfigFileType.SETTINGS_XML, destUri)
    }

    fun exportFullBackupZip(context: Context, destUri: Uri): Result<Int> = runCatching {
        val configDir = env.xournalConfigDir
        if (!configDir.exists()) {
            error("Xournal++ configuration directory does not exist")
        }

        val filesToBackup = configDir.walkTopDown()
            .filter { it.isFile && !it.name.endsWith(".tmp") && !it.name.equals("emergencysave.xopp") }
            .toList()

        if (filesToBackup.isEmpty()) {
            error("No configuration files found to backup")
        }

        var exportedCount = 0
        context.contentResolver.openOutputStream(destUri)?.use { out ->
            ZipOutputStream(out).use { zipOut ->
                for (file in filesToBackup) {
                    val relativePath = file.relativeTo(configDir).path
                    val entry = ZipEntry(relativePath)
                    zipOut.putNextEntry(entry)
                    file.inputStream().use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    exportedCount++
                }
            }
        } ?: error("Failed to open output stream for ZIP backup")

        exportedCount
    }

    fun importConfigFile(
        context: Context,
        sourceUri: Uri,
        explicitType: ConfigFileType? = null
    ): Result<ConfigFileType> = runCatching {
        val rawContent = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            input.bufferedReader().readText()
        } ?: error("Unable to read source configuration file")

        if (rawContent.isBlank()) {
            error("Selected file is empty")
        }

        val detectedType = explicitType ?: detectConfigFileType(sourceUri, rawContent)
            ?: error("Unable to identify configuration file format. Please choose a valid .xml or .ini file.")

        // Basic structural validation (schema/semantic values are not validated by Android layer)
        when (detectedType) {
            ConfigFileType.SETTINGS_XML -> validateXmlStructure(rawContent)
            ConfigFileType.TOOLBAR_INI, ConfigFileType.COLORNAMES_INI, ConfigFileType.PRINT_SETTINGS_INI -> validateIniStructure(rawContent)
            ConfigFileType.PALETTE_GPL -> validateGplStructure(rawContent)
        }

        env.xournalConfigDir.mkdirs()
        val target = getConfigFile(detectedType)
        target.writeText(rawContent, Charsets.UTF_8)
        Log.i(TAG, "Successfully imported ${detectedType.fileName} (${rawContent.length} chars)")

        detectedType
    }

    fun importConfig(context: Context, sourceUri: Uri): Result<Unit> = runCatching {
        importConfigFile(context, sourceUri, ConfigFileType.SETTINGS_XML).getOrThrow()
        Unit
    }

    fun importFullBackupZip(context: Context, sourceUri: Uri): Result<Int> = runCatching {
        env.xournalConfigDir.mkdirs()
        var extractedFiles = 0

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(input).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val cleanName = File(entry.name).name
                        if (!cleanName.startsWith(".") && (cleanName.endsWith(".xml") || cleanName.endsWith(".ini") || cleanName.endsWith(".gpl") || cleanName.endsWith(".lua"))) {
                            val destFile = File(env.xournalConfigDir, cleanName)
                            FileOutputStream(destFile).use { out ->
                                zipIn.copyTo(out)
                            }
                            extractedFiles++
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        } ?: error("Failed to open input stream for ZIP restore")

        if (extractedFiles == 0) {
            error("No valid Xournal++ configuration files (.xml, .ini, .gpl) found in ZIP archive")
        }

        extractedFiles
    }

    private fun detectConfigFileType(uri: Uri, content: String): ConfigFileType? {
        val path = uri.path?.lowercase() ?: ""
        return when {
            path.endsWith("toolbar.ini") || (content.contains("[Toolbars]") || content.contains("[General]")) ->
                ConfigFileType.TOOLBAR_INI
            path.endsWith("palette.gpl") || content.startsWith("GIMP Palette") ->
                ConfigFileType.PALETTE_GPL
            path.endsWith("colornames.ini") ->
                ConfigFileType.COLORNAMES_INI
            path.endsWith("print-settings.ini") || content.contains("[Print Settings]") ->
                ConfigFileType.PRINT_SETTINGS_INI
            path.endsWith(".xml") || content.trimStart().startsWith("<?xml") || content.contains("<settings") ->
                ConfigFileType.SETTINGS_XML
            else -> null
        }
    }

    private fun validateXmlStructure(xmlContent: String) {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xmlContent))
        var eventType = parser.eventType
        var hasRoot = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                hasRoot = true
                break
            }
            eventType = parser.next()
        }

        if (!hasRoot) error("Invalid XML: No root element detected.")
    }

    private fun validateIniStructure(iniContent: String) {
        val nonCommentLines = iniContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith(";") }
            .toList()

        if (nonCommentLines.isEmpty()) {
            error("Configuration file contains no active settings or sections.")
        }
    }

    private fun validateGplStructure(gplContent: String) {
        if (!gplContent.trimStart().startsWith("GIMP Palette")) {
            error("Invalid palette file: Must start with 'GIMP Palette' header.")
        }
    }
}
