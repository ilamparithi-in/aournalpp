package dev.ilamparithi.aournalpp.ui.util

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Material 3 Expressive Tooltip wrapper that displays a tooltip upon:
 * 1. Pointer / mouse / stylus hover
 * 2. Long press on touch / stylus
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTooltipBox(
    tooltipText: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (tooltipText.isBlank()) {
        content()
    } else {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(tooltipText)
                }
            },
            state = rememberTooltipState(isPersistent = false),
            modifier = modifier
        ) {
            content()
        }
    }
}

/**
 * Standard Material 3 IconButton with integrated hover and long-press tooltip,
 * haptic vibration, toast fallback, and WCAG 48dp minimum touch target enforcement.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(isPersistent = false)

    val handleLongClick: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope.launch {
            try {
                tooltipState.show()
            } catch (_: Exception) {}
        }
        if (tooltip.isNotBlank()) {
            Toast.makeText(context, tooltip, Toast.LENGTH_SHORT).show()
        }
        onLongClick?.invoke()
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            if (tooltip.isNotBlank()) {
                PlainTooltip {
                    Text(tooltip)
                }
            }
        },
        state = tooltipState
    ) {
        Surface(
            modifier = modifier
                .minTouchTarget()
                .semantics {
                    role = Role.Button
                    if (tooltip.isNotBlank()) {
                        this.contentDescription = tooltip
                    }
                }
                .combinedClickable(
                    enabled = true,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = 20.dp),
                    onClick = { if (enabled) onClick() },
                    onLongClick = handleLongClick
                ),
            shape = CircleShape,
            color = if (enabled) colors.containerColor else colors.disabledContainerColor,
            contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

/**
 * Material 3 FilledTonalIconButton with integrated tooltip, haptic vibration,
 * long-press toast description, custom shape support, and minimum touch target.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppFilledTonalIconButton(
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    interactionSource: MutableInteractionSource? = null,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(isPersistent = false)

    val handleLongClick: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope.launch {
            try {
                tooltipState.show()
            } catch (_: Exception) {}
        }
        if (tooltip.isNotBlank()) {
            Toast.makeText(context, tooltip, Toast.LENGTH_SHORT).show()
        }
        onLongClick?.invoke()
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            if (tooltip.isNotBlank()) {
                PlainTooltip {
                    Text(tooltip)
                }
            }
        },
        state = tooltipState
    ) {
        Surface(
            modifier = modifier
                .minTouchTarget()
                .semantics {
                    role = Role.Button
                    if (tooltip.isNotBlank()) {
                        this.contentDescription = tooltip
                    }
                }
                .combinedClickable(
                    enabled = true,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = { if (enabled) onClick() },
                    onLongClick = handleLongClick
                ),
            shape = shape,
            color = if (enabled) colors.containerColor else colors.disabledContainerColor,
            contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

