package dev.ilamparithi.aournalpp.ui.util

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

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
            state = rememberTooltipState(),
            modifier = modifier
        ) {
            content()
        }
    }
}

/**
 * Standard Material 3 IconButton with integrated hover and long-press tooltip,
 * and WCAG 48dp minimum touch target enforcement.
 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    AppTooltipBox(tooltipText = tooltip) {
        IconButton(
            onClick = onClick,
            modifier = modifier.minTouchTarget(),
            enabled = enabled,
            interactionSource = interactionSource,
            colors = colors,
            content = content
        )
    }
}

/**
 * Material 3 FilledTonalIconButton with integrated tooltip and minimum touch target.
 */
@Composable
fun AppFilledTonalIconButton(
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    interactionSource: MutableInteractionSource? = null,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    content: @Composable () -> Unit
) {
    AppTooltipBox(tooltipText = tooltip) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier.minTouchTarget(),
            enabled = enabled,
            shape = shape,
            interactionSource = interactionSource,
            colors = colors,
            content = content
        )
    }
}

