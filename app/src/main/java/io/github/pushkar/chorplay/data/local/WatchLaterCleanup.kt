package io.github.pushkar.chorplay.data.local

import io.github.pushkar.chorplay.data.local.entity.PlaylistVideoCrossRef
import io.github.pushkar.chorplay.data.model.Video
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchLaterCleanup
    @Inject
    constructor(
        private val playlists: PlaylistRepository,
        private val preferences: PlayerPreferences,
        private val viewHistory: ViewHistory,
    ) {
        suspend fun onProgress(
            videoId: String,
            positionMs: Long,
            durationMs: Long,
        ) {
            if (!preferences.removeWatchedFromWatchLater.first()) return
            if (!preferences.watchedThreshold.first().isWatched(positionMs, durationMs)) return
            if (playlists.isInWatchLater(videoId)) playlists.removeFromWatchLater(videoId)
        }

        suspend fun onFinished(videoId: String) {
            if (!preferences.removeWatchedFromWatchLater.first()) return
            if (playlists.isInWatchLater(videoId)) playlists.removeFromWatchLater(videoId)
        }

        suspend fun sweep(videos: List<Video>): List<PlaylistVideoCrossRef> {
            if (videos.isEmpty() || !preferences.removeWatchedFromWatchLater.first()) return emptyList()
            val watched = watchedIds(videos, viewHistory.getVideoHistoryFlow().first(), preferences.watchedThreshold.first())
            if (watched.isEmpty()) return emptyList()
            return playlists.takeVideosFromPlaylist(PlaylistRepository.WATCH_LATER_ID, watched)
        }
    }

internal fun watchedIds(
    videos: List<Video>,
    history: List<VideoHistoryEntry>,
    threshold: WatchedThreshold,
): List<String> {
    val progress = history.associateBy { it.videoId }
    return videos.mapNotNull { video ->
        video.id.takeIf { progress[it]?.let { entry -> threshold.isWatched(entry.position, entry.duration) } == true }
    }
}
