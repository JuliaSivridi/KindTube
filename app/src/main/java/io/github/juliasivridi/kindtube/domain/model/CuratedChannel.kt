package io.github.juliasivridi.kindtube.domain.model

data class CuratedChannel(
    val id: String,
    val title: String,
    val avatarUrl: String?,
    val uploadsPlaylistId: String,
    val addedAt: Long = System.currentTimeMillis(),
)
