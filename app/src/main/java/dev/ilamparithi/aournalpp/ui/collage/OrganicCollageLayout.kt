package dev.ilamparithi.aournalpp.ui.collage

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.ui.theme.ArchShape
import dev.ilamparithi.aournalpp.ui.theme.CloverShape
import dev.ilamparithi.aournalpp.ui.theme.ScallopShape
import dev.ilamparithi.aournalpp.ui.theme.SunnyShape
import dev.ilamparithi.aournalpp.utils.ThumbnailManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Random
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Data class representing a note placed geometrically in the organic collage.
 */
data class PlacedCollageCard(
    val note: NoteDocument,
    val x: Float, // in dp
    val y: Float, // in dp
    val width: Float, // in dp
    val height: Float, // in dp
    val shape: Shape
)

data class CollageLayoutResult(
    val cards: List<PlacedCollageCard>,
    val totalWidth: Float,
    val totalHeight: Float,
    val isScrollable: Boolean
)

/**
 * Directional enum for round-robin adjacent packing.
 */
enum class CollageDirection {
    RIGHT, BOTTOM, LEFT, TOP
}

/**
 * Computes organic center-outward adjacent-packing layout with controlled non-deterministic randomness.
 *
 * Rules:
 * 1. Note 0 (Primary / Pinned #1) is placed at the center (0, 0).
 * 2. Additions evaluate all 4 directions around perimeter cards.
 * 3. At least one side of the new incoming note is adjacent to an existing note with uniform spacing (10dp).
 * 4. Sides need NOT be of the same length, creating an organic, varied collage look.
 * 5. Full width space is filled before expanding downwards, and downward rows span all columns evenly.
 * 6. Non-deterministic seed adds subtle organic variations on refresh.
 */
object OrganicCollageEngine {

    private const val SPACING_DP = 10f
    private const val CORNER_RADIUS_DP = 14f

    fun computeLayout(
        notes: List<NoteDocument>,
        maxWidthDp: Float = 840f,
        seed: Long = 0L
    ): CollageLayoutResult {
        if (notes.isEmpty()) {
            return CollageLayoutResult(emptyList(), 0f, 0f, isScrollable = false)
        }

        val random = if (seed != 0L) Random(seed) else Random(notes.hashCode().toLong())
        val placedList = mutableListOf<PlacedCollageCard>()
        val defaultShape = RoundedCornerShape(CORNER_RADIUS_DP.dp)
        val usableWidth = (maxWidthDp - 28f).coerceAtLeast(320f)

        // Note 0: Center Hero Card (centered at origin (0,0))
        val (firstW, firstH) = getCardDimensions(notes[0], index = 0, isSingle = (notes.size == 1), maxWidthDp = maxWidthDp, random = random)
        placedList.add(
            PlacedCollageCard(
                note = notes[0],
                x = -firstW / 2f,
                y = -firstH / 2f,
                width = firstW,
                height = firstH,
                shape = defaultShape
            )
        )

        for (i in 1 until notes.size) {
            val note = notes[i]
            val (w, h) = getCardDimensions(note, index = i, isSingle = false, maxWidthDp = maxWidthDp, random = random)

            val candidate = findBestAdjacentPosition(
                placed = placedList,
                width = w,
                height = h,
                stepIndex = i,
                usableWidth = usableWidth,
                random = random
            )

            placedList.add(
                PlacedCollageCard(
                    note = note,
                    x = candidate.x,
                    y = candidate.y,
                    width = w,
                    height = h,
                    shape = defaultShape
                )
            )
        }

        // Normalize bounds so min(x)=0, min(y)=0
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (c in placedList) {
            minX = minOf(minX, c.x)
            minY = minOf(minY, c.y)
            maxX = maxOf(maxX, c.x + c.width)
            maxY = maxOf(maxY, c.y + c.height)
        }

        val totalW = maxX - minX
        val totalH = maxY - minY

        val normalized = placedList.map { card ->
            card.copy(x = card.x - minX, y = card.y - minY)
        }

        return CollageLayoutResult(
            cards = normalized,
            totalWidth = totalW,
            totalHeight = totalH,
            isScrollable = (notes.size > 5)
        )
    }

