package io.github.juliasivridi.kindtube.domain.repository

import io.github.juliasivridi.kindtube.domain.model.CuratedChannel
import kotlinx.coroutines.flow.Flow

interface CuratedChannelRepository {
    fun getChannels(): Flow<List<CuratedChannel>>
    suspend fun addChannel(channel: CuratedChannel)
    suspend fun removeChannel(channelId: String)
    suspend fun searchChannels(query: String): Result<List<CuratedChannel>>
}
