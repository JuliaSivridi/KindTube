package io.github.juliasivridi.kindtube.di

import android.content.Context
import androidx.room.Room
import io.github.juliasivridi.kindtube.data.local.db.AppDatabase
import io.github.juliasivridi.kindtube.data.local.db.BlockedChannelDao
import io.github.juliasivridi.kindtube.data.local.db.CuratedChannelDao
import io.github.juliasivridi.kindtube.data.local.db.FeedVideoDao
import io.github.juliasivridi.kindtube.data.local.db.HistoryDao
import io.github.juliasivridi.kindtube.data.local.db.VideoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mykidtube.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideVideoDao(db: AppDatabase): VideoDao = db.videoDao()
    @Provides fun provideCuratedChannelDao(db: AppDatabase): CuratedChannelDao = db.curatedChannelDao()
    @Provides fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()
    @Provides fun provideBlockedChannelDao(db: AppDatabase): BlockedChannelDao = db.blockedChannelDao()
    @Provides fun provideFeedVideoDao(db: AppDatabase): FeedVideoDao = db.feedVideoDao()
}
