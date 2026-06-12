package io.github.juliasivridi.kindtube.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PlaylistItemListDto(
    @SerializedName("items") val items: List<PlaylistItemDto>?,
    @SerializedName("nextPageToken") val nextPageToken: String?,
    @SerializedName("pageInfo") val pageInfo: PageInfoDto?,
)

data class PlaylistItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("snippet") val snippet: PlaylistItemSnippetDto?,
    @SerializedName("contentDetails") val contentDetails: PlaylistItemContentDetailsDto?,
)

data class PlaylistItemSnippetDto(
    @SerializedName("title") val title: String?,
    @SerializedName("thumbnails") val thumbnails: ThumbnailsDto?,
    @SerializedName("channelId") val channelId: String?,
    @SerializedName("channelTitle") val channelTitle: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("resourceId") val resourceId: PlaylistResourceIdDto?,
)

data class PlaylistResourceIdDto(
    @SerializedName("videoId") val videoId: String?,
)

data class PlaylistItemContentDetailsDto(
    @SerializedName("videoId") val videoId: String?,
)
