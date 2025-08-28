# Additional ProGuard Rules

# Keep helper classes
-keep class dev.brahmkshatriya.echo.extension.UserToArtistHelper { *; }
-keepclassmembers class dev.brahmkshatriya.echo.extension.UserToArtistHelper {
    public static dev.brahmkshatriya.echo.common.models.Artist safeConvertUserToArtist(dev.brahmkshatriya.echo.common.models.User);
    public static dev.brahmkshatriya.echo.common.models.Artist convertIfUser(java.lang.Object);
}

-keep class dev.brahmkshatriya.echo.extension.ModelTypeHelper { *; }
-keepclassmembers class dev.brahmkshatriya.echo.extension.ModelTypeHelper {
    public static * userToArtist(dev.brahmkshatriya.echo.common.models.User);
    public static * artistToUser(dev.brahmkshatriya.echo.common.models.Artist);
    public static * ensureProperArtistsInAlbum(dev.brahmkshatriya.echo.common.models.Album);
    public static * ensureProperArtistsInTrack(dev.brahmkshatriya.echo.common.models.Track);
    public static * ensureProperAuthorsInPlaylist(dev.brahmkshatriya.echo.common.models.Playlist);
}

# Ensure model classes are preserved
-keep class dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension$Companion {
    private static dev.brahmkshatriya.echo.common.models.Artist withExtensionId(dev.brahmkshatriya.echo.common.models.Artist, java.lang.String, java.lang.Object);
    private static dev.brahmkshatriya.echo.common.models.EchoMediaItem withExtensionId(dev.brahmkshatriya.echo.common.models.EchoMediaItem, java.lang.String, java.lang.Object);
}

# Prevent class merging for these model classes
-keepnames class dev.brahmkshatriya.echo.common.models.User
-keepnames class dev.brahmkshatriya.echo.common.models.Artist
-keepnames class dev.brahmkshatriya.echo.common.models.Album
-keepnames class dev.brahmkshatriya.echo.common.models.Playlist
-keepnames class dev.brahmkshatriya.echo.common.models.Track
-keepnames class dev.brahmkshatriya.echo.common.models.Radio

# Prevent optimization that could break model conversions
-keepclassmembernames class dev.brahmkshatriya.echo.extension.Convertors {
    public static ** toArtist(...);
    public static ** toUser(...);
    public static ** toAlbum(...);
    public static ** toPlaylist(...);
    public static ** toTrack(...);
}