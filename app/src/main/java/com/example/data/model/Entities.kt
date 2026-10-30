package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val format: String,
    val type: String, // TEXT, URL, WIFI, PHONE, CONTACT, BARCODE
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(tableName = "trash_items")
data class TrashRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val trashPath: String,
    val fileName: String,
    val fileExtension: String,
    val fileSize: Long,
    val deletedTimestamp: Long = System.currentTimeMillis(),
    val mimeType: String = ""
)

@Entity(tableName = "video_history")
data class VideoHistoryRecord(
    @PrimaryKey val videoUri: String,
    val videoTitle: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_files")
data class FavoriteRecord(
    @PrimaryKey val filePath: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val addedTimestamp: Long = System.currentTimeMillis()
)

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
