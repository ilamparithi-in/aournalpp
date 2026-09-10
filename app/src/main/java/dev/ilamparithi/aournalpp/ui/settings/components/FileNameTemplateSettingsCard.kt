package dev.ilamparithi.aournalpp.ui.settings.components

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.utils.FileNameTemplateEngine
import java.io.File

enum class FileNameTemplateTarget(
    val key: String,
    val label: String,
    val defaultTemplate: String,
    val description: String,
    val icon: ImageVector
) {
    NEW_FILE(
        FileNameTemplateEngine.PREF_KEY_TEMPLATE_NEW_FILE,
        "New File",
        FileNameTemplateEngine.DEFAULT_TEMPLATE_NEW_FILE,
        "Default name pattern when creating a blank note in Home or Files Hub.",
        Icons.Default.Add
    ),
    SAVE_AS(
        FileNameTemplateEngine.PREF_KEY_TEMPLATE_SAVE_AS,
        "Save As",
        FileNameTemplateEngine.DEFAULT_TEMPLATE_SAVE_AS,
        "Default name pattern when duplicating or saving a note under a new copy.",
        Icons.Default.ContentCopy
    ),
    EXPORT_PDF(
        FileNameTemplateEngine.PREF_KEY_TEMPLATE_EXPORT_PDF,
        "Export PDF",
        FileNameTemplateEngine.DEFAULT_TEMPLATE_EXPORT_PDF,
        "Default file name pattern when exporting notes to Exports/ folder.",
        Icons.Default.FileDownload
    ),
    SHARE_PDF(
        FileNameTemplateEngine.PREF_KEY_TEMPLATE_SHARE_PDF,
        "Share PDF",
        FileNameTemplateEngine.DEFAULT_TEMPLATE_SHARE_PDF,
        "Default file name pattern when rendering and sharing PDF with external apps.",
        Icons.Default.PictureAsPdf
    ),
    SHARE_XOPP(
        FileNameTemplateEngine.PREF_KEY_TEMPLATE_SHARE_XOPP,
        "Share XOPP",
        FileNameTemplateEngine.DEFAULT_TEMPLATE_SHARE_XOPP,
        "Default file name pattern when sharing the notebook (.xopp) file.",
        Icons.Default.Share
    )
}

