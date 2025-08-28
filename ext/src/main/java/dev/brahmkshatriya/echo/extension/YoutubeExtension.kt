package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.QuickSearchClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.clients.TrackerMarkClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.helpers.WebViewRequest
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.loadAll
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.NetworkRequest
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Companion.toGetRequest
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toMedia
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.endpoints.EchoArtistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoArtistMoreEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoEditPlaylistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoLibraryEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoLyricsEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoPlaylistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSearchEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSearchSuggestionsEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongEndPoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongFeedEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoSongRelatedEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoVideoEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.EchoVisitorEndpoint
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import dev.toastbits.ytmkt.model.external.PlaylistEditor
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.HIGH
import dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.LOW
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.http.headers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import java.security.MessageDigest

// Extension function to load Feed data
suspend fun <T : Any> Feed<T>.load(): List<T> = this.loadAll()

class YoutubeExtension : ExtensionClient,
    SearchFeedClient,
    TrackClient,
    AlbumClient,
    ArtistClient,
    PlaylistClient,
    QuickSearchClient,
    HomeFeedClient,
    RadioClient,
    LyricsClient,
    LyricsSearchClient,
    LoginClient,
    LibraryFeedClient,
    ShareClient,
    PlaylistEditClient,
    FollowClient,
    LikeClient,
    TrackerClient,
    TrackerMarkClient {

    companion object {
        const val WEB_VERSION = "1.20240620.1.0"
        const val ID = "youtube"

        val SINGLES = mapOf("en-GB" to "Singles", "en" to "Singles")
        val ENGLISH = "en-GB"
    }

    private var language = "en-GB"
    private val thumbnailQuality = HIGH

    lateinit var api: YoutubeiApi

    private val trackMap = mutableMapOf<String, PagedData<Track>>()

    private lateinit var artistEndPoint: EchoArtistEndpoint
    private lateinit var artistMoreEndPoint: EchoArtistMoreEndpoint
    private lateinit var editPlaylistEndpoint: EchoEditPlaylistEndpoint
    private lateinit var libraryEndPoint: EchoLibraryEndPoint
    private lateinit var lyricsEndPoint: EchoLyricsEndPoint
    private lateinit var playlistEndPoint: EchoPlaylistEndpoint
    private lateinit var searchEndpoint: EchoSearchEndpoint
    private lateinit var searchSuggestionsEndpoint: EchoSearchSuggestionsEndpoint
    private lateinit var songEndPoint: EchoSongEndPoint
    private lateinit var songFeedEndpoint: EchoSongFeedEndpoint
    private lateinit var songRelatedEndpoint: EchoSongRelatedEndpoint
    private lateinit var videoEndpoint: EchoVideoEndpoint
    private lateinit var mobileVideoEndpoint: EchoVideoEndpoint
    private lateinit var visitorEndpoint: EchoVisitorEndpoint

    private var showVideos = false
    private var preferVideos = false

    private val settings = Settings(
        id = "youtube_settings",
        title = "YouTube Music Settings",
        description = "Configure YouTube Music extension settings",
        categories = listOf(
            Settings.Category(
                id = "streaming",
                title = "Streaming Settings",
                description = "Configure streaming preferences",
                settings = listOf(
                    SettingSwitch(
                        id = "show_videos",
                        title = "Enable Video Streaming",
                        description = "Allow playback of music videos (may use more bandwidth)",
                        defaultValue = false,
                        saveAction = { value -> showVideos = value }
                    ),
                    SettingSwitch(
                        id = "prefer_videos",
                        title = "Prefer Video Streams",
                        description = "Default to video when available (requires video streaming enabled)",
                        defaultValue = false,
                        saveAction = { value -> preferVideos = value }
                    )
                )
            ),
            Settings.Category(
                id = "language",
                title = "Language Settings",
                description = "Configure language preferences",
                settings = listOf(
                    Setting.Select(
                        id = "language",
                        title = "Language",
                        description = "Select your preferred language",
                        options = listOf(
                            Setting.Select.Option("en-GB", "English (UK)"),
                            Setting.Select.Option("en", "English (US)"),
                            Setting.Select.Option("es", "Español"),
                            Setting.Select.Option("fr", "Français"),
                            Setting.Select.Option("de", "Deutsch"),
                            Setting.Select.Option("it", "Italiano"),
                            Setting.Select.Option("pt", "Português"),
                            Setting.Select.Option("ru", "Русский"),
                            Setting.Select.Option("ja", "日本語"),
                            Setting.Select.Option("ko", "한국어"),
                            Setting.Select.Option("zh", "中文"),
                            Setting.Select.Option("ar", "العربية"),
                            Setting.Select.Option("hi", "हिन्दी")
                        ),
                        defaultValue = "en-GB",
                        saveAction = { value -> language = value }
                    )
                )
            )
        )
    )

    override val defaultSettings = settings

    override fun onLoad() {
        api = YoutubeiApi(WEB_VERSION)
        artistEndPoint = EchoArtistEndpoint(api)
        artistMoreEndPoint = EchoArtistMoreEndpoint(api)
        editPlaylistEndpoint = EchoEditPlaylistEndpoint(api)
        libraryEndPoint = EchoLibraryEndPoint(api)
        lyricsEndPoint = EchoLyricsEndPoint()
        playlistEndPoint = EchoPlaylistEndpoint(api)
        searchEndpoint = EchoSearchEndpoint(api)
        searchSuggestionsEndpoint = EchoSearchSuggestionsEndpoint()
        songEndPoint = EchoSongEndPoint(api)
        songFeedEndpoint = EchoSongFeedEndpoint(api)
        songRelatedEndpoint = EchoSongRelatedEndpoint(api)
        videoEndpoint = EchoVideoEndpoint(api, false)
        mobileVideoEndpoint = EchoVideoEndpoint(api, true)
        visitorEndpoint = EchoVisitorEndpoint()
    }

    override fun onDestroy() {
    }

    private suspend fun <T> withVisitorId(
        maxRetries: Int = 3,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            if (attempt > 0) {
                // Reset visitor ID on retry
                api.visitor_id = null
            }
            ensureVisitorId()
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (e !is ConnectTimeoutException && e !is ClientRequestException) {
                    throw e
                }
                println("DEBUG: Request failed, retrying with new visitor ID: ${e.message}")
            }
        }
        throw lastException ?: Exception("Failed after $maxRetries attempts")
    }

    private suspend fun ensureVisitorId() {
        if (api.visitor_id == null) {
            api.visitor_id = visitorEndpoint.getVisitorId()
            println("DEBUG: Set visitor ID: ${api.visitor_id}")
        }
    }

    override suspend fun search(query: String): Feed<Shelf> {
        return withVisitorId {
            val (shelves, searches) = searchEndpoint.search(query, language, thumbnailQuality)
            Feed(searches) { tab ->
                val searchQuery = tab?.id ?: query
                val pagedData = PagedData.Continuous<Shelf> { continuation ->
                    val (data, cont) = searchEndpoint.searchContinuation(
                        searchQuery, continuation, language, thumbnailQuality
                    )
                    Page(data, cont)
                }
                Feed.Data(pagedData)
            }.apply { setExtra("shelves", shelves) }
        }
    }

    override suspend fun getAutocompleteSuggestions(query: String): List<String> {
        val results = try {
            searchSuggestionsEndpoint.getSearchSuggestions(query)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        return results
    }

    private fun getBestAudioSource(sources: List<Streamable.Source>, networkType: String): Streamable.Source? {
        // Sort audio sources by quality and select best one based on network type
        val sortedSources = sources.sortedByDescending { it.quality ?: 0 }
        
        // If on cellular, prefer medium quality to save data
        if (networkType == "cellular" && sortedSources.size > 1) {
            val midIndex = sortedSources.size / 2
            return sortedSources[midIndex]
        }
        
        // Otherwise return highest quality
        return sortedSources.firstOrNull()
    }
    
    private fun getBestVideoSourceByQuality(
        sources: List<Streamable.Source>,
        targetQuality: Int?
    ): Streamable.Source? {
        if (sources.isEmpty()) return null
        
        // If target quality specified, find closest match
        if (targetQuality != null) {
            // Find source with closest match to target quality
            return sources.minByOrNull { 
                Math.abs((it.quality ?: 0) - targetQuality) 
            }
        }
        
        // Otherwise return highest quality
        return sources.maxByOrNull { it.quality ?: 0 }
    }
    
    private fun getTargetVideoQuality(streamable: Streamable): Int? {
        return streamable.extras["preferredQuality"]?.toIntOrNull()
    }
    
    private fun detectNetworkType(): String {
        // In a real implementation, this would detect network type
        // For now, always assume WiFi
        return "wifi"
    }
    
    private fun getStrategyForNetwork(attempt: Int, networkType: String): String {
        return when {
            attempt == 1 -> "standard"
            attempt == 2 -> "reset_visitor"
            attempt == 3 -> "mobile_emulation"
            attempt == 4 -> "aggressive_mobile"
            else -> "desktop_fallback"
        }
    }
    
    private suspend fun handleMPDStream(
        mpdUrl: String, 
        strategy: String,
        networkType: String
    ): Streamable.Media {
        // In this simplified version, we'll just create an MPD source
        val source = Streamable.Source.Dash(
            NetworkRequest(mpdUrl, emptyMap())
        )
        return Streamable.Media.Server(listOf(source), false)
    }

    override suspend fun getStream(streamable: Streamable): Streamable.Media {
        println("DEBUG: Getting stream for type: ${streamable.type}")
        val audioOnly = !showVideos || (streamable.type == "AUDIO")
        
        return when (streamable.type) {
            "AUDIO", "VIDEO_MP4", "VIDEO_WEBM" -> {
                withVisitorId {
                    try {
                        val videoId = streamable.extras["videoId"] ?: streamable.id
                        println("DEBUG: Loading stream for videoId: $videoId")
                        
                        val networkType = detectNetworkType()
                        println("DEBUG: Detected network type: $networkType")
                        
                        // Track audio and video sources separately
                        val audioSources = mutableListOf<Streamable.Source>()
                        val videoSources = mutableListOf<Streamable.Source>()
                        
                        // Get different formats based on strategy
                        val strategy = "standard"
                        val currentVideoEndpoint = if (strategy == "mobile_emulation") {
                            mobileVideoEndpoint
                        } else {
                            videoEndpoint
                        }
                        
                        val (video, _) = currentVideoEndpoint.getVideo(false, videoId)
                        
                        // Process adaptive formats
                        video.streamingData.adaptiveFormats.forEach { format ->
                            val freshUrl = format.url ?: return@forEach
                            val mimeType = format.mimeType ?: return@forEach
                            
                            if (freshUrl.isNotEmpty()) {
                                val qualityValue = format.qualityLabel?.let {
                                    it.replace("p", "").toIntOrNull()
                                } ?: format.audioQuality?.let {
                                    when (it) {
                                        "AUDIO_QUALITY_LOW" -> 96
                                        "AUDIO_QUALITY_MEDIUM" -> 128
                                        "AUDIO_QUALITY_HIGH" -> 256
                                        else -> 96
                                    }
                                } ?: 0
                                
                                val videoSource = when {
                                    mimeType.startsWith("audio/") -> {
                                        Streamable.Source.Http(
                                            NetworkRequest(freshUrl, emptyMap()),
                                            quality = qualityValue
                                        )
                                    }
                                    else -> {
                                        Streamable.Source.Http(
                                            NetworkRequest(freshUrl, emptyMap()),
                                            quality = qualityValue
                                        )
                                    }
                                }
                                
                                if (mimeType.startsWith("audio/")) {
                                    audioSources.add(videoSource)
                                    println("DEBUG: Added audio source (quality: $qualityValue, mimeType: $mimeType)")
                                } else if (!audioOnly) {
                                    videoSources.add(videoSource)
                                    println("DEBUG: Added video source (quality: $qualityValue, mimeType: $mimeType)")
                                }
                            }
                        }
                        
                        val mpdUrl = try {
                            video.streamingData.javaClass.getDeclaredField("dashManifestUrl").let { field ->
                                field.isAccessible = true
                                field.get(video.streamingData) as? String
                            }
                        } catch (e: Exception) {
                            null
                        }
                        
                        if (mpdUrl != null && showVideos) {
                            println("DEBUG: Found MPD stream URL: $mpdUrl")
                            val mpdMedia = handleMPDStream(mpdUrl, strategy, networkType)
                            return@withVisitorId mpdMedia
                        }
                        
                        val targetQuality = getTargetVideoQuality(streamable)
                        println("DEBUG: Target video quality: ${targetQuality ?: "any"}")
                        
                        val resultMedia = when {
                            preferVideos && videoSources.isNotEmpty() && audioSources.isNotEmpty() -> {
                                println("DEBUG: Creating merged audio+video stream")
                                val bestAudioSource = getBestAudioSource(audioSources, networkType)
                                val bestVideoSource = getBestVideoSourceByQuality(videoSources, targetQuality)
                                
                                if (bestAudioSource != null && bestVideoSource != null) {
                                    Streamable.Media.Server(
                                        sources = listOf(bestAudioSource, bestVideoSource),
                                        merged = true
                                    )
                                } else {
                                    val fallbackAudioSource = getBestAudioSource(audioSources, networkType)
                                    if (fallbackAudioSource != null) {
                                        Streamable.Media.Server(listOf(fallbackAudioSource), false)
                                    } else {
                                        throw Exception("No valid audio sources found")
                                    }
                                }
                            }
                            audioSources.isNotEmpty() -> {
                                println("DEBUG: Creating audio-only stream")
                                val bestAudioSource = getBestAudioSource(audioSources, networkType)
                                if (bestAudioSource != null) {
                                    Streamable.Media.Server(listOf(bestAudioSource), false)
                                } else {
                                    throw Exception("No valid audio sources found")
                                }
                            }
                            else -> throw Exception("No playable streams found")
                        }
                        
                        resultMedia
                    } catch (e: Exception) {
                        println("DEBUG: Stream retrieval error: ${e.message}")
                        throw e
                    }
                }
            }
            else -> throw Exception("Unsupported stream type: ${streamable.type}")
        }
    }

    override suspend fun loadTrack(track: Track, withDetails: Boolean): Track {
        return withVisitorId {
            val ytmSong = songEndPoint.loadFromTrack(track.id, thumbnailQuality)
            ytmSong.toTrack(thumbnailQuality)
        }
    }

    override suspend fun loadLyrics(track: Track): Lyrics {
        return lyricsEndPoint.getLyrics(track.title, track.artists.joinToString { it.name })
    }

    override suspend fun searchLyrics(query: String): List<Lyrics> {
        return lyricsEndPoint.searchLyrics(query)
    }

    override suspend fun loadAlbum(album: Album): Album {
        return withVisitorId {
            val (ytmAlbum, tracks) = playlistEndPoint.loadFromPlaylist(
                album.id,
                null,
                thumbnailQuality
            )
            trackMap[ytmAlbum.id] = tracks
            ytmAlbum.toAlbum(false, thumbnailQuality)
        }
    }

    override suspend fun loadTracks(album: Album): Feed<Track> {
        return trackMap[album.id]?.toFeed() ?: listOf<Track>().toFeed()
    }

    private suspend fun getArtistMediaItems(artist: Artist): List<Shelf> {
        val data = artistMoreEndPoint.loadFromArtist(artist.id, language).getOrThrow()
        return data.mapNotNull { it.toShelf(api, language, thumbnailQuality) }
    }

    override suspend fun followArtist(artist: Artist, follow: Boolean) {
        // Implement follow functionality
        withUserAuth {
            val subId = artist.extras["subId"] ?: throw Exception("No subId found")
            it.SetSubscribedToArtist.setSubscribedToArtist(artist.id, follow, subId).getOrThrow()
        }
    }

    private var loadedArtist: YtmArtist? = null
    override suspend fun loadArtist(artist: Artist): Artist {
        val result = artistEndPoint.loadArtist(artist.id)
        loadedArtist = result
        return result.toArtist(HIGH)
    }

    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? {
        val cont = playlist.extras["relatedId"] ?: throw Exception("No related id found.")
        val shelves = if (cont.startsWith("id://")) {
            val id = cont.substring(5)
            val track = Track(id, "")
            val loadedTrack = loadTrack(track, false)
            val feed = loadFeed(loadedTrack)
            coroutineScope { 
                feed?.getPagedData(null)?.pagedData?.let {
                    val page = it.load(null)
                    page.data.filterIsInstance<Shelf.Category>()
                } ?: emptyList()
            }
        } else {
            val continuation = songRelatedEndpoint.loadFromPlaylist(cont).getOrThrow()
            continuation.map { it.toShelf(api, language, thumbnailQuality) }
        }
        return Feed(emptyList()) { _ -> Feed.Data(PagedData.Single { shelves }) }
    }


    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        val (ytmPlaylist, related, data) = playlistEndPoint.loadFromPlaylist(
            playlist.id,
            null,
            thumbnailQuality
        )
        trackMap[ytmPlaylist.id] = data
        return ytmPlaylist.toPlaylist(HIGH, related)
    }

    override suspend fun loadTracks(playlist: Playlist): Feed<Track> = trackMap[playlist.id]?.toFeed() ?: listOf<Track>().toFeed()


    override val webViewRequest = object : WebViewRequest.Cookie<List<User>> {
        override val initialUrl =
            "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin".toGetRequest()
        override val stopUrlRegex = "https://music\\.youtube\\.com/.*".toRegex()
        override suspend fun onStop(url: NetworkRequest, cookie: String): List<User> {
            if (!cookie.contains("SAPISID")) throw Exception("Login Failed, could not load SAPISID")
            val auth = run {
                val currentTime = System.currentTimeMillis() / 1000
                val id = cookie.split("SAPISID=")[1].split(";")[0]
                val str = "$currentTime $id https://music.youtube.com"
                val idHash = MessageDigest.getInstance("SHA-1").digest(str.toByteArray())
                    .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
                "SAPISIDHASH ${currentTime}_${idHash}"
            }
            val headersMap = mutableMapOf("cookie" to cookie, "authorization" to auth)
            val headers = headers { headersMap.forEach { (t, u) -> append(t, u) } }
            return api.client.request("https://music.youtube.com/getAccountSwitcherEndpoint") {
                headers {
                    append("referer", "https://music.youtube.com/")
                    appendAll(headers)
                }
            }.getUsers(cookie, auth)
        }
    }

    // Implement LoginClient methods
    override suspend fun onSetLoginUser(user: User?) {
        if (user == null) {
            api.user_auth_state = null
        } else {
            val cookie = user.extras["cookie"] ?: throw Exception("No cookie")
            val auth = user.extras["auth"] ?: throw Exception("No auth")

            val headers = headers {
                append("cookie", cookie)
                append("authorization", auth)
            }
            val authenticationState =
                YoutubeiAuthenticationState(api, headers, user.id.ifEmpty { null })
            api.user_auth_state = authenticationState
        }
        api.visitor_id = visitorEndpoint.getVisitorId()
    }

    override suspend fun getCurrentUser(): User? {
        val headers = api.user_auth_state?.headers ?: return null
        return api.client.request("https://music.youtube.com/getAccountSwitcherEndpoint") {
            headers {
                append("referer", "https://music.youtube.com/")
                appendAll(headers)
            }
        }.getUsers("", "").firstOrNull()
    }

    // Implement TrackerMarkClient methods
    override val markAsPlayedDuration: Long = 30000L

    override suspend fun onMarkAsPlayed(details: TrackDetails) {
        api.user_auth_state?.MarkSongAsWatched?.markSongAsWatched(details.track.id)?.getOrThrow()
    }

    private suspend fun <T> withUserAuth(
        block: suspend (auth: YoutubeiAuthenticationState) -> T
    ): T {
        val state = api.user_auth_state
            ?: throw ClientException.LoginRequired()
        return runCatching { block(state) }.getOrElse {
            if (it is ClientRequestException) {
                if (it.response.status.value == 401) {
                    val user = state.own_channel_id
                        ?: throw ClientException.LoginRequired()
                    throw ClientException.Unauthorized(user)
                }
            }
            throw it
        }
    }

    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        val tabs = listOf(
            Tab("FEmusic_library_landing", "All"),
            Tab("FEmusic_history", "History"),
            Tab("FEmusic_liked_playlists", "Playlists"),
//            Tab("FEmusic_listening_review", "Review"),
            Tab("FEmusic_liked_videos", "Songs"),
            Tab("FEmusic_library_corpus_track_artists", "Artists")
        )
        
        return Feed(tabs) { tab ->
            val pagedData = PagedData.Continuous<Shelf> { cont ->
                val browseId = tab?.id ?: "FEmusic_library_landing"
                val (result, ctoken) = withUserAuth { libraryEndPoint.loadLibraryFeed(browseId, cont) }
                val data = result.mapNotNull { playlist ->
                    playlist.toEchoMediaItem(false, thumbnailQuality)?.toShelf()
                }
                Page(data, ctoken)
            }
            Feed.Data(pagedData)
        }
    }

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        val playlistId = withUserAuth {
            it.CreateAccountPlaylist
                .createAccountPlaylist(title, description ?: "")
                .getOrThrow()
        }
        return loadPlaylist(Playlist(playlistId, "", true))
    }

    override suspend fun deletePlaylist(playlist: Playlist) = withUserAuth {
        it.DeleteAccountPlaylist.deleteAccountPlaylist(playlist.id).getOrThrow()
    }

    // Implement LikeClient methods
    override suspend fun likeTrack(track: Track, isLiked: Boolean) {
        val likeStatus = if (isLiked) SongLikedStatus.LIKED else SongLikedStatus.NEUTRAL
        withUserAuth { it.SetSongLiked.setSongLiked(track.id, likeStatus).getOrThrow() }
    }

    override suspend fun listEditablePlaylists(track: Track?): List<Pair<Playlist, Boolean>> =
        withUserAuth { auth ->
            auth.AccountPlaylists.getAccountPlaylists().getOrThrow().mapNotNull {
                if (it.id != "VLSE") it.toPlaylist(thumbnailQuality) to false
                else null
            }
        }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist,
        title: String?,
        description: String?
    ): Playlist {
        withUserAuth {
            editPlaylistEndpoint.updatePlaylist(
                playlist.id,
                title ?: playlist.title,
                description
            )
        }
        return loadPlaylist(playlist)
    }

    override suspend fun addToPlaylist(
        playlist: Playlist, position: Int, vararg tracks: Track
    ): Boolean {
        return withUserAuth { auth ->
            val editor: PlaylistEditor = editPlaylistEndpoint.getPlaylistEditor(playlist.id)
            tracks.forEachIndexed { i, track ->
                editor.addSongByVideoId(track.id, position + i)
            }
            true
        }
    }

    override suspend fun removeFromPlaylist(playlist: Playlist, vararg tracks: Track): Boolean {
        return withUserAuth { auth ->
            val editor: PlaylistEditor = editPlaylistEndpoint.getPlaylistEditor(playlist.id)
            tracks.forEach { track ->
                editor.removeSong(track.id)
            }
            true
        }
    }

    override suspend fun shareEntity(item: EchoMediaItem): String? {
        return "https://music.youtube.com/watch?v=${item.id}"
    }

    override suspend fun loadFeed(track: Track): Feed<Shelf>? = withVisitorId {
        val id = track.id.ifEmpty { track.extras["videoId"] }
        val result = songFeedEndpoint.loadFromTrack(id ?: return@withVisitorId null)
        result.map { it.toShelf(api, language, thumbnailQuality) }.let { shelves ->
            Feed(emptyList()) { _ -> Feed.Data(PagedData.Single { shelves }) }
        }
    }

    override suspend fun loadHomeFeed(): Feed<Shelf> = withVisitorId {
        val shelves = songFeedEndpoint.loadHomePage().map {
            it.toShelf(api, language, thumbnailQuality)
        }
        Feed(emptyList()) { _ -> Feed.Data(PagedData.Single { shelves }) }
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        val shelves = getArtistMediaItems(artist)
        return Feed(emptyList()) { _ -> PagedData.Single { shelves }.toFeedData() }
    }

    private suspend fun EchoMediaItem.toShelf(): Shelf {
        val items = when (this) {
            is Playlist -> {
                val data = loadTracks(this).load()
                Shelf.Lists.Items(
                    id = id,
                    title = title,
                    subtitle = authors.firstOrNull()?.name,
                    list = data,
                    more = null,
                )
            }
            is Album -> {
                val data = loadTracks(this).load()
                Shelf.Lists.Items(
                    id = id,
                    title = title,
                    subtitle = artists.firstOrNull()?.name,
                    list = data,
                    more = null,
                )
            }
            is Artist -> {
                loadFeed(this)?.getPagedData(null)?.pagedData?.let { pagedData ->
                    coroutineScope {
                        val shelves = async { pagedData.load(null) }
                        Shelf.Category(
                            id = id,
                            title = name,
                            subtitle = null,
                            shelves = shelves.await().data,
                        )
                    }
                } ?: Shelf.Category(
                    id = id,
                    title = name,
                    subtitle = null,
                    shelves = emptyList(),
                )
            }
            is Track -> {
                val data = loadFeed(this)?.getPagedData(null)?.pagedData?.load(null)?.data
                    ?: emptyList()
                Shelf.Category(
                    id = id,
                    title = title,
                    subtitle = artists.firstOrNull()?.name,
                    shelves = data,
                )
            }
            else -> {
                Shelf.Empty(id = id, title = "Unknown", subtitle = null)
            }
        }
        return items
    }

    override suspend fun loadRadio(radio: Radio): Feed<Track> {
        val (_, _, data) = playlistEndPoint.loadFromPlaylist(
            radio.id,
            null,
            thumbnailQuality
        )
        return data.toFeed()
    }

    override suspend fun getRadio(track: Track): Radio {
        val id = try {
            val response = songFeedEndpoint.getRadioId(track.id).getOrThrow()
            response
        } catch (e: Exception) {
            e.printStackTrace()
            "RDAMVM${track.id}"
        }
        return Radio(
            id = id,
            cover = track.cover,
            title = "Radio",
            subtitle = "Based on ${track.title}",
            track = track
        )
    }

    override suspend fun getQuickSearch(track: Track): List<QuickSearchItem> {
        val results = mutableListOf<QuickSearchItem>()
        
        // Add "Artist Radio" search item if track has artists
        track.artists.firstOrNull()?.let { artist ->
            results.add(
                QuickSearchItem(
                    id = "artist:${artist.id}",
                    title = "More from ${artist.name}",
                    subtitle = "Artist radio",
                    cover = artist.cover
                )
            )
        }
        
        // Add "Track Radio" search item
        results.add(
            QuickSearchItem(
                id = "track:${track.id}",
                title = "Similar to ${track.title}",
                subtitle = "Song radio",
                cover = track.cover
            )
        )
        
        return results
    }

    override suspend fun loadQuickSearch(item: QuickSearchItem): Feed<Track> {
        val id = item.id.split(":")
        return when (id[0]) {
            "artist" -> {
                val artist = Artist(id[1], item.title.substringAfter("More from "))
                val radio = Radio("RDEM${artist.id}", null, "Radio", artist.name, null)
                loadRadio(radio)
            }
            else -> {
                val track = Track(id[1], item.title.substringAfter("Similar to "))
                val radio = getRadio(track)
                loadRadio(radio)
            }
        }
    }

    override val extensionInfo = ExtensionClient.Info(
        id = ID,
        name = "YouTube Music",
        description = "Extension for YouTube Music",
        version = "1.0.0",
        supportedClients = listOf(
            ExtensionClient.ClientType.AUDIO,
            ExtensionClient.ClientType.VIDEO
        ),
        loginButton = "Sign in with Google"
    )
}