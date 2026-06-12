package io.github.juliasivridi.kindtube.domain.model

data class BlockedChannel(
    val channelId: String,
    val channelTitle: String,
    val channelAvatarUrl: String?,
    val blockedAt: Long,
)
