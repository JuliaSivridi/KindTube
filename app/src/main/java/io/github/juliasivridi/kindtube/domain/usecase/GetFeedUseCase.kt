package io.github.juliasivridi.kindtube.domain.usecase

import io.github.juliasivridi.kindtube.domain.model.Video
import io.github.juliasivridi.kindtube.domain.repository.BlockedChannelRepository
import io.github.juliasivridi.kindtube.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetFeedUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val blockedChannelRepository: BlockedChannelRepository,
) {
    operator fun invoke(): Flow<List<Video>> =
        feedRepository.getFeed().combine(blockedChannelRepository.getBlockedChannels()) { videos, blocked ->
            val blockedIds = blocked.map { it.channelId }.toSet()
            videos.filter { it.channelId !in blockedIds }
        }
}
