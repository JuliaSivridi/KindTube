package io.github.juliasivridi.kindtube.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.juliasivridi.kindtube.domain.model.BlockedChannel

@Entity(tableName = "blocked_channels")
data class BlockedChannelEntity(
    @PrimaryKey val channelId: String,
    val channelTitle: String,
    val channelAvatarUrl: String?,
    val blockedAt: Long,
)

fun BlockedChannelEntity.toDomain() = BlockedChannel(
    channelId = channelId,
    channelTitle = channelTitle,
    channelAvatarUrl = channelAvatarUrl,
    blockedAt = blockedAt,
)

fun BlockedChannel.toEntity() = BlockedChannelEntity(
    channelId = channelId,
    channelTitle = channelTitle,
    channelAvatarUrl = channelAvatarUrl,
    blockedAt = blockedAt,
)
