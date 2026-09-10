package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.ui.StandardNoteCard
import dev.ilamparithi.aournalpp.utils.a11yHeading

/**
 * Dynamic horizontal multi-browse carousel.
 * Powered by Material 3 HorizontalMultiBrowseCarousel for authentic, adaptive responsive cards,
 * buttery smooth gesture physics, and zero rubberband scroll contention.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicRecentsCarousel(
    recentNotes: List<NoteDocument>,
    pdfExportManager: PdfExportManager,
    onOpenNote: (NoteDocument) -> Unit,
    onTogglePin: (NoteDocument) -> Unit,
    onShareExport: ((NoteDocument) -> Unit)? = null,
    onExportPdf: ((NoteDocument) -> Unit)? = null,
    onSharePdf: ((NoteDocument) -> Unit)? = null,
    onShareXopp: ((NoteDocument) -> Unit)? = null,
    onDuplicate: ((NoteDocument) -> Unit)? = null,
    onDeleteNote: (NoteDocument) -> Unit,
    onRenameNote: (NoteDocument) -> Unit
) {
    if (recentNotes.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.a11yHeading()
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.home_title_recent_notes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                pluralStringResource(R.plurals.hub_recent_notes_count, recentNotes.size, recentNotes.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { recentNotes.size },
            preferredItemWidth = 230.dp,
            itemSpacing = 10.dp,
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
        ) { page ->
            val note = recentNotes.getOrNull(page) ?: return@HorizontalMultiBrowseCarousel
            StandardNoteCard(
                note = note,
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge),
                shape = MaterialTheme.shapes.extraLarge,
                pdfExportManager = pdfExportManager,
                onClick = { onOpenNote(note) },
                onTogglePin = { onTogglePin(note) },
                onShareExport = onShareExport?.let { { it(note) } },
                onExportPdf = onExportPdf?.let { { it(note) } },
                onSharePdf = onSharePdf?.let { { it(note) } },
                onShareXopp = onShareXopp?.let { { it(note) } },
                onRename = { onRenameNote(note) },
                onDuplicate = onDuplicate?.let { { it(note) } },
                onDelete = { onDeleteNote(note) }
            )
        }
    }
}
