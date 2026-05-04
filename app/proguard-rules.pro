# ProGuard rules for HiFi Music Player

# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep JAudioTagger
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Keep Hilt-generated components
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Keep Room entities
-keep class com.musicplayer.data.local.db.** { *; }

# Keep model classes (used for serialization/DB)
-keep class com.musicplayer.domain.model.** { *; }
