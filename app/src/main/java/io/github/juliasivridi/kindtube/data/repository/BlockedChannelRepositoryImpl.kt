package io.github.juliasivridi.kindtube.data.repository

import io.github.juliasivridi.kindtube.data.local.db.BlockedChannelDao
import io.github.juliasivridi.kindtube.data.local.model.toDomain
import io.github.juliasivridi.kindtube.data.local.model.toEntity
import io.github.juliasivridi.kindtube.domain.model.BlockedChannel
import io.github.juliasivridi.kindtube.domain.repository.BlockedChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedChannelRepositoryImpl @Inject constructor(
    private val blockedChannelDao: BlockedChannelDao,
) : BlockedChannelRepository {

    override fun getBlockedChannels(): Flow<List<BlockedChannel>> =
        blockedChannelDao.getAllBlocked().map { list -> list.map { it.toDomain() } }

    override suspend fun blockChannel(channel: BlockedChannel) {
        blockedChannelDao.insert(channel.toEntity())
    }

    override suspend fun unblockChannel(channelId: String) {
        blockedChannelDao.deleteByChannelId(channelId)
    }

    override suspend fun isBlocked(channelId: String): Boolean =
        blockedChannelDao.isBlocked(channelId)
}
