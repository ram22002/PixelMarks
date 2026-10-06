package com.pixelmarks.app

import android.content.Context
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.network.DefaultMetadataHttpClient
import com.pixelmarks.core.network.MetadataHttpClient
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.BookmarkRepositoryImpl
import com.pixelmarks.data.repository.CollectionRepository
import com.pixelmarks.data.repository.CollectionRepositoryImpl
import com.pixelmarks.data.repository.MetadataRepository
import com.pixelmarks.data.repository.MetadataRepositoryImpl
import com.pixelmarks.data.repository.TagRepository
import com.pixelmarks.data.repository.TagRepositoryImpl
import com.pixelmarks.domain.usecase.AddBookmarkUseCase
import com.pixelmarks.domain.usecase.DeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.FetchBookmarkMetadataUseCase
import com.pixelmarks.domain.usecase.GetBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksUseCase
import coil3.ImageLoader
import com.pixelmarks.core.image.PixelMarksImageLoader
import com.pixelmarks.domain.usecase.UpdateBookmarkUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application dependency container providing singleton instances of repositories and use cases.
 */
interface AppContainer {
    val bookmarkRepository: BookmarkRepository
    val collectionRepository: CollectionRepository
    val tagRepository: TagRepository
    val metadataRepository: MetadataRepository
    val imageLoader: ImageLoader

    val addBookmarkUseCase: AddBookmarkUseCase
    val observeBookmarksUseCase: ObserveBookmarksUseCase
    val getBookmarkUseCase: GetBookmarkUseCase
    val deleteBookmarkUseCase: DeleteBookmarkUseCase
    val updateBookmarkUseCase: UpdateBookmarkUseCase
    val fetchBookmarkMetadataUseCase: FetchBookmarkMetadataUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: PixelMarksDatabase by lazy {
        PixelMarksDatabase.getInstance(context)
    }

    private val metadataHttpClient: MetadataHttpClient by lazy {
        DefaultMetadataHttpClient()
    }

    override val bookmarkRepository: BookmarkRepository by lazy {
        BookmarkRepositoryImpl(database.bookmarkDao())
    }

    override val collectionRepository: CollectionRepository by lazy {
        CollectionRepositoryImpl(database.collectionDao())
    }

    override val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(database.tagDao(), database.bookmarkTagDao())
    }

    override val metadataRepository: MetadataRepository by lazy {
        MetadataRepositoryImpl(metadataHttpClient)
    }

    override val imageLoader: ImageLoader by lazy {
        PixelMarksImageLoader.create(context)
    }

    override val fetchBookmarkMetadataUseCase: FetchBookmarkMetadataUseCase by lazy {
        FetchBookmarkMetadataUseCase(bookmarkRepository, metadataRepository)
    }

    override val addBookmarkUseCase: AddBookmarkUseCase by lazy {
        AddBookmarkUseCase(
            bookmarkRepository = bookmarkRepository,
            fetchBookmarkMetadataUseCase = fetchBookmarkMetadataUseCase,
            coroutineScope = applicationScope
        )
    }

    override val observeBookmarksUseCase: ObserveBookmarksUseCase by lazy {
        ObserveBookmarksUseCase(bookmarkRepository)
    }

    override val getBookmarkUseCase: GetBookmarkUseCase by lazy {
        GetBookmarkUseCase(bookmarkRepository)
    }

    override val deleteBookmarkUseCase: DeleteBookmarkUseCase by lazy {
        DeleteBookmarkUseCase(bookmarkRepository)
    }

    override val updateBookmarkUseCase: UpdateBookmarkUseCase by lazy {
        UpdateBookmarkUseCase(bookmarkRepository)
    }
}
