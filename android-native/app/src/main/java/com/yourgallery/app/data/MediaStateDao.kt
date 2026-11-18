package com.yourgallery.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaStateDao {
    @Query("SELECT * FROM media_state")
    fun getAllStates(): List<MediaState>

    @Query("SELECT * FROM media_state")
    fun getAllStatesFlow(): Flow<List<MediaState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: MediaState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(states: List<MediaState>)

    @Query("SELECT * FROM media_state WHERE mediaId = :id LIMIT 1")
    suspend fun getState(id: Long): MediaState?

    @Query("UPDATE media_state SET isFavorite = :isFav WHERE mediaId = :id")
    suspend fun updateFavorite(id: Long, isFav: Boolean)

    @Query("UPDATE media_state SET isHidden = :isHidden WHERE mediaId = :id")
    suspend fun updateHidden(id: Long, isHidden: Boolean)

    @Query("UPDATE media_state SET trashedAt = :trashedAt WHERE mediaId IN (:ids)")
    suspend fun updateTrashed(ids: List<Long>, trashedAt: Long)

    @Query("DELETE FROM media_state WHERE mediaId IN (:ids)")
    suspend fun deleteStates(ids: List<Long>)

    @Query("UPDATE media_state SET caption = :caption WHERE mediaId = :id")
    suspend fun updateCaption(id: Long, caption: String)

    @Query("UPDATE media_state SET tags = :tags WHERE mediaId = :id")
    suspend fun updateTags(id: Long, tags: String)

    @Query("UPDATE media_state SET albums = :albums WHERE mediaId = :id")
    suspend fun updateAlbums(id: Long, albums: String)

    // AppConfig
    @Query("SELECT value FROM app_config WHERE `key` = :key LIMIT 1")
    suspend fun getConfig(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: AppConfig)
}