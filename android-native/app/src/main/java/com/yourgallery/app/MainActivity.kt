package com.yourgallery.app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.yourgallery.app.data.GalleryDatabase
import com.yourgallery.app.data.MediaState
import com.yourgallery.app.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YourGalleryApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourGalleryApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("your_gallery_prefs", Context.MODE_PRIVATE) }
    var galleryTitle by remember { mutableStateOf(prefs.getString("gallery_title", "Your Gallery") ?: "Your Gallery") }
    var darkMode by remember { mutableStateOf(true) }

    // Enforce proper status bar contrast for light/dark mode
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as Activity).window
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkMode
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkMode
        }
    }

    var mediaList by remember { mutableStateOf<List<DeviceMedia>>(emptyList()) }
    var activeTab by remember { mutableIntStateOf(0) }
    var showThemesSheet by remember { mutableStateOf(false) }

    // Search state
    var isSearching by remember { mutableStateOf(false) }

    // Settings state
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
    var gridColumns by remember { mutableIntStateOf(3) }

    // Multi-select state
    var selectedMediaIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedMediaIds.isNotEmpty()

    // Album detail navigation state
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }

    // Viewer state: active list and starting index
    var viewerState by remember { mutableStateOf<Pair<List<DeviceMedia>, Int>?>(null) }
    var infoTarget by remember { mutableStateOf<DeviceMedia?>(null) }
    
    // Deletion states
    var deleteConfirmTargets by remember { mutableStateOf<List<DeviceMedia>?>(null) }
    var deleteSnoozeUntil by remember { mutableLongStateOf(0L) }

    // Compare state
    var compareTargets by remember { mutableStateOf<List<DeviceMedia>?>(null) }
    
    // Editor state
    var editorTarget by remember { mutableStateOf<DeviceMedia?>(null) }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Custom Dialog States
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var showVaultConfirmDialog by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var permanentDeleteTargets by remember { mutableStateOf<List<DeviceMedia>?>(null) }
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var showVaultPinModal by remember { mutableStateOf(false) }
    var isEditorSaving by remember { mutableStateOf(false) }
    var customAlbumNames by remember { mutableStateOf(prefs.getStringSet("custom_albums", emptySet()) ?: emptySet()) }
    var albumToManage by remember { mutableStateOf<Album?>(null) }
    var showRenameAlbumDialog by remember { mutableStateOf(false) }
    var showDeleteAlbumDialog by remember { mutableStateOf(false) }

    // Hoisted Grid States so scroll position is NEVER lost across navigation/viewer/feature pages!
    val photosGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val albumListState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val albumDetailState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    var permissionGranted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result.values.any { it }
    }

    // Full Storage Access Request Launcher (Android 11+)
    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissionGranted = android.os.Environment.isExternalStorageManager()
        }
    }

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            mediaList = MediaRepository.loadAllDeviceMedia(context)
            viewerState = null
            deleteConfirmTargets = null
            selectedMediaIds = emptySet()
        }
    }

    // Configure Coil for Video Thumbnails
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                manageStorageLauncher.launch(intent)
            } else {
                permissionGranted = true
            }
        } else {
            permLauncher.launch(permissions)
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            mediaList = withContext(Dispatchers.IO) {
                MediaRepository.loadAllDeviceMedia(context)
            }
        }
    }

    // Live ContentObserver to detect new photos, downloads, screenshots, or external deletions
    DisposableEffect(context, permissionGranted) {
        if (!permissionGranted) {
            onDispose {}
        } else {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    coroutineScope.launch(Dispatchers.IO) {
                        val refreshed = MediaRepository.loadAllDeviceMedia(context)
                        withContext(Dispatchers.Main) {
                            mediaList = refreshed
                        }
                    }
                }
            }
            try {
                context.contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
                context.contentResolver.registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onDispose {
                try {
                    context.contentResolver.unregisterContentObserver(observer)
                } catch (_: Exception) {}
            }
        }
    }

    fun performPermanentDelete(items: List<DeviceMedia>) {
        val idsToDelete = items.map { it.id }.toSet()
        // INSTANT UI UPDATE
        mediaList = mediaList.filterNot { it.id in idsToDelete }
        viewerState?.let { (list, currentIndex) ->
            val newList = list.filterNot { it.id in idsToDelete }
            if (newList.isEmpty()) {
                viewerState = null
            } else {
                val nextIndex = currentIndex.coerceAtMost(newList.size - 1)
                viewerState = newList to nextIndex
            }
        }
        permanentDeleteTargets = null
        deleteConfirmTargets = null
        selectedMediaIds = emptySet()

        // BACKGROUND STORAGE & DB DELETION
        coroutineScope.launch(Dispatchers.IO) {
            val db = GalleryDatabase.getDatabase(context)
            db.mediaStateDao().deleteStates(items.map { it.id })
            items.forEach { item ->
                try {
                    context.contentResolver.delete(item.uri, null, null)
                } catch (_: Exception) {}
                if (item.path.isNotBlank()) {
                    try {
                        val f = File(item.path)
                        if (f.exists()) f.delete()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun performTrash(items: List<DeviceMedia>) {
        val trashedIds = items.map { it.id }.toSet()
        // INSTANT UI UPDATE
        mediaList = mediaList.map { if (it.id in trashedIds) it.copy(trashedAt = System.currentTimeMillis()) else it }
        viewerState?.let { (list, currentIndex) ->
            val newList = list.filterNot { it.id in trashedIds }
            if (newList.isEmpty()) {
                viewerState = null
            } else {
                val nextIndex = currentIndex.coerceAtMost(newList.size - 1)
                viewerState = newList to nextIndex
            }
        }
        deleteConfirmTargets = null
        selectedMediaIds = emptySet()

        // BACKGROUND DB UPDATE
        coroutineScope.launch(Dispatchers.IO) {
            val db = GalleryDatabase.getDatabase(context)
            val dao = db.mediaStateDao()
            val now = System.currentTimeMillis()
            items.forEach { item ->
                val state = dao.getState(item.id) ?: MediaState(item.id)
                dao.insertOrUpdate(state.copy(trashedAt = now))
            }
        }
    }

    fun shareMedia(items: List<DeviceMedia>) {
        val uris = items.map { it.uri }
        val shareIntent = Intent().apply {
            action = if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
            type = "*/*"
            if (uris.size > 1) {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            } else {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
    }

    // Top-Level BackHandler to smoothly dismiss selection mode, search, overlays, viewer, editor, etc.
    BackHandler(
        enabled = isSearching || isSelectionMode || compareTargets != null || editorTarget != null || viewerState != null || selectedAlbum != null || infoTarget != null || showThemesSheet || showVaultPinModal || permanentDeleteTargets != null
    ) {
        when {
            showVaultPinModal -> showVaultPinModal = false
            permanentDeleteTargets != null -> permanentDeleteTargets = null
            isSearching -> { isSearching = false; searchQuery = "" }
            showThemesSheet -> showThemesSheet = false
            infoTarget != null -> infoTarget = null
            editorTarget != null -> editorTarget = null
            compareTargets != null -> compareTargets = null
            viewerState != null -> viewerState = null
            isSelectionMode -> selectedMediaIds = emptySet()
            selectedAlbum != null -> selectedAlbum = null
        }
    }

    GalleryTheme(darkMode = darkMode) {
        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    val selectedItems = remember(mediaList, selectedMediaIds) {
                        mediaList.filter { selectedMediaIds.contains(it.id) }
                    }
                    val selectedTotalBytes = remember(selectedItems) {
                        selectedItems.sumOf { it.sizeBytes }
                    }
                    TopAppBar(
                        title = {
                            Column {
                                Text("${selectedMediaIds.size} Selected", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (selectedTotalBytes > 0) {
                                    Text(
                                        Formatter.formatFileSize(context, selectedTotalBytes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedMediaIds = emptySet() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Clear Selection")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                if (selectedMediaIds.size >= mediaList.size) {
                                    selectedMediaIds = emptySet()
                                } else {
                                    selectedMediaIds = mediaList.map { it.id }.toSet()
                                }
                            }) {
                                Text(
                                    text = if (selectedMediaIds.size >= mediaList.size) "Deselect All" else "Select All",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                } else if (isSearching) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search photos...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search")
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                                }
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (activeTab == 1 && selectedAlbum != null) selectedAlbum!!.name else galleryTitle,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            if (activeTab == 1 && selectedAlbum != null) {
                                IconButton(onClick = { selectedAlbum = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Albums")
                                }
                            }
                        },
                        actions = {
                            if (activeTab == 1 && selectedAlbum?.name == "Recently Deleted") {
                                TextButton(onClick = { showEmptyTrashDialog = true }) {
                                    Text("Empty Trash", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                IconButton(onClick = { isSearching = true }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search Photos")
                                }
                                IconButton(onClick = { showThemesSheet = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "Themes & Options")
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isSelectionMode) {
                    BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                        val selectedItems = remember(mediaList, selectedMediaIds) {
                            mediaList.filter { selectedMediaIds.contains(it.id) }
                        }
                        val isInTrash = selectedAlbum?.name == "Recently Deleted"
                        val isInVault = selectedAlbum?.name == "Locked Vault"

                        if (isInTrash) {
                            SelectionActionButton(
                                icon = Icons.Filled.Restore,
                                label = "Restore",
                                onClick = {
                                    val selectedSet = selectedMediaIds.toSet()
                                    mediaList = mediaList.map { if (it.id in selectedSet) it.copy(trashedAt = 0L) else it }
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val db = GalleryDatabase.getDatabase(context)
                                        val dao = db.mediaStateDao()
                                        selectedItems.forEach { item ->
                                            val state = dao.getState(item.id) ?: MediaState(item.id)
                                            dao.insertOrUpdate(state.copy(trashedAt = 0L))
                                        }
                                    }
                                    selectedMediaIds = emptySet()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SelectionActionButton(
                                icon = Icons.Filled.DeleteForever,
                                label = "Delete",
                                tint = Color(0xFFFF5252),
                                onClick = {
                                    permanentDeleteTargets = selectedItems
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else if (isInVault) {
                            SelectionActionButton(
                                icon = Icons.Filled.LockOpen,
                                label = "Unhide",
                                onClick = {
                                    val selectedSet = selectedMediaIds.toSet()
                                    mediaList = mediaList.map { if (it.id in selectedSet) it.copy(isHidden = false) else it }
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val db = GalleryDatabase.getDatabase(context)
                                        val dao = db.mediaStateDao()
                                        selectedItems.forEach { item ->
                                            val state = dao.getState(item.id) ?: MediaState(item.id)
                                            dao.insertOrUpdate(state.copy(isHidden = false))
                                        }
                                    }
                                    selectedMediaIds = emptySet()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SelectionActionButton(
                                icon = Icons.Filled.Delete,
                                label = "Trash",
                                tint = Color(0xFFFF5252),
                                onClick = {
                                    performTrash(selectedItems)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            SelectionActionButton(
                                icon = Icons.Filled.FavoriteBorder,
                                label = "Favorite",
                                onClick = { 
                                    val selectedSet = selectedMediaIds.toSet()
                                    val anyNotFav = selectedItems.any { !it.isFavorite }
                                    mediaList = mediaList.map { 
                                        if (it.id in selectedSet) it.copy(isFavorite = anyNotFav) else it 
                                    }
                                    // Retain selection so user does not lose their place!
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val db = GalleryDatabase.getDatabase(context)
                                        val dao = db.mediaStateDao()
                                        selectedItems.forEach { item ->
                                            val state = dao.getState(item.id) ?: MediaState(item.id)
                                            dao.insertOrUpdate(state.copy(isFavorite = anyNotFav))
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            SelectionActionButton(
                                icon = Icons.Filled.LibraryAdd,
                                label = "Album",
                                onClick = { showAddToAlbumDialog = true },
                                modifier = Modifier.weight(1f)
                            )

                            SelectionActionButton(
                                icon = Icons.Filled.Share,
                                label = "Share",
                                onClick = { shareMedia(selectedItems) },
                                modifier = Modifier.weight(1f)
                            )

                            SelectionActionButton(
                                icon = Icons.Filled.Lock,
                                label = "Vault",
                                onClick = { showVaultConfirmDialog = true },
                                modifier = Modifier.weight(1f)
                            )

                            if (selectedItems.size in 2..4) {
                                SelectionActionButton(
                                    icon = Icons.Filled.Compare,
                                    label = "Compare",
                                    onClick = { compareTargets = selectedItems },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            SelectionActionButton(
                                icon = Icons.Filled.Delete,
                                label = "Trash",
                                tint = Color(0xFFFF5252),
                                onClick = { 
                                    if (System.currentTimeMillis() < deleteSnoozeUntil) {
                                        performTrash(selectedItems)
                                    } else {
                                        deleteConfirmTargets = selectedItems
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    NavigationBar {
                        listOf(
                            "Photos" to Icons.Filled.PhotoLibrary,
                            "Albums" to Icons.Filled.PhotoAlbum,
                            "Memories" to Icons.Filled.AutoAwesome,
                            "Search" to Icons.Filled.Search
                        ).forEachIndexed { idx, (label, icon) ->
                            NavigationBarItem(
                                selected = activeTab == idx,
                                onClick = {
                                    activeTab = idx
                                    if (idx != 1) selectedAlbum = null
                                    selectedMediaIds = emptySet()
                                },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                val displayedMedia = remember(mediaList, sortOrder, searchQuery) {
                    val nonTrashedAndVisible = mediaList.filter { it.trashedAt == 0L && !it.isHidden }
                    val filtered = if (searchQuery.isBlank()) nonTrashedAndVisible else nonTrashedAndVisible.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
                    when (sortOrder) {
                        SortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateTakenMs }
                        SortOrder.DATE_ASC -> filtered.sortedBy { it.dateTakenMs }
                        SortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
                        SortOrder.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase() }
                    }
                }

                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "TabAnimation"
                ) { currentTab ->
                    when (currentTab) {
                        0 -> { // Photos Grid
                            MediaGrid(
                                mediaList = displayedMedia,
                                imageLoader = imageLoader,
                                selectedMediaIds = selectedMediaIds,
                                gridColumns = gridColumns,
                                gridState = photosGridState,
                                onGridColumnsChange = { gridColumns = it },
                                onItemClick = { item, index ->
                                    if (isSelectionMode) {
                                        selectedMediaIds = if (selectedMediaIds.contains(item.id)) selectedMediaIds - item.id else selectedMediaIds + item.id
                                    } else {
                                        viewerState = displayedMedia to index
                                    }
                                },
                                onItemLongClick = { item ->
                                    selectedMediaIds = if (selectedMediaIds.contains(item.id)) selectedMediaIds - item.id else selectedMediaIds + item.id
                                },
                                onDragSelect = { item ->
                                    if (isSelectionMode) {
                                        selectedMediaIds = selectedMediaIds + item.id
                                    }
                                },
                                onSelectGroup = { group ->
                                    selectedMediaIds = selectedMediaIds + group.map { it.id }
                                },
                                onDeselectGroup = { group ->
                                    selectedMediaIds = selectedMediaIds - group.map { it.id }.toSet()
                            }
                        )
                    }
                    1 -> { // Albums View
                        val albums = remember(displayedMedia, customAlbumNames) { MediaRepository.groupIntoAlbums(displayedMedia, customAlbumNames) }
                        if (selectedAlbum != null) {
                            // Find the updated album reference
                            val activeAlbum = albums.find { it.name == selectedAlbum!!.name } ?: selectedAlbum!!
                            BackHandler { 
                                if (isSelectionMode) selectedMediaIds = emptySet() else selectedAlbum = null 
                            }
                            if (activeAlbum.mediaList.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text("No photos in this album yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Select photos in the gallery and choose 'Add to Album'", fontSize = 13.sp, color = Color.Gray)
                                }
                            } else {
                                MediaGrid(
                                    mediaList = activeAlbum.mediaList,
                                    imageLoader = imageLoader,
                                    selectedMediaIds = selectedMediaIds,
                                    gridColumns = gridColumns,
                                    gridState = albumDetailState,
                                    onGridColumnsChange = { gridColumns = it },
                                    onItemClick = { item, index ->
                                        if (isSelectionMode) {
                                            selectedMediaIds = if (selectedMediaIds.contains(item.id)) selectedMediaIds - item.id else selectedMediaIds + item.id
                                        } else {
                                            viewerState = activeAlbum.mediaList to index
                                        }
                                    },
                                    onItemLongClick = { item ->
                                        selectedMediaIds = if (selectedMediaIds.contains(item.id)) selectedMediaIds - item.id else selectedMediaIds + item.id
                                    },
                                    onDragSelect = { item ->
                                        if (isSelectionMode) {
                                            selectedMediaIds = selectedMediaIds + item.id
                                        }
                                    },
                                    onSelectGroup = { group ->
                                        selectedMediaIds = selectedMediaIds + group.map { it.id }
                                    },
                                    onDeselectGroup = { group ->
                                        selectedMediaIds = selectedMediaIds - group.map { it.id }.toSet()
                                    }
                                )
                            }
                        } else {
                            AlbumGrid(
                                albums = albums,
                                imageLoader = imageLoader,
                                gridState = albumListState,
                                onAlbumClick = { album ->
                                    selectedAlbum = album
                                },
                                onAlbumLongClick = { album ->
                                    albumToManage = album
                                },
                                onUtilityClick = { utility ->
                                    when (utility) {
                                        "favorites" -> {
                                            val favs = displayedMedia.filter { it.isFavorite }
                                            val cover = favs.firstOrNull() ?: displayedMedia.firstOrNull()
                                            if (cover != null) selectedAlbum = Album("Favorites", cover, favs.size, favs)
                                        }
                                        "videos" -> {
                                            val vids = displayedMedia.filter { it.isVideo }
                                            val cover = vids.firstOrNull() ?: displayedMedia.firstOrNull()
                                            if (cover != null) selectedAlbum = Album("Videos", cover, vids.size, vids)
                                        }
                                        "edited" -> {
                                            val edited = displayedMedia.filter { it.isEdited }
                                            val cover = edited.firstOrNull() ?: displayedMedia.firstOrNull()
                                            if (cover != null) selectedAlbum = Album("Edited", cover, edited.size, edited)
                                        }
                                        "vault", "hidden" -> {
                                            if (isVaultUnlocked) {
                                                val hidden = mediaList.filter { it.isHidden && it.trashedAt == 0L }
                                                val cover = hidden.firstOrNull() ?: mediaList.firstOrNull()
                                                if (cover != null) selectedAlbum = Album("Locked Vault", cover, hidden.size, hidden)
                                            } else {
                                                showVaultPinModal = true
                                            }
                                        }
                                        "trash" -> {
                                            val trashed = mediaList.filter { it.trashedAt > 0 }
                                            val cover = trashed.firstOrNull() ?: mediaList.firstOrNull()
                                            if (cover != null) selectedAlbum = Album("Recently Deleted", cover, trashed.size, trashed)
                                        }
                                        "themes" -> {
                                            showThemesSheet = true
                                        }
                                    }
                                },
                                onCreateAlbumClick = {
                                    showCreateAlbumDialog = true
                                }
                            )
                        }
                    }
                    2 -> { // Memories View
                        MemoriesView(
                            mediaList = mediaList.sortedByDescending { it.dateTakenMs },
                            imageLoader = imageLoader,
                            onItemClick = { item ->
                                val index = mediaList.indexOf(item).coerceAtLeast(0)
                                viewerState = mediaList to index
                            }
                        )
                    }
                    3 -> { // Search View
                        SearchView(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            mediaList = displayedMedia,
                            imageLoader = imageLoader,
                            selectedMediaIds = selectedMediaIds,
                            gridColumns = gridColumns,
                            onItemClick = { item ->
                                if (isSelectionMode) {
                                    selectedMediaIds = if (selectedMediaIds.contains(item.id)) selectedMediaIds - item.id else selectedMediaIds + item.id
                                } else {
                                    val index = displayedMedia.indexOf(item).coerceAtLeast(0)
                                    viewerState = displayedMedia to index
                                }
                            },
                            onItemLongClick = { item ->
                                selectedMediaIds = selectedMediaIds + item.id
                            },
                            onDragSelect = { item ->
                                if (isSelectionMode) {
                                    selectedMediaIds = selectedMediaIds + item.id
                                }
                            },
                            onSelectGroup = { group ->
                                selectedMediaIds = selectedMediaIds + group.map { it.id }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Overlays wrapped in AnimatedVisibility
        AnimatedVisibility(
            visible = viewerState != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            viewerState?.let { (list, initialIndex) ->
                InteractiveViewerPager(
                    mediaList = list,
                    initialIndex = initialIndex,
                    imageLoader = imageLoader,
                    onClose = { viewerState = null },
                    onShare = { item -> shareMedia(listOf(item)) },
                    onFavorite = { item ->
                        val newFav = !item.isFavorite
                        // INSTANT UI UPDATE
                        mediaList = mediaList.map { if (it.id == item.id) it.copy(isFavorite = newFav) else it }
                        viewerState?.let { (currentList, currentIdx) ->
                            val updatedList = currentList.map {
                                if (it.id == item.id) it.copy(isFavorite = newFav) else it
                            }
                            viewerState = updatedList to currentIdx
                        }
                        
                        // BACKGROUND DB UPDATE
                        coroutineScope.launch(Dispatchers.IO) {
                            val db = GalleryDatabase.getDatabase(context)
                            val dao = db.mediaStateDao()
                            val state = dao.getState(item.id) ?: MediaState(item.id)
                            dao.insertOrUpdate(state.copy(isFavorite = newFav))
                        }
                    },
                    onInfo = { item -> infoTarget = item },
                    onEdit = { item -> editorTarget = item },
                    onDelete = { item ->
                        if (item.trashedAt > 0L) {
                            permanentDeleteTargets = listOf(item)
                        } else {
                            if (System.currentTimeMillis() < deleteSnoozeUntil) {
                                performTrash(listOf(item))
                            } else {
                                deleteConfirmTargets = listOf(item)
                            }
                        }
                    },
                    onRestore = { item ->
                        mediaList = mediaList.map { if (it.id == item.id) it.copy(trashedAt = 0L) else it }
                        coroutineScope.launch(Dispatchers.IO) {
                            val db = GalleryDatabase.getDatabase(context)
                            val dao = db.mediaStateDao()
                            val state = dao.getState(item.id) ?: MediaState(item.id)
                            dao.insertOrUpdate(state.copy(trashedAt = 0L))
                        }
                        viewerState = null
                    }
                )
            }
        }

        // Compare Screen
        AnimatedVisibility(
            visible = compareTargets != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            compareTargets?.let { targets ->
                CompareScreen(
                    mediaItems = targets,
                    imageLoader = imageLoader,
                    onClose = { compareTargets = null },
                    onFavorite = { item ->
                        val newFav = !item.isFavorite
                        // INSTANT UI UPDATE
                        mediaList = mediaList.map { if (it.id == item.id) it.copy(isFavorite = newFav) else it }
                        
                        // BACKGROUND DB UPDATE
                        coroutineScope.launch(Dispatchers.IO) {
                            val db = GalleryDatabase.getDatabase(context)
                            val dao = db.mediaStateDao()
                            val state = dao.getState(item.id) ?: MediaState(item.id)
                            dao.insertOrUpdate(state.copy(isFavorite = newFav))
                        }
                    },
                    onDelete = { item ->
                        if (System.currentTimeMillis() < deleteSnoozeUntil) {
                            performTrash(listOf(item))
                        } else {
                            deleteConfirmTargets = listOf(item)
                        }
                        compareTargets = compareTargets?.filter { it.id != item.id }
                        if (compareTargets?.isEmpty() == true) {
                            compareTargets = null
                            selectedMediaIds = emptySet()
                        }
                    }
                )
            }
        }

        // Editor Screen
        AnimatedVisibility(
            visible = editorTarget != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            editorTarget?.let { target ->
                EditorScreen(
                    item = target,
                    imageLoader = imageLoader,
                    isSaving = isEditorSaving,
                    onClose = { editorTarget = null },
                    onSave = { rotation, exposure, contrast, saturation, filter ->
                        coroutineScope.launch {
                            isEditorSaving = true
                            val savedUri = withContext(Dispatchers.IO) {
                                saveEditedImageBitmap(
                                    context = context,
                                    item = target,
                                    rotation = rotation,
                                    exposure = exposure,
                                    contrast = contrast,
                                    saturation = saturation,
                                    filter = filter
                                )
                            }
                            isEditorSaving = false
                            if (savedUri != null) {
                                Toast.makeText(context, "Saved to Pictures/YourGallery", Toast.LENGTH_SHORT).show()
                                val refreshed = withContext(Dispatchers.IO) {
                                    MediaRepository.loadAllDeviceMedia(context)
                                }
                                mediaList = refreshed
                            } else {
                                Toast.makeText(context, "Failed to save edited photo", Toast.LENGTH_SHORT).show()
                            }
                            editorTarget = null
                        }
                    }
                )
            }
        }

        // Info / Details Modal Bottom Sheet
        infoTarget?.let { item ->
            var exifMap by remember { mutableStateOf<Map<String, String>?>(null) }
            LaunchedEffect(item) {
                exifMap = loadExifData(context, item.uri)
            }
            ModalBottomSheet(onDismissRequest = { infoTarget = null }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Media Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    DetailRow("File Name", item.displayName)
                    DetailRow("Album Folder", item.bucketName)
                    DetailRow("Date Taken", SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(item.dateTakenMs)))
                    DetailRow("File Size", Formatter.formatShortFileSize(context, item.sizeBytes))
                    DetailRow("Type", if (item.isVideo) "Video (${item.durationMs / 1000}s)" else "Photo")
                    
                    if (exifMap != null && !item.isVideo) {
                        Spacer(Modifier.height(8.dp))
                        Text("EXIF Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        exifMap!!.forEach { (key, value) ->
                            DetailRow(key, value)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Delete Confirmation Dialog
        deleteConfirmTargets?.let { targets ->
            AlertDialog(
                onDismissRequest = { deleteConfirmTargets = null },
                title = { Text(if (targets.size > 1) "Move ${targets.size} items to Trash?" else "Move to Trash?") },
                text = { Text("You can snooze confirmation temporarily (15 minutes).") },
                confirmButton = {
                    TextButton(onClick = {
                        val itemsToTrash = targets
                        deleteConfirmTargets = null
                        performTrash(itemsToTrash)
                    }) { Text("Move to Trash", color = Color.Red) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            deleteSnoozeUntil = System.currentTimeMillis() + 15 * 60 * 1000L
                            val itemsToTrash = targets
                            deleteConfirmTargets = null
                            performTrash(itemsToTrash)
                        }) { Text("Snooze 15m") }
                        TextButton(onClick = { deleteConfirmTargets = null }) { Text("Cancel") }
                    }
                }
            )
        }

        // Create New Album Dialog
        if (showCreateAlbumDialog) {
            var newAlbumName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateAlbumDialog = false },
                title = { Text("Create New Album") },
                text = {
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        label = { Text("Album Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newAlbumName.isNotBlank()) {
                                val name = newAlbumName.trim()
                                val updated = customAlbumNames + name
                                customAlbumNames = updated
                                prefs.edit().putStringSet("custom_albums", updated).apply()
                                val firstItem = mediaList.firstOrNull()
                                val album = Album(name, firstItem, 0, emptyList())
                                selectedAlbum = album
                                showCreateAlbumDialog = false
                            }
                        }
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateAlbumDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Add to Album Dialog (Selection Mode)
        if (showAddToAlbumDialog) {
            var targetAlbumInput by remember { mutableStateOf("") }
            val existingAlbums = remember(mediaList) {
                MediaRepository.groupIntoAlbums(mediaList).map { it.name }.filter { it !in listOf("Camera", "Videos", "Screenshots", "Downloads", "Recently Deleted") }
            }
            val selectedItems = remember(mediaList, selectedMediaIds) {
                mediaList.filter { selectedMediaIds.contains(it.id) }
            }
            AlertDialog(
                onDismissRequest = { showAddToAlbumDialog = false },
                title = { Text("Add to Album") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${selectedItems.size} items will be added.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (existingAlbums.isNotEmpty()) {
                            Text("Existing Albums:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(existingAlbums) { albumName ->
                                    FilterChip(
                                        selected = targetAlbumInput == albumName,
                                        onClick = { targetAlbumInput = albumName },
                                        label = { Text(albumName) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = targetAlbumInput,
                            onValueChange = { targetAlbumInput = it },
                            label = { Text("Album Name") },
                            placeholder = { Text("e.g. Vacation, Trips, Work") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (targetAlbumInput.isNotBlank()) {
                                val targetName = targetAlbumInput.trim()
                                val selectedSet = selectedMediaIds.toSet()
                                mediaList = mediaList.map { item ->
                                    if (item.id in selectedSet) {
                                        val existing = if (item.customAlbums.isBlank()) emptyList() else item.customAlbums.split(",").map { it.trim() }
                                        val updated = if (existing.contains(targetName)) existing else existing + targetName
                                        item.copy(customAlbums = updated.joinToString(","))
                                    } else item
                                }
                                coroutineScope.launch(Dispatchers.IO) {
                                    val db = GalleryDatabase.getDatabase(context)
                                    val dao = db.mediaStateDao()
                                    selectedItems.forEach { item ->
                                        val state = dao.getState(item.id) ?: MediaState(item.id)
                                        val existing = if (state.albums.isBlank()) emptyList() else state.albums.split(",").map { it.trim() }
                                        val updated = if (existing.contains(targetName)) existing else existing + targetName
                                        dao.insertOrUpdate(state.copy(albums = updated.joinToString(",")))
                                    }
                                }
                                selectedMediaIds = emptySet()
                                showAddToAlbumDialog = false
                            }
                        }
                    ) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddToAlbumDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Vault Confirmation Dialog
        if (showVaultConfirmDialog) {
            val selectedItems = remember(mediaList, selectedMediaIds) {
                mediaList.filter { selectedMediaIds.contains(it.id) }
            }
            AlertDialog(
                onDismissRequest = { showVaultConfirmDialog = false },
                title = { Text("Move to Private Vault?") },
                text = {
                    Text("${selectedItems.size} items will be hidden from the main gallery timeline and albums.", fontSize = 14.sp)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedSet = selectedMediaIds.toSet()
                            mediaList = mediaList.map { if (it.id in selectedSet) it.copy(isHidden = true) else it }
                            coroutineScope.launch(Dispatchers.IO) {
                                val db = GalleryDatabase.getDatabase(context)
                                val dao = db.mediaStateDao()
                                selectedItems.forEach { item ->
                                    val state = dao.getState(item.id) ?: MediaState(item.id)
                                    dao.insertOrUpdate(state.copy(isHidden = true))
                                }
                            }
                            selectedMediaIds = emptySet()
                            showVaultConfirmDialog = false
                        }
                    ) { Text("Move to Vault") }
                },
                dismissButton = {
                    TextButton(onClick = { showVaultConfirmDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Album Management Sheet
        albumToManage?.let { album ->
            val isCustom = album.name !in listOf("Camera", "Screenshots", "Downloads", "Videos")
            ModalBottomSheet(onDismissRequest = { albumToManage = null }) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Manage Album", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                    Text(album.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                    
                    if (isCustom) {
                        ListItem(
                            headlineContent = { Text("Rename Album") },
                            leadingContent = { Icon(Icons.Filled.Edit, null) },
                            modifier = Modifier.clickable {
                                showRenameAlbumDialog = true
                                albumToManage = album
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Delete Album") },
                            leadingContent = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFFF5252)) },
                            modifier = Modifier.clickable {
                                showDeleteAlbumDialog = true
                                albumToManage = album
                            }
                        )
                    } else {
                        Text("Device folders cannot be renamed or deleted directly.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        if (showRenameAlbumDialog && albumToManage != null) {
            var newName by remember { mutableStateOf(albumToManage!!.name) }
            AlertDialog(
                onDismissRequest = { showRenameAlbumDialog = false },
                title = { Text("Rename Album") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val oldName = albumToManage!!.name
                        val finalName = newName.trim()
                        if (finalName.isNotBlank() && finalName != oldName) {
                            val updatedSet = (customAlbumNames - oldName) + finalName
                            customAlbumNames = updatedSet
                            prefs.edit().putStringSet("custom_albums", updatedSet).apply()
                            coroutineScope.launch(Dispatchers.IO) {
                                val db = GalleryDatabase.getDatabase(context)
                                val dao = db.mediaStateDao()
                                val states = dao.getAllStates()
                                states.forEach { state ->
                                    val currentAlbums = state.albums.split(",").map { it.trim() }.toMutableList()
                                    if (currentAlbums.contains(oldName)) {
                                        currentAlbums.remove(oldName)
                                        currentAlbums.add(finalName)
                                        dao.insertOrUpdate(state.copy(albums = currentAlbums.filter { it.isNotEmpty() }.joinToString(",")))
                                    }
                                }
                                val refreshed = MediaRepository.loadAllDeviceMedia(context)
                                withContext(Dispatchers.Main) {
                                    mediaList = refreshed
                                    showRenameAlbumDialog = false
                                    albumToManage = null
                                }
                            }
                        }
                    }) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameAlbumDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showDeleteAlbumDialog && albumToManage != null) {
            AlertDialog(
                onDismissRequest = { showDeleteAlbumDialog = false },
                title = { Text("Delete Album?") },
                text = { Text("Are you sure you want to delete '${albumToManage!!.name}'? The photos will remain in your gallery, but the album will be removed.") },
                confirmButton = {
                    TextButton(onClick = {
                        val nameToRemove = albumToManage!!.name
                        val updatedSet = customAlbumNames - nameToRemove
                        customAlbumNames = updatedSet
                        prefs.edit().putStringSet("custom_albums", updatedSet).apply()
                        coroutineScope.launch(Dispatchers.IO) {
                            val db = GalleryDatabase.getDatabase(context)
                            val dao = db.mediaStateDao()
                            val states = dao.getAllStates()
                            states.forEach { state ->
                                val currentAlbums = state.albums.split(",").map { it.trim() }.toMutableList()
                                if (currentAlbums.contains(nameToRemove)) {
                                    currentAlbums.remove(nameToRemove)
                                    dao.insertOrUpdate(state.copy(albums = currentAlbums.filter { it.isNotEmpty() }.joinToString(",")))
                                }
                            }
                            val refreshed = MediaRepository.loadAllDeviceMedia(context)
                            withContext(Dispatchers.Main) {
                                mediaList = refreshed
                                showDeleteAlbumDialog = false
                                albumToManage = null
                            }
                        }
                    }) { Text("Delete", color = Color(0xFFFF5252)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAlbumDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Empty Trash Confirmation Dialog
        if (showEmptyTrashDialog) {
            val trashedItems = remember(mediaList) { mediaList.filter { it.trashedAt > 0 } }
            AlertDialog(
                onDismissRequest = { showEmptyTrashDialog = false },
                title = { Text("Empty Trash?") },
                text = { Text("All ${trashedItems.size} items in Recently Deleted will be permanently deleted.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEmptyTrashDialog = false
                            selectedAlbum = null
                            performPermanentDelete(trashedItems)
                        }
                    ) { Text("Empty Trash", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Permanent Delete Confirmation Dialog (Single/Selection in Trash)
        permanentDeleteTargets?.let { targets ->
            AlertDialog(
                onDismissRequest = { permanentDeleteTargets = null },
                title = { Text(if (targets.size > 1) "Permanently delete ${targets.size} items?" else "Permanently delete item?") },
                text = { Text("This will permanently remove the files from your device storage. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val toDelete = targets
                            permanentDeleteTargets = null
                            performPermanentDelete(toDelete)
                        }
                    ) { Text("Delete Forever", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { permanentDeleteTargets = null }) { Text("Cancel") }
                }
            )
        }

        // Vault PIN Setup/Unlock Dialog
        if (showVaultPinModal) {
            val savedPinHash = prefs.getString("vault_pin_hash", null)
            VaultPinDialog(
                hasExistingPin = savedPinHash != null,
                onVerifyPin = { pin ->
                    if (hashVaultPin(pin) == savedPinHash) {
                        isVaultUnlocked = true
                        showVaultPinModal = false
                        val hidden = mediaList.filter { it.isHidden && it.trashedAt == 0L }
                        val cover = hidden.firstOrNull() ?: mediaList.firstOrNull()
                        if (cover != null) selectedAlbum = Album("Locked Vault", cover, hidden.size, hidden)
                        true
                    } else {
                        false
                    }
                },
                onSetPin = { newPin ->
                    prefs.edit().putString("vault_pin_hash", hashVaultPin(newPin)).apply()
                    isVaultUnlocked = true
                    showVaultPinModal = false
                    val hidden = mediaList.filter { it.isHidden && it.trashedAt == 0L }
                    val cover = hidden.firstOrNull() ?: mediaList.firstOrNull()
                    if (cover != null) selectedAlbum = Album("Locked Vault", cover, hidden.size, hidden)
                    Toast.makeText(context, "Vault PIN set successfully", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showVaultPinModal = false }
            )
        }

        // Themes & Customization Sheet
        if (showThemesSheet) {
            ModalBottomSheet(onDismissRequest = { showThemesSheet = false }) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Settings & Options", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    Text("Sort By", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortOrder.entries.take(2).forEach { s ->
                            FilterChip(
                                selected = sortOrder == s,
                                onClick = { sortOrder = s },
                                label = { Text(s.label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortOrder.entries.drop(2).forEach { s ->
                            FilterChip(
                                selected = sortOrder == s,
                                onClick = { sortOrder = s },
                                label = { Text(s.label) }
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    Text("Grid Layout Columns", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 3, 4, 5).forEach { cols ->
                            FilterChip(
                                selected = gridColumns == cols,
                                onClick = { gridColumns = cols },
                                label = { Text(cols.toString()) }
                            )
                        }
                    }
                    
                    HorizontalDivider()

                    Text("Theme Palette", style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it })
                    }
                    
                    HorizontalDivider()

                    Text("Gallery Title", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = galleryTitle,
                        onValueChange = {
                            galleryTitle = it
                            prefs.edit().putString("gallery_title", it).apply()
                        },
                        placeholder = { Text("Your Gallery") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    Text("Security & Privacy", style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Vault PIN Protection", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (prefs.getString("vault_pin_hash", null) != null) "PIN protection is active" else "No PIN set",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        TextButton(onClick = {
                            showThemesSheet = false
                            showVaultPinModal = true
                        }) {
                            Text(if (prefs.getString("vault_pin_hash", null) != null) "Change PIN" else "Set PIN")
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = "Your Gallery • Fluid & Open Source", 
                        fontSize = 10.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SelectionActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = tint, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun VaultPinDialog(
    hasExistingPin: Boolean,
    onVerifyPin: (String) -> Boolean,
    onSetPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val title = if (hasExistingPin) {
        "Unlock Vault"
    } else if (confirmPin == null) {
        "Set Vault PIN"
    } else {
        "Confirm Vault PIN"
    }

    val subtitle = if (hasExistingPin) {
        "Enter your 4-digit PIN to access private media."
    } else if (confirmPin == null) {
        "Choose a 4-digit PIN to protect private photos."
    } else {
        "Re-enter your 4-digit PIN to confirm."
    }

    fun handleDigit(digit: String) {
        if (pin.length >= 4) return
        val nextPin = pin + digit
        errorMessage = null
        pin = nextPin

        if (nextPin.length == 4) {
            if (hasExistingPin) {
                val ok = onVerifyPin(nextPin)
                if (!ok) {
                    errorMessage = "Incorrect PIN. Try again."
                    pin = ""
                }
            } else {
                if (confirmPin == null) {
                    confirmPin = nextPin
                    pin = ""
                } else if (confirmPin == nextPin) {
                    onSetPin(nextPin)
                } else {
                    errorMessage = "PINs did not match. Start over."
                    confirmPin = null
                    pin = ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp).padding(bottom = 8.dp)
                )
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    repeat(4) { idx ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (idx < pin.length) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        errorMessage!!,
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { k ->
                            if (k.isEmpty()) {
                                Spacer(Modifier.size(56.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(28.dp))
                                        .clickable {
                                            if (k == "DEL") {
                                                if (pin.isNotEmpty()) {
                                                    pin = pin.dropLast(1)
                                                    errorMessage = null
                                                }
                                            } else {
                                                handleDigit(k)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (k == "DEL") {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            k,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun saveEditedImageBitmap(
    context: Context,
    item: DeviceMedia,
    rotation: Float,
    exposure: Float,
    contrast: Float,
    saturation: Float,
    filter: String
): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(item.uri) ?: return null
        val sourceBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()

        val matrix = Matrix()
        if (rotation != 0f) {
            matrix.postRotate(rotation)
        }
        val rotatedBitmap = if (rotation != 0f) {
            Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true).also {
                if (it != sourceBitmap) sourceBitmap.recycle()
            }
        } else {
            sourceBitmap
        }

        val cm = android.graphics.ColorMatrix()
        when (filter) {
            "Rose Film" -> {
                val rose = android.graphics.ColorMatrix(floatArrayOf(
                    1.15f, 0.05f, 0.05f, 0f, 15f,
                    0.05f, 0.95f, 0.05f, 0f, 5f,
                    0.05f, 0.05f, 1.05f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(rose)
            }
            "Soft Glow" -> {
                val glow = android.graphics.ColorMatrix(floatArrayOf(
                    1.08f, 0.02f, 0.02f, 0f, 10f,
                    0.02f, 1.05f, 0.02f, 0f, 10f,
                    0.02f, 0.02f, 1.02f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(glow)
            }
            "Vivid" -> {
                val vivid = android.graphics.ColorMatrix().apply { setSaturation(1.6f) }
                cm.postConcat(vivid)
            }
            "Golden" -> {
                val golden = android.graphics.ColorMatrix(floatArrayOf(
                    1.25f, 0.05f, 0f, 0f, 12f,
                    0.05f, 1.10f, 0f, 0f, 8f,
                    0f, 0f, 0.85f, 0f, -5f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(golden)
            }
            "Dramatic" -> {
                val dramatic = android.graphics.ColorMatrix().apply { setSaturation(0.6f) }
                cm.postConcat(dramatic)
            }
            "Mono", "Noir" -> {
                val mono = android.graphics.ColorMatrix().apply { setSaturation(0f) }
                cm.postConcat(mono)
            }
        }

        if (exposure != 0f) {
            val brightnessAdd = exposure * 100f
            val brightMat = android.graphics.ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, brightnessAdd,
                0f, 1f, 0f, 0f, brightnessAdd,
                0f, 0f, 1f, 0f, brightnessAdd,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(brightMat)
        }

        if (contrast != 1f) {
            val scale = contrast
            val translate = (-0.5f * scale + 0.5f) * 255f
            val contrastMat = android.graphics.ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastMat)
        }

        if (saturation != 1f && filter != "Mono" && filter != "Noir") {
            val satMat = android.graphics.ColorMatrix().apply { setSaturation(saturation) }
            cm.postConcat(satMat)
        }

        val resultBitmap = Bitmap.createBitmap(rotatedBitmap.width, rotatedBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(rotatedBitmap, 0f, 0f, paint)
        rotatedBitmap.recycle()

        val filename = "EDIT_${System.currentTimeMillis()}_${item.displayName.replace(".png", ".jpg").replace(".webp", ".jpg")}"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/YourGallery")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val insertedUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (insertedUri != null) {
            context.contentResolver.openOutputStream(insertedUri)?.use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(insertedUri, contentValues, null, null)
            }
        }
        resultBitmap.recycle()
        insertedUri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun hashVaultPin(pin: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(pin.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}



