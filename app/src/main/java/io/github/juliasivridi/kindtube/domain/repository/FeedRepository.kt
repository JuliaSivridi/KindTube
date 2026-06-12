package io.github.juliasivridi.kindtube.domain.repository

import io.github.juliasivridi.kindtube.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getFeed(): Flow<List<Video>>
    suspend fun refreshFeed(): Result<Unit>
}
