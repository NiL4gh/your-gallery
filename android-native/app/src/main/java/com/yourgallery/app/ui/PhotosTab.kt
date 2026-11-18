package com.yourgallery.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.yourgallery.app.DeviceMedia
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    mediaList: List<DeviceMedia>,
    imageLoader: ImageLoader,
    selectedMediaIds: Set<Long>,
    gridColumns: Int,
    gridState: LazyGridState,
    onGridColumnsChange: (Int) -> Unit = {},
    onItemClick: (DeviceMedia, Int) -> Unit,
    onItemLongClick: (DeviceMedia) -> Unit,
    onDragSelect: (DeviceMedia) -> Unit = {},
    onSelectGroup: (List<DeviceMedia>) -> Unit = {},
    onDeselectGroup: (List<DeviceMedia>) -> Unit = {}
) {
    var timelineZoom by remember { mutableStateOf("All") } // Years, Months, Days, All
    val isSelectionMode = selectedMediaIds.isNotEmpty()

    // Grouping logic based on timelineZoom
    val groupedMedia = remember(mediaList, timelineZoom) {
        mediaList.groupBy { item ->
            when (timelineZoom) {
                "Years" -> SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(item.dateTakenMs))
                "Months" -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(item.dateTakenMs))
                else -> { // Days and All
                    val date = Date(item.dateTakenMs)
                    val today = Date()
                    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    if (fmt.format(date) == fmt.format(today)) "Today"
                    else if (fmt.format(date) == fmt.format(Date(today.time - 86400000))) "Yesterday"
                    else SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(date)
                }
            }
        }
    }

    var lastZoomTime by remember { mutableLongStateOf(0L) }
    var gridScale by remember { mutableFloatStateOf(1f) }
    val animatedGridScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = gridScale, 
        animationSpec = androidx.compose.animation.core.spring(stiffness = 300f, dampingRatio = 0.8f),
        label = "gridScale"
    )

    Column(Modifier.fillMaxSize()) {
        // Clean Minimal Header: Timeline Capsule + Item Count
        PhotosToolbar(
            timelineZoom = timelineZoom,
            onTimelineZoomChange = { timelineZoom = it },
            itemCount = mediaList.size
        )

        if (mediaList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No photos or videos found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(gridColumns) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.size >= 2) {
                                    val zoom = event.calculateZoom()
                                    gridScale = (gridScale * zoom).coerceIn(0.7f, 1.4f)
                                    event.changes.forEach { it.consume() }

                                    val now = System.currentTimeMillis()
                                    if (now - lastZoomTime > 200L) {
                                        if (gridScale > 1.25f && gridColumns > 1) {
                                            onGridColumnsChange(gridColumns - 1)
                                            lastZoomTime = now
                                            gridScale = 1f
                                        } else if (gridScale < 0.8f && gridColumns < 8) {
                                            onGridColumnsChange(gridColumns + 1)
                                            lastZoomTime = now
                                            gridScale = 1f
                                        }
                                    }
                                } else {
                                    // Smoothly reset scale when gesture ends
                                    gridScale = 1f
                                }
                            }
                        }
                    }
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val maxHeightPx = constraints.maxHeight.toFloat()
                    val coroutineScope = rememberCoroutineScope()
                    val haptic = LocalHapticFeedback.current
                    var isDragSelecting by remember { mutableStateOf(false) }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = animatedGridScale,
                                scaleY = animatedGridScale
                            )
                            .pointerInput(isSelectionMode, mediaList) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        isDragSelecting = true
                                        val hit = findItemAtOffset(offset, gridState, mediaList)
                                        hit?.let {
                                            onItemLongClick(it)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val hit = findItemAtOffset(change.position, gridState, mediaList)
                                        hit?.let {
                                            if (!selectedMediaIds.contains(it.id)) {
                                                onDragSelect(it)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                        val y = change.position.y
                                        if (y < 120f) {
                                            coroutineScope.launch { gridState.scrollBy(-35f) }
                                        } else if (y > maxHeightPx - 120f) {
                                            coroutineScope.launch { gridState.scrollBy(35f) }
                                        }
                                    },
                                    onDragEnd = { isDragSelecting = false },
                                    onDragCancel = { isDragSelecting = false }
                                )
                            }
                    ) {
                        groupedMedia.forEach { (date, itemsForDate) ->
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                val isAllGroupSelected = itemsForDate.all { selectedMediaIds.contains(it.id) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = date,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = if (isAllGroupSelected) "Deselect All" else "Select All",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable {
                                                if (isAllGroupSelected) {
                                                    onDeselectGroup(itemsForDate)
                                                } else {
                                                    onSelectGroup(itemsForDate)
                                                }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 8.dp)
                                    )
                                }
                            }
                            itemsIndexed(itemsForDate, key = { _, item -> item.id }) { _, item ->
                                val globalIndex = mediaList.indexOf(item)
                                val isSelected = selectedMediaIds.contains(item.id)
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(if (isSelected) 14.dp else 2.dp))
                                        .combinedClickable(
                                            onLongClick = { onItemLongClick(item) },
                                            onClick = {
                                                if (isSelectionMode) {
                                                    onItemLongClick(item)
                                                } else {
                                                    onItemClick(item, globalIndex)
                                                }
                                            }
                                        )
                                ) {
                                    AsyncImage(
                                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                            .data(item.uri)
                                            .crossfade(true)
                                            .build(),
                                        imageLoader = imageLoader,
                                        contentDescription = item.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = if (isSelected) 0.88f else 1f,
                                                scaleY = if (isSelected) 0.88f else 1f
                                            )
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(22.dp)
                                        )
                                    }
                                    if (item.isFavorite && !isSelected) {
                                        Icon(
                                            Icons.Filled.Favorite,
                                            contentDescription = "Favorite",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(16.dp)
                                        )
                                    }
                                    if (item.isVideo) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = "Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                text = formatDuration(item.durationMs),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Single Smooth Draggable Date Handle (Zero Duplicate Bars)
                var isDraggingScrollbar by remember { mutableStateOf(false) }

                // Sub-pixel smooth scroll progress calculation (no integer jumping)
                val scrollData by remember {
                    derivedStateOf {
                        val totalItems = gridState.layoutInfo.totalItemsCount.coerceAtLeast(1)
                        val firstIndex = gridState.firstVisibleItemIndex
                        val firstOffset = gridState.firstVisibleItemScrollOffset.toFloat()
                        val visibleItems = gridState.layoutInfo.visibleItemsInfo
                        val avgItemHeight = if (visibleItems.isNotEmpty()) {
                            visibleItems.sumOf { it.size.height }.toFloat() / visibleItems.size
                        } else 150f
                        
                        val exactItemPos = firstIndex.toFloat() + (firstOffset / avgItemHeight.coerceAtLeast(1f))
                        val progress = (exactItemPos / totalItems.toFloat()).coerceIn(0f, 1f)
                        
                        val firstMediaKey = visibleItems.firstOrNull { it.key is Long }?.key as? Long
                        val media = if (firstMediaKey != null) {
                            mediaList.find { it.id == firstMediaKey }
                        } else {
                            val estimatedIndex = (progress * (mediaList.size - 1)).toInt().coerceIn(0, (mediaList.size - 1).coerceAtLeast(0))
                            mediaList.getOrNull(estimatedIndex)
                        }
                        Triple(totalItems, progress, media)
                    }
                }

                if (mediaList.isNotEmpty()) {
                    val (totalGridItems, scrollProgress, currentVisibleMedia) = scrollData
                    val capsuleHeightDp = 32.dp
                    val capsuleHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { capsuleHeightDp.toPx() }
                    val capsuleYPx = (scrollProgress * (maxHeightPx - capsuleHeightPx)).coerceIn(0f, maxHeightPx - capsuleHeightPx)
                    
                    val isScrollActive = gridState.isScrollInProgress || isDraggingScrollbar
                    val alpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isScrollActive) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
                        label = "scrollbarAlpha"
                    )

                    if (alpha > 0f) {
                        currentVisibleMedia?.let { media ->
                            val dateText = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(media.dateTakenMs))
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 6.dp)
                                    .offset(y = with(androidx.compose.ui.platform.LocalDensity.current) { capsuleYPx.toDp() })
                                    .graphicsLayer { this.alpha = alpha }
                                    .clip(CircleShape)
                                    .background(if (isDraggingScrollbar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (isDraggingScrollbar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .pointerInput(totalGridItems, maxHeightPx) {
                                        detectDragGestures(
                                            onDragStart = { isDraggingScrollbar = true },
                                            onDragEnd = { isDraggingScrollbar = false },
                                            onDragCancel = { isDraggingScrollbar = false },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val deltaFraction = dragAmount.y / (maxHeightPx - capsuleHeightPx).coerceAtLeast(1f)
                                                val newProgress = (scrollProgress + deltaFraction).coerceIn(0f, 1f)
                                                val targetGridItem = (newProgress * (totalGridItems - 1)).toInt().coerceIn(0, totalGridItems - 1)
                                                coroutineScope.launch {
                                                    gridState.scrollToItem(targetGridItem)
                                                }
                                            }
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = dateText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDraggingScrollbar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun PhotosToolbar(
    timelineZoom: String,
    onTimelineZoomChange: (String) -> Unit,
    itemCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Timeline Segmented Capsule
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Years", "Months", "Days", "All").forEach { label ->
                val isSelected = timelineZoom == label
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onTimelineZoomChange(label) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "$itemCount items",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

fun findItemAtOffset(
    offset: Offset,
    gridState: LazyGridState,
    mediaList: List<DeviceMedia>
): DeviceMedia? {
    val items = gridState.layoutInfo.visibleItemsInfo
    val hit = items.firstOrNull { info ->
        val x = offset.x
        val y = offset.y
        x >= info.offset.x && x <= (info.offset.x + info.size.width) &&
        y >= info.offset.y && y <= (info.offset.y + info.size.height)
    } ?: return null

    val key = hit.key
    return if (key is Long) {
        mediaList.find { it.id == key }
    } else null
}
