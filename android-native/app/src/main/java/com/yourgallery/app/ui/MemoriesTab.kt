package com.yourgallery.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.yourgallery.app.DeviceMedia
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoriesView(mediaList: List<DeviceMedia>, imageLoader: ImageLoader, onItemClick: (DeviceMedia) -> Unit) {
    val nonTrashed = remember(mediaList) { mediaList.filter { it.trashedAt == 0L } }
    val highlights = remember(nonTrashed) { nonTrashed.take(10) }
    val videos = remember(nonTrashed) { nonTrashed.filter { it.isVideo } }
    val favorites = remember(nonTrashed) { nonTrashed.filter { it.isFavorite } }
    val monthGroups = remember(nonTrashed) {
        nonTrashed.groupBy { item ->
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(item.dateTakenMs))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. On This Day / Throwback Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        highlights.firstOrNull()?.let { onItemClick(it) }
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(Modifier.fillMaxSize()) {
                    highlights.firstOrNull()?.let { cover ->
                        AsyncImage(
                            model = cover.uri,
                            imageLoader = imageLoader,
                            contentDescription = "On This Day",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("ON THIS DAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                        }
                        Text("Flashback Memories", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${nonTrashed.size} moments preserved", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        }

        // 2. Recent Highlights Stories Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Highlights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text("Auto-Curated", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (highlights.isEmpty()) {
                    Text("No recent memories yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(highlights, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .size(width = 130.dp, height = 180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onItemClick(item) }
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    imageLoader = imageLoader,
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                            )
                                        )
                                )
                                if (item.isFavorite) {
                                    Icon(
                                        Icons.Filled.Favorite,
                                        contentDescription = "Favorite",
                                        tint = Color.Red,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Featured Video Reel
        if (videos.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Featured Video Memories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(videos.take(10), key = { it.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onItemClick(item) }
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    imageLoader = imageLoader,
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f))
                                )
                                Icon(
                                    Icons.Filled.PlayCircle,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Monthly Time Capsules
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Monthly Time Capsules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                monthGroups.forEach { (monthLabel, items) ->
                    val cover = items.firstOrNull() ?: return@forEach
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onItemClick(cover) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = cover.uri,
                            imageLoader = imageLoader,
                            contentDescription = monthLabel,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = monthLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${items.size} moments captured",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
