package com.pixelmarks.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private val testDbName = "migration_test.db"
    private lateinit var context: Context
    private var database: PixelMarksDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(testDbName)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(testDbName)
    }

    @Test
    fun testMigration1To2PreservesDataAndAddsTrashColumns() = runBlocking {
        // Step 1: Create Version 1 database manually with SQLite OpenHelper
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(testDbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createV1Schema(db)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v1Db = helper.writableDatabase

        // Insert V1 sample data
        val now = 1700000000000L
        v1Db.execSQL(
            """
            INSERT INTO bookmarks (
                id, url, normalizedUrl, title, description, domain,
                faviconUrl, thumbnailUrl, collectionId, isFavorite, isPinned,
                isArchived, notes, createdAt, updatedAt, lastOpenedAt,
                visitCount, reminderAt, metadataStatus, metadataUpdatedAt
            ) VALUES (
                1, 'https://example.com/one', 'https://example.com/one', 'Example One', 'Desc 1', 'example.com',
                'https://example.com/fav.png', 'https://example.com/thumb.png', NULL, 1, 0,
                0, 'Test Note', $now, $now, NULL,
                5, NULL, 'SUCCESS', $now
            )
            """.trimIndent()
        )
        v1Db.execSQL(
            """
            INSERT INTO bookmarks (
                id, url, normalizedUrl, title, description, domain,
                faviconUrl, thumbnailUrl, collectionId, isFavorite, isPinned,
                isArchived, notes, createdAt, updatedAt, lastOpenedAt,
                visitCount, reminderAt, metadataStatus, metadataUpdatedAt
            ) VALUES (
                2, 'https://archive.org/page', 'https://archive.org/page', 'Archive Page', NULL, 'archive.org',
                NULL, NULL, NULL, 0, 1,
                1, NULL, $now, $now, NULL,
                0, NULL, 'PENDING', NULL
            )
            """.trimIndent()
        )
        v1Db.close()

        // Step 2: Open with Room v4 using migrations
        database = Room.databaseBuilder(
            context,
            PixelMarksDatabase::class.java,
            testDbName
        ).addMigrations(
            PixelMarksDatabase.MIGRATION_1_2,
            PixelMarksDatabase.MIGRATION_2_3,
            PixelMarksDatabase.MIGRATION_3_4
        ).build()

        val dao = database!!.bookmarkDao()

        // Step 3: Verify existing data in Room V2/V4
        val item1 = dao.getById(1L)
        assertNotNull(item1)
        assertEquals("Example One", item1?.title)
        assertEquals("https://example.com/one", item1?.url)
        assertEquals(true, item1?.isFavorite)
        assertEquals(false, item1?.isPinned)
        assertEquals(false, item1?.isArchived)
        assertEquals(false, item1?.isTrashed) // Defaulted to false
        assertNull(item1?.trashedAt) // Defaulted to null
        assertEquals(5, item1?.visitCount)

        val item2 = dao.getById(2L)
        assertNotNull(item2)
        assertEquals("Archive Page", item2?.title)
        assertEquals(true, item2?.isPinned)
        assertEquals(true, item2?.isArchived)
        assertEquals(false, item2?.isTrashed)
        assertNull(item2?.trashedAt)

        // Step 4: Verify active bookmarks exclude archived and trashed
        val activeBookmarks = dao.observeAll().first()
        assertEquals(1, activeBookmarks.size)
        assertEquals(1L, activeBookmarks.first().id)

        // Step 5: Test moving item1 to trash
        dao.moveToTrash(1L, trashedAt = now + 1000)
        val trashedItem = dao.getById(1L)
        assertTrue(trashedItem!!.isTrashed)
        assertEquals(now + 1000, trashedItem.trashedAt)
        // Verify all other fields still intact
        assertEquals("Example One", trashedItem.title)
        assertTrue(trashedItem.isFavorite)

        // Active should now be empty
        assertEquals(0, dao.observeAll().first().size)

        // Trash should have item 1
        val trashedList = dao.observeTrashed().first()
        assertEquals(1, trashedList.size)
        assertEquals(1L, trashedList.first().id)

        // Restore item 1
        dao.restoreFromTrash(1L)
        val restoredItem = dao.getById(1L)
        assertFalse(restoredItem!!.isTrashed)
        assertNull(restoredItem.trashedAt)
        assertEquals("Example One", restoredItem.title)
        assertTrue(restoredItem.isFavorite)

        // Active should have item 1 again
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun testMigration2To3PreservesDataAndAddsIsPrivateColumns() = runBlocking {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(testDbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createV1Schema(db)
                    PixelMarksDatabase.MIGRATION_1_2.migrate(db)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v2Db = helper.writableDatabase

        val now = 1700000000000L
        v2Db.execSQL(
            """
            INSERT INTO bookmarks (
                id, url, normalizedUrl, title, description, domain,
                faviconUrl, thumbnailUrl, collectionId, isFavorite, isPinned,
                isArchived, isTrashed, trashedAt, notes, createdAt, updatedAt, lastOpenedAt,
                visitCount, reminderAt, metadataStatus, metadataUpdatedAt
            ) VALUES (
                10, 'https://v2test.com', 'https://v2test.com', 'V2 Test Bookmark', NULL, 'v2test.com',
                NULL, NULL, NULL, 0, 0,
                0, 0, NULL, NULL, $now, $now, NULL,
                1, NULL, 'SUCCESS', $now
            )
            """.trimIndent()
        )
        v2Db.close()

        database = Room.databaseBuilder(
            context,
            PixelMarksDatabase::class.java,
            testDbName
        ).addMigrations(
            PixelMarksDatabase.MIGRATION_2_3,
            PixelMarksDatabase.MIGRATION_3_4
        ).build()

        val dao = database!!.bookmarkDao()
        val item = dao.getById(10L)
        assertNotNull(item)
        assertEquals("V2 Test Bookmark", item?.title)
        assertFalse(item!!.isPrivate)
    }

    @Test
    fun testMigration3To4PreservesDataAndAddsCollectionDescriptionAndKeywords() = runBlocking {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(testDbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createV1Schema(db)
                    PixelMarksDatabase.MIGRATION_1_2.migrate(db)
                    PixelMarksDatabase.MIGRATION_2_3.migrate(db)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v3Db = helper.writableDatabase

        val now = 1700000000000L
        v3Db.execSQL(
            """
            INSERT INTO collections (
                id, name, parentId, icon, color, position, isLocked, isHidden,
                isDomain, domain, createdAt, updatedAt
            ) VALUES (
                5, 'Architecture', NULL, 'folder', '#C5A059', 0, 0, 0,
                0, NULL, $now, $now
            )
            """.trimIndent()
        )
        v3Db.close()

        database = Room.databaseBuilder(
            context,
            PixelMarksDatabase::class.java,
            testDbName
        ).addMigrations(
            PixelMarksDatabase.MIGRATION_3_4
        ).build()

        val collectionDao = database!!.collectionDao()
        val collection = collectionDao.getById(5L)
        assertNotNull(collection)
        assertEquals("Architecture", collection?.name)
        assertNull(collection?.description)
        assertNull(collection?.keywords)

        // Update with description and keywords
        collectionDao.update(
            collection!!.copy(
                description = "Modern and archival architecture folios",
                keywords = "design, blueprints, archival"
            )
        )
        val updated = collectionDao.getById(5L)
        assertEquals("Modern and archival architecture folios", updated?.description)
        assertEquals("design, blueprints, archival", updated?.keywords)
    }

    private fun createV1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `collections` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `parentId` INTEGER,
                `icon` TEXT,
                `color` TEXT,
                `position` INTEGER NOT NULL,
                `isLocked` INTEGER NOT NULL,
                `isHidden` INTEGER NOT NULL,
                `isDomain` INTEGER NOT NULL,
                `domain` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`parentId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_collections_parentId` ON `collections` (`parentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_collections_name` ON `collections` (`name`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bookmarks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `url` TEXT NOT NULL,
                `normalizedUrl` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `domain` TEXT NOT NULL,
                `faviconUrl` TEXT,
                `thumbnailUrl` TEXT,
                `collectionId` INTEGER,
                `isFavorite` INTEGER NOT NULL,
                `isPinned` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `lastOpenedAt` INTEGER,
                `visitCount` INTEGER NOT NULL,
                `reminderAt` INTEGER,
                `metadataStatus` TEXT NOT NULL,
                `metadataUpdatedAt` INTEGER,
                FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_normalizedUrl` ON `bookmarks` (`normalizedUrl`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_collectionId` ON `bookmarks` (`collectionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_domain` ON `bookmarks` (`domain`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_createdAt` ON `bookmarks` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_updatedAt` ON `bookmarks` (`updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_isFavorite` ON `bookmarks` (`isFavorite`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_isPinned` ON `bookmarks` (`isPinned`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_isArchived` ON `bookmarks` (`isArchived`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tags` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `color` TEXT,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bookmark_tag_cross_ref` (
                `bookmarkId` INTEGER NOT NULL,
                `tagId` INTEGER NOT NULL,
                PRIMARY KEY(`bookmarkId`, `tagId`),
                FOREIGN KEY(`bookmarkId`) REFERENCES `bookmarks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmark_tag_cross_ref_tagId` ON `bookmark_tag_cross_ref` (`tagId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `bookmarkId` INTEGER NOT NULL,
                `reminderAt` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                FOREIGN KEY(`bookmarkId`) REFERENCES `bookmarks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_bookmarkId` ON `reminders` (`bookmarkId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_reminderAt` ON `reminders` (`reminderAt`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `connected_devices` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `deviceName` TEXT NOT NULL,
                `deviceType` TEXT NOT NULL,
                `tokenHash` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `lastSeenAt` INTEGER NOT NULL,
                `revokedAt` INTEGER,
                `isBlocked` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_connected_devices_tokenHash` ON `connected_devices` (`tokenHash`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `backup_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL,
                `location` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `sizeBytes` INTEGER NOT NULL,
                `checksum` TEXT NOT NULL,
                `status` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_backup_records_createdAt` ON `backup_records` (`createdAt`)")
    }
}
