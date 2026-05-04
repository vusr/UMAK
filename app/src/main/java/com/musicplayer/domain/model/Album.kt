package com.musicplayer.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey val id: Long,
    val name: String,
    val artist: String,
    val artistId: Long,
    val trackCount: Int,
    val year: Int,
    val albumArtUri: String?,    // Content URI string for embedded art
    val genre: String,
)
