package com.musicplayer.util

/**
 * Helpers for displaying audio format information on the Now Playing screen.
 */
object AudioFormatUtil {

    /** Returns a compact badge string, e.g. "FLAC 24-bit / 96 kHz" */
    fun formatBadge(mimeType: String, sampleRate: Int, bitDepth: Int, bitrate: Int): String {
        val format = mimeTypeToFormat(mimeType)
        val rate = sampleRateLabel(sampleRate)
        val depth = if (bitDepth > 0 && bitDepth != 16) "$bitDepth-bit" else ""
        val bitrateStr = if (bitrate > 0 && isLossyFormat(mimeType)) "${bitrate}k" else ""

        return buildString {
            append(format)
            if (depth.isNotEmpty()) append(" $depth")
            if (rate.isNotEmpty()) append(" / $rate")
            if (bitrateStr.isNotEmpty()) append(" $bitrateStr")
        }.trim()
    }

    fun mimeTypeToFormat(mimeType: String): String = when {
        mimeType.contains("flac") -> "FLAC"
        mimeType.contains("wav") || mimeType.contains("wave") -> "WAV"
        mimeType.contains("mp4") || mimeType.contains("m4a") || mimeType.contains("aac") -> "AAC"
        mimeType.contains("mpeg") || mimeType.contains("mp3") -> "MP3"
        mimeType.contains("ogg") -> "OGG"
        mimeType.contains("opus") -> "OPUS"
        mimeType.contains("wma") -> "WMA"
        mimeType.contains("ape") -> "APE"
        mimeType.contains("dsf") || mimeType.contains("dff") -> "DSD"
        mimeType.contains("aiff") || mimeType.contains("aif") -> "AIFF"
        mimeType.contains("alac") -> "ALAC"
        mimeType.contains("matroska") || mimeType.contains("mka") -> "MKA"
        else -> mimeType.substringAfterLast("/").uppercase()
    }

    fun sampleRateLabel(sampleRateHz: Int): String = when {
        sampleRateHz <= 0 -> ""
        sampleRateHz >= 1000 -> "${"%.0f".format(sampleRateHz / 1000.0)} kHz"
        else -> "$sampleRateHz Hz"
    }

    fun isLossyFormat(mimeType: String): Boolean =
        mimeType.contains("mp3") || mimeType.contains("mpeg") ||
        mimeType.contains("aac") || mimeType.contains("ogg") ||
        mimeType.contains("opus") || mimeType.contains("wma")

    /** Returns true if the format is hi-res (>16-bit or >48kHz). */
    fun isHiRes(sampleRate: Int, bitDepth: Int): Boolean =
        sampleRate > 48000 || bitDepth > 16
}