    private fun getCardDimensions(
        note: NoteDocument,
        index: Int,
        isSingle: Boolean,
        maxWidthDp: Float,
        random: Random
    ): Pair<Float, Float> {
        if (isSingle) {
            return Pair(if (maxWidthDp >= 700f) 360f else 280f, 240f)
        }

        val variations = if (maxWidthDp >= 700f) {
            listOf(
                Pair(270f, 220f), // Landscape
                Pair(240f, 275f), // Portrait
                Pair(285f, 210f), // Wide
                Pair(230f, 255f), // Medium portrait
                Pair(255f, 245f), // Square
                Pair(280f, 235f), // Large
                Pair(240f, 205f)  // Compact
            )
        } else {
            listOf(
                Pair(180f, 190f),
                Pair(160f, 170f),
                Pair(190f, 160f),
                Pair(170f, 180f),
                Pair(175f, 175f)
            )
        }
        val baseIndex = (abs(note.title.hashCode() + index * 31) + random.nextInt(variations.size)) % variations.size
        val (baseW, baseH) = variations[baseIndex]
        val jitterW = (random.nextFloat() - 0.5f) * 12f
        val jitterH = (random.nextFloat() - 0.5f) * 12f
        return Pair((baseW + jitterW).coerceAtLeast(160f), (baseH + jitterH).coerceAtLeast(160f))
    }

    private data class CandidatePos(val x: Float, val y: Float, val score: Float)

    private fun findBestAdjacentPosition(
        placed: List<PlacedCollageCard>,
        width: Float,
        height: Float,
        stepIndex: Int,
        usableWidth: Float,
        random: Random
    ): CandidatePos {
        val candidates = mutableListOf<CandidatePos>()
        val alignOptions = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
        val allDirections = listOf(CollageDirection.RIGHT, CollageDirection.LEFT, CollageDirection.BOTTOM, CollageDirection.TOP)

        for (target in placed) {
            for (dir in allDirections) {
                for (align in alignOptions) {
                    val (x, y) = when (dir) {
                        CollageDirection.RIGHT -> Pair(target.x + target.width + SPACING_DP, target.y + align * (target.height - height))
                        CollageDirection.LEFT -> Pair(target.x - SPACING_DP - width, target.y + align * (target.height - height))
                        CollageDirection.BOTTOM -> Pair(target.x + align * (target.width - width), target.y + target.height + SPACING_DP)
                        CollageDirection.TOP -> Pair(target.x + align * (target.width - width), target.y - SPACING_DP - height)
                    }

                    if (isValidPlacement(x, y, width, height, placed, usableWidth, stepIndex)) {
                        val baseScore = calculateCandidateScore(x, y, width, height, placed, dir, stepIndex, usableWidth)
                        val jitter = (random.nextFloat() - 0.5f) * 28f
                        candidates.add(CandidatePos(x, y, baseScore + jitter))
                    }
                }
            }
        }

        if (candidates.isNotEmpty()) {
            return candidates.minByOrNull { it.score }!!
        }

        // Fallback: Place in lowest row
        val lowest = placed.maxByOrNull { it.y + it.height } ?: placed.first()
        return CandidatePos(lowest.x, lowest.y + lowest.height + SPACING_DP, 1000f)
    }

