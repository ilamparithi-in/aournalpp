package dev.ilamparithi.aournalpp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.model.NoteFileType

fun getFileTypeColor(fileType: NoteFileType): Color {
    return when (fileType) {
        NoteFileType.XOPP -> Color(0xFF3F51B5) // Indigo Blue (.xopp)
        NoteFileType.XOJ -> Color(0xFFE65100)  // Deep Orange / Warm Amber (.xoj)
        NoteFileType.PDF -> Color(0xFFD32F2F)  // Crimson Red (.pdf)
    }
}

@Composable
fun FileTypePill(
    fileType: NoteFileType,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
    fontSize: Float = 10f
) {
    val badgeColor = getFileTypeColor(fileType)
    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = badgeColor.copy(alpha = 0.90f),
        modifier = modifier
    ) {
        Text(
            text = fileType.displayName, // ".xopp", ".xoj", ".pdf"
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = fontSize.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
