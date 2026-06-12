package io.github.juliasivridi.kindtube.data.remote.api

import io.github.juliasivridi.kindtube.data.remote.dto.ChannelListDto
import io.github.juliasivridi.kindtube.data.remote.dto.PlaylistItemListDto
import io.github.juliasivridi.kindtube.data.remote.dto.SearchListDto
import io.github.juliasivridi.kindtube.data.remote.dto.VideoListDto
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("channels")
    suspend fun getChannels(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("id") ids: String,
    ): ChannelListDto

    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("pageToken") pageToken: String? = null,
    ): PlaylistItemListDto

    @GET("videos")
    suspend fun getVideos(
        @Query("part") part: String = "snippet,contentDetails,status",
        @Query("id") ids: String,
    ): VideoListDto

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("safeSearch") safeSearch: String = "strict",
        @Query("maxResults") maxResults: Int = 25,
        @Query("pageToken") pageToken: String? = null,
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
    ): SearchListDto

    @GET("search")
    suspend fun searchChannels(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "channel",
        @Query("maxResults") maxResults: Int = 10,
    ): SearchListDto
}
