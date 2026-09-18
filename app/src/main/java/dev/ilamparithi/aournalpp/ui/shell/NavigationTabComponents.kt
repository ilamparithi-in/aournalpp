package dev.ilamparithi.aournalpp.ui.shell

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Shared composable for tab icons in both NavigationRail and NavigationBar,
 * handling session window count badge and emergency/conflict alert red dots.
 */
@Composable
fun NavigationTabIcon(
    tab: AppTab,
    isSelected: Boolean,
    isSessionRunning: Boolean,
    windowCount: Int,
    showFilesRedDot: Boolean,
    showCloudRedDot: Boolean,
    modifier: Modifier = Modifier
) {
    val tabTitle = stringResource(tab.titleRes)
    val isWorkspace = tab == AppTab.WORKSPACE

    if (isWorkspace && isSessionRunning) {
        BadgedBox(
            modifier = modifier,
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = if (windowCount > 9) "9+" else windowCount.coerceAtLeast(1).toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        ) {
            Icon(
                imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                contentDescription = tabTitle,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    } else if (showFilesRedDot) {
        BadgedBox(
            modifier = modifier,
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            }
        ) {
            Icon(
                imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                contentDescription = tabTitle,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    } else if (showCloudRedDot) {
        BadgedBox(
            modifier = modifier,
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            }
        ) {
            Icon(
                imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                contentDescription = tabTitle,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    } else {
        Icon(
            modifier = modifier,
            imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
            contentDescription = tabTitle
        )
    }
}

/**
 * Shared composable for tab text labels in both NavigationRail and NavigationBar.
 */
@Composable
fun NavigationTabLabel(
    tab: AppTab,
    isSelected: Boolean,
    isSessionRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val tabTitle = stringResource(tab.titleRes)
    val isWorkspace = tab == AppTab.WORKSPACE

    Text(
        modifier = modifier,
        text = tabTitle,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isWorkspace && isSessionRunning && !isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified
    )
}
