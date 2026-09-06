package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.ui.util.minTouchTarget
import dev.ilamparithi.aournalpp.ui.util.AppTooltipBox

@Composable
fun QuickSyncButton(
    modifier: Modifier = Modifier,
    onSyncFinished: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val vault = remember { CredentialsVault(context) }
    val prefs = remember { dev.ilamparithi.aournalpp.backup.worker.BackupPreferences(context) }
    val isSyncRunningByManager by FileTransferQueueManager.isSyncRunning.collectAsStateWithLifecycle()

    val queueItems by FileTransferQueueManager.items.collectAsStateWithLifecycle()
    val activeCount = queueItems.count { it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED }

    val isSyncing = activeCount > 0 || isSyncRunningByManager

    // Smooth spinning animation when syncing
    val infiniteTransition = rememberInfiniteTransition(label = "quickSyncSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "quickSyncRotation"
    )

    val syncTooltip = if (isSyncing) stringResource(R.string.cd_syncing) else stringResource(R.string.cd_quick_sync)

    AppTooltipBox(tooltipText = syncTooltip) {
        IconButton(
            onClick = {
                if (!isSyncing) {
                    try {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}

                    val services = vault.getAllServices().filter { it.isEnabled }
                    if (services.isEmpty()) {
                        onSyncFinished?.invoke("No active cloud services configured")
                        return@IconButton
                    }

                    val netCheck = dev.ilamparithi.aournalpp.utils.NetworkUtils.checkSyncNetworkPreconditions(
                        context,
                        wifiOnly = prefs.isWifiOnlyEnabled
                    )
                    if (!netCheck.canSync) {
                        onSyncFinished?.invoke(netCheck.errorMessage ?: "Network not available")
                        return@IconButton
                    }

                    dev.ilamparithi.aournalpp.backup.worker.BackupScheduler.triggerImmediateSync(
                        context,
                        wifiOnly = prefs.isWifiOnlyEnabled
                    )
                    onSyncFinished?.invoke("Synchronization queued in background...")
                }
            },
            modifier = modifier.minTouchTarget()
        ) {
            BadgedBox(
                badge = {
                    if (activeCount > 0) {
                        Badge { Text(activeCount.toString()) }
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                    contentDescription = syncTooltip,
                    tint = if (isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .then(if (isSyncing) Modifier.rotate(rotation) else Modifier)
                )
            }
        }
    }
}
