package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.provider.WebDavStorageProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavPathExtractionTest {

    private val nextcloudConfig = ServiceConfig(
        id = "nc-1",
        name = "My Nextcloud",
        providerType = StorageProviderType.NEXTCLOUD,
        serverUrl = "https://cloud.example.org",
        username = "ilam",
        passwordOrSecret = "pwd"
    )

    private val provider = WebDavStorageProvider(nextcloudConfig)

    @Test
    fun testBaseUrlConstruction() {
        assertEquals("https://cloud.example.org/remote.php/dav/files/ilam/", provider.baseUrl)
    }

    @Test
    fun testExtractRelativePathNextcloudRootAndSubfolders() {
        val extractMethod = WebDavStorageProvider::class.java.getDeclaredMethod(
            "extractRelativePath",
            String::class.java,
            String::class.java
        ).apply { isAccessible = true }

        val baseUrl = provider.baseUrl

        // Self / Root href
        val rootPath = extractMethod.invoke(provider, "/remote.php/dav/files/ilam/", baseUrl) as String
        assertEquals("", rootPath)

        // Subfolder href with relative path
        val notesPath = extractMethod.invoke(provider, "/remote.php/dav/files/ilam/Notes/", baseUrl) as String
        assertEquals("Notes", notesPath)

        // Nested subfolder
        val biologyPath = extractMethod.invoke(provider, "/remote.php/dav/files/ilam/Notes/Biology/", baseUrl) as String
        assertEquals("Notes/Biology", biologyPath)

        // URL encoded subfolder
        val encodedPath = extractMethod.invoke(provider, "/remote.php/dav/files/ilam/Notes/My%20Folder%20Name/", baseUrl) as String
        assertEquals("Notes/My Folder Name", encodedPath)

        // Full URL in href
        val fullUrlPath = extractMethod.invoke(provider, "https://cloud.example.org/remote.php/dav/files/ilam/Notes/Chemistry/", baseUrl) as String
        assertEquals("Notes/Chemistry", fullUrlPath)
    }

    @Test
    fun testExtractRelativePathGenericWebDav() {
        val webdavConfig = ServiceConfig(
            id = "wd-1",
            name = "Generic WebDAV",
            providerType = StorageProviderType.WEBDAV,
            serverUrl = "https://dav.server.com/remote.php/webdav/",
            username = "user"
        )
        val wdProvider = WebDavStorageProvider(webdavConfig)

        val extractMethod = WebDavStorageProvider::class.java.getDeclaredMethod(
            "extractRelativePath",
            String::class.java,
            String::class.java
        ).apply { isAccessible = true }

        val baseUrl = wdProvider.baseUrl
        val rootPath = extractMethod.invoke(wdProvider, "/remote.php/webdav/", baseUrl) as String
        assertEquals("", rootPath)

        val docsPath = extractMethod.invoke(wdProvider, "/remote.php/webdav/Documents/", baseUrl) as String
        assertEquals("Documents", docsPath)
    }
}
