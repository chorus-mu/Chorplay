package io.github.pushkar.chorplay.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_feed_cache")
data class SubscriptionFeedEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val thumbnailUrl: String,
    val duration: Int,
    val viewCount: Long,
    val uploadDate: String,
    val timestamp: Long = System.currentTimeMillis(),
    val channelThumbnailUrl: String,
    val isShort: Boolean = false,
    val isLive: Boolean = false,
    val isUpcoming: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(defaultValue = "") val feedChannelId: String = "",

    @ColumnInfo(defaultValue = "") val collaboratorsJson: String = "",

    @ColumnInfo(defaultValue = "0") val timestampIsExact: Boolean = false,
)
