package io.github.juliasivridi.kindtube.domain.model

data class Channel(
    val id: String,
    val title: String,
    val avatarUrl: String?,
    val uploadsPlaylistId: String,
)
