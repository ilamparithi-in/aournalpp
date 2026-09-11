package dev.ilamparithi.aournalpp.ui.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy
import dev.ilamparithi.aournalpp.backup.model.ExclusionFilterConfig
import dev.ilamparithi.aournalpp.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictPolicyCard(
    selectedPolicy: ConflictResolutionPolicy,
    onPolicySelected: (ConflictResolutionPolicy) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.cloud_policy_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.cloud_policy_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedPolicy.displayName,
                    onValueChange = {},
                    readOnly = true,
                    supportingText = { Text(selectedPolicy.description) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ConflictResolutionPolicy.entries.forEach { policy ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(policy.displayName, fontWeight = FontWeight.SemiBold)
                                    Text(policy.description, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            onClick = {
                                onPolicySelected(policy)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExclusionFiltersCard(
    filter: ExclusionFilterConfig,
    onFilterUpdated: (ExclusionFilterConfig) -> Unit,
    onConfigure: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.cloud_exclusion_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (filter.isWhitelistMode) "Mode: Whitelist (Include only matches)" else "Mode: Blacklist (Exclude matches)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (filter.isWhitelistMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(onClick = onConfigure) {
                    Text(stringResource(R.string.cloud_customize_button))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Switch 1: Whitelist vs Blacklist Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Whitelist Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (filter.isWhitelistMode) {
                            "Only notes matching configured extensions or folders will sync"
                        } else {
                            "Notes matching configured extensions or folders are skipped"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = filter.isWhitelistMode,
                    onCheckedChange = { onFilterUpdated(filter.copy(isWhitelistMode = it)) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Switch 2: Sync Trash Folder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sync Trash Folder (.Trash)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (filter.syncTrash) {
                            "Deleted notes in .Trash folder will be uploaded"
                        } else {
                            "Deleted notes in .Trash are excluded from cloud sync"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = filter.syncTrash,
                    onCheckedChange = { onFilterUpdated(filter.copy(syncTrash = it)) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            val transientPrefix = if (filter.skipDefaultTransient) "Transient & lock files ignored • " else ""
            val regexPluralRes = if (filter.isWhitelistMode) {
                R.plurals.cloud_inclusion_regex_patterns_count
            } else {
                R.plurals.cloud_exclusion_regex_patterns_count
            }
            val extPluralRes = if (filter.isWhitelistMode) {
                R.plurals.cloud_inclusion_extensions_count
            } else {
                R.plurals.cloud_exclusion_extensions_count
            }
            val regexFormatted = pluralStringResource(regexPluralRes, filter.regexPatterns.size, filter.regexPatterns.size)
            val extFormatted = pluralStringResource(extPluralRes, filter.excludedExtensions.size, filter.excludedExtensions.size)
            Text(
                text = stringResource(R.string.cloud_exclusion_card_summary, transientPrefix, regexFormatted, extFormatted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationCard(
    isAutoBackupOnExit: Boolean,
    onAutoBackupOnExitChange: (Boolean) -> Unit,
    isCheckRemoteOnLaunch: Boolean,
    onCheckRemoteOnLaunchChange: (Boolean) -> Unit,
    periodicIntervalMinutes: Int,
    onPeriodicIntervalChange: (Int) -> Unit,
    isDailyScheduledSync: Boolean,
    onDailyScheduledSyncChange: (Boolean) -> Unit,
    dailyHour: Int,
    dailyMinute: Int,
    onTimePickerClick: () -> Unit,
    isWifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    concurrency: Int,
    onConcurrencyChange: (Int) -> Unit
) {
    val timeFormatted = FormatUtils.formatTime(dailyHour, dailyMinute)
    var isFrequencyDropdownExpanded by remember { mutableStateOf(false) }

    val intervalOptions = listOf(
        0 to stringResource(R.string.pref_sync_freq_manual),
        5 to stringResource(R.string.pref_sync_freq_5m),
        15 to stringResource(R.string.pref_sync_freq_15m),
        30 to stringResource(R.string.pref_sync_freq_30m),
        60 to stringResource(R.string.pref_sync_freq_1h),
        1440 to stringResource(R.string.pref_sync_freq_daily)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.cloud_automation_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Check for Remote Changes on Launch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cloud_check_launch_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.cloud_check_launch_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isCheckRemoteOnLaunch,
                    onCheckedChange = onCheckRemoteOnLaunchChange
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Periodic Sync Interval (OneNote style)
            Column {
                Text(stringResource(R.string.cloud_sync_freq_title), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.cloud_sync_freq_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = isFrequencyDropdownExpanded,
                    onExpandedChange = { isFrequencyDropdownExpanded = !isFrequencyDropdownExpanded }
                ) {
                    val currentLabel = intervalOptions.firstOrNull { it.first == periodicIntervalMinutes }?.second ?: stringResource(R.string.pref_sync_freq_15m)
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFrequencyDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isFrequencyDropdownExpanded,
                        onDismissRequest = { isFrequencyDropdownExpanded = false }
                    ) {
                        intervalOptions.forEach { (mins, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onPeriodicIntervalChange(mins)
                                    isFrequencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Auto-backup on exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cloud_auto_backup_exit_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.cloud_auto_backup_exit_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isAutoBackupOnExit,
                    onCheckedChange = onAutoBackupOnExitChange
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Daily Scheduled Sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cloud_daily_sync_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.cloud_daily_sync_desc, timeFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDailyScheduledSync) {
                        IconButton(onClick = onTimePickerClick) {
                            Icon(Icons.Default.Schedule, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                    Switch(
                        checked = isDailyScheduledSync,
                        onDailyScheduledSyncChange
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Wi-Fi Only
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cloud_wifi_only_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.cloud_wifi_only_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isWifiOnly,
                    onCheckedChange = onWifiOnlyChange
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Concurrency Slider
            Column {
                Text(
                    text = stringResource(R.string.cloud_concurrency_title),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.cloud_concurrency_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pluralStringResource(R.plurals.cloud_concurrency_workers_label, concurrency, concurrency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = concurrency.toFloat(),
                    onValueChange = { onConcurrencyChange(it.toInt()) },
                    valueRange = 1f..4f,
                    steps = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
