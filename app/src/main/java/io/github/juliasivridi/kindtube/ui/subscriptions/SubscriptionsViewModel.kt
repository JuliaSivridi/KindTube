package io.github.juliasivridi.kindtube.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.juliasivridi.kindtube.domain.model.CuratedChannel
import io.github.juliasivridi.kindtube.domain.repository.CuratedChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val repository: CuratedChannelRepository,
) : ViewModel() {

    val channels: StateFlow<List<CuratedChannel>> = repository.getChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CuratedChannel>>(emptyList())
    val searchResults: StateFlow<List<CuratedChannel>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private var searchJob: Job? = null

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _searchResults.value = emptyList()
        _searchError.value = null
        searchJob?.cancel()
        if (query.isBlank()) return
        searchJob = viewModelScope.launch {
            delay(600)
            _isSearching.value = true
            repository.searchChannels(query)
                .onSuccess { _searchResults.value = it }
                .onFailure { _searchError.value = "Не удалось выполнить поиск" }
            _isSearching.value = false
        }
    }

    fun addChannel(channel: CuratedChannel) {
        viewModelScope.launch {
            repository.addChannel(channel)
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
    }

    /** Called after parental control dialog succeeds */
    fun removeChannel(channelId: String) {
        viewModelScope.launch { repository.removeChannel(channelId) }
    }
}
