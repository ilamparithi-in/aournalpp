package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.ui.common.AppIconButton

@Composable
fun DocumentHubSelectionBar(
    isVisible: Boolean,
    isViewingTrash: Boolean,
    selectedDocs: List<NoteDocument>,
    onRestoreSelected: () -> Unit,
    onDeletePermanentlySelected: () -> Unit,
    onTogglePinSelected: () -> Unit,
    onMoveToFolderSelected: () -> Unit,
    onShareExportSelected: () -> Unit,
    onMoveToTrashSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && selectedDocs.isNotEmpty(),
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 10.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isViewingTrash) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRestoreSelected,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = stringResource(R.string.action_restore),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.action_restore)} (${selectedDocs.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onDeletePermanentlySelected,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete_permanent),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_delete),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                val allSelectedPinned = selectedDocs.isNotEmpty() && selectedDocs.all { it.isPinned }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pinActionLabel = if (allSelectedPinned) stringResource(R.string.action_unpin)
                    else stringResource(R.string.action_pin)
                    AppIconButton(
                        onClick = onTogglePinSelected,
                        tooltip = pinActionLabel
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = pinActionLabel,
                                tint = if (allSelectedPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = pinActionLabel,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }

                    val moveActionLabel = stringResource(R.string.action_move)
                    AppIconButton(
                        onClick = onMoveToFolderSelected,
                        tooltip = moveActionLabel
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.DriveFileMove,
                                contentDescription = moveActionLabel,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    val shareExportLabel = stringResource(R.string.action_share_export)
                    AppIconButton(
                        onClick = onShareExportSelected,
                        tooltip = shareExportLabel
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = shareExportLabel
                            )
                        }
                    }

                    val deleteLabel = stringResource(R.string.action_delete)
                    AppIconButton(
                        onClick = onMoveToTrashSelected,
                        tooltip = deleteLabel
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = deleteLabel,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
