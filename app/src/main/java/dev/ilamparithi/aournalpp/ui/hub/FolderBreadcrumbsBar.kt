package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import java.io.File

/**
 * Breadcrumb navigation bar showing the folder path from "Notes" root down to the current active directory.
 */
@Composable
fun FolderBreadcrumbsBar(
    currentDirectory: File,
    rootDirectory: File,
    onNavigateTo: (File) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val relativePath = remember(currentDirectory, rootDirectory) {
        val rootPath = rootDirectory.absolutePath
        val currPath = currentDirectory.absolutePath
        if (currPath.startsWith(rootPath)) {
            currPath.removePrefix(rootPath).trim('/')
        } else {
            ""
        }
    }

    val rootFolderName = stringResource(R.string.hub_root_folder_name)
    val pathSegments = remember(relativePath, rootFolderName) {
        if (relativePath.isEmpty()) listOf(rootFolderName) else listOf(rootFolderName) + relativePath.split('/').filter { it.isNotEmpty() }
    }

    val isAtRoot = relativePath.isEmpty()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (enabled && !isAtRoot) {
                        val parentFile = currentDirectory.parentFile
                        if (parentFile != null && parentFile.absolutePath.startsWith(rootDirectory.absolutePath)) {
                            onNavigateTo(parentFile)
                        } else {
                            onNavigateTo(rootDirectory)
                        }
                    }
                },
                enabled = enabled && !isAtRoot,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = if (enabled && !isAtRoot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(18.dp)
                )
            }

            pathSegments.forEachIndexed { index, seg ->
                if (index > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val isLast = index == pathSegments.lastIndex

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLast) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = enabled && !isLast) {
                            if (index == 0) {
                                onNavigateTo(rootDirectory)
                            } else {
                                val targetSegments = pathSegments.subList(1, index + 1)
                                val targetRelative = targetSegments.joinToString("/")
                                onNavigateTo(File(rootDirectory, targetRelative))
                            }
                        }
                ) {
                    Text(
                        text = seg,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLast) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else if (!enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