    private fun isValidPlacement(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        placed: List<PlacedCollageCard>,
        usableWidth: Float,
        stepIndex: Int
    ): Boolean {
        // Horizontal bound constraint
        if (placed.isNotEmpty()) {
            val minX = minOf(placed.minOf { it.x }, x)
            val maxX = maxOf(placed.maxOf { it.x + it.width }, x + width)
            if (maxX - minX > usableWidth) {
                return false
            }
        }

        // Upward constraint: for notes 5+, do not push higher than top boundary
        if (stepIndex >= 5 && y < -360f) {
            return false
        }

        // Overlap check with existing cards
        for (c in placed) {
            val overlapX = (x < c.x + c.width) && (x + width > c.x)
            val overlapY = (y < c.y + c.height) && (y + height > c.y)
            if (overlapX && overlapY) return false
        }
        return true
    }

    private fun calculateCandidateScore(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        placed: List<PlacedCollageCard>,
        dir: CollageDirection,
        stepIndex: Int,
        usableWidth: Float
    ): Float {
        val centerX = x + w / 2f
        val centerY = y + h / 2f

        // 1. Shared edge contact length (Snugness / Corner packing)
        var sharedContactLength = 0f
        for (c in placed) {
            val touchingH = (abs(x - (c.x + c.width + SPACING_DP)) < 1.5f) || (abs((x + w + SPACING_DP) - c.x) < 1.5f)
            if (touchingH) {
                val overlapY = maxOf(0f, minOf(y + h, c.y + c.height) - maxOf(y, c.y))
                sharedContactLength += overlapY
            }
            val touchingV = (abs(y - (c.y + c.height + SPACING_DP)) < 1.5f) || (abs((y + h + SPACING_DP) - c.y) < 1.5f)
            if (touchingV) {
                val overlapX = maxOf(0f, minOf(x + w, c.x + c.width) - maxOf(x, c.x))
                sharedContactLength += overlapX
            }
        }

        var score = 0f
        score -= sharedContactLength * 1.5f

        if (stepIndex < 5) {
            val distFromCenter = sqrt(centerX * centerX + centerY * centerY)
            score += distFromCenter

            val preferredDir = when (stepIndex) {
                1 -> CollageDirection.RIGHT
                2 -> CollageDirection.BOTTOM
                3 -> CollageDirection.LEFT
                4 -> CollageDirection.TOP
                else -> CollageDirection.RIGHT
            }
            if (dir == preferredDir) score -= 140f
        } else {
            val minPlacedX = placed.minOf { it.x }
            val maxPlacedX = placed.maxOf { it.x + it.width }
            val currentSpanX = maxPlacedX - minPlacedX
            val maxPlacedY = placed.maxOf { it.y + it.height }

            // Priority 1: Fill upper/middle notches
            if (y < maxPlacedY - 140f) {
                score -= 500f
            }

            // Priority 2: Expand horizontally to fill available width
            if (currentSpanX < usableWidth * 0.90f) {
                if (dir == CollageDirection.LEFT || dir == CollageDirection.RIGHT) {
                    score -= 350f
                }
            }

            // Priority 3: Prevent single-file vertical column stacking
            val columnDensity = placed.count { c ->
                val cardCenterX = c.x + c.width / 2f
                abs(cardCenterX - centerX) < 130f
            }
            score += columnDensity * 160f

            // Row progression
            score += y * 0.7f
        }

        return score
    }
}

/**
 * Organic Expressive Collage Composable with Non-Blocking Asynchronous Computation
 * and Material 3 Expressive Loading Screen during Resizing/Rotation.
 */
