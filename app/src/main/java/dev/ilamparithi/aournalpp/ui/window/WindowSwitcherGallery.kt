package dev.ilamparithi.aournalpp.ui.window

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.ilamparithi.aournalpp.ui.preview.DreamyStarsBackground
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor

private val M3MorphEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * Material 3 Window Switcher Gallery overlay.
 *
 * Triggered on long-pressing the window switcher button in the floating toolbar.
 * - Stays open after the finger is released.
 * - Displays horizontal cards for all open windows.
 * - Shows cached preview snapshots without blocking the UI thread or animation.
 * - Includes a small Material 3 Close ('X') button on each window card.
 * - Highlights the currently active window.
 * - Tapping a card switches to that window.
 * - Tapping the background scrim or pressing Back dismisses the gallery.
 */
@Composable
fun WindowSwitcherGallery(
    windows: List<ProcessSupervisor.X11WindowInfo>,
    previewCache: Map<String, Bitmap>,
    originOffset: Offset = Offset.Unspecified,
    onSelectWindow: (ProcessSupervisor.X11WindowInfo) -> Unit,
    onCloseWindow: (ProcessSupervisor.X11WindowInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val morphProgress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val isLandscape = config.screenWidthDp > config.screenHeightDp

    LaunchedEffect(Unit) {
        morphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 320, easing = M3MorphEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(200f)
            .background(Color.Black.copy(alpha = (0.6f * morphProgress.value).coerceIn(0f, 0.6f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Dreamy stars background with reduced strength (0.65f)
        DreamyStarsBackground(
            modifier = Modifier.fillMaxSize(),
            accentColor = MaterialTheme.colorScheme.primary,
            progress = morphProgress.value,
            alpha = morphProgress.value,
            strength = 0.65f
        )

        Column(
            modifier = Modifier
                .graphicsLayer {
                    val p = morphProgress.value
                    scaleX = 0.88f + 0.12f * p
                    scaleY = 0.88f + 0.12f * p
                    alpha = p.coerceIn(0f, 1f)
                }
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title & Window Count Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Open Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "${windows.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Carousel / Row of Window Preview Cards
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(windows, key = { it.id }) { win ->
                    val previewBitmap = previewCache[win.id]
                    WindowPreviewCard(
                        windowInfo = win,
                        preview = previewBitmap,
                        isLandscape = isLandscape,
                        onSelect = { onSelectWindow(win) },
                        onClose = { onCloseWindow(win) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowPreviewCard(
    windowInfo: ProcessSupervisor.X11WindowInfo,
    preview: Bitmap?,
    isLandscape: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val cardWidth = if (isLandscape) 280.dp else 230.dp
    val previewHeight = if (isLandscape) 175.dp else 220.dp
    val isActive = windowInfo.isActive

    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    val borderWidth = if (isActive) 2.5.dp else 1.dp

    Surface(
        modifier = Modifier
            .width(cardWidth)
            .shadow(
                elevation = if (isActive) 12.dp else 4.dp,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (isActive) 6.dp else 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header Bar: Icon, Title, and Small Close ('X') Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = windowInfo.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Small Material 3 Close Button with Red Hover Highlight
                val closeInteractionSource = remember { MutableInteractionSource() }
                val isCloseHovered by closeInteractionSource.collectIsHoveredAsState()
                val closeBgColor by animateColorAsState(
                    targetValue = if (isCloseHovered) Color(0xFFD32F2F) else Color.Transparent,
                    label = "closeHoverBg"
                )
                val closeIconTint by animateColorAsState(
                    targetValue = if (isCloseHovered) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    label = "closeHoverTint"
                )

                Surface(
                    shape = CircleShape,
                    color = closeBgColor,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .hoverable(interactionSource = closeInteractionSource)
                        .clickable(
                            interactionSource = closeInteractionSource,
                            indication = ripple(bounded = true, color = Color.Red)
                        ) { onClose() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close ${windowInfo.title}",
                            modifier = Modifier.size(14.dp),
                            tint = closeIconTint
                        )
                    }
                }
            }

            // Card Body: Window Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null && !preview.isRecycled) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "Preview of ${windowInfo.title}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    // Shimmer / Placeholder Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Preview Loading...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
