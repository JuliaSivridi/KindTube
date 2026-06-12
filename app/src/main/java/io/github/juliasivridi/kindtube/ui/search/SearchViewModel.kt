package io.github.juliasivridi.kindtube.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.juliasivridi.kindtube.domain.model.Video
import io.github.juliasivridi.kindtube.domain.repository.SearchRepository
import io.github.juliasivridi.kindtube.domain.repository.SearchResult
import io.github.juliasivridi.kindtube.util.ErrorType
import io.github.juliasivridi.kindtube.util.UiState
import retrofit2.HttpException
import java.net.UnknownHostException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<Video>>>(UiState.Empty)
    val uiState: StateFlow<UiState<List<Video>>> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var lastResult: SearchResult? = null

    fun onQueryChange(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.isBlank()) {
            _uiState.value = UiState.Empty
            return
        }
        searchJob = viewModelScope.launch {
            delay(500) // debounce
            search(q)
        }
    }

    private suspend fun search(q: String, pageToken: String? = null) {
        if (pageToken == null) _uiState.value = UiState.Loading
        searchRepository.searchVideos(q, pageToken)
            .onSuccess { result ->
                lastResult = result
                val existing = if (pageToken != null && _uiState.value is UiState.Success) {
                    (_uiState.value as UiState.Success<List<Video>>).data
                } else emptyList()
                val combined = existing + result.videos
                _uiState.value = if (combined.isEmpty()) UiState.Empty else UiState.Success(combined)
            }
            .onFailure { e ->
                val errorType = when {
                    e is HttpException && e.code() == 403 -> ErrorType.QUOTA_EXCEEDED
                    e is UnknownHostException -> ErrorType.NO_NETWORK
                    else -> ErrorType.GENERIC
                }
                _uiState.value = UiState.Error(errorType)
            }
    }

    fun loadMore() {
        val token = lastResult?.nextPageToken ?: return
        val q = _query.value.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch { search(q, token) }
    }
}
