package dev.ilamparithi.aournalpp.ui.preview

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import org.intellij.lang.annotations.Language
import java.util.Random
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

@Language("AGSL")
private const val PERSISTENT_TURBULENCE_SHADER = """
    uniform float2 resolution;
    uniform float2 origin;
    uniform float time;
    uniform float progress;
    uniform float maxRadius;
    uniform float4 color;

    // High-frequency hash for crisp micro-sparkle grain (not cloudy blobs)
    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
    }

    float grain(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
                   mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
    }

    half4 main(float2 fragCoord) {
        float dist = distance(fragCoord, origin);
        float currentRadius = maxRadius * progress;

        if (dist > currentRadius + 20.0) {
            return half4(0.0);
        }

        // High frequency fine sparkle noise (crisp pinpoints, zero cloudiness)
        float g1 = grain(fragCoord * 0.35 + float2(time * 0.8, time * 0.4));
        float g2 = grain(fragCoord * 0.70 - float2(time * 0.6, time * 0.7));
        float g = g1 * 0.6 + g2 * 0.4;

        // Sharp threshold for delicate, subtle pinpoint sparkles
        float sparkle = smoothstep(0.78, 0.94, g);
        float waveFront = 1.0 - clamp(abs(dist - currentRadius) / 60.0, 0.0, 1.0);
        float edgeFade = 1.0 - smoothstep(currentRadius - 10.0, currentRadius + 20.0, dist);

        float a = color.a * (sparkle * 0.75 + waveFront * 0.25) * edgeFade;
        return half4(color.rgb, clamp(a, 0.0, 1.0));
    }
"""

data class DreamyParticle(
    val xNorm: Float,
    val yNorm: Float,
    val baseRadiusPx: Float,
    val baseAlpha: Float,
    val shimmerSpeed: Float,
    val phase: Float
)

/**
 * Reusable dreamy stars background combining AGSL runtime noise turbulence
 * with twinkling stardust micro-particles and a sweeping radiant scrim.
 *
 * @param strength Multiplier to modulate particle count and sparkle opacity (e.g. 0.65f for a gentler gallery backdrop).
 */
@Composable
fun DreamyStarsBackground(
    modifier: Modifier = Modifier,
    origin: Offset? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    progress: Float = 1f,
    alpha: Float = 1f,
    strength: Float = 1f
) {
    Box(modifier = modifier) {
        PersistentTurbulenceLayer(
            origin = origin,
            progress = progress,
            alpha = alpha * strength,
            modifier = Modifier.fillMaxSize()
        )

        DreamyParticleField(
            origin = origin,
            accentColor = accentColor,
            progress = progress,
            alpha = alpha,
            strength = strength,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun PersistentTurbulenceLayer(
    origin: Offset?,
    progress: Float,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (alpha <= 0.01f || progress <= 0.001f) return

    val infiniteTransition = rememberInfiniteTransition(label = "agslTurbulence")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "agslTime"
    )

    val shader = remember {
        try {
            RuntimeShader(PERSISTENT_TURBULENCE_SHADER)
        } catch (e: Throwable) {
            null
        }
    }

    if (shader != null) {
        Box(
            modifier = modifier.drawWithCache {
                try {
                    val resolvedOrigin = origin ?: Offset(size.width / 2f, size.height / 2f)
                    val maxDist = hypot(
                        max(resolvedOrigin.x, size.width - resolvedOrigin.x),
                        max(resolvedOrigin.y, size.height - resolvedOrigin.y)
                    ) * 1.15f

                    shader.setFloatUniform("resolution", size.width, size.height)
                    shader.setFloatUniform("origin", resolvedOrigin.x, resolvedOrigin.y)
                    shader.setFloatUniform("time", time)
                    shader.setFloatUniform("progress", progress)
                    shader.setFloatUniform("maxRadius", maxDist)
                    shader.setFloatUniform("color", 1.0f, 1.0f, 1.0f, 0.35f * alpha)
                    val brush = ShaderBrush(shader)

                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = brush)
                    }
                } catch (e: Throwable) {
                    onDrawWithContent {
                        drawContent()
                    }
                }
            }
        )
    }
}

