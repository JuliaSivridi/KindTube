package io.github.juliasivridi.kindtube.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.juliasivridi.kindtube.domain.model.Video
import io.github.juliasivridi.kindtube.domain.repository.FeedRepository
import io.github.juliasivridi.kindtube.domain.repository.VideoRepository
import io.github.juliasivridi.kindtube.domain.usecase.AddToHistoryUseCase
import io.github.juliasivridi.kindtube.domain.usecase.BlockChannelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val videoRepository: VideoRepository,
    private val feedRepository: FeedRepository,
    private val addToHistoryUseCase: AddToHistoryUseCase,
    private val blockChannelUseCase: BlockChannelUseCase,
) : ViewModel() {

    val videoId: String = checkNotNull(savedStateHandle["videoId"])

    private val _video = MutableStateFlow<Video?>(null)
    val video: StateFlow<Video?> = _video.asStateFlow()

    /**
     * Related videos = 2–4 from the current channel + videos from other
     * subscribed channels taken from the pre-built feed (no extra API calls).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val relatedVideos: StateFlow<List<Video>> = _video
        .filterNotNull()
        .flatMapLatest { v ->
            combine(
                videoRepository.getChannelVideos(v.channelId, ""),
                feedRepository.getFeed(),
            ) { channelVideos, feedVideos ->
                // 2–4 from current channel (excluding current video)
                val fromSameChannel = channelVideos
                    .filter { it.id != videoId }
                    .shuffled()
                    .take(4)

                // From other channels in the feed (excluding current video & channel)
                val fromOtherChannels = feedVideos
                    .filter { it.id != videoId && it.channelId != v.channelId }
                    .shuffled()

                (fromSameChannel + fromOtherChannels).take(20)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadVideo()
    }

    private fun loadVideo() {
        viewModelScope.launch {
            videoRepository.getVideoById(videoId)
                .onSuccess { video ->
                    _video.value = video
                    addToHistoryUseCase(video)
                }
        }
    }

    fun blockChannel() {
        val v = _video.value ?: return
        viewModelScope.launch { blockChannelUseCase(v) }
    }
}
