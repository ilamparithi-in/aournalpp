package dev.ilamparithi.aournalpp.ui.cloud

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme
import dev.ilamparithi.aournalpp.ui.util.a11yHeading
import java.io.File

enum class DiffLineType {
    SAME,
    ADDED,
    DELETED
}

data class DiffLine(
    val type: DiffLineType,
    val text: String,
    val oldLineNum: Int?,
    val newLineNum: Int?
)

/**
 * Full-page activity displaying a rich, side-by-side or unified line diff between local and cloud configuration files.
 */
class ConfigDiffActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_LOCAL_PATH = "extra_local_path"
        const val EXTRA_REMOTE_PATH = "extra_remote_path"
        const val EXTRA_LOCAL_LABEL = "extra_local_label"
        const val EXTRA_REMOTE_LABEL = "extra_remote_label"

        fun createIntent(
            context: Context,
            fileName: String,
            localPath: String,
            remotePath: String,
            localLabel: String = "Local Version",
            remoteLabel: String = "Cloud Version"
        ): Intent {
            return Intent(context, ConfigDiffActivity::class.java).apply {
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_LOCAL_PATH, localPath)
                putExtra(EXTRA_REMOTE_PATH, remotePath)
                putExtra(EXTRA_LOCAL_LABEL, localLabel)
                putExtra(EXTRA_REMOTE_LABEL, remoteLabel)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Config"
        val localPath = intent.getStringExtra(EXTRA_LOCAL_PATH) ?: ""
        val remotePath = intent.getStringExtra(EXTRA_REMOTE_PATH) ?: ""
        val localLabel = intent.getStringExtra(EXTRA_LOCAL_LABEL) ?: getString(R.string.label_diff_local_side)
        val remoteLabel = intent.getStringExtra(EXTRA_REMOTE_LABEL) ?: getString(R.string.label_diff_cloud_side)

        val localText = if (localPath.isNotBlank()) {
            val f = File(localPath)
            if (f.exists() && f.isFile) f.readText(Charsets.UTF_8) else ""
        } else ""

        val remoteText = if (remotePath.isNotBlank()) {
            val f = File(remotePath)
            if (f.exists() && f.isFile) f.readText(Charsets.UTF_8) else ""
        } else ""

        val diffLines = computeDiff(localText, remoteText)

        setContent {
            AournalTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = fileName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.a11yHeading()
                                    )
                                    Text(
                                        text = "$localLabel  vs  $remoteLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.action_back)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { paddingValues ->
                    ConfigDiffContent(
                        diffLines = diffLines,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }
    }

    private fun computeDiff(text1: String, text2: String): List<DiffLine> {
        val lines1 = if (text1.isEmpty()) emptyList() else text1.lines()
        val lines2 = if (text2.isEmpty()) emptyList() else text2.lines()

        val n = lines1.size
        val m = lines2.size

        // Standard Longest Common Subsequence (LCS) matrix
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in 0 until n) {
            for (j in 0 until m) {
                if (lines1[i] == lines2[j]) {
                    lcs[i + 1][j + 1] = lcs[i][j] + 1
                } else {
                    lcs[i + 1][j + 1] = maxOf(lcs[i + 1][j], lcs[i][j + 1])
                }
            }
        }

        val result = mutableListOf<DiffLine>()
        var i = n
        var j = m

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && lines1[i - 1] == lines2[j - 1]) {
                result.add(
                    DiffLine(
                        type = DiffLineType.SAME,
                        text = lines1[i - 1],
                        oldLineNum = i,
                        newLineNum = j
                    )
                )
                i--
                j--
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                result.add(
                    DiffLine(
                        type = DiffLineType.ADDED,
                        text = lines2[j - 1],
                        oldLineNum = null,
                        newLineNum = j
                    )
                )
                j--
            } else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
                result.add(
                    DiffLine(
                        type = DiffLineType.DELETED,
                        text = lines1[i - 1],
                        oldLineNum = i,
                        newLineNum = null
                    )
                )
                i--
            }
        }

        return result.reversed()
    }
}

@Composable
fun ConfigDiffContent(
    diffLines: List<DiffLine>,
    modifier: Modifier = Modifier
) {
    val addedCount = remember(diffLines) { diffLines.count { it.type == DiffLineType.ADDED } }
    val deletedCount = remember(diffLines) { diffLines.count { it.type == DiffLineType.DELETED } }

    Column(modifier = modifier) {
        // Summary header strip
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$addedCount additions (Cloud)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$deletedCount deletions (Local)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }

        if (diffLines.isEmpty() || (addedCount == 0 && deletedCount == 0)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.label_diff_identical),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val horizontalScrollState = rememberScrollState()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                itemsIndexed(diffLines) { _, line ->
                    DiffLineRow(line = line)
                }
            }
        }
    }
}

@Composable
fun DiffLineRow(line: DiffLine) {
    val backgroundColor = when (line.type) {
        DiffLineType.SAME -> Color.Transparent
        DiffLineType.ADDED -> Color(0x224CAF50)
        DiffLineType.DELETED -> Color(0x22F44336)
    }

    val textColor = when (line.type) {
        DiffLineType.SAME -> MaterialTheme.colorScheme.onSurface
        DiffLineType.ADDED -> Color(0xFF2E7D32)
        DiffLineType.DELETED -> Color(0xFFC62828)
    }

    val prefixSymbol = when (line.type) {
        DiffLineType.SAME -> " "
        DiffLineType.ADDED -> "+"
        DiffLineType.DELETED -> "-"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 1.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Line numbers
        Text(
            text = (line.oldLineNum?.toString() ?: "").padStart(4, ' '),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = (line.newLineNum?.toString() ?: "").padStart(4, ' '),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.width(36.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Prefix (+, -, space)
        Text(
            text = prefixSymbol,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = textColor
            ),
            modifier = Modifier.width(14.dp)
        )

        // Line content
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = textColor
            )
        )
    }
}
