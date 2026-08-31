package dev.ilamparithi.aournalpp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Encapsulated App Logo Badge Component.
 *
 * Serves as the single source of truth for the app's brand badge (styled "A" badge on [primaryContainer]).
 * Used in the Home Screen top bar (in portrait) and in the Navigation Rail header (in landscape/tablet).
 * When an icon or drawable asset is introduced in the future, updating this composable will automatically
 * propagate the icon across both locations.
 */
@Composable
fun AppLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(
        shape = shape,
        color = containerColor,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "A",
                style = if (size <= 32.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = contentColor
            )
        }
    }
}
