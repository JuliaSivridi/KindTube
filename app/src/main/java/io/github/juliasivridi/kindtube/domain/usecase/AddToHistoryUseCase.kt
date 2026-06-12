package io.github.juliasivridi.kindtube.domain.usecase

import io.github.juliasivridi.kindtube.domain.model.HistoryEntry
import io.github.juliasivridi.kindtube.domain.model.Video
import io.github.juliasivridi.kindtube.domain.repository.HistoryRepository
import javax.inject.Inject

class AddToHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(video: Video) {
        historyRepository.addToHistory(
            HistoryEntry(
                video = video,
                watchedAt = System.currentTimeMillis(),
            )
        )
    }
}
