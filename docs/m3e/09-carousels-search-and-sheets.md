# 09: Carousels, Adaptive Search, Sheets & Tooltips Architecture

## 1. Carousels: Multi-Browse & Uncontained

Material 3 Carousels provide adaptive, responsive horizontal browsing with built-in item focal point scaling, smooth snapping physics, and zero scroll contention.

Package: `androidx.compose.material3.carousel`

### 1.1. `HorizontalMultiBrowseCarousel`
Ideal for recent notes, document galleries, and template browsers where items dynamically adapt size across screen widths:

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentNotesCarousel(
    notes: List<NoteItem>,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) return

    val state = rememberCarouselState { notes.size }

    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 186.dp,
        itemSpacing = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(vertical = 8.dp)
    ) { index ->
        val note = notes[index]
        NoteCarouselCard(
            note = note,
            onClick = { onNoteClick(note) },
            modifier = Modifier.maskClip(MaterialTheme.shapes.large)
        )
    }
}
```

### 1.2. `HorizontalUncontainedCarousel`
For previewing variable-width content where items scroll freely without snapping to fixed slots.

---

## 2. Adaptive Search: `SearchBar` & `DockedSearchBar`

Material 3 Search adapts fluidly between full-screen search on compact screens (phones) and docked floating search on expanded screens (tablets and foldables).

Package: `androidx.compose.material3`

### 2.1. Canonical Implementation

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveNoteSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    searchResults: @Composable () -> Unit
) {
    val searchContent: @Composable () -> Unit = {
        searchResults()
    }

    if (isTablet) {
        // Tablet: Docked floating search dropdown
        DockedSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            active = active,
            onActiveChange = onActiveChange,
            placeholder = { Text("Search notes and annotations...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            } else null,
            modifier = modifier
        ) {
            searchContent()
        }
    } else {
        // Phone: Fullscreen expandable search
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            active = active,
            onActiveChange = onActiveChange,
            placeholder = { Text("Search notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = modifier.fillMaxWidth()
        ) {
            searchContent()
        }
    }
}
```

---

## 3. Pull-To-Refresh: `PullToRefreshBox`

The modern M3 pull-to-refresh container wraps scrollable lists cleanly with built-in spring indicators and elevation.

Package: `androidx.compose.material3.pulltorefresh`

```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableNoteList(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    notes: List<NoteItem>,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn {
            items(notes.size) { index ->
                NoteRow(notes[index])
            }
        }
    }
}
```

---

## 4. Modal Bottom Sheets: `ModalBottomSheet`

Material 3 bottom sheets handle drag handles, inset padding, edge-to-edge backdrop scrims, and TalkBack sheet dismissal natively.

Package: `androidx.compose.material3`

```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteOptionsBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        content()
    }
}
```

---

## 5. Expressive Tooltips: `TooltipBox` & `RichTooltip`

Tooltips in Material 3 Expressive support carets, action buttons, and keyboard navigation.

Package: `androidx.compose.material3`

```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylusHelpTooltip() {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            RichTooltip(
                title = { Text("Active Stylus Mode") },
                action = { TextButton(onClick = { /* Help */ }) { Text("Learn More") } }
            ) {
                Text("Hardware palm rejection is active for Precision Pen.")
            }
        },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = { /* Toggle */ }) {
            Icon(Icons.Default.Info, contentDescription = "Stylus Information")
        }
    }
}
```

---

## 6. Strict Anti-Mimicry Directives

❌ **PROHIBITED IN THIS REPOSITORY**:
1. Hand-rolling swipe-to-refresh using custom offset tracking or gesture listeners.
2. Building custom modal sheets with `Dialog` and manual slide-in animations.
3. Implementing custom floating search popups with `Popup` and manual bounds math.
4. Always use `PullToRefreshBox`, `ModalBottomSheet`, `DockedSearchBar`, and `HorizontalMultiBrowseCarousel`.
