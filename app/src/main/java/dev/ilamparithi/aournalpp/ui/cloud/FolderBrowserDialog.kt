package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.ui.util.a11yHeading
import dev.ilamparithi.aournalpp.ui.util.minTouchTarget
import androidx.compose.ui.res.stringResource
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.provider.StorageProviderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

enum class FolderBrowserMode {
    LOCAL,
    REMOTE
}

data class BrowserFolderItem(
    val name: String,
    val fullPath: String
)

private fun decodeUrlSafe(value: String): String {
    return try {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    } catch (_: Exception) {
        value
    }
}

@Composable
fun FolderBrowserDialog(
    mode: FolderBrowserMode,
    title: String = if (mode == FolderBrowserMode.LOCAL) "Select Local Folder" else "Select Remote Folder",
    initialPath: String = "",
    rootDirectory: File? = null, // Used for LOCAL mode
    serviceConfig: ServiceConfig? = null, // Used for REMOTE mode
    onFolderSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val localRoot = remember(rootDirectory) { rootDirectory ?: File("/sdcard") }

    // Normalize initial relative path
    val normalizedInitial = remember(initialPath, mode, localRoot) {
        val raw = initialPath.trim().trimEnd('/')
        if (mode == FolderBrowserMode.LOCAL) {
            if (raw.startsWith(localRoot.absolutePath)) {
                raw.removePrefix(localRoot.absolutePath).trim('/')
            } else {
                ""
            }
        } else {
            raw.trim('/')
        }
    }

    var currentRelativePath by remember { mutableStateOf(normalizedInitial) }
    var folderItems by remember { mutableStateOf<List<BrowserFolderItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    fun loadFolders(relPath: String) {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                if (mode == FolderBrowserMode.LOCAL) {
                    val targetDir = if (relPath.isEmpty()) localRoot else File(localRoot, relPath)
                    if (targetDir.exists() && targetDir.isDirectory) {
                        val subdirs = withContext(Dispatchers.IO) {
                            targetDir.listFiles { file -> file.isDirectory && !file.name.startsWith(".") }
                                ?.sortedBy { it.name.lowercase() }
                                ?.map {
                                    val itemRelPath = if (relPath.isEmpty()) it.name else "$relPath/${it.name}"
                                    BrowserFolderItem(name = it.name, fullPath = itemRelPath)
                                }
                                ?: emptyList()
                        }
                        folderItems = subdirs
                    } else {
                        folderItems = emptyList()
                    }
                } else {
                    // REMOTE MODE
                    if (serviceConfig != null) {
                        val provider = StorageProviderFactory.createProvider(serviceConfig)
                        val listResult = provider.listFiles(relPath)
                        provider.disconnect()
                        if (listResult.isSuccess) {
                            val entries = listResult.getOrNull() ?: emptyList()
                            folderItems = entries.filter { it.isDirectory }.map {
                                val decodedRemotePath = decodeUrlSafe(it.remotePath)
                                val rawName = File(decodedRemotePath).name.ifBlank { decodedRemotePath }
                                val decodedName = decodeUrlSafe(rawName)
                                val cleanFullPath = if (it.remotePath.startsWith(relPath) && relPath.isNotEmpty()) {
                                    it.remotePath.trim('/')
                                } else if (relPath.isEmpty()) {
                                    rawName
                                } else {
                                    "$relPath/$rawName"
                                }
                                BrowserFolderItem(
                                    name = decodedName,
                                    fullPath = cleanFullPath
                                )
                            }.sortedBy { it.name.lowercase() }
                        } else {
                            errorMessage = listResult.exceptionOrNull()?.message ?: "Failed to list remote folders"
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load directory"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentRelativePath) {
        loadFolders(currentRelativePath)
    }

    androidx.activity.compose.PredictiveBackHandler(enabled = currentRelativePath.isNotEmpty()) { progressFlow ->
        try {
            progressFlow.collect { /* tracking */ }
            val parent = if (currentRelativePath.contains('/')) {
                currentRelativePath.substringBeforeLast('/')
            } else {
                ""
            }
            currentRelativePath = parent
        } catch (_: kotlinx.coroutines.CancellationException) {
            // Cancelled
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(540.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.a11yHeading()
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                }

                // Breadcrumb path navigation bar
                val rootLabel = if (mode == FolderBrowserMode.LOCAL) (localRoot.name.ifBlank { "Notes" }) else "Cloud Root"
                val pathSegments = if (currentRelativePath.isEmpty()) listOf(rootLabel) else listOf(rootLabel) + currentRelativePath.split('/').filter { it.isNotEmpty() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pathSegments.forEachIndexed { index, seg ->
                        if (index > 0) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(12.dp)
                                    .padding(horizontal = 2.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }

                        val displaySegment = if (index == 0) seg else decodeUrlSafe(seg)

                        Text(
                            text = displaySegment,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (index == pathSegments.lastIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == pathSegments.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    if (index == 0) {
                                        currentRelativePath = ""
                                    } else {
                                        val targetSegments = pathSegments.subList(1, index + 1)
                                        currentRelativePath = targetSegments.joinToString("/")
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Directory Contents List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (errorMessage != null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(onClick = { loadFolders(currentRelativePath) }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry")
                            }
                        }
                    } else if (folderItems.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No subfolders found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "You can select this folder or create a new one below",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(folderItems, key = { it.fullPath }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { currentRelativePath = item.fullPath }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            newFolderName = ""
                            showNewFolderDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Folder")
                    }

                    Button(
                        onClick = {
                            val selectedResult = if (mode == FolderBrowserMode.LOCAL) {
                                if (currentRelativePath.isEmpty()) localRoot.absolutePath else File(localRoot, currentRelativePath).absolutePath
                            } else {
                                decodeUrlSafe(currentRelativePath)
                            }
                            onFolderSelected(selectedResult)
                            onDismissRequest()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Current Folder")
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            properties = AppDialogDefaults.Properties,
            modifier = Modifier.promptWidth(),
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = newFolderName.trim()
                        if (cleanName.isNotBlank()) {
                            coroutineScope.launch {
                                if (mode == FolderBrowserMode.LOCAL) {
                                    val parent = if (currentRelativePath.isEmpty()) localRoot else File(localRoot, currentRelativePath)
                                    val newDir = File(parent, cleanName)
                                    newDir.mkdirs()
                                    currentRelativePath = if (currentRelativePath.isEmpty()) cleanName else "$currentRelativePath/$cleanName"
                                } else if (serviceConfig != null) {
                                    val newRemotePath = if (currentRelativePath.isEmpty()) cleanName else "$currentRelativePath/$cleanName"
                                    val provider = StorageProviderFactory.createProvider(serviceConfig)
                                    provider.createDirectory(newRemotePath)
                                    provider.disconnect()
                                    currentRelativePath = newRemotePath
                                }
                                showNewFolderDialog = false
                            }
                        }
                    },
                    enabled = newFolderName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
