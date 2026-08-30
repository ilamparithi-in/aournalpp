package dev.ilamparithi.aournalpp.ui

import androidx.activity.compose.BackHandler

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.ui.theme.ExpressiveSprings
import dev.ilamparithi.aournalpp.ui.theme.ScallopShape
import dev.ilamparithi.aournalpp.ui.theme.SunnyShape
import dev.ilamparithi.aournalpp.utils.FormatUtils
import kotlinx.coroutines.delay

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import kotlin.math.hypot

private val TIPS_LIST = listOf(
    dev.ilamparithi.aournalpp.R.string.bootstrap_tip_1,
    dev.ilamparithi.aournalpp.R.string.bootstrap_tip_2,
    dev.ilamparithi.aournalpp.R.string.bootstrap_tip_3,
    dev.ilamparithi.aournalpp.R.string.bootstrap_tip_4,
    dev.ilamparithi.aournalpp.R.string.bootstrap_tip_5
)

@Composable
fun BootstrapScreen(
    state: BootstrapState,
    onRetry: () -> Unit,
    isReady: Boolean = false,
    onRevealFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val aournalPrefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    val reduceAnimations = remember { aournalPrefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false) }

    // Intercept back button during checking, extraction or bootstrap error
    BackHandler(enabled = true) {
        // No-op: Prevent dismissal via back button
    }

    val isError = state is BootstrapState.Error

    // Material 3 Expressive Infinite Animations
    val infiniteTransition = rememberInfiniteTransition(label = "expressiveWaitingTransition")
    
    // Slow decorative rotation
    val animatedRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "organicRotation"
    )
    val rotation = if (reduceAnimations || isError) 0f else animatedRotation

    // Breathing pulse for icon
    val animatedPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconBreathingPulse"
    )
    val pulseScale = if (reduceAnimations || isError) 1f else animatedPulseScale

    // Rotating Tip Index
    var tipIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4500L)
            tipIndex = (tipIndex + 1) % TIPS_LIST.size
        }
    }

    // Hero Element Expand & Punch-Out Reveal Animation State
    var rootLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var heroCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var capturedRotationAngle by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var heroCenterOffset by remember { mutableStateOf<Offset?>(null) }
    var isRevealing by remember { mutableStateOf(false) }
    val revealRadius = remember { Animatable(0f) }

    LaunchedEffect(isReady, state) {
        if ((isReady || state is BootstrapState.Ready) && !isRevealing && !isError) {
            isRevealing = true
            capturedRotationAngle = rotation
            val rootCoords = rootLayoutCoordinates
            val heroCoords = heroCoordinates

            val (heroW, heroH) = if (heroCoords != null) {
                heroCoords.size.let { it.component1().toFloat() to it.component2().toFloat() }
            } else {
                (140f * density.density) to (140f * density.density)
            }

            val (rootW, rootH) = if (rootCoords != null) {
                rootCoords.size.let { it.component1().toFloat() to it.component2().toFloat() }
            } else {
                2500f to 1600f
            }

            val center = if (rootCoords != null && heroCoords != null && rootCoords.isAttached && heroCoords.isAttached) {
                val pos = rootCoords.localPositionOf(heroCoords, Offset.Zero)
                Offset(pos.x + heroW / 2f, pos.y + heroH / 2f)
            } else {
                Offset(rootW / 2f, rootH / 2f)
            }
            heroCenterOffset = center

            val maxRadius = maxOf(
                hypot(center.x, center.y),
                hypot(rootW - center.x, center.y),
                hypot(center.x, rootH - center.y),
                hypot(rootW - center.x, rootH - center.y)
            ) * 1.35f

            if (reduceAnimations) {
                onRevealFinished()
            } else {
                val initialRadius = heroW / 2f
                revealRadius.snapTo(initialRadius)
                revealRadius.animateTo(
                    targetValue = maxRadius,
                    animationSpec = tween(
                        durationMillis = 750,
                        easing = FastOutSlowInEasing
                    )
                )
                onRevealFinished()
            }
        }
    }

    val reusableMatrix = remember { android.graphics.Matrix() }
    val reusableTransformedPath = remember { android.graphics.Path() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootLayoutCoordinates = it }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val radius = revealRadius.value
                if (radius > 0f) {
                    val center = heroCenterOffset ?: Offset(size.width / 2f, size.height / 2f)
                    val sunnyOutline = SunnyShape(vertices = 10, roundness = 0.35f)
                        .createOutline(Size(radius * 2f, radius * 2f), layoutDirection, density)
                    if (sunnyOutline is Outline.Generic) {
                        val androidPath = sunnyOutline.path.asAndroidPath()
                        reusableMatrix.reset()
                        reusableMatrix.postTranslate(-radius, -radius)
                        reusableMatrix.postRotate(capturedRotationAngle)
                        reusableMatrix.postTranslate(center.x, center.y)

                        reusableTransformedPath.reset()
                        androidPath.transform(reusableMatrix, reusableTransformedPath)
                        val composePath = reusableTransformedPath.asComposePath()

                        // Punch out hero shape hole revealing Home screen
                        drawPath(
                            path = composePath,
                            color = Color.Black,
                            blendMode = BlendMode.Clear
                        )

                        // Glowing rim along outer contour of SunnyShape
                        val ringAlpha = (1f - (radius / (size.maxDimension * 0.95f)).coerceIn(0f, 1f))
                        if (ringAlpha > 0.01f) {
                            drawPath(
                                path = composePath,
                                color = Color.White.copy(alpha = ringAlpha * 0.65f),
                                style = Stroke(width = 3.5.dp.toPx())
                            )
                        }
                    }
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Expressive Multi-Layered Hero Animation
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .onGloballyPositioned { heroCoordinates = it },
                    contentAlignment = Alignment.Center
                ) {
                    if (!isError) {
                        // Outer rotating expressive organic shape
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .rotate(rotation)
                                .clip(SunnyShape(vertices = 10, roundness = 0.35f))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Secondary counter-rotating scallop shape
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .rotate(-rotation * 0.7f)
                                .clip(ScallopShape(lobes = 8, depth = 0.1f))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        )
                    }

                    // Inner brand container
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (isError) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.primary
                            )
                            .scale(if (isError) 1f else pulseScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.Create,
                            contentDescription = "App Icon",
                            modifier = Modifier.size(40.dp),
                            tint = if (isError) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Title & Subtitle
                Text(
                    text = if (isError) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.bootstrap_title_failed)
                    else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.bootstrap_title_setup),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isError) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.bootstrap_desc_error)
                    else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.bootstrap_desc_extracting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!isError) {
                    val installingState = state as? BootstrapState.Installing
                    val progress = installingState?.progress

                    // Progress Bar Container (Clean, Direct Progress)
                    Column(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            if (progress != null) {
                                val percentage = (progress.percentage / 100f).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { percentage },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Extracted statistics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (progress != null) {
                                Text(
                                    text = FormatUtils.formatPercentage(progress.percentage),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = androidx.compose.ui.res.stringResource(
                                        dev.ilamparithi.aournalpp.R.string.bootstrap_extracted_mb,
                                        progress.extractedBytes / (1024 * 1024)
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.bootstrap_preparing_archive),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Current File Log Line (Simple rapidly changing single log line, no animation)
                        val currentFileText = if (progress != null && progress.currentFile.isNotBlank()) {
                            progress.currentFile.substringAfterLast('/')
                        } else {
                            val defaultUnpacking = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.bootstrap_unpacking_libraries)
                            val defaultInit = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.bootstrap_initializing_env)
                            installingState?.message?.ifBlank { defaultUnpacking } ?: defaultInit
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentFileText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Rotating Feature Tips Card
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            AnimatedContent(
                                targetState = TIPS_LIST[tipIndex],
                                transitionSpec = {
                                    (slideInVertically(animationSpec = spring(stiffness = 380f)) { it / 2 } + fadeIn())
                                        .togetherWith(
                                            slideOutVertically(animationSpec = spring(stiffness = 380f)) { -it / 2 } + fadeOut()
                                        )
                                },
                                label = "tipCarousel",
                                modifier = Modifier.weight(1f)
                            ) { tipRes ->
                                Text(
                                    text = androidx.compose.ui.res.stringResource(tipRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    // Error state details
                    val errorThrowable = (state as BootstrapState.Error).throwable
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorThrowable.message ?: "Unknown initialization failure occurred.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.bootstrap_retry_btn))
                    }
                }
            }
        }
    }
}
