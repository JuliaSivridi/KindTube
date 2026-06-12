package io.github.juliasivridi.kindtube.domain.model

data class Video(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val channelId: String,
    val channelTitle: String,
    val channelAvatarUrl: String?,
    val durationSeconds: Int?,
    val publishedAt: Long,
)
