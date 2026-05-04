package com.musicplayer.util

import kotlin.math.pow

/**
 * Maps a UI volume level (1–50) to a linear gain factor (0.0–1.0).
 *
 * Uses a logarithmic curve so that each level step sounds perceptually equal.
 * Level 50 = 0 dB (full gain = 1.0)
 * Level 1  = -40 dB (gain ≈ 0.01 — barely audible)
 *
 * Formula: gain = 10 ^ ((level - 50) × 40 / (50 × 20))
 *        = 10 ^ ((level - 50) × 0.04)
 */
object VolumeUtil {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 50

    /** Convert UI level (1–50) to ExoPlayer gain (0.0–1.0). */
    fun levelToGain(level: Int): Float {
        val clamped = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        return 10f.pow((clamped - MAX_LEVEL) * 0.04f)
    }

    /** Convert ExoPlayer gain (0.0–1.0) back to the nearest UI level (1–50). */
    fun gainToLevel(gain: Float): Int {
        if (gain <= 0f) return MIN_LEVEL
        val level = (MAX_LEVEL + Math.log10(gain.toDouble()) / 0.04).toInt()
        return level.coerceIn(MIN_LEVEL, MAX_LEVEL)
    }

    /** Human-readable dB string for the given level, e.g. "-12.0 dB". */
    fun levelToDbString(level: Int): String {
        val db = (level - MAX_LEVEL) * 0.04 * 20
        return if (level == MAX_LEVEL) "0 dB" else "${"%.1f".format(db)} dB"
    }
}
