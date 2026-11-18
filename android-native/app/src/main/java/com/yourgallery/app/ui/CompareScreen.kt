package com.yourgallery.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.yourgallery.app.DeviceMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    mediaItems: List<DeviceMedia>,
    imageLoader: ImageLoader,
    onClose: () -> Unit,
    onFavorite: (DeviceMedia) -> Unit,
    onDelete: (DeviceMedia) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare (${mediaItems.size})") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            if (mediaItems.size <= 2) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    mediaItems.forEach { item ->
                        CompareItem(
                            item = item,
                            imageLoader = imageLoader,
                            onFavorite = onFavorite,
                            onDelete = onDelete,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(4.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val rows = mediaItems.chunked(2)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowItems.forEach { item ->
                                CompareItem(
                                    item = item,
                                    imageLoader = imageLoader,
                                    onFavorite = onFavorite,
                                    onDelete = onDelete,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(4.dp)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompareItem(
    item: DeviceMedia,
    imageLoader: ImageLoader,
    onFavorite: (DeviceMedia) -> Unit,
    onDelete: (DeviceMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = item.uri,
                imageLoader = imageLoader,
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { onFavorite(item) }) {
                Icon(
                    if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) Color.Red else Color.White
                )
            }
            IconButton(onClick = { onDelete(item) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
            }
        }
    }
}
