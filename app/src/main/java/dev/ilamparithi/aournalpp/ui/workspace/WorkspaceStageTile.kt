package dev.ilamparithi.aournalpp.ui.workspace

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asAndroidBitmap
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.runtime.ActiveWindowEntry
import dev.ilamparithi.aournalpp.runtime.ActiveWorkspaceState
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.ui.snap.SnapLayoutMode
import dev.ilamparithi.aournalpp.utils.ThumbnailManager
import dev.ilamparithi.aournalpp.utils.WindowPreviewManager
import java.io.File

/**
 * Compound stage tile representing the physical multi-window arrangement
 * (mimicking SnapLayoutMode: Single, Split Two, Split Three, Grid Four).
 * Tapping a pane activates the corresponding window without altering
 * geometry or snap configuration.
 */
@Composable
fun WorkspaceStageTile(
    workspaceState: ActiveWorkspaceState,
    tmpDir: File,
    onWindowClick: (ActiveWindowEntry, Rect?, Bitmap?) -> Unit,
    onShareClick: (ActiveWindowEntry) -> Unit,
    onCloseClick: (ActiveWindowEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val mode = remember(workspaceState.snapMode) {
        SnapLayoutMode.fromId(workspaceState.snapMode)
    }
    val slotAssignments = workspaceState.slotAssignments
    val windows = workspaceState.windows

    fun findWindowForSlot(slotIndex: Int): ActiveWindowEntry? {
        val wid = slotAssignments[slotIndex]
        return if (wid != null) {
            windows.firstOrNull { it.id == wid }
        } else {
            // Fallback: slot index matches index in windows list
            windows.getOrNull(slotIndex)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val isLandscape = maxWidth > maxHeight

            when (mode) {
                SnapLayoutMode.SPLIT_TWO -> {
                    val ratio = (workspaceState.dividerRatios["d1"] ?: 0.5f).coerceIn(0.2f, 0.8f)
                    val win0 = findWindowForSlot(0) ?: windows.firstOrNull()
                    val win1 = findWindowForSlot(1) ?: windows.getOrNull(1)

                    if (isLandscape) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            StagePane(
                                window = win0,
                                tmpDir = tmpDir,
                                timestamp = workspaceState.previewTimestamp,
                                onWindowClick = onWindowClick,
                                onShareClick = onShareClick,
                                onCloseClick = onCloseClick,
                                modifier = Modifier
                                    .weight(ratio)
                                    .fillMaxHeight()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StagePane(
                                window = win1,
                                tmpDir = tmpDir,
                                timestamp = workspaceState.previewTimestamp,
                                onWindowClick = onWindowClick,
                                onShareClick = onShareClick,
                                onCloseClick = onCloseClick,
                                modifier = Modifier
                                    .weight(1f - ratio)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            StagePane(
                                window = win0,
                                tmpDir = tmpDir,
                                timestamp = workspaceState.previewTimestamp,
                                onWindowClick = onWindowClick,
                                onShareClick = onShareClick,
                                onCloseClick = onCloseClick,
                                modifier = Modifier
                                    .weight(ratio)
                                    .fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            StagePane(
                                window = win1,
                                tmpDir = tmpDir,
                                timestamp = workspaceState.previewTimestamp,
                                onWindowClick = onWindowClick,
                                onShareClick = onShareClick,
                                onCloseClick = onCloseClick,
                                modifier = Modifier
                                    .weight(1f - ratio)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }

                SnapLayoutMode.GRID_FOUR -> {
                    val win0 = findWindowForSlot(0) ?: windows.getOrNull(0)
                    val win1 = findWindowForSlot(1) ?: windows.getOrNull(1)
                    val win2 = findWindowForSlot(2) ?: windows.getOrNull(2)
                    val win3 = findWindowForSlot(3) ?: windows.getOrNull(3)

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            StagePane(
                                window = win0,
                                tmpDir = tmpDir,
                                timestamp = workspaceState.previewTimestamp,
                                onWindowClick = onWindowClick,
                                onShareClick = onShareClick,
                                onCloseClick = onCloseClick,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StagePane(
                                window = win1,
                                tmpDir = tmpDir,
                                timestamp = workspaceState.previewTimestamp,
                                onWindowClick = onWindowClick,
                                onShareClick = onShareClick,
                                onCloseClick = onCloseClick,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            StagePane(
                                window = win2,
                                tmpDir = tmpDir,
                                timestamp = workspaceState.previewTimestamp,
                                onWindowClick = onWindowClick,
                                onShareClick = onShareClick,
                                onCloseClick = onCloseClick,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StagePane(
                                window = win3,
                                tmpDir = tmpDir,
                                timestamp = workspaceState.previewTimestamp,
                                onWindowClick = onWindowClick,
                                onShareClick = onShareClick,
                                onCloseClick = onCloseClick,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }

                SnapLayoutMode.SPLIT_THREE -> {
                    val ratio = (workspaceState.dividerRatios["d1"] ?: 0.5f).coerceIn(0.2f, 0.8f)
                    val win0 = findWindowForSlot(0) ?: windows.getOrNull(0)
                    val win1 = findWindowForSlot(1) ?: windows.getOrNull(1)
                    val win2 = findWindowForSlot(2) ?: windows.getOrNull(2)

                    if (isLandscape) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            StagePane(
                                window = win0,
                                tmpDir = tmpDir,
                                timestamp = workspaceState.previewTimestamp,
                                onWindowClick = onWindowClick,
                                onShareClick = onShareClick,
                                onCloseClick = onCloseClick,
                                modifier = Modifier.weight(ratio).fillMaxHeight()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f - ratio).fillMaxHeight()) {
                                StagePane(
                                    window = win1,
                                    tmpDir = tmpDir,
                                    timestamp = workspaceState.previewTimestamp,
                                    onWindowClick = onWindowClick,
                                    onShareClick = onShareClick,
                                    onCloseClick = onCloseClick,
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                StagePane(
                                    window = win2,
                                    tmpDir = tmpDir,
                                    timestamp = workspaceState.previewTimestamp,
                                    onWindowClick = onWindowClick,
                                    onShareClick = onShareClick,
                                    onCloseClick = onCloseClick,
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            StagePane(
                                window = win0,
                                tmpDir = tmpDir,
                                timestamp = workspaceState.previewTimestamp,
                                onWindowClick = onWindowClick,
                                onShareClick = onShareClick,
                                onCloseClick = onCloseClick,
                                modifier = Modifier.weight(ratio).fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.weight(1f - ratio).fillMaxWidth()) {
                                StagePane(
                                    window = win1,
                                    tmpDir = tmpDir,
                                    timestamp = workspaceState.previewTimestamp,
                                    onWindowClick = onWindowClick,
                                    onShareClick = onShareClick,
                                    onCloseClick = onCloseClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StagePane(
                                    window = win2,
                                    tmpDir = tmpDir,
                                    timestamp = workspaceState.previewTimestamp,
                                    onWindowClick = onWindowClick,
                                    onShareClick = onShareClick,
                                    onCloseClick = onCloseClick,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                }

                else -> {
                    // SINGLE or UNLOCKED mode
                    val activeOrFirst = windows.firstOrNull { it.isActive } ?: windows.firstOrNull()
                    StagePane(
                        window = activeOrFirst,
                        tmpDir = tmpDir,
                        timestamp = workspaceState.previewTimestamp,
                        onWindowClick = onWindowClick,
                        onShareClick = onShareClick,
                        onCloseClick = onCloseClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Individual window preview pane inside the stage layout or stashed list.
 */
@Composable
fun StagePane(
    window: ActiveWindowEntry?,
    tmpDir: File,
    timestamp: Long,
    onWindowClick: (ActiveWindowEntry, Rect?, Bitmap?) -> Unit,
    onShareClick: (ActiveWindowEntry) -> Unit,
    onCloseClick: (ActiveWindowEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (window == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.active_session_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    val context = LocalContext.current
    var previewBitmap by remember(window.id) { mutableStateOf<Bitmap?>(null) }
    var paneBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(window.id, timestamp) {
        var bmp = WindowPreviewManager.loadPreview(tmpDir, window.id, timestamp)
        if (bmp == null) {
            delay(150.milliseconds)
            bmp = WindowPreviewManager.loadPreview(tmpDir, window.id, timestamp)
        }
        // Fallback to note document thumbnail if no live window capture exists yet
        if (bmp == null) {
            val targetPath = window.filePath ?: run {
                val f = dev.ilamparithi.aournalpp.runtime.ProcessSupervisor.resolveNoteFile(
                    dev.ilamparithi.aournalpp.runtime.LinuxEnvironment(context).getNotesDirectory(),
                    window.cleanTitle
                )
                f?.absolutePath
            }
            if (!targetPath.isNullOrBlank()) {
                val file = File(targetPath)
                if (file.exists()) {
                    val cached = ThumbnailManager.getCachedThumbnail(file)
                    if (cached != null) {
                        bmp = cached.asAndroidBitmap()
                    } else {
                        val created = ThumbnailManager.getOrCreateThumbnailBitmap(context, file, null)
                        if (created != null) {
                            bmp = created.asAndroidBitmap()
                        }
                    }
                }
            } else {
                val doc = DocumentRepository.getInstance(context).findNoteDocumentByTitle(window.cleanTitle)
                if (doc != null) {
                    val thumb = ThumbnailManager.getOrCreateThumbnailBitmap(context, doc.file, null, doc.lastModifiedMs)
                    if (thumb != null) {
                        bmp = thumb.asAndroidBitmap()
                    }
                }
            }
        }
        if (bmp != null) {
            previewBitmap = bmp
        }
    }

    val isActive = window.isActive
    val activeBorder = if (isActive) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        border = activeBorder,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 6.dp else 2.dp),
        modifier = modifier
            .onGloballyPositioned { coords ->
                paneBounds = coords.boundsInRoot()
            }
            .clickable {
                onWindowClick(window, paneBounds, previewBitmap)
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Window snapshot preview (strictly ContentScale.Crop)
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = window.cleanTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback placeholder canvas background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Unified Top Overlay Header: Title, dirty indicator, REC badge, Share and Close buttons
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = window.cleanTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee()
                        )
                        if (window.isDirty) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.tertiary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (window.isAudioRecording) {
                            AudioRecordingWaveformBadge()
                        }

                        IconButton(
                            onClick = { onShareClick(window) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.action_share_note),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onCloseClick(window) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pulsing red waveform indicator rendered on note cards actively recording audio.
 */
@Composable
fun AudioRecordingWaveformBadge(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWaveform")
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFD32F2F),
        contentColor = Color.White,
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((10 * bar1Height).coerceAtLeast(3f).dp)
                    .background(Color.White, RoundedCornerShape(1.dp))
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((10 * bar2Height).coerceAtLeast(3f).dp)
                    .background(Color.White, RoundedCornerShape(1.dp))
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((10 * bar3Height).coerceAtLeast(3f).dp)
                    .background(Color.White, RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = stringResource(R.string.badge_recording_audio),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
