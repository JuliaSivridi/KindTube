package io.github.juliasivridi.kindtube.domain.repository

import io.github.juliasivridi.kindtube.domain.model.BlockedChannel
import kotlinx.coroutines.flow.Flow

interface BlockedChannelRepository {
    fun getBlockedChannels(): Flow<List<BlockedChannel>>
    suspend fun blockChannel(channel: BlockedChannel)
    suspend fun unblockChannel(channelId: String)
    suspend fun isBlocked(channelId: String): Boolean
}
