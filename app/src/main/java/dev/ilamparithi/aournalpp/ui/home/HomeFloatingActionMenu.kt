package dev.ilamparithi.aournalpp.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.ui.SpeedDialActionItem
import dev.ilamparithi.aournalpp.ui.animation.AppAnimatedVisibility
import dev.ilamparithi.aournalpp.ui.animation.LocalMotionPreferences

/**
 * Expressive Speed Dial Floating Action Menu for the Home screen, featuring staggered spring
 * action items (Create Folder, Open File, Create Note) and an animated rotating FAB with dimming scrim.
 */
@Composable
fun HomeFloatingActionMenu(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreateFolderClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onCreateNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceAnimations = LocalMotionPreferences.current.reduceAnimations

    val fabRotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = if (reduceAnimations) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fabRotation"
    )

    // Animated dimming backdrop scrim
    AppAnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(animationSpec = spring(stiffness = 400f)),
        exit = fadeOut(animationSpec = spring(stiffness = 400f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExpandedChange(false) }
        )
    }

    // Speed Dial Action Items + Main FAB
    Box(
        modifier = modifier
            .padding(24.dp)
    ) {
        val folderItemSpring by animateFloatAsState(
            targetValue = if (isExpanded) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
            label = "folderItemSpring"
        )
        val pdfItemSpring by animateFloatAsState(
            targetValue = if (isExpanded) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f),
            label = "pdfItemSpring"
        )
        val noteItemSpring by animateFloatAsState(
            targetValue = if (isExpanded) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
            label = "noteItemSpring"
        )

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Staggered Spring Action Items
            SpeedDialActionItem(
                progress = folderItemSpring,
                icon = Icons.Default.CreateNewFolder,
                label = stringResource(R.string.hub_create_folder),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = {
                    onExpandedChange(false)
                    onCreateFolderClick()
                }
            )

            SpeedDialActionItem(
                progress = pdfItemSpring,
                icon = Icons.Default.FileOpen,
                label = stringResource(R.string.action_open),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = {
                    onExpandedChange(false)
                    onOpenFileClick()
                }
            )

            SpeedDialActionItem(
                progress = noteItemSpring,
                icon = Icons.Default.Edit,
                label = stringResource(R.string.hub_create_note),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = {
                    onExpandedChange(false)
                    onCreateNoteClick()
                }
            )

            // Main Speed Dial FAB with spring physics on press & rotate
            val fabInteractionSource = remember { MutableInteractionSource() }
            val isFabPressed by fabInteractionSource.collectIsPressedAsState()
            val fabPressScale by animateFloatAsState(
                targetValue = if (isFabPressed) 0.90f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "fabPressScale"
            )

            FloatingActionButton(
                onClick = { onExpandedChange(!isExpanded) },
                interactionSource = fabInteractionSource,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .size(64.dp)
                    .scale(fabPressScale)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_expand_doc_actions),
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(fabRotation)
                )
            }
        }
    }
}
