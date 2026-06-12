package io.github.juliasivridi.kindtube.data.repository

import io.github.juliasivridi.kindtube.data.local.db.CuratedChannelDao
import io.github.juliasivridi.kindtube.data.local.db.FeedVideoDao
import io.github.juliasivridi.kindtube.data.local.db.VideoDao
import io.github.juliasivridi.kindtube.data.local.model.FeedVideoEntity
import io.github.juliasivridi.kindtube.data.local.model.toEntity
import io.github.juliasivridi.kindtube.data.local.model.toDomain
import io.github.juliasivridi.kindtube.data.remote.api.YouTubeApiService
import io.github.juliasivridi.kindtube.data.remote.util.parseIso8601Duration
import io.github.juliasivridi.kindtube.data.remote.util.parseIso8601Timestamp
import io.github.juliasivridi.kindtube.domain.model.Video
import io.github.juliasivridi.kindtube.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNELS_TO_SAMPLE = 12
private const val VIDEOS_MIN_PER_CHANNEL = 3
private const val VIDEOS_MAX_PER_CHANNEL = 5

@Singleton
class FeedRepositoryImpl @Inject constructor(
    private val videoDao: VideoDao,
    private val feedVideoDao: FeedVideoDao,
    private val curatedChannelDao: CuratedChannelDao,
    private val api: YouTubeApiService,
) : FeedRepository {

    /**
     * Returns the current feed as a Flow.
     * The feed is a pre-computed ordered list of video IDs stored in feed_videos table.
     * It changes only when refreshFeed() is called.
     */
    override fun getFeed(): Flow<List<Video>> =
        feedVideoDao.getOrderedVideoIds().map { ids ->
            if (ids.isEmpty()) return@map emptyList()
            val entities = videoDao.getVideosByIds(ids)
            val byId = entities.associateBy { it.id }
            ids.mapNotNull { byId[it]?.toDomain() }
        }

    /**
     * Refresh algorithm (per architecture spec):
     * 1. Get all curated channels
     * 2. Randomly sample up to 12 channels
     * 3. For each channel: fetch latest videos from uploads playlist (API)
     * 4. Randomly pick 3–5 videos from each channel's results
     * 5. Shuffle combined list → store in feed_videos
     *
     * Pull-to-refresh always produces a new random selection.
     */
    override suspend fun refreshFeed(): Result<Unit> = runCatching {
        val allChannels = curatedChannelDao.getAll().first()
        if (allChannels.isEmpty()) {
            feedVideoDao.clear()
            return@runCatching
        }

        val sampledChannels = allChannels.shuffled().take(CHANNELS_TO_SAMPLE)
        val feedVideos = mutableListOf<Video>()

        for (channel in sampledChannels) {
            runCatching {
                val playlistItems = api.getPlaylistItems(
                    playlistId = channel.uploadsPlaylistId,
                    maxResults = 50,
                )
                val videoIds = playlistItems.items
                    ?.mapNotNull { it.snippet?.resourceId?.videoId ?: it.contentDetails?.videoId }
                    ?: return@runCatching
                if (videoIds.isEmpty()) return@runCatching

                val videosResponse = api.getVideos(ids = videoIds.joinToString(","))
                val channelVideos = videosResponse.items?.mapNotNull { item ->
                    // Skip videos whose owner disabled embedding — they show
                    // "Video unavailable" in our player and frustrate the kid
                    if (item.status?.embeddable == false) return@mapNotNull null
                    val title = item.snippet?.title ?: return@mapNotNull null
                    val thumbnailUrl = item.snippet.thumbnails?.medium?.url
                        ?: item.snippet.thumbnails?.default?.url
                        ?: return@mapNotNull null
                    Video(
                        id = item.id,
                        title = title,
                        thumbnailUrl = thumbnailUrl,
                        channelId = item.snippet.channelId ?: channel.id,
                        channelTitle = item.snippet.channelTitle ?: channel.title,
                        channelAvatarUrl = channel.avatarUrl,
                        durationSeconds = parseIso8601Duration(item.contentDetails?.duration),
                        publishedAt = parseIso8601Timestamp(item.snippet.publishedAt),
                    )
                } ?: return@runCatching

                // Random 3–5 videos from this channel
                val pick = (VIDEOS_MIN_PER_CHANNEL..VIDEOS_MAX_PER_CHANNEL).random()
                feedVideos.addAll(channelVideos.shuffled().take(pick))

                // Cache all fetched videos in the videos table (for player + related videos)
                videoDao.insertVideos(channelVideos.map { it.toEntity() })
            }
        }

        // Shuffle the combined feed and persist the order
        val shuffled = feedVideos.shuffled()
        feedVideoDao.clear()
        feedVideoDao.insertAll(
            shuffled.mapIndexed { index, video ->
                FeedVideoEntity(videoId = video.id, position = index)
            }
        )
    }
}
