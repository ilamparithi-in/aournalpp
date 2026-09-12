package dev.ilamparithi.aournalpp.ui.canvas

import android.graphics.BitmapFactory
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import dev.ilamparithi.aournalpp.ui.animation.AppAnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.ilamparithi.aournalpp.ui.FloatingToolbarLayout
import dev.ilamparithi.aournalpp.ui.SafeAreaInsets
import dev.ilamparithi.aournalpp.ui.animation.AppAnimationSpecs
import dev.ilamparithi.aournalpp.ui.animation.LocalMotionPreferences
import dev.ilamparithi.aournalpp.ui.animation.SpringSlideTransition
import dev.ilamparithi.aournalpp.ui.rememberCutoutPlacement
import dev.ilamparithi.aournalpp.ui.snap.SnapLayoutMode
import dev.ilamparithi.aournalpp.ui.snap.SnapLayoutToolbarButton
import dev.ilamparithi.aournalpp.utils.WindowPreviewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TemporarySaveCard(
    file: File,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = file.path) {
        value = withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                val targetSize = 256
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > targetSize || bounds.outHeight / sampleSize > targetSize) {
                    sampleSize *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bm = BitmapFactory.decodeFile(file.absolutePath, opts)
                bm?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    val timeFormatted = remember(file.lastModified()) {
        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        sdf.format(Date(file.lastModified()))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = modifier
            .width(110.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val bitmap = bitmapState.value
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onDelete() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete temporary image",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingToolbarOverlay(
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    defaultNormX: Float,
    defaultNormY: Float,
    centerTopBarWithinBounds: Boolean,
    effectiveInsets: SafeAreaInsets,
    systemBarPadding: PaddingValues,
    isFullscreen: Boolean,
    displayTitle: String,
    windowIcon: ImageVector,
    windowIndex: Int = 0,
    startCollapsed: Boolean,
    pinButtonMode: Boolean,
    autoCollapseTimeoutMs: Int,
    showStylusClickOverride: Boolean,
    showTouchStylus: Boolean,
    isFingerAsStylus: Boolean,
    onToggleFingerAsStylus: () -> Unit,
    stylusClickMode: Int,
    onStylusClickModeChange: (Int) -> Unit,
    showTitle: Boolean,
    showBack: Boolean,
    showClose: Boolean,
    showWindowSwitcher: Boolean = true,
    showSnapLayouts: Boolean = true,
    activeSnapMode: SnapLayoutMode = SnapLayoutMode.SINGLE,
    isSnapMirrored: Boolean = false,
    onSelectSnapMode: (SnapLayoutMode, Boolean) -> Unit = { _, _ -> },
    onToggleSnapMirror: () -> Unit = {},
    openWindowCount: Int = 1,
    onQuickSwitchWindow: () -> Unit = {},
    onOpenWindowGallery: () -> Unit = {},
    showKeyboard: Boolean,
    showDragHandle: Boolean,
    showCut: Boolean,
    showCopy: Boolean,
    showPaste: Boolean,
    showImage: Boolean,
    onOpenImageSelector: () -> Unit,
    stylusHoverExpands: Boolean,
    onSmartBackPress: () -> Unit,
    onCloseWindow: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onInjectShortcut: (Int, String) -> Unit,
    isKeyboardOpen: Boolean
) {
    val m3MorphEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) }
    val cutoutPlacement = rememberCutoutPlacement()
    val density = LocalDensity.current
    val cutoutTopOffsetDp = with(density) { cutoutPlacement.topOffsetPx.toDp() }

    var isHeaderExpanded by rememberSaveable { mutableStateOf(!startCollapsed) }
    var isPinned by rememberSaveable { mutableStateOf(false) }
    var lastCollapseTimeMs by remember { mutableLongStateOf(0L) }

    val toolbarResizeDebounceMs = remember(autoCollapseTimeoutMs) {
        WindowPreviewUtils.calculateToolbarResizeDebounceMs(autoCollapseTimeoutMs)
    }
    var altTabClickCount by remember { mutableIntStateOf(0) }
    var isAltTabDebouncing by remember { mutableStateOf(false) }
    var heldDebounceWidthDp by remember { mutableStateOf(0.dp) }

    val textMeasurer = rememberTextMeasurer()
    val titleTextStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
    val currentTitleNaturalWidthDp = remember(displayTitle, density) {
        val measuredPx = textMeasurer.measure(
            text = displayTitle,
            style = titleTextStyle,
            maxLines = 1,
            softWrap = false
        ).size.width
        with(density) { measuredPx.toDp() }.coerceIn(90.dp, 220.dp) + 32.dp
    }
    val maxSafeTitleWidthDp = with(density) {
        (canvasWidthPx - 32.dp.toPx()).coerceAtLeast(0f).toDp()
    }
    val safeCurrentTitleWidthDp = minOf(currentTitleNaturalWidthDp, maxSafeTitleWidthDp)

    LaunchedEffect(altTabClickCount) {
        if (altTabClickCount > 0) {
            isAltTabDebouncing = true
            delay(toolbarResizeDebounceMs.milliseconds)
            isAltTabDebouncing = false
            heldDebounceWidthDp = 0.dp
        }
    }

    LaunchedEffect(isAltTabDebouncing, safeCurrentTitleWidthDp) {
        if (isAltTabDebouncing) {
            heldDebounceWidthDp = maxOf(heldDebounceWidthDp, safeCurrentTitleWidthDp)
        }
    }

    LaunchedEffect(openWindowCount) {
        if (openWindowCount <= 1) {
            isAltTabDebouncing = false
            heldDebounceWidthDp = 0.dp
        }
    }

    val targetTitleWidthDp = if (isAltTabDebouncing) {
        minOf(maxOf(heldDebounceWidthDp, safeCurrentTitleWidthDp), maxSafeTitleWidthDp)
    } else {
        safeCurrentTitleWidthDp
    }

    val animatedTitleWidthDp by animateDpAsState(
        targetValue = targetTitleWidthDp,
        animationSpec = AppAnimationSpecs.springDp(
            dampingRatio = SpringSlideTransition.SLIDE_DAMPING,
            stiffness = SpringSlideTransition.SLIDE_STIFFNESS
        ),
        label = "ToolbarTitleWidthSpring"
    )

    val interactionSignal = remember {
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    var isSnapDropdownOpen by remember { mutableStateOf(false) }

    LaunchedEffect(isHeaderExpanded) {
        if (!isHeaderExpanded) {
            isSnapDropdownOpen = false
        }
    }

    LaunchedEffect(isHeaderExpanded, isPinned, pinButtonMode, autoCollapseTimeoutMs, isSnapDropdownOpen) {
        if (pinButtonMode && isHeaderExpanded && !isPinned && !isSnapDropdownOpen) {
            while (isActive) {
                val triggered = withTimeoutOrNull(autoCollapseTimeoutMs.milliseconds) {
                    interactionSignal.first()
                }
                if (triggered == null) {
                    if (isHeaderExpanded && !isPinned && !isSnapDropdownOpen) {
                        lastCollapseTimeMs = SystemClock.uptimeMillis()
                        isHeaderExpanded = false
                    }
                    break
                }
            }
        }
    }

    var dragNormX by remember { mutableFloatStateOf(defaultNormX) }
    var dragNormY by remember { mutableFloatStateOf(defaultNormY) }
    val animPixelOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isMovedFromDefault by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var toolbarSizePx by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .zIndex(100f)
            .offset {
                val tWidthPx = if (toolbarSizePx.width > 0) toolbarSizePx.width.toFloat() else 320.dp.toPx()
                val tHeightPx = if (toolbarSizePx.height > 0) toolbarSizePx.height.toFloat() else 48.dp.toPx()

                val minX: Float
                val maxX: Float
                val minY: Float
                val maxY: Float

                if (centerTopBarWithinBounds) {
                    minX = effectiveInsets.left.dp.toPx() + systemBarPadding.calculateStartPadding(LayoutDirection.Ltr)
                        .toPx() + 8.dp.toPx()
                    maxX = maxOf(
                        minX,
                        canvasWidthPx - tWidthPx - effectiveInsets.right.dp.toPx() - systemBarPadding.calculateEndPadding(
                            LayoutDirection.Ltr
                        ).toPx() - 8.dp.toPx()
                    )
                    minY = effectiveInsets.top.dp.toPx() + systemBarPadding.calculateTopPadding().toPx() + 8.dp.toPx()
                    maxY = maxOf(
                        minY,
                        canvasHeightPx - tHeightPx - effectiveInsets.bottom.dp.toPx() - systemBarPadding.calculateBottomPadding()
                            .toPx() - 8.dp.toPx()
                    )
                } else {
                    minX = 8.dp.toPx()
                    maxX = maxOf(minX, canvasWidthPx - tWidthPx - 8.dp.toPx())
                    minY = if (isFullscreen) {
                        if (cutoutPlacement.hasCenterCutout) cutoutTopOffsetDp.toPx() + 8.dp.toPx() else 8.dp.toPx()
                    } else {
                        systemBarPadding.calculateTopPadding().toPx() + 8.dp.toPx()
                    }
                    maxY = maxOf(minY, canvasHeightPx - tHeightPx - 8.dp.toPx())
                }

                val basePosX = minX + (maxX - minX) * dragNormX
                val basePosY = minY + (maxY - minY) * dragNormY
                val posX = (basePosX + animPixelOffset.value.x).roundToInt()
                val posY = (basePosY + animPixelOffset.value.y).roundToInt()
                IntOffset(posX, posY)
            }
            .onSizeChanged { size ->
                if (toolbarSizePx != size) {
                    toolbarSizePx = size
                }
            }
    ) {
        Surface(
            modifier = Modifier
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 6.dp
        ) {
            val reduceMotion = LocalMotionPreferences.current.reduceAnimations
            AnimatedContent(
                targetState = isHeaderExpanded,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(animationSpec = snap()) togetherWith
                                fadeOut(animationSpec = snap()) using
                                SizeTransform(clip = true, sizeAnimationSpec = { _, _ -> snap() })
                    } else {
                        fadeIn(animationSpec = tween(durationMillis = 180, easing = m3MorphEasing)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 120, easing = m3MorphEasing)) using
                                SizeTransform(
                                    clip = true,
                                    sizeAnimationSpec = { _, _ -> tween(durationMillis = 300, easing = m3MorphEasing) }
                                )
                    }
                },
                contentAlignment = Alignment.Center,
                label = "HeaderMorphTransition"
            ) { expanded ->
                if (expanded) {
                    FloatingToolbarLayout(
                        modifier = Modifier
                            .widthIn(max = with(density) { (canvasWidthPx - 16.dp.toPx()).toDp() })
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { interactionSignal.tryEmit(Unit) },
                        mainContent = {
                            if (showBack) {
                                IconButton(
                                    onClick = {
                                        interactionSignal.tryEmit(Unit)
                                        onSmartBackPress()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Return to Home",
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
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            interactionSignal.tryEmit(Unit)
                                            try {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } catch (_: Exception) {
                                            }
                                            onCloseWindow()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Note (Ctrl+Q)",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            if (showWindowSwitcher && openWindowCount > 1) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                interactionSignal.tryEmit(Unit)
                                                try {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                } catch (_: Exception) {
                                                }
                                                heldDebounceWidthDp =
                                                    maxOf(heldDebounceWidthDp, safeCurrentTitleWidthDp)
                                                altTabClickCount++
                                                onQuickSwitchWindow()
                                            },
                                            onLongClick = {
                                                interactionSignal.tryEmit(Unit)
                                                try {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                } catch (_: Exception) {
                                                }
                                                onOpenWindowGallery()
                                            }
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
                                                contentDescription = "Switch Note Window (Tap to cycle, Long press for gallery)",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            if (showSnapLayouts && openWindowCount > 1) {
                                SnapLayoutToolbarButton(
                                    activeMode = activeSnapMode,
                                    isMirrored = isSnapMirrored,
                                    openWindowCount = openWindowCount,
                                    isMenuOpen = isSnapDropdownOpen,
                                    onMenuOpenChange = { isSnapDropdownOpen = it },
                                    onSelectMode = { mode, mirrored ->
                                        interactionSignal.tryEmit(Unit)
                                        onSelectSnapMode(mode, mirrored)
                                    },
                                    onToggleMirror = {
                                        interactionSignal.tryEmit(Unit)
                                        onToggleSnapMirror()
                                    }
                                )
                            }

                            if (showTitle) {
                                val cleanDisplayTitle = remember(displayTitle) { displayTitle.removePrefix("*").trim() }
                                val isDirty = displayTitle.startsWith("*")
                                Box(
                                    modifier = Modifier.width(animatedTitleWidthDp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedContent(
                                        targetState = Triple(cleanDisplayTitle, windowIcon, windowIndex),
                                        transitionSpec = {
                                            val isForward = targetState.third >= initialState.third
                                            SpringSlideTransition.createSpec<Triple<String, ImageVector, Int>>(
                                                isForward = isForward,
                                                reduceAnimations = reduceMotion
                                            )(this)
                                        },
                                        label = "windowTitleSwitchTransition"
                                    ) { (currentCleanTitle, currentIcon, _) ->
                                        val titleText = if (isDirty) "*$currentCleanTitle" else currentCleanTitle
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .height(36.dp)
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = currentIcon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )

                                            Spacer(modifier = Modifier.width(6.dp))

                                            dev.ilamparithi.aournalpp.ui.InteractiveMarqueeText(
                                                text = titleText,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                minWidth = 90.dp,
                                                maxWidth = 220.dp
                                            )
                                        }
                                    }
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

                                val indicatorOffset by animateDpAsState(
                                    targetValue = (itemWidth + spacing) * selectedIndex,
                                    animationSpec = if (reduceMotion) snap() else tween(
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
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            interactionSignal.tryEmit(Unit)
                                                            onStylusClickModeChange(modeValue)
                                                        },
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
                                    targetValue = if (isFingerAsStylus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    ),
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
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            interactionSignal.tryEmit(Unit)
                                            try {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } catch (_: Exception) {
                                            }
                                            onToggleFingerAsStylus()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Draw,
                                            contentDescription = if (isFingerAsStylus) "Finger as Stylus (Enabled)" else "Finger as Stylus (Disabled)",
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
                                                onClick = {
                                                    interactionSignal.tryEmit(Unit)
                                                    try {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    } catch (_: Exception) {
                                                    }
                                                    onInjectShortcut(KeyEvent.KEYCODE_X, "ctrl+x")
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCut,
                                                    contentDescription = "Cut (Ctrl+X)",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (showCopy) {
                                            IconButton(
                                                onClick = {
                                                    interactionSignal.tryEmit(Unit)
                                                    try {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    } catch (_: Exception) {
                                                    }
                                                    onInjectShortcut(KeyEvent.KEYCODE_C, "ctrl+c")
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy (Ctrl+C)",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (showPaste) {
                                            IconButton(
                                                onClick = {
                                                    interactionSignal.tryEmit(Unit)
                                                    try {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    } catch (_: Exception) {
                                                    }
                                                    onInjectShortcut(KeyEvent.KEYCODE_V, "ctrl+v")
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = "Paste (Ctrl+V)",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (showImage) {
                                            IconButton(
                                                onClick = {
                                                    interactionSignal.tryEmit(Unit)
                                                    try {
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    } catch (_: Exception) {
                                                    }
                                                    onOpenImageSelector()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Image,
                                                    contentDescription = "Insert Image (Camera, Gallery, Files)",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (showKeyboard) {
                                val activeBgColor by animateColorAsState(
                                    targetValue = if (isKeyboardOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    ),
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
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            interactionSignal.tryEmit(Unit)
                                            onToggleKeyboard()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Keyboard,
                                            contentDescription = if (isKeyboardOpen) "Hide Keyboard" else "Show Keyboard",
                                            modifier = Modifier.size(17.dp),
                                            tint = activeIconColor
                                        )
                                    }
                                }
                            }

                            AppAnimatedVisibility(
                                visible = isMovedFromDefault,
                                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                                exit = fadeOut() + scaleOut(targetScale = 0.6f)
                            ) {
                                IconButton(
                                    onClick = {
                                        interactionSignal.tryEmit(Unit)
                                        try {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } catch (_: Exception) {
                                        }

                                        val (startDeltaX, startDeltaY) = with(density) {
                                            val tW =
                                                if (toolbarSizePx.width > 0) toolbarSizePx.width.toFloat() else 320f
                                            val tH =
                                                if (toolbarSizePx.height > 0) toolbarSizePx.height.toFloat() else 48f

                                            val minX: Float
                                            val maxX: Float
                                            val minY: Float
                                            val maxY: Float

                                            if (centerTopBarWithinBounds) {
                                                minX =
                                                    effectiveInsets.left.dp.toPx() + systemBarPadding.calculateStartPadding(
                                                        LayoutDirection.Ltr
                                                    ).toPx() + 8.dp.toPx()
                                                maxX = maxOf(
                                                    minX,
                                                    canvasWidthPx - tW - effectiveInsets.right.dp.toPx() - systemBarPadding.calculateEndPadding(
                                                        LayoutDirection.Ltr
                                                    ).toPx() - 8.dp.toPx()
                                                )
                                                minY =
                                                    effectiveInsets.top.dp.toPx() + systemBarPadding.calculateTopPadding()
                                                        .toPx() + 8.dp.toPx()
                                                maxY = maxOf(
                                                    minY,
                                                    canvasHeightPx - tH - effectiveInsets.bottom.dp.toPx() - systemBarPadding.calculateBottomPadding()
                                                        .toPx() - 8.dp.toPx()
                                                )
                                            } else {
                                                minX = 8.dp.toPx()
                                                maxX = maxOf(minX, canvasWidthPx - tW - 8.dp.toPx())
                                                minY = if (isFullscreen) {
                                                    if (cutoutPlacement.hasCenterCutout) cutoutTopOffsetDp.toPx() + 8.dp.toPx() else 8.dp.toPx()
                                                } else {
                                                    systemBarPadding.calculateTopPadding().toPx() + 8.dp.toPx()
                                                }
                                                maxY = maxOf(minY, canvasHeightPx - tH - 8.dp.toPx())
                                            }

                                            val currentBaseX = minX + (maxX - minX) * dragNormX
                                            val currentBaseY = minY + (maxY - minY) * dragNormY
                                            val targetBaseX = minX + (maxX - minX) * defaultNormX
                                            val targetBaseY = minY + (maxY - minY) * defaultNormY

                                            (currentBaseX - targetBaseX) to (currentBaseY - targetBaseY)
                                        }

                                        dragNormX = defaultNormX
                                        dragNormY = defaultNormY
                                        isMovedFromDefault = false

                                        coroutineScope.launch {
                                            animPixelOffset.snapTo(Offset(startDeltaX, startDeltaY))
                                            if (reduceMotion) {
                                                animPixelOffset.snapTo(Offset.Zero)
                                            } else {
                                                animPixelOffset.animateTo(
                                                    targetValue = Offset.Zero,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "Reset Toolbar Position",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            if (pinButtonMode) {
                                IconButton(
                                    onClick = {
                                        interactionSignal.tryEmit(Unit)
                                        isPinned = !isPinned
                                        try {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = if (isPinned) "Unpin Toolbar" else "Pin Toolbar",
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        interactionSignal.tryEmit(Unit)
                                        lastCollapseTimeMs = SystemClock.uptimeMillis()
                                        isHeaderExpanded = false
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandLess,
                                        contentDescription = "Collapse Toolbar",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (showDragHandle) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    coroutineScope.launch { animPixelOffset.snapTo(Offset.Zero) }
                                                    interactionSignal.tryEmit(Unit)
                                                    try {
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    } catch (_: Exception) {
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    interactionSignal.tryEmit(Unit)
                                                    val tW =
                                                        if (toolbarSizePx.width > 0) toolbarSizePx.width.toFloat() else 320f
                                                    val tH =
                                                        if (toolbarSizePx.height > 0) toolbarSizePx.height.toFloat() else 48f
                                                    val spanX = maxOf(1f, canvasWidthPx - tW)
                                                    val spanY = maxOf(1f, canvasHeightPx - tH)
                                                    dragNormX = (dragNormX + dragAmount.x / spanX).coerceIn(0f, 1f)
                                                    dragNormY = (dragNormY + dragAmount.y / spanY).coerceIn(0f, 1f)
                                                    isMovedFromDefault =
                                                        abs(dragNormX - defaultNormX) > 0.03f || abs(dragNormY - defaultNormY) > 0.03f
                                                },
                                                onDragEnd = {
                                                    interactionSignal.tryEmit(Unit)
                                                    try {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    } catch (_: Exception) {
                                                    }
                                                    isMovedFromDefault =
                                                        abs(dragNormX - defaultNormX) > 0.03f || abs(dragNormY - defaultNormY) > 0.03f
                                                },
                                                onDragCancel = {}
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator,
                                        contentDescription = "Drag to Move Toolbar",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        interactionSignal.tryEmit(Unit)
                                        isHeaderExpanded = true
                                    }
                                )
                            }
                            .pointerInput(stylusHoverExpands) {
                                if (stylusHoverExpands) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val isStylus = event.changes.any {
                                                it.type == PointerType.Stylus || it.type == PointerType.Eraser
                                            }
                                            if (isStylus && (event.type == PointerEventType.Move || event.type == PointerEventType.Enter)) {
                                                if (SystemClock.uptimeMillis() - lastCollapseTimeMs > 600L) {
                                                    interactionSignal.tryEmit(Unit)
                                                    isHeaderExpanded = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        coroutineScope.launch { animPixelOffset.snapTo(Offset.Zero) }
                                        interactionSignal.tryEmit(Unit)
                                        try {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } catch (_: Exception) {
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        interactionSignal.tryEmit(Unit)
                                        val tW = if (toolbarSizePx.width > 0) toolbarSizePx.width.toFloat() else 140f
                                        val tH = if (toolbarSizePx.height > 0) toolbarSizePx.height.toFloat() else 36f
                                        val spanX = maxOf(1f, canvasWidthPx - tW)
                                        val spanY = maxOf(1f, canvasHeightPx - tH)
                                        dragNormX = (dragNormX + dragAmount.x / spanX).coerceIn(0f, 1f)
                                        dragNormY = (dragNormY + dragAmount.y / spanY).coerceIn(0f, 1f)
                                        isMovedFromDefault =
                                            abs(dragNormX - defaultNormX) > 0.03f || abs(dragNormY - defaultNormY) > 0.03f
                                    },
                                    onDragEnd = {
                                        interactionSignal.tryEmit(Unit)
                                        try {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {
                                        }
                                        isMovedFromDefault =
                                            abs(dragNormX - defaultNormX) > 0.03f || abs(dragNormY - defaultNormY) > 0.03f
                                    },
                                    onDragCancel = {}
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showTitle) {
                            val cleanDisplayTitle = remember(displayTitle) { displayTitle.removePrefix("*").trim() }
                            val isDirty = displayTitle.startsWith("*")
                            val reduceMotion = LocalMotionPreferences.current.reduceAnimations
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = Triple(cleanDisplayTitle, windowIcon, windowIndex),
                                    transitionSpec = {
                                        val isForward = targetState.third >= initialState.third
                                        SpringSlideTransition.createSpec<Triple<String, ImageVector, Int>>(
                                            isForward = isForward,
                                            reduceAnimations = reduceMotion
                                        )(this)
                                    },
                                    label = "collapsedWindowTitleSwitchTransition"
                                ) { (currentCleanTitle, currentIcon, _) ->
                                    val titleText = if (isDirty) "*$currentCleanTitle" else currentCleanTitle
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = currentIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = titleText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand Toolbar",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
