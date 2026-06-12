package io.github.juliasivridi.kindtube.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SubscriptionListDto(
    @SerializedName("items") val items: List<SubscriptionItemDto>?,
    @SerializedName("nextPageToken") val nextPageToken: String?,
    @SerializedName("pageInfo") val pageInfo: PageInfoDto?,
)

data class SubscriptionItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("snippet") val snippet: SubscriptionSnippetDto?,
)

data class SubscriptionSnippetDto(
    @SerializedName("title") val title: String?,
    @SerializedName("resourceId") val resourceId: ResourceIdDto?,
    @SerializedName("thumbnails") val thumbnails: ThumbnailsDto?,
)

data class ResourceIdDto(
    @SerializedName("channelId") val channelId: String?,
)
