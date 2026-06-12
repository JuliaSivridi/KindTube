package io.github.juliasivridi.kindtube.domain.repository

import io.github.juliasivridi.kindtube.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistory(): Flow<List<HistoryEntry>>
    suspend fun addToHistory(entry: HistoryEntry)
    suspend fun clearHistory()
}
