package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User

/**
 * Special handler for search results to fix User to Artist conversion issues
 * This class is specifically designed to address ClassCastExceptions that occur
 * when scrolling through search results.
 */
object SearchResultsFixer {

    /**
     * Fix a list of search results, ensuring all User objects are properly converted to Artists
     * @param results List of search result items
     * @return Fixed list with proper type conversions
     */
    @JvmStatic
    fun fixSearchResults(results: List<EchoMediaItem>): List<EchoMediaItem> {
        return results.map { fixSearchResultItem(it) }
    }
    
    /**
     * Fix a shelf of search results
     * @param shelf Shelf containing search results
     * @return Fixed shelf with proper type conversions
     */
    @JvmStatic
    fun fixSearchResultShelf(shelf: Shelf): Shelf {
        return when (shelf) {
            is Shelf.Item -> Shelf.Item(fixSearchResultItem(shelf.media))
            
            is Shelf.Lists.Items -> Shelf.Lists.Items(
                id = shelf.id,
                title = shelf.title,
                subtitle = shelf.subtitle,
                list = shelf.list.map { fixSearchResultItem(it) },
                more = shelf.more
            )
            
            is Shelf.Lists.Tracks -> Shelf.Lists.Tracks(
                id = shelf.id,
                title = shelf.title,
                subtitle = shelf.subtitle,
                list = shelf.list.map { fixTrack(it) },
                more = shelf.more
            )
            
            is Shelf.Lists.Categories -> Shelf.Lists.Categories(
                id = shelf.id,
                title = shelf.title,
                list = shelf.list.map { fixSearchResultCategory(it) },
                more = shelf.more
            )
            
            is Shelf.Category -> Shelf.Category(
                id = shelf.id,
                title = shelf.title,
                feed = shelf.feed
            )
        }
    }
    
    /**
     * Fix a category shelf from search results
     */
    @JvmStatic
    private fun fixSearchResultCategory(category: Shelf.Category): Shelf.Category {
        return category // Categories don't need fixing
    }
    
    /**
     * Fix a single search result item
     */
    @JvmStatic
    fun fixSearchResultItem(item: EchoMediaItem): EchoMediaItem {
        return when (item) {
            is Track -> fixTrack(item)
            is Album -> fixAlbum(item)
            is Playlist -> fixPlaylist(item)
            is Artist -> item
            is User -> ModelTypeHelper.userToArtist(item)
            else -> item
        }
    }
    
    /**
     * Fix a track by ensuring all artists are Artist objects, not User objects
     */
    @JvmStatic
    fun fixTrack(track: Track): Track {
        val fixedArtists = track.artists.map { artist ->
            when (artist) {
                is Artist -> artist
                is User -> ModelTypeHelper.userToArtist(artist)
                else -> throw IllegalArgumentException("Unknown artist type: ${artist.javaClass}")
            }
        }
        
        val fixedAlbum = track.album?.let { fixAlbum(it) }
        
        return track.copy(
            artists = fixedArtists,
            album = fixedAlbum
        )
    }
    
    /**
     * Fix an album by ensuring all artists are Artist objects, not User objects
     */
    @JvmStatic
    fun fixAlbum(album: Album): Album {
        val fixedArtists = album.artists.map { artist ->
            when (artist) {
                is Artist -> artist
                is User -> ModelTypeHelper.userToArtist(artist)
                else -> throw IllegalArgumentException("Unknown artist type: ${artist.javaClass}")
            }
        }
        
        return album.copy(
            artists = fixedArtists
        )
    }
    
    /**
     * Fix a playlist by ensuring all authors are Artist objects, not User objects
     */
    @JvmStatic
    fun fixPlaylist(playlist: Playlist): Playlist {
        val fixedAuthors = playlist.authors.map { author ->
            when (author) {
                is Artist -> author
                is User -> ModelTypeHelper.userToArtist(author)
                else -> throw IllegalArgumentException("Unknown author type: ${author.javaClass}")
            }
        }
        
        return playlist.copy(
            authors = fixedAuthors
        )
    }
}