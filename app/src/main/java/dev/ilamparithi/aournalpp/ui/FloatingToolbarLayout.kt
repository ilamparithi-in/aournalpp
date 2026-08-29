package dev.ilamparithi.aournalpp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive layout for the Floating Toolbar that displays controls in a unified row,
 * wrapping smoothly on smaller screens while guaranteeing that trailing controls (Pin/Collapse & Drag Handle)
 * are always right-aligned when overflowing.
 */
@Composable
fun FloatingToolbarLayout(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 4.dp,
    verticalSpacing: Dp = 4.dp,
    mainContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        contents = listOf(mainContent, trailingContent)
    ) { (mainMeasurables, trailingMeasurables), constraints ->
        val hSpacingPx = horizontalSpacing.roundToPx()
        val vSpacingPx = verticalSpacing.roundToPx()
        val maxW = constraints.maxWidth

        val trailingPlaceables = trailingMeasurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val trailingTotalWPx = trailingPlaceables.sumOf { it.width } + if (trailingPlaceables.isNotEmpty()) (trailingPlaceables.size - 1) * hSpacingPx else 0
        val trailingMaxHPx = trailingPlaceables.maxOfOrNull { it.height } ?: 0

        val mainPlaceables = mainMeasurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }

        val rows = mutableListOf<MutableList<Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()

        var currentRow = mutableListOf<Placeable>()
        var currentW = 0
        var currentH = 0

        for (placeable in mainPlaceables) {
            val neededW = if (currentRow.isEmpty()) placeable.width else currentW + hSpacingPx + placeable.width
            if (currentRow.isNotEmpty() && neededW > maxW) {
                rows.add(currentRow)
                rowWidths.add(currentW)
                rowHeights.add(currentH)
                currentRow = mutableListOf(placeable)
                currentW = placeable.width
                currentH = placeable.height
            } else {
                currentRow.add(placeable)
                currentW = neededW
                currentH = maxOf(currentH, placeable.height)
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentW)
            rowHeights.add(currentH)
        }

        val lastRowIndex = if (rows.isNotEmpty()) rows.size - 1 else 0
        val isSingleRow = rows.size <= 1 && (if (rows.isNotEmpty()) rowWidths[0] + hSpacingPx + trailingTotalWPx else trailingTotalWPx) <= maxW

        val finalRowWidths = rowWidths.toMutableList()
        val finalRowHeights = rowHeights.toMutableList()

        var trailingOnLastRow = false
        if (rows.isNotEmpty()) {
            val neededOnLastRow = rowWidths[lastRowIndex] + hSpacingPx + trailingTotalWPx
            if (neededOnLastRow <= maxW) {
                trailingOnLastRow = true
                finalRowWidths[lastRowIndex] = maxOf(finalRowWidths[lastRowIndex] + hSpacingPx + trailingTotalWPx, neededOnLastRow)
                finalRowHeights[lastRowIndex] = maxOf(finalRowHeights[lastRowIndex], trailingMaxHPx)
            } else {
                finalRowWidths.add(trailingTotalWPx)
                finalRowHeights.add(trailingMaxHPx)
            }
        } else {
            finalRowWidths.add(trailingTotalWPx)
            finalRowHeights.add(trailingMaxHPx)
        }

        val totalLayoutWidth = finalRowWidths.maxOrNull()?.coerceIn(constraints.minWidth, constraints.maxWidth) ?: 0
        val totalLayoutHeight = (finalRowHeights.sum() + (finalRowHeights.size - 1) * vSpacingPx).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(totalLayoutWidth, totalLayoutHeight) {
            var currentY = 0
            for (rIndex in rows.indices) {
                val rowItems = rows[rIndex]
                val rHeight = finalRowHeights[rIndex]
                var currentX = 0
                for (p in rowItems) {
                    val yOffset = currentY + (rHeight - p.height) / 2
                    p.placeRelative(currentX, yOffset)
                    currentX += p.width + hSpacingPx
                }

                if (trailingOnLastRow && rIndex == lastRowIndex) {
                    var trailX = if (isSingleRow) {
                        currentX
                    } else {
                        totalLayoutWidth - trailingTotalWPx
                    }
                    for (tp in trailingPlaceables) {
                        val yOffset = currentY + (rHeight - tp.height) / 2
                        tp.placeRelative(trailX, yOffset)
                        trailX += tp.width + hSpacingPx
                    }
                }
                currentY += rHeight + vSpacingPx
            }

            if (!trailingOnLastRow) {
                val rHeight = finalRowHeights.last()
                var trailX = totalLayoutWidth - trailingTotalWPx
                for (tp in trailingPlaceables) {
                    val yOffset = currentY + (rHeight - tp.height) / 2
                    tp.placeRelative(trailX, yOffset)
                    trailX += tp.width + hSpacingPx
                }
            }
        }
    }
}
