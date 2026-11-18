package com.yourgallery.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.yourgallery.app.DeviceMedia

@Composable
fun SearchView(
    query: String,
    onQueryChange: (String) -> Unit,
    mediaList: List<DeviceMedia>,
    imageLoader: ImageLoader,
    selectedMediaIds: Set<Long>,
    gridColumns: Int,
    onItemClick: (DeviceMedia) -> Unit,
    onItemLongClick: (DeviceMedia) -> Unit,
    onDragSelect: (DeviceMedia) -> Unit = {},
    onSelectGroup: (List<DeviceMedia>) -> Unit = {}
) {
    var activeFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Photos", "Videos", "Favorites", "Screenshots")

    // Filter out trashed and locked vault photos to prevent leaks
    val filteredMedia = remember(query, mediaList, activeFilter) {
        val baseVisible = mediaList.filter { it.trashedAt == 0L && !it.isHidden }
        val categoryFiltered = when (activeFilter) {
            "Photos" -> baseVisible.filter { !it.isVideo }
            "Videos" -> baseVisible.filter { it.isVideo }
            "Favorites" -> baseVisible.filter { it.isFavorite }
            "Screenshots" -> baseVisible.filter { it.bucketName.contains("Screenshot", ignoreCase = true) }
            else -> baseVisible
        }

        if (query.isBlank()) {
            categoryFiltered
        } else {
            val q = query.trim().lowercase()
            categoryFiltered.filter { item ->
                item.displayName.lowercase().contains(q) ||
                item.bucketName.lowercase().contains(q) ||
                item.caption.lowercase().contains(q) ||
                item.tags.lowercase().contains(q)
            }
        }
    }
    val gridState = rememberLazyGridState()

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search photos, albums, or tags...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { opt ->
                FilterChip(
                    selected = activeFilter == opt,
                    onClick = { activeFilter = opt },
                    label = { Text(opt) }
                )
            }
        }

        MediaGrid(
            mediaList = filteredMedia,
            imageLoader = imageLoader,
            selectedMediaIds = selectedMediaIds,
            gridColumns = gridColumns,
            gridState = gridState,
            onItemClick = { item, _ -> onItemClick(item) },
            onItemLongClick = onItemLongClick,
            onDragSelect = onDragSelect,
            onSelectGroup = onSelectGroup
        )
    }
}
