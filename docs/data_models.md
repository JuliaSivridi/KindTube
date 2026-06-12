# MyKidTube — Модели данных

## 1. Доменные модели (domain/model/)

```kotlin
data class Video(
    val id: String,                  // YouTube video ID
    val title: String,
    val thumbnailUrl: String,
    val channelId: String,
    val channelTitle: String,
    val channelAvatarUrl: String?,
    val durationSeconds: Int?,       // из videos.list (contentDetails)
    val publishedAt: Long,           // epoch ms
)

data class Channel(
    val id: String,                  // YouTube channel ID
    val title: String,
    val avatarUrl: String?,
    val uploadsPlaylistId: String,   // нужен для получения видео канала
    val isBlocked: Boolean = false,
)

data class Subscription(
    val channelId: String,
    val channel: Channel,
    val subscribedAt: Long,          // epoch ms
)

data class HistoryEntry(
    val id: Long = 0,                // автоинкремент Room
    val video: Video,
    val watchedAt: Long,             // epoch ms
)

data class SearchResult(
    val videos: List<Video>,
    val nextPageToken: String?,
)
```

---

## 2. Room — Entity классы (data/local/db/)

```kotlin
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val thumbnailUrl: String,
    val channelId: String,
    val channelTitle: String,
    val channelAvatarUrl: String?,
    val durationSeconds: Int?,
    val publishedAt: Long,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val title: String,
    val avatarUrl: String?,
    val uploadsPlaylistId: String,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val channelId: String,
    val subscribedAt: Long,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "feed_items")
data class FeedItemEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val videoId: String,
    val position: Int,               // порядок в ленте
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val watchedAt: Long,
)

@Entity(tableName = "blocked_channels")
data class BlockedChannelEntity(
    @PrimaryKey val channelId: String,
    val channelTitle: String,        // денормализован для отображения без доп. запроса
    val channelAvatarUrl: String?,
    val blockedAt: Long = System.currentTimeMillis(),
)
```

---

## 3. DAO интерфейсы (data/local/db/)

```kotlin
@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Query("SELECT * FROM videos WHERE channelId = :channelId")
    fun getByChannel(channelId: String): Flow<List<VideoEntity>>
}

@Dao
interface FeedDao {
    @Query("""
        SELECT v.* FROM feed_items fi
        INNER JOIN videos v ON fi.videoId = v.id
        WHERE fi.cachedAt > :since
        ORDER BY fi.position ASC
    """)
    fun getFeed(since: Long): Flow<List<VideoEntity>>

    @Transaction
    suspend fun replaceFeed(items: List<FeedItemEntity>, videos: List<VideoEntity>) {
        clearFeed()
        insertFeedItems(items)
        // videos сохраняются через VideoDao
    }

    @Query("DELETE FROM feed_items")
    suspend fun clearFeed()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedItems(items: List<FeedItemEntity>)

    @Query("SELECT MAX(cachedAt) FROM feed_items")
    suspend fun getLastCachedAt(): Long?
}

@Dao
interface SubscriptionDao {
    @Query("""
        SELECT c.* FROM subscriptions s
        INNER JOIN channels c ON s.channelId = c.id
        WHERE s.cachedAt > :since
        ORDER BY c.title ASC
    """)
    fun getSubscribedChannels(since: Long): Flow<List<ChannelEntity>>

    @Query("SELECT MAX(cachedAt) FROM subscriptions")
    suspend fun getLastCachedAt(): Long?

    @Transaction
    suspend fun replaceSubscriptions(subs: List<SubscriptionEntity>, channels: List<ChannelEntity>)

    @Query("DELETE FROM subscriptions WHERE channelId = :channelId")
    suspend fun delete(channelId: String)
}

@Dao
interface HistoryDao {
    @Query("""
        SELECT v.* FROM history h
        INNER JOIN videos v ON h.videoId = v.id
        ORDER BY h.watchedAt DESC
        LIMIT 200
    """)
    fun getHistory(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface BlockedChannelDao {
    @Query("SELECT * FROM blocked_channels ORDER BY blockedAt DESC")
    fun getAll(): Flow<List<BlockedChannelEntity>>

    @Query("SELECT channelId FROM blocked_channels")
    suspend fun getAllIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun block(channel: BlockedChannelEntity)

    @Query("DELETE FROM blocked_channels WHERE channelId = :channelId")
    suspend fun unblock(channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_channels WHERE channelId = :channelId)")
    suspend fun isBlocked(channelId: String): Boolean
}
```

---

## 4. YouTube API DTO (data/remote/dto/)

