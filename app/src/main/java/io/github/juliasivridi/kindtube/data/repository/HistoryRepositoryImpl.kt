package io.github.juliasivridi.kindtube.data.repository

import io.github.juliasivridi.kindtube.data.local.db.HistoryDao
import io.github.juliasivridi.kindtube.data.local.model.toDomain
import io.github.juliasivridi.kindtube.data.local.model.toEntity
import io.github.juliasivridi.kindtube.domain.model.HistoryEntry
import io.github.juliasivridi.kindtube.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
) : HistoryRepository {

    override fun getHistory(): Flow<List<HistoryEntry>> =
        historyDao.getAllHistory().map { list -> list.map { it.toDomain() } }

    override suspend fun addToHistory(entry: HistoryEntry) {
        // Remove existing entry for this video before inserting so there are no duplicates
        historyDao.deleteByVideoId(entry.video.id)
        historyDao.insert(entry.toEntity())
    }

    override suspend fun clearHistory() {
        historyDao.deleteAll()
    }
}
