package io.github.juliasivridi.kindtube.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VideoListDto(
    @SerializedName("items") val items: List<VideoItemDto>?,
)

data class VideoItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("snippet") val snippet: VideoSnippetDto?,
    @SerializedName("contentDetails") val contentDetails: VideoContentDetailsDto?,
    @SerializedName("status") val status: VideoStatusDto?,
)

data class VideoStatusDto(
    @SerializedName("embeddable") val embeddable: Boolean?,
)

data class VideoSnippetDto(
    @SerializedName("title") val title: String?,
    @SerializedName("channelId") val channelId: String?,
    @SerializedName("channelTitle") val channelTitle: String?,
    @SerializedName("thumbnails") val thumbnails: ThumbnailsDto?,
    @SerializedName("publishedAt") val publishedAt: String?,
)

data class VideoContentDetailsDto(
    @SerializedName("duration") val duration: String?, // ISO 8601 e.g. "PT4M13S"
)

data class SearchListDto(
    @SerializedName("items") val items: List<SearchItemDto>?,
    @SerializedName("nextPageToken") val nextPageToken: String?,
    @SerializedName("pageInfo") val pageInfo: PageInfoDto?,
)

data class SearchItemDto(
    @SerializedName("id") val id: SearchItemIdDto?,
    @SerializedName("snippet") val snippet: VideoSnippetDto?,
)

data class SearchItemIdDto(
    @SerializedName("videoId") val videoId: String?,
    @SerializedName("channelId") val channelId: String?,
)
