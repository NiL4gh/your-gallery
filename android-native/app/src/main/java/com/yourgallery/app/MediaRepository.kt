package com.yourgallery.app

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class DeviceMedia(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val bucketName: String,
    val dateTakenMs: Long,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val durationMs: Long,
    // Database injected state
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isEdited: Boolean = false,
    val trashedAt: Long = 0L,
    val caption: String = "",
    val tags: String = "",
    val customAlbums: String = ""
)

data class Album(
    val name: String,
    val coverMedia: DeviceMedia,
    val mediaCount: Int,
    val mediaList: List<DeviceMedia>
)

object MediaRepository {
    fun loadAllDeviceMedia(context: Context): List<DeviceMedia> {
        val items = mutableListOf<DeviceMedia>()

        fun queryCollection(collection: Uri, isVideo: Boolean) {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE,
                if (isVideo) MediaStore.Video.VideoColumns.DURATION else MediaStore.MediaColumns.WIDTH
            )
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val durCol = if (isVideo) cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION) else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val taken = cursor.getLong(takenCol)
                    val added = cursor.getLong(addedCol) * 1000L
                    items.add(
                        DeviceMedia(
                            id = id,
                            uri = uri,
                            displayName = cursor.getString(nameCol) ?: "Photo",
                            bucketName = cursor.getString(bucketCol) ?: "Camera",
                            dateTakenMs = if (taken > 0) taken else added,
                            sizeBytes = cursor.getLong(sizeCol),
                            isVideo = isVideo,
                            durationMs = if (isVideo && durCol >= 0) cursor.getLong(durCol) else 0L
                        )
                    )
                }
            }
        }

        queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false)
        queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
        
        // Fetch database states
        val db = com.yourgallery.app.data.GalleryDatabase.getDatabase(context)
        val statesMap = db.mediaStateDao().getAllStates().associateBy { it.mediaId }

        return items.map { item ->
            val state = statesMap[item.id]
            if (state != null) {
                item.copy(
                    isFavorite = state.isFavorite, 
                    isHidden = state.isHidden, 
                    isEdited = state.isEdited,
                    trashedAt = state.trashedAt,
                    caption = state.caption,
                    tags = state.tags,
                    customAlbums = state.albums
                )
            } else {
                item
            }
        }.sortedByDescending { it.dateTakenMs }
    }

    fun groupIntoAlbums(mediaList: List<DeviceMedia>): List<Album> {
        val standardAlbums = mediaList.groupBy { it.bucketName }.toMutableMap()
        
        // Add custom albums
        mediaList.forEach { item ->
            if (item.customAlbums.isNotBlank()) {
                val albums = item.customAlbums.split(",")
                for (a in albums) {
                    val albumName = a.trim()
                    if (albumName.isNotEmpty()) {
                        val currentList = standardAlbums.getOrDefault(albumName, emptyList())
                        standardAlbums[albumName] = currentList + item
                    }
                }
            }
        }
        
        // Unified Videos album containing ALL videos regardless of folder
        val allVideos = mediaList.filter { it.isVideo }
        if (allVideos.isNotEmpty()) {
            standardAlbums["Videos"] = allVideos
        }

        val priorityNames = listOf("Camera", "Videos", "Screenshots", "Downloads")

        return standardAlbums.map { (bucketName, items) ->
                Album(
                    name = bucketName,
                    coverMedia = items.first(),
                    mediaCount = items.size,
                    mediaList = items.sortedByDescending { it.dateTakenMs }
                )
            }
            .sortedWith(Comparator { a1, a2 ->
                val p1 = priorityNames.indexOfFirst { it.equals(a1.name, ignoreCase = true) }
                val p2 = priorityNames.indexOfFirst { it.equals(a2.name, ignoreCase = true) }
                if (p1 != -1 && p2 != -1) p1.compareTo(p2)
                else if (p1 != -1) -1
                else if (p2 != -1) 1
                else a2.mediaCount.compareTo(a1.mediaCount)
            })
    }
}
