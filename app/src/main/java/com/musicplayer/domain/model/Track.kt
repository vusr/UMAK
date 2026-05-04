package com.musicplayer.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single audio track on the device.
 * Maps directly to a Room database entity and is populated from MediaStore.
 */
@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val albumName: String,
    val albumId: Long,
    val artistId: Long,
    val duration: Long,           // milliseconds
    val filePath: String,
    val mimeType: String,         // e.g. "audio/flac", "audio/mpeg"
    val sampleRate: Int,          // Hz e.g. 44100, 96000, 192000
    val bitDepth: Int,            // bits e.g. 16, 24, 32
    val bitrate: Int,             // kbps
    val fileSize: Long,           // bytes
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val genre: String,
    val dateAdded: Long,          // Unix timestamp
    val dateModified: Long,
    val playCount: Int = 0,
    val lastPlayed: Long = 0,
    val isFavorite: Boolean = false,
    val replayGainTrack: Float = 0f,  // dB value from ReplayGain tag
    val replayGainAlbum: Float = 0f,
)
