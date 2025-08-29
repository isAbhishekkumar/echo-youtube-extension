# Additional ProGuard rules specific to search result fixes

# Ensure all SearchResultsFixer methods are kept
-keep class dev.brahmkshatriya.echo.extension.SearchResultsFixer {
    public static ** fixSearchResults(...);
    public static ** fixSearchResultShelf(...);
    public static ** fixSearchResultItem(...);
    public static ** fixTrack(...);
    public static ** fixAlbum(...);
    public static ** fixPlaylist(...);
}

# Ensure the ModelTypeHelper methods are kept
-keep class dev.brahmkshatriya.echo.extension.ModelTypeHelper {
    public static ** userToArtist(...);
    public static ** artistToUser(...);
    public static ** safeArtistConversion(...);
    public static ** safeArtistListConversion(...);
    public static ** ensureProperTypesInMediaItem(...);
    public static ** fixSearchResultShelf(...);
}

# Ensure the UnifiedExtensionPatch methods are kept
-keep class dev.brahmkshatriya.echo.extension.UnifiedExtensionPatch {
    public static ** patchMediaItem(...);
    public static ** patchTrack(...);
    public static ** patchAlbum(...);
    public static ** patchPlaylist(...);
    public static ** patchSearchResults(...);
    public static ** patchSearchResultShelf(...);
}

# Prevent any optimization that would interfere with our type conversion
-keepclassmembers class dev.brahmkshatriya.echo.common.models.User { *; }
-keepclassmembers class dev.brahmkshatriya.echo.common.models.Artist { *; }
-keepclassmembers class dev.brahmkshatriya.echo.common.models.EchoMediaItem { *; }

# Maintain proper inheritance relationship between User and EchoMediaItem
-keep class dev.brahmkshatriya.echo.common.models.User extends dev.brahmkshatriya.echo.common.models.EchoMediaItem
-keep class dev.brahmkshatriya.echo.common.models.Artist extends dev.brahmkshatriya.echo.common.models.EchoMediaItem

# Prevent any field inlining or removal in model classes
-keepclassmembers class dev.brahmkshatriya.echo.common.models.** {
    <fields>;
    <methods>;
}

# Preserve type information needed for type checking and casting
-keepattributes Signature,InnerClasses,EnclosingMethod

# Keep loadSearchFeed method intact
-keep,includedescriptorclasses class dev.brahmkshatriya.echo.extension.YoutubeExtension {
    public *** loadSearchFeed(...);
    public *** radio(...);
}