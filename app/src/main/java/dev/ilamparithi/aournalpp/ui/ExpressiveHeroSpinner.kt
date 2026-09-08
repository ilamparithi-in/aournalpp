package dev.ilamparithi.aournalpp.ui

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.ui.theme.ScallopShape
import dev.ilamparithi.aournalpp.ui.theme.SunnyShape

/**
 * Reusable Material 3 Expressive multi-layered hero spinner adapted from the Linux environment
 * bootstrap screen.
 *
 * Features:
 * - Outer rotating expressive organic shape ([SunnyShape]) with radial gradient.
 * - Secondary counter-rotating scallop shape ([ScallopShape]).
 * - Inner brand container with a breathing pulse scale and customizable icon (default [Icons.Default.Sync]).
 */
@Composable
fun ExpressiveHeroSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    icon: ImageVector = Icons.Default.Sync,
    iconDescription: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    iconTint: Color = MaterialTheme.colorScheme.onPrimary
) {
    val context = LocalContext.current
    val aournalPrefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    val reduceAnimations = remember { aournalPrefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false) }

    val infiniteTransition = rememberInfiniteTransition(label = "expressiveHeroSpinnerTransition")

    val animatedRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "expressiveSpinnerRotation"
    )
    val rotation = if (reduceAnimations) 0f else animatedRotation

    val animatedPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "expressiveSpinnerPulse"
    )
    val pulseScale = if (reduceAnimations) 1f else animatedPulseScale

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer rotating expressive organic shape
        Box(
            modifier = Modifier
                .size(size * 0.97f)
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
                .size(size * 0.8f)
                .rotate(-rotation * 0.7f)
                .clip(ScallopShape(lobes = 8, depth = 0.1f))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        )

        // Inner brand container
        Box(
            modifier = Modifier
                .size(size * 0.58f)
                .clip(CircleShape)
                .background(containerColor)
                .scale(pulseScale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                modifier = Modifier.size(size * 0.34f),
                tint = iconTint
            )
        }
    }
}
