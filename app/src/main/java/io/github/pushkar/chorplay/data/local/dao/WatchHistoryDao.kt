package io.github.pushkar.chorplay.data.local.dao

import androidx.room.*
import io.github.pushkar.chorplay.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<WatchHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<WatchHistoryEntity>)

    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun deleteEntry(videoId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()

    @Query("DELETE FROM watch_history WHERE isShort = 1")
    suspend fun clearShorts()

    @Query("SELECT * FROM watch_history WHERE isShort = 0 AND isLocal = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLibraryHistory(limit: Int): Flow<List<WatchHistoryEntity>>

    @Query(
        """
        SELECT * FROM watch_history
        WHERE (:isMusic < 0 OR isMusic = :isMusic)
        AND (:isLocal < 0 OR isLocal = :isLocal)
        AND (timestamp < :beforeTimestamp OR (timestamp = :beforeTimestamp AND videoId > :afterVideoId))
        ORDER BY timestamp DESC, videoId ASC
        LIMIT :limit
    """,
    )
    suspend fun getHistoryPage(
        isMusic: Int,
        isLocal: Int,
        beforeTimestamp: Long,
        afterVideoId: String,
        limit: Int,
    ): List<WatchHistoryEntity>

    @Query(
        """
        SELECT videoId, position, duration, timestamp FROM watch_history
        WHERE (:isMusic < 0 OR isMusic = :isMusic)
        AND (:isLocal < 0 OR isLocal = :isLocal)
        AND (timestamp < :beforeTimestamp OR (timestamp = :beforeTimestamp AND videoId > :afterVideoId))
        ORDER BY timestamp DESC, videoId ASC
        LIMIT :limit
    """,
    )
    suspend fun getProgressPage(
        isMusic: Int,
        isLocal: Int,
        beforeTimestamp: Long,
        afterVideoId: String,
        limit: Int,
    ): List<WatchProgress>

    @Query(
        "SELECT * FROM watch_history WHERE isMusic = 0 AND isLocal = 0 AND (:includeShorts OR isShort = 0) " +
            "ORDER BY timestamp DESC LIMIT :limit",
    )
    suspend fun getRecentVideoHistory(
        limit: Int,
        includeShorts: Boolean,
    ): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE videoId = :videoId")
    fun getEntry(videoId: String): Flow<WatchHistoryEntity?>

    @Query("UPDATE watch_history SET position = :durationMs, duration = :durationMs WHERE videoId = :videoId")
    suspend fun markCompleted(
        videoId: String,
        durationMs: Long,
    )

    @Query("UPDATE watch_history SET position = :position, duration = :duration WHERE videoId = :videoId")
    suspend fun updateProgress(
        videoId: String,
        position: Long,
        duration: Long,
    ): Int

    @Query("UPDATE watch_history SET position = 0 WHERE videoId = :videoId")
    suspend fun clearProgress(videoId: String)

    @Query("SELECT position FROM watch_history WHERE videoId = :videoId")
    suspend fun getPosition(videoId: String): Long?

    @Query("SELECT duration FROM watch_history WHERE videoId = :videoId")
    suspend fun getDuration(videoId: String): Long?

    @Query("SELECT COUNT(*) FROM watch_history")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM watch_history WHERE isMusic = 0 AND isLocal = 0")
    fun getVideoCount(): Flow<Int>

    @Query("SELECT DISTINCT channelId FROM watch_history WHERE isMusic = 0 AND isLocal = 0 AND channelId != ''")
    suspend fun getWatchedChannelIds(): List<String>

    @Query(
        "SELECT channelId, MAX(channelName) AS channelName, COUNT(*) AS videos FROM watch_history " +
            "WHERE isMusic = 0 AND isLocal = 0 AND channelId != '' GROUP BY channelId ORDER BY videos DESC LIMIT :limit",
    )
    suspend fun getChannelVideoCounts(limit: Int): List<ChannelVideoCount>

    @Query("SELECT COUNT(*) FROM watch_history WHERE isShort = 0 AND isLocal = 0")
    fun getLibraryHistoryCount(): Flow<Int>

    @Query("SELECT videoId FROM watch_history WHERE isMusic = 0 AND isLocal = 0")
    suspend fun getAllWatchedVideoIds(): List<String>

    @Query(
        "SELECT videoId, position, duration, timestamp FROM watch_history " +
            "WHERE isMusic = 0 AND isLocal = 0 AND isShort = 1 AND position > 0 AND duration > 0",
    )
    suspend fun readShortProgress(): List<WatchProgress>

    @Query("SELECT videoId, position, duration, timestamp FROM watch_history WHERE videoId = :videoId")
    suspend fun getProgress(videoId: String): WatchProgress?

    @Query(
        "SELECT videoId, position, duration, timestamp FROM watch_history " +
            "WHERE isMusic = 0 AND isLocal = 0 AND videoId IN (:videoIds)",
    )
    suspend fun getProgressFor(videoIds: List<String>): List<WatchProgress>

    @Query(
        """
        SELECT * FROM watch_history
        WHERE isMusic = 0
        AND isShort = 0
        AND isLocal = 0
        AND duration > 0
        AND position > 0
        AND (CAST(position AS REAL) / CAST(duration AS REAL)) < 0.95
        AND (duration - position) > 30000
        AND timestamp = (SELECT MAX(timestamp) FROM watch_history WHERE isMusic = 0 AND isShort = 0 AND isLocal = 0)
        LIMIT 1
    """,
    )
    suspend fun getLatestUnfinishedVideo(): WatchHistoryEntity?

    @Query("UPDATE watch_history SET position = duration WHERE videoId = :videoId")
    suspend fun markAsWatched(videoId: String)

    companion object {
        const val ANY = -1
    }
}

data class WatchProgress(
    val videoId: String,
    val position: Long,
    val duration: Long,
    val timestamp: Long,
)

data class ChannelVideoCount(
    val channelId: String,
    val channelName: String,
    val videos: Int,
)
