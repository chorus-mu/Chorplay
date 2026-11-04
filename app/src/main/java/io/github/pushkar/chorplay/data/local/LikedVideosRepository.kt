package io.github.pushkar.chorplay.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.pushkar.chorplay.data.profile.StoreScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val Context.likedVideosDataStore: DataStore<Preferences> by safePreferencesDataStore(
    name = "liked_videos",
    scope = StoreScope.PROFILE_LIBRARY,
)

class LikedVideosRepository private constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        @Volatile
        @Suppress("ktlint:standard:property-naming")
        private var INSTANCE: LikedVideosRepository? = null

        fun getInstance(context: Context): LikedVideosRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LikedVideosRepository(
                    context.likedVideosDataStore,
                ).also { INSTANCE = it }
            }

        private fun videoKey(videoId: String) = stringPreferencesKey("video_$videoId")

        private const val LIKE_STATE_PREFIX = "like_state_"

        private fun likeStateKey(videoId: String) = stringPreferencesKey("$LIKE_STATE_PREFIX$videoId")

        private const val LIKED_VIDEOS_ORDER_KEY = "liked_videos_order"
    }

    suspend fun likeVideo(videoInfo: LikedVideoInfo) {
        dataStore.edit { preferences ->
            val stored = preferences[videoKey(videoInfo.videoId)]?.let(::deserializeVideo)
            preferences[videoKey(videoInfo.videoId)] = serializeVideo(stored?.let(videoInfo::keepingDetailsOf) ?: videoInfo)
            preferences[likeStateKey(videoInfo.videoId)] = "LIKED"

            val currentOrder = preferences[stringPreferencesKey(LIKED_VIDEOS_ORDER_KEY)] ?: ""
            val orderList =
                if (currentOrder.isEmpty()) {
                    mutableListOf()
                } else {
                    currentOrder.split(",").toMutableList()
                }

            if (!orderList.contains(videoInfo.videoId)) {
                orderList.add(0, videoInfo.videoId)
                preferences[stringPreferencesKey(LIKED_VIDEOS_ORDER_KEY)] = orderList.joinToString(",")
            }
        }
    }

    suspend fun dislikeVideo(videoId: String) {
        dataStore.edit { preferences ->
            preferences[likeStateKey(videoId)] = "DISLIKED"

            val currentOrder = preferences[stringPreferencesKey(LIKED_VIDEOS_ORDER_KEY)] ?: ""
            if (currentOrder.isNotEmpty()) {
                val orderList = currentOrder.split(",").toMutableList()
                orderList.remove(videoId)
                preferences[stringPreferencesKey(LIKED_VIDEOS_ORDER_KEY)] = orderList.joinToString(",")
            }
        }
    }

    suspend fun dislikedVideoIds(): Set<String> =
        dataStore.data
            .first()
            .asMap()
            .mapNotNullTo(HashSet()) { (key, value) ->
                key.name.removePrefix(LIKE_STATE_PREFIX).takeIf { value == "DISLIKED" && key.name.startsWith(LIKE_STATE_PREFIX) }
            }

    fun getLikeState(videoId: String): Flow<String?> =
        dataStore.data.map { preferences ->
            preferences[likeStateKey(videoId)]
        }

    suspend fun removeLikeState(videoId: String) {
        dataStore.edit { preferences ->
            preferences.remove(likeStateKey(videoId))

            // Remove from liked videos list
            val currentOrder = preferences[stringPreferencesKey(LIKED_VIDEOS_ORDER_KEY)] ?: ""
            if (currentOrder.isNotEmpty()) {
                val orderList = currentOrder.split(",").toMutableList()
                orderList.remove(videoId)
                preferences[stringPreferencesKey(LIKED_VIDEOS_ORDER_KEY)] = orderList.joinToString(",")
            }
        }
    }

    suspend fun updateDetails(videoInfo: LikedVideoInfo) {
        dataStore.edit { preferences ->
            val key = videoKey(videoInfo.videoId)
            val stored = preferences[key]?.let(::deserializeVideo) ?: return@edit
            preferences[key] = serializeVideo(videoInfo.copy(likedAt = stored.likedAt, isMusic = stored.isMusic))
        }
    }

    suspend fun takeLikes(videoIds: Collection<String>): List<LikedVideoInfo> {
        val ids = videoIds.toSet()
        var taken = emptyList<LikedVideoInfo>()
        dataStore.edit { preferences ->
            val order = preferences[stringPreferencesKey(LIKED_VIDEOS_ORDER_KEY)].orEmpty().split(",").filter(String::isNotEmpty)
            taken = order.filter { it in ids }.mapNotNull { id -> preferences[videoKey(id)]?.let(::deserializeVideo) }
            taken.forEach { preferences.remove(likeStateKey(it.videoId)) }
            preferences[stringPreferencesKey(LIKED_VIDEOS_ORDER_KEY)] = order.filterNot { it in ids }.joinToString(",")
        }
        return taken
    }

    suspend fun restoreLikes(likes: List<LikedVideoInfo>) {
        if (likes.isEmpty()) return
        dataStore.edit { preferences -> preferences.insertLikes(likes) }
    }

    suspend fun importLikes(likes: List<LikedVideoInfo>): List<LikedVideoInfo> {
        var added = emptyList<LikedVideoInfo>()
        dataStore.edit { preferences ->
            added = likes.distinctBy { it.videoId }.filter { preferences[likeStateKey(it.videoId)] == null }
            if (added.isNotEmpty()) preferences.insertLikes(added)
        }
        return added
    }

    private fun MutablePreferences.insertLikes(likes: List<LikedVideoInfo>) {
        val orderKey = stringPreferencesKey(LIKED_VIDEOS_ORDER_KEY)
        val order = this[orderKey].orEmpty().split(",").filter(String::isNotEmpty)
        likes.forEach { like ->
            val stored = this[videoKey(like.videoId)]?.let(::deserializeVideo)
            this[videoKey(like.videoId)] = serializeVideo(stored?.let(like::keepingDetailsOf) ?: like)
            this[likeStateKey(like.videoId)] = "LIKED"
        }
        val likedAt = likes.associate { it.videoId to it.likedAt }
        val merged = (order + likes.map { it.videoId }).distinct()
        val dated = merged.map { id -> id to (likedAt[id] ?: this[videoKey(id)]?.let(::deserializeVideo)?.likedAt ?: 0L) }
        this[orderKey] = restoredOrder(order, dated).joinToString(",")
    }

    fun getAllLikedVideos(): Flow<List<LikedVideoInfo>> =
        dataStore.data.map { preferences ->
            val orderString = preferences[stringPreferencesKey(LIKED_VIDEOS_ORDER_KEY)] ?: ""
            if (orderString.isEmpty()) {
                emptyList()
            } else {
                orderString
                    .split(",")
                    .mapNotNull { videoId -> preferences[videoKey(videoId)]?.let(::deserializeVideo) }
                    .newestLikeFirst()
            }
        }

    fun getLikedVideosFlow(): Flow<List<LikedVideoInfo>> = getAllLikedVideos()

    private fun serializeVideo(video: LikedVideoInfo): String = LikeJson.encodeToString(LikedVideoInfo.serializer(), video)
}

