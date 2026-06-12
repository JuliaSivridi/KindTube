package io.github.juliasivridi.kindtube.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.juliasivridi.kindtube.domain.model.CuratedChannel

@Entity(tableName = "curated_channels")
data class CuratedChannelEntity(
    @PrimaryKey val id: String,
    val title: String,
    val avatarUrl: String?,
    val uploadsPlaylistId: String,
    val addedAt: Long,
)

fun CuratedChannelEntity.toDomain() = CuratedChannel(
    id = id,
    title = title,
    avatarUrl = avatarUrl,
    uploadsPlaylistId = uploadsPlaylistId,
    addedAt = addedAt,
)

fun CuratedChannel.toEntity() = CuratedChannelEntity(
    id = id,
    title = title,
    avatarUrl = avatarUrl,
    uploadsPlaylistId = uploadsPlaylistId,
    addedAt = addedAt,
)
