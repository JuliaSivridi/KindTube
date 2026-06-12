package io.github.juliasivridi.kindtube.domain.model

data class HistoryEntry(
    val id: Long = 0,
    val video: Video,
    val watchedAt: Long,
)