@Composable
fun OrganicCollageView(
    notes: List<NoteDocument>,
    pdfExportManager: PdfExportManager,
    onNoteClick: (NoteDocument) -> Unit,
    onNewNoteClick: () -> Unit,
    refreshSeed: Long = 0L,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) {
        CreativeEmptyCollageState(onNewNoteClick = onNewNoteClick)
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val availableWidthDp = maxWidth.value

        // Asynchronous computation on Dispatchers.Default prevents any render thread blocking / microstutters
        val layoutState by produceState<CollageLayoutResult?>(
            initialValue = null,
            key1 = notes,
            key2 = availableWidthDp,
            key3 = refreshSeed
        ) {
            value = null // Trigger M3E loading state smoothly
            value = withContext(Dispatchers.Default) {
                OrganicCollageEngine.computeLayout(
                    notes = notes,
                    maxWidthDp = availableWidthDp,
                    seed = refreshSeed
                )
            }
        }

        AnimatedContent(
            targetState = layoutState,
            transitionSpec = {
                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                 scaleIn(initialScale = 0.96f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleOut(targetScale = 0.96f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    )
            },
            label = "collageLayoutTransition"
        ) { layout ->
            if (layout == null) {
                // Material 3 Expressive Morphing/Jumping Shapes Loading Screen
                Material3ExpressiveLoadingScreen(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(layout.totalWidth.dp)
                        .height(layout.totalHeight.dp)
                ) {
                    layout.cards.forEach { card ->
                        Box(
                            modifier = Modifier
                                .offset(x = card.x.dp, y = card.y.dp)
                                .size(card.width.dp, card.height.dp)
                        ) {
                            CollageCardView(
                                note = card.note,
                                shape = card.shape,
                                pdfExportManager = pdfExportManager,
                                onClick = { onNoteClick(card.note) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Card in the Collage with Floating Color-Coded Details Pill.
 */
@Composable
fun CollageCardView(
    note: NoteDocument,
    shape: Shape,
    pdfExportManager: PdfExportManager,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )

    val thumbnailFile by produceState<File?>(
        initialValue = ThumbnailManager.getCachedThumbnailFile(context, note.file),
        key1 = note.lastModifiedMs
    ) {
        value = ThumbnailManager.getOrCreateThumbnail(context, note.file, pdfExportManager)
    }

    // Folder Palette Color Variant
    val folderAccentColor = note.folderColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
            .shadow(elevation = 4.dp, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, folderAccentColor.copy(alpha = 0.25f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Thumbnail Image
        if (thumbnailFile != null && thumbnailFile!!.exists()) {
            val bitmap = remember(thumbnailFile) {
                try { BitmapFactory.decodeFile(thumbnailFile!!.absolutePath) } catch (e: Exception) { null }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = note.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(folderAccentColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (note.fileType) {
                        NoteFileType.PDF -> Icons.Default.PictureAsPdf
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = folderAccentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Top-Right: Pinned Pushpin Badge (if pinned)
        if (note.isPinned) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned Note",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bottom: Floating Folder-Palette Details Pill
        FloatingDetailsPill(
            note = note,
            folderColor = folderAccentColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

/**
 * Floating Details Pill using a palette variant of the folder color with color-coded details.
 */
@Composable
fun FloatingDetailsPill(
    note: NoteDocument,
    folderColor: Color,
    modifier: Modifier = Modifier
) {
    val tintedBgColor = folderColor.copy(alpha = 0.22f)
        .compositeOver(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tintedBgColor,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, folderColor.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Top Row: Title + Uniform Format Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Uniform format chip color per file type across all notes
                val formatBgColor = when (note.fileType) {
                    NoteFileType.PDF -> Color(0xFFE53935)       // Crimson Red for PDF
                    NoteFileType.XOJ -> Color(0xFFF57C00)       // Vibrant Amber for XOJ
                    else -> Color(0xFF3F51B5)                   // Indigo Blue for XOPP
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = formatBgColor.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = ".${note.file.extension.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = formatBgColor,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            // Bottom Row: Folder & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (note.folder.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = folderColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = note.folder,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = folderColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = note.lastModifiedFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Material 3 Expressive Loading Screen.
 * Features rotating, jumping, sliding and pulsing M3 expressive shapes (Arch, Scallop, Sunny, Clover)
 * with spring physics and fluid color transitions.
 */
@Composable
fun Material3ExpressiveLoadingScreen(
    modifier: Modifier = Modifier,
    message: String = "Arranging studio notes..."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3eLoadingInfinite")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shapeRotation"
    )

    val jumpOffset1 by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jump1"
    )
    val jumpOffset2 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jump2"
    )
    val jumpOffset3 by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jump3"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Expressive Shapes Cluster
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shape 1: CloverShape (Primary)
                Box(
                    modifier = Modifier
                        .offset(y = jumpOffset1.dp)
                        .size(44.dp)
                        .scale(pulseScale)
                        .rotate(rotation)
                        .clip(CloverShape())
                        .background(MaterialTheme.colorScheme.primary)
                )

                // Shape 2: SunnyShape (Tertiary)
                Box(
                    modifier = Modifier
                        .offset(y = jumpOffset2.dp)
                        .size(50.dp)
                        .scale(2f - pulseScale)
                        .rotate(-rotation * 1.3f)
                        .clip(SunnyShape(vertices = 8, roundness = 0.25f))
                        .background(MaterialTheme.colorScheme.tertiary)
                )

                // Shape 3: ArchShape (Secondary)
                Box(
                    modifier = Modifier
                        .offset(y = jumpOffset3.dp)
                        .size(46.dp)
                        .scale(pulseScale)
                        .rotate(rotation * 0.8f)
                        .clip(ArchShape(cornerRadiusRatio = 0.45f))
                        .background(MaterialTheme.colorScheme.secondary)
                )

                // Shape 4: ScallopShape (Primary Container)
                Box(
                    modifier = Modifier
                        .offset(y = -jumpOffset1.dp)
                        .size(40.dp)
                        .scale(1.1f)
                        .rotate(-rotation * 0.9f)
                        .clip(ScallopShape(lobes = 8, depth = 0.10f))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }

            // Expressive Status Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                ),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Pull-To-Refresh Indicator.
 * Displays rotating & morphing expressive shapes that expand as user pulls and bounce when refreshing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressivePullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pullInfinite")
    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pullSpin"
    )
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pullBounce"
    )

    val pullFraction = state.distanceFraction.coerceIn(0f, 1f)
    val displayRotation = if (isRefreshing) spinRotation else pullFraction * 180f
    val displayScale = if (isRefreshing) bounceScale else pullFraction.coerceAtLeast(0.4f)

    if (pullFraction > 0.05f || isRefreshing) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ),
            modifier = modifier
                .padding(top = 12.dp)
                .scale(displayScale)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(displayRotation)
                        .clip(if (isRefreshing) SunnyShape(vertices = 8, roundness = 0.25f) else CloverShape())
                        .background(MaterialTheme.colorScheme.primary)
                )

                Text(
                    text = if (isRefreshing) "Refreshing studio..." else "Pull to refresh",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Creative Animated Empty State with 4-5 scattered floating Material 3 Expressive Shapes on the background.
 */
@Composable
fun CreativeEmptyCollageState(
    onNewNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyShapesMotion")
    val floatAnim1 by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )
    val floatAnim2 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )
    val rotAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating M3 Expressive Background Shapes (4-5 shapes)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 18.dp, y = (18f + floatAnim1).dp)
                .size(76.dp)
                .rotate(rotAnim)
                .clip(SunnyShape(vertices = 8, roundness = 0.25f))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-20).dp, y = ((-20f) + floatAnim2).dp)
                .size(88.dp)
                .rotate(-rotAnim / 2)
                .clip(ScallopShape(lobes = 8, depth = 0.10f))
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = (24f + floatAnim2).dp)
                .size(68.dp)
                .rotate(rotAnim / 3)
                .clip(CloverShape())
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 26.dp, y = ((-16f) + floatAnim1).dp)
                .size(72.dp)
                .clip(ArchShape(cornerRadiusRatio = 0.45f))
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
        )

        Box(
            modifier = Modifier
                .size(140.dp, 80.dp)
                .rotate(15f + floatAnim1)
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
        )

        // Center Content Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = "Your Canvas Awaits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Notes you create will assemble into an organic collage right here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onNewNoteClick,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Note", fontWeight = FontWeight.Bold)
            }
        }
    }
}
