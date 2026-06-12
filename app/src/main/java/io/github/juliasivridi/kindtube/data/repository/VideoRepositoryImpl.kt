package io.github.juliasivridi.kindtube.data.repository

import io.github.juliasivridi.kindtube.data.local.db.VideoDao
import io.github.juliasivridi.kindtube.data.local.model.toDomain
import io.github.juliasivridi.kindtube.data.local.model.toEntity
import io.github.juliasivridi.kindtube.data.remote.api.YouTubeApiService
import io.github.juliasivridi.kindtube.data.remote.util.parseIso8601Duration
import io.github.juliasivridi.kindtube.data.remote.util.parseIso8601Timestamp
import io.github.juliasivridi.kindtube.domain.model.Video
import io.github.juliasivridi.kindtube.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val videoDao: VideoDao,
    private val api: YouTubeApiService,
) : VideoRepository {

    override fun getChannelVideos(channelId: String, uploadsPlaylistId: String): Flow<List<Video>> =
        videoDao.getVideosByChannel(channelId).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshChannelVideos(
        channelId: String,
        uploadsPlaylistId: String,
    ): Result<Unit> = runCatching {
        val playlistItems = api.getPlaylistItems(playlistId = uploadsPlaylistId, maxResults = 20)
        val videoIds = playlistItems.items
            ?.mapNotNull { it.snippet?.resourceId?.videoId ?: it.contentDetails?.videoId }
            ?: return@runCatching

        if (videoIds.isEmpty()) return@runCatching

        val videosResponse = api.getVideos(ids = videoIds.joinToString(","))
        val entities = videosResponse.items?.mapNotNull { item ->
            // Skip videos whose owner disabled embedding — unplayable in our player
            if (item.status?.embeddable == false) return@mapNotNull null
            val title = item.snippet?.title ?: return@mapNotNull null
            val thumbnailUrl = item.snippet.thumbnails?.medium?.url
                ?: item.snippet.thumbnails?.default?.url
                ?: return@mapNotNull null
            Video(
                id = item.id,
                title = title,
                thumbnailUrl = thumbnailUrl,
                channelId = item.snippet.channelId ?: channelId,
                channelTitle = item.snippet.channelTitle ?: "",
                channelAvatarUrl = null,
                durationSeconds = parseIso8601Duration(item.contentDetails?.duration),
                publishedAt = parseIso8601Timestamp(item.snippet.publishedAt),
            ).toEntity()
        } ?: return@runCatching

        videoDao.deleteVideosByChannel(channelId)
        videoDao.insertVideos(entities)
    }

    override suspend fun getVideoById(videoId: String): Result<Video> = runCatching {
        videoDao.getVideoById(videoId)?.toDomain()
            ?: run {
                val response = api.getVideos(ids = videoId)
                val item = response.items?.firstOrNull()
                    ?: throw Exception("Video not found: $videoId")
                Video(
                    id = item.id,
                    title = item.snippet?.title ?: "",
                    thumbnailUrl = item.snippet?.thumbnails?.medium?.url
                        ?: item.snippet?.thumbnails?.default?.url ?: "",
                    channelId = item.snippet?.channelId ?: "",
                    channelTitle = item.snippet?.channelTitle ?: "",
                    channelAvatarUrl = null,
                    durationSeconds = parseIso8601Duration(item.contentDetails?.duration),
                    publishedAt = parseIso8601Timestamp(item.snippet?.publishedAt),
                )
            }
    }
}
