package com.musicplayer.util

object TimeUtil {
    /** Format milliseconds as M:SS or H:MM:SS */
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /** Format file size as human-readable string */
    fun formatFileSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "${"%.1f".format(bytes / 1_073_741_824.0)} GB"
        bytes >= 1_048_576 -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
        bytes >= 1024 -> "${"%.0f".format(bytes / 1024.0)} KB"
        else -> "$bytes B"
    }
}
