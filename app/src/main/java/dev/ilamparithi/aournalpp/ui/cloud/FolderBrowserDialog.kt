package dev.ilamparithi.aournalpp.ui.cloud

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.provider.StorageProviderFactory
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserDialog(
    mode: FolderBrowserMode,
    title: String = if (mode == FolderBrowserMode.LOCAL) "Select Local Folder" else "Select Remote Folder",
    subtitle: String? = null,
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
    val pullRefreshState = rememberPullToRefreshState()

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
                    if (serviceConfig == null) {
                        errorMessage = "No cloud service configured"
                        folderItems = emptyList()
                    } else {
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            AppDialogDefaults.makeDialogFullscreenEdgeToEdge(view)
            onDispose {}
        }

        // Navigate up one level if in a subfolder, otherwise exit
        BackHandler {
            if (currentRelativePath.isNotEmpty()) {
                val parentPath = if (currentRelativePath.contains('/')) {
                    currentRelativePath.substringBeforeLast('/')
                } else {
                    ""
                }
                currentRelativePath = parentPath
            } else {
                onDismissRequest()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar / Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onDismissRequest,
                                    modifier = Modifier.minTouchTarget()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.action_cancel)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (mode == FolderBrowserMode.LOCAL) Icons.Default.Folder else Icons.Default.Cloud,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.a11yHeading()
                                    )
                                    if (!subtitle.isNullOrBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    newFolderName = ""
                                    showNewFolderDialog = true
                                },
                                modifier = Modifier.minTouchTarget()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = "New Folder",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Breadcrumbs bar
                val rootLabel = if (mode == FolderBrowserMode.LOCAL) (localRoot.name.ifBlank { "Notes" }) else "Cloud Root"
                val pathSegments = if (currentRelativePath.isEmpty()) listOf(rootLabel) else listOf(rootLabel) + currentRelativePath.split('/').filter { it.isNotEmpty() }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Up Button - always present, greyed out and disabled when in root
                        val isAtRoot = currentRelativePath.isEmpty()
                        IconButton(
                            onClick = {
                                if (!isAtRoot) {
                                    val parentPath = if (currentRelativePath.contains('/')) {
                                        currentRelativePath.substringBeforeLast('/')
                                    } else {
                                        ""
                                    }
                                    currentRelativePath = parentPath
                                }
                            },
                            enabled = !isAtRoot,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = if (!isAtRoot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
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

                            val displaySegment = if (index == 0) seg else decodeUrlSafe(seg)
                            val isLast = index == pathSegments.lastIndex

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isLast) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (index == 0) {
                                            currentRelativePath = ""
                                        } else {
                                            val targetSegments = pathSegments.subList(1, index + 1)
                                            currentRelativePath = targetSegments.joinToString("/")
                                        }
                                    }
                            ) {
                                Text(
                                    text = displaySegment,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Directory Contents List
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { loadFolders(currentRelativePath) },
                    state = pullRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = isLoading,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isLoading && folderItems.isEmpty()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Loading directories…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (errorMessage != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = errorMessage!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            textAlign = TextAlign.Center
                                        )
                                        FilledTonalButton(onClick = { loadFolders(currentRelativePath) }) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        } else if (folderItems.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No subfolders found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "You can select this folder or create a new one below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (mode == FolderBrowserMode.REMOTE && serviceConfig?.providerType == StorageProviderType.GOOGLE_DRIVE) {
                                    Text(
                                        text = stringResource(R.string.hint_google_drive_folder_browser_scope),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        newFolderName = ""
                                        showNewFolderDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create Folder")
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            items(folderItems, key = { it.fullPath }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    tonalElevation = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { currentRelativePath = item.fullPath }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Docked Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.navigationBars
                                    .union(WindowInsets.systemGestures)
                                    .only(WindowInsetsSides.Bottom)
                            )
                            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Current Path Label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            val currentDisplayPath = if (mode == FolderBrowserMode.LOCAL) {
                                if (currentRelativePath.isEmpty()) localRoot.name.ifBlank { "Notes" } else "${localRoot.name}/$currentRelativePath"
                            } else {
                                if (currentRelativePath.isEmpty()) "Cloud Root" else "/${decodeUrlSafe(currentRelativePath)}"
                            }
                            Text(
                                text = "Selected: $currentDisplayPath",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    newFolderName = ""
                                    showNewFolderDialog = true
                                },
                                modifier = Modifier.weight(1f)
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
                                },
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Folder")
                            }
                        }
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
