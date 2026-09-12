package dev.ilamparithi.aournalpp.ui.home

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo
import dev.ilamparithi.aournalpp.ui.AppLogoBadge
import dev.ilamparithi.aournalpp.ui.animation.AppAnimatedVisibility
import dev.ilamparithi.aournalpp.ui.cloud.QuickSyncButton

/**
 * Top app bar for the Home screen, featuring the app title/badge, dynamic fun subhero pill on scroll,
 * active session status banner, and quick sync action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    isWideOrLandscape: Boolean,
    isScrolled: Boolean,
    funSubhero: String,
    activeSession: ActiveSessionInfo?,
    onReturnToActiveSession: () -> Unit,
    onSyncFinished: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Suppress the "A" logo badge in landscape/wide mode (where the navigation rail is on the left)
                    if (!isWideOrLandscape) {
                        AppLogoBadge(
                            size = 36.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dynamic fun subhero badge appearing in top bar when scrolled up
                AppAnimatedVisibility(
                    visible = isScrolled,
                    enter = fadeIn() + slideInHorizontally { it / 2 },
                    exit = fadeOut() + slideOutHorizontally { it / 2 }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = funSubhero,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        actions = {
            AppAnimatedVisibility(
                visible = activeSession?.isRunning == true,
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = fadeOut() + slideOutHorizontally { it / 2 }
            ) {
                activeSession?.let { session ->
                    HomeActiveSessionBanner(
                        sessionInfo = session,
                        onClick = onReturnToActiveSession
                    )
                }
            }

            QuickSyncButton(
                onSyncFinished = onSyncFinished
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}
