package dev.ilamparithi.aournalpp.backup.provider

import android.util.Log
import dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.security.GoogleOAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage provider for Google Drive using Google Drive REST API v3 over OkHttp.
 * Fully compatible with FOSS, microG, and de-Googled devices without requiring Google Play Services.
 */
class GoogleDriveProvider(
    private val config: ServiceConfig,
    private val httpClient: OkHttpClient = StorageProviderFactory.sharedHttpClient,
    private val onTokenRefreshed: ((newAccessToken: String, newRefreshToken: String?, expiryEpochMs: Long) -> Unit)? = null
) : CloudStorageProvider {

    companion object {
        private const val TAG = "GoogleDriveProvider"
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"

        private val ISO_DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }
        }

        private val ISO_FALLBACK_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }
        }

        private fun parseIsoTime(modifiedStr: String): Long {
            if (modifiedStr.isBlank()) return 0L
            return try {
                ISO_DATE_FORMAT.get()?.parse(modifiedStr)?.time
                    ?: ISO_FALLBACK_FORMAT.get()?.parse(modifiedStr)?.time
                    ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    data class DriveFileInfo(val id: String, val lastModifiedMs: Long)

    override val providerType: StorageProviderType = StorageProviderType.GOOGLE_DRIVE

    // Cache of remotePath -> folderId
    private val folderIdCache = ConcurrentHashMap<String, String>()

    private val refreshMutex = Mutex()
    private var currentAccessToken: String = config.authToken.ifBlank { config.passwordOrSecret }.trim()
    private var currentRefreshToken: String = config.refreshToken.trim()
    private var currentExpiryEpochMs: Long = config.tokenExpiryEpochMs

    private suspend fun refreshAndPersistToken(): Boolean = refreshMutex.withLock {
        if (currentRefreshToken.isBlank()) {
            Log.w(TAG, "Cannot refresh Google Drive token: refresh token is blank.")
            return false
        }
        Log.i(TAG, "Refreshing Google Drive OAuth2 access token for ${config.name}...")
        val refreshResult = GoogleOAuthManager.refreshAccessToken(currentRefreshToken)
        if (refreshResult.isSuccess) {
            val resp = refreshResult.getOrNull()
            if (resp != null) {
                currentAccessToken = resp.accessToken
                if (!resp.refreshToken.isNullOrBlank()) {
                    currentRefreshToken = resp.refreshToken
                }
                val newExpiry = System.currentTimeMillis() + (resp.expiresInSeconds * 1000L)
                currentExpiryEpochMs = newExpiry
                Log.i(TAG, "Google Drive access token refreshed successfully, expires in ${resp.expiresInSeconds}s")
                onTokenRefreshed?.invoke(resp.accessToken, currentRefreshToken, newExpiry)
                return true
            }
        } else {
            Log.e(TAG, "Failed to refresh Google Drive token: ${refreshResult.exceptionOrNull()?.message}")
        }
        return false
    }

    private suspend fun ensureValidToken() {
        val isExpired = currentExpiryEpochMs > 0L && System.currentTimeMillis() >= (currentExpiryEpochMs - 60_000L)
        if ((currentAccessToken.isBlank() || isExpired) && currentRefreshToken.isNotBlank()) {
            refreshAndPersistToken()
        }
    }

    private suspend fun executeWithAuth(requestFactory: (token: String) -> Request): Response {
        ensureValidToken()
        val initialRequest = requestFactory(currentAccessToken)
        var response = httpClient.newCall(initialRequest).execute()

        if (response.code == 401 && currentRefreshToken.isNotBlank()) {
            response.close()
            Log.i(TAG, "Google Drive returned HTTP 401 Unauthorized. Attempting token refresh & retry...")
            val refreshed = refreshAndPersistToken()
            if (refreshed) {
                val retryRequest = requestFactory(currentAccessToken)
                response = httpClient.newCall(retryRequest).execute()
            }
        }
        return response
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            ensureValidToken()
            if (currentAccessToken.isEmpty()) {
                error("Google Drive OAuth2 Access Token is missing. Please authorize or configure token.")
            }
            val response = executeWithAuth { token ->
                Request.Builder()
                    .url("$DRIVE_API_BASE/files?pageSize=1&fields=files(id)")
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
            }

            response.use { resp ->
                if (resp.isSuccessful) {
                    true
                } else {
                    val rawBody = resp.body?.string() ?: ""
                    Log.e(TAG, "testConnection failed HTTP ${resp.code}: $rawBody")
                    val detailedMessage = try {
                        val json = JSONObject(rawBody)
                        val errObj = json.optJSONObject("error")
                        val msg = errObj?.optString("message")
                        val reason = errObj?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason")
                        if (!reason.isNullOrBlank() && msg != null && !msg.contains(reason)) {
                            "$msg (reason: $reason)"
                        } else {
                            msg ?: rawBody
                        }
                    } catch (e: Exception) {
                        rawBody.ifBlank { resp.message }
                    }
                    error("Google Drive test failed (${resp.code}): $detailedMessage")
                }
            }
        }
    }

    override suspend fun listFiles(remoteDirectory: String): Result<List<RemoteFileMetadata>> = withContext(Dispatchers.IO) {
        runCatching {
            val folderId = resolveFolderId(remoteDirectory, createIfMissing = false) ?: return@runCatching emptyList()
            val query = "'$folderId' in parents and trashed = false"
            val url = "$DRIVE_API_BASE/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,mimeType,size,modifiedTime,md5Checksum)&pageSize=1000"

            val response = executeWithAuth { token ->
                Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
            }
            response.use { resp ->
                if (!resp.isSuccessful) {
                    error("Failed to list files: HTTP ${resp.code} ${resp.message}")
                }
                val body = resp.body?.string() ?: "{}"
                val json = JSONObject(body)
                val filesArray = json.optJSONArray("files") ?: return@runCatching emptyList()

                val list = mutableListOf<RemoteFileMetadata>()
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

                    val modEpoch = parseIsoTime(modifiedStr)

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

            fun createProgressBody() = object : RequestBody() {
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

            val mtimeIso = ISO_DATE_FORMAT.get()?.format(java.util.Date(localFile.lastModified()))
            if (existingFileId != null) {
                // Update content & preserve modification time via multipart update
                val metadataJson = JSONObject().apply {
                    if (mtimeIso != null) put("modifiedTime", mtimeIso)
                }.toString()

                val uploadUrl = "$DRIVE_UPLOAD_BASE/files/$existingFileId?uploadType=multipart"
                val response = executeWithAuth { token ->
                    val multipartBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("metadata", null, metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull()))
                        .addFormDataPart("file", fileName, createProgressBody())
                        .build()
                    Request.Builder()
                        .url(uploadUrl)
                        .header("Authorization", "Bearer $token")
                        .patch(multipartBody)
                        .build()
                }
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        error("Google Drive update failed: HTTP ${resp.code} ${resp.message}")
                    }
                }
            } else {
                // Multipart Create with modifiedTime
                val metadataJson = JSONObject().apply {
                    put("name", fileName)
                    put("parents", org.json.JSONArray().put(parentFolderId))
                    if (mtimeIso != null) put("modifiedTime", mtimeIso)
                }.toString()

                val uploadUrl = "$DRIVE_UPLOAD_BASE/files?uploadType=multipart"
                val response = executeWithAuth { token ->
                    val multipartBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("metadata", null, metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull()))
                        .addFormDataPart("file", fileName, createProgressBody())
                        .build()
                    Request.Builder()
                        .url(uploadUrl)
                        .header("Authorization", "Bearer $token")
                        .post(multipartBody)
                        .build()
                }
                response.use { resp ->
                    if (!resp.isSuccessful && resp.code != 200 && resp.code != 201) {
                        error("Google Drive upload failed: HTTP ${resp.code} ${resp.message}")
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

            val fileInfo = findFileInfoInFolder(parentFolderId, fileName) ?: error("File not found on Google Drive: $remotePath")
            val downloadUrl = "$DRIVE_API_BASE/files/${fileInfo.id}?alt=media"
            val response = executeWithAuth { token ->
                Request.Builder()
                    .url(downloadUrl)
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
            }

            response.use { resp ->
                if (!resp.isSuccessful) {
                    error("Google Drive download failed: HTTP ${resp.code} ${resp.message}")
                }
                val body = resp.body ?: error("Empty response body")
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
                if (fileInfo.lastModifiedMs > 0L) {
                    destinationFile.setLastModified(fileInfo.lastModifiedMs)
                }
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

            val response = executeWithAuth { token ->
                Request.Builder()
                    .url("$DRIVE_API_BASE/files/$fileId")
                    .header("Authorization", "Bearer $token")
                    .delete()
                    .build()
            }
            response.use { resp ->
                if (!resp.isSuccessful && resp.code != 404) {
                    error("Failed to delete file on Google Drive: HTTP ${resp.code}")
                }
            }
        }
    }

    override suspend fun disconnect() {
        folderIdCache.clear()
    }

    private suspend fun resolveFolderId(path: String, createIfMissing: Boolean): String? {
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

    private suspend fun findFolderIdInParent(parentId: String, folderName: String): String? {
        val query = "'$parentId' in parents and name = '$folderName' and mimeType = '$FOLDER_MIME_TYPE' and trashed = false"
        val url = "$DRIVE_API_BASE/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id)&pageSize=1"

        return try {
            val response = executeWithAuth { token ->
                Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
            }
            response.use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
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

    private suspend fun createFolderInParent(parentId: String, folderName: String): String? {
        val meta = JSONObject()
            .put("name", folderName)
            .put("mimeType", FOLDER_MIME_TYPE)
            .put("parents", org.json.JSONArray().put(parentId))
            .toString()

        return try {
            val response = executeWithAuth { token ->
                Request.Builder()
                    .url("$DRIVE_API_BASE/files")
                    .header("Authorization", "Bearer $token")
                    .post(meta.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull()))
                    .build()
            }
            response.use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string() ?: "")
                    json.getString("id")
                } else {
                    Log.e(TAG, "Failed to create folder $folderName: HTTP ${resp.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder $folderName", e)
            null
        }
    }

    private suspend fun findFileInfoInFolder(parentId: String, fileName: String): DriveFileInfo? {
        val query = "'$parentId' in parents and name = '$fileName' and trashed = false"
        val url = "$DRIVE_API_BASE/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,modifiedTime)&pageSize=1"

        return try {
            val response = executeWithAuth { token ->
                Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
            }
            response.use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string() ?: "")
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        val fileObj = files.getJSONObject(0)
                        val id = fileObj.getString("id")
                        val modifiedStr = fileObj.optString("modifiedTime", "")
                        val mtime = parseIsoTime(modifiedStr)
                        DriveFileInfo(id, mtime)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun findFileIdInFolder(parentId: String, fileName: String): String? {
        return findFileInfoInFolder(parentId, fileName)?.id
    }
}
