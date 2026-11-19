package com.yourgallery.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.yourgallery.app.DeviceMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CollageRatio(val label: String, val ratio: Float) {
    SQUARE("1:1", 1f),
    PORTRAIT("4:5", 0.8f),
    STORY("9:16", 0.5625f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageScreen(
    items: List<DeviceMedia>,
    imageLoader: ImageLoader,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSaveCollage: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var selectedRatio by remember { mutableStateOf(CollageRatio.SQUARE) }
    var cornerRadiusDp by remember { mutableFloatStateOf(8f) }
    var spacingDp by remember { mutableFloatStateOf(6f) }
    var activeTemplate by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Collage Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${items.size} photos selected", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose, enabled = !isSaving) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Render high-res collage bitmap on IO
                            coroutineScope.launch {
                                val bitmap = withContext(Dispatchers.IO) {
                                    renderCollageBitmap(
                                        context = context,
                                        items = items,
                                        ratio = selectedRatio,
                                        cornerRadiusPx = cornerRadiusDp * 3f,
                                        spacingPx = spacingDp * 3f,
                                        templateIndex = activeTemplate
                                    )
                                }
                                if (bitmap != null) {
                                    onSaveCollage(bitmap)
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = "Save Collage", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Preview Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(selectedRatio.ratio)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF18181B))
                        .padding(spacingDp.dp)
                ) {
                    CollageLayoutView(
                        items = items,
                        imageLoader = imageLoader,
                        cornerRadiusDp = cornerRadiusDp,
                        spacingDp = spacingDp,
                        templateIndex = activeTemplate
                    )
                }
            }

            // Controls Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Ratio Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aspect Ratio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CollageRatio.entries.forEach { r ->
                            FilterChip(
                                selected = selectedRatio == r,
                                onClick = { selectedRatio = r },
                                label = { Text(r.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Layout Templates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Layout Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Classic", "Split", "Mosaic").take(if (items.size > 2) 3 else 2).forEachIndexed { idx, name ->
                            FilterChip(
                                selected = activeTemplate == idx,
                                onClick = { activeTemplate = idx },
                                label = { Text(name, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Corner Radius Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Roundness", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
                    Slider(
                        value = cornerRadiusDp,
                        onValueChange = { cornerRadiusDp = it },
                        valueRange = 0f..20f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Spacing Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Spacing", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
                    Slider(
                        value = spacingDp,
                        onValueChange = { spacingDp = it },
                        valueRange = 0f..14f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CollageLayoutView(
    items: List<DeviceMedia>,
    imageLoader: ImageLoader,
    cornerRadiusDp: Float,
    spacingDp: Float,
    templateIndex: Int
) {
    val count = items.size
    val shape = RoundedCornerShape(cornerRadiusDp.dp)

    when (count) {
        2 -> {
            if (templateIndex == 0) {
                // Left-Right Split
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacingDp.dp)) {
                    items.forEach { item ->
                        AsyncImage(
                            model = item.uri,
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(shape)
                        )
                    }
                }
            } else {
                // Top-Bottom Split
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacingDp.dp)) {
                    items.forEach { item ->
                        AsyncImage(
                            model = item.uri,
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(shape)
                        )
                    }
                }
            }
        }
        3 -> {
            if (templateIndex == 0) {
                // 1 Top, 2 Bottom
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacingDp.dp)) {
                    AsyncImage(
                        model = items[0].uri,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(shape)
                    )
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(spacingDp.dp)) {
                        AsyncImage(
                            model = items[1].uri,
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(shape)
                        )
                        AsyncImage(
                            model = items[2].uri,
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(shape)
                        )
                    }
                }
            } else {
                // 3 Vertical Strips
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacingDp.dp)) {
                    items.forEach { item ->
                        AsyncImage(
                            model = item.uri,
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(shape)
                        )
                    }
                }
            }
        }
        else -> {
            // 4, 5, or 6 items in 2x2 or 2x3 Grid
            val rows = items.chunked(if (count <= 4) 2 else 3)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacingDp.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacingDp.dp)
                    ) {
                        rowItems.forEach { item ->
                            AsyncImage(
                                model = item.uri,
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(shape)
                            )
                        }
                    }
                }
            }
        }
    }
}

