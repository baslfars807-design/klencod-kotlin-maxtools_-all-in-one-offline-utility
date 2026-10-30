package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.FavoriteRecord
import com.example.data.model.ScanRecord
import com.example.data.model.TrashRecord
import com.example.data.model.VideoHistoryRecord

@Database(
    entities = [
        ScanRecord::class,
        TrashRecord::class,
        VideoHistoryRecord::class,
        FavoriteRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MaxToolsDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun trashDao(): TrashDao
    abstract fun videoHistoryDao(): VideoHistoryDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: MaxToolsDatabase? = null

        fun getDatabase(context: Context): MaxToolsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MaxToolsDatabase::class.java,
                    "maxtools_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
