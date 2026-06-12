package io.github.juliasivridi.kindtube.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.juliasivridi.kindtube.domain.model.HistoryEntry
import io.github.juliasivridi.kindtube.domain.model.Video

@Entity(tableName = "history")
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val videoTitle: String,
    val videoThumbnailUrl: String,
    val videoChannelId: String,
    val videoChannelTitle: String,
    val videoChannelAvatarUrl: String?,
    val videoDurationSeconds: Int?,
    val videoPublishedAt: Long,
    val watchedAt: Long,
)

fun HistoryEntryEntity.toDomain(): HistoryEntry {
    val video = Video(
        id = videoId,
        title = videoTitle,
        thumbnailUrl = videoThumbnailUrl,
        channelId = videoChannelId,
        channelTitle = videoChannelTitle,
        channelAvatarUrl = videoChannelAvatarUrl,
        durationSeconds = videoDurationSeconds,
        publishedAt = videoPublishedAt,
    )
    return HistoryEntry(id = id, video = video, watchedAt = watchedAt)
}

fun HistoryEntry.toEntity() = HistoryEntryEntity(
    id = id,
    videoId = video.id,
    videoTitle = video.title,
    videoThumbnailUrl = video.thumbnailUrl,
    videoChannelId = video.channelId,
    videoChannelTitle = video.channelTitle,
    videoChannelAvatarUrl = video.channelAvatarUrl,
    videoDurationSeconds = video.durationSeconds,
    videoPublishedAt = video.publishedAt,
    watchedAt = watchedAt,
)
