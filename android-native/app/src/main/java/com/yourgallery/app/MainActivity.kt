package com.yourgallery.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
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
        enabled = isSearching || isSelectionMode || compareTargets != null || editorTarget != null || viewerState != null || selectedAlbum != null || infoTarget != null || showThemesSheet
    ) {
        when {
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
                    TopAppBar(
                        title = { Text("${selectedMediaIds.size} Selected", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { selectedMediaIds = emptySet() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Clear Selection")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                if (selectedMediaIds.size == mediaList.size) {
                                    selectedMediaIds = emptySet()
                                } else {
                                    selectedMediaIds = mediaList.map { it.id }.toSet()
                                }
                            }) {
                                Text(
                                    text = if (selectedMediaIds.size == mediaList.size) "Deselect All" else "Select All",
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
                        val selectedItems = mediaList.filter { selectedMediaIds.contains(it.id) }
                        
                        SelectionActionButton(
                            icon = Icons.Filled.FavoriteBorder,
                            label = "Favorite",
                            onClick = { 
                                // INSTANT UI UPDATE
                                val selectedSet = selectedMediaIds.toSet()
                                mediaList = mediaList.map { 
                                    if (it.id in selectedSet) it.copy(isFavorite = !it.isFavorite) else it 
                                }
                                selectedMediaIds = emptySet()

                                // BACKGROUND DB UPDATE
                                coroutineScope.launch(Dispatchers.IO) {
                                    val db = GalleryDatabase.getDatabase(context)
                                    val dao = db.mediaStateDao()
                                    selectedItems.forEach { item ->
                                        val state = dao.getState(item.id) ?: MediaState(item.id)
                                        dao.insertOrUpdate(state.copy(isFavorite = !state.isFavorite))
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        SelectionActionButton(
                            icon = Icons.Filled.LibraryAdd,
                            label = "Album",
                            onClick = { /* Add to Album stub */ },
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
                            onClick = { /* Lock stub */ },
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
                    val filtered = if (searchQuery.isBlank()) mediaList else mediaList.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
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
                        val albums = remember(displayedMedia) { MediaRepository.groupIntoAlbums(displayedMedia) }
                        if (selectedAlbum != null) {
                            // Find the updated album reference
                            val activeAlbum = albums.find { it.name == selectedAlbum!!.name } ?: selectedAlbum!!
                            BackHandler { 
                                if (isSelectionMode) selectedMediaIds = emptySet() else selectedAlbum = null 
                            }
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
                                            val hidden = displayedMedia.filter { it.isHidden }
                                            val cover = hidden.firstOrNull() ?: displayedMedia.firstOrNull()
                                            if (cover != null) selectedAlbum = Album("Locked Vault", cover, hidden.size, hidden)
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
                        if (System.currentTimeMillis() < deleteSnoozeUntil) {
                            performTrash(listOf(item))
                        } else {
                            deleteConfirmTargets = listOf(item)
                        }
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
                    onClose = { editorTarget = null },
                    onSave = { 
                        // INSTANT UI UPDATE
                        mediaList = mediaList.map { if (it.id == target.id) it.copy(isEdited = true) else it }
                        editorTarget = null
                        
                        // BACKGROUND DB UPDATE
                        coroutineScope.launch(Dispatchers.IO) {
                            val db = GalleryDatabase.getDatabase(context)
                            val dao = db.mediaStateDao()
                            val state = dao.getState(target.id) ?: MediaState(target.id)
                            dao.insertOrUpdate(state.copy(isEdited = true))
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
                                if (mediaList.isNotEmpty()) {
                                    val firstItem = mediaList.first()
                                    val album = Album(name, firstItem, 0, emptyList())
                                    selectedAlbum = album
                                }
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
            AlertDialog(
                onDismissRequest = { showEmptyTrashDialog = false },
                title = { Text("Empty Trash?") },
                text = { Text("All items in Recently Deleted will be permanently deleted.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val db = GalleryDatabase.getDatabase(context)
                                val dao = db.mediaStateDao()
                                val trashed = dao.getAllStates().filter { it.trashedAt > 0 }
                                trashed.forEach { state ->
                                    dao.insertOrUpdate(state.copy(trashedAt = 0))
                                }
                                val refreshed = MediaRepository.loadAllDeviceMedia(context)
                                withContext(Dispatchers.Main) {
                                    mediaList = refreshed
                                    selectedAlbum = null
                                    showEmptyTrashDialog = false
                                }
                            }
                        }
                    ) { Text("Empty Trash", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashDialog = false }) { Text("Cancel") }
                }
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



