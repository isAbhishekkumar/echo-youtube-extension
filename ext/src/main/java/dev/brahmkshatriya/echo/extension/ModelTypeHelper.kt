package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User

/**
 * Helper class to ensure safe type conversions between Echo model classes
 * This class is designed to prevent ClassCastExceptions when models are being used across extensions
 */
object ModelTypeHelper {
    /**
     * Safe conversion from User to Artist
     */
    @JvmStatic
    fun userToArtist(user: User): Artist = Artist(
        id = user.id,
        name = user.name,
        cover = user.cover,
        subtitle = user.subtitle,
        extras = user.extras
    )
    
    /**
     * Safe conversion from Artist to User
     */
    @JvmStatic
    fun artistToUser(artist: Artist): User = User(
        id = artist.id,
        name = artist.name,
        cover = artist.cover,
        subtitle = artist.subtitle,
        extras = artist.extras
    )
    
    /**
     * Ensure Artists in Album are proper Artist objects, not Users
     */
    @JvmStatic
    fun ensureProperArtistsInAlbum(album: Album): Album {
        val artists = album.artists.map { 
            if (it !is Artist) userToArtist(it as User) else it
        }
        
        return album.copy(artists = artists)
    }
    
    /**
     * Ensure Artists in Track are proper Artist objects, not Users
     */
    @JvmStatic
    fun ensureProperArtistsInTrack(track: Track): Track {
        val artists = track.artists.map { 
            if (it !is Artist) userToArtist(it as User) else it
        }
        
        return track.copy(artists = artists)
    }
    
    /**
     * Ensure Authors in Playlist are proper Artist objects, not Users
     */
    @JvmStatic
    fun ensureProperAuthorsInPlaylist(playlist: Playlist): Playlist {
        val authors = playlist.authors.map { 
            if (it !is Artist) userToArtist(it as User) else it
        }
        
        return playlist.copy(authors = authors)
    }
}