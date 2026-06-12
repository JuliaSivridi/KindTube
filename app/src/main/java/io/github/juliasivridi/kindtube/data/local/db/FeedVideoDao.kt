package io.github.juliasivridi.kindtube.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.juliasivridi.kindtube.data.local.model.FeedVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedVideoDao {

    @Query("SELECT videoId FROM feed_videos ORDER BY position ASC")
    fun getOrderedVideoIds(): Flow<List<String>>

    @Query("DELETE FROM feed_videos")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FeedVideoEntity>)
}
