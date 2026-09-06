package dev.ilamparithi.aournalpp.backup.provider

import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object StorageProviderFactory {

    val sharedHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun createProvider(config: ServiceConfig): CloudStorageProvider {
        return when (config.providerType) {
            StorageProviderType.NEXTCLOUD,
            StorageProviderType.WEBDAV -> WebDavStorageProvider(config, sharedHttpClient)
            StorageProviderType.GOOGLE_DRIVE -> GoogleDriveProvider(config, sharedHttpClient)
            StorageProviderType.SFTP -> SftpStorageProvider(config)
            StorageProviderType.SMB3 -> SmbStorageProvider(config)
            StorageProviderType.FTP -> FtpStorageProvider(config)
        }
    }
}
