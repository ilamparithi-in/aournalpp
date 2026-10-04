package dev.ilamparithi.aournalpp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import dev.ilamparithi.aournalpp.ui.animation.AppAnimationSpecs

/**
 * Canonical Floating Toolbar Component.
 *
 * Renders the floating toolbar surface with exact styling, dimensions, drop shadows, and layout
 * wrapping identical to the in-canvas [FloatingToolbarOverlay]. Shared across canvas, preview,
 * position editor, and settings.
 */
@Composable
fun FloatingToolbarBar(
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    showClose: Boolean = true,
    showWindowSwitcher: Boolean = true,
    showSnapLayouts: Boolean = true,
    showTitle: Boolean = true,
    displayTitle: String = stringResource(R.string.pref_toolbar_sample_filename),
    titleIcon: ImageVector = Icons.Default.Description,
    showStylusClickOverride: Boolean = false,
    stylusClickMode: Int = 1,
    onStylusClickModeChange: ((Int) -> Unit)? = null,
    showTouchStylus: Boolean = true,
    isFingerAsStylus: Boolean = false,
    onToggleFingerAsStylus: (() -> Unit)? = null,
    showCut: Boolean = false,
    showCopy: Boolean = false,
    showPaste: Boolean = false,
    showImage: Boolean = false,
    showSync: Boolean = true,
    showKeyboard: Boolean = true,
    isKeyboardOpen: Boolean = false,
    onToggleKeyboard: (() -> Unit)? = null,
    pinButtonMode: Boolean = true,
    isPinned: Boolean = false,
    onTogglePinOrCollapse: (() -> Unit)? = null,
    showDragHandle: Boolean = true,
    openWindowCount: Int = 1,
    maxLayoutWidth: Dp = Dp.Unspecified,
    borderColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    onSmartBackPress: (() -> Unit)? = null,
    onCloseWindow: (() -> Unit)? = null,
    onQuickSwitchWindow: (() -> Unit)? = null,
    onOpenImageSelector: (() -> Unit)? = null,
    onInjectShortcut: ((Int, String) -> Unit)? = null,
    onSyncNow: (() -> Unit)? = null,
    isSyncing: Boolean = false
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 6.dp
    ) {
        val layoutModifier = if (maxLayoutWidth != Dp.Unspecified) {
            Modifier
                .widthIn(max = maxLayoutWidth)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        } else {
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        }

        FloatingToolbarLayout(
            modifier = layoutModifier,
            mainContent = {
                if (showBack) {
                    IconButton(
                        onClick = { onSmartBackPress?.invoke() },
                        enabled = onSmartBackPress != null,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.toolbar_cd_back),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (showClose) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (onCloseWindow != null) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onCloseWindow() }
                                } else Modifier
                            )
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.toolbar_cd_close),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (showWindowSwitcher) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (onQuickSwitchWindow != null) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onQuickSwitchWindow() }
                                } else Modifier
                            )
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    if (openWindowCount > 1) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(
                                                text = "$openWindowCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = stringResource(R.string.toolbar_cd_switcher),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                if (showSnapLayouts) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = stringResource(R.string.toolbar_cd_snap),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (showTitle) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = titleIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        InteractiveMarqueeText(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            minWidth = 90.dp,
                            maxWidth = 220.dp
                        )
                    }
                }

                if (showStylusClickOverride) {
                    val modes = listOf(1 to "L", 2 to "M", 4 to "R")
                    val selectedIndex = when (stylusClickMode) {
                        2 -> 1
                        4 -> 2
                        else -> 0
                    }

                    val itemWidth = 26.dp
                    val itemHeight = 24.dp
                    val spacing = 2.dp
                    val padding = 2.dp

                    val m3MorphEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) }
                    val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
                        targetValue = (itemWidth + spacing) * selectedIndex,
                        animationSpec = tween(
                            durationMillis = 240,
                            easing = m3MorphEasing
                        ),
                        label = "StylusIndicatorOffset"
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Box(modifier = Modifier.padding(padding)) {
                            Surface(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .size(itemWidth, itemHeight),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 1.dp
                            ) {}

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(spacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                modes.forEach { (modeValue, label) ->
                                    val isSelected = stylusClickMode == modeValue
                                    val textColor by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        animationSpec = AppAnimationSpecs.springColor(),
                                        label = "StylusTextColor"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(itemWidth, itemHeight)
                                            .clip(RoundedCornerShape(8.dp))
                                            .then(
                                                if (onStylusClickModeChange != null) {
                                                    Modifier.clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { onStylusClickModeChange(modeValue) }
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showTouchStylus) {
                    val activeBgColor by animateColorAsState(
                        targetValue = if (isFingerAsStylus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        animationSpec = AppAnimationSpecs.springColor(),
                        label = "TouchStylusBgColor"
                    )
                    val activeIconColor by animateColorAsState(
                        targetValue = if (isFingerAsStylus) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = AppAnimationSpecs.springColor(),
                        label = "TouchStylusIconColor"
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = activeBgColor,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (onToggleFingerAsStylus != null) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onToggleFingerAsStylus() }
                                } else Modifier
                            )
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = if (isFingerAsStylus) {
                                    stringResource(R.string.toolbar_cd_touch_stylus_enabled)
                                } else {
                                    stringResource(R.string.toolbar_cd_touch_stylus_disabled)
                                },
                                modifier = Modifier.size(17.dp),
                                tint = activeIconColor
                            )
                        }
                    }
                }

                if (showCut || showCopy || showPaste || showImage) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            if (showCut) {
                                IconButton(
                                    onClick = { onInjectShortcut?.invoke(android.view.KeyEvent.KEYCODE_X, "ctrl+x") },
                                    enabled = onInjectShortcut != null,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCut,
                                        contentDescription = stringResource(R.string.toolbar_cd_cut),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (showCopy) {
                                IconButton(
                                    onClick = { onInjectShortcut?.invoke(android.view.KeyEvent.KEYCODE_C, "ctrl+c") },
                                    enabled = onInjectShortcut != null,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = stringResource(R.string.toolbar_cd_copy),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (showPaste) {
                                IconButton(
                                    onClick = { onInjectShortcut?.invoke(android.view.KeyEvent.KEYCODE_V, "ctrl+v") },
                                    enabled = onInjectShortcut != null,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = stringResource(R.string.toolbar_cd_paste),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (showImage) {
                                IconButton(
                                    onClick = { onOpenImageSelector?.invoke() },
                                    enabled = onOpenImageSelector != null,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = stringResource(R.string.toolbar_cd_image),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSync) {
                    val syncBgColor by animateColorAsState(
                        targetValue = if (isSyncing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        animationSpec = AppAnimationSpecs.springColor(),
                        label = "SyncBgColor"
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = syncBgColor,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (onSyncNow != null && !isSyncing) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onSyncNow() }
                                } else Modifier
                            )
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Crossfade(
                                targetState = isSyncing,
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                label = "SyncButtonState"
                            ) { syncing ->
                                if (syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        strokeCap = StrokeCap.Round,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = stringResource(R.string.toolbar_cd_sync),
                                        modifier = Modifier.size(17.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (showKeyboard) {
                    val activeBgColor by animateColorAsState(
                        targetValue = if (isKeyboardOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        animationSpec = AppAnimationSpecs.springColor(),
                        label = "KeyboardBgColor"
                    )
                    val activeIconColor by animateColorAsState(
                        targetValue = if (isKeyboardOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = AppAnimationSpecs.springColor(),
                        label = "KeyboardIconColor"
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = activeBgColor,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (onToggleKeyboard != null) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onToggleKeyboard() }
                                } else Modifier
                            )
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = if (isKeyboardOpen) {
                                    stringResource(R.string.toolbar_cd_keyboard_hide)
                                } else {
                                    stringResource(R.string.toolbar_cd_keyboard_show)
                                },
                                modifier = Modifier.size(17.dp),
                                tint = activeIconColor
                            )
                        }
                    }
                }
            },
            trailingContent = {
                if (pinButtonMode) {
                    IconButton(
                        onClick = { onTogglePinOrCollapse?.invoke() },
                        enabled = onTogglePinOrCollapse != null,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) {
                                stringResource(R.string.toolbar_cd_unpin)
                            } else {
                                stringResource(R.string.toolbar_cd_pin)
                            },
                            modifier = Modifier.size(20.dp),
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    IconButton(
                        onClick = { onTogglePinOrCollapse?.invoke() },
                        enabled = onTogglePinOrCollapse != null,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = stringResource(R.string.toolbar_cd_collapse),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showDragHandle) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = stringResource(R.string.toolbar_cd_drag_handle),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    }
}