// High-resolution bitmap renderer for saving collage to storage
fun renderCollageBitmap(
    context: android.content.Context,
    items: List<DeviceMedia>,
    ratio: CollageRatio,
    cornerRadiusPx: Float,
    spacingPx: Float,
    templateIndex: Int
): Bitmap? {
    return try {
        val baseWidth = 1800
        val baseHeight = (baseWidth / ratio.ratio).toInt()
        val result = Bitmap.createBitmap(baseWidth, baseHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(android.graphics.Color.BLACK)

        val count = items.size
        val rects = mutableListOf<RectF>()

        if (count == 2) {
            if (templateIndex == 0) {
                // Side-by-side
                val w = (baseWidth - spacingPx * 3) / 2f
                val h = baseHeight - spacingPx * 2
                rects.add(RectF(spacingPx, spacingPx, spacingPx + w, spacingPx + h))
                rects.add(RectF(spacingPx * 2 + w, spacingPx, baseWidth - spacingPx, spacingPx + h))
            } else {
                // Top-bottom
                val w = baseWidth - spacingPx * 2
                val h = (baseHeight - spacingPx * 3) / 2f
                rects.add(RectF(spacingPx, spacingPx, spacingPx + w, spacingPx + h))
                rects.add(RectF(spacingPx, spacingPx * 2 + h, spacingPx + w, baseHeight - spacingPx))
            }
        } else if (count == 3) {
            val topH = (baseHeight - spacingPx * 3) / 2f
            rects.add(RectF(spacingPx, spacingPx, baseWidth - spacingPx, spacingPx + topH))
            val botW = (baseWidth - spacingPx * 3) / 2f
            val botTop = spacingPx * 2 + topH
            rects.add(RectF(spacingPx, botTop, spacingPx + botW, baseHeight - spacingPx))
            rects.add(RectF(spacingPx * 2 + botW, botTop, baseWidth - spacingPx, baseHeight - spacingPx))
        } else {
            // 4+ items in 2 rows
            val cols = if (count <= 4) 2 else 3
            val rowCount = 2
            val cellW = (baseWidth - spacingPx * (cols + 1)) / cols
            val cellH = (baseHeight - spacingPx * (rowCount + 1)) / rowCount

            var idx = 0
            for (r in 0 until rowCount) {
                for (c in 0 until cols) {
                    if (idx < count) {
                        val left = spacingPx + c * (cellW + spacingPx)
                        val top = spacingPx + r * (cellH + spacingPx)
                        rects.add(RectF(left, top, left + cellW, top + cellH))
                        idx++
                    }
                }
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until minOf(items.size, rects.size)) {
            val item = items[i]
            val rect = rects[i]
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                val src = BitmapFactory.decodeStream(stream)
                if (src != null) {
                    val cropped = cropCenterBitmap(src, rect.width().toInt(), rect.height().toInt())
                    if (cropped != src) src.recycle()

                    canvas.save()
                    val path = android.graphics.Path()
                    path.addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, android.graphics.Path.Direction.CW)
                    canvas.clipPath(path)
                    canvas.drawBitmap(cropped, null, rect, paint)
                    canvas.restore()
                    cropped.recycle()
                }
            }
        }

        result
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun cropCenterBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
    val srcWidth = src.width
    val srcHeight = src.height
    val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
    val srcRatio = srcWidth.toFloat() / srcHeight.toFloat()

    var cropWidth = srcWidth
    var cropHeight = srcHeight
    if (srcRatio > targetRatio) {
        cropWidth = (srcHeight * targetRatio).toInt()
    } else {
        cropHeight = (srcWidth / targetRatio).toInt()
    }

    val x = (srcWidth - cropWidth) / 2
    val y = (srcHeight - cropHeight) / 2
    return Bitmap.createBitmap(src, x, y, cropWidth, cropHeight)
}
