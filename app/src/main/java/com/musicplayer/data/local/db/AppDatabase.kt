package com.musicplayer.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.musicplayer.data.local.db.dao.*
import com.musicplayer.domain.model.*

@Database(
    entities = [Track::class, Album::class, Artist::class, Playlist::class, EqPreset::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun eqPresetDao(): EqPresetDao

    companion object {
        const val DATABASE_NAME = "musicplayer.db"

        val BUILT_IN_PRESETS = listOf(
            EqPreset(name = "Flat",          bandGains = "[0,0,0,0,0]",            isBuiltIn = true),
            EqPreset(name = "Bass Boost",    bandGains = "[600,400,0,0,0]",        isBuiltIn = true),
            EqPreset(name = "Treble Boost",  bandGains = "[0,0,0,400,600]",        isBuiltIn = true),
            EqPreset(name = "Vocal Clarity", bandGains = "[-200,0,400,300,0]",     isBuiltIn = true),
            EqPreset(name = "Rock",          bandGains = "[400,200,-100,200,400]", isBuiltIn = true),
            EqPreset(name = "Classical",     bandGains = "[300,0,0,0,300]",        isBuiltIn = true),
            EqPreset(name = "Electronic",    bandGains = "[400,300,0,300,200]",    isBuiltIn = true),
        )
    }
}
