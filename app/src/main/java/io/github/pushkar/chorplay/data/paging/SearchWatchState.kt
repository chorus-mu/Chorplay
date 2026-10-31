package io.github.pushkar.chorplay.data.paging

import io.github.pushkar.chorplay.data.local.WatchState
import io.github.pushkar.chorplay.data.local.WatchedThreshold
import io.github.pushkar.chorplay.data.local.dao.WatchProgress
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.shorts.isShortComplete

fun interface WatchedVideoLookup {
    suspend operator fun invoke(videos: List<Video>): Set<String>
}

internal fun List<SearchResultItem>.videos(): List<Video> =
    flatMap { item ->
        when (item) {
            is SearchResultItem.VideoResult -> listOf(item.video)
            is SearchResultItem.ShelfResult -> item.videos
            is SearchResultItem.ChannelResult, is SearchResultItem.PlaylistResult -> emptyList()
        }
    }

internal fun List<SearchResultItem>.withWatchState(
    state: WatchState,
    watchedIds: Set<String>,
): List<SearchResultItem> {
    if (state == WatchState.ANY) return this
    val keepWatched = state == WatchState.WATCHED

    fun Video.kept() = (id in watchedIds) == keepWatched

    return mapNotNull { item ->
        when (item) {
            is SearchResultItem.VideoResult -> {
                item.takeIf { it.video.kept() }
            }

            is SearchResultItem.ChannelResult, is SearchResultItem.PlaylistResult -> {
                item.takeUnless { keepWatched }
            }

            is SearchResultItem.ShelfResult -> {
                val videos = item.videos.filter { it.kept() }
                when {
                    videos.isNotEmpty() -> item.copy(videos = videos)
                    item.posts.isNotEmpty() && !keepWatched -> item
                    else -> null
                }
            }
        }
    }
}

internal fun watchedVideoIds(
    videos: List<Video>,
    progress: List<WatchProgress>,
    threshold: WatchedThreshold,
): Set<String> {
    val byId = progress.associateBy { it.videoId }
    return videos
        .filter { video ->
            val entry = byId[video.id] ?: return@filter false
            if (video.isShort) {
                isShortComplete(entry.position, entry.duration)
            } else {
                threshold.isWatched(entry.position, entry.duration)
            }
        }.mapTo(HashSet()) { it.id }
}
