package io.github.juliasivridi.kindtube.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_videos")
data class FeedVideoEntity(
    @PrimaryKey val videoId: String,
    val position: Int,
)
