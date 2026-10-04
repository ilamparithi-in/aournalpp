package dev.ilamparithi.aournalpp.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable Material 3 SearchBar component adhering strictly to official M3 guidelines
 * using Compose Material 3 experimental APIs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    placeholderText: String = "Search…",
    leadingIcon: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null
        )
    },
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {}
) {
    val effectiveTrailingIcon: @Composable (() -> Unit)? = trailingIcon ?: if (query.isNotEmpty()) {
        {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search"
                )
            }
        }
    } else null

    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                placeholder = { Text(placeholderText) },
                leadingIcon = leadingIcon,
                trailingIcon = effectiveTrailingIcon,
                enabled = enabled
            )
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        colors = colors,
        content = content
    )
}
