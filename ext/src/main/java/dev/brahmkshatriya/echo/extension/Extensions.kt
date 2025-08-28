package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.NetworkRequest
import dev.brahmkshatriya.echo.common.models.Shelf

/**
 * Extension function to create a Feed from a PagedData
 */
fun <T : Any> PagedData<T>.toFeed(): Feed<T> {
    return Feed(listOf()) { _ -> Feed.Data(this) }
}

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
    val feedData = this.getPagedData(null)
    val pagedData = feedData.pagedData
    val page = pagedData.load(null)
    return page.data
}

/**
 * Extension function to create a Feed<Shelf> from a PagedData<EchoMediaItem>
 * This converts media items to shelf items for proper display
 */
fun PagedData<EchoMediaItem>.toShelfFeed(): Feed<Shelf> {
    return Feed(listOf()) { _ -> 
        Feed.Data(object : PagedData<Shelf> {
            override suspend fun load(continuation: String?): PagedData.Page<Shelf> {
                val page = this@toShelfFeed.load(continuation)
                val shelves = page.data.map { item ->
                    Shelf.Item(item)
                }
                return PagedData.Page(shelves, page.continuation)
            }
        })
    }
}

/**
 * Extension function to check if a string contains a timestamp
 */
fun String.containsTimestamp(): Boolean {
    return this.contains(Regex("\\d{10,}"))
}

/**
 * Extension function to create a Date object from a string year
 */
fun String.toDate(): dev.brahmkshatriya.echo.common.models.Date = dev.brahmkshatriya.echo.common.models.Date(this.toInt())

/**
 * Extension function to create a Date from a string containing a timestamp
 */
fun String.toDateFromTimestamp(): dev.brahmkshatriya.echo.common.models.Date = dev.brahmkshatriya.echo.common.models.Date(this.toLong())