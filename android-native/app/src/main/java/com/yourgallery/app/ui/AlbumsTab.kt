package com.yourgallery.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.yourgallery.app.Album

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumGrid(
    albums: List<Album>,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    onUtilityClick: (String) -> Unit,
    onCreateAlbumClick: () -> Unit
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("My Albums", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onCreateAlbumClick) {
                    Icon(Icons.Filled.Add, contentDescription = "New Album")
                }
            }
        }
        
        if (albums.isEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No albums found", color = Color.Gray)
                }
            }
        } else {
            items(albums.size, key = { "album_${albums[it].name}" }) { idx ->
                val album = albums[idx]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onAlbumClick(album) },
                            onLongClick = { onAlbumLongClick(album) }
                        )
                ) {
                    AsyncImage(
                        model = album.coverMedia.uri,
                        imageLoader = imageLoader,
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(album.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${album.mediaCount} items", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        // Utilities Section
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                Text("Utilities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                UtilityRow(Icons.Filled.Favorite, "Favorites", onClick = { onUtilityClick("favorites") })
                HorizontalDivider(Modifier.padding(start = 48.dp))
                UtilityRow(Icons.Filled.VideoLibrary, "Videos", onClick = { onUtilityClick("videos") })
                HorizontalDivider(Modifier.padding(start = 48.dp))
                UtilityRow(Icons.Filled.Edit, "Edited", onClick = { onUtilityClick("edited") })
                HorizontalDivider(Modifier.padding(start = 48.dp))
                UtilityRow(Icons.Filled.Lock, "Locked Vault", onClick = { onUtilityClick("vault") })
                HorizontalDivider(Modifier.padding(start = 48.dp))
                UtilityRow(Icons.Filled.VisibilityOff, "Hidden", onClick = { onUtilityClick("hidden") })
                HorizontalDivider(Modifier.padding(start = 48.dp))
                UtilityRow(Icons.Filled.Delete, "Recently Deleted", onClick = { onUtilityClick("trash") })
                HorizontalDivider(Modifier.padding(start = 48.dp))
                UtilityRow(Icons.Filled.AutoAwesome, "Themes & Smart Tools", onClick = { onUtilityClick("themes") })
            }
        }
    }
}

@Composable
fun UtilityRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray)
    }
}