### Subscriptions response
```kotlin
data class SubscriptionsResponse(
    val items: List<SubscriptionItem>,
    val nextPageToken: String?,
    val pageInfo: PageInfo,
)

data class SubscriptionItem(
    val id: String,
    val snippet: SubscriptionSnippet,
)

data class SubscriptionSnippet(
    val title: String,
    val resourceId: ResourceId,   // resourceId.channelId
    val thumbnails: Thumbnails,
    val publishedAt: String,      // ISO 8601
)
```

### PlaylistItems response (uploads)
```kotlin
data class PlaylistItemsResponse(
    val items: List<PlaylistItem>,
    val nextPageToken: String?,
    val pageInfo: PageInfo,
)

data class PlaylistItem(
    val snippet: PlaylistItemSnippet,
)

data class PlaylistItemSnippet(
    val title: String,
    val resourceId: ResourceId,   // resourceId.videoId
    val thumbnails: Thumbnails,
    val channelId: String,
    val channelTitle: String,
    val publishedAt: String,
)
```

### Channels response
```kotlin
data class ChannelsResponse(
    val items: List<ChannelItem>,
)

data class ChannelItem(
    val id: String,
    val snippet: ChannelSnippet,
    val contentDetails: ChannelContentDetails,
)

data class ChannelSnippet(
    val title: String,
    val thumbnails: Thumbnails,
)

data class ChannelContentDetails(
    val relatedPlaylists: RelatedPlaylists,  // .uploads = playlist ID
)
```

### Search response
```kotlin
data class SearchResponse(
    val items: List<SearchItem>,
    val nextPageToken: String?,
    val pageInfo: PageInfo,
)

data class SearchItem(
    val id: SearchItemId,          // id.videoId
    val snippet: SearchSnippet,
)

data class SearchSnippet(
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val thumbnails: Thumbnails,
    val publishedAt: String,
)
```

### Общие типы
```kotlin
data class ResourceId(
    val kind: String,
    val videoId: String? = null,
    val channelId: String? = null,
)

data class Thumbnails(
    val default: Thumbnail?,
    val medium: Thumbnail?,
    val high: Thumbnail?,
    val maxres: Thumbnail?,
)

data class Thumbnail(
    val url: String,
    val width: Int?,
    val height: Int?,
)

data class PageInfo(
    val totalResults: Int,
    val resultsPerPage: Int,
)

data class SearchItemId(
    val kind: String,
    val videoId: String?,
)
```

---

## 5. Retrofit API интерфейс (data/remote/api/)

```kotlin
interface YouTubeApiService {

    @GET("subscriptions")
    suspend fun getSubscriptions(
        @Query("part") part: String = "snippet",
        @Query("mine") mine: Boolean = true,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
    ): SubscriptionsResponse

    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part") part: String = "snippet",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
    ): PlaylistItemsResponse

    @GET("channels")
    suspend fun getChannels(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("id") ids: String,       // comma-separated channel IDs
    ): ChannelsResponse

    @GET("search")
    suspend fun search(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("safeSearch") safeSearch: String = "strict",
        @Query("maxResults") maxResults: Int = 20,
        @Query("pageToken") pageToken: String? = null,
    ): SearchResponse

    @HTTP(method = "DELETE", path = "subscriptions", hasBody = false)
    suspend fun deleteSubscription(
        @Query("id") subscriptionId: String,
    )
}
```

---

## 6. SharedPreferences ключи (data/local/prefs/)

```kotlin
object PrefKeys {
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val TOKEN_EXPIRY = "token_expiry_ms"
    const val ACCOUNT_EMAIL = "account_email"
    const val ACCOUNT_NAME = "account_name"
    const val ACCOUNT_AVATAR_URL = "account_avatar_url"
    const val QUOTA_EXCEEDED_DATE = "quota_exceeded_date"  // ISO date string
}
```

---

## 7. Маппинг DTO → Domain

```kotlin
// Пример маппера
fun PlaylistItemSnippet.toVideo(): Video = Video(
    id = resourceId.videoId ?: error("videoId is null"),
    title = title,
    thumbnailUrl = thumbnails.high?.url
        ?: thumbnails.medium?.url
        ?: thumbnails.default?.url
        ?: "",
    channelId = channelId,
    channelTitle = channelTitle,
    channelAvatarUrl = null,   // нужен отдельный запрос channels.list
    durationSeconds = null,    // нужен отдельный запрос videos.list
    publishedAt = publishedAt.toEpochMs(),
)

fun ChannelItem.toChannel(): Channel = Channel(
    id = id,
    title = snippet.title,
    avatarUrl = snippet.thumbnails.high?.url ?: snippet.thumbnails.default?.url,
    uploadsPlaylistId = contentDetails.relatedPlaylists.uploads,
)
```
