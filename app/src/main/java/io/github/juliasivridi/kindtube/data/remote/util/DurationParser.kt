package io.github.juliasivridi.kindtube.data.remote.util

import java.time.Instant

/**
 * Parse ISO 8601 duration string (e.g. "PT4M13S") into total seconds.
 */
fun parseIso8601Duration(duration: String?): Int? {
    if (duration.isNullOrBlank()) return null
    return try {
        val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
        val match = regex.find(duration) ?: return null
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toIntOrNull() ?: 0
        val seconds = match.groupValues[3].toIntOrNull() ?: 0
        hours * 3600 + minutes * 60 + seconds
    } catch (e: Exception) {
        null
    }
}

/**
 * Parse ISO 8601 timestamp string (e.g. "2023-01-15T12:00:00Z") into epoch millis.
 */
fun parseIso8601Timestamp(timestamp: String?): Long {
    if (timestamp.isNullOrBlank()) return 0L
    return try {
        Instant.parse(timestamp).toEpochMilli()
    } catch (e: Exception) {
        0L
    }
}