@Composable
fun DreamyParticleField(
    origin: Offset?,
    accentColor: Color,
    progress: Float,
    alpha: Float,
    strength: Float = 1f,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val particleCount = (520 * strength.coerceIn(0.2f, 1.5f)).toInt().coerceAtLeast(100)

    val particles = remember(density, particleCount) {
        val random = Random(1337L)
        val dpiScale = (density / 2.75f).coerceIn(0.85f, 1.25f)

        List(particleCount) {
            val r = random.nextFloat()
            val radius = when {
                r < 0.70f -> (0.80f + random.nextFloat() * 0.45f) * dpiScale
                r < 0.90f -> (1.25f + random.nextFloat() * 0.45f) * dpiScale
                else -> (1.70f + random.nextFloat() * 0.40f) * dpiScale
            }
            DreamyParticle(
                xNorm = random.nextFloat(),
                yNorm = random.nextFloat(),
                baseRadiusPx = radius,
                baseAlpha = (0.35f + random.nextFloat() * 0.45f) * strength.coerceIn(0.4f, 1f),
                shimmerSpeed = 1.2f + random.nextFloat() * 2.2f,
                phase = random.nextFloat() * (2f * Math.PI.toFloat())
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particleShimmer")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalAlphaMultiplier = alpha

        if (totalAlphaMultiplier <= 0.01f || progress <= 0.001f) return@Canvas

        val resolvedOrigin = origin ?: Offset(canvasWidth / 2f, canvasHeight / 2f)
        val maxDist = hypot(
            max(resolvedOrigin.x, canvasWidth - resolvedOrigin.x),
            max(resolvedOrigin.y, canvasHeight - resolvedOrigin.y)
        ) * 1.12f

        val currentSweepRadius = maxDist * progress
        val waveBandWidth = 120f * density

        // Draw Sweeping Radial Scrim & Radiant Wavefront Glow
        if (currentSweepRadius > 1f) {
            val gradientRadius = (currentSweepRadius + 35f * density).coerceAtLeast(10f)
            val waveEdgeFraction = (currentSweepRadius / gradientRadius).coerceIn(0.1f, 0.95f)

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to accentColor.copy(alpha = 0.06f * totalAlphaMultiplier * strength),
                        waveEdgeFraction to accentColor.copy(alpha = 0.15f * (1f - progress * 0.45f) * totalAlphaMultiplier * strength),
                        1.0f to Color.Transparent
                    ),
                    center = resolvedOrigin,
                    radius = gradientRadius
                ),
                radius = gradientRadius,
                center = resolvedOrigin
            )
        }

        // Draw Sweeping Particle Sparkle Ignition
        for (i in 0 until particles.size) {
            val p = particles[i]
            val x = p.xNorm * canvasWidth
            val y = p.yNorm * canvasHeight

            val dx = x - resolvedOrigin.x
            val dy = y - resolvedOrigin.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist > currentSweepRadius) continue

            val distFromWavefront = currentSweepRadius - dist
            val isNearWavefront = distFromWavefront < waveBandWidth

            val flash = if (isNearWavefront) {
                (1f - (distFromWavefront / waveBandWidth)).coerceIn(0f, 1f)
            } else {
                0f
            }

            val shimmer = (sin(time * p.shimmerSpeed + p.phase) * 0.40f + 0.60f)
            val dynamicAlpha = ((p.baseAlpha * shimmer + flash * 0.65f) * totalAlphaMultiplier * strength).coerceIn(0f, 1f)
            val dynamicRadius = p.baseRadiusPx * (1f + flash * 0.50f)

            drawCircle(
                color = Color.White.copy(alpha = dynamicAlpha),
                radius = dynamicRadius,
                center = Offset(x, y)
            )
        }
    }
}
