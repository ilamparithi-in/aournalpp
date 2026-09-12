package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.model.FolderItem
import java.io.File

/**
 * Breadcrumb navigation bar showing the folder path from "Notes" root down to the current active directory.
 */
@Composable
fun FolderBreadcrumbsBar(
    currentDirectory: File,
    currentFolderItem: FolderItem?,
    isEmergencySavesFolder: Boolean,
    onNavigateToRoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToRoot() }
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .size(14.dp)
                    .padding(horizontal = 2.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            if (!currentFolderItem?.iconEmoji.isNullOrBlank()) {
                Text(text = currentFolderItem.iconEmoji, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
            } else if (currentFolderItem?.iconType == "emergency" || isEmergencySavesFolder) {
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = currentFolderItem?.colorHex?.let {
                        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
                    } ?: MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = currentDirectory.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
