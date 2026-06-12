package io.github.juliasivridi.kindtube.domain.repository

import io.github.juliasivridi.kindtube.domain.model.Video

interface SearchRepository {
    suspend fun searchVideos(query: String, pageToken: String? = null): Result<SearchResult>
}

data class SearchResult(
    val videos: List<Video>,
    val nextPageToken: String?,
)
