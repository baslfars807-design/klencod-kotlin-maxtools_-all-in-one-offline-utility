package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FavoriteRecord
import com.example.data.model.ScanRecord
import com.example.data.model.TrashRecord
import com.example.data.model.VideoHistoryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(record: ScanRecord): Long

    @Delete
    suspend fun deleteScan(record: ScanRecord)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()

    @Update
    suspend fun updateScan(record: ScanRecord)
}

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY deletedTimestamp DESC")
    fun getAllTrash(): Flow<List<TrashRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrash(record: TrashRecord): Long

    @Delete
    suspend fun deleteTrash(record: TrashRecord)

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trash_items")
    suspend fun clearAll()
}

@Dao
interface VideoHistoryDao {
    @Query("SELECT * FROM video_history ORDER BY lastPlayedTimestamp DESC")
    fun getAllHistory(): Flow<List<VideoHistoryRecord>>

    @Query("SELECT * FROM video_history WHERE videoUri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): VideoHistoryRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistory(record: VideoHistoryRecord)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_files ORDER BY addedTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteRecord>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_files WHERE filePath = :path)")
    fun isFavoriteFlow(path: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_files WHERE filePath = :path)")
    suspend fun isFavorite(path: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(record: FavoriteRecord)

    @Query("DELETE FROM favorite_files WHERE filePath = :path")
    suspend fun removeFavorite(path: String)
}
