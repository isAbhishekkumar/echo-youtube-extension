package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
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
 * Extension function to create a Feed<Shelf> from a PagedData<EchoMediaItem>
 * This converts media items to shelf items for proper display
 */
fun PagedData<EchoMediaItem>.toShelfFeed(): Feed<Shelf> {
    return Feed(listOf()) { _ -> 
        Feed.Data(object : PagedData<Shelf>() {
            override fun clear() {
                this@toShelfFeed.clear()
            }
            
            override suspend fun loadAllInternal(): List<Shelf> {
                val items = this@toShelfFeed.loadAll()
                return items.map { Shelf.Item(it) }
            }
            
            override suspend fun loadListInternal(continuation: String?): Page<Shelf> {
                val page = this@toShelfFeed.loadPage(continuation)
                val shelves = page.data.map { item -> Shelf.Item(item) }
                return Page(shelves, page.continuation)
            }
            
            override fun invalidate(continuation: String?) {
                this@toShelfFeed.invalidate(continuation)
            }
            
            override fun <R : Any> map(block: suspend (Result<List<Shelf>>) -> List<R>): PagedData<R> {
                return PagedData.Single { block(runCatching { loadAll() }) }
            }
        })
    }
}

// Extension functions for date conversion moved to Convertors.kt