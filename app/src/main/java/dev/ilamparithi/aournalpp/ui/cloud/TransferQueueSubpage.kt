package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.TransferDirection
import dev.ilamparithi.aournalpp.backup.model.TransferItem
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import dev.ilamparithi.aournalpp.utils.FormatUtils
import kotlinx.coroutines.launch

enum class QueueFilter {
    ALL,
    ACTIVE,
    QUEUED,
    PAUSED,
    COMPLETED,
    FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferQueueSubpage(
    engine: BackupEngine? = null,
    onNavigateBack: () -> Unit
) {
    val items: List<TransferItem> by FileTransferQueueManager.items.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // Multi-cloud detection & filtering
    val cloudServices = remember(items) {
        items.map { it.serviceId to it.serviceName }.distinct()
    }
    var selectedServiceId by remember { mutableStateOf<String?>(null) }

    // Adaptive filter behavior:
    // If only one cloud service is present in the queue, adapt to it automatically.
    // If multiple exist, keep user selection or default to all.
    LaunchedEffect(cloudServices) {
        if (cloudServices.size == 1) {
            selectedServiceId = cloudServices.first().first
        } else if (cloudServices.isEmpty()) {
            selectedServiceId = null
        } else if (selectedServiceId != null && cloudServices.none { it.first == selectedServiceId }) {
            selectedServiceId = null
        }
    }

    // Items filtered by selected cloud service
    val cloudFilteredItems = remember(items, selectedServiceId) {
        if (selectedServiceId == null) items else items.filter { it.serviceId == selectedServiceId }
    }

    var selectedFilter by remember { mutableStateOf(QueueFilter.ALL) }

    val activeCount = cloudFilteredItems.count { it.status == TransferStatus.IN_PROGRESS }
    val queuedCount = cloudFilteredItems.count { it.status == TransferStatus.QUEUED }
    val pausedCount = cloudFilteredItems.count { it.status == TransferStatus.PAUSED }
    val completedCount = cloudFilteredItems.count { it.status == TransferStatus.COMPLETED }
    val failedCount = cloudFilteredItems.count { it.status == TransferStatus.FAILED }

    val totalActiveSpeed = cloudFilteredItems.filter { it.status == TransferStatus.IN_PROGRESS }.sumOf { it.speedBytesPerSec }

    val filteredItems = remember(cloudFilteredItems, selectedFilter) {
        val list = when (selectedFilter) {
            QueueFilter.ALL -> cloudFilteredItems
            QueueFilter.ACTIVE -> cloudFilteredItems.filter { it.status == TransferStatus.IN_PROGRESS }
            QueueFilter.QUEUED -> cloudFilteredItems.filter { it.status == TransferStatus.QUEUED }
            QueueFilter.PAUSED -> cloudFilteredItems.filter { it.status == TransferStatus.PAUSED }
            QueueFilter.COMPLETED -> cloudFilteredItems.filter { it.status == TransferStatus.COMPLETED }
            QueueFilter.FAILED -> cloudFilteredItems.filter { it.status == TransferStatus.FAILED }
        }
        list.distinctBy { it.id }
    }

    var isRetryingAll by remember { mutableStateOf(false) }
    val retryingItemIds = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.queue_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredItems.size} items • ${formatSpeedRate(totalActiveSpeed)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    // Retry All Failed Transfers
                    if (failedCount > 0 && engine != null) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    isRetryingAll = true
                                    try {
                                        if (selectedServiceId != null) {
                                            val failedInView = cloudFilteredItems.filter { it.status == TransferStatus.FAILED }
                                            failedInView.forEach { engine.retryTransfer(it) }
                                        } else {
                                            engine.retryAllFailed()
                                        }
                                    } finally {
                                        isRetryingAll = false
                                    }
                                }
                            },
                            enabled = !isRetryingAll
                        ) {
                            if (isRetryingAll) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_retry_all))
                        }
                    }

                    // Pause All / Resume All
                    if (activeCount > 0 || queuedCount > 0) {
                        IconButton(
                            onClick = {
                                if (selectedServiceId != null) {
                                    val activeInView = cloudFilteredItems.filter { it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED }
                                    FileTransferQueueManager.pauseItems(activeInView.map { it.id }.toSet())
                                } else {
                                    FileTransferQueueManager.pauseAll()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = stringResource(R.string.action_pause_all),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (pausedCount > 0) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (selectedServiceId != null) {
                                        val pausedInView = cloudFilteredItems.filter { it.status == TransferStatus.PAUSED }
                                        if (engine != null) {
                                            engine.resumeItems(pausedInView)
                                        } else {
                                            FileTransferQueueManager.resumeItems(pausedInView.map { it.id }.toSet())
                                        }
                                    } else {
                                        if (engine != null) {
                                            engine.resumeAllPaused()
                                        } else {
                                            FileTransferQueueManager.resumeAll()
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.action_resume_all),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Clear Completed Transfers
                    if (completedCount > 0) {
                        IconButton(onClick = { FileTransferQueueManager.clearCompleted(selectedServiceId) }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.action_clear_completed)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Live Stats Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.queue_active_transfers),
                    value = "$activeCount",
                    icon = Icons.Default.Sync,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Speed",
                    value = formatSpeedRate(totalActiveSpeed),
                    icon = Icons.Default.Speed,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.queue_queued_transfers),
                    value = "$queuedCount",
                    icon = Icons.Default.HourglassEmpty,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                if (pausedCount > 0) {
                    StatCard(
                        title = stringResource(R.string.queue_paused_transfers),
                        value = "$pausedCount",
                        icon = Icons.Default.PauseCircle,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
                StatCard(
                    title = stringResource(R.string.queue_completed_transfers),
                    value = "$completedCount",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                if (failedCount > 0) {
                    StatCard(
                        title = stringResource(R.string.queue_failed_transfers),
                        value = "$failedCount",
                        icon = Icons.Default.Error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Cloud Service Filter Row (Show all clouds and allow filtering to a single cloud)
            if (cloudServices.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedServiceId == null,
                            onClick = { selectedServiceId = null },
                            leadingIcon = {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("${stringResource(R.string.queue_filter_all_clouds)} (${items.size})") }
                        )
                    }
                    items(cloudServices, key = { it.first }) { (svcId, svcName) ->
                        val count = items.count { it.serviceId == svcId }
                        FilterChip(
                            selected = selectedServiceId == svcId,
                            onClick = { selectedServiceId = svcId },
                            leadingIcon = {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("$svcName ($count)") }
                        )
                    }
                }
            }

            // Status Filter Chips Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == QueueFilter.ALL,
                        onClick = { selectedFilter = QueueFilter.ALL },
                        label = { Text("All (${cloudFilteredItems.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == QueueFilter.ACTIVE,
                        onClick = { selectedFilter = QueueFilter.ACTIVE },
                        label = { Text("${stringResource(R.string.queue_active_transfers)} ($activeCount)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == QueueFilter.QUEUED,
                        onClick = { selectedFilter = QueueFilter.QUEUED },
                        label = { Text("${stringResource(R.string.queue_queued_transfers)} ($queuedCount)") }
                    )
                }
                if (pausedCount > 0) {
                    item {
                        FilterChip(
                            selected = selectedFilter == QueueFilter.PAUSED,
                            onClick = { selectedFilter = QueueFilter.PAUSED },
                            label = { Text("${stringResource(R.string.queue_paused_transfers)} ($pausedCount)") }
                        )
                    }
                }
                item {
                    FilterChip(
                        selected = selectedFilter == QueueFilter.COMPLETED,
                        onClick = { selectedFilter = QueueFilter.COMPLETED },
                        label = { Text("${stringResource(R.string.queue_completed_transfers)} ($completedCount)") }
                    )
                }
                if (failedCount > 0) {
                    item {
                        FilterChip(
                            selected = selectedFilter == QueueFilter.FAILED,
                            onClick = { selectedFilter = QueueFilter.FAILED },
                            label = { Text("${stringResource(R.string.queue_failed_transfers)} ($failedCount)") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.queue_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val isItemRetrying = retryingItemIds[item.id] == true
                        QueueItemCard(
                            item = item,
                            isRetrying = isItemRetrying,
                            onPause = { FileTransferQueueManager.requestPause(item.id) },
                            onResume = {
                                coroutineScope.launch {
                                    retryingItemIds[item.id] = true
                                    try {
                                        if (engine != null) {
                                            engine.resumeTransfer(item)
                                        } else {
                                            FileTransferQueueManager.requestResume(item.id)
                                        }
                                    } finally {
                                        retryingItemIds.remove(item.id)
                                    }
                                }
                            },
                            onCancel = { FileTransferQueueManager.requestCancel(item.id) },
                            onRetry = {
                                coroutineScope.launch {
                                    retryingItemIds[item.id] = true
                                    try {
                                        engine?.retryTransfer(item)
                                    } finally {
                                        retryingItemIds.remove(item.id)
                                    }
                                }
                            },
                            onDismiss = { FileTransferQueueManager.dismissItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surface)
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun QueueItemCard(
    item: TransferItem,
    isRetrying: Boolean = false,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                TransferStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                TransferStatus.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon for Direction / Status
                val icon = when (item.direction) {
                    TransferDirection.UPLOAD -> Icons.Default.CloudUpload
                    TransferDirection.DOWNLOAD -> Icons.Default.CloudDownload
                }
                val iconTint = when (item.status) {
                    TransferStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                    TransferStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
                    TransferStatus.COMPLETED -> Color(0xFF2E7D32)
                    TransferStatus.FAILED -> MaterialTheme.colorScheme.error
                    TransferStatus.QUEUED, TransferStatus.SKIPPED, TransferStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                }

                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.serviceName} • ${item.remotePath}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action Controls based on Status
                when (item.status) {
                    TransferStatus.IN_PROGRESS, TransferStatus.QUEUED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onPause,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = stringResource(R.string.action_pause),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StopCircle,
                                    contentDescription = stringResource(R.string.action_cancel),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    TransferStatus.PAUSED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isRetrying) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = onResume,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.action_resume),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StopCircle,
                                    contentDescription = stringResource(R.string.action_cancel),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    TransferStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.queue_completed_transfers),
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    TransferStatus.FAILED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isRetrying) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = onRetry,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.action_retry),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_dismiss),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    TransferStatus.CANCELLED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isRetrying) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = onRetry,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.action_retry),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_dismiss),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    TransferStatus.SKIPPED -> {
                        Text(
                            text = stringResource(R.string.action_skip),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Progress bar and details for Active or Paused transfer
            if (item.status == TransferStatus.IN_PROGRESS || item.status == TransferStatus.PAUSED) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (item.status == TransferStatus.PAUSED) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatByteSize(item.transferredBytes)} / ${formatByteSize(item.totalBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (item.status == TransferStatus.PAUSED) stringResource(R.string.queue_paused_transfers) else formatSpeedRate(item.speedBytesPerSec),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.status == TransferStatus.PAUSED) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Prominent Error Box for Failed Transfers with Retry button
            if (item.status == TransferStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.errorMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onRetry,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_retry), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun formatByteSize(bytes: Long): String {
    return FormatUtils.formatFileSize(bytes)
}

private fun formatSpeedRate(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 KB/s"
    return "${FormatUtils.formatFileSize(bytesPerSec)}/s"
}
