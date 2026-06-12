package io.github.juliasivridi.kindtube.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.juliasivridi.kindtube.domain.model.BlockedChannel
import io.github.juliasivridi.kindtube.domain.repository.BlockedChannelRepository
import io.github.juliasivridi.kindtube.domain.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val blockedChannelRepository: BlockedChannelRepository,
    private val feedRepository: FeedRepository,
) : ViewModel() {

    val blockedChannels: StateFlow<List<BlockedChannel>> = blockedChannelRepository
        .getBlockedChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unblockChannel(channelId: String) {
        viewModelScope.launch { blockedChannelRepository.unblockChannel(channelId) }
    }

    fun clearCache() {
        viewModelScope.launch { feedRepository.refreshFeed() }
    }
}
