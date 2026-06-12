package io.github.juliasivridi.kindtube.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.juliasivridi.kindtube.domain.model.Video

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val thumbnailUrl: String,
    val channelId: String,
    val channelTitle: String,
    val channelAvatarUrl: String?,
    val durationSeconds: Int?,
    val publishedAt: Long,
    val cachedAt: Long = System.currentTimeMillis(),
)

fun VideoEntity.toDomain() = Video(
    id = id,
    title = title,
    thumbnailUrl = thumbnailUrl,
    channelId = channelId,
    channelTitle = channelTitle,
    channelAvatarUrl = channelAvatarUrl,
    durationSeconds = durationSeconds,
    publishedAt = publishedAt,
)

fun Video.toEntity() = VideoEntity(
    id = id,
    title = title,
    thumbnailUrl = thumbnailUrl,
    channelId = channelId,
    channelTitle = channelTitle,
    channelAvatarUrl = channelAvatarUrl,
    durationSeconds = durationSeconds,
    publishedAt = publishedAt,
)
