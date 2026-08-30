package dev.ilamparithi.aournalpp.backup.provider

import android.util.Log
import android.util.Xml
import dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.buffer
import okio.sink
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Storage provider for Nextcloud and Generic WebDAV servers using HTTP WebDAV extensions over OkHttp.
 */
class WebDavStorageProvider(
    private val config: ServiceConfig
) : CloudStorageProvider {

    companion object {
        private const val TAG = "WebDavStorageProvider"
    }

    override val providerType: StorageProviderType = config.providerType

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    internal val baseUrl: String = buildBaseUrl()

    private fun buildBaseUrl(): String {
        var url = config.serverUrl.trim()
        if (url.isEmpty()) {
            val scheme = if (config.port == 443 || config.port == 8443) "https" else "http"
            url = "$scheme://${config.host}:${config.port}"
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (providerType == StorageProviderType.NEXTCLOUD && !url.contains("/remote.php/dav/files/")) {
            val cleanBase = url.trimEnd('/')
            val user = config.username.trim()
            if (user.isNotEmpty() && !cleanBase.endsWith("/remote.php/webdav")) {
                url = "$cleanBase/remote.php/dav/files/$user"
            }
        }
        return url.trimEnd('/') + "/"
    }

    private fun addAuth(builder: Request.Builder): Request.Builder {
        if (config.authToken.isNotBlank()) {
            builder.header("Authorization", "Bearer ${config.authToken.trim()}")
        } else if (config.username.isNotBlank()) {
            builder.header("Authorization", Credentials.basic(config.username.trim(), config.passwordOrSecret))
        }
        return builder
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val request = addAuth(
                Request.Builder()
                    .url(baseUrl)
                    .method("PROPFIND", RequestBody.create("text/xml".toMediaTypeOrNull(), ""))
                    .header("Depth", "0")
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 207 || response.code == 404) {
                    true
                } else {
                    error("Connection failed with HTTP ${response.code}: ${response.message}")
                }
            }
        }
    }

    override suspend fun listFiles(remoteDirectory: String): Result<List<RemoteFileMetadata>> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = resolveUrl(remoteDirectory)
            val propfindXml = """
                <?xml version="1.0" encoding="utf-8" ?>
                <d:propfind xmlns:d="DAV:">
                  <d:prop>
                    <d:displayname/>
                    <d:getcontentlength/>
                    <d:getlastmodified/>
                    <d:resourcetype/>
                    <d:getetag/>
                  </d:prop>
                </d:propfind>
            """.trimIndent()

            val request = addAuth(
                Request.Builder()
                    .url(targetUrl)
                    .method("PROPFIND", RequestBody.create("application/xml; charset=utf-8".toMediaTypeOrNull(), propfindXml))
                    .header("Depth", "1")
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@runCatching emptyList()
                }
                if (!response.isSuccessful && response.code != 207) {
                    error("PROPFIND failed with HTTP ${response.code}: ${response.message}")
                }
                val bodyString = response.body?.string() ?: ""
                parseWebDavMultiStatus(bodyString, targetUrl)
            }
        }
    }

    override suspend fun createDirectory(remoteDirectory: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanDir = remoteDirectory.trim('/').replace('\\', '/')
            if (cleanDir.isEmpty()) return@runCatching

            val parts = cleanDir.split('/')
            var currentPath = ""
            for (part in parts) {
                if (part.isEmpty()) continue
                currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                val dirUrl = resolveUrl(currentPath).trimEnd('/') + "/"

                val mkcolRequest = addAuth(
                    Request.Builder()
                        .url(dirUrl)
                        .method("MKCOL", null)
                ).build()

                httpClient.newCall(mkcolRequest).execute().use { response ->
                    if (response.isSuccessful || response.code == 201 || response.code == 405 || response.code == 409) {
                        // 201 Created, 405 Method Not Allowed (already exists), 409 already exists
                    } else {
                        Log.d(TAG, "MKCOL for $dirUrl returned ${response.code}")
                    }
                }
            }
        }
    }

    override suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val parentDir = File(remotePath).parent?.replace('\\', '/')
            if (!parentDir.isNullOrEmpty()) {
                createDirectory(parentDir).getOrThrow()
            }

            val targetUrl = resolveUrl(remotePath)
            val totalBytes = localFile.length()
            val requestBody = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
                override fun contentLength() = totalBytes

                override fun writeTo(sink: BufferedSink) {
                    localFile.inputStream().use { source ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var uploaded = 0L
                        while (source.read(buffer).also { bytesRead = it } != -1) {
                            sink.write(buffer, 0, bytesRead)
                            uploaded += bytesRead
                            onProgress(uploaded, totalBytes)
                        }
                    }
                }
            }

            val request = addAuth(
                Request.Builder()
                    .url(targetUrl)
                    .put(requestBody)
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 200 && response.code != 201 && response.code != 204) {
                    error("Upload failed with HTTP ${response.code}: ${response.message}")
                }
            }
        }
    }

    override suspend fun downloadFile(
        remotePath: String,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            destinationFile.parentFile?.mkdirs()
            val targetUrl = resolveUrl(remotePath)
            val request = addAuth(
                Request.Builder()
                    .url(targetUrl)
                    .get()
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Download failed with HTTP ${response.code}: ${response.message}")
                }
                val body = response.body ?: error("Empty response body")
                val totalBytes = body.contentLength()
                val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.download.tmp")

                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            onProgress(downloaded, if (totalBytes > 0) totalBytes else downloaded)
                        }
                    }
                }

                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                tempFile.renameTo(destinationFile)
                Unit
            }
        }
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = resolveUrl(remotePath)
            val request = addAuth(
                Request.Builder()
                    .url(targetUrl)
                    .delete()
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404 && response.code != 204) {
                    error("Delete failed with HTTP ${response.code}: ${response.message}")
                }
            }
        }
    }

    override suspend fun disconnect() {
        // OkHttp handles connection pool eviction automatically
    }

    private fun resolveUrl(relativePath: String): String {
        val cleanPath = relativePath.trim().trim('/').replace('\\', '/')
        if (cleanPath.isEmpty()) return baseUrl
        val encodedSegments = cleanPath.split('/').map { segment ->
            try {
                java.net.URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
            } catch (_: Exception) {
                segment
            }
        }
        return "$baseUrl${encodedSegments.joinToString("/")}"
    }

    private fun extractRelativePath(href: String, baseUrl: String): String {
        val decodedHref = try {
            java.net.URLDecoder.decode(href, java.nio.charset.StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            href
        }
        val decodedBase = try {
            java.net.URLDecoder.decode(baseUrl, java.nio.charset.StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            baseUrl
        }

        // 1. Extract path component of baseUrl (e.g. "/remote.php/dav/files/ilam")
        val basePath = try {
            val uri = java.net.URI(decodedBase)
            uri.path?.trimEnd('/') ?: ""
        } catch (_: Exception) {
            decodedBase.substringAfter("://").substringAfter('/', "").let { if (it.isNotEmpty()) "/$it" else "" }.trimEnd('/')
        }

        // 2. Extract path component of href (e.g. "/remote.php/dav/files/ilam/Notes")
        val hrefPath = try {
            if (decodedHref.startsWith("http://", ignoreCase = true) || decodedHref.startsWith("https://", ignoreCase = true)) {
                java.net.URI(decodedHref).path?.trimEnd('/') ?: ""
            } else {
                decodedHref.trimEnd('/')
            }
        } catch (_: Exception) {
            decodedHref.removePrefix("http://").removePrefix("https://").substringAfter('/', "").let { if (it.isNotEmpty()) "/$it" else "" }.trimEnd('/')
        }

        // 3. Strip basePath prefix from hrefPath
        return if (basePath.isNotEmpty() && hrefPath.startsWith(basePath)) {
            hrefPath.removePrefix(basePath).trim('/')
        } else {
            val fallbackIndex = hrefPath.indexOf("/remote.php/")
            if (fallbackIndex != -1) {
                val afterRemote = hrefPath.substring(fallbackIndex)
                if (basePath.isNotEmpty() && afterRemote.startsWith(basePath)) {
                    afterRemote.removePrefix(basePath).trim('/')
                } else {
                    val userFilesPrefix = basePath.ifEmpty { "/remote.php/dav/files/${config.username.trim()}" }
                    if (afterRemote.startsWith(userFilesPrefix)) {
                        afterRemote.removePrefix(userFilesPrefix).trim('/')
                    } else {
                        hrefPath.trim('/')
                    }
                }
            } else {
                hrefPath.trim('/')
            }
        }
    }

    private fun parseWebDavMultiStatus(xmlContent: String, requestUrl: String): List<RemoteFileMetadata> {
        val results = mutableListOf<RemoteFileMetadata>()
        if (xmlContent.isBlank()) return results

        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var inResponse = false
            var currentHref = ""
            var contentLength = 0L
            var lastModifiedEpoch = 0L
            var isDirectory = false
            var etag: String? = null

            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }

            val reqRelative = extractRelativePath(requestUrl, baseUrl)

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name?.lowercase() ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "response" -> {
                                inResponse = true
                                currentHref = ""
                                contentLength = 0L
                                lastModifiedEpoch = 0L
                                isDirectory = false
                                etag = null
                            }
                            "href" -> if (inResponse) currentHref = parser.nextText().trim()
                            "getcontentlength" -> if (inResponse) contentLength = parser.nextText().toLongOrNull() ?: 0L
                            "getlastmodified" -> if (inResponse) {
                                val text = parser.nextText().trim()
                                lastModifiedEpoch = try {
                                    dateFormat.parse(text)?.time ?: 0L
                                } catch (e: Exception) { 0L }
                            }
                            "collection" -> if (inResponse) isDirectory = true
                            "getetag" -> if (inResponse) etag = parser.nextText().trim().removeSurrounding("\"")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag == "response" && inResponse) {
                            inResponse = false
                            if (currentHref.isNotEmpty()) {
                                val pathSegment = extractRelativePath(currentHref, baseUrl)
                                val isSelf = pathSegment.isEmpty() || pathSegment.equals(reqRelative, ignoreCase = true)

                                if (!isSelf) {
                                    results.add(
                                        RemoteFileMetadata(
                                            remotePath = pathSegment,
                                            isDirectory = isDirectory || currentHref.endsWith("/"),
                                            sizeBytes = contentLength,
                                            lastModifiedEpochMs = lastModifiedEpoch,
                                            contentHash = etag
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse WebDAV multi-status XML", e)
        }
        return results
    }
}
