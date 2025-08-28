# Keep Kotlin serialization
-keep class kotlinx.serialization.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions
-dontwarn kotlinx.serialization.**

# Keep extension classes
-keep class dev.brahmkshatriya.echo.extension.YoutubeExtension { *; }
-keepclassmembers class dev.brahmkshatriya.echo.extension.YoutubeExtension { *; }
-keep class dev.brahmkshatriya.echo.extension.** { *; }

# Keep YTM-kt library
-keep class dev.toastbits.ytmkt.** { *; }
-keep interface dev.toastbits.ytmkt.** { *; }

# Keep Ktor classes
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**

# Keep reflection
-dontwarn kotlin.reflect.jvm.internal.**

# Keep network libraries
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Keep enums
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    **[] values();
    ** valueOf(java.lang.String);
}

# Prevent obfuscation
-dontobfuscate

# Keep extension interfaces
-keep interface dev.brahmkshatriya.echo.common.clients.** { *; }