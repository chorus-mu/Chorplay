package io.github.pushkar.chorplay.data.local

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.local.entity.PlaylistEntity
import io.github.pushkar.chorplay.data.local.entity.PlaylistVideoCrossRef
import io.github.pushkar.chorplay.data.local.entity.VideoEntity
import io.github.pushkar.chorplay.data.recommendation.ChorplayNeuroEngine
import io.github.pushkar.chorplay.di.ProfileDb
import io.github.pushkar.chorplay.utils.ThumbnailUrlResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.SerializationException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream

private const val ENGLISH_TAKEOUT_WATCH_HISTORY = "history/watch-history.html"
private const val TAKEOUT_PLAYLIST_ID_PREFIX = "yt_takeout_"
private const val NEURO_CANDIDATE_LIMIT = 800
private const val WATCH_BATCH = 500

internal class YouTubeTakeoutImporter(
    private val context: Context,
    @ProfileDb private val database: AppDatabase,
    private val viewHistory: ViewHistory,
    private val searchHistory: SearchHistoryRepository,
    private val subscriptions: SubscriptionRepository,
    private val saveLikes: suspend (List<TakeoutLike>) -> Int,
    private val channelAvatar: suspend (String) -> String,
    private val learnFromHistory: suspend (Collection<VideoHistoryEntry>) -> Unit,
) {
    private class Found {
        val subscriptions = mutableListOf<YouTubeTakeoutSubscription>()
        val playlistsByDirectory = mutableMapOf<String, MutableList<TakeoutPlaylistInfo>>()
        val playlistVideos = mutableMapOf<String, List<String>>()
        var likes: List<TakeoutLike>? = null
        val searches = mutableListOf<TakeoutSearch>()
        var watches = 0
        val learnable = LinkedHashMap<String, VideoHistoryEntry>()
    }

    suspend fun import(
        uris: List<Uri>,
        onProgress: ((label: String, current: Int, total: Int) -> Unit)? = null,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val found = Found()
                val budget = YouTubeTakeoutCsvBudget()
                for (uri in uris) {
                    context.contentResolver.openInputStream(uri)
                        ?.use { raw -> readArchive(raw, found, budget, onProgress) }
                        ?: return@withContext Result.failure(Exception("Could not open file"))
                }

                val subscriptionsImported = saveSubscriptions(found.subscriptions, onProgress)
                val playlists = savePlaylists(found)
                val likesImported = found.likes?.let { saveLikes(it) } ?: 0
                val searchesImported = saveSearches(found.searches)

                val counts =
                    listOf(
                        subscriptionsImported,
                        found.watches,
                        playlists.count,
                        likesImported,
                        searchesImported
                    )
                if (counts.all { it == 0 }) return@withContext Result.failure(Exception("no_content"))
                if (found.watches > 0) runCatching { learnFromHistory(found.learnable.values) }

                Result.success(
                    buildList {
                        if (subscriptionsImported > 0) add(
                            plural(
                                R.plurals.import_takeout_part_subscriptions,
                                subscriptionsImported
                            )
                        )
                        if (found.watches > 0) add(plural(R.plurals.import_takeout_part_history, found.watches))
                        if (playlists.count > 0) {
                            add(
                                context.resources.getQuantityString(
                                    R.plurals.import_takeout_part_playlists,
                                    playlists.count,
                                    playlists.count,
                                    playlists.videos,
                                ),
                            )
                        }
                        if (likesImported > 0) add(context.getString(R.string.import_takeout_part_likes, likesImported))
                        if (searchesImported > 0) add(plural(R.plurals.import_takeout_part_searches, searchesImported))
                    }.joinToString(", "),
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun importHtmlHistory(input: InputStream): Int {
        val found = Found()
        val read =
            input.bufferedReader(Charsets.UTF_8).use { reader ->
                readTakeoutHtmlActivity(
                    reader,
                    System.currentTimeMillis(),
                    requireActivityMarkup = false
                ) { saveWatches(it, found) }
            }
        if (found.watches > 0) runCatching { learnFromHistory(found.learnable.values) }
        return found.watches + saveSearches(read.searches)
    }

    private suspend fun readArchive(
        raw: InputStream,
        found: Found,
        budget: YouTubeTakeoutCsvBudget,
        onProgress: ((String, Int, Int) -> Unit)?,
    ) {
        ZipInputStream(raw.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                val english = name.endsWith(ENGLISH_TAKEOUT_WATCH_HISTORY, ignoreCase = true)
                when {
                    english || isYouTubeTakeoutHtmlEntry(name) -> {
                        onProgress?.invoke(context.getString(R.string.import_takeout_step_history), 0, 0)

                        val start = System.currentTimeMillis() - found.watches - found.searches.size
                        val read =
                            readTakeoutHtmlActivity(
                                zip.bufferedReader(Charsets.UTF_8),
                                start,
                                requireActivityMarkup = !english
                            ) {
                                saveWatches(it, found)
                            }
                        found.searches += read.searches
                    }

                    !entry.isDirectory && isYouTubeTakeoutJsonEntry(name) -> {
                        onProgress?.invoke(context.getString(R.string.import_takeout_step_history), 0, 0)
                        readActivity(zip, keepWatches = true)?.let { activity ->
                            activity.watches.chunked(WATCH_BATCH).forEach { saveWatches(it, found) }
                            found.searches += activity.searches
                        }
                    }

                    !entry.isDirectory && isMyActivityYouTubeEntry(name) -> {
                        onProgress?.invoke(context.getString(R.string.import_takeout_step_activity), 0, 0)
                        readActivity(zip, keepWatches = false)?.let { activity ->
                            if (found.likes == null) found.likes = activity.likes.likes
                            found.searches += activity.searches
                        }
                    }

                    !entry.isDirectory && isYouTubeTakeoutCsvEntry(name) -> {
                        budget.startEntry()
                        when (val content = readYouTubeTakeoutCsv(zip.bufferedReader(Charsets.UTF_8), budget)) {
                            is YouTubeTakeoutCsvContent.Subscriptions -> {
                                onProgress?.invoke(context.getString(R.string.import_takeout_step_subscriptions), 0, 0)
                                found.subscriptions += content.rows
                            }

                            is YouTubeTakeoutCsvContent.PlaylistVideos -> {
                                found.playlistVideos[name] = content.videoIds
                            }

                            is YouTubeTakeoutCsvContent.PlaylistMetadata -> {
                                found.playlistsByDirectory.getOrPut(name.takeoutParentPath()) { mutableListOf() } += content.playlists
                            }

                            YouTubeTakeoutCsvContent.Unsupported -> {}
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun readActivity(
        input: InputStream,
        keepWatches: Boolean,
    ): TakeoutActivity? =
        try {
            readTakeoutActivity(input, keepWatches = keepWatches)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private suspend fun saveSearches(searches: List<TakeoutSearch>): Int =
        searchHistory.importSearches(
            searches.map { SearchHistoryItem(query = it.query, timestamp = it.searchedAt) },
            SearchHistoryScope.VIDEO
        )

    private suspend fun saveWatches(
        watches: List<TakeoutWatch>,
        found: Found,
    ) {
        if (watches.isEmpty()) return
        val filtered =
            watches.filterNot { watch ->
                watch.channelName.contains("Google Ads", ignoreCase = true) ||
                    watch.title.contains("Google Ads", ignoreCase = true) ||
                    watch.title.equals("Watched an ad", ignoreCase = true) ||
                    watch.title.equals("an ad", ignoreCase = true)
            }
        if (filtered.isEmpty()) return
        val entries = filtered.map { it.toHistoryEntry() }
        viewHistory.bulkSaveHistoryEntries(entries)
        entries.forEach { entry ->
            if (found.learnable.size < NEURO_CANDIDATE_LIMIT && entry.title.isNotBlank()) found.learnable.putIfAbsent(
                entry.videoId,
                entry
            )
        }
        found.watches += entries.size
        yield()
    }

    private suspend fun saveSubscriptions(
        rows: List<YouTubeTakeoutSubscription>,
        onProgress: ((String, Int, Int) -> Unit)?,
    ): Int {
        if (rows.isEmpty()) return 0
        val label = context.getString(R.string.import_takeout_step_subscriptions)
        onProgress?.invoke(label, 0, rows.size)
        val completed = AtomicInteger(0)
        val now = System.currentTimeMillis()
        val imported =
            rows.map { sub ->
                onProgress?.invoke(label, completed.incrementAndGet(), rows.size)
                ChannelSubscription(
                    channelId = sub.channelId,
                    channelName = sub.channelName,
                    channelThumbnail = "",
                    subscribedAt = now,
                )
            }
        subscriptions.subscribeAll(imported)
        val names = rows.map { it.channelName }.filter { it.isNotEmpty() }
        if (names.isNotEmpty()) runCatching { ChorplayNeuroEngine.bootstrapFromSubscriptions(context, names) }
        return imported.size
    }

    private data class SavedPlaylists(
        val count: Int,
        val videos: Int,
    )

    private suspend fun savePlaylists(found: Found): SavedPlaylists {
        validateYouTubeTakeoutPlaylistCount(
            videoFileCount = found.playlistVideos.size,
            metadataTitleCount = found.playlistsByDirectory.values.sumOf { it.size },
        )
        val fallbackName = context.getString(R.string.imported_playlist_fallback)
        var playlists = 0
        var videos = 0
        found.playlistVideos.keys
            .groupBy { it.takeoutParentPath() }
            .forEach { (directory, files) ->
                val infos = found.playlistsByDirectory[directory].orEmpty()
                val unclaimed = infos.toMutableList()
                resolveYouTubeTakeoutPlaylistNames(
                    files,
                    infos.map { it.title },
                    fallbackName
                ).forEach { (file, name) ->
                    val info = unclaimed.firstOrNull { it.title == name }?.also(unclaimed::remove)
                    val videoIds = found.playlistVideos.getValue(file)
                    savePlaylist(file, name, info, videoIds)
                    playlists++
                    videos += videoIds.size
                }
            }
        return SavedPlaylists(playlists, videos)
    }

    private suspend fun savePlaylist(
        file: String,
        name: String,
        info: TakeoutPlaylistInfo?,
        videoIds: List<String>,
    ) {
        val isWatchLater = name.equals("watch later", ignoreCase = true)
        val playlistId =
            when {
                isWatchLater -> PlaylistRepository.WATCH_LATER_ID
                info != null -> TAKEOUT_PLAYLIST_ID_PREFIX + info.id
                else -> TAKEOUT_PLAYLIST_ID_PREFIX + file.substringAfterLast('/').hashCode().toUInt()
            }
        val entity =
            PlaylistEntity(
                id = playlistId,
                name = if (isWatchLater) "Watch Later" else name,
                description = context.getString(R.string.imported_from_google_takeout),
                thumbnailUrl = ThumbnailUrlResolver.buildHighQualityYoutubeThumbnail(videoIds.first()),
                isPrivate = isWatchLater,
                createdAt = info?.createdAt ?: System.currentTimeMillis(),
                isMusic = false,
                isUserCreated = true,
            )
        fillPlaylist(entity, videoIds.map { videoEntity(it, title = "", channelName = "") })
    }

    private fun plural(
        res: Int,
        count: Int,
    ): String = context.resources.getQuantityString(res, count, count)

    private suspend fun fillPlaylist(
        playlist: PlaylistEntity,
        videos: List<VideoEntity>,
    ) {
        if (videos.isEmpty()) return
        database.withTransaction {
            val dao = database.playlistDao()
            if (dao.getPlaylist(playlist.id) == null) dao.insertPlaylist(playlist)
            val held = dao.getVideoIdsInPlaylist(playlist.id).toHashSet()
            var position = (dao.getMaxPlaylistPosition(playlist.id) ?: -1L) + 1L
            videos.forEach { video ->
                database.videoDao().insertVideoOrIgnore(video)
                if (!held.add(video.id)) return@forEach
                dao.insertPlaylistVideoCrossRef(
                    PlaylistVideoCrossRef(
                        playlistId = playlist.id,
                        videoId = video.id,
                        position = position++
                    )
                )
            }
        }
    }
}

private fun TakeoutWatch.toHistoryEntry() =
    VideoHistoryEntry(
        videoId = videoId,
        position = 0L,
        duration = 0L,
        timestamp = watchedAt,
        title = title,
        thumbnailUrl = ThumbnailUrlResolver.buildHighQualityYoutubeThumbnail(videoId),
        channelName = channelName,
        channelId = channelId,
        isMusic = false,
    )

private fun videoEntity(
    videoId: String,
    title: String = "",
    channelName: String = "",
    isMusic: Boolean = false,
) = VideoEntity(
    id = videoId,
    title = title,
    channelName = channelName,
    channelId = "",
    thumbnailUrl = ThumbnailUrlResolver.buildHighQualityYoutubeThumbnail(videoId),
    duration = 0,
    viewCount = 0L,
    uploadDate = "",
    description = "",
    channelThumbnailUrl = "",
    isMusic = false,
)
