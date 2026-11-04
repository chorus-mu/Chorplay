package io.github.pushkar.chorplay.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.pushkar.chorplay.data.local.dao.WatchHistoryDao
import io.github.pushkar.chorplay.data.local.dao.WatchProgress
import io.github.pushkar.chorplay.data.local.entity.WatchHistoryEntity
import io.github.pushkar.chorplay.data.localmedia.LocalMediaIds
import io.github.pushkar.chorplay.data.profile.StoreScope
import io.github.pushkar.chorplay.utils.ThumbnailUrlResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private val Context.viewHistoryDataStore: DataStore<Preferences> by safePreferencesDataStore(
    name = "view_history",
    scope = StoreScope.PROFILE_LIBRARY,
)

class ViewHistory private constructor(
    private val context: Context,
) {
    private val database = AppDatabase.profile(context)
    private val dao = database.watchHistoryDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "ViewHistory"
        private const val WATCH_HISTORY_TABLE = "watch_history"

        @Volatile
        private var instance: ViewHistory? = null

        fun getInstance(context: Context): ViewHistory =
            instance ?: synchronized(this) {
                instance ?: ViewHistory(context.applicationContext).also { created ->
                    instance = created
                    created.scope.launch { created.migrateFromDataStoreIfNeeded() }
                }
            }

        private fun positionKey(id: String) = longPreferencesKey("video_${id}_position")

        private fun durationKey(id: String) = longPreferencesKey("video_${id}_duration")

        private fun timestampKey(id: String) = longPreferencesKey("video_${id}_timestamp")

        private fun titleKey(id: String) = stringPreferencesKey("video_${id}_title")

        private fun thumbnailKey(id: String) = stringPreferencesKey("video_${id}_thumbnail")

        private fun channelNameKey(id: String) = stringPreferencesKey("video_${id}_channel_name")

        private fun channelIdKey(id: String) = stringPreferencesKey("video_${id}_channel_id")

        private fun isMusicKey(id: String) = booleanPreferencesKey("video_${id}_is_music")
    }

    suspend fun savePlaybackPosition(
        videoId: String,
        position: Long,
        duration: Long,
        title: String = "",
        thumbnailUrl: String = "",
        channelName: String = "",
        channelId: String = "",
        isMusic: Boolean = false,
        isShort: Boolean = false,
        isLocal: Boolean = LocalMediaIds.isLocal(videoId),
    ) {
        if (PlayerPreferences(context).isWatchHistorySavingBlocked()) return

        val thumbnail = if (isLocal) thumbnailUrl else ThumbnailUrlResolver.normalizeVideoThumbnail(videoId, thumbnailUrl)
        dao.upsert(
            WatchHistoryEntity(
                videoId = videoId,
                position = position,
                duration = duration,
                timestamp = System.currentTimeMillis(),
                title = title,
                thumbnailUrl = thumbnail,
                channelName = channelName,
                channelId = channelId,
                isMusic = isMusic,
                isShort = isShort,
                isLocal = isLocal,
            ),
        )
    }

    suspend fun markCompleted(
        videoId: String,
        durationMs: Long,
    ) {
        if (durationMs <= 0L) return
        if (PlayerPreferences(context).isWatchHistorySavingBlocked()) return
        dao.markCompleted(videoId, durationMs)
    }

    suspend fun clearProgress(videoId: String) {
        dao.clearProgress(videoId)
    }

    suspend fun getSavedPosition(videoId: String): Long = dao.getPosition(videoId) ?: 0L

    suspend fun getSavedDuration(videoId: String): Long = dao.getDuration(videoId) ?: 0L

    suspend fun touchHistoryEntry(
        videoId: String,
        title: String = "",
        thumbnailUrl: String = "",
        channelName: String = "",
        channelId: String = "",
        duration: Long = 0L,
        isShort: Boolean = false,
    ) {
        if (PlayerPreferences(context).isWatchHistorySavingBlocked()) return

        val thumbnail = ThumbnailUrlResolver.normalizeVideoThumbnail(videoId, thumbnailUrl)
        val existingPosition = dao.getPosition(videoId) ?: 0L
        val resolvedDuration = duration.takeIf { it > 0 } ?: dao.getDuration(videoId) ?: 0L
        val existing = if (title.isBlank() || channelName.isBlank() || channelId.isBlank()) dao.getEntry(videoId).first() else null
        dao.upsert(
            WatchHistoryEntity(
                videoId = videoId,
                position = existingPosition,
                duration = resolvedDuration,
                timestamp = System.currentTimeMillis(),
                title = title.ifBlank { existing?.title.orEmpty() },
                thumbnailUrl = thumbnail,
                channelName = channelName.ifBlank { existing?.channelName.orEmpty() },
                channelId = channelId.ifBlank { existing?.channelId.orEmpty() },
                isMusic = false,
                isShort = isShort,
                isLocal = LocalMediaIds.isLocal(videoId),
            ),
        )
    }

    suspend fun bulkSaveHistoryEntries(entries: List<VideoHistoryEntry>) {
        if (entries.isEmpty()) return
        val entities =
            entries.map { entry ->
                WatchHistoryEntity(
                    videoId = entry.videoId,
                    position = entry.position,
                    duration = entry.duration,
                    timestamp = entry.timestamp,
                    title = entry.title,
                    thumbnailUrl = ThumbnailUrlResolver.normalizeVideoThumbnail(entry.videoId, entry.thumbnailUrl),
                    channelName = entry.channelName,
                    channelId = entry.channelId,
                    isMusic = entry.isMusic,
                    isShort = entry.isShort,
                )
            }
        dao.insertAll(entities)
    }

    suspend fun clearVideoHistory(videoId: String) {
        dao.deleteEntry(videoId)
    }

    suspend fun markAsWatched(videoId: String) {
        dao.markAsWatched(videoId)
    }

    suspend fun clearAllHistory() {
        dao.clearAll()
    }

    suspend fun clearShortsHistory() {
        dao.clearShorts()
    }

    fun getPlaybackPosition(videoId: String): Flow<Long> = dao.getEntry(videoId).map { it?.position ?: 0L }

    fun getVideoHistory(videoId: String): Flow<VideoHistoryEntry?> = dao.getEntry(videoId).map { it?.toDomain() }

    fun getAllHistory(): Flow<List<VideoHistoryEntry>> = observeHistory()

    fun getRecentLibraryHistory(limit: Int): Flow<List<VideoHistoryEntry>> =
        dao.getRecentLibraryHistory(limit).map { list -> list.map { it.toDomain() } }

    fun getVideoHistoryFlow(): Flow<List<VideoHistoryEntry>> = observeHistory(isMusic = 0, isLocal = 0)

    fun getLocalHistoryFlow(): Flow<List<VideoHistoryEntry>> = observeHistory(isLocal = 1)

    fun getAllWatchProgress(): Flow<List<WatchProgress>> = observe { dao.readProgress() }

    fun getVideoWatchProgress(): Flow<List<WatchProgress>> = observe { dao.readProgress(isMusic = 0, isLocal = 0) }

    suspend fun getRecentVideoHistory(
        limit: Int,
        includeShorts: Boolean,
    ): List<VideoHistoryEntry> = dao.getRecentVideoHistory(limit, includeShorts).map { it.toDomain() }

    fun observeRecentVideoHistory(
        limit: Int,
        includeShorts: Boolean,
    ): Flow<List<VideoHistoryEntry>> = observe { getRecentVideoHistory(limit, includeShorts) }

    private fun observeHistory(
        isMusic: Int = WatchHistoryDao.ANY,
        isLocal: Int = WatchHistoryDao.ANY,
    ): Flow<List<VideoHistoryEntry>> = observe { dao.readHistory(isMusic, isLocal).map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> observe(read: suspend () -> T): Flow<T> =
        database.invalidationTracker
            .createFlow(WATCH_HISTORY_TABLE)
            .mapLatest { read() }
            .flowOn(Dispatchers.IO)

    suspend fun getShortProgress(): List<WatchProgress> = dao.readShortProgress()

    suspend fun getWatchProgress(videoId: String): WatchProgress? = dao.getProgress(videoId)

    fun getVideoCount(): Flow<Int> = dao.getVideoCount()

    fun getLibraryHistoryCount(): Flow<Int> = dao.getLibraryHistoryCount()

    suspend fun getLatestUnfinishedVideo() = dao.getLatestUnfinishedVideo()

    private suspend fun migrateFromDataStoreIfNeeded() {
        try {
            val roomCount = dao.getCount().first()
            if (roomCount > 0) return

            val prefs = context.viewHistoryDataStore.data.first()
            val videoIds = mutableSetOf<String>()
            prefs.asMap().keys.forEach { key ->
                val name = key.name
                if (name.startsWith("video_") && name.endsWith("_position")) {
                    videoIds.add(name.removePrefix("video_").removeSuffix("_position"))
                }
            }
            if (videoIds.isEmpty()) return

            Log.i(TAG, "Migrating ${videoIds.size} watch-history entries from DataStore → Room")

            val batchSize = 500
            val batch = mutableListOf<WatchHistoryEntity>()

            for (videoId in videoIds) {
                val position = prefs[positionKey(videoId)] ?: continue
                val duration = prefs[durationKey(videoId)] ?: 0L
                val ts = prefs[timestampKey(videoId)] ?: 0L
                val title = prefs[titleKey(videoId)] ?: ""
                val thumb = ThumbnailUrlResolver.normalizeVideoThumbnail(videoId, prefs[thumbnailKey(videoId)])
                val chName = prefs[channelNameKey(videoId)] ?: ""
                val chId = prefs[channelIdKey(videoId)] ?: ""
                val isMusic = prefs[isMusicKey(videoId)] ?: false

                batch += WatchHistoryEntity(videoId, position, duration, ts, title, thumb, chName, chId, isMusic)

                if (batch.size >= batchSize) {
                    dao.insertAll(batch)
                    batch.clear()
                    yield()
                }
            }
            if (batch.isNotEmpty()) dao.insertAll(batch)

            context.viewHistoryDataStore.edit { it.clear() }
            Log.i(TAG, "DataStore → Room migration complete (${videoIds.size} entries)")
        } catch (e: Exception) {
            Log.e(TAG, "Migration from DataStore failed", e)
        }
    }
}

data class VideoHistoryEntry(
    val videoId: String,
    val position: Long,
    val duration: Long,
    val timestamp: Long,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String = "",
    val channelId: String = "",
    val isMusic: Boolean = false,
    val isShort: Boolean = false,
    val isLocal: Boolean = false,
) {
    val progressPercentage: Float
        get() = if (duration > 0) (position.toFloat() / duration.toFloat()) * 100f else 0f
}
