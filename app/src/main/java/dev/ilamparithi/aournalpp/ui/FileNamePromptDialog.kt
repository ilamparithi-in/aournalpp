package dev.ilamparithi.aournalpp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.utils.FileNameTemplateEngine

enum class FileActionPromptType {
    EXPORT_PDF,
    SHARE_PDF,
    SHARE_XOPP
}

@Composable
fun FileNamePromptDialog(
    title: String,
    subtitle: String? = null,
    extension: String,
    icon: ImageVector,
    initialName: String,
    confirmButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: (sanitizedName: String) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(0, initialName.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val sanitized = FileNameTemplateEngine.sanitizeFileName(textFieldValue.text)
    val isError = textFieldValue.text.isNotBlank() && sanitized.isEmpty()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = isLandscape),
        modifier = if (isLandscape) Modifier else Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        icon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!subtitle.isNullOrBlank()) {
                    InteractiveMarqueeText(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    label = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_note_name_hint)) },
                    suffix = {
                        Text(
                            text = extension,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    trailingIcon = {
                        if (textFieldValue.text.isNotEmpty()) {
                            IconButton(onClick = { textFieldValue = TextFieldValue("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_clear),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("File name cannot be empty or contain invalid symbols") }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sanitized.isNotBlank()) {
                        onConfirm(sanitized)
                    }
                },
                enabled = sanitized.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel))
            }
        }
    )
}
