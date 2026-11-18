package com.yourgallery.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import com.yourgallery.app.DeviceMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveViewerPager(
    mediaList: List<DeviceMedia>,
    initialIndex: Int,
    imageLoader: ImageLoader,
    onClose: () -> Unit,
    onShare: (DeviceMedia) -> Unit,
    onFavorite: (DeviceMedia) -> Unit,
    onEdit: (DeviceMedia) -> Unit,
    onInfo: (DeviceMedia) -> Unit,
    onDelete: (DeviceMedia) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0))) {
        mediaList.size
    }
    val currentItem = mediaList.getOrNull(pagerState.currentPage)
    var showChrome by remember { mutableStateOf(true) }
    var isImageZoomedIn by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Native HorizontalPager for 100% fluid left/right swiping
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isImageZoomedIn,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = mediaList[page]
            Box(Modifier.fillMaxSize()) {
                if (item.isVideo) {
                    val isActivePage = pagerState.currentPage == page
                    VideoPlayerItem(
                        uri = item.uri,
                        isActivePage = isActivePage,
                        showChrome = showChrome,
                        onSingleTap = { showChrome = !showChrome }
                    )
                } else {
                    ZoomableImageItem(
                        item = item,
                        imageLoader = imageLoader,
                        onSingleTap = { showChrome = !showChrome },
                        onZoomChanged = { isZoomed -> isImageZoomedIn = isZoomed }
                    )
                }
            }
        }

        // Top Control Header
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
                currentItem?.let { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.displayName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} of ${mediaList.size}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.width(48.dp))
            }
        }

        // Bottom Control Bar
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.75f)).navigationBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    currentItem?.let { item ->
                        ViewerActionButton(
                            icon = Icons.Filled.Share,
                            label = "Share",
                            onClick = { onShare(item) }
                        )
                        ViewerActionButton(
                            icon = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            label = "Favorite",
                            tint = if (item.isFavorite) Color.Red else Color.White,
                            onClick = { onFavorite(item) }
                        )
                        ViewerActionButton(
                            icon = Icons.Filled.Edit,
                            label = "Edit",
                            onClick = { onEdit(item) }
                        )
                        ViewerActionButton(
                            icon = Icons.Filled.Info,
                            label = "Details",
                            onClick = { onInfo(item) }
                        )
                        ViewerActionButton(
                            icon = Icons.Filled.Delete,
                            label = "Trash",
                            tint = Color(0xFFFF5252),
                            onClick = { onDelete(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ZoomableImageItem(
    item: DeviceMedia, 
    imageLoader: ImageLoader, 
    onSingleTap: () -> Unit = {},
    onZoomChanged: (Boolean) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Notify parent pager whether page swiping should be disabled (when zoomed in)
    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(scale) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (scale > 1.05f) {
                            // When zoomed in: consume touches to allow panning and zooming inside the photo
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            
                            if (newScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                            scale = newScale
                            event.changes.forEach { it.consume() }
                        } else if (event.changes.size >= 2) {
                            // At 1x scale: 2 fingers touch = Pinch to Zoom in!
                            val zoom = event.calculateZoom()
                            if (zoom > 1.01f) {
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                event.changes.forEach { it.consume() }
                            }
                        }
                        // Single finger at 1x scale: DO NOT CONSUME! Allows HorizontalPager left/right swiping!
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = { onSingleTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = item.uri,
            imageLoader = imageLoader,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerItem(uri: android.net.Uri, isActivePage: Boolean, showChrome: Boolean, onSingleTap: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    LaunchedEffect(isActivePage) {
        if (isActivePage) {
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    // Add listener so tapping ExoPlayer's surface triggers our global chrome
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        val isVisible = visibility == android.view.View.VISIBLE
                        if (isVisible != showChrome) {
                            onSingleTap()
                        }
                    })
                }
            },
            update = { view ->
                // Do NOT force showController/hideController here. 
                // ExoPlayer handles its own tap-to-toggle internally.
                // Forcing it here based on showChrome (which is updated BY the listener above)
                // causes an infinite feedback loop (flickering).
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

suspend fun loadExifData(context: android.content.Context, uri: android.net.Uri): Map<String, String> {
    return withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                map["Camera"] = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "Unknown"
                map["Model"] = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "Unknown"
                map["Aperture"] = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" } ?: "N/A"
                map["Focal Length"] = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" } ?: "N/A"
                map["ISO"] = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: "N/A"
                map["Shutter"] = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { "${it}s" } ?: "N/A"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        map
    }
}
