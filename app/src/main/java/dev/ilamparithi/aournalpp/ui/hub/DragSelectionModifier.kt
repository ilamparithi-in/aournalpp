package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import dev.ilamparithi.aournalpp.model.NoteDocument
import kotlinx.coroutines.withTimeoutOrNull

fun Modifier.notesGridDragSelect(
    lazyGridState: LazyGridState,
    notes: () -> List<NoteDocument>,
    selectedPaths: () -> Set<String>,
    setSelectedPaths: (Set<String>) -> Unit,
    lastSelectedPath: () -> String?,
    setLastSelectedPath: (String?) -> Unit,
    isSelectionMode: () -> Boolean,
    setIsSelectionMode: (Boolean) -> Unit,
    setIsDragSelecting: (Boolean) -> Unit = {},
    setIsInitialEntryDrag: (Boolean) -> Unit = {},
    hapticFeedback: HapticFeedback,
    autoScrollThreshold: Float,
    setAutoScrollSpeed: (Float) -> Unit
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val downEvent = awaitPointerEvent(PointerEventPass.Initial)
            val down = downEvent.changes.firstOrNull { it.pressed } ?: continue
            val downId = down.id
            val downPos = down.position

            val initialList = notes()
            val initialPathMap = initialList.mapIndexed { idx, doc -> doc.path to idx }.toMap()

            fun findNotePathAt(point: Offset): String? {
                val rounded = point.round()
                val match = lazyGridState.layoutInfo.visibleItemsInfo.find { itemInfo ->
                    itemInfo.size.toIntRect().contains(rounded - itemInfo.offset)
                }
                val key = match?.key as? String ?: return null
                return if (initialPathMap.containsKey(key)) key else null
            }

            val hitPath = findNotePathAt(downPos) ?: continue

            var isLongPressed = false
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis
            val touchSlop = viewConfiguration.touchSlop
            val pointerType = down.type
            val effectiveSlop = if (pointerType == PointerType.Stylus || pointerType == PointerType.Eraser) {
                touchSlop * 2.5f
            } else {
                touchSlop
            }

            val dragCancelled = withTimeoutOrNull(longPressTimeout) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == downId } ?: break
                    if (!change.pressed) {
                        return@withTimeoutOrNull true
                    }
                    val dist = (change.position - downPos).getDistance()
                    if (dist > effectiveSlop) {
                        return@withTimeoutOrNull true
                    }
                }
                true
            }

            if (dragCancelled == null) {
                isLongPressed = true
            }

            if (isLongPressed) {
                val activeSelection = isSelectionMode()
                val isFirstEntry = !activeSelection
                if (isFirstEntry) {
                    setIsInitialEntryDrag(true)
                }
                setIsDragSelecting(true)
                val currentNotesList = initialList
                val pathToIndex = initialPathMap
                val currentSelected = selectedPaths()

                val wasSelected = currentSelected.contains(hitPath)
                val initialPath = hitPath
                var lastReportedPath = hitPath
                val baseSnapshot: Set<String>

                if (activeSelection && !wasSelected && currentSelected.isNotEmpty()) {
                    // Shift-click range selection when long-pressing a deselected item in selection mode
                    val anchorPath = lastSelectedPath() ?: currentSelected.lastOrNull()
                    val anchorIdx = pathToIndex[anchorPath] ?: -1
                    val hitIdx = pathToIndex[hitPath] ?: -1

                    val rangePaths = if (anchorIdx >= 0 && hitIdx >= 0) {
                        val start = minOf(anchorIdx, hitIdx)
                        val end = maxOf(anchorIdx, hitIdx)
                        currentNotesList.subList(start, end + 1).map { it.path }.toSet()
                    } else {
                        setOf(hitPath)
                    }

                    val newSelection = currentSelected + rangePaths
                    setSelectedPaths(newSelection)
                    setLastSelectedPath(hitPath)
                    baseSnapshot = newSelection
                } else {
                    // Standard selection mode entry / start drag
                    setIsSelectionMode(true)
                    val newSelection = currentSelected + hitPath
                    setSelectedPaths(newSelection)
                    setLastSelectedPath(hitPath)
                    baseSnapshot = currentSelected
                }

                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                try {
                    while (true) {
                        val dragEvent = awaitPointerEvent(PointerEventPass.Initial)
                        val dragChange = dragEvent.changes.firstOrNull { it.id == downId } ?: break
                        if (!dragChange.pressed) {
                            dragChange.consume()
                            break
                        }
                        dragChange.consume()
                        val currentDragPos = dragChange.position

                        val viewportHeight = lazyGridState.layoutInfo.viewportSize.height
                        val distFromBottom = viewportHeight - currentDragPos.y
                        val distFromTop = currentDragPos.y
                        setAutoScrollSpeed(
                            when {
                                distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                                distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                                else -> 0f
                            }
                        )

                        val currentHit = findNotePathAt(currentDragPos)
                        if (currentHit != null && currentHit != lastReportedPath) {
                            val initialIdx = pathToIndex[initialPath] ?: -1
                            val currentIdx = pathToIndex[currentHit] ?: -1

                            if (initialIdx >= 0 && currentIdx >= 0) {
                                val start = minOf(initialIdx, currentIdx)
                                val end = maxOf(initialIdx, currentIdx)
                                val dragRange = currentNotesList.subList(start, end + 1).map { it.path }.toSet()

                                val updatedSelection = baseSnapshot + dragRange
                                setSelectedPaths(updatedSelection)
                                setLastSelectedPath(currentHit)
                                lastReportedPath = currentHit
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                } finally {
                    setAutoScrollSpeed(0f)
                    setIsDragSelecting(false)
                    setIsInitialEntryDrag(false)
                }
            }
        }
    }
}