private val LikeJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

internal fun deserializeVideo(data: String): LikedVideoInfo? {
    if (data.startsWith("{")) return runCatching { LikeJson.decodeFromString(LikedVideoInfo.serializer(), data) }.getOrNull()
    val parts = data.split("|")
    if (parts.size < LEGACY_MIN_PARTS) return null
    val hasMusicFlag = parts.last() == "true" || parts.last() == "false"
    val likedAtIndex = if (hasMusicFlag) parts.lastIndex - 1 else parts.lastIndex
    val likedAt = parts[likedAtIndex].toLongOrNull() ?: return null
    val middle = parts.subList(1, likedAtIndex)
    val thumbnailIndex = middle.indexOfFirst { it.startsWith("http") }.takeIf { it >= 0 } ?: 1.coerceAtMost(middle.lastIndex)
    return LikedVideoInfo(
        videoId = parts[0],
        title = middle.subList(0, thumbnailIndex).joinToString("|"),
        thumbnail = middle.getOrElse(thumbnailIndex) { "" },
        channelName = middle.drop(thumbnailIndex + 1).joinToString("|"),
        likedAt = likedAt,
        isMusic = hasMusicFlag && parts.last().toBoolean(),
    )
}

private const val LEGACY_MIN_PARTS = 5

internal fun restoredOrder(
    order: List<String>,
    dated: List<Pair<String, Long>>,
): List<String> {
    val present = order.toHashSet()
    val result = order.toMutableList()
    val likedAt = dated.toMap()
    dated.filter { (id, _) -> id !in present }.sortedByDescending { it.second }.forEach { (id, at) ->
        val index = result.indexOfFirst { (likedAt[it] ?: 0L) < at }
        if (index < 0) result.add(id) else result.add(index, id)
    }
    return result
}

internal fun List<LikedVideoInfo>.newestLikeFirst(): List<LikedVideoInfo> = sortedByDescending { it.likedAt }

@Serializable
data class LikedVideoInfo(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val likedAt: Long = System.currentTimeMillis(),
    val isMusic: Boolean = false,
    val channelId: String? = null,
    val durationSeconds: Int = 0,
) {

    fun keepingDetailsOf(stored: LikedVideoInfo): LikedVideoInfo =
        copy(
            channelId = channelId?.takeIf(String::isNotBlank) ?: stored.channelId,
            durationSeconds = durationSeconds.takeIf { it > 0 } ?: stored.durationSeconds,
        )
}
