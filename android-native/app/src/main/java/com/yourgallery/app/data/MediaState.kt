package com.yourgallery.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_state")
data class MediaState(
    @PrimaryKey val mediaId: Long,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isEdited: Boolean = false,
    val trashedAt: Long = 0L, // 0 means not trashed, >0 is the timestamp it was trashed
    val caption: String = "",
    val tags: String = "", // Comma-separated tags
    val albums: String = "" // Comma-separated album names
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val key: String,
    val value: String
)
