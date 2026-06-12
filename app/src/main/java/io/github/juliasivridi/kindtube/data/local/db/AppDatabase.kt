package io.github.juliasivridi.kindtube.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.juliasivridi.kindtube.data.local.model.BlockedChannelEntity
import io.github.juliasivridi.kindtube.data.local.model.CuratedChannelEntity
import io.github.juliasivridi.kindtube.data.local.model.FeedVideoEntity
import io.github.juliasivridi.kindtube.data.local.model.HistoryEntryEntity
import io.github.juliasivridi.kindtube.data.local.model.VideoEntity

@Database(
    entities = [
        VideoEntity::class,
        CuratedChannelEntity::class,
        HistoryEntryEntity::class,
        BlockedChannelEntity::class,
        FeedVideoEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun curatedChannelDao(): CuratedChannelDao
    abstract fun historyDao(): HistoryDao
    abstract fun blockedChannelDao(): BlockedChannelDao
    abstract fun feedVideoDao(): FeedVideoDao
}
