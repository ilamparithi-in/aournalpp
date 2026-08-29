package dev.ilamparithi.aournalpp.backup.provider

import android.util.Log
import dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Storage provider for Google Drive using Google Drive REST API v3 over OkHttp.
 * Fully compatible with FOSS, microG, and de-Googled devices without requiring Google Play Services.
 */
class GoogleDriveProvider(
    private val config: ServiceConfig
) : CloudStorageProvider {

    companion object {
        private const val TAG = "GoogleDriveProvider"
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
    }

    override val providerType: StorageProviderType = StorageProviderType.GOOGLE_DRIVE

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Cache of remotePath -> folderId
    private val folderIdCache = ConcurrentHashMap<String, String>()

    private val token: String
        get() = config.authToken.ifBlank { config.passwordOrSecret }.trim()

    private fun addAuth(builder: Request.Builder): Request.Builder {
        val t = token
        if (t.isNotEmpty()) {
            builder.header("Authorization", "Bearer $t")
        }
        return builder
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (token.isEmpty()) {
                error("Google Drive OAuth2 Access Token is missing. Please authorize or configure token.")
            }
            val request = addAuth(
                Request.Builder()
                    .url("$DRIVE_API_BASE/about?fields=user(displayName,emailAddress)")
                    .get()
            ).build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    true
                } else {
                    error("Google Drive connection test failed: HTTP ${response.code} ${response.message}")
                }
            }
        }
    }

    override suspend fun listFiles(remoteDirectory: String): Result<List<RemoteFileMetadata>> = withContext(Dispatchers.IO) {
        runCatching {
            val folderId = resolveFolderId(remoteDirectory, createIfMissing = false) ?: return@runCatching emptyList()
            val query = "'$folderId' in parents and trashed = false"
            val url = "$DRIVE_API_BASE/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,mimeType,size,modifiedTime,md5Checksum)&pageSize=1000"

            val request = addAuth(Request.Builder().url(url).get()).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Failed to list files: HTTP ${response.code} ${response.message}")
                }
                val body = response.body?.string() ?: "{}"
                val json = JSONObject(body)
                val filesArray = json.optJSONArray("files") ?: return@runCatching emptyList()

                val list = mutableListOf<RemoteFileMetadata>()
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val isoFallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }

                val cleanDir = remoteDirectory.trim('/').replace('\\', '/')

                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.getJSONObject(i)
                    val id = fileObj.getString("id")
                    val name = fileObj.getString("name")
                    val mimeType = fileObj.optString("mimeType", "")
                    val isDir = mimeType == FOLDER_MIME_TYPE
                    val size = fileObj.optLong("size", 0L)
                    val modifiedStr = fileObj.optString("modifiedTime", "")
                    val md5 = fileObj.optString("md5Checksum", "").ifEmpty { null }

                    val modEpoch = try {
                        isoFormat.parse(modifiedStr)?.time ?: isoFallback.parse(modifiedStr)?.time ?: 0L
                    } catch (e: Exception) { 0L }

                    val itemPath = if (cleanDir.isEmpty()) name else "$cleanDir/$name"
                    if (isDir) {
                        folderIdCache[itemPath] = id
                    }

                    list.add(
                        RemoteFileMetadata(
                            remotePath = itemPath,
                            isDirectory = isDir,
                            sizeBytes = size,
                            lastModifiedEpochMs = modEpoch,
                            contentHash = md5
                        )
                    )
                }
                list
            }
        }
    }

    override suspend fun createDirectory(remoteDirectory: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            resolveFolderId(remoteDirectory, createIfMissing = true)
            Unit
        }
    }

    override suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanPath = remotePath.trim('/').replace('\\', '/')
            val fileName = File(cleanPath).name
            val parentPath = File(cleanPath).parent?.replace('\\', '/') ?: ""

            val parentFolderId = if (parentPath.isNotEmpty()) {
                resolveFolderId(parentPath, createIfMissing = true) ?: "root"
            } else {
                "root"
            }

            // Check if file with same name already exists in folder
            val existingFileId = findFileIdInFolder(parentFolderId, fileName)
            val totalBytes = localFile.length()

            val fileProgressBody = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
                override fun contentLength() = totalBytes

                override fun writeTo(sink: BufferedSink) {
                    localFile.inputStream().use { input ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var uploaded = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            sink.write(buffer, 0, bytesRead)
                            uploaded += bytesRead
                            onProgress(uploaded, totalBytes)
                        }
                    }
                }
            }

            if (existingFileId != null) {
                // Update content
                val uploadUrl = "$DRIVE_UPLOAD_BASE/files/$existingFileId?uploadType=media"
                val request = addAuth(Request.Builder().url(uploadUrl).patch(fileProgressBody)).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Google Drive update failed: HTTP ${response.code} ${response.message}")
                    }
                }
            } else {
                // Multipart Create
                val metadataJson = JSONObject()
                    .put("name", fileName)
                    .put("parents", org.json.JSONArray().put(parentFolderId))
                    .toString()

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("metadata", null, RequestBody.create("application/json; charset=UTF-8".toMediaTypeOrNull(), metadataJson))
                    .addFormDataPart("file", fileName, fileProgressBody)
                    .build()

                val uploadUrl = "$DRIVE_UPLOAD_BASE/files?uploadType=multipart"
                val request = addAuth(Request.Builder().url(uploadUrl).post(multipartBody)).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 200 && response.code != 201) {
                        error("Google Drive upload failed: HTTP ${response.code} ${response.message}")
                    }
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
            val cleanPath = remotePath.trim('/').replace('\\', '/')
            val fileName = File(cleanPath).name
            val parentPath = File(cleanPath).parent?.replace('\\', '/') ?: ""

            val parentFolderId = if (parentPath.isNotEmpty()) {
                resolveFolderId(parentPath, createIfMissing = false) ?: error("Parent folder not found for $remotePath")
            } else {
                "root"
            }

            val fileId = findFileIdInFolder(parentFolderId, fileName) ?: error("File not found on Google Drive: $remotePath")
            val downloadUrl = "$DRIVE_API_BASE/files/$fileId?alt=media"
            val request = addAuth(Request.Builder().url(downloadUrl).get()).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Google Drive download failed: HTTP ${response.code} ${response.message}")
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

                if (destinationFile.exists()) destinationFile.delete()
                tempFile.renameTo(destinationFile)
                Unit
            }
        }
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanPath = remotePath.trim('/').replace('\\', '/')
            val fileName = File(cleanPath).name
            val parentPath = File(cleanPath).parent?.replace('\\', '/') ?: ""
            val parentFolderId = if (parentPath.isNotEmpty()) resolveFolderId(parentPath, false) ?: return@runCatching else "root"
            val fileId = findFileIdInFolder(parentFolderId, fileName) ?: return@runCatching

            val request = addAuth(Request.Builder().url("$DRIVE_API_BASE/files/$fileId").delete()).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) {
                    error("Failed to delete file on Google Drive: HTTP ${response.code}")
                }
            }
        }
    }

    override suspend fun disconnect() {
        folderIdCache.clear()
    }

    private fun resolveFolderId(path: String, createIfMissing: Boolean): String? {
        val cleanPath = path.trim('/').replace('\\', '/')
        if (cleanPath.isEmpty() || cleanPath == "root") return "root"

        folderIdCache[cleanPath]?.let { return it }

        val parts = cleanPath.split('/')
        var currentParentId = "root"
        var currentAccumulatedPath = ""

        for (part in parts) {
            if (part.isEmpty()) continue
            currentAccumulatedPath = if (currentAccumulatedPath.isEmpty()) part else "$currentAccumulatedPath/$part"

            val cachedId = folderIdCache[currentAccumulatedPath]
            if (cachedId != null) {
                currentParentId = cachedId
                continue
            }

            var folderId = findFolderIdInParent(currentParentId, part)
            if (folderId == null && createIfMissing) {
                folderId = createFolderInParent(currentParentId, part)
            }
            if (folderId == null) {
                return null
            }
            folderIdCache[currentAccumulatedPath] = folderId
            currentParentId = folderId
        }

        return currentParentId
    }

    private fun findFolderIdInParent(parentId: String, folderName: String): String? {
        val query = "'$parentId' in parents and name = '$folderName' and mimeType = '$FOLDER_MIME_TYPE' and trashed = false"
        val url = "$DRIVE_API_BASE/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id)&pageSize=1"
        val request = addAuth(Request.Builder().url(url).get()).build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        files.getJSONObject(0).getString("id")
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createFolderInParent(parentId: String, folderName: String): String? {
        val meta = JSONObject()
            .put("name", folderName)
            .put("mimeType", FOLDER_MIME_TYPE)
            .put("parents", org.json.JSONArray().put(parentId))
            .toString()

        val request = addAuth(
            Request.Builder()
                .url("$DRIVE_API_BASE/files")
                .post(RequestBody.create("application/json; charset=UTF-8".toMediaTypeOrNull(), meta))
        ).build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    json.getString("id")
                } else {
                    Log.e(TAG, "Failed to create folder $folderName: HTTP ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder $folderName", e)
            null
        }
    }

    private fun findFileIdInFolder(parentId: String, fileName: String): String? {
        val query = "'$parentId' in parents and name = '$fileName' and trashed = false"
        val url = "$DRIVE_API_BASE/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id)&pageSize=1"
        val request = addAuth(Request.Builder().url(url).get()).build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        files.getJSONObject(0).getString("id")
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
