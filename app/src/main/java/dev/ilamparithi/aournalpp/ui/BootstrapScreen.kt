package dev.ilamparithi.aournalpp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BootstrapScreen(
    state: BootstrapState,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isError = state is BootstrapState.Error
            
            Icon(
                imageVector = if (isError) Icons.Default.Warning else Icons.Default.Edit,
                contentDescription = "Stylus Branding",
                modifier = Modifier.size(64.dp),
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Initializing Linux Environment",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (!isError) {
                val installingState = state as? BootstrapState.Installing
                val progress = installingState?.progress

                val progressMessage = when (state) {
                    is BootstrapState.Checking -> "Checking runtime version and environment..."
                    is BootstrapState.Installing -> {
                        if (state.message.isNotEmpty()) {
                            state.message
                        } else if (state.progress != null) {
                            "Extracting ${state.progress.currentFile}..."
                        } else {
                            "Extracting application packages..."
                        }
                    }
                    else -> ""
                }
                
                if (progress != null) {
                    val percentage = progress.percentage / 100f
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${String.format("%.1f", progress.percentage)}% • ${progress.extractedBytes / (1024 * 1024)} MB extracted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = progressMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = (state as BootstrapState.Error).throwable.message ?: "Unknown initialization failure occurred.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                FilledTonalButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
