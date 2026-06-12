package io.github.juliasivridi.kindtube.domain.usecase

import io.github.juliasivridi.kindtube.domain.repository.FeedRepository
import javax.inject.Inject

class RefreshFeedUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
) {
    suspend operator fun invoke(): Result<Unit> = feedRepository.refreshFeed()
}
