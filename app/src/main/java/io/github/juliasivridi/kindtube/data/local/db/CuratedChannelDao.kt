package io.github.juliasivridi.kindtube.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.juliasivridi.kindtube.data.local.model.CuratedChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CuratedChannelDao {
    @Query("SELECT * FROM curated_channels ORDER BY addedAt DESC")
    fun getAll(): Flow<List<CuratedChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: CuratedChannelEntity)

    @Query("DELETE FROM curated_channels WHERE id = :channelId")
    suspend fun deleteById(channelId: String)
}