data class TemplatePlaceholderGuide(
    val token: String,
    val label: String,
    val description: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileNameTemplateSettingsCard(
    prefs: android.content.SharedPreferences,
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    var selectedTarget by remember { mutableStateOf(FileNameTemplateTarget.NEW_FILE) }

    var currentTemplateText by remember(selectedTarget) {
        mutableStateOf(prefs.getString(selectedTarget.key, selectedTarget.defaultTemplate) ?: selectedTarget.defaultTemplate)
    }

    var selectedGuideCategory by remember { mutableStateOf(0) }
    var selectedPlaceholderHelp by remember { mutableStateOf<TemplatePlaceholderGuide?>(null) }

    val dummySampleFile = remember {
        File(
            File(
                File(context.filesDir, "sample_root"),
                "Physics_101"
            ),
            "Mechanics_Lecture.xopp"
        )
    }

    val livePreviewResult = remember(currentTemplateText, selectedTarget) {
        try {
            FileNameTemplateEngine.evaluate(currentTemplateText, context, dummySampleFile)
        } catch (e: Exception) {
            "Invalid template syntax"
        }
    }

    val datePlaceholders = remember {
        listOf(
            TemplatePlaceholderGuide("{date}", "Date (yyyy-MM-dd)", "Current date formatted as 2026-08-30"),
            TemplatePlaceholderGuide("{time}", "Time (HH-mm-ss)", "Current time formatted as 14-30-00"),
            TemplatePlaceholderGuide("{datetime}", "DateTime (ISO)", "Combined timestamp: 2026-08-30_14-30-00"),
            TemplatePlaceholderGuide("{datetime:yyyy_MM_dd}", "Custom Format", "Custom SimpleDateFormat pattern inside {datetime:PATTERN}"),
            TemplatePlaceholderGuide("{year}", "Year (YYYY)", "4-digit current year e.g. 2026"),
            TemplatePlaceholderGuide("{month}", "Month (MM)", "2-digit month e.g. 08"),
            TemplatePlaceholderGuide("{day}", "Day (dd)", "2-digit day of month e.g. 30"),
            TemplatePlaceholderGuide("{hour}", "Hour (HH)", "2-digit 24-hour hour e.g. 14"),
            TemplatePlaceholderGuide("{minute}", "Minute (mm)", "2-digit minute e.g. 30"),
            TemplatePlaceholderGuide("{second}", "Second (ss)", "2-digit second e.g. 45")
        )
    }

    val contextPlaceholders = remember {
        listOf(
            TemplatePlaceholderGuide("{name}", "Note Name", "Base name of current note without extension (e.g. Mechanics_Lecture)"),
            TemplatePlaceholderGuide("{filename}", "Full Filename", "Original file name with extension"),
            TemplatePlaceholderGuide("{ext}", "Extension", "File extension without dot (e.g. xopp, pdf)"),
            TemplatePlaceholderGuide("{folder}", "Folder Name", "Direct parent folder name (e.g. Physics_101)"),
            TemplatePlaceholderGuide("{folder:1}", "Folder (Level N)", "Folder name N levels up in folder path hierarchy"),
            TemplatePlaceholderGuide("{folders}", "Folder Hierarchy", "Full relative folder path separated by underscores")
        )
    }

    val metadataPlaceholders = remember {
        listOf(
            TemplatePlaceholderGuide("{created}", "Created Date", "File creation timestamp (yyyy-MM-dd)"),
            TemplatePlaceholderGuide("{created:yyyy_MM_dd}", "Custom Created", "Custom SimpleDateFormat pattern for file creation time"),
            TemplatePlaceholderGuide("{modified}", "Modified Date", "File last modified timestamp (yyyy-MM-dd)"),
            TemplatePlaceholderGuide("{modified:HH-mm}", "Custom Modified", "Custom SimpleDateFormat pattern for modification time")
        )
    }

    val randomPlaceholders = remember {
        listOf(
            TemplatePlaceholderGuide("{random}", "Random (4 chars)", "4 random alphanumeric characters for unique filenames"),
            TemplatePlaceholderGuide("{random:6}", "Random (N chars)", "Custom N random alphanumeric characters e.g. {random:6}")
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Configure customizable file name templates using interactive placeholders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Scrollable Category Tabs for 5 Targets
            ScrollableTabRow(
                selectedTabIndex = selectedTarget.ordinal,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                FileNameTemplateTarget.entries.forEach { target ->
                    Tab(
                        selected = selectedTarget == target,
                        onClick = { selectedTarget = target },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = target.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = target.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTarget == target) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            // Description of active target
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedTarget.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Template Editor TextField with Long-Press Reset
            OutlinedTextField(
                value = currentTemplateText,
                onValueChange = {
                    currentTemplateText = it
                    prefs.edit().putString(selectedTarget.key, it).apply()
                },
                label = { Text("${selectedTarget.label} Pattern") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .pointerInput(selectedTarget) {
                                detectTapGestures(
                                    onTap = {
                                        Toast.makeText(context, "Long press to reset", Toast.LENGTH_SHORT).show()
                                    },
                                    onLongPress = {
                                        currentTemplateText = selectedTarget.defaultTemplate
                                        prefs.edit().putString(selectedTarget.key, selectedTarget.defaultTemplate).apply()
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                                                vm?.defaultVibrator?.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                            } else {
                                                @Suppress("DEPRECATION")
                                                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                                @Suppress("DEPRECATION")
                                                v?.vibrate(50)
                                            }
                                        } catch (_: Exception) {}
                                        Toast.makeText(context, "Reset ${selectedTarget.label} pattern to default", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Long press to reset pattern",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            // Live Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Live Evaluation Preview",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val extensionSuffix = when (selectedTarget) {
                        FileNameTemplateTarget.EXPORT_PDF, FileNameTemplateTarget.SHARE_PDF -> ".pdf"
                        else -> ".xopp"
                    }
                    Text(
                        text = "$livePreviewResult$extensionSuffix",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider()

            // Interactive Placeholder Guide & Reference Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Placeholder Guide & Quick Insert",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tab ribbon matching the target template tab ribbon above
            val guideCategories = listOf("Date/Time", "Context", "Metadata", "Random")
            ScrollableTabRow(
                selectedTabIndex = selectedGuideCategory,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                guideCategories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedGuideCategory == index,
                        onClick = { selectedGuideCategory = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedGuideCategory == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            val activePlaceholders = when (selectedGuideCategory) {
                0 -> datePlaceholders
                1 -> contextPlaceholders
                2 -> metadataPlaceholders
                else -> randomPlaceholders
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activePlaceholders.forEach { guide ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            selectedPlaceholderHelp = guide
                            val separator = if (currentTemplateText.isNotEmpty() && !currentTemplateText.endsWith("-") && !currentTemplateText.endsWith("_")) "-" else ""
                            currentTemplateText = currentTemplateText + separator + guide.token
                            prefs.edit().putString(selectedTarget.key, currentTemplateText).apply()
                        },
                        label = {
                            Text(
                                text = guide.token,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            selectedPlaceholderHelp?.let { help ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = help.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = help.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
