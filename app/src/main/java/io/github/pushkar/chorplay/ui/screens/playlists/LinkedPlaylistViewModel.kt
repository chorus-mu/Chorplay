package io.github.pushkar.chorplay.ui.screens.playlists

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.repository.RemotePlaylistPage
import io.github.pushkar.chorplay.data.repository.YouTubePlaylistRepository
import io.github.pushkar.chorplay.data.repository.walkPlaylistPages
import javax.inject.Inject

internal data class LinkedQueue<T>(
    val items: List<T>,
    val startIndex: Int,
    val title: String?,
)

@HiltViewModel
internal class LinkedPlaylistViewModel
@Inject
constructor(
    private val playlists: YouTubePlaylistRepository,
) : ViewModel() {
    suspend fun videoQueue(
        playlistId: String,
        startVideoId: String,
    ): LinkedQueue<Video>? {
        playlists.cachedComplete(playlistId)?.let { cached ->
            return linkedQueue(cached.videos, startVideoId, cached.title, Video::id)
        }
        val first = playlists.firstPage(playlistId) ?: return null
        return findLinkedVideoQueue(first, startVideoId) { token -> playlists.nextPage(playlistId, token) }
    }
}

internal fun <T> linkedQueue(
    items: List<T>,
    startId: String,
    title: String?,
    idOf: (T) -> String,
): LinkedQueue<T>? {
    val index = items.indexOfFirst { idOf(it) == startId }.takeIf { it >= 0 } ?: return null
    return LinkedQueue(items, index, title)
}

internal suspend fun findLinkedVideoQueue(
    firstPage: RemotePlaylistPage,
    videoId: String,
    fetchNext: suspend (String) -> Pair<List<Video>, String?>?,
): LinkedQueue<Video>? {
    val accumulated = ArrayList<Video>(firstPage.videos)
    var targetIndex = accumulated.indexOfFirst { it.id == videoId }
    if (targetIndex >= 0) return LinkedQueue(accumulated, targetIndex, firstPage.title)

    walkPlaylistPages(
        firstVideos = firstPage.videos,
        firstToken = firstPage.continuation,
        maxPages = YouTubePlaylistRepository.PAGE_LIMIT,
        nextPage = fetchNext,
        keepGoing = { currentVideos ->
            targetIndex = currentVideos.indexOfFirst { it.id == videoId }
            if (targetIndex >= 0) {
                accumulated.clear()
                accumulated.addAll(currentVideos)
                false
            } else {
                true
            }
        },
    )

    return if (targetIndex >= 0) LinkedQueue(accumulated, targetIndex, firstPage.title) else null
}
