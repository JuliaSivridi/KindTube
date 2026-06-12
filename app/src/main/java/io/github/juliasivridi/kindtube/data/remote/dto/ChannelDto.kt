package io.github.juliasivridi.kindtube.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChannelListDto(
    @SerializedName("items") val items: List<ChannelItemDto>?,
)

data class ChannelItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("snippet") val snippet: ChannelSnippetDto?,
    @SerializedName("contentDetails") val contentDetails: ChannelContentDetailsDto?,
)

data class ChannelSnippetDto(
    @SerializedName("title") val title: String?,
    @SerializedName("thumbnails") val thumbnails: ThumbnailsDto?,
)

data class ChannelContentDetailsDto(
    @SerializedName("relatedPlaylists") val relatedPlaylists: RelatedPlaylistsDto?,
)

data class RelatedPlaylistsDto(
    @SerializedName("uploads") val uploads: String?,
)

data class ThumbnailsDto(
    @SerializedName("default") val default: ThumbnailDto?,
    @SerializedName("medium") val medium: ThumbnailDto?,
    @SerializedName("high") val high: ThumbnailDto?,
)

data class ThumbnailDto(
    @SerializedName("url") val url: String?,
)

data class PageInfoDto(
    @SerializedName("totalResults") val totalResults: Int?,
    @SerializedName("resultsPerPage") val resultsPerPage: Int?,
)
