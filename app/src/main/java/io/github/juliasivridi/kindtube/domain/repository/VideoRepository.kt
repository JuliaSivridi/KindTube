package io.github.juliasivridi.kindtube.domain.repository

import io.github.juliasivridi.kindtube.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getChannelVideos(channelId: String, uploadsPlaylistId: String): Flow<List<Video>>
    suspend fun refreshChannelVideos(channelId: String, uploadsPlaylistId: String): Result<Unit>
    suspend fun getVideoById(videoId: String): Result<Video>
}
