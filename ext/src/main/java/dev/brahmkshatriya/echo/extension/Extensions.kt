package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.loadAll
import dev.brahmkshatriya.echo.common.models.NetworkRequest
import dev.brahmkshatriya.echo.common.models.Shelf

/**
 * Extension function to convert String to NetworkRequest
 */
fun String.toRequest(headers: Map<String, String> = emptyMap()): NetworkRequest {
    return NetworkRequest(this, headers)
}

/**
 * Extension function to load all items in a Feed
 */
suspend fun <T : Any> Feed<T>.load(): List<T> {
    return Feed.Companion.loadAll(this)
}

/**
 * A PagedData implementation that converts EchoMediaItems to Shelves
 */
private class ShelfPagedData(private val original: PagedData<EchoMediaItem>) : PagedData<Shelf>() {
    override fun clear() {
        original.clear()
    }
    
    override suspend fun loadAllInternal(): List<Shelf> {
        val items = original.loadAll()
        return items.map { Shelf.Item(it) }
    }
    
    override suspend fun loadListInternal(continuation: String?): Page<Shelf> {
        val page = original.loadPage(continuation)
        val shelves = page.data.map { item -> Shelf.Item(item) }
        return Page(shelves, page.continuation)
    }
    
    override fun invalidate(continuation: String?) {
        original.invalidate(continuation)
    }
    
    override fun <R : Any> map(block: suspend (Result<List<Shelf>>) -> List<R>): PagedData<R> {
        return PagedData.Single { block(runCatching { loadAll() }) }
    }
}

/**
 * Extension function to create a Feed<Shelf> from a PagedData<EchoMediaItem>
 * This converts media items to shelf items for proper display
 */
fun PagedData<EchoMediaItem>.toShelfFeed(): Feed<Shelf> {
    return Feed(listOf()) { _ -> 
        Feed.Data(ShelfPagedData(this))
    }
}

// Extension functions for date conversion moved to Convertors.kt