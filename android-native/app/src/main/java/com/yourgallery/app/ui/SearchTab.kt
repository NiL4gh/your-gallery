package com.yourgallery.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val filteredMedia = remember(query, mediaList) {
        if (query.isBlank()) mediaList else {
            mediaList.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                it.bucketName.contains(query, ignoreCase = true)
            }
        }
    }
    val gridState = rememberLazyGridState()

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search photos or albums...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

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
