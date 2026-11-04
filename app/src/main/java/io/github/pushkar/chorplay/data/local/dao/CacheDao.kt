package io.github.pushkar.chorplay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.pushkar.chorplay.data.local.entity.SubscriptionFeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheDao {
    @Query("SELECT * FROM subscription_feed_cache ORDER BY timestamp DESC LIMIT 1500")
    fun getSubscriptionFeed(): Flow<List<SubscriptionFeedEntity>>

    @Query("SELECT COUNT(*) FROM subscription_feed_cache")
    suspend fun getSubscriptionFeedCount(): Int

    @Query("SELECT MAX(cachedAt) FROM subscription_feed_cache")
    suspend fun getLatestCachedAt(): Long?

    @Query(
        "SELECT * FROM subscription_feed_cache WHERE channelId IN (:channelIds) OR feedChannelId IN (:channelIds) " +
                "ORDER BY timestamp DESC",
    )
    fun observeSubscriptionFeedForChannels(channelIds: List<String>): Flow<List<SubscriptionFeedEntity>>

    @Query("SELECT DISTINCT channelId FROM subscription_feed_cache WHERE feedChannelId = ''")
    suspend fun subscriptionUploadChannelIds(): List<String>

    @Query("SELECT * FROM subscription_feed_cache WHERE channelId IN (:channelIds)")
    suspend fun getSubscriptionFeedForChannels(channelIds: List<String>): List<SubscriptionFeedEntity>

    @Query("SELECT * FROM subscription_feed_cache WHERE videoId = :videoId")
    suspend fun getSubscriptionFeedRow(videoId: String): SubscriptionFeedEntity?

    @Query("SELECT DISTINCT channelId, channelName FROM subscription_feed_cache WHERE channelId IN (:channelIds) AND channelName != ''")
    suspend fun subscriptionFeedChannelNames(channelIds: List<String>): List<ChannelNameRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptionFeed(videos: List<SubscriptionFeedEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubscriptionFeedIfAbsent(videos: List<SubscriptionFeedEntity>)

    @Query(
        """
        UPDATE subscription_feed_cache
        SET title = :title,
            channelName = :channelName,
            channelId = :channelId,
            thumbnailUrl = :thumbnailUrl,
            duration = :duration,
            viewCount = :viewCount,
            isLive = :isLive,
            isUpcoming = :isUpcoming,
            uploadDate = :uploadDate,
            timestamp = :timestamp,
            timestampIsExact = :timestampIsExact
        WHERE videoId = :videoId
        """,
    )
    suspend fun updateSubscriptionFeedMetadata(
        videoId: String,
        title: String,
        channelName: String,
        channelId: String,
        thumbnailUrl: String,
        duration: Int,
        viewCount: Long,
        isLive: Boolean,
        isUpcoming: Boolean,
        uploadDate: String,
        timestamp: Long,
        timestampIsExact: Boolean,
    )

    @Query("DELETE FROM subscription_feed_cache WHERE channelId = :channelId OR feedChannelId = :channelId")
    suspend fun deleteSubscriptionFeedForChannel(channelId: String)

    @Query("DELETE FROM subscription_feed_cache WHERE channelId IN (:channelIds) AND feedChannelId = ''")
    suspend fun deleteSubscriptionFeedForChannels(channelIds: List<String>)

    @Query("DELETE FROM subscription_feed_cache WHERE feedChannelId = :feedChannelId")
    suspend fun deleteCollaborationsFrom(feedChannelId: String)

    @Query("DELETE FROM subscription_feed_cache WHERE feedChannelId != ''")
    suspend fun deleteCollaborations()

    @Query(
        """
        DELETE FROM subscription_feed_cache
        WHERE isUpcoming = 0 AND timestamp > 0 AND timestamp < :cutoffMillis
        """,
    )
    suspend fun pruneSubscriptionFeedOlderThan(cutoffMillis: Long)

    @Query("DELETE FROM subscription_feed_cache")
    suspend fun clearSubscriptionFeed()

    @Transaction
    suspend fun replaceCollaborations(
        feedChannelId: String,
        rows: List<SubscriptionFeedEntity>,
    ) {
        deleteCollaborationsFrom(feedChannelId)
        insertSubscriptionFeedIfAbsent(rows)
    }

}
