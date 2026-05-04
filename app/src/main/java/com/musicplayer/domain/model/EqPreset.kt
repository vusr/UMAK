package com.musicplayer.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a named equalizer preset.
 * bandGains is a JSON array of Int values in millibels (e.g. [-500, 0, 300, -200, 100]).
 * The length of the array matches the hardware band count queried at runtime.
 */
@Entity(tableName = "eq_presets")
data class EqPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bandGains: String,          // JSON: "[millibelGain, ...]"
    val bassBoostStrength: Int = 0, // 0–1000 per Android AudioFX API
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,    // millibels
    val isBuiltIn: Boolean = false, // built-in presets cannot be deleted
)
