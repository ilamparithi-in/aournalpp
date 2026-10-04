package dev.ilamparithi.aournalpp.ui.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.ilamparithi.aournalpp.ui.animation.LocalMotionPreferences

data class ConnectedButtonItem<T>(
    val value: T,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null
)

/**
 * Material 3 Expressive Connected Button Group.
 * Replaces the deprecated Segmented Button component per M3 Expressive guidelines.
 * Reference: https://m3.material.io/components/segmented-buttons/overview
 *
 * Expressive Physics & Micro-interactions:
 * - Dynamic Button Expansion: Selected item dynamically expands its weight/width with bouncy spring physics,
 *   causing adjacent items to elastically compress and recoil just like the official M3 Expressive previews.
 * - Corner Morphing: Inner corners smoothly morph between subtle connecting radius (6dp) and pill curves (18dp).
 * - Physical Press Dynamics: Compresses on press (0.93x) and snaps back with bouncy overshoot on release.
 * - Accessible Semantics: Configured with Role.RadioButton and minimum 48dp touch target height.
 * - Motion Reduction: Strictly respects [LocalMotionPreferences] to disable bouncy oscillations when requested.
 */
@Composable
fun <T> ConnectedButtonGroup(
    items: List<ConnectedButtonItem<T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = false,
    showCheckmark: Boolean = true
) {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    val outerRadius = 24.dp

    // M3 Expressive Spring Motion Specs (Snappier speed, preserved springiness)
    val bouncyFloatSpec: AnimationSpec<Float> = if (reduceMotion) snap() else spring(
        dampingRatio = 0.52f, // Visible elastic overshoot and bounce
        stiffness = 460f      // Increased stiffness for faster, punchy response
    )
    val pressScaleSpec: AnimationSpec<Float> = if (reduceMotion) snap() else spring(
        dampingRatio = 0.48f, // High spring bounce on release
        stiffness = 500f      // Snappy rebound
    )
    val bouncyDpSpec: AnimationSpec<Dp> = if (reduceMotion) snap() else spring(
        dampingRatio = 0.55f,
        stiffness = 460f
    )
    val colorSpec = if (reduceMotion) snap() else spring<androidx.compose.ui.graphics.Color>(
        dampingRatio = 0.70f,
        stiffness = 550f
    )

    val rowModifier = if (isScrollable) {
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    } else {
        modifier.fillMaxWidth()
    }

    // Expansion ratio per M3 Expressive specs: selected item expands, compressing adjacent items
    val expansionRatio = when {
        items.size <= 2 -> 0.28f
        items.size == 3 -> 0.22f
        items.size == 4 -> 0.16f
        else -> 0.12f
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = item.value == selectedItem
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            // Dynamic weight expansion with elastic spring
            val targetWeight = if (isSelected) 1f + expansionRatio else 1f
            val animatedWeight by animateFloatAsState(
                targetValue = targetWeight,
                animationSpec = bouncyFloatSpec,
                label = "ConnectedButtonWeight_$index"
            )

            // Dynamic padding for scrollable mode
            val targetPadding = if (isSelected) 22.dp else 14.dp
            val animatedPadding by animateDpAsState(
                targetValue = targetPadding,
                animationSpec = bouncyDpSpec,
                label = "ConnectedButtonPadding_$index"
            )

            // Corner morphing: active/pressed states morph inner corners
            val targetInner = when {
                isPressed -> 20.dp
                isSelected -> 16.dp
                else -> 6.dp
            }
            val animatedInnerRadius by animateDpAsState(
                targetValue = targetInner,
                animationSpec = bouncyDpSpec,
                label = "ConnectedButtonInnerRadius_$index"
            )

            val shape = when {
                items.size == 1 -> RoundedCornerShape(outerRadius)
                index == 0 -> RoundedCornerShape(
                    topStart = outerRadius,
                    bottomStart = outerRadius,
                    topEnd = animatedInnerRadius,
                    bottomEnd = animatedInnerRadius
                )
                index == items.lastIndex -> RoundedCornerShape(
                    topStart = animatedInnerRadius,
                    bottomStart = animatedInnerRadius,
                    topEnd = outerRadius,
                    bottomEnd = outerRadius
                )
                else -> RoundedCornerShape(animatedInnerRadius)
            }

            // Spring animated container colors matching M3 Expressive
            val targetContainerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            }
            val animatedContainerColor by animateColorAsState(
                targetValue = targetContainerColor,
                animationSpec = colorSpec,
                label = "ConnectedButtonContainerColor_$index"
            )

            val targetContentColor = if (isSelected) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
            val animatedContentColor by animateColorAsState(
                targetValue = targetContentColor,
                animationSpec = colorSpec,
                label = "ConnectedButtonContentColor_$index"
            )

            // Tactile press scale with spring recoil
            val animatedScale by animateFloatAsState(
                targetValue = if (isPressed) 0.93f else 1.0f,
                animationSpec = pressScaleSpec,
                label = "ConnectedButtonScale_$index"
            )

            val baseModifier = Modifier
                .zIndex(if (isSelected) 1f else 0f)
                .scale(animatedScale)
                .height(48.dp)
                .clip(shape)
                .background(animatedContainerColor, shape)
                .selectable(
                    selected = isSelected,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.RadioButton,
                    onClick = { onItemSelected(item.value) }
                )

            val itemModifier = if (isScrollable) {
                baseModifier.padding(horizontal = animatedPadding)
            } else {
                baseModifier
                    .weight(animatedWeight)
                    .padding(horizontal = if (items.size >= 5) 2.dp else 6.dp)
            }

            Box(
                modifier = itemModifier,
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showCheckmark) {
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(if (reduceMotion) snap() else spring(stiffness = 460f)) +
                                    expandHorizontally(
                                        animationSpec = if (reduceMotion) snap() else spring(
                                            dampingRatio = 0.52f,
                                            stiffness = 460f
                                        ),
                                        expandFrom = Alignment.Start
                                    ),
                            exit = fadeOut(if (reduceMotion) snap() else spring(stiffness = 550f)) +
                                   shrinkHorizontally(
                                       animationSpec = if (reduceMotion) snap() else spring(
                                           dampingRatio = 0.70f,
                                           stiffness = 550f
                                       ),
                                       shrinkTowards = Alignment.Start
                                   )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = animatedContentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    } else if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = animatedContentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.label,
                        style = if (items.size >= 5) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = animatedContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Multi-Select Connected Button Group.
 * Allows multiple items to be toggled independently, with dynamic corner morphing,
 * spring color transitions, accessible Checkbox semantics, and 48dp touch targets.
 */
@Composable
fun <T> MultiSelectConnectedButtonGroup(
    items: List<ConnectedButtonItem<T>>,
    selectedItems: Set<T>,
    onItemToggled: (T) -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = false,
    showCheckmark: Boolean = true
) {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    val outerRadius = 24.dp

    val bouncyFloatSpec: AnimationSpec<Float> = if (reduceMotion) snap() else spring(
        dampingRatio = 0.52f,
        stiffness = 460f
    )
    val pressScaleSpec: AnimationSpec<Float> = if (reduceMotion) snap() else spring(
        dampingRatio = 0.48f,
        stiffness = 500f
    )
    val bouncyDpSpec: AnimationSpec<Dp> = if (reduceMotion) snap() else spring(
        dampingRatio = 0.55f,
        stiffness = 460f
    )
    val colorSpec = if (reduceMotion) snap() else spring<androidx.compose.ui.graphics.Color>(
        dampingRatio = 0.70f,
        stiffness = 550f
    )

    val rowModifier = if (isScrollable) {
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    } else {
        modifier.fillMaxWidth()
    }

    val expansionRatio = when {
        items.size <= 2 -> 0.28f
        items.size == 3 -> 0.22f
        items.size == 4 -> 0.16f
        else -> 0.12f
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = item.value in selectedItems
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val targetWeight = if (isSelected) 1f + expansionRatio else 1f
            val animatedWeight by animateFloatAsState(
                targetValue = targetWeight,
                animationSpec = bouncyFloatSpec,
                label = "MultiConnectedButtonWeight_$index"
            )

            val targetPadding = if (isSelected) 22.dp else 14.dp
            val animatedPadding by animateDpAsState(
                targetValue = targetPadding,
                animationSpec = bouncyDpSpec,
                label = "MultiConnectedButtonPadding_$index"
            )

            val targetInner = when {
                isPressed -> 20.dp
                isSelected -> 16.dp
                else -> 6.dp
            }
            val animatedInnerRadius by animateDpAsState(
                targetValue = targetInner,
                animationSpec = bouncyDpSpec,
                label = "MultiConnectedButtonInnerRadius_$index"
            )

            val shape = when {
                items.size == 1 -> RoundedCornerShape(outerRadius)
                index == 0 -> RoundedCornerShape(
                    topStart = outerRadius,
                    bottomStart = outerRadius,
                    topEnd = animatedInnerRadius,
                    bottomEnd = animatedInnerRadius
                )
                index == items.lastIndex -> RoundedCornerShape(
                    topStart = animatedInnerRadius,
                    bottomStart = animatedInnerRadius,
                    topEnd = outerRadius,
                    bottomEnd = outerRadius
                )
                else -> RoundedCornerShape(animatedInnerRadius)
            }

            val targetContainerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            }
            val animatedContainerColor by animateColorAsState(
                targetValue = targetContainerColor,
                animationSpec = colorSpec,
                label = "MultiConnectedButtonContainerColor_$index"
            )

            val targetContentColor = if (isSelected) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
            val animatedContentColor by animateColorAsState(
                targetValue = targetContentColor,
                animationSpec = colorSpec,
                label = "MultiConnectedButtonContentColor_$index"
            )

            val animatedScale by animateFloatAsState(
                targetValue = if (isPressed) 0.93f else 1.0f,
                animationSpec = pressScaleSpec,
                label = "MultiConnectedButtonScale_$index"
            )

            val baseModifier = Modifier
                .zIndex(if (isSelected) 1f else 0f)
                .scale(animatedScale)
                .height(48.dp)
                .clip(shape)
                .background(animatedContainerColor, shape)
                .selectable(
                    selected = isSelected,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Checkbox,
                    onClick = { onItemToggled(item.value) }
                )

            val itemModifier = if (isScrollable) {
                baseModifier.padding(horizontal = animatedPadding)
            } else {
                baseModifier
                    .weight(animatedWeight)
                    .padding(horizontal = if (items.size >= 5) 2.dp else 6.dp)
            }

            Box(
                modifier = itemModifier,
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showCheckmark) {
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(if (reduceMotion) snap() else spring(stiffness = 460f)) +
                                    expandHorizontally(
                                        animationSpec = if (reduceMotion) snap() else spring(
                                            dampingRatio = 0.52f,
                                            stiffness = 460f
                                        ),
                                        expandFrom = Alignment.Start
                                    ),
                            exit = fadeOut(if (reduceMotion) snap() else spring(stiffness = 550f)) +
                                   shrinkHorizontally(
                                       animationSpec = if (reduceMotion) snap() else spring(
                                           dampingRatio = 0.70f,
                                           stiffness = 550f
                                       ),
                                       shrinkTowards = Alignment.Start
                                   )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = animatedContentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    } else if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = animatedContentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.label,
                        style = if (items.size >= 5) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = animatedContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}

