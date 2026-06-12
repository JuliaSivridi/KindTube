package io.github.juliasivridi.kindtube.data.repository

import io.github.juliasivridi.kindtube.data.local.db.CuratedChannelDao
import io.github.juliasivridi.kindtube.data.local.model.toDomain
import io.github.juliasivridi.kindtube.data.local.model.toEntity
import io.github.juliasivridi.kindtube.data.remote.api.YouTubeApiService
import io.github.juliasivridi.kindtube.domain.model.CuratedChannel
import io.github.juliasivridi.kindtube.domain.repository.BlockedChannelRepository
import io.github.juliasivridi.kindtube.domain.repository.CuratedChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CuratedChannelRepositoryImpl @Inject constructor(
    private val dao: CuratedChannelDao,
    private val api: YouTubeApiService,
    private val blockedChannelRepository: BlockedChannelRepository,
) : CuratedChannelRepository {

    override fun getChannels(): Flow<List<CuratedChannel>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addChannel(channel: CuratedChannel) {
        dao.insert(channel.toEntity())
    }

    override suspend fun removeChannel(channelId: String) {
        dao.deleteById(channelId)
    }

    override suspend fun searchChannels(query: String): Result<List<CuratedChannel>> =
        runCatching {
            val searchResult = api.searchChannels(query = query)
            val channelIds = searchResult.items
                ?.mapNotNull { it.id?.channelId }
                ?: return@runCatching emptyList()

            if (channelIds.isEmpty()) return@runCatching emptyList()

            // Fetch full channel details (including uploadsPlaylistId)
            val details = api.getChannels(ids = channelIds.joinToString(","))
            details.items?.mapNotNull { item ->
                val title = item.snippet?.title ?: return@mapNotNull null
                val avatarUrl = item.snippet.thumbnails?.medium?.url
                    ?: item.snippet.thumbnails?.default?.url
                val uploadsPlaylistId = item.contentDetails?.relatedPlaylists?.uploads
                    ?: return@mapNotNull null

                // Hide blocked channels from search results
                if (blockedChannelRepository.isBlocked(item.id)) return@mapNotNull null

                CuratedChannel(
                    id = item.id,
                    title = title,
                    avatarUrl = avatarUrl,
                    uploadsPlaylistId = uploadsPlaylistId,
                )
            } ?: emptyList()
        }
}
