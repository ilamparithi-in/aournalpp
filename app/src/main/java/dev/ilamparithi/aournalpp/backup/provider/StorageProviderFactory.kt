package dev.ilamparithi.aournalpp.backup.provider

import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType

object StorageProviderFactory {

    fun createProvider(config: ServiceConfig): CloudStorageProvider {
        return when (config.providerType) {
            StorageProviderType.NEXTCLOUD,
            StorageProviderType.WEBDAV -> WebDavStorageProvider(config)
            StorageProviderType.GOOGLE_DRIVE -> GoogleDriveProvider(config)
            StorageProviderType.SFTP -> SftpStorageProvider(config)
            StorageProviderType.SMB3 -> SmbStorageProvider(config)
            StorageProviderType.FTP -> FtpStorageProvider(config)
        }
    }
}
