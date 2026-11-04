package io.github.pushkar.chorplay.data.local

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeToSequence
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder
import java.time.Instant

internal data class TakeoutLike(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val likedAt: Long,
    val isMusic: Boolean,
)

internal data class TakeoutLikes(
    val likes: List<TakeoutLike>,
    val totalFound: Int,
)

internal data class TakeoutSearch(
    val query: String,
    val searchedAt: Long,
    val isMusic: Boolean,
)

internal data class TakeoutActivity(
    val likes: TakeoutLikes,
    val searches: List<TakeoutSearch>,
    val watches: List<TakeoutWatch>,
)

@Serializable
private data class MyActivityRecord(
    val header: String? = null,
    val title: String? = null,
    val titleUrl: String? = null,
    val subtitles: List<MyActivitySubtitle>? = null,
    val details: List<JsonElement>? = null,
    val time: String? = null,
)

@Serializable
private data class MyActivitySubtitle(
    val name: String? = null,
    val url: String? = null,
)

private data class LikeDecision(
    val at: Long,
    val like: TakeoutLike?,
)

internal const val MAX_IMPORTED_LIKES = 5_000

private const val LIKED_PREFIX = "Liked "
private const val DISLIKED_PREFIX = "Disliked "
private const val WATCHED_PREFIX = "Watched "
private const val YOUTUBE_MUSIC_HEADER = "YouTube Music"
private const val YOUTUBE_MUSIC_HOST = "music.youtube.com"

private val myActivityJson = Json { ignoreUnknownKeys = true }
private val watchVideoIdPattern = Regex("""[?&]v=([\w-]{11})(?:[&#]|$)""")
private val activityChannelIdPattern = Regex("""/channel/(UC[\w-]{22})""")

internal fun isMyActivityYouTubeEntry(entryName: String): Boolean {
    val segments = entryName.replace('\\', '/').split('/')
    return segments.size >= 3 &&
        segments.none { it.isEmpty() || it == "." || it == ".." } &&
        segments[segments.lastIndex - 1].equals("YouTube", ignoreCase = true) &&
        segments.last().endsWith(".json", ignoreCase = true)
}

internal fun readMyActivityLikes(
    input: InputStream,
    limit: Int = MAX_IMPORTED_LIKES,
): TakeoutLikes = readTakeoutActivity(input, likeLimit = limit, keepWatches = false).likes

@OptIn(ExperimentalSerializationApi::class)
internal fun readTakeoutActivity(
    input: InputStream,
    likeLimit: Int = MAX_IMPORTED_LIKES,
    keepWatches: Boolean = true,
): TakeoutActivity {
    val decisions = HashMap<String, LikeDecision>()
    val searches = mutableListOf<TakeoutSearch>()
    val watches = mutableListOf<TakeoutWatch>()
    myActivityJson
        .decodeToSequence(input, MyActivityRecord.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)
        .forEach { record ->
            if (record.details != null) return@forEach
            val title = record.title ?: return@forEach
            val at = record.time?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: return@forEach
            val isLike = title.startsWith(LIKED_PREFIX)
            if (isLike || title.startsWith(DISLIKED_PREFIX)) {
                val videoId = record.videoId() ?: return@forEach
                val previous = decisions[videoId]
                if (previous == null || previous.at < at) {
                    decisions[videoId] = LikeDecision(at, if (isLike) record.toLike(videoId, title, at) else null)
                }
                return@forEach
            }
            takeoutSearchOf(record.titleUrl)?.let { (query, isMusic) ->
                searches += TakeoutSearch(query, at, isMusic || record.header == YOUTUBE_MUSIC_HEADER)
                return@forEach
            }
            if (keepWatches) record.toWatch(title, at)?.let(watches::add)
        }
    val likes = decisions.values.mapNotNull { it.like }.sortedByDescending { it.likedAt }
    return TakeoutActivity(TakeoutLikes(likes.take(likeLimit), likes.size), searches, watches)
}

internal fun takeoutSearchOf(url: String?): Pair<String, Boolean>? {
    val uri = runCatching { URI(url?.trim()) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    val isMusic = host == YOUTUBE_MUSIC_HOST
    val key =
        when {
            isMusic && uri.path == "/search" -> "q"
            !isMusic && (host == "youtube.com" || host.endsWith(".youtube.com")) && uri.path == "/results" -> "search_query"
            else -> return null
        }
    val query =
        uri.rawQuery
            ?.split('&')
            ?.firstOrNull { it.startsWith("$key=") }
            ?.substringAfter('=')
            ?.let { runCatching { URLDecoder.decode(it, Charsets.UTF_8.name()) }.getOrNull() }
            ?.trim()
            ?.takeIf(String::isNotEmpty) ?: return null
    return query to isMusic
}

private fun MyActivityRecord.videoId(): String? =
    watchVideoIdPattern
        .find(titleUrl.orEmpty())
        ?.groupValues
        ?.get(1)

private fun MyActivityRecord.isMusic(): Boolean = header == YOUTUBE_MUSIC_HEADER || titleUrl.orEmpty().contains(YOUTUBE_MUSIC_HOST)

private fun MyActivityRecord.channelId(): String =
    activityChannelIdPattern
        .find(subtitles?.firstOrNull()?.url.orEmpty())
        ?.groupValues
        ?.get(1)
        .orEmpty()

private fun MyActivityRecord.toLike(
    videoId: String,
    title: String,
    likedAt: Long,
): TakeoutLike? {
    val videoTitle = title.removePrefix(LIKED_PREFIX).trim().takeIf(String::isNotEmpty) ?: return null
    return TakeoutLike(
        videoId = videoId,
        title = videoTitle,
        channelName =
            subtitles
                ?.firstOrNull()
                ?.name
                ?.trim()
                .orEmpty(),
        channelId = channelId(),
        likedAt = likedAt,
        isMusic = isMusic(),
    )
}

private fun MyActivityRecord.toWatch(
    title: String,
    watchedAt: Long,
): TakeoutWatch? {
    val videoId = videoId() ?: return null
    val videoTitle = title.removePrefix(WATCHED_PREFIX).trim().takeIf(String::isNotEmpty) ?: return null
    return TakeoutWatch(
        videoId = videoId,
        title = videoTitle,
        channelName =
            subtitles
                ?.firstOrNull()
                ?.name
                ?.trim()
                .orEmpty(),
        channelId = channelId(),
        watchedAt = watchedAt,
        isMusic = isMusic(),
    )
}
