package com.musicplayer.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class Artist(
    @PrimaryKey val id: Long,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
)
