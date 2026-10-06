package com.pixelmarks.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pixelmarks.core.database.dao.BackupRecordDao
import com.pixelmarks.core.database.dao.BookmarkDao
import com.pixelmarks.core.database.dao.BookmarkTagDao
import com.pixelmarks.core.database.dao.CollectionDao
import com.pixelmarks.core.database.dao.ConnectedDeviceDao
import com.pixelmarks.core.database.dao.ReminderDao
import com.pixelmarks.core.database.dao.TagDao
import com.pixelmarks.core.database.entity.BackupRecordEntity
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.BookmarkTagCrossRef
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.core.database.entity.ConnectedDeviceEntity
import com.pixelmarks.core.database.entity.ReminderEntity
import com.pixelmarks.core.database.entity.TagEntity

/**
 * PixelMarks local Room database.
 * Serves as the single source of truth for all bookmarks, collections, tags, reminders, and PC sessions.
 * Version 1 establishes the baseline schema.
 */
@Database(
    entities = [
        BookmarkEntity::class,
        CollectionEntity::class,
        TagEntity::class,
        BookmarkTagCrossRef::class,
        ReminderEntity::class,
        ConnectedDeviceEntity::class,
        BackupRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class PixelMarksDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao
    abstract fun collectionDao(): CollectionDao
    abstract fun tagDao(): TagDao
    abstract fun bookmarkTagDao(): BookmarkTagDao
    abstract fun reminderDao(): ReminderDao
    abstract fun connectedDeviceDao(): ConnectedDeviceDao
    abstract fun backupRecordDao(): BackupRecordDao

    companion object {
        const val DATABASE_NAME = "pixelmarks.db"

        @Volatile
        private var instance: PixelMarksDatabase? = null

        /**
         * Returns the application-wide singleton database instance.
         */
        fun getInstance(context: Context): PixelMarksDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): PixelMarksDatabase {
            return Room.databaseBuilder(
                context,
                PixelMarksDatabase::class.java,
                DATABASE_NAME
            ).build()
        }

        /**
         * In-memory database builder for isolated testing.
         */
        fun buildInMemory(context: Context): PixelMarksDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                PixelMarksDatabase::class.java
            ).build()
        }
    }
}
