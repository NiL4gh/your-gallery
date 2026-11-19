package com.yourgallery.app.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

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
    onDelete: (DeviceMedia) -> Unit,
    onRestore: ((DeviceMedia) -> Unit)? = null,
    onQuickCrop: ((item: DeviceMedia, scale: Float, offsetX: Float, offsetY: Float, containerSize: IntSize) -> Unit)? = null,
    onSetWallpaper: ((DeviceMedia) -> Unit)? = null
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0))) {
        mediaList.size
    }
    val currentItem = mediaList.getOrNull(pagerState.currentPage)
    var showChrome by remember { mutableStateOf(true) }
    var isImageZoomedIn by remember { mutableStateOf(false) }
    var isSlideshowActive by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isImageZoomedIn = false
    }

    // Slideshow Auto-advance with gentle 3.5s timing
    LaunchedEffect(isSlideshowActive, pagerState.currentPage) {
        if (isSlideshowActive) {
            delay(3500L)
            if (pagerState.currentPage < mediaList.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            } else {
                isSlideshowActive = false
            }
        }
    }

    BackHandler { 
        if (isSlideshowActive) {
            isSlideshowActive = false
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Native HorizontalPager for fluid 120Hz swiping
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isImageZoomedIn && !isSlideshowActive,
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
                        isSlideshow = isSlideshowActive,
                        onSingleTap = { 
                            if (isSlideshowActive) {
                                isSlideshowActive = false
                            } else {
                                showChrome = !showChrome 
                            }
                        },
                        onZoomChanged = { isZoomed -> isImageZoomedIn = isZoomed },
                        onDismissRequest = onClose,
                        onSwipeUpDetails = { currentItem?.let { onInfo(it) } },
                        onQuickCrop = onQuickCrop
                    )
                }
            }
        }

        // Top Control Header
        AnimatedVisibility(
            visible = showChrome && !isSlideshowActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} of ${mediaList.size}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Slideshow Action
                    IconButton(onClick = { isSlideshowActive = true }) {
                        Icon(Icons.Filled.PlayCircleOutline, contentDescription = "Slideshow", tint = Color.White)
                    }

                    // Wallpaper Action (For Photos)
                    if (currentItem != null && !currentItem.isVideo) {
                        IconButton(onClick = { 
                            if (onSetWallpaper != null) {
                                onSetWallpaper(currentItem)
                            } else {
                                setMediaAsWallpaper(context, currentItem.uri)
                            }
                        }) {
                            Icon(Icons.Filled.Wallpaper, contentDescription = "Set Wallpaper", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Floating Slideshow Active Indicator
        AnimatedVisibility(
            visible = isSlideshowActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.clickable { isSlideshowActive = false }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Playing Slideshow (Tap to pause)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Bottom Control Bar
        AnimatedVisibility(
            visible = showChrome && !isSlideshowActive,
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
                        if (item.trashedAt > 0L) {
                            ViewerActionButton(
                                icon = Icons.Filled.Restore,
                                label = "Restore",
                                onClick = { onRestore?.invoke(item) }
                            )
                            ViewerActionButton(
                                icon = Icons.Filled.Info,
                                label = "Details",
                                onClick = { onInfo(item) }
                            )
                            ViewerActionButton(
                                icon = Icons.Filled.DeleteForever,
                                label = "Delete",
                                tint = Color(0xFFFF5252),
                                onClick = { onDelete(item) }
                            )
                        } else {
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
                            if (!item.isVideo) {
                                ViewerActionButton(
                                    icon = Icons.Filled.Edit,
                                    label = "Edit",
                                    onClick = { onEdit(item) }
                                )
                            }
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
    isSlideshow: Boolean = false,
    onSingleTap: () -> Unit = {},
    onZoomChanged: (Boolean) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onSwipeUpDetails: () -> Unit = {},
    onQuickCrop: ((item: DeviceMedia, scale: Float, offsetX: Float, offsetY: Float, containerSize: IntSize) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val scaleAnim = remember(item.id) { Animatable(1f) }
    val offsetXAnim = remember(item.id) { Animatable(0f) }
    val offsetYAnim = remember(item.id) { Animatable(0f) }
    val dismissYAnim = remember(item.id) { Animatable(0f) }

    // Subtle Ken Burns slow pan/zoom during slideshow mode
    LaunchedEffect(isSlideshow) {
        if (isSlideshow) {
            scaleAnim.animateTo(1.08f, spring(stiffness = Spring.StiffnessVeryLow))
        } else {
            scaleAnim.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    val isZoomed = scaleAnim.value > 1.05f

    // Notify parent pager whether page swiping should be disabled (when zoomed in)
    LaunchedEffect(isZoomed) {
        onZoomChanged(isZoomed)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(containerSize) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pointerCount = event.changes.size
                        val currentScale = scaleAnim.value

                        if (currentScale > 1.05f) {
                            // Zoomed in: calculate zoom & pan with STRICT boundary clamping
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (currentScale * zoom).coerceIn(1f, 5f)

                            val maxOffsetX = max(0f, (containerSize.width * (newScale - 1f)) / 2f)
                            val maxOffsetY = max(0f, (containerSize.height * (newScale - 1f)) / 2f)

                            val targetX = if (newScale <= 1.02f) 0f else (offsetXAnim.value + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            val targetY = if (newScale <= 1.02f) 0f else (offsetYAnim.value + pan.y).coerceIn(-maxOffsetY, maxOffsetY)

                            coroutineScope.launch {
                                scaleAnim.snapTo(newScale)
                                offsetXAnim.snapTo(targetX)
                                offsetYAnim.snapTo(targetY)
                            }
                            event.changes.forEach { it.consume() }
                        } else if (pointerCount >= 2) {
                            // At 1x scale: 2 fingers touch = Pinch to Zoom in!
                            val zoom = event.calculateZoom()
                            if (zoom > 1.01f) {
                                val newScale = (currentScale * zoom).coerceIn(1f, 5f)
                                coroutineScope.launch {
                                    scaleAnim.snapTo(newScale)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }

                        // Check if all pointers released to smoothly snap back if needed
                        val allPointersUp = event.changes.all { !it.pressed }
                        if (allPointersUp) {
                            val activeScale = scaleAnim.value
                            if (activeScale < 1.05f) {
                                coroutineScope.launch {
                                    launch { scaleAnim.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                    launch { offsetXAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                    launch { offsetYAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                }
                            } else {
                                val maxOffsetX = max(0f, (containerSize.width * (activeScale - 1f)) / 2f)
                                val maxOffsetY = max(0f, (containerSize.height * (activeScale - 1f)) / 2f)
                                val clampedX = offsetXAnim.value.coerceIn(-maxOffsetX, maxOffsetX)
                                val clampedY = offsetYAnim.value.coerceIn(-maxOffsetY, maxOffsetY)
                                if (clampedX != offsetXAnim.value || clampedY != offsetYAnim.value) {
                                    coroutineScope.launch {
                                        launch { offsetXAnim.animateTo(clampedX, spring(stiffness = Spring.StiffnessMediumLow)) }
                                        launch { offsetYAnim.animateTo(clampedY, spring(stiffness = Spring.StiffnessMediumLow)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(containerSize) {
                detectVerticalDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        val currentDismissY = dismissYAnim.value
                        if (currentDismissY > 160f) {
                            onDismissRequest()
                        } else if (currentDismissY < -100f) {
                            onSwipeUpDetails()
                            coroutineScope.launch {
                                dismissYAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        } else {
                            coroutineScope.launch {
                                dismissYAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dismissYAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // Only allow vertical swipe-to-dismiss/details when NOT zoomed in
                        if (scaleAnim.value <= 1.05f) {
                            change.consume()
                            coroutineScope.launch {
                                dismissYAnim.snapTo(dismissYAnim.value + dragAmount)
                            }
                        }
                    }
                )
            }
            .pointerInput(containerSize) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            if (scaleAnim.value > 1.1f) {
                                launch { scaleAnim.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetXAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetYAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                            } else {
                                val targetScale = 2.5f
                                val maxOffsetX = max(0f, (containerSize.width * (targetScale - 1f)) / 2f)
                                val maxOffsetY = max(0f, (containerSize.height * (targetScale - 1f)) / 2f)
                                val centerX = containerSize.width / 2f
                                val centerY = containerSize.height / 2f
                                val targetX = ((centerX - tapOffset.x) * 1.5f).coerceIn(-maxOffsetX, maxOffsetX)
                                val targetY = ((centerY - tapOffset.y) * 1.5f).coerceIn(-maxOffsetY, maxOffsetY)
                                launch { scaleAnim.animateTo(targetScale, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetXAnim.animateTo(targetX, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetYAnim.animateTo(targetY, spring(stiffness = Spring.StiffnessMediumLow)) }
                            }
                        }
                    },
                    onTap = { onSingleTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val dismissY = dismissYAnim.value
        val dismissAlpha = if (dismissY > 0f) (1f - (dismissY / 600f)).coerceIn(0.4f, 1f) else 1f

        AsyncImage(
            model = item.uri,
            imageLoader = imageLoader,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scaleAnim.value,
                    scaleY = scaleAnim.value,
                    translationX = offsetXAnim.value,
                    translationY = offsetYAnim.value + dismissY,
                    alpha = dismissAlpha
                )
        )

        // Samsung Gallery Signature: Floating Quick Crop Button when zoomed in (> 1.25x)
        AnimatedVisibility(
            visible = scaleAnim.value > 1.25f,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 64.dp, start = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.72f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        onQuickCrop?.invoke(item, scaleAnim.value, offsetXAnim.value, offsetYAnim.value, containerSize)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCut,
                        contentDescription = "Quick Crop",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Crop",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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

    DisposableEffect(exoPlayer, lifecycleOwner) {
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

    // Samsung Video Player signature gesture controls: Brightness, Volume, Scrub Seek HUD
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    var hudType by remember { mutableStateOf<String?>(null) } // "brightness", "volume", "seek"
    var hudText by remember { mutableStateOf("") }
    var hudIcon by remember { mutableStateOf(Icons.AutoMirrored.Filled.VolumeUp) }
    var hudProgress by remember { mutableFloatStateOf(0f) }

    var containerWidth by remember { mutableIntStateOf(0) }
    var containerHeight by remember { mutableIntStateOf(0) }

    var isLeftHalf by remember { mutableStateOf(false) }
    var dragMode by remember { mutableStateOf<String?>(null) } // "vertical" or "horizontal"
    var dragDeltaSumX by remember { mutableFloatStateOf(0f) }
    var seekBasePosition by remember { mutableLongStateOf(0L) }
    var targetSeekPosition by remember { mutableLongStateOf(0L) }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                containerWidth = it.width
                containerHeight = it.height
            }
            .pointerInput(containerWidth, containerHeight) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val duration = exoPlayer.duration
                        if (duration > 0) {
                            if (offset.x < containerWidth / 2) {
                                val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                hudType = "seek"
                                hudIcon = Icons.Filled.Replay10
                                hudText = "-10s (${formatDuration(target)})"
                            } else {
                                val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(duration)
                                exoPlayer.seekTo(target)
                                hudType = "seek"
                                hudIcon = Icons.Filled.Forward10
                                hudText = "+10s (${formatDuration(target)})"
                            }
                            coroutineScope.launch {
                                delay(800)
                                hudType = null
                            }
                        }
                    },
                    onTap = { onSingleTap() }
                )
            }
            .pointerInput(containerWidth, containerHeight) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isLeftHalf = offset.x < containerWidth / 2
                        dragMode = null
                        dragDeltaSumX = 0f
                        seekBasePosition = exoPlayer.currentPosition
                        targetSeekPosition = seekBasePosition
                    },
                    onDragEnd = {
                        if (dragMode == "seek" && exoPlayer.duration > 0) {
                            exoPlayer.seekTo(targetSeekPosition)
                        }
                        dragMode = null
                        coroutineScope.launch {
                            delay(700)
                            hudType = null
                        }
                    },
                    onDragCancel = {
                        dragMode = null
                        coroutineScope.launch {
                            delay(500)
                            hudType = null
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragMode == null) {
                            if (abs(dragAmount.x) > abs(dragAmount.y) * 1.2f) {
                                dragMode = "seek"
                            } else if (abs(dragAmount.y) > abs(dragAmount.x) * 1.2f) {
                                dragMode = "vertical"
                            }
                        }

                        if (dragMode == "seek") {
                            val duration = exoPlayer.duration
                            if (duration > 0) {
                                dragDeltaSumX += dragAmount.x
                                val deltaMs = (dragDeltaSumX * 90).toLong()
                                targetSeekPosition = (seekBasePosition + deltaMs).coerceIn(0L, duration)
                                val diffSec = (targetSeekPosition - seekBasePosition) / 1000
                                val sign = if (diffSec >= 0) "+" else ""
                                hudType = "seek"
                                hudIcon = if (diffSec >= 0) Icons.Filled.FastForward else Icons.Filled.FastRewind
                                hudText = "$sign${diffSec}s (${formatDuration(targetSeekPosition)} / ${formatDuration(duration)})"
                                hudProgress = targetSeekPosition.toFloat() / duration.toFloat()
                            }
                        } else if (dragMode == "vertical") {
                            val dragRatio = -dragAmount.y / (containerHeight * 0.7f).coerceAtLeast(1f)
                            if (isLeftHalf) {
                                // Brightness adjustment
                                val activity = context as? Activity
                                val window = activity?.window
                                if (window != null) {
                                    val currentBrightness = window.attributes.screenBrightness.let { if (it < 0f) 0.5f else it }
                                    val newBrightness = (currentBrightness + dragRatio).coerceIn(0.02f, 1f)
                                    val lp = window.attributes
                                    lp.screenBrightness = newBrightness
                                    window.attributes = lp
                                    hudType = "brightness"
                                    hudIcon = Icons.Filled.BrightnessMedium
                                    hudProgress = newBrightness
                                    hudText = "${(newBrightness * 100).toInt()}%"
                                }
                            } else {
                                // Volume adjustment
                                audioManager?.let { am ->
                                    val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val volDelta = (dragRatio * maxVolume).toInt()
                                    if (volDelta != 0) {
                                        val newVol = (currentVol + volDelta).coerceIn(0, maxVolume)
                                        am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        hudType = "volume"
                                        hudIcon = if (newVol == 0) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp
                                        hudProgress = newVol.toFloat() / maxVolume.toFloat()
                                        hudText = "${((newVol.toFloat() / maxVolume) * 100).toInt()}%"
                                    }
                                }
                            }
                        }
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        val isVisible = visibility == android.view.View.VISIBLE
                        if (isVisible != showChrome) {
                            onSingleTap()
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Glassmorphic Gesture HUD
        AnimatedVisibility(
            visible = hudType != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.78f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Icon(hudIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(hudText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (hudType == "brightness" || hudType == "volume") {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { hudProgress },
                            modifier = Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                    }
                }
            }
        }
    }
}

fun setMediaAsWallpaper(context: Context, uri: Uri) {
    try {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val intent = wallpaperManager.getCropAndSetWallpaperIntent(uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("mimeType", "image/*")
            }
            context.startActivity(Intent.createChooser(intent, "Set as Wallpaper"))
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot open wallpaper chooser", Toast.LENGTH_SHORT).show()
        }
    }
}

suspend fun performQuickCrop(
    context: Context,
    item: DeviceMedia,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    containerSize: IntSize
): Uri? = withContext(Dispatchers.IO) {
    try {
        val stream = context.contentResolver.openInputStream(item.uri) ?: return@withContext null
        val original = BitmapFactory.decodeStream(stream) ?: return@withContext null
        stream.close()

        val imgW = original.width.toFloat()
        val imgH = original.height.toFloat()
        val contW = containerSize.width.toFloat()
        val contH = containerSize.height.toFloat()
        if (contW <= 0f || contH <= 0f || imgW <= 0f || imgH <= 0f) return@withContext null

        val fitScale = minOf(contW / imgW, contH / imgH)
        val fittedW = imgW * fitScale
        val fittedH = imgH * fitScale
        val left0 = (contW - fittedW) / 2f
        val top0 = (contH - fittedH) / 2f

        val xMin = contW / 2f - (contW / 2f + offsetX) / scale
        val xMax = contW / 2f + (contW / 2f - offsetX) / scale
        val yMin = contH / 2f - (contH / 2f + offsetY) / scale
        val yMax = contH / 2f + (contH / 2f - offsetY) / scale

        val normX1 = ((xMin - left0) / fittedW).coerceIn(0f, 1f)
        val normX2 = ((xMax - left0) / fittedW).coerceIn(0f, 1f)
        val normY1 = ((yMin - top0) / fittedH).coerceIn(0f, 1f)
        val normY2 = ((yMax - top0) / fittedH).coerceIn(0f, 1f)

        val cropX = (normX1 * imgW).toInt().coerceIn(0, original.width - 1)
        val cropY = (normY1 * imgH).toInt().coerceIn(0, original.height - 1)
        val cropW = ((normX2 - normX1) * imgW).toInt().coerceIn(1, original.width - cropX)
        val cropH = ((normY2 - normY1) * imgH).toInt().coerceIn(1, original.height - cropY)

        val cropped = Bitmap.createBitmap(original, cropX, cropY, cropW, cropH)
        if (cropped != original) original.recycle()

        val filename = "QuickCrop_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/YourGallery")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val outUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (outUri != null) {
            context.contentResolver.openOutputStream(outUri)?.use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(outUri, values, null, null)
            }
        }
        cropped.recycle()
        outUri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun loadExifData(context: Context, uri: Uri): Map<String, String> {
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
