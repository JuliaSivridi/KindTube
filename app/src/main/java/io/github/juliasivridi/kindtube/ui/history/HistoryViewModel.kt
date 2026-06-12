package io.github.juliasivridi.kindtube.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.juliasivridi.kindtube.domain.model.HistoryEntry
import io.github.juliasivridi.kindtube.domain.repository.HistoryRepository
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
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<HistoryEntry>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<HistoryEntry>>> = _uiState.asStateFlow()

    init {
        historyRepository.getHistory()
            .onEach { entries ->
                _uiState.value = if (entries.isEmpty()) UiState.Empty else UiState.Success(entries)
            }
            .catch { _uiState.value = UiState.Error(ErrorType.GENERIC) }
            .launchIn(viewModelScope)
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
}
