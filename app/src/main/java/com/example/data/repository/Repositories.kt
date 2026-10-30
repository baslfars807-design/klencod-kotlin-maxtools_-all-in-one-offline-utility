package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.data.local.FavoriteDao
import com.example.data.local.ScanDao
import com.example.data.local.TrashDao
import com.example.data.local.VideoHistoryDao
import com.example.data.model.FavoriteRecord
import com.example.data.model.ScanRecord
import com.example.data.model.TrashRecord
import com.example.data.model.VideoHistoryRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanRecord>> = scanDao.getAllScans()

    suspend fun insertScan(record: ScanRecord) = scanDao.insertScan(record)
    suspend fun deleteScan(record: ScanRecord) = scanDao.deleteScan(record)
    suspend fun clearHistory() = scanDao.clearAll()
    suspend fun toggleFavorite(record: ScanRecord) {
        scanDao.updateScan(record.copy(isFavorite = !record.isFavorite))
    }
}

class TrashRepository(
    private val trashDao: TrashDao,
    private val context: Context
) {
    val allTrash: Flow<List<TrashRecord>> = trashDao.getAllTrash()

    private val trashDir: File by lazy {
        val dir = File(context.filesDir, ".maxtools_trash")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    suspend fun moveToTrash(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        try {
            val trashTarget = File(trashDir, "${System.currentTimeMillis()}_${file.name}")
            val success = file.renameTo(trashTarget) || (file.copyTo(trashTarget, overwrite = true).exists() && file.delete())
            if (success) {
                val record = TrashRecord(
                    originalPath = file.absolutePath,
                    trashPath = trashTarget.absolutePath,
                    fileName = file.name,
                    fileExtension = file.extension,
                    fileSize = trashTarget.length()
                )
                trashDao.insertTrash(record)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFile(record: TrashRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(record.trashPath)
            val originalFile = File(record.originalPath)
            val parent = originalFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            if (trashFile.exists()) {
                val success = trashFile.renameTo(originalFile) || (trashFile.copyTo(originalFile, overwrite = true).exists() && trashFile.delete())
                if (success) {
                    trashDao.deleteById(record.id)
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun permanentlyDelete(record: TrashRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(record.trashPath)
            if (trashFile.exists()) {
                trashFile.delete()
            }
            trashDao.deleteById(record.id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun emptyTrash(): Boolean = withContext(Dispatchers.IO) {
        try {
            val files = trashDir.listFiles()
            files?.forEach { it.deleteRecursively() }
            trashDao.clearAll()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

class VideoHistoryRepository(private val videoHistoryDao: VideoHistoryDao) {
    val allHistory: Flow<List<VideoHistoryRecord>> = videoHistoryDao.getAllHistory()

    suspend fun getPosition(uri: String): Long {
        return videoHistoryDao.getByUri(uri)?.lastPositionMs ?: 0L
    }

    suspend fun savePosition(uri: String, title: String, posMs: Long, durationMs: Long) {
        videoHistoryDao.saveHistory(
            VideoHistoryRecord(
                videoUri = uri,
                videoTitle = title,
                lastPositionMs = posMs,
                durationMs = durationMs,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
        )
    }
}

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    val allFavorites: Flow<List<FavoriteRecord>> = favoriteDao.getAllFavorites()

    fun isFavoriteFlow(path: String): Flow<Boolean> = favoriteDao.isFavoriteFlow(path)

    suspend fun toggleFavorite(file: File) {
        val exists = favoriteDao.isFavorite(file.absolutePath)
        if (exists) {
            favoriteDao.removeFavorite(file.absolutePath)
        } else {
            favoriteDao.addFavorite(
                FavoriteRecord(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    fileType = file.extension.uppercase(),
                    fileSize = file.length()
                )
            )
        }
    }
}

data class StorageStats(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val usedPercentage: Float,
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val audioCount: Int = 0,
    val documentCount: Int = 0,
    val apkCount: Int = 0,
    val totalFilesCount: Int = 0
)

class StorageRepository(private val context: Context) {
    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - freeBytes
        val usedPercentage = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f

        // Count files in app files and common media directories
        var photos = 0
        var videos = 0
        var audio = 0
        var docs = 0
        var apks = 0

        val targetDirs = listOfNotNull(
            context.filesDir,
            context.getExternalFilesDir(null),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        )

        val imageExts = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
        val videoExts = setOf("mp4", "mkv", "3gp", "avi", "mov", "webm", "flv")
        val audioExts = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg")
        val docExts = setOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx")
        val apkExts = setOf("apk", "apks", "xapk", "apkm")

        for (dir in targetDirs) {
            if (dir.exists()) {
                dir.walkTopDown().maxDepth(3).forEach { f ->
                    if (f.isFile) {
                        val ext = f.extension.lowercase()
                        when {
                            ext in imageExts -> photos++
                            ext in videoExts -> videos++
                            ext in audioExts -> audio++
                            ext in docExts -> docs++
                            ext in apkExts -> apks++
                        }
                    }
                }
            }
        }

        StorageStats(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            usedPercentage = usedPercentage,
            photoCount = photos,
            videoCount = videos,
            audioCount = audio,
            documentCount = docs,
            apkCount = apks,
            totalFilesCount = photos + videos + audio + docs + apks
        )
    }
}
