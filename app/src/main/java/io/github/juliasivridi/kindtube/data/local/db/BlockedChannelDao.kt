package io.github.juliasivridi.kindtube.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.juliasivridi.kindtube.data.local.model.BlockedChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedChannelDao {

    @Query("SELECT * FROM blocked_channels ORDER BY blockedAt DESC")
    fun getAllBlocked(): Flow<List<BlockedChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: BlockedChannelEntity)

    @Query("DELETE FROM blocked_channels WHERE channelId = :channelId")
    suspend fun deleteByChannelId(channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_channels WHERE channelId = :channelId)")
    suspend fun isBlocked(channelId: String): Boolean
}
