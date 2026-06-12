package io.github.juliasivridi.kindtube.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.github.juliasivridi.kindtube.domain.model.BlockedChannel
import io.github.juliasivridi.kindtube.domain.model.CuratedChannel
import io.github.juliasivridi.kindtube.domain.repository.BlockedChannelRepository
import io.github.juliasivridi.kindtube.domain.repository.CuratedChannelRepository
import io.github.juliasivridi.kindtube.domain.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// JSON schema for the channel backup file
private data class ChannelsBackup(
    @SerializedName("app") val app: String = "KindTube",
    @SerializedName("version") val version: Int = 1,
    @SerializedName("channels") val channels: List<BackupChannel>,
)

private data class BackupChannel(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("uploadsPlaylistId") val uploadsPlaylistId: String?,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val blockedChannelRepository: BlockedChannelRepository,
    private val curatedChannelRepository: CuratedChannelRepository,
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

    /** Serialize all curated channels to a backup JSON string. */
    suspend fun buildExportJson(): String {
        val channels = curatedChannelRepository.getChannels().first()
        val backup = ChannelsBackup(
            channels = channels.map {
                BackupChannel(
                    id = it.id,
                    title = it.title,
                    avatarUrl = it.avatarUrl,
                    uploadsPlaylistId = it.uploadsPlaylistId,
                )
            },
        )
        return Gson().toJson(backup)
    }

    /**
     * Import channels from a backup JSON string. Existing channels are kept
     * (insert is upsert by channel id). Returns the number of imported
     * channels, or -1 if the file could not be parsed.
     */
    suspend fun importChannelsJson(json: String): Int {
        val backup = try {
            Gson().fromJson(json, ChannelsBackup::class.java)
        } catch (e: Exception) {
            null
        }
        val channels = backup?.channels ?: return -1
        var imported = 0
        for (ch in channels) {
            val id = ch.id ?: continue
            val title = ch.title ?: continue
            // uploadsPlaylistId is derivable from the channel id (UC… → UU…)
            val uploads = ch.uploadsPlaylistId
                ?: if (id.startsWith("UC")) "UU" + id.removePrefix("UC") else continue
            curatedChannelRepository.addChannel(
                CuratedChannel(
                    id = id,
                    title = title,
                    avatarUrl = ch.avatarUrl,
                    uploadsPlaylistId = uploads,
                ),
            )
            imported++
        }
        if (imported > 0) feedRepository.refreshFeed()
        return imported
    }
}
