package io.github.juliasivridi.kindtube.ui.channel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.juliasivridi.kindtube.domain.model.Video
import io.github.juliasivridi.kindtube.domain.repository.VideoRepository
import io.github.juliasivridi.kindtube.util.ErrorType
import io.github.juliasivridi.kindtube.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val videoRepository: VideoRepository,
) : ViewModel() {

    val channelId: String = checkNotNull(savedStateHandle["channelId"])
    val uploadsPlaylistId: String = checkNotNull(savedStateHandle["uploadsPlaylistId"])

    private val _uiState = MutableStateFlow<UiState<List<Video>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Video>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        observeVideos()
        refresh()
    }

    private fun observeVideos() {
        videoRepository.getChannelVideos(channelId, uploadsPlaylistId)
            .onEach { videos ->
                _uiState.value = if (videos.isEmpty()) UiState.Empty else UiState.Success(videos)
            }
            .catch { _uiState.value = UiState.Error(ErrorType.GENERIC) }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            videoRepository.refreshChannelVideos(channelId, uploadsPlaylistId)
            _isRefreshing.value = false
        }
    }
}
