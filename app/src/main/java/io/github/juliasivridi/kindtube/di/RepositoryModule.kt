package io.github.juliasivridi.kindtube.di

import io.github.juliasivridi.kindtube.data.repository.BlockedChannelRepositoryImpl
import io.github.juliasivridi.kindtube.data.repository.CuratedChannelRepositoryImpl
import io.github.juliasivridi.kindtube.data.repository.FeedRepositoryImpl
import io.github.juliasivridi.kindtube.data.repository.HistoryRepositoryImpl
import io.github.juliasivridi.kindtube.data.repository.SearchRepositoryImpl
import io.github.juliasivridi.kindtube.data.repository.VideoRepositoryImpl
import io.github.juliasivridi.kindtube.domain.repository.BlockedChannelRepository
import io.github.juliasivridi.kindtube.domain.repository.CuratedChannelRepository
import io.github.juliasivridi.kindtube.domain.repository.FeedRepository
import io.github.juliasivridi.kindtube.domain.repository.HistoryRepository
import io.github.juliasivridi.kindtube.domain.repository.SearchRepository
import io.github.juliasivridi.kindtube.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindFeedRepository(impl: FeedRepositoryImpl): FeedRepository

    @Binds @Singleton
    abstract fun bindCuratedChannelRepository(impl: CuratedChannelRepositoryImpl): CuratedChannelRepository

    @Binds @Singleton
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

    @Binds @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds @Singleton
    abstract fun bindBlockedChannelRepository(impl: BlockedChannelRepositoryImpl): BlockedChannelRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}
