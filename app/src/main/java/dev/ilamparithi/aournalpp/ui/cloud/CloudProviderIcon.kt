package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType

/**
 * Renders dedicated official vector branding for Google Drive and Nextcloud,
 * and appropriate standard protocol icons for WebDAV, SFTP, SMB3, and FTP.
 */
@Composable
fun CloudProviderIcon(
    providerType: StorageProviderType,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    contentDescription: String? = providerType.displayName,
    tint: Color? = null
) {
    val finalModifier = modifier.size(size)
    when (providerType) {
        StorageProviderType.GOOGLE_DRIVE -> {
            Icon(
                painter = painterResource(R.drawable.ic_google_drive),
                contentDescription = contentDescription,
                modifier = finalModifier,
                tint = tint ?: Color.Unspecified
            )
        }
        StorageProviderType.NEXTCLOUD -> {
            Icon(
                painter = painterResource(R.drawable.ic_nextcloud),
                contentDescription = contentDescription,
                modifier = finalModifier,
                tint = tint ?: Color.Unspecified
            )
        }
        StorageProviderType.WEBDAV -> {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = contentDescription,
                modifier = finalModifier,
                tint = tint ?: LocalContentColor.current
            )
        }
        StorageProviderType.SFTP -> {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = contentDescription,
                modifier = finalModifier,
                tint = tint ?: LocalContentColor.current
            )
        }
        StorageProviderType.SMB3 -> {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = contentDescription,
                modifier = finalModifier,
                tint = tint ?: LocalContentColor.current
            )
        }
        StorageProviderType.FTP -> {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = contentDescription,
                modifier = finalModifier,
                tint = tint ?: LocalContentColor.current
            )
        }
    }
}
