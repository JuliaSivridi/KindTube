package io.github.juliasivridi.kindtube.domain.usecase

import io.github.juliasivridi.kindtube.domain.model.BlockedChannel
import io.github.juliasivridi.kindtube.domain.model.Video
import io.github.juliasivridi.kindtube.domain.repository.BlockedChannelRepository
import io.github.juliasivridi.kindtube.domain.repository.CuratedChannelRepository
import javax.inject.Inject

class BlockChannelUseCase @Inject constructor(
    private val blockedChannelRepository: BlockedChannelRepository,
    private val curatedChannelRepository: CuratedChannelRepository,
) {
    suspend operator fun invoke(video: Video) {
        // 1. Add channel to blocked list
        blockedChannelRepository.blockChannel(
            BlockedChannel(
                channelId = video.channelId,
                channelTitle = video.channelTitle,
                channelAvatarUrl = video.channelAvatarUrl,
                blockedAt = System.currentTimeMillis(),
            )
        )
        // 2. Remove from curated subscriptions if it was there
        runCatching { curatedChannelRepository.removeChannel(video.channelId) }
    }
}
