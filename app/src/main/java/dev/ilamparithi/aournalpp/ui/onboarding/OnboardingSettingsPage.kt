package dev.ilamparithi.aournalpp.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import kotlinx.coroutines.launch

@Composable
fun OnboardingSettingsPage(
    context: Context,
    onContinue: () -> Unit
) {
    val aournalPrefs = remember { AppPreferences.getGeneral(context) }
    val x11Prefs = remember { X11Preferences.getPrefs(context) }

    // 1. Intelligent Session Recovery
    var intelligentRecovery by remember {
        mutableStateOf(aournalPrefs.getBoolean("pref_intelligent_emergency_recovery", true))
    }

    // 2. Fullscreen Canvas
    var fullscreenCanvas by remember {
        mutableStateOf(x11Prefs.getBoolean(X11Preferences.KEY_FULLSCREEN, false))
    }

    // 3. Reduce Animations
    var reduceAnimations by remember {
        mutableStateOf(aournalPrefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false))
    }

    // 3. Screen Idle Timeout
    var idleTimeout by remember {
        mutableStateOf(x11Prefs.getString(X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, "system") ?: "system")
    }
    var showTimeoutMenu by remember { mutableStateOf(false) }

    val timeoutOptions = listOf(
        "system" to androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.timeout_system),
        "never" to androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.timeout_never),
        "1" to androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.timeout_1m),
        "5" to androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.timeout_5m),
        "15" to androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.timeout_15m)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_settings_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_settings_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Settings Group Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                // Setting 1: Intelligent Session Recovery
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text(
                            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pref_session_recovery_title),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pref_session_recovery_desc))
                    },
                    trailingContent = {
                        Switch(
                            checked = intelligentRecovery,
                            onCheckedChange = {
                                intelligentRecovery = it
                                aournalPrefs.edit().putBoolean("pref_intelligent_emergency_recovery", it).apply()
                            }
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Setting 2: Fullscreen Canvas
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text(
                            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pref_fullscreen_title),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pref_fullscreen_desc))
                    },
                    trailingContent = {
                        Switch(
                            checked = fullscreenCanvas,
                            onCheckedChange = {
                                fullscreenCanvas = it
                                x11Prefs.edit().putBoolean(X11Preferences.KEY_FULLSCREEN, it).apply()
                                X11Preferences.notifyChanged(context, X11Preferences.KEY_FULLSCREEN)
                            }
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Setting 3: Screen Idle Timeout
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text(
                            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pref_idle_timeout_title),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pref_idle_timeout_desc))
                    },
                    trailingContent = {
                        Box {
                            val defaultTimeoutLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.timeout_system)
                            OutlinedButton(
                                onClick = { showTimeoutMenu = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = timeoutOptions.firstOrNull { it.first == idleTimeout }?.second
                                        ?: defaultTimeoutLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }

                            DropdownMenu(
                                expanded = showTimeoutMenu,
                                onDismissRequest = { showTimeoutMenu = false }
                            ) {
                                timeoutOptions.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            idleTimeout = key
                                            x11Prefs.edit().putString(X11Preferences.KEY_SCREEN_IDLE_TIMEOUT, key).apply()
                                            X11Preferences.notifyChanged(context, X11Preferences.KEY_SCREEN_IDLE_TIMEOUT)
                                            showTimeoutMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Setting 4: Reduce Animations
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text(
                            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pref_reduce_anim_title),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.pref_reduce_anim_desc))
                    },
                    trailingContent = {
                        Switch(
                            checked = reduceAnimations,
                            onCheckedChange = {
                                reduceAnimations = it
                                aournalPrefs.edit().putBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, it).apply()
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_more_settings_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_continue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

