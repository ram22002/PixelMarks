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
import com.pixelmarks.domain.usecase.CreateCollectionUseCase
import com.pixelmarks.domain.usecase.DeleteCollectionUseCase
import com.pixelmarks.domain.usecase.GetCollectionUseCase
import com.pixelmarks.domain.usecase.MoveBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveCollectionsUseCase
import com.pixelmarks.domain.usecase.UpdateBookmarkUseCase
import com.pixelmarks.domain.usecase.UpdateCollectionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

import com.pixelmarks.domain.usecase.CreateTagUseCase
import com.pixelmarks.domain.usecase.DeleteTagUseCase
import com.pixelmarks.domain.usecase.GetTagUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarkTagsUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksByTagUseCase
import com.pixelmarks.domain.usecase.ObserveTagsUseCase
import com.pixelmarks.domain.usecase.ReplaceBookmarkTagsUseCase
import com.pixelmarks.domain.usecase.UpdateTagUseCase

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

    val observeCollectionsUseCase: ObserveCollectionsUseCase
    val getCollectionUseCase: GetCollectionUseCase
    val createCollectionUseCase: CreateCollectionUseCase
    val updateCollectionUseCase: UpdateCollectionUseCase
    val deleteCollectionUseCase: DeleteCollectionUseCase
    val moveBookmarkUseCase: MoveBookmarkUseCase

    val observeTagsUseCase: ObserveTagsUseCase
    val getTagUseCase: GetTagUseCase
    val createTagUseCase: CreateTagUseCase
    val updateTagUseCase: UpdateTagUseCase
    val deleteTagUseCase: DeleteTagUseCase
    val observeBookmarkTagsUseCase: ObserveBookmarkTagsUseCase
    val observeBookmarksByTagUseCase: ObserveBookmarksByTagUseCase
    val replaceBookmarkTagsUseCase: ReplaceBookmarkTagsUseCase
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
            tagRepository = tagRepository,
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

    override val observeCollectionsUseCase: ObserveCollectionsUseCase by lazy {
        ObserveCollectionsUseCase(collectionRepository)
    }

    override val getCollectionUseCase: GetCollectionUseCase by lazy {
        GetCollectionUseCase(collectionRepository)
    }

    override val createCollectionUseCase: CreateCollectionUseCase by lazy {
        CreateCollectionUseCase(collectionRepository)
    }

    override val updateCollectionUseCase: UpdateCollectionUseCase by lazy {
        UpdateCollectionUseCase(collectionRepository)
    }

    override val deleteCollectionUseCase: DeleteCollectionUseCase by lazy {
        DeleteCollectionUseCase(collectionRepository, bookmarkRepository)
    }

    override val moveBookmarkUseCase: MoveBookmarkUseCase by lazy {
        MoveBookmarkUseCase(bookmarkRepository)
    }

    override val observeTagsUseCase: ObserveTagsUseCase by lazy {
        ObserveTagsUseCase(tagRepository)
    }

    override val getTagUseCase: GetTagUseCase by lazy {
        GetTagUseCase(tagRepository)
    }

    override val createTagUseCase: CreateTagUseCase by lazy {
        CreateTagUseCase(tagRepository)
    }

    override val updateTagUseCase: UpdateTagUseCase by lazy {
        UpdateTagUseCase(tagRepository)
    }

    override val deleteTagUseCase: DeleteTagUseCase by lazy {
        DeleteTagUseCase(tagRepository)
    }

    override val observeBookmarkTagsUseCase: ObserveBookmarkTagsUseCase by lazy {
        ObserveBookmarkTagsUseCase(tagRepository)
    }

    override val observeBookmarksByTagUseCase: ObserveBookmarksByTagUseCase by lazy {
        ObserveBookmarksByTagUseCase(tagRepository)
    }

    override val replaceBookmarkTagsUseCase: ReplaceBookmarkTagsUseCase by lazy {
        ReplaceBookmarkTagsUseCase(tagRepository)
    }
}
