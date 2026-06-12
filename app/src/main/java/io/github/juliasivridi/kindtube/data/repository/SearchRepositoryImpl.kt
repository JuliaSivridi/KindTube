package io.github.juliasivridi.kindtube.data.repository

import io.github.juliasivridi.kindtube.data.remote.api.YouTubeApiService
import io.github.juliasivridi.kindtube.data.remote.util.parseIso8601Timestamp
import io.github.juliasivridi.kindtube.domain.model.Video
import io.github.juliasivridi.kindtube.domain.repository.BlockedChannelRepository
import io.github.juliasivridi.kindtube.domain.repository.SearchRepository
import io.github.juliasivridi.kindtube.domain.repository.SearchResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val api: YouTubeApiService,
    private val blockedChannelRepository: BlockedChannelRepository,
) : SearchRepository {

    override suspend fun searchVideos(query: String, pageToken: String?): Result<SearchResult> =
        runCatching {
            val response = api.searchVideos(query = query, pageToken = pageToken)
            val videoIds = response.items
                ?.mapNotNull { it.id?.videoId }
                ?: emptyList()

            if (videoIds.isEmpty()) {
                return@runCatching SearchResult(emptyList(), null)
            }

            val videosResponse = api.getVideos(ids = videoIds.joinToString(","))
            val videos = videosResponse.items?.mapNotNull { item ->
                val title = item.snippet?.title ?: return@mapNotNull null
                val channelId = item.snippet.channelId ?: return@mapNotNull null
                val thumbnailUrl = item.snippet.thumbnails?.medium?.url
                    ?: item.snippet.thumbnails?.default?.url
                    ?: return@mapNotNull null

                // Skip videos from blocked channels
                if (blockedChannelRepository.isBlocked(channelId)) return@mapNotNull null

                Video(
                    id = item.id,
                    title = title,
                    thumbnailUrl = thumbnailUrl,
                    channelId = channelId,
                    channelTitle = item.snippet.channelTitle ?: "",
                    channelAvatarUrl = null,
                    durationSeconds = null,
                    publishedAt = parseIso8601Timestamp(item.snippet.publishedAt),
                )
            } ?: emptyList()

            SearchResult(videos = videos, nextPageToken = response.nextPageToken)
        }
}
