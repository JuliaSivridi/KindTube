package io.github.juliasivridi.kindtube.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.toRelativeDate(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < TimeUnit.HOURS.toMillis(24) -> "Сегодня"
        diff < TimeUnit.HOURS.toMillis(48) -> "Вчера"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} дн. назад"
        else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(this))
    }
}

fun String.toEpochMs(): Long = try {
    // ISO 8601: "2024-01-15T10:30:00Z"
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.parse(this)?.time ?: 0L
} catch (e: Exception) {
    0L
}

const val FEED_CACHE_TTL_MS = 30 * 60 * 1000L        // 30 минут
const val SUBSCRIPTION_CACHE_TTL_MS = 30 * 60 * 1000L // 30 минут
const val CHANNELS_TO_SAMPLE = 12
const val VIDEOS_PER_CHANNEL = 4
const val YOUTUBE_BASE_URL = "https://www.googleapis.com/youtube/v3/"

fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
