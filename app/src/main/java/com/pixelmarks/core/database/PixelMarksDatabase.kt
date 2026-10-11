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
    version = 4,
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

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN isTrashed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN trashedAt INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_isTrashed ON bookmarks(isTrashed)")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Add isPrivate to bookmarks
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_isPrivate ON bookmarks(isPrivate)")

                // 2. Add isPrivate to collections
                db.execSQL("ALTER TABLE collections ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_isPrivate ON collections(isPrivate)")

                // 3. Add isPrivate to tags
                db.execSQL("ALTER TABLE tags ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DROP INDEX IF EXISTS index_tags_name")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name_isPrivate ON tags(name, isPrivate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tags_isPrivate ON tags(isPrivate)")

                // 4. Migrate existing hidden collections to isPrivate = 1
                db.execSQL("UPDATE collections SET isPrivate = 1 WHERE isHidden = 1")

                // 5. Migrate bookmarks belonging to those private collections to isPrivate = 1
                db.execSQL("UPDATE bookmarks SET isPrivate = 1 WHERE collectionId IN (SELECT id FROM collections WHERE isPrivate = 1)")

                // 6. Check if any tag is attached ONLY to private bookmarks; if so, mark it isPrivate = 1
                db.execSQL("""
                    UPDATE tags SET isPrivate = 1 
                    WHERE id IN (
                        SELECT tagId FROM bookmark_tag_cross_ref 
                        WHERE bookmarkId IN (SELECT id FROM bookmarks WHERE isPrivate = 1)
                    )
                    AND id NOT IN (
                        SELECT tagId FROM bookmark_tag_cross_ref 
                        WHERE bookmarkId IN (SELECT id FROM bookmarks WHERE isPrivate = 0)
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collections ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE collections ADD COLUMN keywords TEXT")
            }
        }

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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
             .build()
        }

        /**
         * In-memory database builder for isolated testing.
         */
        fun buildInMemory(context: Context): PixelMarksDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                PixelMarksDatabase::class.java
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
             .build()
        }
    }
}
