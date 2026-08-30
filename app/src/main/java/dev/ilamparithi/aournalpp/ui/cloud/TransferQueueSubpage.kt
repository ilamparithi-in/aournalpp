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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ilamparithi.aournalpp.backup.model.TransferDirection
import dev.ilamparithi.aournalpp.backup.model.TransferItem
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import dev.ilamparithi.aournalpp.utils.FormatUtils

enum class QueueFilter {
    ALL,
    ACTIVE,
    QUEUED,
    COMPLETED,
    FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferQueueSubpage(
    onNavigateBack: () -> Unit
) {
    val items: List<TransferItem> by FileTransferQueueManager.items.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(QueueFilter.ALL) }

    val activeCount = items.count { it.status == TransferStatus.IN_PROGRESS }
    val queuedCount = items.count { it.status == TransferStatus.QUEUED }
    val completedCount = items.count { it.status == TransferStatus.COMPLETED }
    val failedCount = items.count { it.status == TransferStatus.FAILED }

    val totalActiveSpeed = items.filter { it.status == TransferStatus.IN_PROGRESS }.sumOf { it.speedBytesPerSec }

    val filteredItems = remember(items, selectedFilter) {
        when (selectedFilter) {
            QueueFilter.ALL -> items
            QueueFilter.ACTIVE -> items.filter { it.status == TransferStatus.IN_PROGRESS }
            QueueFilter.QUEUED -> items.filter { it.status == TransferStatus.QUEUED }
            QueueFilter.COMPLETED -> items.filter { it.status == TransferStatus.COMPLETED }
            QueueFilter.FAILED -> items.filter { it.status == TransferStatus.FAILED }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${items.size} total items • ${formatSpeedRate(totalActiveSpeed)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (completedCount > 0 || failedCount > 0) {
                        TextButton(onClick = { FileTransferQueueManager.clearCompleted() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_clear))
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
            // Live Stats Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_active_transfers),
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
                    title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_completed_transfers),
                    value = "$completedCount",
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                if (failedCount > 0) {
                    StatCard(
                        title = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_failed_transfers),
                        value = "$failedCount",
                        icon = Icons.Default.Error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Filter Chips Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == QueueFilter.ALL,
                        onClick = { selectedFilter = QueueFilter.ALL },
                        label = { Text("All (${items.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == QueueFilter.ACTIVE,
                        onClick = { selectedFilter = QueueFilter.ACTIVE },
                        label = { Text("${androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_active_transfers)} ($activeCount)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == QueueFilter.QUEUED,
                        onClick = { selectedFilter = QueueFilter.QUEUED },
                        label = { Text("${androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_queued_transfers)} ($queuedCount)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == QueueFilter.COMPLETED,
                        onClick = { selectedFilter = QueueFilter.COMPLETED },
                        label = { Text("${androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_completed_transfers)} ($completedCount)") }
                    )
                }
                if (failedCount > 0) {
                    item {
                        FilterChip(
                            selected = selectedFilter == QueueFilter.FAILED,
                            onClick = { selectedFilter = QueueFilter.FAILED },
                            label = { Text("${androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_failed_transfers)} ($failedCount)") }
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
                            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_empty_desc),
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
                        QueueItemCard(item = item)
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
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = color)
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
private fun QueueItemCard(item: TransferItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                    TransferStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
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

                // Status or Cancel Button
                when (item.status) {
                    TransferStatus.IN_PROGRESS, TransferStatus.QUEUED -> {
                        IconButton(
                            onClick = { FileTransferQueueManager.requestCancel(item.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TransferStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_completed_transfers),
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    TransferStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.queue_failed_transfers),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    TransferStatus.SKIPPED -> {
                        Text(
                            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_skip),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    TransferStatus.CANCELLED -> {
                        Text(
                            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Progress bar and details for Active transfer
            if (item.status == TransferStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
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
                        text = formatSpeedRate(item.speedBytesPerSec),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Error message if Failed
            if (item.status == TransferStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
