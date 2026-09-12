package dev.ilamparithi.aournalpp.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import dev.ilamparithi.aournalpp.ui.animation.AppAnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.BootstrapState
import dev.ilamparithi.aournalpp.ui.ExpressiveHeroSpinner
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.ui.theme.CloverShape
import dev.ilamparithi.aournalpp.ui.theme.SunnyShape
import dev.ilamparithi.aournalpp.utils.FormatUtils
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.ConfigSyncStatus
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.FileVersionItem
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager
import dev.ilamparithi.aournalpp.ui.cloud.ConfigDiffActivity
import dev.ilamparithi.aournalpp.ui.cloud.ConflictDialogMode
import dev.ilamparithi.aournalpp.ui.cloud.FolderBrowserDialog
import dev.ilamparithi.aournalpp.ui.cloud.FolderBrowserMode
import dev.ilamparithi.aournalpp.ui.cloud.MultiServiceConflictDialog
import dev.ilamparithi.aournalpp.ui.cloud.ServiceConfigDialog
import dev.ilamparithi.aournalpp.ui.cloud.CloudProviderIcon
import dev.ilamparithi.aournalpp.ui.InteractiveMarqueeText
import java.io.File
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch

@Composable
fun OnboardingExtractionBottomPill(
    state: BootstrapState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val installingState = state as? BootstrapState.Installing
    val progress = installingState?.progress
    val isReady = state is BootstrapState.Ready
    val isError = state is BootstrapState.Error

    // Auto-dismiss "Linux environment ready" 10 seconds after completion
    var isReadyDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state is BootstrapState.Ready) {
            delay(10.seconds)
            isReadyDismissed = true
        } else {
            isReadyDismissed = false
        }
    }

    AppAnimatedVisibility(
        visible = !isReadyDismissed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Collapsed Minimal Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isReady) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isError) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            ExpressiveHeroSpinner(
                                size = 20.dp,
                                icon = Icons.Default.Sync,
                                iconDescription = null
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = when {
                                isReady -> androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pill_env_ready)
                                isError -> androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pill_env_issue)
                                progress != null -> androidx.compose.ui.res.stringResource(
                                    dev.ilamparithi.aournalpp.R.string.pill_setting_up,
                                    FormatUtils.formatNumber(progress.percentage, maxDecimals = 0)
                                )
                                else -> androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pill_preparing)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = if (isExpanded) androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_hide)
                        else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_details),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Compact progress bar under minimal header when not fully ready
                if (!isReady && !isError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (progress != null) {
                        val percentage = (progress.percentage / 100f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                // Expandable Detailed Card
                AppAnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 10.dp))

                        if (progress != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pill_extracted_size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${progress.extractedBytes / (1024 * 1024)} MB",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pill_current_file, progress.currentFile),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (isReady) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pill_ready_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (isError) {
                            val err = state.throwable
                            Text(
                                text = androidx.compose.ui.res.stringResource(
                                    dev.ilamparithi.aournalpp.R.string.pill_error_prefix,
                                    err.message ?: "Unknown extraction error"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pill_unpacking_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pill_tap_collapse),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
